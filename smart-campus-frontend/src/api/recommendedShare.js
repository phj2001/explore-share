import request from '@/utils/request.js'

export const getRecommendedShareList = (params) => {
  return request.get('/recommended-shares', { params })
}
