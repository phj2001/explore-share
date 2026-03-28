<template>
  <div class="log-page">
    <section class="page-hero">
      <div class="hero-copy">
        <span class="hero-kicker">系统日志</span>
        <h1>统一追踪后台关键操作，补齐治理与运营留痕</h1>
        <p>记录管理员在公告、用户、内容、举报审核和 POI 分类上的关键动作，方便回溯平台治理与维护过程。</p>
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
          placeholder="搜索管理员、模块、动作或日志摘要"
          @keyup.enter="handleSearch"
          @clear="handleSearch"
        >
          <template #append>
            <el-button :icon="Search" @click="handleSearch" />
          </template>
        </el-input>

        <el-select v-model="moduleName" clearable class="filter-select" placeholder="筛选模块" @change="handleSearch">
          <el-option v-for="item in moduleOptions" :key="item" :label="item" :value="item" />
        </el-select>

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
          <span class="panel-kicker">审计轨迹</span>
          <h2>管理员操作日志</h2>
        </div>
        <el-button text @click="loadLogs">刷新列表</el-button>
      </div>

      <el-table :data="logs" v-loading="loading" stripe>
        <template #empty>
          <el-empty description="当前筛选条件下暂无系统日志" />
        </template>

        <el-table-column prop="id" label="日志ID" width="96" />

        <el-table-column label="操作人" min-width="180">
          <template #default="{ row }">
            <div class="meta-block compact">
              <strong>{{ row.operatorDisplayName || row.operatorUsername }}</strong>
              <span>@{{ row.operatorUsername }}</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column prop="moduleName" label="所属模块" width="120" />
        <el-table-column prop="actionName" label="操作动作" width="140" />

        <el-table-column label="目标对象" min-width="180">
          <template #default="{ row }">
            <div class="meta-block compact">
              <strong>{{ row.targetType }}</strong>
              <span>{{ row.targetId ? `ID：${row.targetId}` : '无关联 ID' }}</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column prop="summary" label="日志摘要" min-width="320" show-overflow-tooltip />

        <el-table-column label="操作时间" width="180">
          <template #default="{ row }">
            {{ formatDate(row.createdAt) }}
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-bar">
        <span>共 {{ total }} 条日志</span>
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="total"
          layout="sizes, prev, pager, next"
          @current-change="loadLogs"
          @size-change="handlePageSizeChange"
        />
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { RefreshRight, Search } from '@element-plus/icons-vue'
import { getAdminOperationLogPage } from '@/api/adminOperationLog'

const moduleOptions = ['公告管理', '用户管理', '内容管理', '举报审核', 'POI分类']

const keyword = ref('')
const moduleName = ref()
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const logs = ref([])
const loading = ref(false)

const activeFilters = computed(() => {
  const items = []

  if (keyword.value.trim()) {
    items.push({ key: 'keyword', label: `关键词：${keyword.value.trim()}` })
  }

  if (moduleName.value) {
    items.push({ key: 'moduleName', label: `模块：${moduleName.value}` })
  }

  return items
})

const heroStats = computed(() => {
  const operators = new Set(logs.value.map((item) => item.operatorUserId).filter(Boolean)).size
  const modules = new Set(logs.value.map((item) => item.moduleName).filter(Boolean)).size

  return [
    { label: '当前总量', value: `${total.value}`, helper: '符合当前筛选条件的日志数量' },
    { label: '本页记录', value: `${logs.value.length}`, helper: '当前分页中已加载的操作记录' },
    { label: '涉及模块', value: `${modules}`, helper: '本页日志覆盖的后台模块数量' },
    { label: '操作人数', value: `${operators}`, helper: '本页日志涉及的管理员人数' }
  ]
})

const loadLogs = async () => {
  loading.value = true
  try {
    const data = await getAdminOperationLogPage({
      keyword: keyword.value.trim() || undefined,
      moduleName: moduleName.value || undefined,
      page: currentPage.value - 1,
      size: pageSize.value
    })
    logs.value = data.records || []
    total.value = data.total || 0
    currentPage.value = (data.page || 0) + 1
  } catch (error) {
    ElMessage.error(error.message || '加载系统日志失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = async () => {
  currentPage.value = 1
  await loadLogs()
}

const handlePageSizeChange = async () => {
  currentPage.value = 1
  await loadLogs()
}

const resetFilters = async () => {
  keyword.value = ''
  moduleName.value = undefined
  currentPage.value = 1
  await loadLogs()
}

const removeFilter = async (key) => {
  if (key === 'keyword') {
    keyword.value = ''
  }
  if (key === 'moduleName') {
    moduleName.value = undefined
  }
  await handleSearch()
}

const formatDate = (value) => {
  if (!value) {
    return '暂无'
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
  await loadLogs()
})
</script>

<style scoped>
.log-page {
  --panel-bg: rgba(255, 255, 255, 0.9);
  --panel-border: rgba(148, 163, 184, 0.18);
  --ink: #0f172a;
  --muted: #64748b;
  --accent: #0f766e;
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
    radial-gradient(circle at top right, rgba(20, 184, 166, 0.16), transparent 26%),
    linear-gradient(135deg, rgba(255, 255, 255, 0.98), rgba(240, 253, 250, 0.94));
}

.hero-kicker,
.panel-kicker {
  display: inline-flex;
  align-items: center;
  padding: 6px 12px;
  border-radius: 999px;
  background: rgba(13, 148, 136, 0.1);
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
  border: 1px solid rgba(94, 234, 212, 0.45);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.95), rgba(240, 253, 250, 0.94));
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
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.filter-main,
.active-filter-list {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.filter-input {
  width: 360px;
}

.filter-select {
  width: 220px;
}

.filter-label {
  color: var(--muted);
  font-size: 12px;
}

.active-filter-chip {
  border: none;
  padding: 8px 12px;
  border-radius: 999px;
  background: rgba(204, 251, 241, 0.95);
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

.meta-block {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.meta-block.compact {
  gap: 2px;
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
