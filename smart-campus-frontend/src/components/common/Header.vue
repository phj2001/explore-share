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

        <div class="mobile-actions">
          <el-button
            class="mobile-action-button mobile-search-button"
            aria-label="打开搜索面板"
            @click="toggleMobileSearch"
          >
            <el-icon><Search /></el-icon>
            <span>搜索</span>
          </el-button>

          <router-link
            v-if="userStore.isLoggedIn"
            to="/settings"
            class="mobile-profile-link front-panel"
            aria-label="个人中心"
          >
            <el-avatar :size="34" :src="avatarUrl || undefined" class="user-avatar">
              {{ displayName.slice(0, 1).toUpperCase() }}
            </el-avatar>
          </router-link>

          <router-link v-else to="/login" class="mobile-login-link" aria-label="登录或注册">
            <el-button type="primary" class="mobile-action-button mobile-login-button">登录</el-button>
          </router-link>

          <el-button
            v-if="userStore.isLoggedIn"
            class="mobile-action-button mobile-logout-button"
            aria-label="退出登录"
            @click="handleMobileLogout"
          >
            退出
          </el-button>
        </div>
      </div>

      <div class="search-strip front-panel" :class="{ 'mobile-search-collapsed': !mobileSearchVisible }">
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
const mobileSearchVisible = ref(false)

const displayName = computed(() => userStore.displayName || '当前用户')
const avatarUrl = computed(() => userStore.avatarUrl)
const SEARCH_RESULT_LIMIT_MESSAGE = '当前结果数量较多，系统仅展示前一部分地点。建议继续输入更精确的关键词或放大地图后查看。'
const EMPTY_SEARCH_RESULT_MESSAGE = '没有找到符合条件的地点，请尝试更换关键词或分类。'

const notifyMapFitSearchResults = () => {
  window.dispatchEvent(new CustomEvent('poi:fit-search-results'))
}

const handleSearchResultFeedback = () => {
  if (!poiStore.searchPoiList.length) {
    ElMessage.info(EMPTY_SEARCH_RESULT_MESSAGE)
    return
  }

  if (poiStore.searchSummary.truncated) {
    ElMessage.warning(SEARCH_RESULT_LIMIT_MESSAGE)
  }

  notifyMapFitSearchResults()
}

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
    mobileSearchVisible.value = false
    return
  }

  try {
    await poiStore.searchByName(searchText.value.trim())
    handleSearchResultFeedback()
    window.scrollTo({ top: 0, behavior: 'smooth' })
    mobileSearchVisible.value = false
  } catch {
    ElMessage.error('搜索失败')
  }
}

const handleCategoryFilter = async () => {
  if (!selectedCategory.value) {
    await resetToCurrentBounds()
    mobileSearchVisible.value = false
    return
  }

  try {
    await poiStore.fetchByCategory(selectedCategory.value)
    handleSearchResultFeedback()
    window.scrollTo({ top: 0, behavior: 'smooth' })
    mobileSearchVisible.value = false
  } catch {
    ElMessage.error('筛选失败')
  }
}

const handleLogout = () => {
  userStore.logout()
  router.push('/login')
}

const handleMobileLogout = () => {
  handleLogout()
}

const toggleMobileSearch = () => {
  mobileSearchVisible.value = !mobileSearchVisible.value
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

.brand-block {
  display: flex;
  align-items: center;
  gap: 10px;
}

.brand-link {
  display: inline-flex;
  align-items: center;
  gap: 12px;
  text-decoration: none;
}

.mobile-actions {
  display: none;
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

  .search-strip {
    gap: 12px;
  }

  .nav-menu {
    justify-content: flex-start;
    flex-wrap: wrap;
  }

  .category-select {
    width: 100%;
  }

  .user-actions {
    justify-content: space-between;
  }
}

@media (max-width: 640px) {
  .site-header {
    padding-top: 8px;
  }

  .header-shell {
    gap: 8px;
  }

  .brand-block {
    justify-content: space-between;
    padding: 8px 12px;
    border-radius: 18px;
    background: rgba(255, 255, 255, 0.88);
    box-shadow: var(--front-shadow-soft);
  }

  .brand-link {
    min-width: 0;
    gap: 10px;
  }

  .brand-mark {
    width: 34px;
    height: 34px;
    border-radius: 12px;
    font-size: 12px;
    flex-shrink: 0;
  }

  .brand-copy strong {
    font-size: 14px;
  }

  .brand-copy span {
    display: none;
  }

  .mobile-actions {
    display: flex;
    align-items: center;
    gap: 8px;
    flex-shrink: 0;
    flex-wrap: wrap;
    justify-content: flex-end;
  }

  .mobile-profile-link {
    padding: 4px;
    border-radius: 14px;
    text-decoration: none;
    order: 2;
  }

  .mobile-login-link {
    text-decoration: none;
    order: 4;
  }

  .mobile-login-button {
    color: #ffffff;
  }

  .mobile-action-button {
    min-height: 34px;
    padding: 0 10px;
    border-radius: 12px;
    border-color: rgba(23, 135, 166, 0.14);
    background: rgba(255, 255, 255, 0.96);
    color: var(--front-text);
    font-size: 11px;
    font-weight: 700;
    display: inline-flex;
    align-items: center;
    gap: 4px;
    margin: 0;
  }

  .mobile-menu-button {
    order: 1;
  }

  .mobile-search-button {
    order: 1;
  }

  .mobile-logout-button {
    order: 3;
  }

  .search-strip {
    padding: 8px;
    border-radius: 16px;
  }

  .mobile-search-collapsed {
    display: none;
  }

  .right-section {
    display: none;
  }

  .profile-link {
    width: 100%;
  }

  .user-actions {
    flex-direction: column;
    align-items: stretch;
    gap: 8px;
  }

  .logout-button,
  .login-link :deep(.el-button) {
    width: 100%;
    min-height: 42px;
  }

  .logout-button {
    justify-content: center;
    margin-left: 0;
    border-radius: 14px;
    background: rgba(255, 255, 255, 0.82);
  }
}

@media (max-width: 480px) {
  .search-strip {
    gap: 8px;
  }

  .search-strip :deep(.el-input__wrapper),
  .search-strip :deep(.el-select__wrapper) {
    min-height: 38px;
    font-size: 13px;
  }

  .search-strip :deep(.el-button) {
    min-height: 38px;
    border-radius: 12px;
    font-size: 13px;
  }

  .brand-copy strong {
    font-size: 13px;
  }

  .mobile-action-button span {
    line-height: 1;
  }

  .profile-copy span {
    display: none;
  }
}
</style>
