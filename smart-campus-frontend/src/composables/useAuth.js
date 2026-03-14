import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'

/**
 * 认证相关的组合式函数
 */
export function useAuth() {
  const router = useRouter()
  const userStore = useUserStore()

  // 是否已登录
  const isLoggedIn = computed(() => userStore.isLoggedIn)

  // 用户信息
  const userInfo = computed(() => userStore.userInfo)

  // 用户名
  const username = computed(() => userStore.username)

  // 是否正在加载
  const isLoading = computed(() => userStore.isLoading)

  /**
   * 登录
   */
  const login = async (username, password) => {
    try {
      const data = await userStore.login(username, password)
      ElMessage.success('登录成功')
      return data
    } catch (error) {
      ElMessage.error(error.message || '登录失败')
      throw error
    }
  }

  /**
   * 注册
   */
  const register = async (username, password) => {
    try {
      const data = await userStore.register(username, password)
      ElMessage.success('注册成功')
      return data
    } catch (error) {
      ElMessage.error(error.message || '注册失败')
      throw error
    }
  }

  /**
   * 登出
   */
  const logout = () => {
    userStore.logout()
    ElMessage.success('已退出登录')
    router.push('/login')
  }

  /**
   * 检查登录状态，未登录则跳转登录页
   */
  const requireAuth = () => {
    if (!isLoggedIn.value) {
      ElMessage.warning('请先登录')
      router.push({
        path: '/login',
        query: { redirect: router.currentRoute.value.fullPath }
      })
      return false
    }
    return true
  }

  /**
   * 刷新用户信息
   */
  const refreshUserInfo = async (id) => {
    if (!id) {
      ElMessage.warning('缺少用户 ID')
      return null
    }
    try {
      const data = await userStore.fetchUserInfo(id)
      return data
    } catch (error) {
      ElMessage.error('获取用户信息失败')
      throw error
    }
  }

  return {
    isLoggedIn,
    userInfo,
    username,
    isLoading,
    login,
    register,
    logout,
    requireAuth,
    refreshUserInfo
  }
}
