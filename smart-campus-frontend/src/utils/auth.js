const TOKEN_KEY = 'smart_campus_token'

/**
 * 获取 token
 */
export const getToken = () => {
  return localStorage.getItem(TOKEN_KEY)
}

/**
 * 设置 token
 */
export const setToken = (token) => {
  localStorage.setItem(TOKEN_KEY, token)
}

/**
 * 移除 token
 */
export const removeToken = () => {
  localStorage.removeItem(TOKEN_KEY)
}

/**
 * 检查是否已登录
 */
export const isAuthenticated = () => {
  return !!getToken()
}
