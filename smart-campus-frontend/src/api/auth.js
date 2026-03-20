import request from '@/utils/request.js'

export const register = (username, password) => {
  return request.post('/auth/register', { username, password })
}

export const login = (username, password) => {
  return request.post('/auth/login', null, {
    params: { username, password }
  })
}

export const checkUsername = (username) => {
  return request.get('/auth/check', {
    params: { username }
  })
}
