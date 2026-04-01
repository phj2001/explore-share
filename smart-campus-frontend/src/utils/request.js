import axios from 'axios'
import { ElMessage } from 'element-plus'
import { clearAuthState, getToken } from '@/utils/auth.js'

export const BASE_URL = 'http://localhost:8080/api'
export const API_ORIGIN = BASE_URL.replace(/\/api$/, '')
const TIMEOUT = 10000

let handlingUnauthorized = false

const redirectToLogin = () => {
  if (handlingUnauthorized) {
    return
  }

  handlingUnauthorized = true
  clearAuthState()
  ElMessage.warning('登录状态已失效，请重新登录')

  const currentPath = `${window.location.pathname}${window.location.search}`
  const redirectQuery = currentPath && currentPath !== '/login'
    ? `?redirect=${encodeURIComponent(currentPath)}`
    : ''

  window.setTimeout(() => {
    window.location.href = `/login${redirectQuery}`
    handlingUnauthorized = false
  }, 150)
}

const request = axios.create({
  baseURL: BASE_URL,
  timeout: TIMEOUT
})

request.interceptors.request.use(
  (config) => {
    const token = getToken()
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

request.interceptors.response.use(
  (response) => {
    const { code, message, data } = response.data
    if (code === 200) {
      return data
    }

    return Promise.reject(new Error(message || '请求失败'))
  },
  (error) => {
    if (error.response) {
      const { status, data } = error.response

      switch (status) {
        case 400:
          error.message = data.message || '请求参数错误'
          break
        case 401:
          error.message = data.message || '登录状态已失效，请重新登录'
          redirectToLogin()
          break
        case 403:
          error.message = data.message || '当前账号没有访问权限'
          break
        case 404:
          error.message = data.message || '请求的资源不存在'
          break
        case 500:
          error.message = data.message || '服务器内部错误'
          break
        default:
          error.message = data.message || `请求失败 (${status})`
      }
    } else if (error.code === 'ECONNABORTED') {
      error.message = '请求超时，请稍后重试'
    } else if (error.request) {
      error.message = '无法连接到服务，请确认后端服务已启动'
    } else {
      error.message = error.message || '请求配置错误'
    }

    return Promise.reject(error)
  }
)

export default request
