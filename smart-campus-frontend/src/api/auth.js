import request from '@/utils/request.js'

export const register = (username, password) => {
  return request.post('/auth/register', { username, password })
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
