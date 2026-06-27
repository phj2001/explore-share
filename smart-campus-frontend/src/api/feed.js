import request from '@/utils/request.js'

export const getFeed = (params = {}) => {
  return request.get('/feed', { params })
}
