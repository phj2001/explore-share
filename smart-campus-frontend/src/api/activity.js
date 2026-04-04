import request from '@/utils/request.js'
import { getCachedValue } from '@/utils/requestCache'

const ACTIVITY_LIST_CACHE_PREFIX = 'activity:list:'
const ACTIVITY_DETAIL_CACHE_PREFIX = 'activity:detail:'

export const getActivityList = (params, options = {}) => {
  const cacheKey = `${ACTIVITY_LIST_CACHE_PREFIX}${JSON.stringify(params || {})}`
  return getCachedValue(
    cacheKey,
    () => request.get('/activities', { params }),
    {
      ttl: 2 * 60_000,
      forceRefresh: options.forceRefresh
    }
  )
}

export const getActivityDetail = (activityId, options = {}) => {
  return getCachedValue(
    `${ACTIVITY_DETAIL_CACHE_PREFIX}${activityId}`,
    () => request.get(`/activities/${activityId}`),
    {
      ttl: 2 * 60_000,
      forceRefresh: options.forceRefresh
    }
  )
}
