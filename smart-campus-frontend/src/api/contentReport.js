import request from '@/utils/request.js'

export const createShareReport = (shareId, payload) => {
  return request.post(`/content-reports/shares/${shareId}`, payload)
}

export const createReplyReport = (replyId, payload) => {
  return request.post(`/content-reports/replies/${replyId}`, payload)
}
