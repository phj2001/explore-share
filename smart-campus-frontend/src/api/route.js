import request from '@/utils/request.js'

/**
 * 路径规划
 */
export const planRoute = (startLat, startLng, endLat, endLng) => {
  return request.post('/routes/plan', null, {
    params: { startLat, startLng, endLat, endLng }
  })
}

/**
 * 查找附近 POI
 */
export const getNearbyPOIs = (lat, lng, radius) => {
  return request.get('/routes/nearby', {
    params: { lat, lng, radius }
  })
}
