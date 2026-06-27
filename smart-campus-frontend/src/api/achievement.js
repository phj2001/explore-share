import request from '@/utils/request.js'

export const getAllAchievements = () => {
  return request.get('/achievements')
}

export const getMyAchievements = () => {
  return request.get('/users/me/achievements')
}

export const getUserAchievements = (userId) => {
  return request.get(`/users/${userId}/achievements`)
}
