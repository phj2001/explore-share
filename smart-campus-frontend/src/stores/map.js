import { defineStore } from 'pinia'
import { ref } from 'vue'
import { planRoute, getNearbyPOIs } from '@/api/route.js'
import { getDefaultCenter, getDefaultZoom } from '@/utils/map.js'

export const useMapStore = defineStore('map', () => {
  // 地图状态
  const center = ref(getDefaultCenter())
  const zoom = ref(getDefaultZoom())
  const isLoading = ref(false)

  // 路径规划状态
  const routeStart = ref(null)
  const routeEnd = ref(null)
  const routeResult = ref(null)

  // 选中的 POI
  const selectedPOI = ref(null)

  /**
   * 设置地图中心
   */
  const setCenter = (lat, lng) => {
    center.value = { lat, lng }
  }

  /**
   * 设置缩放级别
   */
  const setZoom = (level) => {
    zoom.value = level
  }

  /**
   * 移动地图到指定位置
   */
  const flyTo = (lat, lng, zoomLevel) => {
    center.value = { lat, lng }
    if (zoomLevel !== undefined) {
      zoom.value = zoomLevel
    }
  }

  /**
   * 设置路径起点
   */
  const setRouteStart = (point) => {
    routeStart.value = point
  }

  /**
   * 设置路径终点
   */
  const setRouteEnd = (point) => {
    routeEnd.value = point
  }

  /**
   * 清空路径规划
   */
  const clearRoute = () => {
    routeStart.value = null
    routeEnd.value = null
    routeResult.value = null
  }

  /**
   * 路径规划
   */
  const planRouteAsync = async () => {
    if (!routeStart.value || !routeEnd.value) {
      throw new Error('请设置起点和终点')
    }

    isLoading.value = true
    try {
      const data = await planRoute(
        routeStart.value.lat,
        routeStart.value.lng,
        routeEnd.value.lat,
        routeEnd.value.lng
      )
      routeResult.value = data
      return data
    } catch (error) {
      throw error
    } finally {
      isLoading.value = false
    }
  }

  /**
   * 查找附近 POI
   */
  const fetchNearbyPOIs = async (lat, lng, radius = 1000) => {
    isLoading.value = true
    try {
      const data = await getNearbyPOIs(lat, lng, radius)
      return data
    } finally {
      isLoading.value = false
    }
  }

  /**
   * 选中 POI
   */
  const selectPOI = (poi) => {
    selectedPOI.value = poi
    if (poi) {
      flyTo(poi.latitude, poi.longitude, 16)
    }
  }

  /**
   * 清空选中 POI
   */
  const clearSelectedPOI = () => {
    selectedPOI.value = null
  }

  /**
   * 重置地图状态
   */
  const reset = () => {
    center.value = getDefaultCenter()
    zoom.value = getDefaultZoom()
    selectedPOI.value = null
    clearRoute()
  }

  return {
    // 状态
    center,
    zoom,
    isLoading,
    routeStart,
    routeEnd,
    routeResult,
    selectedPOI,
    // 方法
    setCenter,
    setZoom,
    flyTo,
    setRouteStart,
    setRouteEnd,
    clearRoute,
    planRouteAsync,
    fetchNearbyPOIs,
    selectPOI,
    clearSelectedPOI,
    reset
  }
})
