import request from '@/utils/request.js'

export const getAdminRecommendedSharePage = (params) => {
  return request.get('/admin/recommended-shares', { params })
}

export const getAdminRecommendedShareCandidatePage = (params) => {
  return request.get('/admin/recommended-shares/candidates', { params })
}

export const createAdminRecommendedShare = (payload) => {
  return request.post('/admin/recommended-shares', payload)
}

export const updateAdminRecommendedShare = (id, payload) => {
  return request.put(`/admin/recommended-shares/${id}`, payload)
}

export const deleteAdminRecommendedShare = (id) => {
  return request.delete(`/admin/recommended-shares/${id}`)
}
