import axios from 'axios'
import { clearAuthState, getToken } from '@/utils/auth.js'

const BASE_URL = 'http://localhost:8080/api'
const TIMEOUT = 10000

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
          error.message = '未授权，请先登录'
          clearAuthState()
          window.location.href = '/login'
          break
        case 403:
          error.message = '无权访问'
          break
        case 404:
          error.message = '资源不存在'
          break
        case 500:
          error.message = '服务器内部错误'
          break
        default:
          error.message = data.message || `请求失败 (${status})`
      }
    } else if (error.request) {
      error.message = '网络连接失败，请检查网络'
    } else {
      error.message = error.message || '请求配置错误'
    }

    return Promise.reject(error)
  }
)

export default request
