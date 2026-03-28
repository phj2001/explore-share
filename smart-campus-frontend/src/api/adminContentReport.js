import request from '@/utils/request.js'

export const getAdminContentReportPage = (params) => {
  return request.get('/admin/content-reports', { params })
}

export const getAdminContentReportDetail = (reportId) => {
  return request.get(`/admin/content-reports/${reportId}`)
}

export const reviewAdminContentReport = (reportId, payload) => {
  return request.put(`/admin/content-reports/${reportId}/review`, payload)
}
