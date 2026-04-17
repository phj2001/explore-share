import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user.js'
import { isAuthenticated } from '@/utils/auth.js'

const routes = [
  {
    path: '/',
    name: 'Home',
    component: () => import('@/views/Home.vue'),
    meta: { title: '首页' }
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { title: '登录', hideAuth: true }
  },
  {
    path: '/settings',
    name: 'Settings',
    component: () => import('@/views/Settings.vue'),
    meta: { title: '用户设置', requiresAuth: true }
  },
  {
    path: '/admin',
    name: 'Admin',
    component: () => import('@/views/Admin/Dashboard.vue'),
    meta: { title: '运营后台', requiresAuth: true, requiresSuperAdmin: true },
    redirect: '/admin/overview',
    children: [
      {
        path: 'overview',
        name: 'AdminOverview',
        component: () => import('@/views/Admin/Overview.vue'),
        meta: { title: '运营总览', requiresAuth: true, requiresSuperAdmin: true }
      },
      {
        path: 'poi',
        name: 'POIList',
        component: () => import('@/views/Admin/POIList.vue'),
        meta: { title: '地点管理', requiresAuth: true, requiresSuperAdmin: true }
      },
      {
        path: 'poi-categories',
        name: 'AdminPoiCategories',
        component: () => import('@/views/Admin/POICategoryList.vue'),
        meta: { title: '地点分类', requiresAuth: true, requiresSuperAdmin: true }
      },
      {
        path: 'users',
        name: 'AdminUsers',
        component: () => import('@/views/Admin/UserList.vue'),
        meta: { title: '用户管理', requiresAuth: true, requiresSuperAdmin: true }
      },
      {
        path: 'shares',
        name: 'AdminShares',
        component: () => import('@/views/Admin/ShareList.vue'),
        meta: { title: '分享管理', requiresAuth: true, requiresSuperAdmin: true }
      },
      {
        path: 'replies',
        name: 'AdminReplies',
        component: () => import('@/views/Admin/ReplyList.vue'),
        meta: { title: '回复管理', requiresAuth: true, requiresSuperAdmin: true }
      },
      {
        path: 'announcements',
        name: 'AdminAnnouncements',
        component: () => import('@/views/Admin/AnnouncementList.vue'),
        meta: { title: '平台公告', requiresAuth: true, requiresSuperAdmin: true }
      },
      {
        path: 'activities',
        name: 'AdminActivities',
        component: () => import('@/views/Admin/ActivityList.vue'),
        meta: { title: '活动管理', requiresAuth: true, requiresSuperAdmin: true }
      },
      {
        path: 'routes',
        name: 'AdminRoutes',
        component: () => import('@/views/Admin/RouteList.vue'),
        meta: { title: '路线管理', requiresAuth: true, requiresSuperAdmin: true }
      },
      {
        path: 'recommendations',
        name: 'AdminRecommendations',
        component: () => import('@/views/Admin/RecommendedShareList.vue'),
        meta: { title: '推荐内容', requiresAuth: true, requiresSuperAdmin: true }
      },
      {
        path: 'reports',
        name: 'AdminReports',
        component: () => import('@/views/Admin/ReportList.vue'),
        meta: { title: '举报审核', requiresAuth: true, requiresSuperAdmin: true }
      },
      {
        path: 'logs',
        name: 'AdminLogs',
        component: () => import('@/views/Admin/OperationLogList.vue'),
        meta: { title: '系统日志', requiresAuth: true, requiresSuperAdmin: true }
      },
      {
        path: 'files',
        name: 'AdminFiles',
        component: () => import('@/views/Admin/FileResourceList.vue'),
        meta: { title: '文件资源', requiresAuth: true, requiresSuperAdmin: true }
      },
      {
        path: 'configs',
        name: 'AdminConfigs',
        component: () => import('@/views/Admin/SystemConfigList.vue'),
        meta: { title: '系统配置', requiresAuth: true, requiresSuperAdmin: true }
      },
      {
        path: 'poi/create',
        name: 'POICreate',
        component: () => import('@/views/Admin/POIForm.vue'),
        meta: { title: '创建地点', requiresAuth: true, requiresSuperAdmin: true }
      },
      {
        path: 'poi/edit/:id',
        name: 'POIEdit',
        component: () => import('@/views/Admin/POIForm.vue'),
        meta: { title: '编辑地点', requiresAuth: true, requiresSuperAdmin: true }
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    redirect: '/'
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach(async (to) => {
  document.title = to.meta.title ? `${to.meta.title} - 地点探索` : '地点探索'

  const authenticated = isAuthenticated()

  if (to.meta.requiresAuth && !authenticated) {
    return { name: 'Login', query: { redirect: to.fullPath } }
  }

  if (to.meta.hideAuth && authenticated) {
    return { name: 'Home' }
  }

  if (to.meta.requiresSuperAdmin) {
    const userStore = useUserStore()

    if (authenticated && userStore.role == null) {
      try {
        await userStore.syncCurrentUser()
      } catch {
        return { name: 'Login', query: { redirect: to.fullPath } }
      }
    }

    if (!userStore.isAdminOrAbove) {
      return { name: 'Home' }
    }
  }

  return true
})

export default router
