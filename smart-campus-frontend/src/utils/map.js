/**
 * 地图工具函数
 */

/**
 * 计算两点之间的距离（米）
 * 使用 Haversine 公式
 */
export const calculateDistance = (lat1, lng1, lat2, lng2) => {
  const R = 6371000 // 地球半径（米）
  const dLat = toRad(lat2 - lat1)
  const dLng = toRad(lng2 - lng1)

  const a =
    Math.sin(dLat / 2) * Math.sin(dLat / 2) +
    Math.cos(toRad(lat1)) *
      Math.cos(toRad(lat2)) *
      Math.sin(dLng / 2) *
      Math.sin(dLng / 2)

  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
  return R * c
}

/**
 * 角度转弧度
 */
const toRad = (degrees) => {
  return degrees * (Math.PI / 180)
}

/**
 * 格式化坐标显示
 */
export const formatCoordinate = (lat, lng) => {
  return `${lat.toFixed(6)}, ${lng.toFixed(6)}`
}

/**
 * 验证坐标是否有效
 */
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

/**
 * 获取地图默认中心点
 */
export const getDefaultCenter = () => {
  return { lat: 34.2215, lng: 108.9838 } // 北京
}

/**
 * 获取地图默认缩放级别
 */
export const getDefaultZoom = () => {
  return 15
}
