import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import {
  getCurrentUser as getCurrentUserApi,
  login as loginApi,
  register as registerApi
} from '@/api/auth.js'
import { getMyProfile as getMyProfileApi } from '@/api/user.js'
import { SUPER_ADMIN_ROLE } from '@/constants/auth.js'
import {
  clearAuthState,
  getStoredUserInfo,
  getToken,
  getUserInfoFromToken,
  setStoredUserInfo,
  setToken
} from '@/utils/auth.js'
import { API_ORIGIN } from '@/utils/request.js'

const resolveAssetUrl = (value) => {
  if (!value) {
    return ''
  }

  if (/^https?:\/\//i.test(value)) {
    return value
  }

  return `${API_ORIGIN}${value.startsWith('/') ? value : `/${value}`}`
}

const buildUserInfo = (data, fallbackUsername) => {
  if (data?.user) {
    return buildUserInfo(data.user, fallbackUsername)
  }

  return {
    id: data?.id ?? data?.userId ?? null,
    username: data?.username || fallbackUsername || '',
    displayName: data?.displayName || '',
    avatarUrl: resolveAssetUrl(data?.avatarUrl || ''),
    bio: data?.bio || '',
    role: data?.role ?? null
  }
}

const getInitialUserInfo = () => {
  const storedUserInfo = getStoredUserInfo()
  if (storedUserInfo?.username) {
    return buildUserInfo(storedUserInfo, storedUserInfo.username)
  }

  const token = getToken()
  const tokenUserInfo = getUserInfoFromToken(token)
  if (tokenUserInfo?.username) {
    const nextUserInfo = buildUserInfo(tokenUserInfo, tokenUserInfo.username)
    setStoredUserInfo(nextUserInfo)
    return nextUserInfo
  }

  return storedUserInfo
}

export const useUserStore = defineStore('user', () => {
  const token = ref(getToken() || '')
  const userInfo = ref(getInitialUserInfo())
  const isLoading = ref(false)

  const isLoggedIn = computed(() => !!token.value)
  const username = computed(() => userInfo.value?.username || '')
  const displayName = computed(() => userInfo.value?.displayName || userInfo.value?.username || '')
  const avatarUrl = computed(() => userInfo.value?.avatarUrl || '')
  const bio = computed(() => userInfo.value?.bio || '')
  const role = computed(() => userInfo.value?.role ?? null)
  const isSuperAdmin = computed(() => role.value === SUPER_ADMIN_ROLE)

  const applyUserInfo = (data, fallbackUsername) => {
    const nextUserInfo = buildUserInfo(data, fallbackUsername)
    userInfo.value = nextUserInfo
    setStoredUserInfo(nextUserInfo)
    return nextUserInfo
  }

  const login = async (usernameInput, password) => {
    isLoading.value = true
    try {
      const data = await loginApi(usernameInput, password)
      token.value = data.token
      setToken(data.token)
      applyUserInfo(data, usernameInput)
      await syncCurrentUser()
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
    return applyUserInfo(data)
  }

  const fetchMyProfile = async () => {
    if (!token.value) {
      return null
    }

    const data = await getMyProfileApi()
    return applyUserInfo(data)
  }

  const updateUserInfo = (data) => {
    return applyUserInfo(data, userInfo.value?.username)
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
    displayName,
    avatarUrl,
    bio,
    role,
    isSuperAdmin,
    login,
    register,
    syncCurrentUser,
    fetchMyProfile,
    updateUserInfo,
    logout
  }
})
