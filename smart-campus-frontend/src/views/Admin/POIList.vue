<template>
  <div class="poi-page">
    <section class="page-hero">
      <div class="hero-copy">
        <span class="hero-kicker">地点管理</span>
        <h1>统一维护地点名称、分类、坐标与基础说明信息</h1>
        <p>
          这里集中管理地图中的 POI 数据，支持按名称与分类筛选，并可直接进行新增、编辑、删除和批量导入。
        </p>
      </div>

      <div class="hero-stats">
        <article class="hero-stat">
          <span>地点总数</span>
          <strong>{{ totalAll }}</strong>
          <em>当前系统中已录入的全部地点数量</em>
        </article>
        <article class="hero-stat">
          <span>筛选结果</span>
          <strong>{{ total }}</strong>
          <em>符合当前筛选条件的地点数量</em>
        </article>
        <article class="hero-stat">
          <span>分类总数</span>
          <strong>{{ poiStore.categories.length }}</strong>
          <em>当前可用的地点分类数量</em>
        </article>
      </div>
    </section>

    <section class="filter-panel">
      <el-input
        v-model="searchText"
        clearable
        placeholder="搜索地点名称"
        class="filter-input"
        @keyup.enter="handleSearch"
        @clear="handleSearch"
      >
        <template #append>
          <el-button :icon="Search" @click="handleSearch" />
        </template>
      </el-input>

      <el-select
        v-model="selectedCategory"
        clearable
        placeholder="筛选分类"
        class="filter-select"
        @change="handleCategoryChange"
      >
        <el-option
          v-for="category in poiStore.categories"
          :key="category"
          :label="category"
          :value="category"
        />
      </el-select>

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

      <div class="toolbar-actions">
        <el-button :icon="RefreshRight" @click="reloadData">刷新列表</el-button>
        <el-button :icon="Upload" @click="openImportDialog">批量导入</el-button>
        <el-button @click="resetFilters">重置</el-button>
        <el-button type="primary" :icon="Plus" @click="handleCreate">新增地点</el-button>
      </div>
    </section>

    <section class="table-panel">
      <div class="panel-head">
        <div>
          <span class="panel-kicker">地点列表</span>
          <h2>当前地点数据</h2>
        </div>
        <span class="panel-note">采用服务端分页加载，避免全量 POI 导致后台卡顿</span>
      </div>

      <el-table :data="pagedPOIList" v-loading="loading" stripe>
        <template #empty>
          <el-empty description="当前条件下暂无地点数据">
            <el-button type="primary" plain @click="resetFilters">清空筛选</el-button>
          </el-empty>
        </template>

        <el-table-column prop="id" label="ID" width="90" sortable />

        <el-table-column label="地点信息" min-width="280">
          <template #default="{ row }">
            <div class="poi-main">
              <strong>{{ row.name }}</strong>
              <PoiCategoryBadge :category="row.category" />
              <p>{{ row.description || '暂无地点说明' }}</p>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="坐标" min-width="220">
          <template #default="{ row }">
            <div class="coordinate-cell">
              <span>纬度 {{ formatCoordinate(row.latitude) }}</span>
              <span>经度 {{ formatCoordinate(row.longitude) }}</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <div class="action-group">
              <el-button size="small" @click="handleEdit(row)">编辑</el-button>
              <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-bar">
        <span class="total-text">共 {{ total }} 条，当前第 {{ currentPage }} 页</span>
        <div class="pagination-actions">
          <el-select v-model="pageSize" class="page-size-select" @change="handlePageSizeChange">
            <el-option label="20 条/页" :value="20" />
            <el-option label="50 条/页" :value="50" />
            <el-option label="100 条/页" :value="100" />
          </el-select>
          <el-pagination
            v-model:current-page="currentPage"
            :page-size="pageSize"
            :total="total"
            layout="prev, pager, next"
            @current-change="handlePageChange"
          />
        </div>
      </div>
    </section>

    <el-dialog
      v-model="importDialogVisible"
      title="批量导入 POI"
      width="720px"
      destroy-on-close
      @closed="resetImportState"
    >
      <div class="import-dialog">
        <el-alert type="info" show-icon :closable="false">
          <template #title>
            仅支持 UTF-8 编码的 CSV 文件。必填列为 `name/名称`、`latitude/纬度`、`longitude/经度`，可选列为
            `category/分类`、`description/描述`。
          </template>
        </el-alert>

        <div class="import-template-row">
          <span>建议先下载模板，再按模板整理数据。</span>
          <a class="template-link" href="/templates/poi-import-template.csv" download>
            <el-button text type="primary" :icon="Download">下载 CSV 模板</el-button>
          </a>
        </div>

        <el-upload
          ref="uploadRef"
          drag
          :auto-upload="false"
          :limit="1"
          accept=".csv,text/csv"
          :file-list="uploadFileList"
          :on-change="handleImportFileChange"
          :on-remove="handleImportRemove"
          :on-exceed="handleImportExceed"
        >
          <el-icon class="el-icon--upload">
            <Upload />
          </el-icon>
          <div class="el-upload__text">
            将 CSV 文件拖到此处，或 <em>点击选择文件</em>
          </div>
          <template #tip>
            <div class="upload-tip">
              文件建议小于 10MB；当前导入会按“名称 + 分类 + 坐标”判断重复项。
            </div>
          </template>
        </el-upload>

        <div class="import-options">
          <el-checkbox v-model="replaceExisting">
            导入前先清空现有 POI 数据
          </el-checkbox>
          <el-checkbox v-model="skipDuplicates">
            自动跳过重复项
          </el-checkbox>
        </div>

        <el-alert
          v-if="replaceExisting"
          type="warning"
          show-icon
          :closable="false"
          title="你已勾选“先清空再导入”，提交后会先删除现有 POI，再写入本次文件中的有效记录。"
        />

        <div v-if="importResult" class="import-result-card">
          <div class="import-result-head">
            <strong>最近一次导入结果</strong>
            <span>{{ importResult.fileName || '未命名文件' }}</span>
          </div>

          <el-descriptions :column="2" border>
            <el-descriptions-item label="文件总行数">{{ importResult.totalRows }}</el-descriptions-item>
            <el-descriptions-item label="成功导入">{{ importResult.importedCount }}</el-descriptions-item>
            <el-descriptions-item label="跳过总数">{{ importResult.skippedCount }}</el-descriptions-item>
            <el-descriptions-item label="重复项">{{ importResult.duplicateCount }}</el-descriptions-item>
            <el-descriptions-item label="无效行">{{ importResult.invalidCount }}</el-descriptions-item>
            <el-descriptions-item label="空白行">{{ importResult.emptyRowCount }}</el-descriptions-item>
            <el-descriptions-item label="清空旧数据">{{ importResult.clearedCount }}</el-descriptions-item>
            <el-descriptions-item label="导入模式">
              {{ importResult.replaceExisting ? '清空后重导' : '追加导入' }}
            </el-descriptions-item>
          </el-descriptions>

          <div v-if="importResult.errors?.length" class="import-error-list">
            <strong>前 {{ importResult.errors.length }} 条错误示例</strong>
            <ul>
              <li v-for="item in importResult.errors" :key="`${item.rowNumber}-${item.name || 'empty'}`">
                第 {{ item.rowNumber }} 行
                <template v-if="item.name">（{{ item.name }}）</template>
                ：{{ item.message }}
              </li>
            </ul>
          </div>
        </div>
      </div>

      <template #footer>
        <div class="dialog-actions">
          <el-button @click="importDialogVisible = false">关闭</el-button>
          <el-button type="primary" :loading="importing" @click="handleImportSubmit">
            开始导入
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Download, Plus, RefreshRight, Search, Upload } from '@element-plus/icons-vue'
import { deletePOI, getPOICount, getPOIPage, importPOIs } from '@/api/poi'
import { usePOIStore } from '@/stores/poi'
import PoiCategoryBadge from '@/components/common/PoiCategoryBadge.vue'

const router = useRouter()
const route = useRoute()
const poiStore = usePOIStore()

const searchText = ref('')
const selectedCategory = ref('')
const currentPage = ref(1)
const pageSize = ref(20)
const pagedPOIList = ref([])
const total = ref(0)
const totalAll = ref(0)
const loading = ref(false)

const importDialogVisible = ref(false)
const importing = ref(false)
const replaceExisting = ref(false)
const skipDuplicates = ref(true)
const importResult = ref(null)
const importFile = ref(null)
const uploadFileList = ref([])
const uploadRef = ref()

const activeFilters = computed(() => {
  const filters = []

  if (searchText.value.trim()) {
    filters.push({
      key: 'keyword',
      label: `关键词：${searchText.value.trim()}`
    })
  }

  if (selectedCategory.value) {
    filters.push({
      key: 'category',
      label: `分类：${selectedCategory.value}`
    })
  }

  return filters
})

const loadMetaData = async () => {
  try {
    const [, count] = await Promise.all([
      poiStore.fetchCategories(),
      getPOICount()
    ])
    totalAll.value = count || 0
  } catch (error) {
    ElMessage.error(error.message || '加载地点统计数据失败')
    throw error
  }
}

const loadPageData = async () => {
  loading.value = true
  try {
    const response = await getPOIPage({
      keyword: searchText.value.trim() || undefined,
      category: selectedCategory.value || undefined,
      page: currentPage.value - 1,
      size: pageSize.value
    })

    pagedPOIList.value = response?.records || []
    total.value = response?.total || 0
  } catch (error) {
    ElMessage.error(error.message || '加载地点列表失败')
    throw error
  } finally {
    loading.value = false
  }
}

const loadData = async () => {
  await Promise.all([
    loadMetaData(),
    loadPageData()
  ])
}

const applyRouteFilters = () => {
  const routeCategory = typeof route.query.category === 'string' ? route.query.category : ''
  if (routeCategory) {
    selectedCategory.value = routeCategory
  }
}

const handleSearch = async () => {
  currentPage.value = 1
  await loadPageData()
}

const handleCategoryChange = async () => {
  currentPage.value = 1
  await loadPageData()
}

const handlePageChange = async (page) => {
  currentPage.value = page
  await loadPageData()
}

const handlePageSizeChange = async () => {
  currentPage.value = 1
  await loadPageData()
}

const reloadData = async () => {
  await loadData()
}

const resetFilters = async () => {
  searchText.value = ''
  selectedCategory.value = ''
  currentPage.value = 1
  await loadPageData()
}

const removeFilter = async (key) => {
  if (key === 'keyword') {
    searchText.value = ''
  }

  if (key === 'category') {
    selectedCategory.value = ''
  }

  currentPage.value = 1
  await loadPageData()
}

const handleCreate = () => {
  router.push('/admin/poi/create')
}

const handleEdit = (row) => {
  router.push(`/admin/poi/edit/${row.id}`)
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定删除地点“${row.name}”吗？删除后该地点将不再在地图和相关业务中展示。`,
      '删除地点',
      {
        type: 'warning',
        confirmButtonText: '确认删除',
        cancelButtonText: '取消'
      }
    )

    await deletePOI(row.id)
    totalAll.value = Math.max(totalAll.value - 1, 0)

    if (pagedPOIList.value.length === 1 && currentPage.value > 1) {
      currentPage.value -= 1
    }

    await loadPageData()
    ElMessage.success('地点已删除')
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error(error.message || '删除地点失败')
    }
  }
}

const formatCoordinate = (value) => {
  const numericValue = Number(value)
  return Number.isFinite(numericValue) ? numericValue.toFixed(6) : '--'
}

const openImportDialog = () => {
  importDialogVisible.value = true
}

const validateImportFile = (file) => {
  const isCsv = file?.name?.toLowerCase().endsWith('.csv')
  const isReasonableSize = (file?.size || 0) <= 10 * 1024 * 1024

  if (!isCsv) {
    ElMessage.error('仅支持上传 CSV 文件')
    return false
  }

  if (!isReasonableSize) {
    ElMessage.error('文件不能超过 10MB')
    return false
  }

  return true
}

const handleImportFileChange = (uploadFile) => {
  const rawFile = uploadFile.raw
  if (!rawFile) {
    return
  }

  if (!validateImportFile(rawFile)) {
    resetImportFileState()
    return
  }

  importFile.value = rawFile
  uploadFileList.value = [uploadFile]
}

const handleImportRemove = () => {
  importFile.value = null
  uploadFileList.value = []
}

const handleImportExceed = (files) => {
  if (!files?.length) {
    return
  }

  const nextFile = files[0]
  if (!validateImportFile(nextFile)) {
    resetImportFileState()
    return
  }

  resetImportFileState()
  importFile.value = nextFile
  uploadFileList.value = [{
    name: nextFile.name,
    size: nextFile.size,
    status: 'ready',
    raw: nextFile
  }]
}

const resetImportFileState = () => {
  importFile.value = null
  uploadFileList.value = []
  uploadRef.value?.clearFiles()
}

const resetImportState = () => {
  importing.value = false
  replaceExisting.value = false
  skipDuplicates.value = true
  importResult.value = null
  resetImportFileState()
}

const handleImportSubmit = async () => {
  if (!importFile.value) {
    ElMessage.warning('请先选择要导入的 CSV 文件')
    return
  }

  if (replaceExisting.value) {
    try {
      await ElMessageBox.confirm(
        '当前已勾选“导入前先清空现有 POI 数据”。提交后会先删除现有地点，再导入本次文件中的有效记录，是否继续？',
        '确认批量导入',
        {
          type: 'warning',
          confirmButtonText: '继续导入',
          cancelButtonText: '取消'
        }
      )
    } catch {
      return
    }
  }

  importing.value = true
  try {
    const result = await importPOIs(importFile.value, {
      replaceExisting: replaceExisting.value,
      skipDuplicates: skipDuplicates.value
    })

    importResult.value = result
    currentPage.value = 1
    await loadData()

    ElMessage.success(`导入完成，成功写入 ${result.importedCount} 条记录`)
  } catch (error) {
    ElMessage.error(error.message || '批量导入失败')
  } finally {
    importing.value = false
  }
}

onMounted(async () => {
  applyRouteFilters()
  await loadData()
})
</script>

<style scoped>
.poi-page {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.page-hero {
  display: grid;
  grid-template-columns: minmax(0, 1.3fr) minmax(280px, 0.9fr);
  gap: 18px;
}

.hero-copy p {
  max-width: 640px;
}

.hero-stats {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.hero-stat {
  padding: 18px;
}

.filter-panel {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
}

.filter-input {
  width: 320px;
}

.filter-select {
  width: 190px;
}

.active-filter-list {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.filter-label {
  color: var(--admin-text-muted);
  font-size: 12px;
}

.active-filter-chip {
  border: none;
  cursor: pointer;
  padding: 8px 12px;
  border-radius: 999px;
  background: var(--admin-accent-soft);
  color: var(--admin-accent-strong);
  font-size: 12px;
}

.toolbar-actions,
.pagination-actions,
.action-group,
.dialog-actions,
.import-template-row,
.import-options {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.toolbar-actions {
  margin-left: auto;
}

.poi-main {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.poi-main strong {
  color: var(--admin-text);
  font-size: 14px;
}

.poi-main span,
.coordinate-cell,
.total-text {
  color: var(--admin-text-muted);
  font-size: 12px;
}

.poi-main p {
  margin: 0;
  color: var(--ink-600);
  font-size: 13px;
  line-height: 1.65;
}

.coordinate-cell {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.page-size-select {
  width: 124px;
}

.pagination-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 18px 4px 8px;
}

.import-dialog {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.import-template-row {
  justify-content: space-between;
}

.template-link {
  text-decoration: none;
}

.upload-tip {
  color: var(--admin-text-muted);
  font-size: 12px;
}

.import-options {
  justify-content: space-between;
}

.import-result-card {
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding: 18px;
  border-radius: 14px;
  background: var(--admin-panel);
  border: 1px solid var(--admin-border);
}

.import-result-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.import-result-head strong {
  color: var(--admin-text);
}

.import-result-head span {
  color: var(--admin-text-muted);
  font-size: 12px;
}

.import-error-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.import-error-list strong {
  color: #8b2f35;
}

.import-error-list ul {
  margin: 0;
  padding-left: 20px;
  color: #8b2f35;
  line-height: 1.7;
}

@media (max-width: 1080px) {
  .page-hero {
    grid-template-columns: 1fr;
  }

  .hero-stats {
    grid-template-columns: 1fr;
  }

  .filter-input {
    width: 100%;
  }

  .import-options,
  .import-template-row {
    align-items: stretch;
    flex-direction: column;
  }
}

@media (max-width: 760px) {
  .filter-panel,
  .pagination-bar {
    flex-direction: column;
    align-items: stretch;
  }

  .filter-select,
  .page-size-select {
    width: 100%;
  }

  .toolbar-actions {
    margin-left: 0;
  }

  .pagination-actions {
    flex-direction: column;
    align-items: stretch;
  }

  .import-result-head {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
