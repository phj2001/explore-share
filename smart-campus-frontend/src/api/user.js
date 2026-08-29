import request from '@/utils/request.js'

export const getMyProfile = () => {
  return request.get('/users/me/profile')
}

export const updateMyProfile = (payload) => {
  return request.put('/users/me/profile', payload)
}

export const changeMyPassword = (payload) => {
  return request.put('/users/me/password', payload)
}

export const uploadMyAvatar = (file) => {
  const formData = new FormData()
  formData.append('file', file)

  return request.post('/users/me/avatar', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

export const getUserPublicProfile = (userId) => {
  return request.get(`/users/${userId}/profile`)
}

export const getUserPublicShares = (userId, params = {}) => {
  return request.get(`/users/${userId}/shares`, { params })
}

export const getUserCheckIns = (userId, params = {}) => {
  return request.get(`/users/${userId}/checkins`, { params })
}

export const deleteAccountApi = (password) => {
  return request.post('/users/me/account/deletion', { password })
}
