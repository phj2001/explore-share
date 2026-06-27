<template>
  <div class="reply-page">
    <section class="page-hero">
      <div class="hero-copy">
        <span class="hero-kicker">回复管理</span>
        <h1>全局查看平台互动回复，快速定位并清理问题内容</h1>
        <p>支持按作者、所属分享和时间范围筛选回复，并可一键跳回分享详情，形成内容治理闭环。</p>
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
          placeholder="搜索回复内容、回复作者或所属分享内容"
          class="filter-input"
          @keyup.enter="handleSearch"
          @clear="handleSearch"
        >
          <template #append>
            <el-button :icon="Search" @click="handleSearch" />
          </template>
        </el-input>

        <el-input-number
          v-model="shareId"
          :min="1"
          :controls="false"
          placeholder="筛选关联分享 ID"
          class="filter-number"
        />

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
          <span class="panel-kicker">互动列表</span>
          <h2>回复清单</h2>
        </div>
        <el-button text @click="loadReplies">刷新列表</el-button>
      </div>

      <el-table :data="replies" v-loading="loading" stripe>
        <template #empty>
          <el-empty description="当前筛选条件下暂无回复">
            <el-button type="primary" plain @click="resetFilters">清空筛选</el-button>
          </el-empty>
        </template>

        <el-table-column prop="id" label="回复ID" width="96" />
        <el-table-column prop="shareId" label="分享ID" width="96" />

        <el-table-column label="回复作者" min-width="180">
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

        <el-table-column label="关联分享" min-width="300" show-overflow-tooltip>
          <template #default="{ row }">
            <div class="share-meta">
              <strong>{{ row.shareContentPreview || '该分享未填写文字' }}</strong>
              <span>分享作者：{{ row.shareAuthorDisplayName }}</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column prop="content" label="回复内容" min-width="260" show-overflow-tooltip />

        <el-table-column label="发布时间" width="180">
          <template #default="{ row }">
            {{ formatDate(row.createdAt) }}
          </template>
        </el-table-column>

        <el-table-column label="操作" width="210" fixed="right">
          <template #default="{ row }">
            <div class="action-group">
              <el-button size="small" plain @click="goToShare(row.shareId)">查看分享</el-button>
              <el-button
                size="small"
                type="danger"
                :loading="replyDeletingId === row.id"
                @click="handleDeleteReply(row)"
              >
                删除
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-bar">
        <span>共 {{ total }} 条回复</span>
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50]"
          :total="total"
          layout="sizes, prev, pager, next"
          @current-change="loadReplies"
          @size-change="handlePageSizeChange"
        />
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { RefreshRight, Search } from '@element-plus/icons-vue'
import { deleteAdminReply, getAdminReplyPage } from '@/api/adminContent'
import { API_ORIGIN } from '@/utils/request'

const router = useRouter()
const route = useRoute()

const keyword = ref('')
const shareId = ref()
const timeRange = ref([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const replies = ref([])
const loading = ref(false)
const replyDeletingId = ref(null)

const timePresets = [
  { key: '24h', label: '最近 24 小时' },
  { key: '7d', label: '最近 7 天' },
  { key: '30d', label: '最近 30 天' }
]

const activeFilters = computed(() => {
  const items = []

  if (keyword.value.trim()) {
    items.push({ key: 'keyword', label: `关键词：${keyword.value.trim()}` })
  }

  if (shareId.value) {
    items.push({ key: 'shareId', label: `分享ID：${shareId.value}` })
  }

  if (timeRange.value?.length === 2) {
    items.push({ key: 'timeRange', label: `时间：${formatShortDate(timeRange.value[0])} - ${formatShortDate(timeRange.value[1])}` })
  }

  return items
})

const heroStats = computed(() => {
  return [
    { label: '当前总量', value: `${total.value}`, helper: '符合当前筛选的回复数量' },
    { label: '本页回复', value: `${replies.value.length}`, helper: '当前分页中实际加载数量' },
    { label: '聚焦分享', value: shareId.value ? `#${shareId.value}` : '全部', helper: shareId.value ? '仅查看某条分享下的回复' : '当前为全局回复巡检' },
    { label: '筛选条件', value: `${activeFilters.value.length}`, helper: activeFilters.value.length ? '支持单独移除条件' : '当前为全量浏览' }
  ]
})

const loadReplies = async () => {
  loading.value = true
  try {
    const [startTime, endTime] = timeRange.value || []
    const data = await getAdminReplyPage({
      keyword: keyword.value.trim() || undefined,
      shareId: shareId.value || undefined,
      startTime: startTime || undefined,
      endTime: endTime || undefined,
      page: currentPage.value - 1,
      size: pageSize.value
    })
    replies.value = data.records || []
    total.value = data.total || 0
    currentPage.value = (data.page || 0) + 1
  } catch (error) {
    ElMessage.error(error.message || '加载回复列表失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = async () => {
  currentPage.value = 1
  syncQuery()
  await loadReplies()
}

const handlePageSizeChange = async () => {
  currentPage.value = 1
  await loadReplies()
}

const resetFilters = async () => {
  keyword.value = ''
  shareId.value = undefined
  timeRange.value = []
  currentPage.value = 1
  syncQuery()
  await loadReplies()
}

const handleDeleteReply = async (row) => {
  try {
    await ElMessageBox.confirm('确定删除这条回复吗？', '删除回复', {
      type: 'warning',
      confirmButtonText: '确认删除',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }

  replyDeletingId.value = row.id
  try {
    await deleteAdminReply(row.id)
    if (replies.value.length === 1 && currentPage.value > 1) {
      currentPage.value -= 1
    }
    await loadReplies()
    ElMessage.success('回复已删除')
  } catch (error) {
    ElMessage.error(error.message || '删除回复失败')
  } finally {
    replyDeletingId.value = null
  }
}

const goToShare = (shareIdValue) => {
  router.push({
    path: '/admin/shares',
    query: {
      openShareId: String(shareIdValue)
    }
  })
}

const applyTimePreset = async (presetKey) => {
  timeRange.value = buildPresetRange(presetKey)
  await handleSearch()
}

const removeFilter = async (key) => {
  if (key === 'keyword') {
    keyword.value = ''
  }
  if (key === 'shareId') {
    shareId.value = undefined
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
  shareId.value = route.query.shareId ? Number(route.query.shareId) : undefined

  if (typeof route.query.startTime === 'string' && typeof route.query.endTime === 'string') {
    timeRange.value = [route.query.startTime, route.query.endTime]
  } else {
    timeRange.value = []
  }
}

const syncQuery = () => {
  const [startTime, endTime] = timeRange.value || []
  const nextQuery = {
    keyword: keyword.value.trim() || undefined,
    shareId: shareId.value ? String(shareId.value) : undefined,
    startTime: startTime || undefined,
    endTime: endTime || undefined
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

onMounted(async () => {
  applyRouteQuery()
  await loadReplies()
})
</script>

<style scoped>
.reply-page {
  --panel-bg: var(--admin-panel);
  --panel-border: var(--admin-border);
  --ink: var(--admin-text);
  --muted: var(--admin-text-muted);
  --accent: var(--admin-accent-strong);
  --accent-soft: var(--admin-accent-soft);
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
  box-shadow: var(--front-shadow-soft);
}

.page-hero {
  border-radius: 30px;
  padding: 28px 30px;
  display: grid;
  grid-template-columns: minmax(0, 1.3fr) minmax(280px, 0.9fr);
  gap: 20px;
  background:
    radial-gradient(circle at top right, rgba(74, 222, 128, 0.18), transparent 26%),
    linear-gradient(135deg, rgba(255, 255, 255, 0.98), rgba(240, 253, 244, 0.94));
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
  font-family: var(--font-serif);
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
  border: 1px solid var(--admin-border);
  background: var(--admin-panel);
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
  width: 360px;
}

.filter-number {
  width: 180px;
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
.active-filter-chip {
  border: none;
  cursor: pointer;
  padding: 8px 12px;
  border-radius: 999px;
  font-size: 12px;
  transition: all 0.2s ease;
}

.preset-chip {
  background: var(--paper-100);
  color: var(--ink-600);
}

.preset-chip:hover {
  background: var(--admin-accent-soft);
  color: var(--accent);
}

.filter-label {
  color: var(--muted);
  font-size: 12px;
}

.active-filter-chip {
  background: var(--admin-accent-soft);
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
  font-family: var(--font-serif);
  font-size: 24px;
}

.author-cell {
  display: flex;
  align-items: center;
  gap: 12px;
}

.author-avatar {
  background: linear-gradient(135deg, var(--forest-400), var(--forest-700));
  color: #fff;
  font-weight: 700;
}

.author-meta,
.share-meta {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.author-meta strong,
.share-meta strong {
  color: var(--ink);
}

.author-meta span,
.share-meta span {
  color: var(--muted);
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

@media (max-width: 1240px) {
  .page-hero {
    grid-template-columns: 1fr;
  }

  .filter-input,
  .filter-number,
  .filter-date {
    width: 100%;
  }

  .filter-meta {
    align-items: flex-start;
    flex-direction: column;
  }
}

@media (max-width: 760px) {
  .hero-stats {
    grid-template-columns: 1fr;
  }

  .pagination-bar,
  .panel-head {
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
