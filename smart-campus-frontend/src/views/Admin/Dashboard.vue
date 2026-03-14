<template>
  <div class="dashboard-container">
    <!-- 侧边栏 -->
    <aside class="sidebar">
      <div class="logo">
        <h2>智慧校园</h2>
        <p>管理后台</p>
      </div>

      <nav class="nav-menu">
        <router-link to="/admin/poi" class="nav-item">
          <el-icon><Location /></el-icon>
          <span>POI 管理</span>
        </router-link>
      </nav>
    </aside>

    <!-- 主内容区 -->
    <main class="main-content">
      <!-- 顶部栏 -->
      <header class="top-bar">
        <el-button @click="handleBackHome" text>
          <el-icon><HomeFilled /></el-icon>
          返回首页
        </el-button>
        <div class="user-info">
          <span>欢迎，{{ userStore.username }}</span>
          <el-button @click="handleLogout" text>退出登录</el-button>
        </div>
      </header>

      <!-- 内容区域 -->
      <div class="content">
        <router-view />
      </div>
    </main>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { Location, HomeFilled } from '@element-plus/icons-vue'

const router = useRouter()
const userStore = useUserStore()

const handleBackHome = () => {
  router.push('/')
}

const handleLogout = () => {
  userStore.logout()
  router.push('/login')
}
</script>

<style scoped>
.dashboard-container {
  display: flex;
  min-height: 100vh;
}

.sidebar {
  width: 250px;
  background: #304156;
  color: #fff;
}

.logo {
  padding: 30px 20px;
  text-align: center;
  border-bottom: 1px solid #434a58;
}

.logo h2 {
  margin: 0;
  font-size: 24px;
}

.logo p {
  margin: 5px 0 0;
  font-size: 14px;
  color: #aeb9c2;
}

.nav-menu {
  padding: 20px 0;
}

.nav-item {
  display: flex;
  align-items: center;
  padding: 15px 25px;
  color: #bfcbd9;
  text-decoration: none;
  transition: all 0.3s;
}

.nav-item:hover,
.nav-item.router-link-active {
  background: #263445;
  color: #409eff;
}

.nav-item .el-icon {
  margin-right: 10px;
}

.main-content {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.top-bar {
  height: 60px;
  padding: 0 30px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  box-shadow: 0 1px 4px rgba(0, 21, 41, 0.08);
}

.user-info {
  display: flex;
  align-items: center;
  gap: 15px;
}

.content {
  flex: 1;
  padding: 30px;
  background: #f0f2f5;
}
</style>
