<template>
  <header class="header">
    <div class="header-content">
      <!-- Logo - 左侧 -->
      <div class="logo">
        <h1>智慧校园</h1>
      </div>

      <!-- 搜索和筛选 - 居中 -->
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

      <!-- 导航菜单和用户操作区 - 右侧 -->
      <div class="right-section">
        <nav class="nav-menu">
          <router-link to="/" class="nav-item">首页</router-link>
          <router-link to="/admin/poi" class="nav-item">管理后台</router-link>
        </nav>

        <div class="user-actions">
          <template v-if="userStore.isLoggedIn">
            <span class="username">欢迎，{{ userStore.username }}</span>
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
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { usePOIStore } from '@/stores/poi'
import { ElMessage } from 'element-plus'
import { Search } from '@element-plus/icons-vue'

const router = useRouter()
const userStore = useUserStore()
const poiStore = usePOIStore()

const searchText = ref('')
const selectedCategory = ref('')
const poiCategories = ref([])

onMounted(async () => {
  try {
    await poiStore.fetchCategories()
    poiCategories.value = poiStore.categories
  } catch (error) {
    console.error('加载分类失败:', error)
  }
})

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

const handleCategoryFilter = async () => {
  if (!selectedCategory.value) {
    await poiStore.fetchAllPOIs()
  } else {
    try {
      await poiStore.fetchByCategory(selectedCategory.value)
    } catch (error) {
      ElMessage.error('筛选失败')
    }
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
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
}

/* 左侧 - Logo */
.logo {
  flex-shrink: 0;
}

.logo h1 {
  margin: 0;
  font-size: 26px;
  font-weight: bold;
  color: #409eff;
  letter-spacing: 2px;
  text-shadow: 1px 1px 2px rgba(64, 158, 255, 0.2);
}

/* 居中 - 搜索和筛选 */
.search-section {
  flex: 1;
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 10px;
}

.search-input {
  width: 300px;
}

.category-select {
  width: 150px;
}

/* 右侧 - 导航和用户操作 */
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
  gap: 10px;
}

.username {
  color: #666;
  font-size: 14px;
  white-space: nowrap;
}
</style>
