<template>
  <div class="share-page">
    <section class="page-hero">
      <div class="hero-copy">
        <span class="hero-kicker">分享管理</span>
        <h1>集中治理校园打卡分享、图片内容与互动数据</h1>
        <p>支持按作者、POI 和发布时间筛选分享，查看详情、图片和回复，并能和回复管理页联动处理异常内容。</p>
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
          placeholder="搜索作者、POI 或分享内容"
          class="filter-input"
          @keyup.enter="handleSearch"
          @clear="handleSearch"
        >
          <template #append>
            <el-button :icon="Search" @click="handleSearch" />
          </template>
        </el-input>

        <el-select
          v-model="poiId"
          clearable
          filterable
          placeholder="筛选 POI"
          class="filter-select"
          @change="handleSearch"
        >
          <el-option
            v-for="poi in poiOptions"
            :key="poi.id"
            :label="poi.name"
            :value="poi.id"
          />
        </el-select>

        <el-date-picker
          v-model="timeRange"
          type="datetimerange"
          class="filter-date"
          start-placeholder="开始时间"
          end-placeholder="结束时间"
          range-separator="至"
          value-format="YYYY-MM-DDTHH:mm:ss"
        />

        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button :icon="RefreshRight" @click="resetFilters">重置</el-button>
      </div>

      <div class="filter-meta">
        <div class="preset-group">
          <button
            v-for="preset in timePresets"
            :key="preset.key"
            type="button"
            class="preset-chip"
            @click="applyTimePreset(preset.key)"
          >
            {{ preset.label }}
          </button>
        </div>

        <div v-if="activeFilters.length" class="active-filter-list">
          <span class="filter-label">当前筛选</span>
          <button
            v-for="item in activeFilters"
            :key="item.key"
            type="button"
            class="active-filter-chip"
            @click="removeFilter(item.key)"
          >
            {{ item.label }}
          </button>
        </div>
      </div>
    </section>

    <section class="table-panel">
      <div class="panel-head">
        <div>
          <span class="panel-kicker">内容列表</span>
          <h2>分享清单</h2>
        </div>
        <el-button text @click="loadShares">刷新列表</el-button>
      </div>

      <el-table :data="shares" v-loading="loading" stripe>
        <template #empty>
          <el-empty description="当前筛选条件下暂无分享">
            <el-button type="primary" plain @click="resetFilters">清空筛选</el-button>
          </el-empty>
        </template>

        <el-table-column prop="id" label="分享ID" width="96" />

        <el-table-column label="作者" min-width="180">
          <template #default="{ row }">
            <div class="author-cell">
              <el-avatar :size="40" :src="resolveAssetUrl(row.authorAvatarUrl) || undefined" class="author-avatar">
                {{ getNameInitial(row.authorDisplayName || row.authorUsername) }}
              </el-avatar>
              <div class="author-meta">
                <strong>{{ row.authorDisplayName || row.authorUsername }}</strong>
                <span>@{{ row.authorUsername }}</span>
              </div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="POI" min-width="170" show-overflow-tooltip>
          <template #default="{ row }">
            <button type="button" class="poi-card" @click="focusPoi(row.poiId)">
              <strong>{{ row.poiName }}</strong>
              <span>ID: {{ row.poiId }}</span>
            </button>
          </template>
        </el-table-column>

        <el-table-column prop="contentPreview" label="内容摘要" min-width="280" show-overflow-tooltip>
          <template #default="{ row }">
            {{ row.contentPreview || '仅图片分享' }}
          </template>
        </el-table-column>

        <el-table-column label="互动" width="170">
          <template #default="{ row }">
            <div class="metric-pills">
              <span>{{ row.imageCount }} 图</span>
              <span>{{ row.likeCount }} 赞</span>
              <span>{{ row.replyCount }} 回复</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="发布时间" width="180">
          <template #default="{ row }">
            {{ formatDate(row.createdAt) }}
          </template>
        </el-table-column>

        <el-table-column label="操作" width="230" fixed="right">
          <template #default="{ row }">
            <div class="action-group">
              <el-button size="small" @click="openDetail(row)">查看</el-button>
              <el-button size="small" plain @click="goReplies(row.id)">回复管理</el-button>
              <el-button
                size="small"
                type="danger"
                :loading="shareDeletingId === row.id"
                @click="handleDeleteShare(row)"
              >
                删除
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-bar">
        <span>共 {{ total }} 条分享</span>
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50]"
          :total="total"
          layout="sizes, prev, pager, next"
          @current-change="loadShares"
          @size-change="handlePageSizeChange"
        />
      </div>
    </section>

    <el-drawer v-model="drawerVisible" size="620px" :with-header="false" destroy-on-close>
      <div class="drawer-body" v-loading="detailLoading">
        <template v-if="selectedShare">
          <div class="drawer-hero">
            <div class="drawer-profile">
              <el-avatar :size="70" :src="resolveAssetUrl(selectedShare.authorAvatarUrl) || undefined" class="author-avatar">
                {{ getNameInitial(selectedShare.authorDisplayName || selectedShare.authorUsername) }}
              </el-avatar>
              <div>
                <h2>{{ selectedShare.authorDisplayName || selectedShare.authorUsername }}</h2>
                <p>@{{ selectedShare.authorUsername }}</p>
              </div>
            </div>

            <div class="drawer-actions">
              <el-button plain @click="goReplies(selectedShare.id)">查看全部回复</el-button>
              <el-button
                type="danger"
                plain
                :loading="shareDeletingId === selectedShare.id"
                @click="handleDeleteShare(selectedShare)"
              >
                删除该分享
              </el-button>
            </div>
          </div>

          <div class="drawer-tags">
            <el-tag effect="plain" type="info">{{ selectedShare.poiName }}</el-tag>
            <el-tag effect="plain">{{ selectedShare.poiCategory }}</el-tag>
            <el-tag effect="plain" type="success">{{ selectedShare.imageCount || 0 }} 张图片</el-tag>
          </div>

          <div class="stats-grid">
            <div class="stat-card">
              <span>点赞数</span>
              <strong>{{ selectedShare.likeCount || 0 }}</strong>
            </div>
            <div class="stat-card">
              <span>回复数</span>
              <strong>{{ selectedShare.replyCount || 0 }}</strong>
            </div>
            <div class="stat-card">
              <span>发布时间</span>
              <strong>{{ formatShortDate(selectedShare.createdAt) }}</strong>
            </div>
          </div>

          <div class="drawer-section">
            <div class="section-head">
              <h3>分享内容</h3>
              <button type="button" class="anchor-link" @click="copyShareId(selectedShare.id)">
                复制分享ID
              </button>
            </div>
            <div class="content-card">
              {{ selectedShare.content || '该分享未填写文字，仅包含图片。' }}
            </div>
          </div>

          <div class="drawer-section" v-if="selectedShare.imageUrls?.length">
            <h3>分享图片</h3>
            <div class="image-grid">
              <el-image
                v-for="(imageUrl, index) in selectedShare.imageUrls"
                :key="imageUrl"
                :src="resolveAssetUrl(imageUrl)"
                :preview-src-list="selectedImageList"
                :initial-index="index"
                fit="cover"
                class="share-image"
              />
            </div>
          </div>

          <div class="drawer-section">
            <h3>基础信息</h3>
            <div class="info-grid">
              <div class="info-item">
                <span>分享ID</span>
                <strong>{{ selectedShare.id }}</strong>
              </div>
              <div class="info-item">
                <span>POI ID</span>
                <strong>{{ selectedShare.poiId }}</strong>
              </div>
              <div class="info-item">
                <span>发布时间</span>
                <strong>{{ formatDate(selectedShare.createdAt) }}</strong>
              </div>
              <div class="info-item">
                <span>更新时间</span>
                <strong>{{ formatDate(selectedShare.updatedAt) }}</strong>
              </div>
            </div>
          </div>

          <div class="drawer-section">
            <div class="section-head">
              <h3>关联回复</h3>
              <button type="button" class="anchor-link" @click="goReplies(selectedShare.id)">
                在回复管理页处理
              </button>
            </div>

            <div v-if="selectedShare.replies?.length" class="reply-list">
              <article v-for="reply in selectedShare.replies" :key="reply.id" class="reply-card">
                <div class="reply-top">
                  <div class="reply-author">
                    <el-avatar :size="34" :src="resolveAssetUrl(reply.authorAvatarUrl) || undefined" class="reply-avatar">
                      {{ getNameInitial(reply.authorDisplayName || reply.authorUsername) }}
                    </el-avatar>
                    <div>
                      <strong>{{ reply.authorDisplayName || reply.authorUsername }}</strong>
                      <p>@{{ reply.authorUsername }}</p>
                    </div>
                  </div>
                  <span>{{ formatDate(reply.createdAt) }}</span>
                </div>

                <p class="reply-content">{{ reply.content }}</p>

                <div class="reply-actions">
                  <el-button size="small" plain @click="goReplies(selectedShare.id)">去回复管理</el-button>
                  <el-button
                    size="small"
                    type="danger"
                    text
                    :loading="replyDeletingId === reply.id"
                    @click="handleDeleteReply(reply)"
                  >
                    删除回复
                  </el-button>
                </div>
              </article>
            </div>

            <el-empty v-else description="该分享下暂无回复" />
          </div>
        </template>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { RefreshRight, Search } from '@element-plus/icons-vue'
import { getAllPOIs } from '@/api/poi'
import {
  deleteAdminReply,
  deleteAdminShare,
  getAdminShareDetail,
  getAdminSharePage
} from '@/api/adminContent'
import { API_ORIGIN } from '@/utils/request'

const router = useRouter()
const route = useRoute()

const keyword = ref('')
const poiId = ref()
const timeRange = ref([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const shares = ref([])
const poiOptions = ref([])
const loading = ref(false)
const drawerVisible = ref(false)
const detailLoading = ref(false)
const selectedShare = ref(null)
const shareDeletingId = ref(null)
const replyDeletingId = ref(null)

const timePresets = [
  { key: '24h', label: '最近 24 小时' },
  { key: '7d', label: '最近 7 天' },
  { key: '30d', label: '最近 30 天' }
]

const selectedImageList = computed(() => {
  return (selectedShare.value?.imageUrls || []).map((item) => resolveAssetUrl(item))
})

const activeFilters = computed(() => {
  const items = []

  if (keyword.value.trim()) {
    items.push({ key: 'keyword', label: `关键词: ${keyword.value.trim()}` })
  }

  if (poiId.value) {
    const poi = poiOptions.value.find((item) => item.id === poiId.value)
    items.push({ key: 'poiId', label: `POI: ${poi?.name || poiId.value}` })
  }

  if (timeRange.value?.length === 2) {
    items.push({ key: 'timeRange', label: `时间: ${formatShortDate(timeRange.value[0])} - ${formatShortDate(timeRange.value[1])}` })
  }

  return items
})

const heroStats = computed(() => {
  const pageLikeCount = shares.value.reduce((sum, item) => sum + (item.likeCount || 0), 0)
  const pageReplyCount = shares.value.reduce((sum, item) => sum + (item.replyCount || 0), 0)

  return [
    { label: '当前总量', value: `${total.value}`, helper: '符合当前筛选的分享数' },
    { label: '本页点赞', value: `${pageLikeCount}`, helper: '当前页聚合互动热度' },
    { label: '本页回复', value: `${pageReplyCount}`, helper: '当前页累计回复量' },
    { label: '筛选条件', value: `${activeFilters.value.length}`, helper: activeFilters.value.length ? '可点击下方条件单独移除' : '当前为全量浏览' }
  ]
})

const loadPois = async () => {
  try {
    poiOptions.value = await getAllPOIs()
  } catch (error) {
    ElMessage.error(error.message || '加载 POI 列表失败')
  }
}

const loadShares = async () => {
  loading.value = true
  try {
    const [startTime, endTime] = timeRange.value || []
    const data = await getAdminSharePage({
      keyword: keyword.value.trim() || undefined,
      poiId: poiId.value,
      startTime: startTime || undefined,
      endTime: endTime || undefined,
      page: currentPage.value - 1,
      size: pageSize.value
    })
    shares.value = data.records || []
    total.value = data.total || 0
    currentPage.value = (data.page || 0) + 1
  } catch (error) {
    ElMessage.error(error.message || '加载分享列表失败')
  } finally {
    loading.value = false
  }
}

const openDetailById = async (shareId) => {
  if (!shareId) {
    return
  }

  drawerVisible.value = true
  detailLoading.value = true
  try {
    selectedShare.value = await getAdminShareDetail(shareId)
  } catch (error) {
    drawerVisible.value = false
    selectedShare.value = null
    ElMessage.error(error.message || '加载分享详情失败')
  } finally {
    detailLoading.value = false
  }
}

const handleSearch = async () => {
  currentPage.value = 1
  syncQuery()
  await loadShares()
}

const handlePageSizeChange = async () => {
  currentPage.value = 1
  await loadShares()
}

const resetFilters = async () => {
  keyword.value = ''
  poiId.value = undefined
  timeRange.value = []
  currentPage.value = 1
  syncQuery()
  await loadShares()
}

const openDetail = async (row) => {
  syncQuery({ openShareId: row.id })
  await openDetailById(row.id)
}

const refreshDetailIfNeeded = async (shareId) => {
  if (!drawerVisible.value || selectedShare.value?.id !== shareId) {
    return
  }
  selectedShare.value = await getAdminShareDetail(shareId)
}

const handleDeleteShare = async (row) => {
  try {
    await ElMessageBox.confirm('删除分享后，其图片、点赞和回复将一并删除。是否继续？', '删除分享', {
      type: 'warning',
      confirmButtonText: '确认删除',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }

  shareDeletingId.value = row.id
  try {
    await deleteAdminShare(row.id)
    if (shares.value.length === 1 && currentPage.value > 1) {
      currentPage.value -= 1
    }
    if (selectedShare.value?.id === row.id) {
      drawerVisible.value = false
      selectedShare.value = null
      syncQuery({ openShareId: undefined })
    }
    await loadShares()
    ElMessage.success('分享已删除')
  } catch (error) {
    ElMessage.error(error.message || '删除分享失败')
  } finally {
    shareDeletingId.value = null
  }
}

const handleDeleteReply = async (reply) => {
  try {
    await ElMessageBox.confirm('确定删除这条回复吗？', '删除回复', {
      type: 'warning',
      confirmButtonText: '确认删除',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }

  replyDeletingId.value = reply.id
  try {
    await deleteAdminReply(reply.id)
    if (selectedShare.value?.id) {
      await refreshDetailIfNeeded(selectedShare.value.id)
    }
    await loadShares()
    ElMessage.success('回复已删除')
  } catch (error) {
    ElMessage.error(error.message || '删除回复失败')
  } finally {
    replyDeletingId.value = null
  }
}

const goReplies = (shareIdValue) => {
  router.push({
    path: '/admin/replies',
    query: shareIdValue ? { shareId: String(shareIdValue) } : {}
  })
}

const focusPoi = async (poiIdValue) => {
  poiId.value = poiIdValue
  await handleSearch()
}

const copyShareId = async (shareIdValue) => {
  try {
    await navigator.clipboard.writeText(String(shareIdValue))
    ElMessage.success('分享ID已复制')
  } catch {
    ElMessage.warning(`分享ID：${shareIdValue}`)
  }
}

const applyTimePreset = async (presetKey) => {
  timeRange.value = buildPresetRange(presetKey)
  await handleSearch()
}

const removeFilter = async (key) => {
  if (key === 'keyword') {
    keyword.value = ''
  }
  if (key === 'poiId') {
    poiId.value = undefined
  }
  if (key === 'timeRange') {
    timeRange.value = []
  }
  await handleSearch()
}

const buildPresetRange = (presetKey) => {
  const end = new Date()
  const start = new Date(end)

  if (presetKey === '24h') {
    start.setHours(start.getHours() - 24)
  } else if (presetKey === '7d') {
    start.setDate(start.getDate() - 7)
  } else if (presetKey === '30d') {
    start.setDate(start.getDate() - 30)
  }

  return [formatDateTimeValue(start), formatDateTimeValue(end)]
}

const applyRouteQuery = () => {
  keyword.value = typeof route.query.keyword === 'string' ? route.query.keyword : ''
  poiId.value = route.query.poiId ? Number(route.query.poiId) : undefined

  if (typeof route.query.startTime === 'string' && typeof route.query.endTime === 'string') {
    timeRange.value = [route.query.startTime, route.query.endTime]
  } else {
    timeRange.value = []
  }
}

const syncQuery = (extra = {}) => {
  const [startTime, endTime] = timeRange.value || []

  const nextQuery = {
    keyword: keyword.value.trim() || undefined,
    poiId: poiId.value ? String(poiId.value) : undefined,
    startTime: startTime || undefined,
    endTime: endTime || undefined,
    openShareId: drawerVisible.value && selectedShare.value?.id ? String(selectedShare.value.id) : undefined,
    ...extra
  }

  Object.keys(nextQuery).forEach((key) => {
    if (nextQuery[key] == null || nextQuery[key] === '') {
      delete nextQuery[key]
    }
  })

  router.replace({ query: nextQuery })
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

const getNameInitial = (value) => (value || 'U').slice(0, 1).toUpperCase()

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

const formatShortDate = (value) => {
  if (!value) {
    return '--'
  }

  return new Intl.DateTimeFormat('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  }).format(new Date(value))
}

const formatDateTimeValue = (date) => {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  const seconds = String(date.getSeconds()).padStart(2, '0')
  return `${year}-${month}-${day}T${hours}:${minutes}:${seconds}`
}

watch(drawerVisible, (visible) => {
  if (!visible) {
    selectedShare.value = null
    syncQuery({ openShareId: undefined })
  }
})

onMounted(async () => {
  applyRouteQuery()
  await Promise.all([loadPois(), loadShares()])

  if (route.query.openShareId) {
    await openDetailById(Number(route.query.openShareId))
  }
})
</script>

<style scoped>
.share-page {
  --panel-bg: rgba(255, 255, 255, 0.9);
  --panel-border: rgba(148, 163, 184, 0.18);
  --ink: #0f172a;
  --muted: #64748b;
  --accent: #2563eb;
  --accent-soft: rgba(37, 99, 235, 0.08);
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
    radial-gradient(circle at top right, rgba(14, 165, 233, 0.18), transparent 26%),
    linear-gradient(135deg, rgba(255, 255, 255, 0.98), rgba(241, 247, 255, 0.94));
}

.hero-kicker,
.panel-kicker {
  display: inline-flex;
  align-items: center;
  padding: 6px 12px;
  border-radius: 999px;
  background: var(--accent-soft);
  color: var(--accent);
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
  border: 1px solid rgba(191, 219, 254, 0.8);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.94), rgba(244, 248, 255, 0.92));
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
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.filter-main {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.filter-input {
  width: 320px;
}

.filter-select {
  width: 220px;
}

.filter-date {
  width: 380px;
}

.filter-meta,
.preset-group,
.active-filter-list {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.filter-meta {
  justify-content: space-between;
}

.preset-chip,
.active-filter-chip,
.poi-card,
.anchor-link {
  border: none;
  background: none;
  cursor: pointer;
}

.preset-chip,
.active-filter-chip {
  padding: 8px 12px;
  border-radius: 999px;
  font-size: 12px;
  transition: all 0.2s ease;
}

.preset-chip {
  background: rgba(241, 245, 249, 0.95);
  color: #475569;
}

.preset-chip:hover {
  background: rgba(37, 99, 235, 0.1);
  color: var(--accent);
}

.filter-label {
  color: var(--muted);
  font-size: 12px;
}

.active-filter-chip {
  background: rgba(219, 234, 254, 0.95);
  color: var(--accent);
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

.author-cell,
.reply-author {
  display: flex;
  align-items: center;
  gap: 12px;
}

.author-avatar,
.reply-avatar {
  background: linear-gradient(135deg, #38bdf8, #2563eb);
  color: #fff;
  font-weight: 700;
}

.author-meta,
.poi-meta {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.author-meta strong,
.poi-card strong,
.reply-author strong {
  color: var(--ink);
}

.author-meta span,
.poi-card span,
.reply-author p {
  color: var(--muted);
  font-size: 12px;
}

.reply-author p {
  margin: 6px 0 0;
}

.poi-card {
  padding: 0;
  text-align: left;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.poi-card strong {
  transition: color 0.2s ease;
}

.poi-card:hover strong {
  color: var(--accent);
}

.metric-pills {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.metric-pills span {
  padding: 5px 10px;
  border-radius: 999px;
  background: rgba(241, 245, 249, 0.95);
  color: #475569;
  font-size: 12px;
}

.action-group {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
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

.drawer-hero,
.section-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
}

.drawer-profile {
  display: flex;
  align-items: center;
  gap: 16px;
}

.drawer-profile h2 {
  margin: 0;
  color: var(--ink);
  font-size: 30px;
}

.drawer-profile p {
  margin: 6px 0 0;
  color: var(--muted);
}

.drawer-actions,
.drawer-tags {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.stats-grid,
.info-grid,
.image-grid {
  display: grid;
  gap: 12px;
}

.stats-grid {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.info-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.image-grid {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.stat-card,
.info-item,
.content-card,
.reply-card {
  border-radius: 18px;
  padding: 16px;
  background: linear-gradient(180deg, rgba(247, 250, 255, 0.96), rgba(255, 255, 255, 0.96));
  border: 1px solid rgba(226, 232, 240, 0.9);
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

.anchor-link {
  color: var(--accent);
  font-size: 13px;
  padding: 0;
}

.stat-card span,
.info-item span {
  display: block;
  color: var(--muted);
  font-size: 13px;
}

.stat-card strong,
.info-item strong {
  display: block;
  margin-top: 10px;
  color: var(--ink);
}

.stat-card strong {
  font-size: 30px;
  line-height: 1;
}

.content-card,
.reply-content {
  color: #334155;
  line-height: 1.8;
  white-space: pre-wrap;
}

.share-image {
  width: 100%;
  height: 144px;
  border-radius: 16px;
  overflow: hidden;
}

.reply-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.reply-top {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  color: var(--muted);
  font-size: 12px;
}

.reply-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

@media (max-width: 1240px) {
  .page-hero {
    grid-template-columns: 1fr;
  }

  .filter-input,
  .filter-select,
  .filter-date {
    width: 100%;
  }

  .filter-meta {
    align-items: flex-start;
    flex-direction: column;
  }
}

@media (max-width: 760px) {
  .hero-stats,
  .stats-grid,
  .info-grid,
  .image-grid {
    grid-template-columns: 1fr;
  }

  .pagination-bar,
  .drawer-hero,
  .section-head,
  .reply-top,
  .reply-actions {
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
