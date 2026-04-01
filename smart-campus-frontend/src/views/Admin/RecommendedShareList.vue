<template>
  <div class="recommend-page">
    <section class="page-hero">
      <div class="hero-copy">
        <span class="hero-kicker">推荐内容</span>
        <h1>把优质打卡分享直接运营到首页展示位</h1>
        <p>后台可从现有分享中挑选推荐内容，设置排序和推荐语，让首页形成稳定的精选内容区。</p>
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
          placeholder="搜索推荐语、作者、地点或分享内容"
          class="filter-input"
          @keyup.enter="handleSearch"
          @clear="handleSearch"
        >
          <template #append>
            <el-button :icon="Search" @click="handleSearch" />
          </template>
        </el-input>

        <el-button type="primary" :icon="Plus" @click="openCreateDialog">添加推荐</el-button>
        <el-button :icon="RefreshRight" @click="resetFilters">重置</el-button>
      </div>
    </section>

    <section class="table-panel">
      <div class="panel-head">
        <div>
          <span class="panel-kicker">推荐列表</span>
          <h2>当前推荐分享</h2>
        </div>
        <el-button text @click="loadRecommendations">刷新列表</el-button>
      </div>

      <el-table :data="recommendations" v-loading="loading" stripe>
        <template #empty>
          <el-empty description="当前还没有推荐分享">
            <el-button type="primary" plain @click="openCreateDialog">立即添加推荐</el-button>
          </el-empty>
        </template>

        <el-table-column prop="sortOrder" label="排序" width="90" />

        <el-table-column label="推荐分享" min-width="360">
          <template #default="{ row }">
            <div class="share-cell">
              <el-image
                v-if="row.coverImageUrl"
                :src="resolveAssetUrl(row.coverImageUrl)"
                fit="cover"
                class="cover-thumb"
              />
              <div class="share-meta">
                <strong>{{ row.contentPreview }}</strong>
                <p>{{ row.poiName }} · {{ row.authorDisplayName || row.authorUsername }}</p>
              </div>
            </div>
          </template>
        </el-table-column>

        <el-table-column prop="recommendationText" label="推荐语" min-width="240" show-overflow-tooltip>
          <template #default="{ row }">
            {{ row.recommendationText || '未填写推荐语' }}
          </template>
        </el-table-column>

        <el-table-column label="互动" width="160">
          <template #default="{ row }">
            <div class="metric-pills">
              <span>{{ row.imageCount }} 图</span>
              <span>{{ row.likeCount }} 赞</span>
              <span>{{ row.replyCount }} 回复</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <div class="action-group">
              <el-button size="small" @click="viewShare(row.shareId)">查看分享</el-button>
              <el-button size="small" plain @click="openEditDialog(row)">编辑</el-button>
              <el-button
                size="small"
                type="danger"
                :loading="deletingId === row.id"
                @click="handleDelete(row)"
              >
                取消推荐
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-bar">
        <span>共 {{ total }} 条推荐内容</span>
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50]"
          :total="total"
          layout="sizes, prev, pager, next"
          @current-change="loadRecommendations"
          @size-change="handlePageSizeChange"
        />
      </div>
    </section>

    <el-dialog
      v-model="createDialogVisible"
      title="添加推荐分享"
      width="1080px"
      destroy-on-close
      @closed="resetCreateDialog"
    >
      <div class="candidate-toolbar">
        <el-input
          v-model="candidateKeyword"
          clearable
          placeholder="搜索作者、地点或分享内容"
          class="candidate-input"
          @keyup.enter="loadCandidates"
          @clear="loadCandidates"
        >
          <template #append>
            <el-button :icon="Search" @click="loadCandidates" />
          </template>
        </el-input>

        <el-select
          v-model="candidatePoiId"
          clearable
          filterable
          class="candidate-select"
          placeholder="筛选所属地点"
          @change="loadCandidates"
        >
          <el-option v-for="poi in poiOptions" :key="poi.id" :label="poi.name" :value="poi.id" />
        </el-select>
      </div>

      <div class="candidate-layout">
        <div class="candidate-table-wrap">
          <el-table
            :data="candidates"
            v-loading="candidateLoading"
            highlight-current-row
            @current-change="handleCandidateCurrentChange"
          >
            <template #empty>
              <el-empty description="当前没有可加入推荐的分享" />
            </template>

            <el-table-column width="56">
              <template #default="{ row }">
                <el-radio :value="row.shareId" :model-value="selectedCandidate?.shareId" @change="selectCandidate(row)">
                  &nbsp;
                </el-radio>
              </template>
            </el-table-column>

            <el-table-column label="候选分享" min-width="320">
              <template #default="{ row }">
                <div class="share-cell">
                  <el-image
                    v-if="row.coverImageUrl"
                    :src="resolveAssetUrl(row.coverImageUrl)"
                    fit="cover"
                    class="cover-thumb"
                  />
                  <div class="share-meta">
                    <strong>{{ row.contentPreview }}</strong>
                    <p>{{ row.poiName }} · {{ row.authorDisplayName || row.authorUsername }}</p>
                  </div>
                </div>
              </template>
            </el-table-column>

            <el-table-column label="互动" width="160">
              <template #default="{ row }">
                <div class="metric-pills">
                  <span>{{ row.imageCount }} 图</span>
                  <span>{{ row.likeCount }} 赞</span>
                  <span>{{ row.replyCount }} 回复</span>
                </div>
              </template>
            </el-table-column>
          </el-table>

          <div class="pagination-bar compact">
            <span>共 {{ candidateTotal }} 条候选分享</span>
            <el-pagination
              v-model:current-page="candidatePage"
              v-model:page-size="candidatePageSize"
              :page-sizes="[5, 10, 20]"
              :total="candidateTotal"
              layout="sizes, prev, pager, next"
              @current-change="loadCandidates"
              @size-change="handleCandidatePageSizeChange"
            />
          </div>
        </div>

        <div class="candidate-form-wrap">
          <div class="form-head">
            <h3>推荐设置</h3>
            <p v-if="selectedCandidate">当前已选择分享 #{{ selectedCandidate.shareId }}</p>
            <p v-else>请先从左侧候选列表中选择一条分享</p>
          </div>

          <el-form label-width="88px">
            <el-form-item label="推荐排序">
              <el-input-number v-model="createForm.sortOrder" :min="1" :max="999" controls-position="right" />
            </el-form-item>

            <el-form-item label="推荐语">
              <el-input
                v-model="createForm.recommendationText"
                type="textarea"
                :rows="5"
                maxlength="100"
                show-word-limit
                resize="none"
                placeholder="可填写一小段运营文案，展示在首页推荐卡片中"
              />
            </el-form-item>
          </el-form>

          <div class="selected-preview" v-if="selectedCandidate">
            <strong>{{ selectedCandidate.poiName }}</strong>
            <p>{{ selectedCandidate.contentPreview }}</p>
          </div>
        </div>
      </div>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="createDialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="saving" @click="handleCreate">加入推荐</el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog
      v-model="editDialogVisible"
      title="编辑推荐内容"
      width="560px"
      destroy-on-close
      @closed="resetEditDialog"
    >
      <template v-if="editingItem">
        <div class="selected-preview">
          <strong>{{ editingItem.poiName }} · {{ editingItem.authorDisplayName || editingItem.authorUsername }}</strong>
          <p>{{ editingItem.contentPreview }}</p>
        </div>

        <el-form label-width="88px">
          <el-form-item label="推荐排序">
            <el-input-number v-model="editForm.sortOrder" :min="1" :max="999" controls-position="right" />
          </el-form-item>

          <el-form-item label="推荐语">
            <el-input
              v-model="editForm.recommendationText"
              type="textarea"
              :rows="5"
              maxlength="100"
              show-word-limit
              resize="none"
              placeholder="可填写一小段运营文案，展示在首页推荐卡片中"
            />
          </el-form-item>
        </el-form>
      </template>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="editDialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="saving" @click="handleUpdate">保存修改</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, RefreshRight, Search } from '@element-plus/icons-vue'
import { getAllPOIs } from '@/api/poi'
import {
  createAdminRecommendedShare,
  deleteAdminRecommendedShare,
  getAdminRecommendedShareCandidatePage,
  getAdminRecommendedSharePage,
  updateAdminRecommendedShare
} from '@/api/adminRecommendedShare'
import { API_ORIGIN } from '@/utils/request'

const router = useRouter()

const keyword = ref('')
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const recommendations = ref([])
const loading = ref(false)
const deletingId = ref(null)

const poiOptions = ref([])

const createDialogVisible = ref(false)
const candidateKeyword = ref('')
const candidatePoiId = ref()
const candidates = ref([])
const candidateLoading = ref(false)
const candidatePage = ref(1)
const candidatePageSize = ref(5)
const candidateTotal = ref(0)
const selectedCandidate = ref(null)

const editDialogVisible = ref(false)
const editingItem = ref(null)
const saving = ref(false)

const createForm = reactive({
  sortOrder: 1,
  recommendationText: ''
})

const editForm = reactive({
  sortOrder: 1,
  recommendationText: ''
})

const heroStats = computed(() => {
  const withRecommendationText = recommendations.value.filter((item) => item.recommendationText).length
  const totalLikes = recommendations.value.reduce((sum, item) => sum + (item.likeCount || 0), 0)
  const totalReplies = recommendations.value.reduce((sum, item) => sum + (item.replyCount || 0), 0)

  return [
    { label: '当前推荐数', value: `${total.value}`, helper: '已进入首页推荐位的分享数量' },
    { label: '已写推荐语', value: `${withRecommendationText}`, helper: '带运营文案的推荐内容数量' },
    { label: '累计点赞', value: `${totalLikes}`, helper: '当前推荐列表的点赞总量' },
    { label: '累计回复', value: `${totalReplies}`, helper: '当前推荐列表的回复总量' }
  ]
})

const loadPois = async () => {
  try {
    poiOptions.value = await getAllPOIs()
  } catch (error) {
    ElMessage.error(error.message || '加载地点列表失败')
  }
}

const loadRecommendations = async () => {
  loading.value = true
  try {
    const data = await getAdminRecommendedSharePage({
      keyword: keyword.value.trim() || undefined,
      page: currentPage.value - 1,
      size: pageSize.value
    })
    recommendations.value = data.records || []
    total.value = data.total || 0
    currentPage.value = (data.page || 0) + 1
  } catch (error) {
    ElMessage.error(error.message || '加载推荐内容失败')
  } finally {
    loading.value = false
  }
}

const loadCandidates = async () => {
  candidateLoading.value = true
  try {
    const data = await getAdminRecommendedShareCandidatePage({
      keyword: candidateKeyword.value.trim() || undefined,
      poiId: candidatePoiId.value,
      recommended: false,
      page: candidatePage.value - 1,
      size: candidatePageSize.value
    })
    candidates.value = data.records || []
    candidateTotal.value = data.total || 0
    candidatePage.value = (data.page || 0) + 1
    if (selectedCandidate.value) {
      const matched = candidates.value.find((item) => item.shareId === selectedCandidate.value.shareId)
      if (!matched) {
        selectedCandidate.value = null
      }
    }
  } catch (error) {
    ElMessage.error(error.message || '加载候选分享失败')
  } finally {
    candidateLoading.value = false
  }
}

const handleSearch = async () => {
  currentPage.value = 1
  await loadRecommendations()
}

const handlePageSizeChange = async () => {
  currentPage.value = 1
  await loadRecommendations()
}

const handleCandidatePageSizeChange = async () => {
  candidatePage.value = 1
  await loadCandidates()
}

const resetFilters = async () => {
  keyword.value = ''
  currentPage.value = 1
  await loadRecommendations()
}

const openCreateDialog = async () => {
  createDialogVisible.value = true
  await loadCandidates()
}

const resetCreateDialog = () => {
  candidateKeyword.value = ''
  candidatePoiId.value = undefined
  candidates.value = []
  candidatePage.value = 1
  candidatePageSize.value = 5
  candidateTotal.value = 0
  selectedCandidate.value = null
  createForm.sortOrder = 1
  createForm.recommendationText = ''
}

const selectCandidate = (row) => {
  selectedCandidate.value = row
}

const handleCandidateCurrentChange = (row) => {
  if (row) {
    selectedCandidate.value = row
  }
}

const handleCreate = async () => {
  if (!selectedCandidate.value) {
    ElMessage.warning('请先选择一条候选分享')
    return
  }

  saving.value = true
  try {
    await createAdminRecommendedShare({
      shareId: selectedCandidate.value.shareId,
      sortOrder: createForm.sortOrder,
      recommendationText: createForm.recommendationText.trim() || null
    })
    createDialogVisible.value = false
    await loadRecommendations()
    ElMessage.success('推荐内容已添加')
  } catch (error) {
    ElMessage.error(error.message || '添加推荐内容失败')
  } finally {
    saving.value = false
  }
}

const openEditDialog = (row) => {
  editingItem.value = row
  editForm.sortOrder = row.sortOrder
  editForm.recommendationText = row.recommendationText || ''
  editDialogVisible.value = true
}

const resetEditDialog = () => {
  editingItem.value = null
  editForm.sortOrder = 1
  editForm.recommendationText = ''
}

const handleUpdate = async () => {
  if (!editingItem.value) {
    return
  }

  saving.value = true
  try {
    await updateAdminRecommendedShare(editingItem.value.id, {
      sortOrder: editForm.sortOrder,
      recommendationText: editForm.recommendationText.trim() || null
    })
    editDialogVisible.value = false
    await loadRecommendations()
    ElMessage.success('推荐内容已更新')
  } catch (error) {
    ElMessage.error(error.message || '更新推荐内容失败')
  } finally {
    saving.value = false
  }
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm('取消推荐后，这条内容将不会继续展示在首页推荐区。是否继续？', '取消推荐', {
      type: 'warning',
      confirmButtonText: '确认取消',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }

  deletingId.value = row.id
  try {
    await deleteAdminRecommendedShare(row.id)
    if (recommendations.value.length === 1 && currentPage.value > 1) {
      currentPage.value -= 1
    }
    await loadRecommendations()
    ElMessage.success('已取消推荐')
  } catch (error) {
    ElMessage.error(error.message || '取消推荐失败')
  } finally {
    deletingId.value = null
  }
}

const viewShare = (shareId) => {
  router.push({
    path: '/admin/shares',
    query: { openShareId: String(shareId) }
  })
}

const resolveAssetUrl = (value) => {
  if (!value) {
    return ''
  }

  if (/^https?:\/\//i.test(value)) {
    return value
  }

  return `${API_ORIGIN}${value.startsWith('/') ? value : `/${value}`}`
}

onMounted(async () => {
  await Promise.all([loadPois(), loadRecommendations()])
})
</script>

<style scoped>
.recommend-page {
  --panel-bg: rgba(255, 255, 255, 0.9);
  --panel-border: rgba(148, 163, 184, 0.18);
  --ink: #0f172a;
  --muted: #64748b;
  --accent: #2563eb;
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
    radial-gradient(circle at top right, rgba(250, 204, 21, 0.16), transparent 26%),
    linear-gradient(135deg, rgba(255, 255, 255, 0.98), rgba(255, 251, 235, 0.94));
}

.hero-kicker,
.panel-kicker {
  display: inline-flex;
  align-items: center;
  padding: 6px 12px;
  border-radius: 999px;
  background: rgba(250, 204, 21, 0.14);
  color: #a16207;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
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
  max-width: 640px;
}

.hero-stats {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.hero-stat {
  padding: 18px;
  border-radius: 22px;
  border: 1px solid rgba(253, 224, 71, 0.48);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.95), rgba(254, 252, 232, 0.92));
}

.hero-stat span,
.hero-stat em {
  display: block;
  color: var(--muted);
  font-style: normal;
}

.hero-stat span {
  font-size: 12px;
}

.hero-stat strong {
  display: block;
  margin: 12px 0 10px;
  font-size: 30px;
  line-height: 1;
  color: var(--ink);
}

.hero-stat em {
  font-size: 12px;
  line-height: 1.5;
}

.filter-panel {
  border-radius: 26px;
  padding: 18px;
}

.filter-main,
.action-group,
.metric-pills,
.dialog-footer {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.filter-input {
  width: 340px;
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

.share-cell {
  display: flex;
  align-items: center;
  gap: 14px;
}

.cover-thumb {
  width: 84px;
  height: 60px;
  border-radius: 14px;
  overflow: hidden;
  flex-shrink: 0;
}

.share-meta {
  min-width: 0;
}

.share-meta strong {
  display: block;
  color: var(--ink);
}

.share-meta p {
  margin: 6px 0 0;
  color: var(--muted);
  line-height: 1.6;
}

.metric-pills span {
  padding: 5px 10px;
  border-radius: 999px;
  background: rgba(241, 245, 249, 0.95);
  color: #475569;
  font-size: 12px;
}

.pagination-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 18px 6px 8px;
  color: var(--muted);
}

.pagination-bar.compact {
  padding-left: 0;
  padding-right: 0;
}

.candidate-toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.candidate-input {
  flex: 1;
}

.candidate-select {
  width: 240px;
}

.candidate-layout {
  display: grid;
  grid-template-columns: minmax(0, 1.6fr) minmax(280px, 0.9fr);
  gap: 18px;
}

.candidate-table-wrap,
.candidate-form-wrap,
.selected-preview {
  border-radius: 20px;
  border: 1px solid rgba(226, 232, 240, 0.9);
  background: linear-gradient(180deg, rgba(247, 250, 255, 0.96), rgba(255, 255, 255, 0.96));
}

.candidate-table-wrap {
  padding: 14px;
}

.candidate-form-wrap {
  padding: 18px;
}

.form-head h3 {
  margin: 0;
  color: var(--ink);
  font-size: 18px;
}

.form-head p {
  margin: 8px 0 0;
  color: var(--muted);
  line-height: 1.7;
}

.selected-preview {
  margin-top: 18px;
  padding: 16px;
}

.selected-preview strong {
  display: block;
  color: var(--ink);
}

.selected-preview p {
  margin: 10px 0 0;
  color: var(--muted);
  line-height: 1.7;
}

@media (max-width: 1240px) {
  .page-hero,
  .candidate-layout {
    grid-template-columns: 1fr;
  }

  .filter-input,
  .candidate-select {
    width: 100%;
  }

  .candidate-toolbar {
    flex-direction: column;
    align-items: stretch;
  }
}

@media (max-width: 760px) {
  .hero-stats,
  .pagination-bar,
  .dialog-footer {
    flex-direction: column;
    align-items: stretch;
  }

  .page-hero,
  .filter-panel,
  .table-panel {
    border-radius: 22px;
  }
}
</style>
