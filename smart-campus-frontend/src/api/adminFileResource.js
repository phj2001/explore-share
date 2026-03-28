import request from '@/utils/request.js'

export const getAdminFileResourcePage = (params) => {
  return request.get('/admin/files', { params })
}

export const deleteAdminFileResource = (params) => {
  return request.delete('/admin/files', { params })
}
