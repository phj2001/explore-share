import request from '@/utils/request.js'

export const getPoiSharePage = (poiId, params) => {
  return request.get(`/poi-shares/poi/${poiId}`, { params })
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
  })
}

export const deletePoiShare = (shareId) => {
  return request.delete(`/poi-shares/${shareId}`)
}

export const likePoiShare = (shareId) => {
  return request.post(`/poi-shares/${shareId}/likes`)
}

export const unlikePoiShare = (shareId) => {
  return request.delete(`/poi-shares/${shareId}/likes`)
}

export const getPoiShareReplies = (shareId, params) => {
  return request.get(`/poi-shares/${shareId}/replies`, { params })
}

export const createPoiShareReply = (shareId, payload) => {
  return request.post(`/poi-shares/${shareId}/replies`, payload)
}

export const deletePoiShareReply = (replyId) => {
  return request.delete(`/poi-shares/replies/${replyId}`)
}
