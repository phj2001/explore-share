<template>
  <div class="admin-page">
    <div class="admin-page-head">
      <div>
        <h2>评价管理</h2>
        <p>管理用户对地点的评分与评价</p>
      </div>
    </div>

    <div class="admin-filter-bar">
      <el-input
        v-model="filterKeyword"
        placeholder="搜索评价内容、地点名称或用户名"
        clearable
        class="filter-input"
        @keyup.enter="handleSearch"
        @clear="handleSearch"
      />

      <el-input-number
        v-model="filterMinRating"
        :min="1"
        :max="5"
        placeholder="最低评分"
        controls-position="right"
        class="filter-rating"
        @change="handleSearch"
      />

      <el-input-number
        v-model="filterMaxRating"
        :min="1"
        :max="5"
        placeholder="最高评分"
        controls-position="right"
        class="filter-rating"
        @change="handleSearch"
      />

      <el-button type="primary" @click="handleSearch">搜索</el-button>
      <el-button @click="resetFilter">重置</el-button>
    </div>

    <el-table :data="reviews" stripe class="admin-table" @sort-change="handleSortChange">
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column label="地点" min-width="140">
        <template #default="{ row }">
          <span>{{ row.poiName }}</span>
          <br />
          <span class="sub-text">ID: {{ row.poiId }}</span>
        </template>
      </el-table-column>
      <el-table-column label="用户" min-width="120">
        <template #default="{ row }">
          <span>{{ row.authorDisplayName || row.authorUsername }}</span>
          <br />
          <span class="sub-text">@{{ row.authorUsername }}</span>
        </template>
      </el-table-column>
      <el-table-column label="评分" width="130" sortable="custom" prop="rating">
        <template #default="{ row }">
          <span class="review-stars-compact">
            <span v-for="s in 5" :key="s" :class="{ filled: s <= row.rating }">&#9733;</span>
          </span>
        </template>
      </el-table-column>
      <el-table-column prop="content" label="评价内容" min-width="200" show-overflow-tooltip />
      <el-table-column label="时间" width="160" sortable="custom" prop="createdAt">
        <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="90" fixed="right">
        <template #default="{ row }">
          <el-popconfirm title="确认删除该评价？" @confirm="handleDelete(row)">
            <template #reference>
              <el-button type="danger" text size="small">删除</el-button>
            </template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>

    <div class="admin-pagination">
      <el-pagination
        v-model:current-page="currentPage"
        :page-size="pageSize"
        :total="total"
        layout="total, prev, pager, next"
        @current-change="loadData"
      />
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { adminDeleteReview, getAdminReviews } from '@/api/poiReview'

const reviews = ref([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = 20
const loading = ref(false)

const filterKeyword = ref('')
const filterMinRating = ref(undefined)
const filterMaxRating = ref(undefined)

const dateTimeFormatter = new Intl.DateTimeFormat('zh-CN', {
  year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit'
})

const formatTime = (value) => value ? dateTimeFormatter.format(new Date(value)) : ''

const loadData = async () => {
  loading.value = true
  try {
    const data = await getAdminReviews({
      keyword: filterKeyword.value || undefined,
      minRating: filterMinRating.value || undefined,
      maxRating: filterMaxRating.value || undefined,
      page: currentPage.value - 1,
      size: pageSize
    })
    reviews.value = data?.records || []
    total.value = data?.total || 0
  } catch (error) {
    ElMessage.error(error.message || '加载评价列表失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  currentPage.value = 1
  loadData()
}

const resetFilter = () => {
  filterKeyword.value = ''
  filterMinRating.value = undefined
  filterMaxRating.value = undefined
  currentPage.value = 1
  loadData()
}

const handleSortChange = () => {
  loadData()
}

const handleDelete = async (row) => {
  try {
    await adminDeleteReview(row.id)
    ElMessage.success('评价已删除')
    await loadData()
  } catch (error) {
    ElMessage.error(error.message || '删除失败')
  }
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.admin-page-head {
  margin-bottom: 20px;
}

.admin-page-head h2 {
  margin: 0;
  font-family: var(--font-serif);
  font-size: 22px;
  color: var(--admin-text);
}

.admin-page-head p {
  margin: 6px 0 0;
  color: var(--admin-text-muted);
  font-size: 14px;
}

.admin-filter-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 18px;
  flex-wrap: wrap;
}

.filter-input {
  width: 280px;
}

.filter-rating {
  width: 120px;
}

.admin-pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 18px;
}

.sub-text {
  color: var(--ink-400);
  font-size: 12px;
}

.review-stars-compact span {
  font-size: 14px;
  color: var(--ink-400);
}

.review-stars-compact span.filled {
  color: #f59e0b;
}
</style>
