import request from '@/utils/request.js'

const buildAnnouncementFormData = (payload = {}) => {
  const formData = new FormData()

  formData.append('title', payload.title?.trim() || '')
  formData.append('summary', payload.summary?.trim() || '')
  formData.append('content', payload.content?.trim() || '')
  formData.append('status', String(payload.status ?? 0))
  formData.append('pinned', payload.pinned ? 'true' : 'false')

  if (payload.removeCoverImage) {
    formData.append('removeCoverImage', 'true')
  }

  if (payload.coverImage) {
    formData.append('coverImage', payload.coverImage)
  }

  return formData
}

export const getAdminAnnouncementPage = (params) => {
  return request.get('/admin/announcements', { params })
}

export const getAdminAnnouncementDetail = (announcementId) => {
  return request.get(`/admin/announcements/${announcementId}`)
}

export const createAdminAnnouncement = (payload) => {
  return request.post('/admin/announcements', buildAnnouncementFormData(payload), {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

export const updateAdminAnnouncement = (announcementId, payload) => {
  return request.put(`/admin/announcements/${announcementId}`, buildAnnouncementFormData(payload), {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

export const updateAdminAnnouncementPublishStatus = (announcementId, published) => {
  return request.put(`/admin/announcements/${announcementId}/publish`, null, {
    params: { published }
  })
}

export const updateAdminAnnouncementPinnedStatus = (announcementId, pinned) => {
  return request.put(`/admin/announcements/${announcementId}/pin`, null, {
    params: { pinned }
  })
}

export const deleteAdminAnnouncement = (announcementId) => {
  return request.delete(`/admin/announcements/${announcementId}`)
}
