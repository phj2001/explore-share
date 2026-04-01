<template>
  <div class="route-page">
    <section class="toolbar">
      <el-input
        v-model="keyword"
        clearable
        placeholder="搜索路线标题、摘要或地点名称"
        class="keyword"
        @keyup.enter="handleSearch"
        @clear="handleSearch"
      >
        <template #append>
          <el-button :icon="Search" @click="handleSearch" />
        </template>
      </el-input>

      <el-select v-model="statusFilter" clearable placeholder="状态" @change="handleSearch">
        <el-option :value="DRAFT_STATUS" label="草稿" />
        <el-option :value="PUBLISHED_STATUS" label="已发布" />
      </el-select>

      <el-select v-model="modeFilter" clearable placeholder="默认方式" @change="handleSearch">
        <el-option v-for="item in modeOptions" :key="item.value" :label="item.label" :value="item.value" />
      </el-select>

      <el-button type="primary" :icon="Plus" @click="openCreateDialog">新建路线</el-button>
      <el-button :icon="RefreshRight" @click="resetFilters">重置</el-button>
    </section>

    <section class="summary">
      <article v-for="item in heroStats" :key="item.label" class="summary-card">
        <span>{{ item.label }}</span>
        <strong>{{ item.value }}</strong>
        <em>{{ item.helper }}</em>
      </article>
    </section>

    <section class="table-wrap">
      <el-table :data="routes" v-loading="loading" stripe>
        <template #empty>
          <el-empty description="当前没有推荐路线">
            <el-button type="primary" plain @click="openCreateDialog">创建第一条路线</el-button>
          </el-empty>
        </template>

        <el-table-column prop="sortOrder" label="排序" width="80" />
        <el-table-column label="路线" min-width="360">
          <template #default="{ row }">
            <div class="route-main">
              <el-image v-if="row.coverImageUrl" :src="resolveAssetUrl(row.coverImageUrl)" fit="cover" class="cover" />
              <div class="route-meta">
                <strong>{{ row.title }}</strong>
                <p>{{ row.summary }}</p>
                <span>{{ row.startPoiName || '未设置起点' }} -> {{ row.endPoiName || '未设置终点' }}</span>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="结构" width="150">
          <template #default="{ row }">
            <div class="stack">
              <span>{{ row.defaultModeLabel }}</span>
              <span>{{ row.waypointCount }} 个地点</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === PUBLISHED_STATUS ? 'success' : 'info'" effect="plain">
              {{ row.status === PUBLISHED_STATUS ? '已发布' : '草稿' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="300" fixed="right">
          <template #default="{ row }">
            <div class="actions">
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
              <el-button size="small" type="danger" :loading="deletingId === row.id" @click="handleDelete(row)">删除</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination">
        <span>共 {{ total }} 条推荐路线</span>
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50]"
          :total="total"
          layout="sizes, prev, pager, next"
          @current-change="loadRoutes"
          @size-change="handlePageSizeChange"
        />
      </div>
    </section>

    <el-drawer v-model="drawerVisible" size="620px" :with-header="false" destroy-on-close>
      <div class="detail" v-loading="detailLoading">
        <template v-if="selectedRoute">
          <div class="detail-head">
            <div class="tags">
              <el-tag :type="selectedRoute.status === PUBLISHED_STATUS ? 'success' : 'info'" effect="plain">
                {{ selectedRoute.status === PUBLISHED_STATUS ? '已发布' : '草稿' }}
              </el-tag>
              <el-tag effect="plain">{{ selectedRoute.defaultModeLabel }}</el-tag>
              <el-tag effect="plain">{{ selectedRoute.waypoints.length }} 个地点</el-tag>
            </div>
            <h2>{{ selectedRoute.title }}</h2>
            <p>{{ selectedRoute.summary }}</p>
          </div>
          <el-image v-if="selectedRoute.coverImageUrl" :src="resolveAssetUrl(selectedRoute.coverImageUrl)" fit="cover" class="detail-cover" />
          <div class="detail-grid">
            <div class="info-card"><span>默认方式</span><strong>{{ selectedRoute.defaultModeLabel }}</strong></div>
            <div class="info-card"><span>推荐排序</span><strong>{{ selectedRoute.sortOrder }}</strong></div>
            <div class="info-card"><span>发布时间</span><strong>{{ formatDate(selectedRoute.publishedAt) }}</strong></div>
            <div class="info-card"><span>更新时间</span><strong>{{ formatDate(selectedRoute.updatedAt) }}</strong></div>
          </div>
          <div class="text-card">
            <h3>路线说明</h3>
            <p>{{ selectedRoute.description }}</p>
          </div>
          <div class="text-card">
            <h3>地点顺序</h3>
            <div class="poi-list">
              <div v-for="(point, index) in selectedRoute.waypoints" :key="`${point.poiId}-${index}`" class="poi-row">
                <span class="idx">{{ index + 1 }}</span>
                <div>
                  <strong>{{ point.poiName }}</strong>
                  <p>{{ point.poiCategory || '未分类' }}</p>
                </div>
              </div>
            </div>
          </div>
        </template>
      </div>
    </el-drawer>

    <el-dialog v-model="formDialogVisible" :title="isEditMode ? '编辑推荐路线' : '新建推荐路线'" width="820px" destroy-on-close @closed="resetFormState">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="92px">
        <el-form-item label="路线标题" prop="title"><el-input v-model="form.title" maxlength="120" show-word-limit /></el-form-item>
        <el-form-item label="路线摘要" prop="summary"><el-input v-model="form.summary" type="textarea" :rows="3" maxlength="220" show-word-limit /></el-form-item>
        <el-form-item label="路线说明" prop="description"><el-input v-model="form.description" type="textarea" :rows="7" maxlength="10000" show-word-limit /></el-form-item>
        <el-form-item label="推荐语"><el-input v-model="form.recommendationText" maxlength="100" show-word-limit /></el-form-item>
        <el-form-item label="默认方式" prop="defaultMode"><el-segmented v-model="form.defaultMode" :options="modeOptions" /></el-form-item>
        <el-form-item label="推荐排序"><el-input-number v-model="form.sortOrder" :min="1" :max="9999" controls-position="right" /></el-form-item>
        <el-form-item label="路线地点" prop="poiIds">
          <div class="editor">
            <div class="editor-bar">
              <el-select v-model="selectedPoiIdToAdd" filterable clearable placeholder="选择一个地点加入路线" class="poi-picker">
                <el-option v-for="poi in availablePoiOptions" :key="poi.id" :label="poi.name" :value="poi.id" />
              </el-select>
              <el-button :icon="Plus" @click="handleAddPoi">添加地点</el-button>
            </div>
            <div v-if="form.poiIds.length" class="poi-edit-list">
              <div v-for="(poiId, index) in form.poiIds" :key="`${poiId}-${index}`" class="poi-edit-row">
                <div class="poi-main">
                  <span class="idx">{{ index + 1 }}</span>
                  <div>
                    <strong>{{ getPoiName(poiId) }}</strong>
                    <p>{{ getPoiCategory(poiId) }}</p>
                  </div>
                </div>
                <div class="actions">
                  <el-button text :disabled="index === 0" @click="movePoi(index, -1)">上移</el-button>
                  <el-button text :disabled="index === form.poiIds.length - 1" @click="movePoi(index, 1)">下移</el-button>
                  <el-button text type="danger" @click="removePoi(index)">移除</el-button>
                </div>
              </div>
            </div>
            <el-empty v-else description="请至少加入两个地点，系统会按这里的顺序规划整条路线" />
          </div>
        </el-form-item>
        <el-form-item label="封面图">
          <div class="editor">
            <el-upload
              v-model:file-list="coverFileList"
              :auto-upload="false"
              :limit="1"
              accept=".jpg,.jpeg,.png,.webp"
              list-type="picture-card"
              @change="handleCoverChange"
              @remove="handleCoverRemove"
              @exceed="handleCoverExceed"
            >
              <el-icon><Plus /></el-icon>
            </el-upload>
            <div class="cover-meta">
              <span>支持 JPG / PNG / WEBP，单张不超过 5MB</span>
              <el-button v-if="form.coverPreview" size="small" text type="danger" @click="clearCoverSelection">移除当前封面</el-button>
            </div>
          </div>
        </el-form-item>
      </el-form>

      <template #footer>
        <div class="actions">
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
import { API_ORIGIN } from '@/utils/request'
import { getAllPOIs } from '@/api/poi'
import {
  createAdminRecommendedRoute,
  deleteAdminRecommendedRoute,
  getAdminRecommendedRouteDetail,
  getAdminRecommendedRoutePage,
  updateAdminRecommendedRoute,
  updateAdminRecommendedRoutePublishStatus
} from '@/api/adminRecommendedRoute'

const DRAFT_STATUS = 0
const PUBLISHED_STATUS = 1
const modeOptions = [
  { label: '步行', value: 'walking' },
  { label: '驾车', value: 'driving' },
  { label: '骑行', value: 'bicycling' }
]

const route = useRoute()

const keyword = ref('')
const statusFilter = ref()
const modeFilter = ref()
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const routes = ref([])
const poiOptions = ref([])
const loading = ref(false)
const drawerVisible = ref(false)
const detailLoading = ref(false)
const selectedRoute = ref(null)
const deletingId = ref(null)
const publishUpdatingId = ref(null)

const formDialogVisible = ref(false)
const formSubmitting = ref(false)
const isEditMode = ref(false)
const editingId = ref(null)
const formRef = ref(null)
const coverFileList = ref([])
const selectedPoiIdToAdd = ref()

const form = reactive({
  title: '',
  summary: '',
  description: '',
  recommendationText: '',
  defaultMode: 'walking',
  sortOrder: 1,
  poiIds: [],
  coverImage: null,
  coverPreview: '',
  removeCoverImage: false
})

const rules = {
  title: [{ required: true, message: '请输入路线标题', trigger: 'blur' }],
  summary: [{ required: true, message: '请输入路线摘要', trigger: 'blur' }],
  description: [{ required: true, message: '请输入路线说明', trigger: 'blur' }],
  defaultMode: [{ required: true, message: '请选择默认方式', trigger: 'change' }],
  poiIds: [{
    validator: (_, value, callback) => {
      if (!Array.isArray(value) || value.length < 2) {
        callback(new Error('请至少选择两个地点'))
        return
      }
      callback()
    },
    trigger: 'change'
  }]
}

const availablePoiOptions = computed(() => poiOptions.value.filter((poi) => !form.poiIds.includes(poi.id)))
const activeFilters = computed(() => {
  const items = []
  if (keyword.value.trim()) items.push({ key: 'keyword', label: `关键词：${keyword.value.trim()}` })
  if (statusFilter.value === DRAFT_STATUS) items.push({ key: 'status', label: '状态：草稿' })
  if (statusFilter.value === PUBLISHED_STATUS) items.push({ key: 'status', label: '状态：已发布' })
  if (modeFilter.value) items.push({ key: 'mode', label: `方式：${modeOptions.find((item) => item.value === modeFilter.value)?.label || modeFilter.value}` })
  return items
})

const heroStats = computed(() => {
  const publishedCount = routes.value.filter((item) => item.status === PUBLISHED_STATUS).length
  const totalPoints = routes.value.reduce((sum, item) => sum + (item.waypointCount || 0), 0)
  return [
    { label: '当前总量', value: `${total.value}`, helper: '符合当前筛选条件的推荐路线数量' },
    { label: '本页已发布', value: `${publishedCount}`, helper: '发布后会同步展示到前台推荐路线区' },
    { label: '本页地点数', value: `${totalPoints}`, helper: '当前列表内路线覆盖的地点总数' },
    { label: '筛选条件', value: `${activeFilters.value.length}`, helper: activeFilters.value.length ? '可点击上方筛选后重新查询' : '当前为全量浏览' }
  ]
})

const resolveAssetUrl = (value) => {
  if (!value) return ''
  if (/^https?:\/\//i.test(value)) return value
  return `${API_ORIGIN}${value.startsWith('/') ? value : `/${value}`}`
}

const getPoiById = (poiId) => poiOptions.value.find((item) => item.id === poiId)
const getPoiName = (poiId) => getPoiById(poiId)?.name || `地点 #${poiId}`
const getPoiCategory = (poiId) => getPoiById(poiId)?.category || '未分类'

const loadPoiOptions = async () => {
  try {
    poiOptions.value = await getAllPOIs()
  } catch (error) {
    ElMessage.error(error.message || '加载地点列表失败')
  }
}

const loadRoutes = async () => {
  loading.value = true
  try {
    const data = await getAdminRecommendedRoutePage({
      keyword: keyword.value.trim() || undefined,
      status: statusFilter.value,
      defaultMode: modeFilter.value || undefined,
      page: currentPage.value - 1,
      size: pageSize.value
    })
    routes.value = data.records || []
    total.value = data.total || 0
    currentPage.value = (data.page || 0) + 1
  } catch (error) {
    ElMessage.error(error.message || '加载推荐路线失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = async () => {
  currentPage.value = 1
  await loadRoutes()
}

const handlePageSizeChange = async () => {
  currentPage.value = 1
  await loadRoutes()
}

const resetFilters = async () => {
  keyword.value = ''
  statusFilter.value = undefined
  modeFilter.value = undefined
  currentPage.value = 1
  await loadRoutes()
}

const openDetail = async (routeId) => {
  drawerVisible.value = true
  detailLoading.value = true
  try {
    selectedRoute.value = await getAdminRecommendedRouteDetail(routeId)
  } catch (error) {
    drawerVisible.value = false
    selectedRoute.value = null
    ElMessage.error(error.message || '加载路线详情失败')
  } finally {
    detailLoading.value = false
  }
}

const openCreateDialog = () => {
  isEditMode.value = false
  editingId.value = null
  formDialogVisible.value = true
}

const fillForm = (detail) => {
  form.title = detail.title || ''
  form.summary = detail.summary || ''
  form.description = detail.description || ''
  form.recommendationText = detail.recommendationText || ''
  form.defaultMode = detail.defaultMode || 'walking'
  form.sortOrder = detail.sortOrder || 1
  form.poiIds = (detail.waypoints || []).map((item) => item.poiId)
  form.coverImage = null
  form.coverPreview = detail.coverImageUrl || ''
  form.removeCoverImage = false
  coverFileList.value = detail.coverImageUrl ? [{ name: 'current-cover', url: resolveAssetUrl(detail.coverImageUrl) }] : []
}

const openEditDialog = async (routeId) => {
  try {
    const detail = await getAdminRecommendedRouteDetail(routeId)
    isEditMode.value = true
    editingId.value = routeId
    fillForm(detail)
    formDialogVisible.value = true
  } catch (error) {
    ElMessage.error(error.message || '加载路线详情失败')
  }
}

const resetFormState = () => {
  if (form.coverPreview && form.coverPreview.startsWith('blob:')) {
    URL.revokeObjectURL(form.coverPreview)
  }
  form.title = ''
  form.summary = ''
  form.description = ''
  form.recommendationText = ''
  form.defaultMode = 'walking'
  form.sortOrder = 1
  form.poiIds = []
  form.coverImage = null
  form.coverPreview = ''
  form.removeCoverImage = false
  selectedPoiIdToAdd.value = undefined
  coverFileList.value = []
  editingId.value = null
  isEditMode.value = false
  formRef.value?.clearValidate()
}

const handleAddPoi = () => {
  if (!selectedPoiIdToAdd.value) {
    ElMessage.warning('请先选择一个地点')
    return
  }
  if (form.poiIds.includes(selectedPoiIdToAdd.value)) {
    ElMessage.warning('该地点已经在路线中')
    return
  }
  form.poiIds.push(selectedPoiIdToAdd.value)
  selectedPoiIdToAdd.value = undefined
  formRef.value?.validateField('poiIds').catch(() => {})
}

const movePoi = (index, offset) => {
  const nextIndex = index + offset
  if (nextIndex < 0 || nextIndex >= form.poiIds.length) return
  const nextPoiIds = [...form.poiIds]
  const [target] = nextPoiIds.splice(index, 1)
  nextPoiIds.splice(nextIndex, 0, target)
  form.poiIds = nextPoiIds
}

const removePoi = (index) => {
  form.poiIds.splice(index, 1)
  formRef.value?.validateField('poiIds').catch(() => {})
}

const submitForm = async (status) => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  formSubmitting.value = true
  try {
    const payload = {
      title: form.title,
      summary: form.summary,
      description: form.description,
      recommendationText: form.recommendationText.trim() || null,
      defaultMode: form.defaultMode,
      sortOrder: form.sortOrder,
      poiIds: form.poiIds,
      status,
      coverImage: form.coverImage,
      removeCoverImage: form.removeCoverImage
    }

    let detail
    if (isEditMode.value && editingId.value) {
      detail = await updateAdminRecommendedRoute(editingId.value, payload)
      ElMessage.success(status === PUBLISHED_STATUS ? '推荐路线已更新并发布' : '推荐路线草稿已更新')
    } else {
      detail = await createAdminRecommendedRoute(payload)
      ElMessage.success(status === PUBLISHED_STATUS ? '推荐路线已创建并发布' : '推荐路线草稿已保存')
    }

    formDialogVisible.value = false
    await loadRoutes()
    if (drawerVisible.value && selectedRoute.value?.id === detail.id) {
      selectedRoute.value = detail
    }
  } catch (error) {
    ElMessage.error(error.message || '保存推荐路线失败')
  } finally {
    formSubmitting.value = false
  }
}

const togglePublish = async (row) => {
  const nextPublished = row.status !== PUBLISHED_STATUS
  publishUpdatingId.value = row.id
  try {
    const detail = await updateAdminRecommendedRoutePublishStatus(row.id, nextPublished)
    await loadRoutes()
    if (selectedRoute.value?.id === row.id) {
      selectedRoute.value = detail
    }
    ElMessage.success(nextPublished ? '推荐路线已发布' : '推荐路线已转为草稿')
  } catch (error) {
    ElMessage.error(error.message || '更新发布状态失败')
  } finally {
    publishUpdatingId.value = null
  }
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm('删除路线后，封面图和前台展示入口都会一起移除。是否继续？', '删除推荐路线', {
      type: 'warning',
      confirmButtonText: '确认删除',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }

  deletingId.value = row.id
  try {
    await deleteAdminRecommendedRoute(row.id)
    if (routes.value.length === 1 && currentPage.value > 1) {
      currentPage.value -= 1
    }
    if (selectedRoute.value?.id === row.id) {
      drawerVisible.value = false
      selectedRoute.value = null
    }
    await loadRoutes()
    ElMessage.success('推荐路线已删除')
  } catch (error) {
    ElMessage.error(error.message || '删除推荐路线失败')
  } finally {
    deletingId.value = null
  }
}

const handleCoverChange = (uploadFile, uploadFiles) => {
  const nextFile = uploadFile.raw
  if (!nextFile) return
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

const formatDate = (value) => {
  if (!value) return '未发布'
  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  }).format(new Date(value))
}

onMounted(async () => {
  await Promise.all([loadPoiOptions(), loadRoutes()])
  if (route.query.openRouteId) {
    await openDetail(Number(route.query.openRouteId))
  }
})
</script>

<style scoped>
.route-page {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.toolbar,
.summary,
.table-wrap {
  padding: 18px;
  border-radius: 24px;
  border: 1px solid rgba(148, 163, 184, 0.18);
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 16px 40px rgba(15, 23, 42, 0.06);
}

.toolbar,
.summary,
.actions,
.tags,
.editor-bar,
.cover-meta {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  align-items: center;
}

.keyword {
  width: 340px;
}

.summary {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.summary-card {
  padding: 18px;
  border-radius: 18px;
  background: linear-gradient(180deg, rgba(248, 250, 252, 0.96), rgba(239, 246, 255, 0.92));
}

.summary-card span,
.summary-card em {
  display: block;
  color: #64748b;
  font-style: normal;
}

.summary-card strong {
  display: block;
  margin: 10px 0 8px;
  color: #0f172a;
  font-size: 28px;
}

.pagination {
  padding-top: 18px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  color: #64748b;
}

.route-main,
.poi-main {
  display: flex;
  gap: 12px;
  align-items: flex-start;
}

.cover,
.detail-cover {
  width: 84px;
  height: 60px;
  border-radius: 14px;
  overflow: hidden;
  flex-shrink: 0;
}

.route-meta,
.stack,
.detail-head,
.detail,
.editor,
.poi-list,
.poi-edit-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.route-meta strong,
.detail-head h2,
.poi-row strong,
.poi-edit-row strong {
  color: #0f172a;
}

.route-meta p,
.route-meta span,
.stack span,
.detail-head p,
.poi-row p,
.poi-edit-row p,
.text-card p {
  margin: 0;
  color: #64748b;
  line-height: 1.6;
}

.detail {
  padding: 24px;
}

.detail-cover {
  width: 100%;
  height: 220px;
}

.detail-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.info-card,
.text-card {
  padding: 16px;
  border-radius: 18px;
  background: rgba(248, 250, 252, 0.92);
}

.info-card span {
  display: block;
  color: #64748b;
  font-size: 13px;
}

.info-card strong {
  display: block;
  margin-top: 10px;
  color: #0f172a;
}

.text-card h3 {
  margin: 0 0 10px;
  color: #0f172a;
}

.poi-row,
.poi-edit-row {
  display: flex;
  justify-content: space-between;
  gap: 14px;
  padding: 12px 14px;
  border-radius: 16px;
  background: rgba(248, 250, 252, 0.92);
}

.idx {
  width: 28px;
  height: 28px;
  border-radius: 999px;
  background: rgba(14, 165, 233, 0.14);
  color: #0369a1;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  flex-shrink: 0;
}

.poi-picker {
  flex: 1;
}

.cover-meta {
  justify-content: space-between;
  color: #64748b;
  font-size: 13px;
}

@media (max-width: 1024px) {
  .summary {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .toolbar,
  .summary,
  .pagination,
  .detail-grid,
  .cover-meta,
  .editor-bar,
  .poi-row,
  .poi-edit-row {
    flex-direction: column;
    align-items: stretch;
  }

  .summary,
  .detail-grid {
    grid-template-columns: 1fr;
  }

  .keyword {
    width: 100%;
  }
}
</style>
