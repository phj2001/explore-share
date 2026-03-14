import axios from 'axios'

// 基础配置
const BASE_URL = 'http://localhost:8080/api'
const TIMEOUT = 10000
const TOKEN_KEY = 'smart_campus_token'

// 创建 axios 实例
const request = axios.create({
  baseURL: BASE_URL,
  timeout: TIMEOUT
})

// 请求拦截器 - 自动携带 token
request.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem(TOKEN_KEY)
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// 响应拦截器 - 统一处理错误响应
request.interceptors.response.use(
  (response) => {
    const { code, message, data } = response.data

    // 成功响应
    if (code === 200) {
      return data
    }

    // 业务错误
    return Promise.reject(new Error(message || '请求失败'))
  },
  (error) => {
    // HTTP 错误
    if (error.response) {
      const { status, data } = error.response

      switch (status) {
        case 400:
          error.message = data.message || '请求参数错误'
          break
        case 401:
          error.message = '未授权，请先登录'
          // 清除过期 token
          localStorage.removeItem(TOKEN_KEY)
          // 跳转到登录页
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
      // 请求已发出但没有收到响应
      error.message = '网络连接失败，请检查网络'
    } else {
      // 请求配置出错
      error.message = error.message || '请求配置错误'
    }

    return Promise.reject(error)
  }
)

export default request
