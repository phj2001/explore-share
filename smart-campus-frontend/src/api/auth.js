import request from '@/utils/request.js'

/**
 * 用户注册
 */
export const register = (username, password) => {
  return request.post('/auth/register', { username, password })
}

/**
 * 用户登录
 */
export const login = (username, password) => {
  return request.post('/auth/login', null, {
    params: { username, password }
  })
}

/**
 * 检查用户名是否存在
 */
export const checkUsername = (username) => {
  return request.get('/auth/check', {
    params: { username }
  })
}

/**
 * 获取用户信息
 */
export const getUserInfo = (id) => {
  return request.get(`/auth/user/${id}`)
}
