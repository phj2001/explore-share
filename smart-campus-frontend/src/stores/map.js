import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { getPOIById } from '@/api/poi.js'
import { planRoute, getNearbyPOIs } from '@/api/route.js'
import { getDefaultCenter, getDefaultZoom } from '@/utils/map.js'

const ROUTE_MODES = [
  { value: 'walking', label: '步行' },
  { value: 'driving', label: '驾车' },
  { value: 'bicycling', label: '骑行' }
]

const formatDistance = (distanceMeters = 0) => {
  if (distanceMeters >= 1000) {
    return `${(distanceMeters / 1000).toFixed(1)} 公里`
  }
  return `${distanceMeters} 米`
}

const formatDuration = (durationSeconds = 0) => {
  const totalMinutes = Math.max(Math.ceil(durationSeconds / 60), 1)
  if (totalMinutes >= 60) {
    const hours = Math.floor(totalMinutes / 60)
    const minutes = totalMinutes % 60
    return minutes === 0 ? `${hours} 小时` : `${hours} 小时 ${minutes} 分钟`
  }
  return `${totalMinutes} 分钟`
}

const appendUniquePoints = (target, source = []) => {
  source.forEach((point) => {
    const lastPoint = target[target.length - 1]
    if (!lastPoint || lastPoint.lat !== point.lat || lastPoint.lng !== point.lng) {
      target.push(point)
    }
  })
}

const buildRoutePointFromWaypoint = (waypoint) => ({
  poiId: waypoint.poiId,
  name: waypoint.poiName,
  lat: Number(waypoint.latitude),
  lng: Number(waypoint.longitude),
  rawLat: Number(waypoint.latitude),
  rawLng: Number(waypoint.longitude),
  isTemporary: false
})

export const useMapStore = defineStore('map', () => {
  const center = ref(getDefaultCenter())
  const zoom = ref(getDefaultZoom())
  const isLoading = ref(false)

  const routeStart = ref(null)
  const routeEnd = ref(null)
  const routeResult = ref(null)
  const routeMode = ref('walking')
  const routePickMode = ref(null)
  const routeIntermediatePoints = ref([])
  const activeRecommendedRoute = ref(null)

  const selectedPOI = ref(null)

  const isPickingRoutePoint = computed(() => routePickMode.value === 'start' || routePickMode.value === 'end')
  const hasIntermediatePoints = computed(() => routeIntermediatePoints.value.length > 0)
  const activeRoutePoints = computed(() => {
    const points = []
    if (routeStart.value) {
      points.push(routeStart.value)
    }
    points.push(...routeIntermediatePoints.value)
    if (routeEnd.value) {
      points.push(routeEnd.value)
    }
    return points
  })

  const setCenter = (lat, lng) => {
    center.value = { lat, lng }
  }

  const setZoom = (level) => {
    zoom.value = level
  }

  const flyTo = (lat, lng, zoomLevel) => {
    center.value = { lat, lng }
    if (zoomLevel !== undefined) {
      zoom.value = zoomLevel
    }
  }

  const clearRecommendedRouteState = () => {
    routeIntermediatePoints.value = []
    activeRecommendedRoute.value = null
  }

  const setRouteStart = (point, options = {}) => {
    routeStart.value = point
    if (!options.preserveRecommended) {
      clearRecommendedRouteState()
    }
  }

  const setRouteEnd = (point, options = {}) => {
    routeEnd.value = point
    if (!options.preserveRecommended) {
      clearRecommendedRouteState()
    }
  }

  const setRouteMode = (mode) => {
    routeMode.value = mode
  }

  const setRoutePickMode = (mode) => {
    routePickMode.value = mode
  }

  const startPickingRoutePoint = (mode) => {
    routePickMode.value = mode
  }

  const cancelPickingRoutePoint = () => {
    routePickMode.value = null
  }

  const swapRoutePoints = () => {
    const sequence = activeRoutePoints.value
    if (sequence.length < 2) {
      return
    }
    const reversed = [...sequence].reverse()
    routeStart.value = reversed[0]
    routeEnd.value = reversed[reversed.length - 1]
    routeIntermediatePoints.value = reversed.slice(1, -1)
    activeRecommendedRoute.value = null
  }

  const clearRoute = () => {
    routeStart.value = null
    routeEnd.value = null
    routeResult.value = null
    routePickMode.value = null
    clearRecommendedRouteState()
  }

  const mergeRoutePlans = (plans, routePoints) => {
    const mergedPoints = []
    const mergedSteps = []
    let totalDistanceMeters = 0
    let totalDurationSeconds = 0

    plans.forEach((plan, index) => {
      totalDistanceMeters += Number(plan.distanceMeters || 0)
      totalDurationSeconds += Number(plan.durationSeconds || 0)
      appendUniquePoints(mergedPoints, plan.points || [])
      const nextPoint = routePoints[index + 1]
      ;(plan.steps || []).forEach((step, stepIndex) => {
        const instruction = step?.instruction || `继续前往 ${nextPoint?.name || '下一地点'}`
        mergedSteps.push({
          ...step,
          instruction: plans.length > 1 && stepIndex === 0 ? `前往 ${nextPoint?.name || '下一地点'}：${instruction}` : instruction
        })
      })
    })

    return {
      mode: routeMode.value,
      modeLabel: ROUTE_MODES.find((item) => item.value === routeMode.value)?.label || routeMode.value,
      startLat: routeStart.value?.lat ?? null,
      startLng: routeStart.value?.lng ?? null,
      endLat: routeEnd.value?.lat ?? null,
      endLng: routeEnd.value?.lng ?? null,
      distanceMeters: totalDistanceMeters,
      durationSeconds: totalDurationSeconds,
      distanceText: formatDistance(totalDistanceMeters),
      durationText: formatDuration(totalDurationSeconds),
      points: mergedPoints,
      steps: mergedSteps,
      waypoints: routePoints.map((point) => ({
        poiId: point.poiId,
        poiName: point.name,
        latitude: point.rawLat ?? point.lat,
        longitude: point.rawLng ?? point.lng
      }))
    }
  }

  const planRouteAsync = async () => {
    const points = activeRoutePoints.value
    if (points.length < 2) {
      throw new Error('请先选择至少两个地点')
    }

    isLoading.value = true
    try {
      if (points.length === 2) {
        const data = await planRoute(points[0].lat, points[0].lng, points[1].lat, points[1].lng, routeMode.value)
        routeResult.value = {
          ...data,
          waypoints: points.map((point) => ({
            poiId: point.poiId,
            poiName: point.name,
            latitude: point.rawLat ?? point.lat,
            longitude: point.rawLng ?? point.lng
          }))
        }
        return routeResult.value
      }

      const legPlans = []
      for (let index = 0; index < points.length - 1; index += 1) {
        const startPoint = points[index]
        const endPoint = points[index + 1]
        const plan = await planRoute(startPoint.lat, startPoint.lng, endPoint.lat, endPoint.lng, routeMode.value)
        legPlans.push(plan)
      }

      routeResult.value = mergeRoutePlans(legPlans, points)
      return routeResult.value
    } finally {
      isLoading.value = false
    }
  }

  const fetchNearbyPOIs = async (lat, lng, radius = 1000) => {
    isLoading.value = true
    try {
      return await getNearbyPOIs(lat, lng, radius)
    } finally {
      isLoading.value = false
    }
  }

  const selectPOI = (poi) => {
    selectedPOI.value = poi
    if (poi) {
      flyTo(Number(poi.latitude), Number(poi.longitude), 16)
    }
  }

  const clearSelectedPOI = () => {
    selectedPOI.value = null
  }

  const fetchPOIById = async (poiId) => {
    return await getPOIById(poiId)
  }

  const applyRecommendedRoute = async (routeDetail, options = {}) => {
    const waypoints = routeDetail?.waypoints || []
    if (waypoints.length < 2) {
      throw new Error('推荐路线至少需要两个地点')
    }

    const routePoints = waypoints.map(buildRoutePointFromWaypoint)
    routeStart.value = routePoints[0]
    routeEnd.value = routePoints[routePoints.length - 1]
    routeIntermediatePoints.value = routePoints.slice(1, -1)
    activeRecommendedRoute.value = {
      id: routeDetail.id,
      title: routeDetail.title,
      summary: routeDetail.summary,
      recommendationText: routeDetail.recommendationText,
      defaultMode: routeDetail.defaultMode,
      defaultModeLabel: routeDetail.defaultModeLabel,
      waypointCount: routePoints.length
    }
    routeMode.value = routeDetail.defaultMode || routeMode.value
    await planRouteAsync()

    if (options.focus !== false) {
      const firstPoint = routePoints[0]
      flyTo(firstPoint.rawLat ?? firstPoint.lat, firstPoint.rawLng ?? firstPoint.lng, 15)
    }
  }

  const reset = () => {
    center.value = getDefaultCenter()
    zoom.value = getDefaultZoom()
    selectedPOI.value = null
    clearRoute()
    routeMode.value = 'walking'
  }

  return {
    center,
    zoom,
    isLoading,
    routeStart,
    routeEnd,
    routeResult,
    routeMode,
    routeModes: ROUTE_MODES,
    routePickMode,
    routeIntermediatePoints,
    activeRecommendedRoute,
    activeRoutePoints,
    hasIntermediatePoints,
    isPickingRoutePoint,
    selectedPOI,
    setCenter,
    setZoom,
    flyTo,
    setRouteStart,
    setRouteEnd,
    setRouteMode,
    setRoutePickMode,
    startPickingRoutePoint,
    cancelPickingRoutePoint,
    swapRoutePoints,
    clearRoute,
    planRouteAsync,
    fetchNearbyPOIs,
    selectPOI,
    clearSelectedPOI,
    fetchPOIById,
    applyRecommendedRoute,
    reset
  }
})
