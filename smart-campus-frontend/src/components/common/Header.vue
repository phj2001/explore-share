<template>
  <header class="site-header">
    <div class="front-shell header-shell">
      <div class="brand-block">
        <router-link to="/" class="brand-link">
          <span class="brand-mark">FC</span>
          <div class="brand-copy">
            <strong>地点探索与分享</strong>
            <span>发现地点 · 分享体验 · 串联路线</span>
          </div>
        </router-link>
      </div>

      <div class="search-strip front-panel">
        <el-input
          v-model="searchText"
          placeholder="搜索地点、空间或兴趣点"
          class="search-input"
          clearable
          @keyup.enter="handleSearch"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>

        <el-select
          v-model="selectedCategory"
          placeholder="全部分类"
          class="category-select"
          clearable
          @change="handleCategoryFilter"
        >
          <el-option label="全部分类" value="" />
          <el-option
            v-for="category in poiCategories"
            :key="category"
            :label="category"
            :value="category"
          />
        </el-select>

        <el-button type="primary" @click="handleSearch">开始探索</el-button>
      </div>

      <div class="right-section">
        <nav class="nav-menu front-panel">
          <router-link to="/" class="nav-item">探索首页</router-link>
          <router-link v-if="userStore.isLoggedIn" to="/settings" class="nav-item">个人中心</router-link>
          <router-link v-if="userStore.isSuperAdmin" to="/admin/overview" class="nav-item">运营后台</router-link>
        </nav>

        <div class="user-actions">
          <template v-if="userStore.isLoggedIn">
            <router-link to="/settings" class="profile-link front-panel">
              <el-avatar :size="38" :src="avatarUrl || undefined" class="user-avatar">
                {{ displayName.slice(0, 1).toUpperCase() }}
              </el-avatar>
              <div class="profile-copy">
                <strong>{{ displayName }}</strong>
                <span>查看资料与安全设置</span>
              </div>
            </router-link>
            <el-button text class="logout-button" @click="handleLogout">退出</el-button>
          </template>

          <template v-else>
            <router-link to="/login" class="login-link">
              <el-button type="primary">登录 / 注册</el-button>
            </router-link>
          </template>
        </div>
      </div>
    </div>
  </header>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import { usePOIStore } from '@/stores/poi'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()
const poiStore = usePOIStore()

const searchText = ref('')
const selectedCategory = ref('')
const poiCategories = ref([])

const displayName = computed(() => userStore.displayName || '当前用户')
const avatarUrl = computed(() => userStore.avatarUrl)
const SEARCH_RESULT_LIMIT_MESSAGE = '当前结果数量较多，系统仅展示前一部分地点。建议继续输入更精确的关键词或放大地图后查看。'

const resetToCurrentBounds = async () => {
  poiStore.clearSearchPoiList()
  poiStore.showBoundsResults()
  window.dispatchEvent(new CustomEvent('poi:reset-to-bounds'))
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

onMounted(async () => {
  try {
    if (userStore.isLoggedIn && !userStore.userInfo?.displayName && !userStore.userInfo?.avatarUrl) {
      await userStore.syncCurrentUser()
    }

    await poiStore.fetchCategories()
    poiCategories.value = poiStore.categories
  } catch (error) {
    console.error('加载头部数据失败:', error)
  }
})

const handleSearch = async () => {
  if (!searchText.value.trim()) {
    await resetToCurrentBounds()
    return
  }

  try {
    await poiStore.searchByName(searchText.value.trim())
    if (poiStore.searchSummary.truncated) {
      ElMessage.warning(SEARCH_RESULT_LIMIT_MESSAGE)
    }
    window.scrollTo({ top: 0, behavior: 'smooth' })
  } catch {
    ElMessage.error('搜索失败')
  }
}

const handleCategoryFilter = async () => {
  if (!selectedCategory.value) {
    await resetToCurrentBounds()
    return
  }

  try {
    await poiStore.fetchByCategory(selectedCategory.value)
    if (poiStore.searchSummary.truncated) {
      ElMessage.warning(SEARCH_RESULT_LIMIT_MESSAGE)
    }
    window.scrollTo({ top: 0, behavior: 'smooth' })
  } catch {
    ElMessage.error('筛选失败')
  }
}

const handleLogout = () => {
  userStore.logout()
  router.push('/login')
}
</script>

<style scoped>
.site-header {
  position: sticky;
  top: 0;
  z-index: 1000;
  padding: 14px 0 0;
  background: linear-gradient(180deg, rgba(244, 248, 249, 0.92), rgba(244, 248, 249, 0));
  backdrop-filter: blur(12px);
}

.header-shell {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: center;
  gap: 14px;
}

.brand-link {
  display: inline-flex;
  align-items: center;
  gap: 12px;
  text-decoration: none;
}

.brand-mark {
  width: 46px;
  height: 46px;
  border-radius: 16px;
  display: grid;
  place-items: center;
  background: linear-gradient(135deg, var(--front-accent), var(--front-accent-strong));
  color: #ffffff;
  font-size: 15px;
  font-weight: 800;
  letter-spacing: 0.08em;
  box-shadow: 0 12px 24px rgba(23, 135, 166, 0.2);
}

.brand-copy {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.brand-copy strong {
  color: var(--front-text);
  font-size: 18px;
  letter-spacing: 0.01em;
}

.brand-copy span {
  color: var(--front-text-muted);
  font-size: 12px;
}

.search-strip {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px;
  border-radius: 22px;
}

.search-input {
  flex: 1;
}

.category-select {
  width: 160px;
}

.right-section {
  display: flex;
  align-items: center;
  gap: 12px;
}

.nav-menu {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 6px;
  border-radius: 18px;
}

.nav-item {
  min-height: 36px;
  padding: 0 12px;
  border-radius: 12px;
  display: inline-flex;
  align-items: center;
  color: var(--front-text-soft);
  text-decoration: none;
  font-size: 13px;
  font-weight: 600;
  transition: background-color 0.2s ease, color 0.2s ease;
}

.nav-item:hover,
.nav-item.router-link-active {
  background: var(--front-accent-soft);
  color: var(--front-accent-strong);
}

.user-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.profile-link {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  padding: 8px 10px;
  border-radius: 18px;
  color: inherit;
  text-decoration: none;
}

.user-avatar {
  background: linear-gradient(135deg, var(--front-accent), var(--front-accent-strong));
  color: #fff;
  font-weight: 700;
}

.profile-copy {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.profile-copy strong {
  color: var(--front-text);
  font-size: 13px;
}

.profile-copy span {
  color: var(--front-text-muted);
  font-size: 11px;
}

.logout-button {
  color: var(--front-text-soft);
}

.login-link {
  text-decoration: none;
}

@media (max-width: 1220px) {
  .header-shell {
    grid-template-columns: 1fr;
    align-items: stretch;
  }

  .right-section {
    justify-content: space-between;
  }
}

@media (max-width: 860px) {
  .search-strip,
  .right-section {
    flex-direction: column;
    align-items: stretch;
  }

  .nav-menu {
    justify-content: flex-start;
    flex-wrap: wrap;
  }

  .category-select {
    width: 100%;
  }
}

@media (max-width: 640px) {
  .brand-copy strong {
    font-size: 16px;
  }

  .profile-link {
    width: 100%;
  }

  .user-actions {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
