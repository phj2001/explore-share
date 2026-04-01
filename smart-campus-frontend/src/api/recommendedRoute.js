import request from '@/utils/request.js'

export const getRecommendedRouteList = (params) => {
  return request.get('/recommended-routes', { params })
}

export const getRecommendedRouteDetail = (routeId) => {
  return request.get(`/recommended-routes/${routeId}`)
}
