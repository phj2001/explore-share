import request from '@/utils/request.js'

export const followUser = (userId) => {
  return request.post(`/users/${userId}/follow`)
}

export const unfollowUser = (userId) => {
  return request.delete(`/users/${userId}/follow`)
}

export const getFollowStatus = (userId) => {
  return request.get(`/users/${userId}/follow-status`)
}

export const getFollowingList = (userId, params = {}) => {
  return request.get(`/users/${userId}/following`, { params })
}

export const getFollowerList = (userId, params = {}) => {
  return request.get(`/users/${userId}/followers`, { params })
}
