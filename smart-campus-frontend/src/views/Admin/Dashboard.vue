<template>
  <div class="dashboard-container">
    <aside class="sidebar">
      <div class="logo">
        <h2>智慧校园</h2>
        <p>管理后台</p>
      </div>

      <nav class="nav-menu">
        <router-link to="/admin/overview" class="nav-item">
          <el-icon><DataAnalysis /></el-icon>
          <span>运营总览</span>
        </router-link>
        <router-link to="/admin/poi" class="nav-item">
          <el-icon><Location /></el-icon>
          <span>POI 管理</span>
        </router-link>
        <router-link to="/admin/users" class="nav-item">
          <el-icon><UserFilled /></el-icon>
          <span>用户管理</span>
        </router-link>
        <router-link to="/admin/shares" class="nav-item">
          <el-icon><ChatLineSquare /></el-icon>
          <span>分享管理</span>
        </router-link>
        <router-link to="/admin/replies" class="nav-item">
          <el-icon><ChatDotRound /></el-icon>
          <span>回复管理</span>
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
import { ChatDotRound, ChatLineSquare, DataAnalysis, HomeFilled, Location, UserFilled } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

const displayName = computed(() => userStore.displayName || '当前用户')

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
  background:
    radial-gradient(circle at top left, rgba(14, 165, 233, 0.14), transparent 26%),
    linear-gradient(180deg, #edf4fb 0%, #f4f7fb 48%, #eef3f8 100%);
}

.sidebar {
  width: 252px;
  background: linear-gradient(180deg, #213246 0%, #162434 100%);
  color: #fff;
  box-shadow: 12px 0 36px rgba(15, 23, 42, 0.16);
}

.logo {
  padding: 34px 24px 28px;
  border-bottom: 1px solid rgba(148, 163, 184, 0.18);
}

.logo h2 {
  margin: 0;
  font-size: 26px;
  letter-spacing: 0.04em;
}

.logo p {
  margin: 8px 0 0;
  font-size: 13px;
  color: #9eb1c5;
}

.nav-menu {
  padding: 18px 12px;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  margin-bottom: 10px;
  border-radius: 16px;
  color: #c8d5e3;
  text-decoration: none;
  transition: all 0.24s ease;
}

.nav-item:hover,
.nav-item.router-link-active {
  background: linear-gradient(135deg, rgba(56, 189, 248, 0.18), rgba(59, 130, 246, 0.22));
  color: #f8fbff;
  transform: translateX(3px);
}

.main-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.top-bar {
  height: 72px;
  padding: 0 32px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: rgba(255, 255, 255, 0.72);
  backdrop-filter: blur(20px);
  box-shadow: 0 10px 30px rgba(15, 23, 42, 0.06);
}

.user-info {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-shrink: 0;
}

.welcome-text {
  color: #475569;
  font-size: 14px;
  white-space: nowrap;
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.username-highlight {
  color: #0f172a;
  font-weight: 700;
  background: linear-gradient(135deg, rgba(14, 165, 233, 0.14), rgba(59, 130, 246, 0.08));
  border-radius: 999px;
  padding: 5px 12px;
  line-height: 1;
}

.content {
  flex: 1;
  padding: 28px;
}

@media (max-width: 980px) {
  .dashboard-container {
    flex-direction: column;
  }

  .sidebar {
    width: 100%;
  }

  .nav-menu {
    display: flex;
    gap: 12px;
    overflow-x: auto;
  }

  .nav-item {
    margin-bottom: 0;
    white-space: nowrap;
  }
}

@media (max-width: 640px) {
  .top-bar {
    height: auto;
    padding: 18px;
    flex-direction: column;
    align-items: stretch;
    gap: 12px;
  }

  .user-info {
    justify-content: space-between;
  }

  .content {
    padding: 16px;
  }
}
</style>
