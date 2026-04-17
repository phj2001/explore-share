import request from '@/utils/request.js'

export const register = (username, password, email, emailCode) => {
  return request.post('/auth/register', { username, password, email, emailCode })
}

export const login = (username, password) => {
  return request.post('/auth/login', { username, password })
}

export const checkUsername = (username) => {
  return request.get('/auth/check', {
    params: { username }
  })
}

export const getCurrentUser = () => {
  return request.get('/auth/me')
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
