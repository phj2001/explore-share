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
    path: '/admin',
    name: 'Admin',
    component: () => import('@/views/Admin/Dashboard.vue'),
    meta: { title: '管理后台', requiresAuth: true, requiresSuperAdmin: true },
    redirect: '/admin/poi',
    children: [
      {
        path: 'poi',
        name: 'POIList',
        component: () => import('@/views/Admin/POIList.vue'),
        meta: { title: 'POI 管理', requiresAuth: true, requiresSuperAdmin: true }
      },
      {
        path: 'poi/create',
        name: 'POICreate',
        component: () => import('@/views/Admin/POIForm.vue'),
        meta: { title: '创建 POI', requiresAuth: true, requiresSuperAdmin: true }
      },
      {
        path: 'poi/edit/:id',
        name: 'POIEdit',
        component: () => import('@/views/Admin/POIForm.vue'),
        meta: { title: '编辑 POI', requiresAuth: true, requiresSuperAdmin: true }
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

router.beforeEach(async (to, from, next) => {
  document.title = to.meta.title ? `${to.meta.title} - 智慧校园` : '智慧校园'

  const authenticated = isAuthenticated()

  if (to.meta.requiresAuth && !authenticated) {
    next({ name: 'Login', query: { redirect: to.fullPath } })
    return
  }

  if (to.meta.hideAuth && authenticated) {
    next({ name: 'Home' })
    return
  }

  if (to.meta.requiresSuperAdmin) {
    const userStore = useUserStore()

    if (authenticated && userStore.role == null) {
      try {
        await userStore.syncCurrentUser()
      } catch {
        next({ name: 'Login', query: { redirect: to.fullPath } })
        return
      }
    }

    if (!userStore.isSuperAdmin) {
      next({ name: 'Home' })
      return
    }
  }

  next()
})

export default router
