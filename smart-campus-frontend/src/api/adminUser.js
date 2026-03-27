import request from '@/utils/request.js'

export const getAdminUserPage = (params) => {
  return request.get('/admin/users', { params })
}

export const getAdminUserDetail = (userId) => {
  return request.get(`/admin/users/${userId}`)
}

export const updateAdminUserRole = (userId, role) => {
  return request.put(`/admin/users/${userId}/role`, { role })
}

export const updateAdminUserStatus = (userId, status) => {
  return request.put(`/admin/users/${userId}/status`, { status })
}
