import request from '@/utils/request.js'

export const getNotifications = (params = {}) => {
  return request.get('/notifications', { params })
}

export const getUnreadCount = () => {
  return request.get('/notifications/unread-count')
}

export const markAsRead = (id) => {
  return request.post(`/notifications/${id}/read`)
}

export const markAllAsRead = () => {
  return request.post('/notifications/read-all')
}
