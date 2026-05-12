<template>
  <div class="application-page">
    <section class="page-hero">
      <div class="hero-copy">
        <span class="hero-kicker">地点审核</span>
        <h1>审核用户提交的地点申请，共建校园地图</h1>
        <p>支持按状态筛选和关键词搜索，在详情中完成通过或驳回操作。</p>
      </div>
    </section>

    <section class="filter-panel">
      <div class="filter-main">
        <el-input
          v-model="keyword"
          clearable
          class="filter-input"
          placeholder="搜索地点名称或分类"
          @keyup.enter="handleSearch"
          @clear="handleSearch"
        >
          <template #append>
            <el-button :icon="Search" @click="handleSearch" />
          </template>
        </el-input>

        <el-select v-model="status" clearable class="filter-select" placeholder="筛选状态" @change="handleSearch">
          <el-option label="待审核" :value="1" />
          <el-option label="已通过" :value="2" />
          <el-option label="已驳回" :value="3" />
        </el-select>

        <el-button :icon="RefreshRight" @click="resetFilters">重置</el-button>
      </div>
    </section>

    <section class="table-panel">
      <div class="panel-head">
        <div>
          <span class="panel-kicker">审核列表</span>
          <h2>地点申请记录</h2>
        </div>
        <el-button text @click="loadData">刷新列表</el-button>
      </div>

      <el-table :data="applications" v-loading="loading" stripe>
        <template #empty>
          <el-empty description="暂无地点申请" />
        </template>

        <el-table-column prop="id" label="ID" width="72" />

        <el-table-column label="申请人" width="140">
          <template #default="{ row }">
            <div class="applicant-meta">
              <el-avatar :size="28" :src="row.applicantAvatarUrl || undefined">
                {{ (row.applicantName || '?').slice(0, 1) }}
              </el-avatar>
              <span>{{ row.applicantName }}</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column prop="name" label="地点名称" min-width="160" show-overflow-tooltip />

        <el-table-column prop="category" label="分类" width="100" />

        <el-table-column label="坐标" width="180">
          <template #default="{ row }">
            <span class="coord-text">{{ row.longitude }}, {{ row.latitude }}</span>
          </template>
        </el-table-column>

        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" effect="plain">
              {{ statusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="createdAt" label="提交时间" width="170">
          <template #default="{ row }">
            {{ formatDateTime(row.createdAt) }}
          </template>
        </el-table-column>

        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <template v-if="row.status === 1">
              <el-button link type="success" @click="handleReview(row, 2)">通过</el-button>
              <el-button link type="danger" @click="handleReview(row, 3)">驳回</el-button>
            </template>
            <template v-else>
              <el-button link @click="viewDetail(row)">查看</el-button>
            </template>
          </template>
        </el-table-column>
      </el-table>

      <div v-if="hasMore" class="load-more">
        <el-button link type="primary" :loading="loadingMore" @click="loadMore">加载更多</el-button>
      </div>
    </section>

    <!-- 审核弹窗 -->
    <el-dialog v-model="reviewDialogVisible" :title="reviewTarget?.status === 1 ? '审核地点申请' : '申请详情'" :width="500">
      <template v-if="reviewTarget">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="申请人">{{ reviewTarget.applicantName }}</el-descriptions-item>
          <el-descriptions-item label="地点名称">{{ reviewTarget.name }}</el-descriptions-item>
          <el-descriptions-item label="分类">{{ reviewTarget.category }}</el-descriptions-item>
          <el-descriptions-item label="坐标">{{ reviewTarget.longitude }}, {{ reviewTarget.latitude }}</el-descriptions-item>
          <el-descriptions-item v-if="reviewTarget.address" label="地址">{{ reviewTarget.address }}</el-descriptions-item>
          <el-descriptions-item v-if="reviewTarget.description" label="描述">{{ reviewTarget.description }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="statusType(reviewTarget.status)" effect="plain">{{ statusLabel(reviewTarget.status) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item v-if="reviewTarget.reviewedByName" label="审核人">{{ reviewTarget.reviewedByName }}</el-descriptions-item>
          <el-descriptions-item v-if="reviewTarget.reviewNote" label="审核意见">{{ reviewTarget.reviewNote }}</el-descriptions-item>
          <el-descriptions-item v-if="reviewTarget.createdPoiId" label="已创建POI">
            <router-link :to="`/admin/poi-list`">POI #{{ reviewTarget.createdPoiId }}</router-link>
          </el-descriptions-item>
        </el-descriptions>

        <template v-if="reviewTarget.status === 1 && pendingReviewStatus">
          <div class="review-form">
            <el-input
              v-model="reviewNote"
              type="textarea"
              :rows="3"
              :placeholder="pendingReviewStatus === 3 ? '请输入驳回原因（选填）' : '审核意见（选填）'"
              maxlength="500"
              show-word-limit
            />
            <div class="review-actions">
              <el-button @click="cancelReview">取消</el-button>
              <el-button
                :type="pendingReviewStatus === 2 ? 'success' : 'danger'"
                :loading="reviewSubmitting"
                @click="confirmReview"
              >
                {{ pendingReviewStatus === 2 ? '确认通过' : '确认驳回' }}
              </el-button>
            </div>
          </div>
        </template>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, RefreshRight } from '@element-plus/icons-vue'
import { getAdminPOIApplications, reviewPOIApplication } from '@/api/poiApplication'

const keyword = ref('')
const status = ref(null)
const loading = ref(false)
const loadingMore = ref(false)
const applications = ref([])
const page = ref(0)
const hasMore = ref(false)

const reviewDialogVisible = ref(false)
const reviewTarget = ref(null)
const pendingReviewStatus = ref(null)
const reviewNote = ref('')
const reviewSubmitting = ref(false)

const statusLabel = (s) => ({ 1: '待审核', 2: '已通过', 3: '已驳回' }[s] || '未知')
const statusType = (s) => ({ 1: 'warning', 2: 'success', 3: 'danger' }[s] || 'info')

const formatDateTime = (dt) => {
  if (!dt) return ''
  return new Date(dt).toLocaleString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
}

const loadData = async (reset = false) => {
  const nextPage = reset ? 0 : page.value + 1
  const loadingRef = reset ? loading : loadingMore
  loadingRef.value = true
  try {
    const data = await getAdminPOIApplications({
      keyword: keyword.value || undefined,
      status: status.value ?? undefined,
      page: nextPage,
      size: 20
    })
    const records = data?.records || []
    applications.value = reset ? records : [...applications.value, ...records]
    page.value = data?.page || nextPage
    hasMore.value = Boolean(data?.hasNext)
  } catch {
    ElMessage.error('加载失败')
  } finally {
    loadingRef.value = false
  }
}

const loadMore = () => loadData(false)

const handleSearch = () => loadData(true)
const resetFilters = () => {
  keyword.value = ''
  status.value = null
  loadData(true)
}

const handleReview = (row, newStatus) => {
  reviewTarget.value = row
  pendingReviewStatus.value = newStatus
  reviewNote.value = ''
  reviewDialogVisible.value = true
}

const viewDetail = (row) => {
  reviewTarget.value = row
  pendingReviewStatus.value = null
  reviewNote.value = ''
  reviewDialogVisible.value = true
}

const cancelReview = () => {
  reviewDialogVisible.value = false
}

const confirmReview = async () => {
  if (!reviewTarget.value) return
  reviewSubmitting.value = true
  try {
    await reviewPOIApplication(reviewTarget.value.id, {
      status: pendingReviewStatus.value,
      reviewNote: reviewNote.value || undefined
    })
    ElMessage.success(pendingReviewStatus.value === 2 ? '已通过审核，POI 已自动创建' : '已驳回申请')
    reviewDialogVisible.value = false
    loadData(true)
  } catch (err) {
    ElMessage.error(err?.message || '操作失败')
  } finally {
    reviewSubmitting.value = false
  }
}

onMounted(() => loadData(true))
</script>

<style scoped>
.application-page {
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px 20px;
}

.page-hero {
  margin-bottom: 24px;
}

.hero-kicker {
  font-size: 12px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.08em;
  color: var(--front-accent);
}

.page-hero h1 {
  font-size: 22px;
  font-weight: 700;
  color: var(--front-text);
  margin: 6px 0 4px;
}

.page-hero p {
  font-size: 13px;
  color: var(--front-text-muted);
}

.filter-panel {
  margin-bottom: 20px;
}

.filter-main {
  display: flex;
  gap: 10px;
  align-items: center;
  flex-wrap: wrap;
}

.filter-input {
  width: 280px;
}

.filter-select {
  width: 140px;
}

.table-panel {
  background: #fff;
  border-radius: 16px;
  padding: 20px;
  box-shadow: var(--front-shadow-soft);
}

.panel-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.panel-kicker {
  font-size: 11px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.08em;
  color: var(--front-text-muted);
}

.panel-head h2 {
  font-size: 16px;
  font-weight: 700;
  color: var(--front-text);
  margin-top: 2px;
}

.applicant-meta {
  display: flex;
  align-items: center;
  gap: 8px;
}

.applicant-meta .el-avatar {
  background: linear-gradient(135deg, var(--front-accent), var(--front-accent-strong));
  color: #fff;
  font-weight: 700;
  font-size: 11px;
  flex-shrink: 0;
}

.coord-text {
  font-size: 12px;
  color: var(--front-text-muted);
  font-family: monospace;
}

.load-more {
  text-align: center;
  padding: 12px;
}

.review-form {
  margin-top: 16px;
}

.review-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 12px;
}

@media (max-width: 640px) {
  .application-page {
    padding: 16px 12px;
  }

  .filter-input,
  .filter-select {
    width: 100%;
  }

  .filter-main {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
