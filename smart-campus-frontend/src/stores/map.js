import { defineStore } from 'pinia'
import { ref } from 'vue'
import { planRoute, getNearbyPOIs } from '@/api/route.js'
import { getDefaultCenter, getDefaultZoom } from '@/utils/map.js'

const ROUTE_MODES = [
  { value: 'walking', label: 'Walk' },
  { value: 'driving', label: 'Drive' },
  { value: 'bicycling', label: 'Bike' }
]

export const useMapStore = defineStore('map', () => {
  const center = ref(getDefaultCenter())
  const zoom = ref(getDefaultZoom())
  const isLoading = ref(false)

  const routeStart = ref(null)
  const routeEnd = ref(null)
  const routeResult = ref(null)
  const routeMode = ref('walking')

  const selectedPOI = ref(null)

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

  const setRouteStart = (point) => {
    routeStart.value = point
  }

  const setRouteEnd = (point) => {
    routeEnd.value = point
  }

  const setRouteMode = (mode) => {
    routeMode.value = mode
  }

  const swapRoutePoints = () => {
    const nextStart = routeEnd.value
    routeEnd.value = routeStart.value
    routeStart.value = nextStart
  }

  const clearRoute = () => {
    routeStart.value = null
    routeEnd.value = null
    routeResult.value = null
  }

  const planRouteAsync = async () => {
    if (!routeStart.value || !routeEnd.value) {
      throw new Error('Please choose both start and end points')
    }

    isLoading.value = true
    try {
      const data = await planRoute(
        routeStart.value.lat,
        routeStart.value.lng,
        routeEnd.value.lat,
        routeEnd.value.lng,
        routeMode.value
      )
      routeResult.value = data
      return data
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
    selectedPOI,
    setCenter,
    setZoom,
    flyTo,
    setRouteStart,
    setRouteEnd,
    setRouteMode,
    swapRoutePoints,
    clearRoute,
    planRouteAsync,
    fetchNearbyPOIs,
    selectPOI,
    clearSelectedPOI,
    reset
  }
})
