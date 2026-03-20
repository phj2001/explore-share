import request from '@/utils/request.js'

export const planRoute = (startLat, startLng, endLat, endLng, mode = 'walking') => {
  return request.get('/routes/plan', {
    params: { startLat, startLng, endLat, endLng, mode }
  })
}

export const getNearbyPOIs = (lat, lng, radius) => {
  return request.get('/routes/nearby', {
    params: { lat, lng, radius }
  })
}
