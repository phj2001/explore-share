const TOKEN_KEY = 'smart_campus_token'
const USER_INFO_KEY = 'smart_campus_user_info'

const decodeBase64Url = (value) => {
  const normalized = value.replace(/-/g, '+').replace(/_/g, '/')
  const padded = normalized.padEnd(Math.ceil(normalized.length / 4) * 4, '=')
  return atob(padded)
}

export const getToken = () => {
  return localStorage.getItem(TOKEN_KEY)
}

export const setToken = (token) => {
  localStorage.setItem(TOKEN_KEY, token)
}

export const removeToken = () => {
  localStorage.removeItem(TOKEN_KEY)
}

export const getStoredUserInfo = () => {
  const rawValue = localStorage.getItem(USER_INFO_KEY)
  if (!rawValue) {
    return null
  }

  try {
    return JSON.parse(rawValue)
  } catch {
    localStorage.removeItem(USER_INFO_KEY)
    return null
  }
}

export const getUserInfoFromToken = (token) => {
  if (!token) {
    return null
  }

  try {
    const [, payload] = token.split('.')
    if (!payload) {
      return null
    }

    const parsedPayload = JSON.parse(decodeBase64Url(payload))
    return {
      id: parsedPayload.sub ? Number(parsedPayload.sub) : null,
      username: parsedPayload.username || '',
      role: parsedPayload.role ?? null
    }
  } catch {
    return null
  }
}

export const setStoredUserInfo = (userInfo) => {
  localStorage.setItem(USER_INFO_KEY, JSON.stringify(userInfo))
}

export const removeStoredUserInfo = () => {
  localStorage.removeItem(USER_INFO_KEY)
}

export const clearAuthState = () => {
  removeToken()
  removeStoredUserInfo()
}

export const isAuthenticated = () => {
  return !!getToken()
}
