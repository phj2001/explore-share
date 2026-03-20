const PI = Math.PI
const AXIS = 6378245.0
const EE = 0.00669342162296594323

let amapLoaderPromise = null

const getEnvValue = (key) => {
  const value = import.meta.env[key]
  return typeof value === 'string' ? value.trim() : ''
}

const coordinateSource = (getEnvValue('VITE_COORDINATE_SOURCE') || 'wgs84').toLowerCase()
const amapKey = getEnvValue('VITE_AMAP_JS_KEY')
const securityJsCode = getEnvValue('VITE_AMAP_SECURITY_JS_CODE')

const outOfChina = (lng, lat) => {
  return lng < 72.004 || lng > 137.8347 || lat < 0.8293 || lat > 55.8271
}

const transformLat = (lng, lat) => {
  let ret =
    -100.0 +
    2.0 * lng +
    3.0 * lat +
    0.2 * lat * lat +
    0.1 * lng * lat +
    0.2 * Math.sqrt(Math.abs(lng))

  ret +=
    ((20.0 * Math.sin(6.0 * lng * PI) + 20.0 * Math.sin(2.0 * lng * PI)) * 2.0) /
    3.0
  ret +=
    ((20.0 * Math.sin(lat * PI) + 40.0 * Math.sin((lat / 3.0) * PI)) * 2.0) /
    3.0
  ret +=
    ((160.0 * Math.sin((lat / 12.0) * PI) + 320 * Math.sin((lat * PI) / 30.0)) * 2.0) /
    3.0

  return ret
}

const transformLng = (lng, lat) => {
  let ret =
    300.0 +
    lng +
    2.0 * lat +
    0.1 * lng * lng +
    0.1 * lng * lat +
    0.1 * Math.sqrt(Math.abs(lng))

  ret +=
    ((20.0 * Math.sin(6.0 * lng * PI) + 20.0 * Math.sin(2.0 * lng * PI)) * 2.0) /
    3.0
  ret +=
    ((20.0 * Math.sin(lng * PI) + 40.0 * Math.sin((lng / 3.0) * PI)) * 2.0) /
    3.0
  ret +=
    ((150.0 * Math.sin((lng / 12.0) * PI) + 300.0 * Math.sin((lng / 30.0) * PI)) * 2.0) /
    3.0

  return ret
}

export const wgs84ToGcj02 = (lng, lat) => {
  if (outOfChina(lng, lat)) {
    return [lng, lat]
  }

  let dLat = transformLat(lng - 105.0, lat - 35.0)
  let dLng = transformLng(lng - 105.0, lat - 35.0)
  const radLat = (lat / 180.0) * PI
  let magic = Math.sin(radLat)
  magic = 1 - EE * magic * magic
  const sqrtMagic = Math.sqrt(magic)
  dLat = (dLat * 180.0) / (((AXIS * (1 - EE)) / (magic * sqrtMagic)) * PI)
  dLng = (dLng * 180.0) / ((AXIS / sqrtMagic) * Math.cos(radLat) * PI)

  return [lng + dLng, lat + dLat]
}

export const gcj02ToWgs84 = (lng, lat) => {
  if (outOfChina(lng, lat)) {
    return [lng, lat]
  }

  const [convertedLng, convertedLat] = wgs84ToGcj02(lng, lat)
  return [lng * 2 - convertedLng, lat * 2 - convertedLat]
}

export const toAmapCoordinate = (lat, lng) => {
  const numericLat = Number(lat)
  const numericLng = Number(lng)
  if (!Number.isFinite(numericLat) || !Number.isFinite(numericLng)) {
    return null
  }

  if (coordinateSource === 'gcj02') {
    return { lat: numericLat, lng: numericLng }
  }

  const [gcjLng, gcjLat] = wgs84ToGcj02(numericLng, numericLat)
  return { lat: gcjLat, lng: gcjLng }
}

export const fromAmapCoordinate = (lat, lng) => {
  const numericLat = Number(lat)
  const numericLng = Number(lng)
  if (!Number.isFinite(numericLat) || !Number.isFinite(numericLng)) {
    return null
  }

  if (coordinateSource === 'gcj02') {
    return { lat: numericLat, lng: numericLng }
  }

  const [wgsLng, wgsLat] = gcj02ToWgs84(numericLng, numericLat)
  return { lat: wgsLat, lng: wgsLng }
}

export const normalizePoiForAmap = (poi) => {
  if (!poi) {
    return null
  }

  const coordinate = toAmapCoordinate(poi.latitude, poi.longitude)
  if (!coordinate) {
    return null
  }

  return {
    ...poi,
    mapLat: coordinate.lat,
    mapLng: coordinate.lng
  }
}

export const loadAmapSdk = () => {
  if (window.AMap) {
    return Promise.resolve(window.AMap)
  }

  if (amapLoaderPromise) {
    return amapLoaderPromise
  }

  if (!amapKey) {
    return Promise.reject(new Error('Missing VITE_AMAP_JS_KEY'))
  }

  if (securityJsCode) {
    window._AMapSecurityConfig = {
      securityJsCode
    }
  }

  amapLoaderPromise = new Promise((resolve, reject) => {
    const existingScript = document.querySelector('script[data-amap-sdk="true"]')

    const handleLoad = () => {
      if (window.AMap) {
        resolve(window.AMap)
        return
      }

      amapLoaderPromise = null
      reject(new Error('AMap loaded without global object'))
    }

    const handleError = () => {
      amapLoaderPromise = null
      reject(new Error('Failed to load AMap script'))
    }

    if (existingScript) {
      existingScript.addEventListener('load', handleLoad, { once: true })
      existingScript.addEventListener('error', handleError, { once: true })
      return
    }

    const script = document.createElement('script')
    script.src = `https://webapi.amap.com/maps?v=2.0&key=${encodeURIComponent(amapKey)}`
    script.async = true
    script.defer = true
    script.dataset.amapSdk = 'true'
    script.addEventListener('load', handleLoad, { once: true })
    script.addEventListener('error', handleError, { once: true })
    document.head.appendChild(script)
  })

  return amapLoaderPromise
}
