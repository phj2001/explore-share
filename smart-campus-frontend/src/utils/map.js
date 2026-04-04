export const calculateDistance = (lat1, lng1, lat2, lng2) => {
  const earthRadiusMeters = 6371000
  const dLat = toRadians(lat2 - lat1)
  const dLng = toRadians(lng2 - lng1)

  const a =
    Math.sin(dLat / 2) * Math.sin(dLat / 2) +
    Math.cos(toRadians(lat1)) *
      Math.cos(toRadians(lat2)) *
      Math.sin(dLng / 2) *
      Math.sin(dLng / 2)

  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
  return earthRadiusMeters * c
}

const toRadians = (degrees) => {
  return degrees * (Math.PI / 180)
}

export const formatCoordinate = (lat, lng) => {
  return `${Number(lat).toFixed(6)}, ${Number(lng).toFixed(6)}`
}

export const isValidCoordinate = (lat, lng) => {
  return (
    typeof lat === 'number' &&
    typeof lng === 'number' &&
    lat >= -90 &&
    lat <= 90 &&
    lng >= -180 &&
    lng <= 180
  )
}

const DEFAULT_MAP_CENTER = { lat: 35.8617, lng: 104.1954 }
const DEFAULT_MAP_ZOOM = 5

export const getDefaultCenter = () => {
  return DEFAULT_MAP_CENTER
}

export const getDefaultZoom = () => {
  return DEFAULT_MAP_ZOOM
}
