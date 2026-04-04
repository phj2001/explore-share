import request from '@/utils/request.js'
import { clearCachedValue, getCachedValue } from '@/utils/requestCache'

const SHARE_CACHE_PREFIX = 'poi-shares:'
const REPLY_CACHE_PREFIX = 'poi-share-replies:'
const SHARE_CACHE_TTL = 20_000

const normalizePageParam = (value, fallback) => {
  const numericValue = Number(value)
  return Number.isFinite(numericValue) && numericValue >= 0 ? numericValue : fallback
}

const clearPoiShareCache = () => {
  clearCachedValue(SHARE_CACHE_PREFIX)
  clearCachedValue(REPLY_CACHE_PREFIX)
}

export const getPoiSharePage = (poiId, params = {}, options = {}) => {
  const page = normalizePageParam(params.page, 0)
  const size = normalizePageParam(params.size, 10)
  const cacheKeySuffix = options.cacheKeySuffix ? `:${options.cacheKeySuffix}` : ''
  const cacheKey = `${SHARE_CACHE_PREFIX}${poiId}:page:${page}:size:${size}${cacheKeySuffix}`

  return getCachedValue(
    cacheKey,
    () => request.get(`/poi-shares/poi/${poiId}`, { params: { ...params, page, size } }),
    {
      ttl: SHARE_CACHE_TTL,
      forceRefresh: Boolean(options.forceRefresh)
    }
  )
}

export const createPoiShare = (poiId, payload) => {
  const formData = new FormData()

  if (payload.content?.trim()) {
    formData.append('content', payload.content.trim())
  }

  ;(payload.images || []).forEach((image) => {
    formData.append('images', image)
  })

  return request.post(`/poi-shares/poi/${poiId}`, formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  }).finally(() => {
    clearPoiShareCache()
  })
}

export const deletePoiShare = (shareId) => {
  return request.delete(`/poi-shares/${shareId}`).finally(() => {
    clearPoiShareCache()
  })
}

export const likePoiShare = (shareId) => {
  return request.post(`/poi-shares/${shareId}/likes`).finally(() => {
    clearPoiShareCache()
  })
}

export const unlikePoiShare = (shareId) => {
  return request.delete(`/poi-shares/${shareId}/likes`).finally(() => {
    clearPoiShareCache()
  })
}

export const getPoiShareReplies = (shareId, params = {}, options = {}) => {
  const page = normalizePageParam(params.page, 0)
  const size = normalizePageParam(params.size, 3)
  const cacheKeySuffix = options.cacheKeySuffix ? `:${options.cacheKeySuffix}` : ''
  const cacheKey = `${REPLY_CACHE_PREFIX}${shareId}:page:${page}:size:${size}${cacheKeySuffix}`

  return getCachedValue(
    cacheKey,
    () => request.get(`/poi-shares/${shareId}/replies`, { params: { ...params, page, size } }),
    {
      ttl: SHARE_CACHE_TTL,
      forceRefresh: Boolean(options.forceRefresh)
    }
  )
}

export const createPoiShareReply = (shareId, payload) => {
  return request.post(`/poi-shares/${shareId}/replies`, payload).finally(() => {
    clearPoiShareCache()
  })
}

export const deletePoiShareReply = (replyId) => {
  return request.delete(`/poi-shares/replies/${replyId}`).finally(() => {
    clearPoiShareCache()
  })
}
