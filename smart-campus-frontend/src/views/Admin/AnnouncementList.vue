<template>
  <div class="announcement-page">
    <section class="page-hero">
      <div class="hero-copy">
        <span class="hero-kicker">公告管理</span>
        <h1>统一发布校园通知、临时提醒和首页推荐信息</h1>
        <p>支持草稿保存、正式发布、置顶展示和封面图管理，后台编辑完成后会直接同步到前台首页公告区。</p>
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
          placeholder="搜索标题、摘要或正文内容"
          @keyup.enter="handleSearch"
          @clear="handleSearch"
        >
          <template #append>
            <el-button :icon="Search" @click="handleSearch" />
          </template>
        </el-input>

        <el-select v-model="statusFilter" clearable class="filter-select" placeholder="筛选发布状态" @change="handleSearch">
          <el-option :value="DRAFT_STATUS" label="草稿" />
          <el-option :value="PUBLISHED_STATUS" label="已发布" />
        </el-select>

        <el-select v-model="pinnedFilter" clearable class="filter-select" placeholder="筛选置顶状态" @change="handleSearch">
          <el-option :value="true" label="已置顶" />
          <el-option :value="false" label="未置顶" />
        </el-select>

        <el-button type="primary" :icon="Plus" @click="openCreateDialog">新建公告</el-button>
        <el-button :icon="RefreshRight" @click="resetFilters">重置</el-button>
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
    </section>

    <section class="table-panel">
      <div class="panel-head">
        <div>
          <span class="panel-kicker">发布列表</span>
          <h2>校园公告</h2>
        </div>
        <el-button text @click="loadAnnouncements">刷新列表</el-button>
      </div>

      <el-table :data="announcements" v-loading="loading" stripe>
        <template #empty>
          <el-empty description="当前筛选条件下暂无公告">
            <el-button type="primary" plain @click="openCreateDialog">新建第一条公告</el-button>
          </el-empty>
        </template>

        <el-table-column prop="id" label="公告ID" width="100" />

        <el-table-column label="公告信息" min-width="320">
          <template #default="{ row }">
            <div class="title-cell">
              <el-image
                v-if="row.coverImageUrl"
                :src="resolveAssetUrl(row.coverImageUrl)"
                fit="cover"
                class="cover-thumb"
              />
              <div class="title-meta">
                <strong>{{ row.title }}</strong>
                <p>{{ row.summary }}</p>
              </div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="row.status === PUBLISHED_STATUS ? 'success' : 'info'" effect="plain">
              {{ row.status === PUBLISHED_STATUS ? '已发布' : '草稿' }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="置顶" width="110">
          <template #default="{ row }">
            <el-tag :type="row.pinned ? 'warning' : 'info'" effect="plain">
              {{ row.pinned ? '已置顶' : '未置顶' }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="发布时间" width="180">
          <template #default="{ row }">
            {{ formatDate(row.publishedAt) }}
          </template>
        </el-table-column>

        <el-table-column label="更新时间" width="180">
          <template #default="{ row }">
            {{ formatDate(row.updatedAt) }}
          </template>
        </el-table-column>

        <el-table-column label="操作" width="320" fixed="right">
          <template #default="{ row }">
            <div class="action-group">
              <el-button size="small" @click="openDetail(row.id)">查看</el-button>
              <el-button size="small" plain @click="openEditDialog(row.id)">编辑</el-button>
              <el-button
                size="small"
                :type="row.status === PUBLISHED_STATUS ? 'warning' : 'success'"
                :loading="publishUpdatingId === row.id"
                @click="togglePublish(row)"
              >
                {{ row.status === PUBLISHED_STATUS ? '取消发布' : '发布' }}
              </el-button>
              <el-button
                size="small"
                :type="row.pinned ? 'info' : 'warning'"
                :loading="pinUpdatingId === row.id"
                @click="togglePinned(row)"
              >
                {{ row.pinned ? '取消置顶' : '置顶' }}
              </el-button>
              <el-button
                size="small"
                type="danger"
                :loading="deletingId === row.id"
                @click="handleDelete(row)"
              >
                删除
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-bar">
        <span>共 {{ total }} 条公告</span>
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50]"
          :total="total"
          layout="sizes, prev, pager, next"
          @current-change="loadAnnouncements"
          @size-change="handlePageSizeChange"
        />
      </div>
    </section>

    <el-drawer v-model="drawerVisible" size="620px" :with-header="false" destroy-on-close>
      <div class="drawer-body" v-loading="detailLoading">
        <template v-if="selectedAnnouncement">
          <div class="drawer-hero">
            <div>
              <div class="drawer-tags">
                <el-tag :type="selectedAnnouncement.status === PUBLISHED_STATUS ? 'success' : 'info'" effect="plain">
                  {{ selectedAnnouncement.status === PUBLISHED_STATUS ? '已发布' : '草稿' }}
                </el-tag>
                <el-tag :type="selectedAnnouncement.pinned ? 'warning' : 'info'" effect="plain">
                  {{ selectedAnnouncement.pinned ? '已置顶' : '未置顶' }}
                </el-tag>
              </div>
              <h2>{{ selectedAnnouncement.title }}</h2>
              <p>{{ selectedAnnouncement.summary }}</p>
            </div>

            <div class="drawer-actions">
              <el-button plain @click="openEditDialog(selectedAnnouncement.id)">编辑</el-button>
              <el-button
                :type="selectedAnnouncement.status === PUBLISHED_STATUS ? 'warning' : 'success'"
                plain
                :loading="publishUpdatingId === selectedAnnouncement.id"
                @click="togglePublish(selectedAnnouncement)"
              >
                {{ selectedAnnouncement.status === PUBLISHED_STATUS ? '取消发布' : '发布公告' }}
              </el-button>
            </div>
          </div>

          <el-image
            v-if="selectedAnnouncement.coverImageUrl"
            :src="resolveAssetUrl(selectedAnnouncement.coverImageUrl)"
            fit="cover"
            class="drawer-cover"
          />

          <div class="info-grid">
            <div class="info-item">
              <span>公告ID</span>
              <strong>{{ selectedAnnouncement.id }}</strong>
            </div>
            <div class="info-item">
              <span>发布时间</span>
              <strong>{{ formatDate(selectedAnnouncement.publishedAt) }}</strong>
            </div>
            <div class="info-item">
              <span>创建时间</span>
              <strong>{{ formatDate(selectedAnnouncement.createdAt) }}</strong>
            </div>
            <div class="info-item">
              <span>更新时间</span>
              <strong>{{ formatDate(selectedAnnouncement.updatedAt) }}</strong>
            </div>
          </div>

          <div class="content-card">
            {{ selectedAnnouncement.content }}
          </div>
        </template>
      </div>
    </el-drawer>

    <el-dialog
      v-model="formDialogVisible"
      :title="isEditMode ? '编辑公告' : '新建公告'"
      width="760px"
      destroy-on-close
      @closed="resetFormState"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="92px">
        <el-form-item label="标题" prop="title">
          <el-input v-model="form.title" maxlength="120" show-word-limit placeholder="请输入公告标题" />
        </el-form-item>

        <el-form-item label="摘要" prop="summary">
          <el-input
            v-model="form.summary"
            type="textarea"
            :rows="3"
            maxlength="220"
            show-word-limit
            placeholder="用于首页卡片和后台列表的简短摘要"
          />
        </el-form-item>

        <el-form-item label="正文" prop="content">
          <el-input
            v-model="form.content"
            type="textarea"
            :rows="10"
            maxlength="10000"
            show-word-limit
            placeholder="请输入公告正文"
          />
        </el-form-item>

        <el-form-item label="封面图">
          <div class="upload-panel">
            <el-upload
              v-model:file-list="coverFileList"
              :auto-upload="false"
              :limit="1"
              accept=".jpg,.jpeg,.png,.webp"
              list-type="picture-card"
              class="cover-uploader"
              @change="handleCoverChange"
              @remove="handleCoverRemove"
              @exceed="handleCoverExceed"
            >
              <el-icon><Plus /></el-icon>
            </el-upload>

            <div class="upload-meta">
              <span>支持 JPG / PNG / WEBP，单张不超过 5MB</span>
              <el-button
                v-if="form.coverPreview"
                size="small"
                text
                type="danger"
                @click="clearCoverSelection"
              >
                移除当前封面
              </el-button>
            </div>
          </div>
        </el-form-item>

        <el-form-item label="其它设置">
          <div class="switch-group">
            <el-switch v-model="form.pinned" inline-prompt active-text="置顶" inactive-text="普通" />
            <span class="switch-tip">置顶公告会优先展示在前台首页</span>
          </div>
        </el-form-item>
      </el-form>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="formDialogVisible = false">取消</el-button>
          <el-button :loading="formSubmitting" @click="submitForm(DRAFT_STATUS)">保存草稿</el-button>
          <el-button type="primary" :loading="formSubmitting" @click="submitForm(PUBLISHED_STATUS)">
            {{ isEditMode ? '保存并发布' : '创建并发布' }}
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, RefreshRight, Search } from '@element-plus/icons-vue'
import {
  createAdminAnnouncement,
  deleteAdminAnnouncement,
  getAdminAnnouncementDetail,
  getAdminAnnouncementPage,
  updateAdminAnnouncement,
  updateAdminAnnouncementPinnedStatus,
  updateAdminAnnouncementPublishStatus
} from '@/api/adminAnnouncement'
import { API_ORIGIN } from '@/utils/request'

const DRAFT_STATUS = 0
const PUBLISHED_STATUS = 1
const route = useRoute()

const keyword = ref('')
const statusFilter = ref()
const pinnedFilter = ref()
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const announcements = ref([])
const loading = ref(false)
const drawerVisible = ref(false)
const detailLoading = ref(false)
const selectedAnnouncement = ref(null)
const deletingId = ref(null)
const publishUpdatingId = ref(null)
const pinUpdatingId = ref(null)

const formDialogVisible = ref(false)
const formSubmitting = ref(false)
const isEditMode = ref(false)
const editingId = ref(null)
const formRef = ref(null)
const coverFileList = ref([])

const form = reactive({
  title: '',
  summary: '',
  content: '',
  pinned: false,
  coverImage: null,
  coverPreview: '',
  removeCoverImage: false
})

const rules = {
  title: [{ required: true, message: '请输入公告标题', trigger: 'blur' }],
  summary: [{ required: true, message: '请输入公告摘要', trigger: 'blur' }],
  content: [{ required: true, message: '请输入公告正文', trigger: 'blur' }]
}

const activeFilters = computed(() => {
  const items = []

  if (keyword.value.trim()) {
    items.push({ key: 'keyword', label: `关键词：${keyword.value.trim()}` })
  }

  if (statusFilter.value === DRAFT_STATUS) {
    items.push({ key: 'status', label: '状态：草稿' })
  }

  if (statusFilter.value === PUBLISHED_STATUS) {
    items.push({ key: 'status', label: '状态：已发布' })
  }

  if (pinnedFilter.value === true) {
    items.push({ key: 'pinned', label: '置顶：已置顶' })
  }

  if (pinnedFilter.value === false) {
    items.push({ key: 'pinned', label: '置顶：未置顶' })
  }

  return items
})

const heroStats = computed(() => {
  const publishedCount = announcements.value.filter((item) => item.status === PUBLISHED_STATUS).length
  const pinnedCount = announcements.value.filter((item) => item.pinned).length

  return [
    { label: '当前总量', value: `${total.value}`, helper: '符合当前筛选的公告数量' },
    { label: '本页已发布', value: `${publishedCount}`, helper: '可以直接展示到前台首页' },
    { label: '本页置顶', value: `${pinnedCount}`, helper: '首页将优先展示这些公告' },
    { label: '筛选条件', value: `${activeFilters.value.length}`, helper: activeFilters.value.length ? '可点击下方标签快速移除' : '当前为全量浏览' }
  ]
})

const loadAnnouncements = async () => {
  loading.value = true
  try {
    const data = await getAdminAnnouncementPage({
      keyword: keyword.value.trim() || undefined,
      status: statusFilter.value,
      pinned: pinnedFilter.value,
      page: currentPage.value - 1,
      size: pageSize.value
    })
    announcements.value = data.records || []
    total.value = data.total || 0
    currentPage.value = (data.page || 0) + 1
  } catch (error) {
    ElMessage.error(error.message || '加载公告列表失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = async () => {
  currentPage.value = 1
  await loadAnnouncements()
}

const handlePageSizeChange = async () => {
  currentPage.value = 1
  await loadAnnouncements()
}

const resetFilters = async () => {
  keyword.value = ''
  statusFilter.value = undefined
  pinnedFilter.value = undefined
  currentPage.value = 1
  await loadAnnouncements()
}

const removeFilter = async (key) => {
  if (key === 'keyword') {
    keyword.value = ''
  }
  if (key === 'status') {
    statusFilter.value = undefined
  }
  if (key === 'pinned') {
    pinnedFilter.value = undefined
  }
  await handleSearch()
}

const openDetail = async (announcementId) => {
  drawerVisible.value = true
  detailLoading.value = true
  try {
    selectedAnnouncement.value = await getAdminAnnouncementDetail(announcementId)
  } catch (error) {
    drawerVisible.value = false
    selectedAnnouncement.value = null
    ElMessage.error(error.message || '加载公告详情失败')
  } finally {
    detailLoading.value = false
  }
}

const openCreateDialog = () => {
  isEditMode.value = false
  editingId.value = null
  formDialogVisible.value = true
}

const openEditDialog = async (announcementId) => {
  try {
    const detail = await getAdminAnnouncementDetail(announcementId)
    isEditMode.value = true
    editingId.value = announcementId
    fillForm(detail)
    formDialogVisible.value = true
  } catch (error) {
    ElMessage.error(error.message || '加载公告详情失败')
  }
}

const fillForm = (detail) => {
  form.title = detail.title || ''
  form.summary = detail.summary || ''
  form.content = detail.content || ''
  form.pinned = !!detail.pinned
  form.coverImage = null
  form.coverPreview = detail.coverImageUrl || ''
  form.removeCoverImage = false
  coverFileList.value = detail.coverImageUrl
    ? [{
        name: 'current-cover',
        url: resolveAssetUrl(detail.coverImageUrl)
      }]
    : []
}

const resetFormState = () => {
  if (form.coverPreview && form.coverPreview.startsWith('blob:')) {
    URL.revokeObjectURL(form.coverPreview)
  }
  form.title = ''
  form.summary = ''
  form.content = ''
  form.pinned = false
  form.coverImage = null
  form.coverPreview = ''
  form.removeCoverImage = false
  coverFileList.value = []
  editingId.value = null
  isEditMode.value = false
  formRef.value?.clearValidate()
}

const submitForm = async (status) => {
  await formRef.value.validate()

  formSubmitting.value = true
  try {
    const payload = {
      title: form.title,
      summary: form.summary,
      content: form.content,
      pinned: form.pinned,
      status,
      coverImage: form.coverImage,
      removeCoverImage: form.removeCoverImage
    }

    let detail
    if (isEditMode.value && editingId.value) {
      detail = await updateAdminAnnouncement(editingId.value, payload)
      ElMessage.success(status === PUBLISHED_STATUS ? '公告已更新并发布' : '草稿已更新')
    } else {
      detail = await createAdminAnnouncement(payload)
      ElMessage.success(status === PUBLISHED_STATUS ? '公告已创建并发布' : '草稿已保存')
    }

    formDialogVisible.value = false
    await loadAnnouncements()

    if (drawerVisible.value && selectedAnnouncement.value?.id === detail.id) {
      selectedAnnouncement.value = detail
    }
  } catch (error) {
    ElMessage.error(error.message || '保存公告失败')
  } finally {
    formSubmitting.value = false
  }
}

const togglePublish = async (row) => {
  const nextPublished = row.status !== PUBLISHED_STATUS
  publishUpdatingId.value = row.id
  try {
    const detail = await updateAdminAnnouncementPublishStatus(row.id, nextPublished)
    updateRowFromDetail(row, detail)
    await loadAnnouncements()
    if (selectedAnnouncement.value?.id === row.id) {
      selectedAnnouncement.value = detail
    }
    ElMessage.success(nextPublished ? '公告已发布' : '公告已转为草稿')
  } catch (error) {
    ElMessage.error(error.message || '更新发布状态失败')
  } finally {
    publishUpdatingId.value = null
  }
}

const togglePinned = async (row) => {
  const nextPinned = !row.pinned
  pinUpdatingId.value = row.id
  try {
    const detail = await updateAdminAnnouncementPinnedStatus(row.id, nextPinned)
    updateRowFromDetail(row, detail)
    await loadAnnouncements()
    if (selectedAnnouncement.value?.id === row.id) {
      selectedAnnouncement.value = detail
    }
    ElMessage.success(nextPinned ? '公告已置顶' : '公告已取消置顶')
  } catch (error) {
    ElMessage.error(error.message || '更新置顶状态失败')
  } finally {
    pinUpdatingId.value = null
  }
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm('删除公告后，其封面图和前台展示入口都会一起移除。是否继续？', '删除公告', {
      type: 'warning',
      confirmButtonText: '确认删除',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }

  deletingId.value = row.id
  try {
    await deleteAdminAnnouncement(row.id)
    if (announcements.value.length === 1 && currentPage.value > 1) {
      currentPage.value -= 1
    }
    if (selectedAnnouncement.value?.id === row.id) {
      drawerVisible.value = false
      selectedAnnouncement.value = null
    }
    await loadAnnouncements()
    ElMessage.success('公告已删除')
  } catch (error) {
    ElMessage.error(error.message || '删除公告失败')
  } finally {
    deletingId.value = null
  }
}

const updateRowFromDetail = (row, detail) => {
  row.title = detail.title
  row.summary = detail.summary
  row.coverImageUrl = detail.coverImageUrl
  row.status = detail.status
  row.pinned = detail.pinned
  row.publishedAt = detail.publishedAt
  row.updatedAt = detail.updatedAt
}

const handleCoverChange = (uploadFile, uploadFiles) => {
  const nextFile = uploadFile.raw
  if (!nextFile) {
    return
  }

  const isValidType = ['image/jpeg', 'image/png', 'image/webp'].includes(nextFile.type)
  const isValidSize = nextFile.size <= 5 * 1024 * 1024

  if (!isValidType) {
    ElMessage.error('封面图仅支持 JPG、PNG、WEBP 格式')
    coverFileList.value = []
    return
  }

  if (!isValidSize) {
    ElMessage.error('封面图大小不能超过 5MB')
    coverFileList.value = []
    return
  }

  if (form.coverPreview && form.coverPreview.startsWith('blob:')) {
    URL.revokeObjectURL(form.coverPreview)
  }

  form.coverImage = nextFile
  form.coverPreview = URL.createObjectURL(nextFile)
  form.removeCoverImage = false
  coverFileList.value = uploadFiles.slice(-1)
}

const handleCoverRemove = () => {
  clearCoverSelection()
}

const handleCoverExceed = (files) => {
  coverFileList.value = []
  handleCoverChange({ raw: files[0] }, [{ name: files[0].name, raw: files[0] }])
}

const clearCoverSelection = () => {
  form.coverImage = null
  if (form.coverPreview && form.coverPreview.startsWith('blob:')) {
    URL.revokeObjectURL(form.coverPreview)
  }
  form.coverPreview = ''
  form.removeCoverImage = true
  coverFileList.value = []
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

const formatDate = (value) => {
  if (!value) {
    return '暂未发布'
  }

  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  }).format(new Date(value))
}

onMounted(async () => {
  await loadAnnouncements()
  if (route.query.openAnnouncementId) {
    await openDetail(Number(route.query.openAnnouncementId))
  }
})
</script>

<style scoped>
.announcement-page {
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
  grid-template-columns: minmax(0, 1.25fr) minmax(280px, 0.9fr);
  gap: 20px;
  background:
    radial-gradient(circle at top right, rgba(249, 115, 22, 0.16), transparent 26%),
    linear-gradient(135deg, rgba(255, 255, 255, 0.98), rgba(255, 247, 237, 0.92));
}

.hero-kicker,
.panel-kicker {
  display: inline-flex;
  align-items: center;
  padding: 6px 12px;
  border-radius: 999px;
  background: rgba(249, 115, 22, 0.08);
  color: #c2410c;
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
  border: 1px solid rgba(253, 186, 116, 0.45);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.95), rgba(255, 247, 237, 0.92));
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

.filter-main,
.active-filter-list,
.action-group,
.drawer-tags,
.drawer-actions,
.switch-group {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.filter-input {
  width: 340px;
}

.filter-select {
  width: 200px;
}

.filter-label {
  color: var(--muted);
  font-size: 12px;
}

.active-filter-chip {
  border: none;
  padding: 8px 12px;
  border-radius: 999px;
  background: rgba(219, 234, 254, 0.95);
  color: var(--accent);
  cursor: pointer;
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

.title-cell {
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

.title-meta {
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 0;
}

.title-meta strong {
  color: var(--ink);
}

.title-meta p {
  margin: 0;
  color: var(--muted);
  line-height: 1.6;
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
  font-size: 30px;
  color: var(--ink);
}

.drawer-hero p {
  margin: 0;
  color: var(--muted);
  line-height: 1.7;
}

.drawer-cover {
  width: 100%;
  height: 220px;
  border-radius: 22px;
  overflow: hidden;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.info-item,
.content-card {
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

.content-card {
  white-space: pre-wrap;
  line-height: 1.85;
  color: #334155;
}

.upload-panel {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.upload-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  color: var(--muted);
  font-size: 13px;
}

.switch-tip {
  color: var(--muted);
  font-size: 13px;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
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
  .drawer-hero,
  .upload-meta,
  .dialog-footer {
    flex-direction: column;
    align-items: stretch;
  }

  .page-hero,
  .filter-panel,
  .table-panel {
    border-radius: 22px;
  }

  .title-cell {
    align-items: flex-start;
  }
}
</style>
