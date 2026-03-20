import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import {
  getCurrentUser as getCurrentUserApi,
  login as loginApi,
  register as registerApi
} from '@/api/auth.js'
import { SUPER_ADMIN_ROLE } from '@/constants/auth.js'
import {
  clearAuthState,
  getStoredUserInfo,
  getToken,
  getUserInfoFromToken,
  setStoredUserInfo,
  setToken
} from '@/utils/auth.js'

const buildUserInfo = (data, fallbackUsername) => {
  if (data?.user) {
    return data.user
  }

  return {
    id: data?.id ?? data?.userId ?? null,
    username: data?.username || fallbackUsername || '',
    role: data?.role ?? null
  }
}

const getInitialUserInfo = () => {
  const storedUserInfo = getStoredUserInfo()
  if (storedUserInfo?.username) {
    return storedUserInfo
  }

  const token = getToken()
  const tokenUserInfo = getUserInfoFromToken(token)
  if (tokenUserInfo?.username) {
    setStoredUserInfo(tokenUserInfo)
    return tokenUserInfo
  }

  return storedUserInfo
}

export const useUserStore = defineStore('user', () => {
  const token = ref(getToken() || '')
  const userInfo = ref(getInitialUserInfo())
  const isLoading = ref(false)

  const isLoggedIn = computed(() => !!token.value)
  const username = computed(() => userInfo.value?.username || '')
  const role = computed(() => userInfo.value?.role ?? null)
  const isSuperAdmin = computed(() => role.value === SUPER_ADMIN_ROLE)

  const login = async (usernameInput, password) => {
    isLoading.value = true
    try {
      const data = await loginApi(usernameInput, password)
      const nextUserInfo = buildUserInfo(data, usernameInput)

      token.value = data.token
      userInfo.value = nextUserInfo

      setToken(data.token)
      setStoredUserInfo(nextUserInfo)

      return data
    } finally {
      isLoading.value = false
    }
  }

  const register = async (usernameInput, password) => {
    isLoading.value = true
    try {
      return await registerApi(usernameInput, password)
    } finally {
      isLoading.value = false
    }
  }

  const syncCurrentUser = async () => {
    if (!token.value) {
      return null
    }

    const data = await getCurrentUserApi()
    const nextUserInfo = buildUserInfo(data)
    userInfo.value = nextUserInfo
    setStoredUserInfo(nextUserInfo)
    return nextUserInfo
  }

  const logout = () => {
    token.value = ''
    userInfo.value = null
    clearAuthState()
  }

  return {
    token,
    userInfo,
    isLoading,
    isLoggedIn,
    username,
    role,
    isSuperAdmin,
    login,
    register,
    syncCurrentUser,
    logout
  }
})
