import request from '@/utils/request.js'
import { getCachedValue } from '@/utils/requestCache'

const RECOMMENDED_ROUTE_LIST_CACHE_PREFIX = 'recommended-route:list:'
const RECOMMENDED_ROUTE_DETAIL_CACHE_PREFIX = 'recommended-route:detail:'

export const getRecommendedRouteList = (params, options = {}) => {
  const cacheKey = `${RECOMMENDED_ROUTE_LIST_CACHE_PREFIX}${JSON.stringify(params || {})}`
  return getCachedValue(
    cacheKey,
    () => request.get('/recommended-routes', { params }),
    {
      ttl: 2 * 60_000,
      forceRefresh: options.forceRefresh
    }
  )
}

export const getRecommendedRouteDetail = (routeId, options = {}) => {
  return getCachedValue(
    `${RECOMMENDED_ROUTE_DETAIL_CACHE_PREFIX}${routeId}`,
    () => request.get(`/recommended-routes/${routeId}`),
    {
      ttl: 2 * 60_000,
      forceRefresh: options.forceRefresh
    }
  )
}
