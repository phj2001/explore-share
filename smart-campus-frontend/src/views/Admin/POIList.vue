<template>
  <div class="poi-list-container">
    <!-- 工具栏 -->
    <div class="toolbar">
      <el-input
        v-model="searchText"
        placeholder="搜索地点名称"
        style="width: 300px"
        clearable
        @clear="handleSearch"
      >
        <template #append>
          <el-button :icon="Search" @click="handleSearch" />
        </template>
      </el-input>

      <el-select
        v-model="selectedCategory"
        placeholder="选择分类"
        style="width: 200px; margin-left: 10px"
        clearable
        @change="handleCategoryChange"
      >
        <el-option
          v-for="category in poiStore.categories"
          :key="category"
          :label="category"
          :value="category"
        />
      </el-select>

      <el-button type="primary" @click="handleCreate" style="margin-left: auto">
        新增地点
      </el-button>
    </div>

    <!-- 数据表格 -->
    <el-table
      :data="pagedPOIList"
      v-loading="poiStore.isLoading"
      stripe
      style="width: 100%; margin-top: 20px"
    >
      <el-table-column prop="id" label="ID" width="80" sortable />
      <el-table-column prop="name" label="名称" sortable />
      <el-table-column prop="category" label="分类" width="150" />
      <el-table-column prop="latitude" label="纬度" width="120" />
      <el-table-column prop="longitude" label="经度" width="120" />
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="handleEdit(row)">编辑</el-button>
          <el-button size="small" type="danger" @click="handleDelete(row)">
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <div class="pagination-container">
      <span class="total-text">共 {{ total }} 条</span>
      <el-select
        v-model="pageSize"
        placeholder="每页数量"
        style="width: 120px; margin-right: 16px"
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
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { usePOIStore } from '@/stores/poi'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search } from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()
const poiStore = usePOIStore()

const searchText = ref('')
const selectedCategory = ref('')

// 分页相关
const currentPage = ref(1)
const pageSize = ref(20)

// 默认按 ID 升序排列（从小到大）
const sortedPOIList = computed(() => {
  return [...poiStore.poiList].sort((a, b) => a.id - b.id)
})

// 总数
const total = computed(() => sortedPOIList.value.length)

// 当前页显示的数据
const pagedPOIList = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  const end = start + pageSize.value
  return sortedPOIList.value.slice(start, end)
})

// 页码变化
const handlePageChange = (page) => {
  currentPage.value = page
}

// 每页数量变化
const handlePageSizeChange = () => {
  currentPage.value = 1
}

onMounted(async () => {
  await loadData()
  await applyRouteFilters()
})

const loadData = async () => {
  try {
    await Promise.all([
      poiStore.fetchAllPOIs(),
      poiStore.fetchCategories()
    ])
  } catch (error) {
    ElMessage.error('加载数据失败')
  }
}

const applyRouteFilters = async () => {
  const routeCategory = typeof route.query.category === 'string' ? route.query.category : ''
  if (!routeCategory) {
    return
  }

  selectedCategory.value = routeCategory
  await handleCategoryChange()
}

const handleSearch = async () => {
  if (!searchText.value) {
    await poiStore.fetchAllPOIs()
  } else {
    try {
      await poiStore.searchByName(searchText.value)
    } catch (error) {
      ElMessage.error('搜索失败')
    }
  }
}

const handleCategoryChange = async () => {
  if (!selectedCategory.value) {
    await poiStore.fetchAllPOIs()
  } else {
    try {
      await poiStore.fetchByCategory(selectedCategory.value)
    } catch (error) {
      ElMessage.error('加载分类失败')
    }
  }
}

const handleCreate = () => {
  router.push('/admin/poi/create')
}

const handleEdit = (row) => {
  router.push(`/admin/poi/edit/${row.id}`)
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm('确定要删除这个地点吗？', '提示', {
      type: 'warning'
    })

    await poiStore.remove(row.id)
    ElMessage.success('删除成功')
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}
</script>

<style scoped>
.poi-list-container {
  background: #fff;
  padding: 20px;
  border-radius: 4px;
}

.toolbar {
  display: flex;
  align-items: center;
}

.pagination-container {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px solid #ebeef5;
}

.total-text {
  color: #606266;
  font-size: 14px;
  margin-right: auto;
}
</style>
