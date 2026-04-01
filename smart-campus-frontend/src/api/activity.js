import request from '@/utils/request.js'

export const getActivityList = (params) => {
  return request.get('/activities', { params })
}

export const getActivityDetail = (activityId) => {
  return request.get(`/activities/${activityId}`)
}
