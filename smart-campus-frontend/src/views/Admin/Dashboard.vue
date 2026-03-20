<template>
  <div class="dashboard-container">
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

    <main class="main-content">
      <header class="top-bar">
        <el-button @click="handleBackHome" text>
          <el-icon><HomeFilled /></el-icon>
          返回首页
        </el-button>
        <div class="user-info">
          <span class="welcome-text">
            欢迎，
            <span class="username-highlight">{{ displayName }}</span>
          </span>
          <el-button @click="handleLogout" text>退出登录</el-button>
        </div>
      </header>

      <div class="content">
        <router-view />
      </div>
    </main>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { HomeFilled, Location } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

const displayName = computed(() => userStore.username || '当前用户')

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
  flex-shrink: 0;
}

.welcome-text {
  color: #475569;
  font-size: 14px;
  white-space: nowrap;
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.username-highlight {
  color: #0f172a;
  font-weight: 700;
  background: linear-gradient(135deg, rgba(14, 165, 233, 0.14), rgba(59, 130, 246, 0.08));
  border-radius: 999px;
  padding: 4px 10px;
  line-height: 1;
}

.content {
  flex: 1;
  padding: 30px;
  background: #f0f2f5;
}
</style>
