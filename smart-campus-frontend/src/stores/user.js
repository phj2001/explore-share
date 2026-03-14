import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { login as loginApi, register as registerApi, getUserInfo } from '@/api/auth.js'
import { getToken, setToken, removeToken } from '@/utils/auth.js'

export const useUserStore = defineStore('user', () => {
  // 状态
  const token = ref(getToken() || '')
  const userInfo = ref(null)
  const isLoading = ref(false)

  // 计算属性
  const isLoggedIn = computed(() => !!token.value)
  const username = computed(() => userInfo.value?.username || '')

  /**
   * 登录
   */
  const login = async (username, password) => {
    isLoading.value = true
    try {
      const data = await loginApi(username, password)
      token.value = data.token
      userInfo.value = data.user || { username }
      setToken(data.token)
      return data
    } catch (error) {
      throw error
    } finally {
      isLoading.value = false
    }
  }

  /**
   * 注册
   */
  const register = async (username, password) => {
    isLoading.value = true
    try {
      const data = await registerApi(username, password)
      return data
    } finally {
      isLoading.value = false
    }
  }

  /**
   * 登出
   */
  const logout = () => {
    token.value = ''
    userInfo.value = null
    removeToken()
  }

  /**
   * 获取用户信息
   */
  const fetchUserInfo = async (id) => {
    if (!token.value || !id) {
      return userInfo.value
    }

    isLoading.value = true
    try {
      const data = await getUserInfo(id)
      userInfo.value = data
      return data
    } catch (error) {
      // token 可能失效，清除登录状态
      if (error.message?.includes('401') || error.message?.includes('未授权')) {
        logout()
      }
      throw error
    } finally {
      isLoading.value = false
    }
  }

  /**
   * 更新用户信息
   */
  const updateUserInfo = (info) => {
    userInfo.value = { ...userInfo.value, ...info }
  }

  return {
    // 状态
    token,
    userInfo,
    isLoading,
    // 计算属性
    isLoggedIn,
    username,
    // 方法
    login,
    register,
    logout,
    fetchUserInfo,
    updateUserInfo
  }
})
