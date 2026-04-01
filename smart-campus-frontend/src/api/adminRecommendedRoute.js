import request from '@/utils/request.js'

const buildRouteFormData = (payload) => {
  const formData = new FormData()
  formData.append('title', payload.title)
  formData.append('summary', payload.summary)
  formData.append('description', payload.description)
  if (payload.recommendationText) {
    formData.append('recommendationText', payload.recommendationText)
  }
  formData.append('sortOrder', String(payload.sortOrder))
  formData.append('defaultMode', payload.defaultMode)
  formData.append('status', String(payload.status))
  ;(payload.poiIds || []).forEach((poiId) => {
    formData.append('poiIds', String(poiId))
  })
  if (payload.removeCoverImage) {
    formData.append('removeCoverImage', 'true')
  }
  if (payload.coverImage) {
    formData.append('coverImage', payload.coverImage)
  }
  return formData
}

export const getAdminRecommendedRoutePage = (params) => {
  return request.get('/admin/recommended-routes', { params })
}

export const getAdminRecommendedRouteDetail = (routeId) => {
  return request.get(`/admin/recommended-routes/${routeId}`)
}

export const createAdminRecommendedRoute = (payload) => {
  return request.post('/admin/recommended-routes', buildRouteFormData(payload), {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export const updateAdminRecommendedRoute = (routeId, payload) => {
  return request.put(`/admin/recommended-routes/${routeId}`, buildRouteFormData(payload), {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export const updateAdminRecommendedRoutePublishStatus = (routeId, published) => {
  return request.put(`/admin/recommended-routes/${routeId}/publish`, null, {
    params: { published }
  })
}

export const deleteAdminRecommendedRoute = (routeId) => {
  return request.delete(`/admin/recommended-routes/${routeId}`)
}
