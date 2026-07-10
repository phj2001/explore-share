<template>
  <header class="site-header">
    <div class="front-shell header-inner">

      <!-- 品牌 -->
      <router-link to="/" class="brand-link">
        <span class="brand-mark">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M12 22s-8-7.5-8-13a8 8 0 1 1 16 0c0 5.5-8 13-8 13Z"/>
            <circle cx="12" cy="9" r="2.5"/>
          </svg>
        </span>
        <div class="brand-copy">
          <strong>地点探索</strong>
          <span class="brand-sub">发现地点 · 分享体验 · 串联路线</span>
        </div>
      </router-link>

      <!-- 搜索栏（桌面） -->
      <div class="search-bar" :class="{ 'search-bar--active': mobileSearchVisible }">
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
            v-for="cat in poiCategories"
            :key="cat"
            :label="cat"
            :value="cat"
          />
        </el-select>
        <button class="search-btn" @click="handleSearch">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><circle cx="11" cy="11" r="7"/><path d="m20 20-3-3"/></svg>
          探索
        </button>
      </div>

      <!-- 右侧 -->
      <div class="nav-right">
        <nav class="nav-links">
          <router-link to="/" class="nav-item">探索首页</router-link>
          <router-link v-if="userStore.isLoggedIn" to="/settings" class="nav-item">个人中心</router-link>
          <router-link v-if="userStore.isAdminOrAbove" to="/admin/overview" class="nav-item nav-item--admin">
            <svg width="11" height="11" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="3" width="7" height="7"/><rect x="14" y="3" width="7" height="7"/><rect x="3" y="14" width="7" height="7"/><rect x="14" y="14" width="7" height="7"/></svg>
            后台
          </router-link>
        </nav>

        <template v-if="userStore.isLoggedIn">
          <NotificationBell />
          <router-link to="/settings" class="profile-chip">
            <el-avatar :size="30" :src="avatarUrl || undefined" class="profile-avatar">
              {{ displayName.slice(0, 1).toUpperCase() }}
            </el-avatar>
            <div class="profile-info">
              <strong>{{ displayName }}</strong>
              <span>个人中心</span>
            </div>
          </router-link>
          <button class="logout-btn" @click="handleLogout">退出</button>
        </template>
        <template v-else>
          <router-link to="/login" class="login-btn">登录 / 注册</router-link>
        </template>
      </div>

      <!-- 移动端操作 -->
      <div class="mobile-actions">
        <button class="mobile-btn" aria-label="搜索" @click="toggleMobileSearch">
          <el-icon><Search /></el-icon>
        </button>
        <NotificationBell v-if="userStore.isLoggedIn" />
        <router-link v-if="userStore.isLoggedIn" to="/settings" class="mobile-avatar-link">
          <el-avatar :size="32" :src="avatarUrl || undefined" class="profile-avatar">
            {{ displayName.slice(0, 1).toUpperCase() }}
          </el-avatar>
        </router-link>
        <router-link v-else to="/login" class="mobile-login-link">登录</router-link>
      </div>

    </div>

    <!-- 移动端搜索展开 -->
    <div v-if="mobileSearchVisible" class="mobile-search-panel">
      <div class="front-shell mobile-search-inner">
        <el-input v-model="searchText" placeholder="搜索地点、空间或兴趣点" clearable @keyup.enter="handleSearch">
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-select v-model="selectedCategory" placeholder="全部分类" clearable style="flex:1" @change="handleCategoryFilter">
          <el-option label="全部分类" value="" />
          <el-option v-for="cat in poiCategories" :key="cat" :label="cat" :value="cat" />
        </el-select>
        <button class="search-btn" @click="handleSearch">探索</button>
      </div>
    </div>
  </header>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import NotificationBell from '@/components/common/NotificationBell.vue'
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

const notifyMapFitSearchResults = () => window.dispatchEvent(new CustomEvent('poi:fit-search-results'))

const handleSearchResultFeedback = () => {
  if (!poiStore.searchPoiList.length) {
    ElMessage.info('没有找到符合条件的地点，请尝试更换关键词或分类。')
    return
  }
  if (poiStore.searchSummary.truncated) {
    ElMessage.warning('当前结果数量较多，系统仅展示前一部分地点。建议继续输入更精确的关键词。')
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
  } catch {}
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

const handleLogout = async () => {
  await userStore.logout()
  router.push('/login')
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
  background: rgba(247,250,247,0.92);
  backdrop-filter: blur(14px);
  border-bottom: 1px solid var(--front-border);
}

.header-inner {
  display: grid;
  grid-template-columns: auto minmax(0,1fr) auto;
  align-items: center;
  gap: 16px;
  padding: 12px 0;
}

/* 品牌 */
.brand-link {
  display: inline-flex;
  align-items: center;
  gap: 11px;
  text-decoration: none;
  flex-shrink: 0;
}

.brand-mark {
  width: 42px;
  height: 42px;
  border-radius: 12px;
  display: grid;
  place-items: center;
  background: linear-gradient(135deg, var(--forest-600), var(--forest-800));
  color: #fff;
  box-shadow: 0 6px 18px rgba(31,140,105,0.28);
  flex-shrink: 0;
}

.brand-copy {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.brand-copy strong {
  font-family: var(--font-serif);
  font-size: 17px;
  font-weight: 500;
  color: var(--ink-900);
  letter-spacing: -0.01em;
}

.brand-sub {
  font-family: var(--font-mono);
  font-size: 10px;
  letter-spacing: 0.1em;
  color: var(--ink-500);
  text-transform: uppercase;
}

/* 搜索栏 */
.search-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 8px 6px 14px;
  border: 1px solid var(--front-border);
  border-radius: 999px;
  background: #fff;
  box-shadow: var(--front-shadow-soft);
  transition: border-color 0.15s, box-shadow 0.15s;
}

.search-bar:focus-within {
  border-color: var(--forest-400);
  box-shadow: 0 0 0 3px rgba(31,140,105,0.1);
}

.search-input {
  flex: 1;
}
.search-input :deep(.el-input__wrapper) {
  border: none !important;
  box-shadow: none !important;
  background: transparent;
  padding: 0;
  min-height: 32px;
}
.search-input :deep(.el-input__inner) {
  font-family: var(--font-sans);
  font-size: 13.5px;
  color: var(--ink-900);
}

.category-select {
  width: 120px;
  flex-shrink: 0;
}
.category-select :deep(.el-select__wrapper) {
  border: none !important;
  box-shadow: none !important;
  background: transparent;
  min-height: 32px;
  font-size: 13px;
}

.search-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 7px 16px;
  border-radius: 999px;
  background: var(--forest-700);
  color: #fff;
  border: none;
  font-family: var(--font-sans);
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  flex-shrink: 0;
  transition: background 0.15s;
}
.search-btn:hover { background: var(--forest-800); }

/* 右侧导航 */
.nav-right {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
}

.nav-links {
  display: flex;
  align-items: center;
  gap: 2px;
  padding: 4px;
  border: 1px solid var(--front-border);
  border-radius: 12px;
  background: #fff;
}

.nav-item {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 6px 11px;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 500;
  color: var(--ink-600);
  text-decoration: none;
  transition: background 0.15s, color 0.15s;
  white-space: nowrap;
}

.nav-item:hover,
.nav-item.router-link-active {
  background: var(--forest-50);
  color: var(--forest-700);
}

.nav-item--admin {
  color: var(--ink-500);
}
.nav-item--admin:hover,
.nav-item--admin.router-link-active {
  background: rgba(31,140,105,0.08);
  color: var(--forest-700);
}

.profile-chip {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 5px 12px 5px 6px;
  border: 1px solid var(--front-border);
  border-radius: 999px;
  background: #fff;
  text-decoration: none;
  transition: border-color 0.15s, box-shadow 0.15s;
}
.profile-chip:hover {
  border-color: var(--forest-400);
  box-shadow: 0 0 0 2px rgba(31,140,105,0.1);
}

.profile-avatar {
  background: linear-gradient(135deg, var(--forest-500), var(--forest-700));
  color: #fff;
  font-weight: 600;
  font-size: 13px;
}

.profile-info {
  display: flex;
  flex-direction: column;
  gap: 1px;
}
.profile-info strong {
  font-family: var(--font-sans);
  font-size: 12.5px;
  font-weight: 600;
  color: var(--ink-900);
}
.profile-info span {
  font-family: var(--font-mono);
  font-size: 10px;
  color: var(--ink-400);
  letter-spacing: 0.06em;
}

.logout-btn {
  background: none;
  border: 1px solid var(--front-border);
  border-radius: 8px;
  padding: 6px 12px;
  font-family: var(--font-sans);
  font-size: 12.5px;
  color: var(--ink-500);
  cursor: pointer;
  transition: background 0.15s, color 0.15s;
}
.logout-btn:hover {
  background: var(--paper-100);
  color: var(--ink-800);
}

.login-btn {
  display: inline-flex;
  align-items: center;
  padding: 8px 16px;
  border-radius: 999px;
  background: var(--forest-700);
  color: #fff;
  font-size: 13px;
  font-weight: 500;
  text-decoration: none;
  transition: background 0.15s;
}
.login-btn:hover { background: var(--forest-800); }

/* 移动端操作 */
.mobile-actions {
  display: none;
  align-items: center;
  gap: 8px;
}

.mobile-btn {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  border: 1px solid var(--front-border);
  background: #fff;
  display: grid;
  place-items: center;
  cursor: pointer;
  color: var(--ink-600);
  transition: background 0.15s;
}
.mobile-btn:hover { background: var(--paper-100); }

.mobile-avatar-link {
  text-decoration: none;
}

.mobile-login-link {
  padding: 7px 14px;
  border-radius: 999px;
  background: var(--forest-700);
  color: #fff;
  font-size: 13px;
  font-weight: 500;
  text-decoration: none;
}

/* 移动端搜索展开 */
.mobile-search-panel {
  border-top: 1px solid var(--front-border);
  padding: 10px 0 12px;
  background: rgba(247,250,247,0.96);
}

.mobile-search-inner {
  display: flex;
  gap: 8px;
  align-items: center;
}

/* 响应式 */
@media (max-width: 1100px) {
  .header-inner {
    grid-template-columns: auto 1fr auto;
  }
  .brand-sub { display: none; }
}

@media (max-width: 860px) {
  .header-inner {
    grid-template-columns: auto 1fr auto;
  }
  .nav-links { display: none; }
  .profile-info { display: none; }
  .profile-chip { padding: 4px; }
  .logout-btn { display: none; }
}

@media (max-width: 640px) {
  .header-inner {
    grid-template-columns: auto auto;
    justify-content: space-between;
    padding: 10px 0;
  }
  .search-bar { display: none; }
  .nav-right { display: none; }
  .mobile-actions { display: flex; }
}

@media (max-width: 480px) {
  .brand-copy strong { font-size: 15px; }
}
</style>
