<template>
  <div class="poi-page">
    <section class="page-hero">
      <div class="hero-copy">
        <span class="hero-kicker">地点管理</span>
        <h1>统一维护地点名称、分类、坐标与基础说明信息</h1>
        <p>
          这里集中管理地图上的全部地点数据，支持按名称和分类快速筛选，并直接进入新增、编辑、删除和批量导入流程。
        </p>
      </div>

      <div class="hero-stats">
        <article class="hero-stat">
          <span>地点总数</span>
          <strong>{{ totalAll }}</strong>
          <em>当前系统中已录入的地点数量</em>
        </article>
        <article class="hero-stat">
          <span>筛选结果</span>
          <strong>{{ total }}</strong>
          <em>符合当前检索条件的地点数量</em>
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
        <span class="panel-note">按 ID 升序展示，支持编辑、删除和批量导入更新</span>
      </div>

      <el-table :data="pagedPOIList" v-loading="poiStore.isLoading" stripe>
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
              <span>{{ row.category || '未分类' }}</span>
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
            仅支持 UTF-8 编码的 CSV 文件。必填列为 `name/名称`、`latitude/纬度`、`longitude/经度`，
            可选列为 `category/分类`、`description/描述`。
          </template>
        </el-alert>

        <div class="import-template-row">
          <span>推荐先下载模板，再按模板整理数据。</span>
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
import { importPOIs } from '@/api/poi'
import { usePOIStore } from '@/stores/poi'

const router = useRouter()
const route = useRoute()
const poiStore = usePOIStore()

const searchText = ref('')
const selectedCategory = ref('')
const currentPage = ref(1)
const pageSize = ref(20)

const importDialogVisible = ref(false)
const importing = ref(false)
const replaceExisting = ref(false)
const skipDuplicates = ref(true)
const importResult = ref(null)
const importFile = ref(null)
const uploadFileList = ref([])
const uploadRef = ref()

const normalizedKeyword = computed(() => searchText.value.trim().toLowerCase())
const sortedPOIList = computed(() => [...poiStore.poiList].sort((a, b) => a.id - b.id))

const filteredPOIList = computed(() => {
  return sortedPOIList.value.filter((item) => {
    const matchesKeyword = normalizedKeyword.value
      ? (item.name || '').toLowerCase().includes(normalizedKeyword.value)
      : true
    const matchesCategory = selectedCategory.value
      ? item.category === selectedCategory.value
      : true

    return matchesKeyword && matchesCategory
  })
})

const totalAll = computed(() => sortedPOIList.value.length)
const total = computed(() => filteredPOIList.value.length)

const pagedPOIList = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  return filteredPOIList.value.slice(start, start + pageSize.value)
})

const activeFilters = computed(() => {
  const items = []
  if (searchText.value.trim()) {
    items.push({ key: 'keyword', label: `关键词：${searchText.value.trim()}` })
  }
  if (selectedCategory.value) {
    items.push({ key: 'category', label: `分类：${selectedCategory.value}` })
  }
  return items
})

const loadData = async () => {
  try {
    await Promise.all([
      poiStore.fetchAllPOIs(),
      poiStore.fetchCategories()
    ])
  } catch (error) {
    ElMessage.error(error.message || '加载地点数据失败')
  }
}

const applyRouteFilters = () => {
  const routeCategory = typeof route.query.category === 'string' ? route.query.category : ''
  if (routeCategory) {
    selectedCategory.value = routeCategory
  }
}

const handleSearch = () => {
  currentPage.value = 1
}

const handleCategoryChange = () => {
  currentPage.value = 1
}

const handlePageChange = (page) => {
  currentPage.value = page
}

const handlePageSizeChange = () => {
  currentPage.value = 1
}

const reloadData = async () => {
  await loadData()
}

const resetFilters = () => {
  searchText.value = ''
  selectedCategory.value = ''
  currentPage.value = 1
}

const removeFilter = (key) => {
  if (key === 'keyword') {
    searchText.value = ''
  }
  if (key === 'category') {
    selectedCategory.value = ''
  }
  currentPage.value = 1
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

    await poiStore.remove(row.id)

    if (!pagedPOIList.value.length && currentPage.value > 1) {
      currentPage.value -= 1
    }

    ElMessage.success('地点已删除')
  } catch (error) {
    if (error !== 'cancel') {
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
    await loadData()
    currentPage.value = 1

    ElMessage.success(`导入完成，成功写入 ${result.importedCount} 条记录`)
  } catch (error) {
    ElMessage.error(error.message || '批量导入失败')
  } finally {
    importing.value = false
  }
}

onMounted(async () => {
  await loadData()
  applyRouteFilters()
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
  color: #5f7882;
  font-size: 12px;
}

.active-filter-chip {
  border: none;
  cursor: pointer;
  padding: 8px 12px;
  border-radius: 999px;
  background: rgba(23, 135, 166, 0.08);
  color: #0d6b85;
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
  color: #18333d;
  font-size: 14px;
}

.poi-main span,
.coordinate-cell,
.total-text {
  color: #5f7882;
  font-size: 12px;
}

.poi-main p {
  margin: 0;
  color: #45616c;
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
  color: #6b7f88;
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
  background: #f7fbfc;
  border: 1px solid rgba(18, 98, 120, 0.12);
}

.import-result-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.import-result-head strong {
  color: #173b46;
}

.import-result-head span {
  color: #5f7882;
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
