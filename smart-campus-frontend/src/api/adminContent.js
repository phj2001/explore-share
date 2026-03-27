import request from '@/utils/request.js'

export const getAdminSharePage = (params) => {
  return request.get('/admin/shares', { params })
}

export const getAdminShareDetail = (shareId) => {
  return request.get(`/admin/shares/${shareId}`)
}

export const deleteAdminShare = (shareId) => {
  return request.delete(`/admin/shares/${shareId}`)
}

export const getAdminReplyPage = (params) => {
  return request.get('/admin/replies', { params })
}

export const deleteAdminReply = (replyId) => {
  return request.delete(`/admin/replies/${replyId}`)
}
