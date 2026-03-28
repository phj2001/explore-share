<template>
  <div class="report-page">
    <section class="page-hero">
      <div class="hero-copy">
        <span class="hero-kicker">举报审核</span>
        <h1>统一处理分享与回复的用户举报，形成内容治理闭环</h1>
        <p>支持按对象类型、处理状态和举报理由筛选，并在详情中直接完成驳回或处理动作。</p>
      </div>

      <div class="hero-stats">
        <article v-for="item in heroStats" :key="item.label" class="hero-stat">
          <span>{{ item.label }}</span>
          <strong>{{ item.value }}</strong>
          <em>{{ item.helper }}</em>
        </article>
      </div>
    </section>

    <section class="filter-panel">
      <div class="filter-main">
        <el-input
          v-model="keyword"
          clearable
          class="filter-input"
          placeholder="搜索举报内容、被举报作者、举报人或 POI 名称"
          @keyup.enter="handleSearch"
          @clear="handleSearch"
        >
          <template #append>
            <el-button :icon="Search" @click="handleSearch" />
          </template>
        </el-input>

        <el-select v-model="targetType" clearable class="filter-select" placeholder="筛选对象类型" @change="handleSearch">
          <el-option v-for="item in REPORT_TARGET_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>

        <el-select v-model="status" clearable class="filter-select" placeholder="筛选处理状态" @change="handleSearch">
          <el-option v-for="item in REPORT_STATUS_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>

        <el-select v-model="reasonCode" clearable class="filter-select" placeholder="筛选举报理由" @change="handleSearch">
          <el-option v-for="item in REPORT_REASON_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>

        <el-button :icon="RefreshRight" @click="resetFilters">重置</el-button>
      </div>
    </section>

    <section class="table-panel">
      <div class="panel-head">
        <div>
          <span class="panel-kicker">审核列表</span>
          <h2>举报记录</h2>
        </div>
        <el-button text @click="loadReports">刷新列表</el-button>
      </div>

      <el-table :data="reports" v-loading="loading" stripe>
        <template #empty>
          <el-empty description="当前筛选条件下暂无举报记录" />
        </template>

        <el-table-column prop="id" label="举报ID" width="96" />

        <el-table-column label="对象类型" width="110">
          <template #default="{ row }">
            <el-tag effect="plain" :type="row.targetType === REPORT_TARGET_SHARE ? 'primary' : 'warning'">
              {{ getReportTargetLabel(row.targetType) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="被举报内容" min-width="280" show-overflow-tooltip>
          <template #default="{ row }">
            <div class="content-meta">
              <strong>{{ row.targetContentPreview }}</strong>
              <span>{{ row.targetAuthorDisplayName || row.targetAuthorUsername }}</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="举报人" min-width="160">
          <template #default="{ row }">
            <div class="content-meta compact">
              <strong>{{ row.reporterDisplayName || row.reporterUsername }}</strong>
              <span>@{{ row.reporterUsername }}</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="理由" width="120">
          <template #default="{ row }">
            {{ getReportReasonLabel(row.reasonCode) }}
          </template>
        </el-table-column>

        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" effect="plain">
              {{ getReportStatusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="创建时间" width="180">
          <template #default="{ row }">
            {{ formatDate(row.createdAt) }}
          </template>
        </el-table-column>

        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <div class="action-group">
              <el-button size="small" @click="openDetail(row.id)">查看</el-button>
              <el-button size="small" type="primary" plain @click="openReviewDialog(row.id)">审核</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-bar">
        <span>共 {{ total }} 条举报</span>
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50]"
          :total="total"
          layout="sizes, prev, pager, next"
          @current-change="loadReports"
          @size-change="handlePageSizeChange"
        />
      </div>
    </section>

    <el-drawer v-model="drawerVisible" size="620px" :with-header="false" destroy-on-close>
      <div class="drawer-body" v-loading="detailLoading">
        <template v-if="selectedReport">
          <div class="drawer-hero">
            <div>
              <div class="drawer-tags">
                <el-tag :type="selectedReport.targetType === REPORT_TARGET_SHARE ? 'primary' : 'warning'" effect="plain">
                  {{ getReportTargetLabel(selectedReport.targetType) }}
                </el-tag>
                <el-tag :type="statusTagType(selectedReport.status)" effect="plain">
                  {{ getReportStatusLabel(selectedReport.status) }}
                </el-tag>
              </div>
              <h2>举报 #{{ selectedReport.id }}</h2>
              <p>{{ selectedReport.targetPoiName || '未关联 POI' }}</p>
            </div>

            <div class="drawer-actions">
              <el-button plain @click="goToTarget(selectedReport)">查看内容</el-button>
              <el-button type="primary" plain @click="openReviewDialog(selectedReport.id)">审核处理</el-button>
            </div>
          </div>

          <div class="info-grid">
            <div class="info-item">
              <span>举报理由</span>
              <strong>{{ getReportReasonLabel(selectedReport.reasonCode) }}</strong>
            </div>
            <div class="info-item">
              <span>当前状态</span>
              <strong>{{ getReportStatusLabel(selectedReport.status) }}</strong>
            </div>
            <div class="info-item">
              <span>举报人</span>
              <strong>{{ selectedReport.reporterDisplayName || selectedReport.reporterUsername }}</strong>
            </div>
            <div class="info-item">
              <span>被举报作者</span>
              <strong>{{ selectedReport.targetAuthorDisplayName || selectedReport.targetAuthorUsername }}</strong>
            </div>
          </div>

          <div class="drawer-section">
            <h3>举报说明</h3>
            <div class="content-card">
              {{ selectedReport.reasonDetail || '未填写补充说明' }}
            </div>
          </div>

          <div class="drawer-section">
            <h3>举报快照</h3>
            <div class="content-card">
              {{ selectedReport.targetContentPreview }}
            </div>
          </div>

          <div class="drawer-section">
            <h3>当前内容状态</h3>
            <div class="content-card">
              {{ selectedReport.targetExists ? (selectedReport.currentTargetContent || '该内容未填写文字或仅包含图片') : '该内容已不存在' }}
            </div>
          </div>

          <div v-if="selectedReport.reviewedAt" class="drawer-section">
            <h3>处理结果</h3>
            <div class="content-card">
              <p>处理动作：{{ getReportActionLabel(selectedReport.reviewAction) }}</p>
              <p>处理备注：{{ selectedReport.reviewNote || '未填写处理备注' }}</p>
              <p>处理时间：{{ formatDate(selectedReport.reviewedAt) }}</p>
            </div>
          </div>
        </template>
      </div>
    </el-drawer>

    <el-dialog
      v-model="reviewDialogVisible"
      title="处理举报"
      width="560px"
      destroy-on-close
      @closed="resetReviewForm"
    >
      <div v-if="reviewingReport" class="review-form">
        <div class="review-target">
          <span>{{ getReportTargetLabel(reviewingReport.targetType) }}</span>
          <strong>{{ reviewingReport.targetContentPreview }}</strong>
        </div>

        <el-form label-position="top">
          <el-form-item label="处理结果">
            <el-radio-group v-model="reviewStatus">
              <el-radio :label="REPORT_STATUS_PROCESSED">已处理</el-radio>
              <el-radio :label="REPORT_STATUS_REJECTED">已驳回</el-radio>
            </el-radio-group>
          </el-form-item>

          <el-form-item v-if="reviewStatus === REPORT_STATUS_PROCESSED" label="处理动作">
            <el-radio-group v-model="reviewAction">
              <el-radio
                v-for="item in REPORT_ACTION_OPTIONS"
                :key="item.value"
                :label="item.value"
              >
                {{ item.label }}
              </el-radio>
            </el-radio-group>
          </el-form-item>

          <el-form-item label="处理备注">
            <el-input
              v-model="reviewNote"
              type="textarea"
              :rows="4"
              maxlength="200"
              show-word-limit
              resize="none"
              placeholder="可填写处理说明，便于后续追踪"
            />
          </el-form-item>
        </el-form>
      </div>

      <template #footer>
        <div class="dialog-actions">
          <el-button @click="reviewDialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="reviewSubmitting" @click="submitReview">确认处理</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { RefreshRight, Search } from '@element-plus/icons-vue'
import { getAdminContentReportDetail, getAdminContentReportPage, reviewAdminContentReport } from '@/api/adminContentReport'
import {
  getReportActionLabel,
  getReportReasonLabel,
  getReportStatusLabel,
  getReportTargetLabel,
  REPORT_ACTION_NONE,
  REPORT_ACTION_OPTIONS,
  REPORT_REASON_OPTIONS,
  REPORT_STATUS_OPTIONS,
  REPORT_STATUS_PENDING,
  REPORT_STATUS_PROCESSED,
  REPORT_STATUS_REJECTED,
  REPORT_TARGET_OPTIONS,
  REPORT_TARGET_REPLY,
  REPORT_TARGET_SHARE
} from '@/constants/contentReport'

const router = useRouter()

const keyword = ref('')
const targetType = ref()
const status = ref()
const reasonCode = ref()
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const reports = ref([])
const loading = ref(false)

const drawerVisible = ref(false)
const detailLoading = ref(false)
const selectedReport = ref(null)

const reviewDialogVisible = ref(false)
const reviewingReport = ref(null)
const reviewStatus = ref(REPORT_STATUS_PROCESSED)
const reviewAction = ref(REPORT_ACTION_NONE)
const reviewNote = ref('')
const reviewSubmitting = ref(false)

const heroStats = computed(() => {
  return [
    { label: '当前总量', value: `${total.value}`, helper: '符合当前筛选条件的举报数量' },
    { label: '本页待处理', value: `${reports.value.filter((item) => item.status === REPORT_STATUS_PENDING).length}`, helper: '优先处理这些内容风险' },
    { label: '本页已处理', value: `${reports.value.filter((item) => item.status === REPORT_STATUS_PROCESSED).length}`, helper: '已完成审核处置' },
    { label: '本页已驳回', value: `${reports.value.filter((item) => item.status === REPORT_STATUS_REJECTED).length}`, helper: '已确认无需进一步操作' }
  ]
})

const loadReports = async () => {
  loading.value = true
  try {
    const data = await getAdminContentReportPage({
      keyword: keyword.value.trim() || undefined,
      targetType: targetType.value,
      status: status.value,
      reasonCode: reasonCode.value,
      page: currentPage.value - 1,
      size: pageSize.value
    })
    reports.value = data.records || []
    total.value = data.total || 0
    currentPage.value = (data.page || 0) + 1
  } catch (error) {
    ElMessage.error(error.message || '加载举报列表失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = async () => {
  currentPage.value = 1
  await loadReports()
}

const handlePageSizeChange = async () => {
  currentPage.value = 1
  await loadReports()
}

const resetFilters = async () => {
  keyword.value = ''
  targetType.value = undefined
  status.value = undefined
  reasonCode.value = undefined
  currentPage.value = 1
  await loadReports()
}

const openDetail = async (reportId) => {
  drawerVisible.value = true
  detailLoading.value = true
  try {
    selectedReport.value = await getAdminContentReportDetail(reportId)
  } catch (error) {
    drawerVisible.value = false
    selectedReport.value = null
    ElMessage.error(error.message || '加载举报详情失败')
  } finally {
    detailLoading.value = false
  }
}

const openReviewDialog = async (reportId) => {
  try {
    const detail = await getAdminContentReportDetail(reportId)
    reviewingReport.value = detail
    reviewStatus.value = detail.status === REPORT_STATUS_REJECTED ? REPORT_STATUS_REJECTED : REPORT_STATUS_PROCESSED
    reviewAction.value = detail.reviewAction ?? REPORT_ACTION_NONE
    reviewNote.value = detail.reviewNote || ''
    reviewDialogVisible.value = true
  } catch (error) {
    ElMessage.error(error.message || '加载举报详情失败')
  }
}

const resetReviewForm = () => {
  reviewingReport.value = null
  reviewStatus.value = REPORT_STATUS_PROCESSED
  reviewAction.value = REPORT_ACTION_NONE
  reviewNote.value = ''
  reviewSubmitting.value = false
}

const submitReview = async () => {
  if (!reviewingReport.value) {
    return
  }

  reviewSubmitting.value = true
  try {
    const detail = await reviewAdminContentReport(reviewingReport.value.id, {
      status: reviewStatus.value,
      action: reviewStatus.value === REPORT_STATUS_PROCESSED ? reviewAction.value : REPORT_ACTION_NONE,
      reviewNote: reviewNote.value.trim() || undefined
    })

    reviewDialogVisible.value = false
    if (selectedReport.value?.id === detail.id) {
      selectedReport.value = detail
    }
    await loadReports()
    ElMessage.success('举报处理结果已更新')
  } catch (error) {
    ElMessage.error(error.message || '处理举报失败')
  } finally {
    reviewSubmitting.value = false
  }
}

const goToTarget = (report) => {
  const openShareId = report.targetType === REPORT_TARGET_SHARE ? report.targetId : report.relatedShareId
  if (!openShareId) {
    return
  }
  router.push({
    path: '/admin/shares',
    query: { openShareId: String(openShareId) }
  })
}

const formatDate = (value) => {
  if (!value) {
    return '暂无'
  }

  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  }).format(new Date(value))
}

const statusTagType = (value) => {
  if (value === REPORT_STATUS_PENDING) {
    return 'warning'
  }
  if (value === REPORT_STATUS_PROCESSED) {
    return 'success'
  }
  return 'info'
}

onMounted(async () => {
  await loadReports()
})
</script>

<style scoped>
.report-page {
  --panel-bg: rgba(255, 255, 255, 0.9);
  --panel-border: rgba(148, 163, 184, 0.18);
  --ink: #0f172a;
  --muted: #64748b;
  --accent: #dc2626;
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.page-hero,
.filter-panel,
.table-panel {
  border: 1px solid var(--panel-border);
  background: var(--panel-bg);
  backdrop-filter: blur(18px);
  box-shadow: 0 18px 42px rgba(15, 23, 42, 0.06);
}

.page-hero {
  border-radius: 30px;
  padding: 28px 30px;
  display: grid;
  grid-template-columns: minmax(0, 1.3fr) minmax(280px, 0.9fr);
  gap: 20px;
  background:
    radial-gradient(circle at top right, rgba(248, 113, 113, 0.18), transparent 26%),
    linear-gradient(135deg, rgba(255, 255, 255, 0.98), rgba(254, 242, 242, 0.94));
}

.hero-kicker,
.panel-kicker {
  display: inline-flex;
  align-items: center;
  padding: 6px 12px;
  border-radius: 999px;
  background: rgba(220, 38, 38, 0.08);
  color: var(--accent);
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
}

.hero-copy h1 {
  margin: 14px 0 10px;
  font-size: clamp(28px, 4vw, 40px);
  line-height: 1.08;
  color: var(--ink);
}

.hero-copy p {
  margin: 0;
  color: var(--muted);
  line-height: 1.8;
}

.hero-stats {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.hero-stat {
  padding: 18px;
  border-radius: 22px;
  border: 1px solid rgba(252, 165, 165, 0.5);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.95), rgba(254, 242, 242, 0.92));
}

.hero-stat span,
.hero-stat em {
  display: block;
  color: var(--muted);
  font-style: normal;
}

.hero-stat strong {
  display: block;
  margin: 12px 0 10px;
  font-size: 30px;
  line-height: 1;
  color: var(--ink);
}

.filter-panel {
  border-radius: 26px;
  padding: 18px;
}

.filter-main,
.action-group,
.drawer-tags,
.drawer-actions,
.dialog-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.filter-input {
  width: 360px;
}

.filter-select {
  width: 180px;
}

.table-panel {
  border-radius: 28px;
  padding: 18px 18px 8px;
}

.panel-head {
  margin-bottom: 14px;
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
}

.panel-head h2 {
  margin: 12px 0 0;
  color: var(--ink);
  font-size: 24px;
}

.content-meta {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.content-meta.compact {
  gap: 2px;
}

.content-meta strong {
  color: var(--ink);
}

.content-meta span {
  color: var(--muted);
  font-size: 12px;
}

.pagination-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 18px 6px 8px;
  color: var(--muted);
}

.drawer-body {
  height: 100%;
  padding: 24px;
  display: flex;
  flex-direction: column;
  gap: 22px;
}

.drawer-hero {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
}

.drawer-hero h2 {
  margin: 12px 0 8px;
  color: var(--ink);
  font-size: 30px;
}

.drawer-hero p {
  margin: 0;
  color: var(--muted);
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.info-item,
.content-card,
.review-target {
  border-radius: 18px;
  padding: 16px;
  background: linear-gradient(180deg, rgba(247, 250, 255, 0.96), rgba(255, 255, 255, 0.96));
  border: 1px solid rgba(226, 232, 240, 0.9);
}

.info-item span {
  display: block;
  color: var(--muted);
  font-size: 13px;
}

.info-item strong {
  display: block;
  margin-top: 10px;
  color: var(--ink);
}

.drawer-section {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.drawer-section h3 {
  margin: 0;
  color: var(--ink);
  font-size: 18px;
}

.content-card,
.review-target {
  color: #334155;
  line-height: 1.8;
  white-space: pre-wrap;
}

.review-form {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.review-target span {
  display: block;
  color: var(--muted);
  font-size: 12px;
}

.review-target strong {
  display: block;
  margin-top: 10px;
  color: var(--ink);
}

@media (max-width: 1240px) {
  .page-hero {
    grid-template-columns: 1fr;
  }

  .filter-input,
  .filter-select {
    width: 100%;
  }
}

@media (max-width: 760px) {
  .hero-stats,
  .info-grid {
    grid-template-columns: 1fr;
  }

  .pagination-bar,
  .panel-head,
  .drawer-hero,
  .dialog-actions {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
