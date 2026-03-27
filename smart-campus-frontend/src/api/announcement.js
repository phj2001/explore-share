import request from '@/utils/request.js'

export const getAnnouncementList = (params) => {
  return request.get('/announcements', { params })
}

export const getAnnouncementDetail = (announcementId) => {
  return request.get(`/announcements/${announcementId}`)
}
