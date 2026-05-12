<template>
  <div class="dashboard-container admin-shell">
    <aside class="sidebar">
      <div class="logo">
        <div class="logo-mark">FC</div>
        <div class="logo-copy">
          <h2>地点探索与分享</h2>
          <p>内容与运营控制台</p>
        </div>
      </div>

      <nav class="nav-menu">
        <router-link to="/admin/overview" class="nav-item">
          <el-icon><DataAnalysis /></el-icon>
          <span>运营总览</span>
        </router-link>
        <router-link to="/admin/poi" class="nav-item">
          <el-icon><Location /></el-icon>
          <span>地点管理</span>
        </router-link>
        <router-link to="/admin/poi-categories" class="nav-item">
          <el-icon><CollectionTag /></el-icon>
          <span>地点分类</span>
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
        <router-link to="/admin/announcements" class="nav-item">
          <el-icon><Bell /></el-icon>
          <span>平台公告</span>
        </router-link>
        <router-link to="/admin/activities" class="nav-item">
          <el-icon><Calendar /></el-icon>
          <span>活动管理</span>
        </router-link>
        <router-link to="/admin/routes" class="nav-item">
          <el-icon><Guide /></el-icon>
          <span>路线管理</span>
        </router-link>
        <router-link to="/admin/recommendations" class="nav-item">
          <el-icon><Star /></el-icon>
          <span>推荐内容</span>
        </router-link>
        <router-link to="/admin/reports" class="nav-item">
          <el-icon><WarningFilled /></el-icon>
          <span>举报审核</span>
        </router-link>
        <router-link to="/admin/poi-applications" class="nav-item">
          <el-icon><MapLocation /></el-icon>
          <span>地点审核</span>
        </router-link>
        <router-link to="/admin/logs" class="nav-item">
          <el-icon><Tickets /></el-icon>
          <span>系统日志</span>
        </router-link>
        <router-link to="/admin/files" class="nav-item">
          <el-icon><Files /></el-icon>
          <span>文件资源</span>
        </router-link>
        <router-link to="/admin/configs" class="nav-item">
          <el-icon><Setting /></el-icon>
          <span>系统配置</span>
        </router-link>
      </nav>
    </aside>

    <main class="main-content">
      <header class="top-bar">
        <div class="top-bar-copy">
          <span class="top-bar-kicker">地点探索后台</span>
          <strong>地点内容与运营工作区</strong>
        </div>

        <div class="user-info">
          <el-button class="header-action" text @click="handleBackHome">
            <el-icon><HomeFilled /></el-icon>
            返回首页
          </el-button>
          <span class="welcome-text">
            当前用户
            <span class="username-highlight">{{ displayName }}</span>
          </span>
          <el-button class="header-action danger" text @click="handleLogout">
            退出登录
          </el-button>
        </div>
      </header>

      <div class="content">
        <router-view />
      </div>
    </main>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import {
  Bell,
  Calendar,
  ChatDotRound,
  ChatLineSquare,
  CollectionTag,
  DataAnalysis,
  Files,
  Guide,
  HomeFilled,
  Location,
  MapLocation,
  Setting,
  Star,
  Tickets,
  WarningFilled,
  UserFilled
} from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

const displayName = computed(() => userStore.displayName || '当前管理员')

const handleBackHome = () => {
  router.push('/')
}

const handleLogout = () => {
  userStore.logout()
  router.push('/login')
}

onMounted(() => {
  document.body.classList.add('admin-theme-active')
})

onUnmounted(() => {
  document.body.classList.remove('admin-theme-active')
})
</script>

<style scoped>
.dashboard-container {
  display: flex;
  min-height: 100vh;
  background:
    radial-gradient(circle at top left, rgba(23, 135, 166, 0.08), transparent 22%),
    radial-gradient(circle at bottom right, rgba(14, 116, 144, 0.06), transparent 18%),
    linear-gradient(180deg, #eef4f6 0%, #f4f8f9 42%, #eef4f6 100%);
}

.sidebar {
  width: 244px;
  padding: 18px 14px;
  background: linear-gradient(180deg, #14303a 0%, #11262e 100%);
  color: #f3fbfd;
  box-shadow: 16px 0 36px rgba(10, 25, 31, 0.12);
  display: flex;
  flex-direction: column;
}

.logo {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 10px 18px;
  margin-bottom: 10px;
  border-bottom: 1px solid rgba(168, 216, 226, 0.12);
}

.logo-mark {
  width: 42px;
  height: 42px;
  border-radius: 14px;
  display: grid;
  place-items: center;
  font-size: 14px;
  font-weight: 800;
  letter-spacing: 0.08em;
  color: #f6feff;
  background: linear-gradient(135deg, #1ba4c6, #0e708d);
}

.logo-copy h2 {
  margin: 0;
  font-size: 18px;
  letter-spacing: 0.01em;
}

.logo-copy p {
  margin: 5px 0 0;
  font-size: 12px;
  color: rgba(214, 238, 243, 0.68);
}

.nav-menu {
  display: flex;
  flex-direction: column;
  gap: 4px;
  overflow-y: auto;
  padding-right: 4px;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 11px;
  min-height: 42px;
  padding: 0 12px;
  border-radius: 14px;
  color: rgba(220, 240, 245, 0.8);
  text-decoration: none;
  transition: background-color 0.22s ease, color 0.22s ease, transform 0.22s ease;
}

.nav-item:hover {
  color: #f4fdff;
  background: rgba(24, 144, 176, 0.14);
  transform: translateX(2px);
}

.nav-item.router-link-active {
  color: #ffffff;
  background: linear-gradient(135deg, rgba(27, 164, 198, 0.2), rgba(14, 112, 141, 0.22));
  box-shadow: inset 0 0 0 1px rgba(122, 206, 225, 0.14);
}

.nav-item :deep(.el-icon) {
  font-size: 16px;
}

.main-content {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.top-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  padding: 18px 28px;
  margin: 18px 20px 0;
  border: 1px solid rgba(24, 68, 84, 0.1);
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.78);
  backdrop-filter: blur(18px);
  box-shadow: 0 14px 28px rgba(17, 36, 46, 0.05);
}

.top-bar-copy {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.top-bar-kicker {
  display: inline-flex;
  align-items: center;
  width: fit-content;
  padding: 4px 10px;
  border-radius: 999px;
  background: rgba(23, 135, 166, 0.1);
  color: #0d6b85;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.08em;
}

.top-bar-copy strong {
  font-size: 17px;
  color: #18333d;
  letter-spacing: 0.01em;
}

.user-info {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;
  flex-wrap: wrap;
}

.header-action {
  color: #45616c;
}

.header-action.danger {
  color: #9b5151;
}

.welcome-text {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: #59727c;
  white-space: nowrap;
}

.username-highlight {
  display: inline-flex;
  align-items: center;
  min-height: 32px;
  padding: 0 12px;
  border-radius: 999px;
  background: rgba(23, 135, 166, 0.08);
  color: #17333c;
  font-weight: 700;
}

.content {
  flex: 1;
  padding: 20px;
}

@media (max-width: 1180px) {
  .dashboard-container {
    flex-direction: column;
  }

  .sidebar {
    width: 100%;
    box-shadow: none;
  }

  .nav-menu {
    flex-direction: row;
    flex-wrap: nowrap;
    overflow-x: auto;
    padding-bottom: 4px;
  }

  .nav-item {
    white-space: nowrap;
    min-width: max-content;
  }
}

@media (max-width: 720px) {
  .top-bar {
    margin: 14px 14px 0;
    padding: 14px;
    align-items: stretch;
    flex-direction: column;
  }

  .user-info {
    justify-content: space-between;
  }

  .content {
    padding: 14px;
  }
}
</style>
