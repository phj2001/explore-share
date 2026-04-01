import request from '@/utils/request.js'

const buildActivityFormData = (payload = {}) => {
  const formData = new FormData()

  formData.append('title', payload.title?.trim() || '')
  formData.append('summary', payload.summary?.trim() || '')
  formData.append('content', payload.content?.trim() || '')
  formData.append('startTime', payload.startTime || '')
  formData.append('endTime', payload.endTime || '')
  formData.append('status', String(payload.status ?? 0))

  if (payload.poiId) {
    formData.append('poiId', String(payload.poiId))
  }

  if (payload.removeCoverImage) {
    formData.append('removeCoverImage', 'true')
  }

  if (payload.coverImage) {
    formData.append('coverImage', payload.coverImage)
  }

  return formData
}

export const getAdminActivityPage = (params) => {
  return request.get('/admin/activities', { params })
}

export const getAdminActivityDetail = (activityId) => {
  return request.get(`/admin/activities/${activityId}`)
}

export const createAdminActivity = (payload) => {
  return request.post('/admin/activities', buildActivityFormData(payload), {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

export const updateAdminActivity = (activityId, payload) => {
  return request.put(`/admin/activities/${activityId}`, buildActivityFormData(payload), {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

export const updateAdminActivityPublishStatus = (activityId, published) => {
  return request.put(`/admin/activities/${activityId}/publish`, null, {
    params: { published }
  })
}

export const deleteAdminActivity = (activityId) => {
  return request.delete(`/admin/activities/${activityId}`)
}
