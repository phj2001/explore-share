import request from '@/utils/request.js'

/**
 * 获取所有 POI
 */
export const getAllPOIs = () => {
  return request.get('/pois')
}

/**
 * 根据 ID 获取 POI
 */
export const getPOIById = (id) => {
  return request.get(`/pois/${id}`)
}

/**
 * 搜索 POI（按名称）
 */
export const searchPOIByName = (name) => {
  return request.get('/pois/search', {
    params: { name }
  })
}

/**
 * 按分类获取 POI
 */
export const getPOIsByCategory = (category) => {
  return request.get(`/pois/category/${category}`)
}

/**
 * 高级搜索 POI
 */
export const advancedSearchPOI = (params) => {
  return request.get('/pois/search/advanced', { params })
}

/**
 * 获取边界范围内的 POI
 */
export const getPOIsInBounds = (minLat, maxLat, minLng, maxLng) => {
  return request.get('/pois/bounds', {
    params: { minLat, maxLat, minLng, maxLng }
  })
}

/**
 * 获取所有分类
 */
export const getCategories = () => {
  return request.get('/pois/categories')
}

/**
 * 创建 POI（需要登录）
 */
export const createPOI = (data) => {
  return request.post('/pois', data)
}

/**
 * 更新 POI（需要登录）
 */
export const updatePOI = (id, data) => {
  return request.put(`/pois/${id}`, { id, ...data })
}

/**
 * 删除 POI（需要登录）
 */
export const deletePOI = (id) => {
  return request.delete(`/pois/${id}`)
}

/**
 * 批量导入 POI（需管理员）
 */
export const importPOIs = (file, options = {}) => {
  const formData = new FormData()
  formData.append('file', file)

  return request.post('/pois/import', formData, {
    params: {
      replaceExisting: Boolean(options.replaceExisting),
      skipDuplicates: options.skipDuplicates !== false
    }
  })
}
