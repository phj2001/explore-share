import request from '@/utils/request.js'

export const getRatingSummary = (poiId) => {
  return request.get(`/pois/${poiId}/rating`)
}

export const getPoiReviews = (poiId, params = {}) => {
  return request.get(`/pois/${poiId}/reviews`, { params })
}

export const createOrUpdateReview = (poiId, data) => {
  return request.post(`/pois/${poiId}/reviews`, data)
}

export const deleteReview = (reviewId) => {
  return request.delete(`/pois/reviews/${reviewId}`)
}

export const getUserReviews = (params = {}) => {
  return request.get('/users/me/reviews', { params })
}

export const getAdminReviews = (params = {}) => {
  return request.get('/admin/reviews', { params })
}

export const adminDeleteReview = (reviewId) => {
  return request.delete(`/admin/reviews/${reviewId}`)
}
