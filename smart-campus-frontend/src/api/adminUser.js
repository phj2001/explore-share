import request from '@/utils/request.js'

export const getAdminUserPage = (params) => {
  return request.get('/admin/users', { params })
}

export const getAdminUserDetail = (userId) => {
  return request.get(`/admin/users/${userId}`)
}

export const updateAdminUserRole = (userId, role, canResetPassword) => {
  return request.put(`/admin/users/${userId}/role`, { role, canResetPassword })
}

export const updateAdminUserCanResetPassword = (userId, canResetPassword) => {
  return request.put(`/admin/users/${userId}/can-reset-password`, { canResetPassword })
}

export const resetAdminUserPassword = (userId, newPassword) => {
  return request.put(`/admin/users/${userId}/password`, { newPassword })
}

export const updateAdminUserStatus = (userId, status) => {
  return request.put(`/admin/users/${userId}/status`, { status })
}
