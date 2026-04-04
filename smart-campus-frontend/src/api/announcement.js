import request from '@/utils/request.js'
import { getCachedValue } from '@/utils/requestCache'

const ANNOUNCEMENT_LIST_CACHE_PREFIX = 'announcement:list:'
const ANNOUNCEMENT_DETAIL_CACHE_PREFIX = 'announcement:detail:'

export const getAnnouncementList = (params, options = {}) => {
  const cacheKey = `${ANNOUNCEMENT_LIST_CACHE_PREFIX}${JSON.stringify(params || {})}`
  return getCachedValue(
    cacheKey,
    () => request.get('/announcements', { params }),
    {
      ttl: 2 * 60_000,
      forceRefresh: options.forceRefresh
    }
  )
}

export const getAnnouncementDetail = (announcementId, options = {}) => {
  return getCachedValue(
    `${ANNOUNCEMENT_DETAIL_CACHE_PREFIX}${announcementId}`,
    () => request.get(`/announcements/${announcementId}`),
    {
      ttl: 2 * 60_000,
      forceRefresh: options.forceRefresh
    }
  )
}
