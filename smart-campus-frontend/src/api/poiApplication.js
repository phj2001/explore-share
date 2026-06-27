import request from '@/utils/request.js'

export const submitPOIApplication = (data) => {
  return request.post('/poi-applications', data)
}

export const getMyApplications = (params = {}) => {
  return request.get('/poi-applications/my', { params })
}

export const getAdminPOIApplications = (params = {}) => {
  return request.get('/admin/poi-applications', { params })
}

export const getAdminPOIApplicationDetail = (id) => {
  return request.get(`/admin/poi-applications/${id}`)
}

export const reviewPOIApplication = (id, data) => {
  return request.put(`/admin/poi-applications/${id}/review`, data)
}
