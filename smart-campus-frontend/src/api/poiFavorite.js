import request from '@/utils/request.js'

export const getFavoriteStatus = (poiId) => {
  return request.get(`/pois/${poiId}/favorite`)
}

export const addFavorite = (poiId) => {
  return request.post(`/pois/${poiId}/favorite`)
}

export const removeFavorite = (poiId) => {
  return request.delete(`/pois/${poiId}/favorite`)
}

export const getUserFavorites = (params = {}) => {
  return request.get('/users/me/favorites', { params })
}
