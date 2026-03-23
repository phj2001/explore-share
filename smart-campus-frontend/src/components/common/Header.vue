<template>
  <header class="header">
    <div class="header-content">
      <div class="logo">
        <h1>智慧校园</h1>
      </div>

      <div class="search-section">
        <el-input
          v-model="searchText"
          placeholder="搜索地点..."
          class="search-input"
          clearable
          @keyup.enter="handleSearch"
        >
          <template #append>
            <el-button :icon="Search" @click="handleSearch" />
          </template>
        </el-input>

        <el-select
          v-model="selectedCategory"
          placeholder="筛选分类"
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
      </div>

      <div class="right-section">
        <nav class="nav-menu">
          <router-link to="/" class="nav-item">首页</router-link>
          <router-link v-if="userStore.isLoggedIn" to="/settings" class="nav-item">设置</router-link>
          <router-link v-if="userStore.isSuperAdmin" to="/admin/poi" class="nav-item">管理后台</router-link>
        </nav>

        <div class="user-actions">
          <template v-if="userStore.isLoggedIn">
            <router-link to="/settings" class="profile-link">
              <el-avatar :size="36" :src="avatarUrl || undefined" class="user-avatar">
                {{ displayName.slice(0, 1).toUpperCase() }}
              </el-avatar>
              <span class="username-highlight">{{ displayName }}</span>
            </router-link>
            <el-button @click="handleLogout" text>退出登录</el-button>
          </template>
          <template v-else>
            <router-link to="/login">
              <el-button type="primary">登录</el-button>
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
  if (!searchText.value) {
    await poiStore.fetchAllPOIs()
    return
  }

  try {
    await poiStore.searchByName(searchText.value)
  } catch {
    ElMessage.error('搜索失败')
  }
}

const handleCategoryFilter = async () => {
  if (!selectedCategory.value) {
    await poiStore.fetchAllPOIs()
    return
  }

  try {
    await poiStore.fetchByCategory(selectedCategory.value)
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
.header {
  background: #fff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  position: sticky;
  top: 0;
  z-index: 1000;
}

.header-content {
  max-width: 1400px;
  margin: 0 auto;
  padding: 0 20px;
  min-height: 68px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
}

.logo {
  flex-shrink: 0;
}

.logo h1 {
  margin: 0;
  font-size: 26px;
  font-weight: 700;
  color: #409eff;
  letter-spacing: 2px;
  text-shadow: 1px 1px 2px rgba(64, 158, 255, 0.2);
}

.search-section {
  flex: 1;
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.search-input {
  width: 300px;
}

.category-select {
  width: 150px;
}

.right-section {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  gap: 20px;
}

.nav-menu {
  display: flex;
  gap: 20px;
}

.nav-item {
  color: #333;
  text-decoration: none;
  font-size: 15px;
  transition: color 0.3s;
  white-space: nowrap;
}

.nav-item:hover,
.nav-item.router-link-active {
  color: #409eff;
}

.user-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-shrink: 0;
}

.profile-link {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  color: inherit;
  text-decoration: none;
}

.user-avatar {
  background: linear-gradient(135deg, #60a5fa, #2563eb);
  color: #fff;
  font-weight: 700;
}

.username-highlight {
  color: #0f172a;
  font-weight: 700;
  background: linear-gradient(135deg, rgba(14, 165, 233, 0.14), rgba(59, 130, 246, 0.08));
  border-radius: 999px;
  padding: 6px 12px;
  line-height: 1;
}

@media (max-width: 960px) {
  .header-content {
    padding: 12px 16px;
    min-height: auto;
    flex-wrap: wrap;
  }

  .search-section {
    order: 3;
    width: 100%;
    justify-content: stretch;
  }

  .search-input,
  .category-select {
    width: 100%;
  }

  .right-section {
    margin-left: auto;
  }
}

@media (max-width: 640px) {
  .nav-menu {
    gap: 12px;
  }

  .username-highlight {
    max-width: 120px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}
</style>
