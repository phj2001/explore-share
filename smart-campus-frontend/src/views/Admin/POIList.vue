<template>
  <div class="poi-page">
    <section class="page-hero">
      <div class="hero-copy">
        <span class="hero-kicker">地点管理</span>
        <h1>统一维护地点名称、分类、坐标与基础说明信息</h1>
        <p>
          这里集中管理地图上的全部地点数据，支持按名称和分类快速筛选，并直接进入创建、编辑与删除流程。
        </p>
      </div>

      <div class="hero-stats">
        <article class="hero-stat">
          <span>地点总数</span>
          <strong>{{ totalAll }}</strong>
          <em>当前已录入系统的地点数量</em>
        </article>
        <article class="hero-stat">
          <span>筛选结果</span>
          <strong>{{ total }}</strong>
          <em>符合当前搜索条件的数据条数</em>
        </article>
        <article class="hero-stat">
          <span>分类数量</span>
          <strong>{{ poiStore.categories.length }}</strong>
          <em>当前可选地点分类总数</em>
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
        <span class="panel-note">按 ID 升序展示，支持直接编辑与删除</span>
      </div>

      <el-table
        :data="pagedPOIList"
        v-loading="poiStore.isLoading"
        stripe
      >
        <template #empty>
          <el-empty description="当前条件下暂无地点数据">
            <el-button type="primary" plain @click="resetFilters">清空筛选</el-button>
          </el-empty>
        </template>

        <el-table-column prop="id" label="ID" width="80" sortable />

        <el-table-column label="地点信息" min-width="260">
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
          <el-select
            v-model="pageSize"
            class="page-size-select"
            @change="handlePageSizeChange"
          >
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
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, RefreshRight, Search } from '@element-plus/icons-vue'
import { usePOIStore } from '@/stores/poi'

const router = useRouter()
const route = useRoute()
const poiStore = usePOIStore()

const searchText = ref('')
const selectedCategory = ref('')
const currentPage = ref(1)
const pageSize = ref(20)

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
      `确定删除地点“${row.name}”吗？删除后该地点将无法继续在地图中展示。`,
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
  if (typeof value !== 'number') {
    return '--'
  }
  return value.toFixed(6)
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
.action-group {
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
}
</style>
