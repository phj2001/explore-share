import request from '@/utils/request.js'
import { getCachedValue } from '@/utils/requestCache'

const RECOMMENDED_SHARE_LIST_CACHE_PREFIX = 'recommended-share:list:'

export const getRecommendedShareList = (params, options = {}) => {
  const cacheKey = `${RECOMMENDED_SHARE_LIST_CACHE_PREFIX}${JSON.stringify(params || {})}`
  return getCachedValue(
    cacheKey,
    () => request.get('/recommended-shares', { params }),
    {
      ttl: 2 * 60_000,
      forceRefresh: options.forceRefresh
    }
  )
}
