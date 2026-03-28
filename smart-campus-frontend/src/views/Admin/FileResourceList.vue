<template>
  <div class="file-page">
    <section class="page-hero">
      <div class="hero-copy">
        <span class="hero-kicker">文件资源</span>
        <h1>统一管理头像、分享图片和公告封面</h1>
        <p>集中查看上传资源的引用情况、磁盘状态和异常文件，并可直接跳转回对应业务对象进行治理。</p>
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
          placeholder="搜索文件名、资源类型、归属对象或状态"
          @keyup.enter="handleSearch"
          @clear="handleSearch"
        >
          <template #append>
            <el-button :icon="Search" @click="handleSearch" />
          </template>
        </el-input>

        <el-select v-model="resourceType" clearable class="filter-select" placeholder="筛选资源类型" @change="handleSearch">
          <el-option v-for="item in resourceTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>

        <el-select v-model="status" clearable class="filter-select" placeholder="筛选资源状态" @change="handleSearch">
          <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>

        <el-button :icon="RefreshRight" @click="resetFilters">重置</el-button>
      </div>
    </section>

    <section class="table-panel">
      <div class="panel-head">
        <div>
          <span class="panel-kicker">资源清单</span>
          <h2>上传资源列表</h2>
        </div>
        <el-button text @click="loadResources">刷新列表</el-button>
      </div>

      <el-table :data="resources" v-loading="loading" stripe>
        <template #empty>
          <el-empty description="当前筛选条件下暂无文件资源" />
        </template>

        <el-table-column label="预览" width="92">
          <template #default="{ row }">
            <el-image
              v-if="row.fileExists"
              :src="resolveAssetUrl(row.resourceUrl)"
              fit="cover"
              class="resource-thumb"
              :preview-src-list="[resolveAssetUrl(row.resourceUrl)]"
              preview-teleported
            />
            <div v-else class="resource-thumb missing-thumb">缺失</div>
          </template>
        </el-table-column>

        <el-table-column prop="filename" label="文件名" min-width="220" show-overflow-tooltip />

        <el-table-column label="资源类型" width="120">
          <template #default="{ row }">
            <el-tag effect="plain">{{ getResourceTypeLabel(row.resourceType) }}</el-tag>
          </template>
        </el-table-column>

        <el-table-column label="归属对象" min-width="220" show-overflow-tooltip>
          <template #default="{ row }">
            <div class="meta-block">
              <strong>{{ row.ownerName || '残留文件' }}</strong>
              <span>{{ row.ownerType ? `${row.ownerType} · ID ${row.ownerId ?? '-'}` : '未绑定业务对象' }}</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="文件大小" width="120">
          <template #default="{ row }">
            {{ formatFileSize(row.fileSize) }}
          </template>
        </el-table-column>

        <el-table-column label="状态" width="130">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" effect="plain">
              {{ getStatusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="修改时间" width="180">
          <template #default="{ row }">
            {{ formatDate(row.lastModifiedAt) }}
          </template>
        </el-table-column>

        <el-table-column label="操作" width="250" fixed="right">
          <template #default="{ row }">
            <div class="action-group">
              <el-button size="small" plain :disabled="!row.ownerType" @click="goToOwner(row)">查看业务</el-button>
              <el-button
                size="small"
                type="danger"
                :loading="deletingKey === buildDeleteKey(row)"
                @click="handleDelete(row)"
              >
                删除资源
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-bar">
        <span>共 {{ total }} 条资源记录</span>
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="total"
          layout="sizes, prev, pager, next"
          @current-change="loadResources"
          @size-change="handlePageSizeChange"
        />
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { RefreshRight, Search } from '@element-plus/icons-vue'
import { deleteAdminFileResource, getAdminFileResourcePage } from '@/api/adminFileResource'
import { API_ORIGIN } from '@/utils/request'

const router = useRouter()

const resourceTypeOptions = [
  { label: '头像', value: 'AVATAR' },
  { label: '分享图片', value: 'SHARE_IMAGE' },
  { label: '公告封面', value: 'ANNOUNCEMENT_COVER' }
]

const statusOptions = [
  { label: '正常', value: 'NORMAL' },
  { label: '残留文件', value: 'ORPHAN_FILE' },
  { label: '文件缺失', value: 'MISSING_FILE' }
]

const keyword = ref('')
const resourceType = ref()
const status = ref()
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const resources = ref([])
const loading = ref(false)
const deletingKey = ref('')

const heroStats = computed(() => {
  const normalCount = resources.value.filter((item) => item.status === 'NORMAL').length
  const orphanCount = resources.value.filter((item) => item.status === 'ORPHAN_FILE').length
  const missingCount = resources.value.filter((item) => item.status === 'MISSING_FILE').length

  return [
    { label: '当前总量', value: `${total.value}`, helper: '符合当前筛选条件的资源数量' },
    { label: '本页正常', value: `${normalCount}`, helper: '磁盘文件和业务引用均正常' },
    { label: '本页残留', value: `${orphanCount}`, helper: '文件还在，但业务已经不再使用' },
    { label: '本页缺失', value: `${missingCount}`, helper: '业务仍在引用，但磁盘文件已经不存在' }
  ]
})

const loadResources = async () => {
  loading.value = true
  try {
    const data = await getAdminFileResourcePage({
      keyword: keyword.value.trim() || undefined,
      resourceType: resourceType.value || undefined,
      status: status.value || undefined,
      page: currentPage.value - 1,
      size: pageSize.value
    })
    resources.value = data.records || []
    total.value = data.total || 0
    currentPage.value = (data.page || 0) + 1
  } catch (error) {
    ElMessage.error(error.message || '加载文件资源失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = async () => {
  currentPage.value = 1
  await loadResources()
}

const handlePageSizeChange = async () => {
  currentPage.value = 1
  await loadResources()
}

const resetFilters = async () => {
  keyword.value = ''
  resourceType.value = undefined
  status.value = undefined
  currentPage.value = 1
  await loadResources()
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm('删除资源后将同步清理业务引用。该操作不可恢复，是否继续？', '删除资源', {
      type: 'warning',
      confirmButtonText: '确认删除',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }

  const key = buildDeleteKey(row)
  deletingKey.value = key
  try {
    await deleteAdminFileResource({
      resourceType: row.resourceType,
      resourceUrl: row.resourceUrl,
      ownerId: row.ownerId ?? undefined
    })
    await loadResources()
    ElMessage.success('资源已删除')
  } catch (error) {
    ElMessage.error(error.message || '删除资源失败')
  } finally {
    deletingKey.value = ''
  }
}

const goToOwner = (row) => {
  if (!row.ownerType || !row.ownerId) {
    return
  }

  if (row.ownerType === 'USER') {
    router.push({ path: '/admin/users', query: { openUserId: String(row.ownerId) } })
    return
  }

  if (row.ownerType === 'ANNOUNCEMENT') {
    router.push({ path: '/admin/announcements', query: { openAnnouncementId: String(row.ownerId) } })
    return
  }

  if (row.ownerType === 'SHARE') {
    router.push({ path: '/admin/shares', query: { openShareId: String(row.ownerId) } })
  }
}

const buildDeleteKey = (row) => `${row.resourceType}|${row.resourceUrl}|${row.ownerId ?? '-'}`

const getResourceTypeLabel = (value) => {
  if (value === 'AVATAR') return '头像'
  if (value === 'SHARE_IMAGE') return '分享图片'
  if (value === 'ANNOUNCEMENT_COVER') return '公告封面'
  return value || '资源'
}

const getStatusLabel = (value) => {
  if (value === 'NORMAL') return '正常'
  if (value === 'ORPHAN_FILE') return '残留文件'
  if (value === 'MISSING_FILE') return '文件缺失'
  return value || '未知'
}

const statusTagType = (value) => {
  if (value === 'NORMAL') return 'success'
  if (value === 'ORPHAN_FILE') return 'warning'
  if (value === 'MISSING_FILE') return 'danger'
  return 'info'
}

const formatFileSize = (size) => {
  if (size == null) return '未知'
  if (size < 1024) return `${size} B`
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`
  return `${(size / (1024 * 1024)).toFixed(2)} MB`
}

const formatDate = (value) => {
  if (!value) return '暂无'
  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  }).format(new Date(value))
}

const resolveAssetUrl = (value) => {
  if (!value) return ''
  if (/^https?:\/\//i.test(value)) return value
  return `${API_ORIGIN}${value.startsWith('/') ? value : `/${value}`}`
}

onMounted(async () => {
  await loadResources()
})
</script>

<style scoped>
.file-page {
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
  grid-template-columns: minmax(0, 1.2fr) minmax(280px, 0.9fr);
  gap: 20px;
  background:
    radial-gradient(circle at top right, rgba(59, 130, 246, 0.16), transparent 26%),
    linear-gradient(135deg, rgba(255, 255, 255, 0.98), rgba(239, 246, 255, 0.94));
}

.hero-kicker,
.panel-kicker {
  display: inline-flex;
  align-items: center;
  padding: 6px 12px;
  border-radius: 999px;
  background: rgba(37, 99, 235, 0.08);
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
  border: 1px solid rgba(191, 219, 254, 0.8);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.94), rgba(244, 248, 255, 0.92));
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
.action-group {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.filter-input {
  width: 360px;
}

.filter-select {
  width: 200px;
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

.resource-thumb {
  width: 56px;
  height: 56px;
  border-radius: 14px;
  overflow: hidden;
  background: #f1f5f9;
}

.missing-thumb {
  display: flex;
  align-items: center;
  justify-content: center;
  color: #ef4444;
  font-size: 12px;
}

.meta-block {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.meta-block strong {
  color: var(--ink);
}

.meta-block span {
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
  .hero-stats {
    grid-template-columns: 1fr;
  }

  .pagination-bar,
  .panel-head {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
