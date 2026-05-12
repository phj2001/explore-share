import request from '@/utils/request.js'

export const getLeaderboard = (params = {}) => {
  return request.get('/leaderboard', { params })
}

export const getHotPois = (params = {}) => {
  return request.get('/leaderboard/hot-pois', { params })
}
