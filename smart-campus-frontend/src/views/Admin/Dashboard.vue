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
        <router-link to="/admin/user-route-reviews" class="nav-item">
          <el-icon><Finished /></el-icon>
          <span>路线审核</span>
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
  Finished,
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
  background: var(--admin-bg);
}

/* ---- 深翠绿侧栏 ---- */
.sidebar {
  width: 252px;
  flex-shrink: 0;
  padding: 16px 12px;
  background: var(--admin-sidebar-bg);
  display: flex;
  flex-direction: column;
  position: sticky;
  top: 0;
  height: 100vh;
  overflow: hidden;
}

.logo {
  display: flex;
  align-items: center;
  gap: 11px;
  padding: 10px 10px 16px;
  margin-bottom: 8px;
  border-bottom: 1px solid var(--admin-sidebar-border);
}

.logo-mark {
  width: 38px;
  height: 38px;
  border-radius: 10px;
  display: grid;
  place-items: center;
  background: var(--forest-700);
  color: #fff;
  flex-shrink: 0;
}

.logo-copy h2 {
  margin: 0;
  font-family: var(--font-serif);
  font-size: 15px;
  font-weight: 500;
  color: var(--admin-sidebar-text-bright);
  letter-spacing: -0.01em;
}

.logo-copy p {
  margin: 3px 0 0;
  font-family: var(--font-mono);
  font-size: 9.5px;
  letter-spacing: 0.14em;
  text-transform: uppercase;
  color: var(--admin-sidebar-accent);
  opacity: 0.8;
}

.nav-menu {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 2px;
  overflow-y: auto;
  scrollbar-width: none;
  padding: 4px 0;
}
.nav-menu::-webkit-scrollbar { display: none; }

.nav-item {
  display: flex;
  align-items: center;
  gap: 10px;
  min-height: 38px;
  padding: 0 11px;
  border-radius: 8px;
  color: var(--admin-sidebar-text);
  text-decoration: none;
  font-size: 13px;
  transition: background 0.15s, color 0.15s;
  position: relative;
}

.nav-item:hover {
  background: var(--admin-sidebar-hover);
  color: var(--admin-sidebar-text-bright);
}

.nav-item.router-link-active {
  background: var(--admin-sidebar-active);
  color: var(--admin-sidebar-text-bright);
}

.nav-item.router-link-active::before {
  content: '';
  position: absolute;
  left: 0;
  top: 6px;
  bottom: 6px;
  width: 2.5px;
  border-radius: 2px;
  background: var(--admin-sidebar-accent);
}

.nav-item :deep(.el-icon) {
  font-size: 15px;
  flex-shrink: 0;
}

/* ---- 主内容区 ---- */
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
  gap: 16px;
  padding: 14px 24px;
  background: var(--admin-panel);
  border-bottom: 1px solid var(--admin-border);
}

.top-bar-copy {
  display: flex;
  align-items: center;
  gap: 10px;
}

.top-bar-kicker {
  display: inline-flex;
  align-items: center;
  padding: 4px 10px;
  border-radius: 999px;
  background: var(--admin-accent-soft);
  color: var(--admin-accent-strong);
  font-family: var(--font-mono);
  font-size: 10.5px;
  font-weight: 500;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.top-bar-copy strong {
  font-family: var(--font-sans);
  font-size: 14px;
  color: var(--admin-text);
  font-weight: 500;
}

.user-info {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
  flex-wrap: wrap;
}

.header-action {
  color: var(--admin-text-muted);
  font-size: 13px;
}

.header-action.danger {
  color: var(--clay-600);
}

.welcome-text {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: var(--admin-text-muted);
  white-space: nowrap;
}

.username-highlight {
  display: inline-flex;
  align-items: center;
  padding: 3px 10px;
  border-radius: 999px;
  background: var(--admin-accent-soft);
  color: var(--admin-accent-strong);
  font-size: 13px;
  font-weight: 600;
}

.content {
  flex: 1;
  padding: 20px;
  background: var(--admin-bg);
}

@media (max-width: 1180px) {
  .dashboard-container {
    flex-direction: column;
  }

  .sidebar {
    width: 100%;
    position: static;
    height: auto;
    overflow: visible;
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
