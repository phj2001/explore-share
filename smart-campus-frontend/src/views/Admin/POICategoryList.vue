<template>
  <div class="category-page">
    <section class="page-hero">
      <div class="hero-copy">
        <span class="hero-kicker">地点分类</span>
        <h1>统一管理地点分类，避免命名持续发散</h1>
        <p>当前版本直接基于已有地点分类聚合展示，支持批量重命名，并可一键跳到对应分类的地点列表继续处理。</p>
      </div>

      <div class="hero-stats">
        <article class="hero-stat">
          <span>分类总数</span>
          <strong>{{ categories.length }}</strong>
          <em>当前正在被使用的分类数</em>
        </article>
        <article class="hero-stat">
          <span>地点总量</span>
          <strong>{{ totalPoiCount }}</strong>
          <em>所有分类下的地点合计</em>
        </article>
        <article class="hero-stat">
          <span>最大分类</span>
          <strong>{{ hottestCategory?.name || '--' }}</strong>
          <em>{{ hottestCategory ? `${hottestCategory.poiCount} 个地点` : '暂无数据' }}</em>
        </article>
      </div>
    </section>

    <section class="filter-panel">
      <el-input
        v-model="keyword"
        clearable
        placeholder="搜索分类名称"
        class="filter-input"
        @keyup.enter="handleSearch"
        @clear="handleSearch"
      >
        <template #append>
          <el-button :icon="Search" @click="handleSearch" />
        </template>
      </el-input>

      <div v-if="keyword.trim()" class="active-filter-list">
        <span class="filter-label">当前筛选</span>
        <button type="button" class="active-filter-chip" @click="resetFilters">
          关键词：{{ keyword.trim() }}
        </button>
      </div>

      <el-button text @click="loadCategories">刷新列表</el-button>
    </section>

    <section class="table-panel">
      <div class="panel-head">
        <div>
          <span class="panel-kicker">分类列表</span>
          <h2>当前分类清单</h2>
        </div>
        <el-button type="primary" plain @click="goCreatePoi">新增带新分类的地点</el-button>
      </div>

      <el-table :data="filteredCategories" v-loading="loading" stripe>
        <template #empty>
          <el-empty description="当前条件下暂无分类">
            <el-button type="primary" plain @click="resetFilters">清空筛选</el-button>
          </el-empty>
        </template>

        <el-table-column label="分类名称" min-width="240">
          <template #default="{ row }">
            <div class="category-main">
              <strong>{{ row.name }}</strong>
              <span>已被 {{ row.poiCount }} 个地点使用</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="地点数量" width="120">
          <template #default="{ row }">
            <el-tag effect="plain" type="info">{{ row.poiCount }}</el-tag>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="300" fixed="right">
          <template #default="{ row }">
            <div class="action-group">
              <el-button size="small" @click="openRenameDialog(row)">重命名</el-button>
              <el-button size="small" plain @click="goPoiList(row.name)">查看地点</el-button>
              <el-tooltip content="当前版本仅展示正在被使用的分类，因此这里不支持直接删除" placement="top">
                <span class="disabled-wrap">
                  <el-button size="small" type="danger" disabled>删除</el-button>
                </span>
              </el-tooltip>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <el-dialog v-model="renameVisible" width="440px" title="重命名分类" destroy-on-close>
      <el-form ref="renameFormRef" :model="renameForm" :rules="renameRules" label-width="88px">
        <el-form-item label="原分类">
          <el-input :model-value="selectedCategory?.name || ''" disabled />
        </el-form-item>
        <el-form-item label="新名称" prop="newName">
          <el-input
            v-model="renameForm.newName"
            maxlength="50"
            show-word-limit
            placeholder="请输入新的分类名称"
            @keyup.enter="submitRename"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="renameVisible = false">取消</el-button>
        <el-button type="primary" :loading="renaming" @click="submitRename">确认重命名</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import { getAdminPoiCategories, renameAdminPoiCategory } from '@/api/adminPoiCategory'

const router = useRouter()

const keyword = ref('')
const categories = ref([])
const loading = ref(false)
const renameVisible = ref(false)
const renaming = ref(false)
const selectedCategory = ref(null)
const renameFormRef = ref(null)
const renameForm = reactive({
  newName: ''
})

const renameRules = {
  newName: [
    { required: true, message: '请输入新的分类名称', trigger: 'blur' },
    { min: 1, max: 50, message: '分类名称长度需在 1 到 50 个字符之间', trigger: 'blur' }
  ]
}

const filteredCategories = computed(() => {
  const normalizedKeyword = keyword.value.trim().toLowerCase()
  if (!normalizedKeyword) {
    return categories.value
  }

  return categories.value.filter((item) => item.name.toLowerCase().includes(normalizedKeyword))
})

const totalPoiCount = computed(() => categories.value.reduce((sum, item) => sum + (item.poiCount || 0), 0))
const hottestCategory = computed(() => categories.value[0] || null)

const loadCategories = async () => {
  loading.value = true
  try {
    categories.value = await getAdminPoiCategories()
  } catch (error) {
    ElMessage.error(error.message || '加载分类列表失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {}

const resetFilters = () => {
  keyword.value = ''
}

const openRenameDialog = (row) => {
  selectedCategory.value = row
  renameForm.newName = row.name
  renameVisible.value = true
}

const submitRename = async () => {
  if (!selectedCategory.value) {
    return
  }

  await renameFormRef.value.validate()

  renaming.value = true
  try {
    await renameAdminPoiCategory(selectedCategory.value.name, renameForm.newName.trim())
    renameVisible.value = false
    await loadCategories()
    ElMessage.success('分类名称已更新')
  } catch (error) {
    ElMessage.error(error.message || '更新分类失败')
  } finally {
    renaming.value = false
  }
}

const goPoiList = (categoryName) => {
  router.push({
    path: '/admin/poi',
    query: {
      category: categoryName
    }
  })
}

const goCreatePoi = () => {
  router.push('/admin/poi/create')
}

onMounted(async () => {
  await loadCategories()
})
</script>

<style scoped>
.category-page {
  --panel-bg: var(--admin-panel);
  --panel-border: var(--admin-border);
  --ink: var(--admin-text);
  --muted: var(--admin-text-muted);
  --accent: var(--admin-accent-strong);
  --accent-soft: var(--admin-accent-soft);
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
  grid-template-columns: minmax(0, 1.25fr) minmax(300px, 0.95fr);
  gap: 20px;
  background:
    radial-gradient(circle at top right, rgba(74, 222, 128, 0.14), transparent 24%),
    linear-gradient(135deg, rgba(255, 255, 255, 0.98), rgba(240, 253, 244, 0.95));
}

.hero-kicker,
.panel-kicker {
  display: inline-flex;
  align-items: center;
  padding: 6px 12px;
  border-radius: 999px;
  background: var(--accent-soft);
  color: var(--accent);
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
  grid-template-columns: 1fr;
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

.filter-panel,
.table-panel {
  border-radius: 28px;
  padding: 18px;
}

.filter-panel {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.filter-input {
  width: 320px;
}

.active-filter-list {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  margin-right: auto;
}

.filter-label {
  color: var(--muted);
  font-size: 12px;
}

.active-filter-chip {
  border: none;
  cursor: pointer;
  padding: 8px 12px;
  border-radius: 999px;
  font-size: 12px;
  color: var(--accent);
  background: var(--admin-accent-soft);
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

.category-main {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.category-main strong {
  color: var(--ink);
}

.category-main span {
  color: var(--muted);
  font-size: 12px;
}

.action-group {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.disabled-wrap {
  display: inline-flex;
}

@media (max-width: 1120px) {
  .page-hero {
    grid-template-columns: 1fr;
  }

  .filter-input {
    width: 100%;
  }
}

@media (max-width: 760px) {
  .panel-head,
  .filter-panel {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
