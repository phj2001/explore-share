<template>
  <div class="activity-page">
    <section class="page-hero">
      <div class="hero-copy">
        <span class="hero-kicker">活动管理</span>
        <h1>统一发布活动安排，并把地点与时间信息联动到首页展示</h1>
        <p>支持草稿保存、正式发布、关联地点、封面图管理和活动详情查看，发布后的活动会同步展示到首页活动区。</p>
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
          placeholder="搜索活动标题、摘要、正文或地点名称"
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

        <el-select
          v-model="poiIdFilter"
          clearable
          filterable
          remote
          reserve-keyword
          class="filter-select"
          placeholder="筛选关联地点"
          :remote-method="handlePoiOptionSearch"
          @visible-change="handlePoiSelectVisibleChange"
          @change="handleSearch"
        >
          <el-option v-for="poi in poiOptions" :key="poi.id" :label="poi.name" :value="poi.id" />
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

        <el-button type="primary" :icon="Plus" @click="openCreateDialog">新建活动</el-button>
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
          <span class="panel-kicker">活动列表</span>
          <h2>近期活动</h2>
        </div>
        <el-button text @click="loadActivities">刷新列表</el-button>
      </div>

      <el-table :data="activities" v-loading="loading" stripe>
        <template #empty>
          <el-empty description="当前筛选条件下暂无活动">
            <el-button type="primary" plain @click="openCreateDialog">创建第一条活动</el-button>
          </el-empty>
        </template>

        <el-table-column prop="id" label="活动ID" width="100" />

        <el-table-column label="活动信息" min-width="320">
          <template #default="{ row }">
            <div class="title-cell">
              <el-image
                v-if="row.coverImageUrl"
                :src="resolveAssetUrl(row.coverThumbnailUrl || row.coverImageUrl)"
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

        <el-table-column label="关联地点" min-width="150" show-overflow-tooltip>
          <template #default="{ row }">
            {{ row.poiName || '未关联地点' }}
          </template>
        </el-table-column>

        <el-table-column label="活动时间" width="220">
          <template #default="{ row }">
            <div class="time-cell">
              <span>{{ formatDate(row.startTime) }}</span>
              <span>{{ formatDate(row.endTime) }}</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="状态" width="130">
          <template #default="{ row }">
            <el-tag :type="row.status === PUBLISHED_STATUS ? 'success' : 'info'" effect="plain">
              {{ row.status === PUBLISHED_STATUS ? '已发布' : '草稿' }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="280" fixed="right">
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
              <el-button size="small" type="danger" :loading="deletingId === row.id" @click="handleDelete(row)">
                删除
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-bar">
        <span>共 {{ total }} 条活动</span>
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50]"
          :total="total"
          layout="sizes, prev, pager, next"
          @current-change="loadActivities"
          @size-change="handlePageSizeChange"
        />
      </div>
    </section>

    <el-drawer v-model="drawerVisible" size="620px" :with-header="false" destroy-on-close>
      <div class="drawer-body" v-loading="detailLoading">
        <template v-if="selectedActivity">
          <div class="drawer-hero">
            <div>
              <div class="drawer-tags">
                <el-tag :type="selectedActivity.status === PUBLISHED_STATUS ? 'success' : 'info'" effect="plain">
                  {{ selectedActivity.status === PUBLISHED_STATUS ? '已发布' : '草稿' }}
                </el-tag>
                <el-tag :type="getRuntimeStatusType(selectedActivity)" effect="plain">
                  {{ getRuntimeStatusLabel(selectedActivity) }}
                </el-tag>
              </div>
              <h2>{{ selectedActivity.title }}</h2>
              <p>{{ selectedActivity.summary }}</p>
            </div>

            <div class="drawer-actions">
              <el-button plain @click="openEditDialog(selectedActivity.id)">编辑</el-button>
              <el-button
                :type="selectedActivity.status === PUBLISHED_STATUS ? 'warning' : 'success'"
                plain
                :loading="publishUpdatingId === selectedActivity.id"
                @click="togglePublish(selectedActivity)"
              >
                {{ selectedActivity.status === PUBLISHED_STATUS ? '取消发布' : '发布活动' }}
              </el-button>
            </div>
          </div>

          <el-image
            v-if="selectedActivity.coverImageUrl"
            :src="resolveAssetUrl(selectedActivity.coverImageUrl)"
            fit="cover"
            class="drawer-cover"
          />

          <div class="info-grid">
            <div class="info-item">
              <span>活动时间</span>
              <strong>{{ formatDateRange(selectedActivity.startTime, selectedActivity.endTime) }}</strong>
            </div>
            <div class="info-item">
              <span>关联地点</span>
              <strong>{{ selectedActivity.poiName || '未关联地点' }}</strong>
            </div>
            <div class="info-item">
              <span>发布时间</span>
              <strong>{{ formatDate(selectedActivity.publishedAt) }}</strong>
            </div>
            <div class="info-item">
              <span>更新时间</span>
              <strong>{{ formatDate(selectedActivity.updatedAt) }}</strong>
            </div>
          </div>

          <div class="content-card">{{ selectedActivity.content }}</div>
        </template>
      </div>
    </el-drawer>

    <el-dialog
      v-model="formDialogVisible"
      :title="isEditMode ? '编辑活动' : '新建活动'"
      width="760px"
      destroy-on-close
      @closed="resetFormState"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="96px">
        <el-form-item label="标题" prop="title">
          <el-input v-model="form.title" maxlength="120" show-word-limit placeholder="请输入活动标题" />
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
            placeholder="请输入活动正文"
          />
        </el-form-item>

        <el-form-item label="关联地点">
          <el-select
            v-model="form.poiId"
            clearable
            filterable
            remote
            reserve-keyword
            placeholder="可选关联一个地点"
            style="width: 100%"
            :remote-method="handlePoiOptionSearch"
            @visible-change="handlePoiSelectVisibleChange"
          >
            <el-option v-for="poi in poiOptions" :key="poi.id" :label="poi.name" :value="poi.id" />
          </el-select>
        </el-form-item>

        <el-form-item label="开始时间" prop="startTime">
          <el-date-picker
            v-model="form.startTime"
            type="datetime"
            placeholder="请选择开始时间"
            style="width: 100%"
            value-format="YYYY-MM-DDTHH:mm:ss"
          />
        </el-form-item>

        <el-form-item label="结束时间" prop="endTime">
          <el-date-picker
            v-model="form.endTime"
            type="datetime"
            placeholder="请选择结束时间"
            style="width: 100%"
            value-format="YYYY-MM-DDTHH:mm:ss"
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
              <el-button v-if="form.coverPreview" size="small" text type="danger" @click="clearCoverSelection">
                移除当前封面
              </el-button>
            </div>
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
  createAdminActivity,
  deleteAdminActivity,
  getAdminActivityDetail,
  getAdminActivityPage,
  updateAdminActivity,
  updateAdminActivityPublishStatus
} from '@/api/adminActivity'
import { getPOIOptions } from '@/api/poi'
import { API_ORIGIN } from '@/utils/request'

const DRAFT_STATUS = 0
const PUBLISHED_STATUS = 1
const POI_OPTION_LIMIT = 20

const route = useRoute()

const keyword = ref('')
const statusFilter = ref()
const poiIdFilter = ref()
const timeRange = ref([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const activities = ref([])
const poiOptions = ref([])
const loading = ref(false)
const drawerVisible = ref(false)
const detailLoading = ref(false)
const selectedActivity = ref(null)
const deletingId = ref(null)
const publishUpdatingId = ref(null)

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
  poiId: null,
  startTime: '',
  endTime: '',
  coverImage: null,
  coverPreview: '',
  removeCoverImage: false
})

const rules = {
  title: [{ required: true, message: '请输入活动标题', trigger: 'blur' }],
  summary: [{ required: true, message: '请输入活动摘要', trigger: 'blur' }],
  content: [{ required: true, message: '请输入活动正文', trigger: 'blur' }],
  startTime: [{ required: true, message: '请选择活动开始时间', trigger: 'change' }],
  endTime: [{ required: true, message: '请选择活动结束时间', trigger: 'change' }]
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

  if (poiIdFilter.value) {
    const poi = poiOptions.value.find((item) => item.id === poiIdFilter.value)
    items.push({ key: 'poiId', label: `地点：${poi?.name || poiIdFilter.value}` })
  }

  if (timeRange.value?.length === 2) {
    items.push({ key: 'timeRange', label: '时间范围已筛选' })
  }

  return items
})

const heroStats = computed(() => {
  const publishedCount = activities.value.filter((item) => item.status === PUBLISHED_STATUS).length
  const upcomingCount = activities.value.filter((item) => getRuntimeStatusLabel(item) === '即将开始').length

  return [
    { label: '当前总量', value: `${total.value}`, helper: '符合当前筛选条件的活动数量' },
    { label: '本页已发布', value: `${publishedCount}`, helper: '可同步展示到前台首页' },
    { label: '即将开始', value: `${upcomingCount}`, helper: '当前列表里尚未开始的活动数量' },
    { label: '筛选条件', value: `${activeFilters.value.length}`, helper: activeFilters.value.length ? '可点击下方标签快速移除' : '当前为全量浏览' }
  ]
})

const mergePoiOptions = (items = []) => {
  const optionMap = new Map(poiOptions.value.map((item) => [item.id, item]))
  items.forEach((item) => {
    if (item?.id != null) {
      optionMap.set(item.id, item)
    }
  })
  poiOptions.value = Array.from(optionMap.values())
}

const ensurePoiOption = (option) => {
  if (!option?.id) {
    return
  }
  mergePoiOptions([option])
}

const loadPoiOptions = async (keyword = '') => {
  try {
    const data = await getPOIOptions({
      keyword: keyword.trim() || undefined,
      limit: POI_OPTION_LIMIT
    })
    mergePoiOptions(data || [])
  } catch (error) {
    ElMessage.error(error.message || '加载地点列表失败')
  }
}

const handlePoiOptionSearch = async (keyword) => {
  await loadPoiOptions(keyword)
}

const handlePoiSelectVisibleChange = async (visible) => {
  if (!visible || poiOptions.value.length) {
    return
  }
  await loadPoiOptions()
}

const loadActivities = async () => {
  loading.value = true
  try {
    const [startTime, endTime] = timeRange.value || []
    const data = await getAdminActivityPage({
      keyword: keyword.value.trim() || undefined,
      status: statusFilter.value,
      poiId: poiIdFilter.value,
      startTime: startTime || undefined,
      endTime: endTime || undefined,
      page: currentPage.value - 1,
      size: pageSize.value
    })
    activities.value = data.records || []
    total.value = data.total || 0
    currentPage.value = (data.page || 0) + 1
  } catch (error) {
    ElMessage.error(error.message || '加载活动列表失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = async () => {
  currentPage.value = 1
  await loadActivities()
}

const handlePageSizeChange = async () => {
  currentPage.value = 1
  await loadActivities()
}

const resetFilters = async () => {
  keyword.value = ''
  statusFilter.value = undefined
  poiIdFilter.value = undefined
  timeRange.value = []
  currentPage.value = 1
  await loadActivities()
}

const removeFilter = async (key) => {
  if (key === 'keyword') {
    keyword.value = ''
  }
  if (key === 'status') {
    statusFilter.value = undefined
  }
  if (key === 'poiId') {
    poiIdFilter.value = undefined
  }
  if (key === 'timeRange') {
    timeRange.value = []
  }
  await handleSearch()
}

const openDetail = async (activityId) => {
  drawerVisible.value = true
  detailLoading.value = true
  try {
    selectedActivity.value = await getAdminActivityDetail(activityId)
  } catch (error) {
    drawerVisible.value = false
    selectedActivity.value = null
    ElMessage.error(error.message || '加载活动详情失败')
  } finally {
    detailLoading.value = false
  }
}

const openCreateDialog = () => {
  isEditMode.value = false
  editingId.value = null
  formDialogVisible.value = true
}

const openEditDialog = async (activityId) => {
  try {
    const detail = await getAdminActivityDetail(activityId)
    isEditMode.value = true
    editingId.value = activityId
    fillForm(detail)
    formDialogVisible.value = true
  } catch (error) {
    ElMessage.error(error.message || '加载活动详情失败')
  }
}

const fillForm = (detail) => {
  form.title = detail.title || ''
  form.summary = detail.summary || ''
  form.content = detail.content || ''
  form.poiId = detail.poiId || null
  form.startTime = detail.startTime || ''
  form.endTime = detail.endTime || ''
  form.coverImage = null
  form.coverPreview = detail.coverImageUrl || ''
  form.removeCoverImage = false
  ensurePoiOption(
    detail.poiId
      ? {
          id: detail.poiId,
          name: detail.poiName || `地点 #${detail.poiId}`,
          category: detail.poiCategory || ''
        }
      : null
  )
  coverFileList.value = detail.coverImageUrl
    ? [{ name: 'current-cover', url: resolveAssetUrl(detail.coverImageUrl) }]
    : []
}

const resetFormState = () => {
  if (form.coverPreview && form.coverPreview.startsWith('blob:')) {
    URL.revokeObjectURL(form.coverPreview)
  }
  form.title = ''
  form.summary = ''
  form.content = ''
  form.poiId = null
  form.startTime = ''
  form.endTime = ''
  form.coverImage = null
  form.coverPreview = ''
  form.removeCoverImage = false
  coverFileList.value = []
  editingId.value = null
  isEditMode.value = false
  formRef.value?.clearValidate()
}

const submitForm = async (status) => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) {
    return
  }

  if (new Date(form.endTime).getTime() < new Date(form.startTime).getTime()) {
    ElMessage.warning('活动结束时间不能早于开始时间')
    return
  }

  formSubmitting.value = true
  try {
    const payload = {
      title: form.title,
      summary: form.summary,
      content: form.content,
      poiId: form.poiId || undefined,
      startTime: form.startTime,
      endTime: form.endTime,
      status,
      coverImage: form.coverImage,
      removeCoverImage: form.removeCoverImage
    }

    let detail
    if (isEditMode.value && editingId.value) {
      detail = await updateAdminActivity(editingId.value, payload)
      ElMessage.success(status === PUBLISHED_STATUS ? '活动已更新并发布' : '活动草稿已更新')
    } else {
      detail = await createAdminActivity(payload)
      ElMessage.success(status === PUBLISHED_STATUS ? '活动已创建并发布' : '活动草稿已保存')
    }

    formDialogVisible.value = false
    await loadActivities()
    if (drawerVisible.value && selectedActivity.value?.id === detail.id) {
      selectedActivity.value = detail
    }
  } catch (error) {
    ElMessage.error(error.message || '保存活动失败')
  } finally {
    formSubmitting.value = false
  }
}

const togglePublish = async (row) => {
  const nextPublished = row.status !== PUBLISHED_STATUS
  publishUpdatingId.value = row.id
  try {
    const detail = await updateAdminActivityPublishStatus(row.id, nextPublished)
    updateRowFromDetail(row, detail)
    await loadActivities()
    if (selectedActivity.value?.id === row.id) {
      selectedActivity.value = detail
    }
    ElMessage.success(nextPublished ? '活动已发布' : '活动已转为草稿')
  } catch (error) {
    ElMessage.error(error.message || '更新发布状态失败')
  } finally {
    publishUpdatingId.value = null
  }
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm('删除活动后，其封面图和首页展示入口都会一起移除。是否继续？', '删除活动', {
      type: 'warning',
      confirmButtonText: '确认删除',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }

  deletingId.value = row.id
  try {
    await deleteAdminActivity(row.id)
    if (activities.value.length === 1 && currentPage.value > 1) {
      currentPage.value -= 1
    }
    if (selectedActivity.value?.id === row.id) {
      drawerVisible.value = false
      selectedActivity.value = null
    }
    await loadActivities()
    ElMessage.success('活动已删除')
  } catch (error) {
    ElMessage.error(error.message || '删除活动失败')
  } finally {
    deletingId.value = null
  }
}

const updateRowFromDetail = (row, detail) => {
  row.title = detail.title
  row.summary = detail.summary
  row.coverImageUrl = detail.coverImageUrl
  row.poiId = detail.poiId
  row.poiName = detail.poiName
  row.startTime = detail.startTime
  row.endTime = detail.endTime
  row.status = detail.status
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

const toDate = (value) => (value ? new Date(value) : null)

const formatDate = (value) => {
  const date = toDate(value)
  if (!date) {
    return '未发布'
  }
  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  }).format(date)
}

const formatDateRange = (startTime, endTime) => {
  const start = toDate(startTime)
  const end = toDate(endTime)
  if (!start || !end) {
    return '时间待定'
  }
  const formatter = new Intl.DateTimeFormat('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
  return `${formatter.format(start)} - ${formatter.format(end)}`
}

const getRuntimeStatusLabel = (item) => {
  const now = Date.now()
  const start = toDate(item.startTime)?.getTime()
  const end = toDate(item.endTime)?.getTime()

  if (start == null || end == null) {
    return '时间待定'
  }
  if (now < start) {
    return '即将开始'
  }
  if (now <= end) {
    return '进行中'
  }
  return '已结束'
}

const getRuntimeStatusType = (item) => {
  const label = getRuntimeStatusLabel(item)
  if (label === '进行中') {
    return 'success'
  }
  if (label === '即将开始') {
    return 'warning'
  }
  return 'info'
}

onMounted(async () => {
  await Promise.all([loadPoiOptions(), loadActivities()])
  if (route.query.openActivityId) {
    await openDetail(Number(route.query.openActivityId))
  }
})
</script>

<style scoped>
.activity-page {
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
.dialog-footer {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.filter-input {
  width: 320px;
}

.filter-select {
  width: 200px;
}

.filter-date {
  width: 360px;
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

.time-cell {
  display: flex;
  flex-direction: column;
  gap: 6px;
  color: var(--muted);
  font-size: 13px;
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

@media (max-width: 1240px) {
  .page-hero {
    grid-template-columns: 1fr;
  }

  .filter-input,
  .filter-select,
  .filter-date {
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
