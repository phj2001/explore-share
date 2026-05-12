import request from '@/utils/request.js'

export const getPoiGallery = (poiId, params = {}) => {
  return request.get(`/pois/${poiId}/gallery`, { params })
}
