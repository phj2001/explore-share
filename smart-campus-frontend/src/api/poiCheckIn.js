import request from '@/utils/request.js'

export const getPOICheckInStatus = (poiId) => {
  return request.get(`/pois/${poiId}/check-in`)
}

export const checkInPOI = (poiId) => {
  return request.post(`/pois/${poiId}/check-in`)
}

export const cancelCheckInPOI = (poiId) => {
  return request.delete(`/pois/${poiId}/check-in`)
}
