import request from '@/utils/request.js'

export const getAdminOperationLogPage = (params) => {
  return request.get('/admin/operation-logs', { params })
}
