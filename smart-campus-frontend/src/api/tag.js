import request from '@/utils/request.js'

export const getHotTags = () => {
  return request.get('/tags/hot')
}

export const getShareTags = (shareId) => {
  return request.get(`/poi-shares/${shareId}/tags`)
}

export const updateShareTags = (shareId, tags) => {
  return request.put(`/poi-shares/${shareId}/tags`, { tags })
}
