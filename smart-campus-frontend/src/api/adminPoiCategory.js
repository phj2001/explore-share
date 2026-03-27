import request from '@/utils/request.js'

export const getAdminPoiCategories = () => {
  return request.get('/admin/poi-categories')
}

export const renameAdminPoiCategory = (categoryName, newName) => {
  return request.put(`/admin/poi-categories/${encodeURIComponent(categoryName)}`, { newName })
}

export const deleteAdminPoiCategory = (categoryName) => {
  return request.delete(`/admin/poi-categories/${encodeURIComponent(categoryName)}`)
}
