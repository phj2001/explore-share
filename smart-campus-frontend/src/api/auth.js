import request from '@/utils/request.js'

export const register = (username, password, email, emailCode) => {
  return request.post('/auth/register', { username, password, email, emailCode })
}

export const login = (username, password) => {
  // skipAuthRedirect：凭证错误时后端返回 401，但用户本就在登录页，
  // 不应触发拦截器的"登录失效强制跳转"，交由页面静默显示错误文案即可。
  return request.post('/auth/login', { username, password }, { skipAuthRedirect: true })
}

export const checkUsername = (username) => {
  return request.get('/auth/check', {
    params: { username }
  })
}

export const getCurrentUser = () => {
  return request.get('/auth/me')
}

export const logout = () => {
  // skipAuthRedirect：登出场景下即使 token 已失效返回 401，也不需要拦截器再弹"登录失效"并强制跳转
  return request.delete('/auth/logout', { skipAuthRedirect: true })
}

export const sendRegisterCode = (email) => {
  return request.post('/auth/sendRegisterCode', { email })
}

export const sendResetCode = (email) => {
  return request.post('/auth/sendResetCode', { email })
}

export const resetPassword = (email, code, newPassword) => {
  return request.post('/auth/resetPassword', { email, code, newPassword })
}

export const sendBindCodeApi = (email) => {
  return request.post('/auth/sendBindCode', { email })
}

export const bindEmailApi = (email, emailCode) => {
  return request.post('/auth/bindEmail', { email, emailCode })
}
