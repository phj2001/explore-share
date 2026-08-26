<template>
  <div class="resource-page">
    <section class="page-hero">
      <div class="hero-copy">
        <span class="hero-kicker">文件资源</span>
        <h1>统一查看头像、分享图片与公告封面资源</h1>
        <p>
          这里会汇总业务正在使用的文件、磁盘残留文件和已缺失的资源引用，
          方便运营后台统一排查、预览和删除异常资源。
        </p>
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
          placeholder="搜索文件名、归属对象或资源类型"
          @keyup.enter="handleSearch"
          @clear="handleSearch"
        >
          <template #append>
            <el-button :icon="Search" @click="handleSearch" />
          </template>
        </el-input>

        <el-select
          v-model="resourceType"
          clearable
          class="filter-select"
          placeholder="筛选资源类型"
          @change="handleSearch"
        >
          <el-option
            v-for="item in resourceTypeOptions"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>

        <el-select
          v-model="status"
          clearable
          class="filter-select"
          placeholder="筛选资源状态"
          @change="handleSearch"
        >
          <el-option
            v-for="item in statusOptions"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>

        <el-button :icon="RefreshRight" @click="resetFilters">重置</el-button>
      </div>
    </section>

    <section class="table-panel">
      <div class="panel-head">
        <div>
          <span class="panel-kicker">资源列表</span>
          <h2>文件资源台账</h2>
        </div>
        <el-button text @click="loadResources">刷新列表</el-button>
      </div>

      <el-table :data="resources" v-loading="loading" stripe>
        <template #empty>
          <el-empty description="当前筛选条件下暂无文件资源记录" />
        </template>

        <el-table-column label="预览" width="108">
          <template #default="{ row }">
            <el-image
              v-if="resolveAssetUrl(row.resourceUrl)"
              :src="resolveAssetUrl(row.resourceUrl)"
              fit="cover"
              class="resource-thumb"
              :preview-src-list="[resolveAssetUrl(row.resourceUrl)]"
              preview-teleported
            />
            <span v-else class="muted-text">暂无预览</span>
          </template>
        </el-table-column>

        <el-table-column label="文件信息" min-width="260">
          <template #default="{ row }">
            <div class="file-meta">
              <strong>{{ row.filename || '-' }}</strong>
              <span>{{ row.resourceUrl || '-' }}</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="资源类型" width="130">
          <template #default="{ row }">
            <el-tag effect="plain" :type="resourceTagType(row.resourceType)">
              {{ getResourceTypeLabel(row.resourceType) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="归属对象" min-width="220">
          <template #default="{ row }">
            <div class="file-meta compact">
              <strong>{{ row.ownerName || '未关联业务对象' }}</strong>
              <span>{{ getOwnerTypeLabel(row.ownerType) }}{{ row.ownerId ? ` #${row.ownerId}` : '' }}</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="文件大小" width="120">
          <template #default="{ row }">
            {{ formatFileSize(row.fileSize) }}
          </template>
        </el-table-column>

        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <el-tag effect="plain" :type="statusTagType(row.status)">
              {{ getStatusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="最后修改时间" width="180">
          <template #default="{ row }">
            {{ formatDate(row.lastModifiedAt) }}
          </template>
        </el-table-column>

        <el-table-column label="操作" width="210" fixed="right">
          <template #default="{ row }">
            <div class="action-group">
              <el-button size="small" plain @click="goToOwner(row)">查看归属</el-button>
              <el-button
                size="small"
                type="danger"
                :loading="deletingKey === buildDeleteKey(row)"
                @click="handleDelete(row)"
              >
                删除
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
          :page-sizes="[10, 20, 50]"
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
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { RefreshRight, Search } from '@element-plus/icons-vue'
import { deleteAdminFileResource, getAdminFileResourcePage } from '@/api/adminFileResource'
import { API_ORIGIN } from '@/utils/request'
import { useAdminList } from '@/composables/useAdminList'

const router = useRouter()

const keyword = ref('')
const resourceType = ref()
const status = ref()
const deletingKey = ref('')

const {
  list: resources,
  loading,
  currentPage,
  pageSize,
  total,
  load: loadResources,
  search: handleSearch,
  reset,
  handlePageSizeChange
} = useAdminList({
  fetchPage: ({ page, size }) => getAdminFileResourcePage({
    keyword: keyword.value.trim() || undefined,
    resourceType: resourceType.value,
    status: status.value,
    page,
    size
  }),
  errorMessage: '加载文件资源失败'
})

const resourceTypeOptions = [
  { label: '用户头像', value: 'AVATAR' },
  { label: '分享图片', value: 'SHARE_IMAGE' },
  { label: '公告封面', value: 'ANNOUNCEMENT_COVER' }
]

const statusOptions = [
  { label: '正常资源', value: 'NORMAL' },
  { label: '残留文件', value: 'ORPHAN_FILE' },
  { label: '文件缺失', value: 'MISSING_FILE' }
]

const heroStats = computed(() => {
  const list = resources.value
  return [
    { label: '当前总量', value: `${total.value}`, helper: '符合当前筛选条件的资源数量' },
    { label: '本页头像', value: `${list.filter((item) => item.resourceType === 'AVATAR').length}`, helper: '用户资料相关的头像资源' },
    { label: '本页残留', value: `${list.filter((item) => item.status === 'ORPHAN_FILE').length}`, helper: '文件还在，但业务已不再使用' },
    { label: '本页缺失', value: `${list.filter((item) => item.status === 'MISSING_FILE').length}`, helper: '业务仍在引用，但磁盘文件已不存在' }
  ]
})

const resetFilters = async () => {
  keyword.value = ''
  resourceType.value = undefined
  status.value = undefined
  await reset()
}

const buildDeleteKey = (row) => `${row.resourceType}|${row.resourceUrl}|${row.ownerId ?? '-'}`

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(
      '删除资源后，会同步清理对应的业务引用。此操作不可恢复，是否继续？',
      '删除文件资源',
      {
        type: 'warning',
        confirmButtonText: '确认删除',
        cancelButtonText: '取消'
      }
    )
  } catch {
    return
  }

  const deleteKey = buildDeleteKey(row)
  deletingKey.value = deleteKey

  try {
    await deleteAdminFileResource({
      resourceType: row.resourceType,
      resourceUrl: row.resourceUrl,
      ownerId: row.ownerId ?? undefined
    })
    await loadResources()
    ElMessage.success('文件资源已删除')
  } catch (error) {
    ElMessage.error(error.message || '删除文件资源失败')
  } finally {
    deletingKey.value = ''
  }
}

const goToOwner = (row) => {
  if (!row.ownerId) {
    ElMessage.info('该资源未关联具体业务对象')
    return
  }

  if (row.ownerType === 'USER') {
    router.push({ path: '/admin/users', query: { openUserId: String(row.ownerId) } })
    return
  }

  if (row.ownerType === 'SHARE') {
    router.push({ path: '/admin/shares', query: { openShareId: String(row.ownerId) } })
    return
  }

  if (row.ownerType === 'ANNOUNCEMENT') {
    router.push({ path: '/admin/announcements', query: { openAnnouncementId: String(row.ownerId) } })
    return
  }

  ElMessage.info('暂不支持跳转到该资源的归属对象')
}

const getResourceTypeLabel = (value) => {
  if (value === 'AVATAR') return '用户头像'
  if (value === 'SHARE_IMAGE') return '分享图片'
  if (value === 'ANNOUNCEMENT_COVER') return '公告封面'
  return '未知资源'
}

const getOwnerTypeLabel = (value) => {
  if (value === 'USER') return '用户'
  if (value === 'SHARE') return '分享'
  if (value === 'ANNOUNCEMENT') return '公告'
  return '未关联'
}

const getStatusLabel = (value) => {
  if (value === 'NORMAL') return '正常资源'
  if (value === 'ORPHAN_FILE') return '残留文件'
  if (value === 'MISSING_FILE') return '文件缺失'
  return '未知状态'
}

const resourceTagType = (value) => {
  if (value === 'AVATAR') return 'primary'
  if (value === 'SHARE_IMAGE') return 'success'
  if (value === 'ANNOUNCEMENT_COVER') return 'warning'
  return 'info'
}

const statusTagType = (value) => {
  if (value === 'NORMAL') return 'success'
  if (value === 'ORPHAN_FILE') return 'warning'
  if (value === 'MISSING_FILE') return 'danger'
  return 'info'
}

const formatFileSize = (value) => {
  if (value == null) return '未知'
  if (value < 1024) return `${value} B`
  if (value < 1024 * 1024) return `${(value / 1024).toFixed(1)} KB`
  return `${(value / (1024 * 1024)).toFixed(1)} MB`
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
</script>

<style scoped>
.resource-page {
  --panel-bg: var(--admin-panel);
  --panel-border: var(--admin-border);
  --ink: var(--admin-text);
  --muted: var(--admin-text-muted);
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
  grid-template-columns: minmax(0, 1.25fr) minmax(280px, 0.9fr);
  gap: 20px;
  background:
    radial-gradient(circle at top right, rgba(74, 222, 128, 0.16), transparent 26%),
    linear-gradient(135deg, rgba(255, 255, 255, 0.98), rgba(240, 253, 244, 0.92));
}

.hero-kicker,
.panel-kicker {
  display: inline-flex;
  align-items: center;
  padding: 6px 12px;
  border-radius: 999px;
  background: var(--admin-accent-soft);
  color: var(--admin-accent-strong);
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
  font-family: var(--font-serif);
  font-size: 24px;
}

.resource-thumb {
  width: 70px;
  height: 70px;
  border-radius: 16px;
  overflow: hidden;
  background: var(--paper-50);
}

.file-meta {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.file-meta.compact {
  gap: 2px;
}

.file-meta strong {
  color: var(--ink);
}

.file-meta span,
.muted-text {
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

@media (max-width: 1180px) {
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

  .page-hero,
  .filter-panel,
  .table-panel {
    border-radius: 22px;
  }

  .pagination-bar,
  .panel-head {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
