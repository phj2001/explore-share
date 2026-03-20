<template>
  <div class="map-container">
    <div ref="mapRoot" class="map-view"></div>

    <RoutePolyline />

    <div v-if="sdkError" class="map-error">
      <h3>AMap failed to load</h3>
      <p>{{ sdkError }}</p>
      <p>Check `VITE_AMAP_JS_KEY` and the AMap domain whitelist.</p>
    </div>

    <el-dialog
      v-model="showDetailDialog"
      :title="selectedPOI?.name"
      width="420px"
      destroy-on-close
    >
      <div v-if="selectedPOI" class="poi-detail">
        <p><strong>Category:</strong> {{ selectedPOI.category }}</p>
        <p><strong>Coordinate:</strong> {{ selectedPOI.latitude }}, {{ selectedPOI.longitude }}</p>
        <p><strong>Description:</strong> {{ selectedPOI.description || 'N/A' }}</p>
      </div>

      <template #footer>
        <div class="dialog-actions">
          <el-button @click="setAsRoutePoint('start')">Set Start</el-button>
          <el-button type="primary" plain @click="setAsRoutePoint('end')">Set End</el-button>
          <el-button @click="showDetailDialog = false">Close</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import RoutePolyline from '@/components/map/RoutePolyline.vue'
import { usePOIStore } from '@/stores/poi'
import { useMapStore } from '@/stores/map'
import {
  fromAmapCoordinate,
  loadAmapSdk,
  normalizePoiForAmap,
  toAmapCoordinate
} from '@/utils/amap'

const poiStore = usePOIStore()
const mapStore = useMapStore()

const mapRoot = ref(null)
const showDetailDialog = ref(false)
const selectedPOI = ref(null)
const sdkError = ref('')

let AMapRef = null
let map = null
let poiMarkers = []
let routePolyline = null
let routeEndpointMarkers = []

const getFitViewPadding = () => {
  return window.innerWidth <= 768 ? [80, 80, 320, 80] : [80, 420, 80, 80]
}

const createPoiMarkerContent = () => {
  return `
    <div style="
      width: 18px;
      height: 18px;
      border-radius: 999px;
      border: 3px solid #ffffff;
      background: linear-gradient(135deg, #0f766e, #0ea5e9);
      box-shadow: 0 10px 20px rgba(14, 165, 233, 0.32);
    "></div>
  `
}

const createEndpointMarkerContent = (label, colors) => {
  return `
    <div style="
      width: 36px;
      height: 36px;
      border-radius: 999px;
      display: flex;
      align-items: center;
      justify-content: center;
      color: #ffffff;
      font-size: 14px;
      font-weight: 700;
      border: 3px solid rgba(255, 255, 255, 0.92);
      background: linear-gradient(135deg, ${colors[0]}, ${colors[1]});
      box-shadow: 0 14px 24px rgba(15, 23, 42, 0.24);
    ">${label}</div>
  `
}

const clearPoiMarkers = () => {
  if (map && poiMarkers.length) {
    map.remove(poiMarkers)
  }
  poiMarkers = []
}

const clearRoutePolyline = () => {
  if (map && routePolyline) {
    map.remove(routePolyline)
  }
  routePolyline = null
}

const clearRouteEndpoints = () => {
  if (map && routeEndpointMarkers.length) {
    map.remove(routeEndpointMarkers)
  }
  routeEndpointMarkers = []
}

const getPoiMapPosition = (poi) => {
  const normalizedPoi = normalizePoiForAmap(poi)
  if (!normalizedPoi) {
    return null
  }

  return [normalizedPoi.mapLng, normalizedPoi.mapLat]
}

const syncMapCenterToStore = () => {
  if (!map) return

  const center = map.getCenter()
  const convertedCenter = fromAmapCoordinate(center.getLat(), center.getLng())
  if (convertedCenter) {
    mapStore.setCenter(convertedCenter.lat, convertedCenter.lng)
  }

  mapStore.setZoom(map.getZoom())
}

const fitRouteView = () => {
  if (!map) return

  const overlays = [...routeEndpointMarkers]
  if (routePolyline) {
    overlays.unshift(routePolyline)
  }

  if (!overlays.length) {
    return
  }

  map.setFitView(overlays, false, getFitViewPadding())
}

const focusSelectedPoi = (poi) => {
  if (!map || !poi) return

  const position = getPoiMapPosition(poi)
  if (!position) return

  map.setZoomAndCenter(Math.max(map.getZoom(), 17), position)
}

const initMap = async () => {
  AMapRef = await loadAmapSdk()

  const center = toAmapCoordinate(mapStore.center.lat, mapStore.center.lng)
  if (!center) {
    throw new Error('Invalid default map center')
  }

  map = new AMapRef.Map(mapRoot.value, {
    viewMode: '2D',
    zoom: mapStore.zoom,
    center: [center.lng, center.lat],
    resizeEnable: true,
    zooms: [3, 20]
  })

  map.on('moveend', handleMapMove)
  map.on('zoomend', syncMapCenterToStore)
}

const loadPOIs = async () => {
  try {
    await Promise.all([poiStore.fetchAllPOIs(), poiStore.fetchCategories()])
    renderMarkers()
  } catch (error) {
    ElMessage.error('Failed to load POIs')
  }
}

const renderMarkers = () => {
  if (!map || !AMapRef) return

  clearPoiMarkers()

  poiMarkers = poiStore.poiList
    .map((poi) => {
      const position = getPoiMapPosition(poi)
      if (!position) {
        return null
      }

      const marker = new AMapRef.Marker({
        position,
        title: poi.name,
        anchor: 'center',
        content: createPoiMarkerContent(),
        offset: new AMapRef.Pixel(-9, -9),
        topWhenClick: true
      })

      marker.on('click', () => {
        selectedPOI.value = poi
        showDetailDialog.value = true
        mapStore.selectPOI(poi)
        focusSelectedPoi(poi)
      })

      return marker
    })
    .filter(Boolean)

  if (poiMarkers.length) {
    map.add(poiMarkers)
  }
}

const drawRouteEndpoints = () => {
  if (!map || !AMapRef) return

  clearRouteEndpoints()

  const start = mapStore.routeStart
  const end = mapStore.routeEnd

  if (start) {
    routeEndpointMarkers.push(
      new AMapRef.Marker({
        position: [Number(start.lng), Number(start.lat)],
        anchor: 'center',
        content: createEndpointMarkerContent('S', ['#15803d', '#22c55e']),
        offset: new AMapRef.Pixel(-18, -18),
        title: start.name || 'Start'
      })
    )
  }

  if (end) {
    routeEndpointMarkers.push(
      new AMapRef.Marker({
        position: [Number(end.lng), Number(end.lat)],
        anchor: 'center',
        content: createEndpointMarkerContent('E', ['#b91c1c', '#ef4444']),
        offset: new AMapRef.Pixel(-18, -18),
        title: end.name || 'End'
      })
    )
  }

  if (routeEndpointMarkers.length) {
    map.add(routeEndpointMarkers)
  }
}

const drawRoute = () => {
  if (!map || !AMapRef) return

  clearRoutePolyline()
  drawRouteEndpoints()

  const points = mapStore.routeResult?.points
  if (!points?.length) {
    return
  }

  routePolyline = new AMapRef.Polyline({
    path: points.map((point) => [Number(point.lng), Number(point.lat)]),
    strokeColor: '#0ea5e9',
    strokeOpacity: 0.92,
    strokeWeight: 7,
    strokeStyle: 'solid',
    lineJoin: 'round',
    lineCap: 'round',
    showDir: true
  })

  map.add(routePolyline)
  fitRouteView()
}

const setAsRoutePoint = (type) => {
  if (!selectedPOI.value) return

  const mapCoordinate = toAmapCoordinate(selectedPOI.value.latitude, selectedPOI.value.longitude)
  if (!mapCoordinate) {
    ElMessage.error('Invalid POI coordinate')
    return
  }

  const point = {
    poiId: selectedPOI.value.id,
    name: selectedPOI.value.name,
    lat: mapCoordinate.lat,
    lng: mapCoordinate.lng,
    rawLat: Number(selectedPOI.value.latitude),
    rawLng: Number(selectedPOI.value.longitude)
  }

  if (type === 'start') {
    mapStore.setRouteStart(point)
    ElMessage.success(`Set ${point.name} as start`)
  } else {
    mapStore.setRouteEnd(point)
    ElMessage.success(`Set ${point.name} as end`)
  }

  showDetailDialog.value = false
}

const handleMapMove = async () => {
  if (!map) return

  syncMapCenterToStore()

  const bounds = map.getBounds()
  const southWest = bounds.getSouthWest()
  const northEast = bounds.getNorthEast()
  const convertedSouthWest = fromAmapCoordinate(southWest.getLat(), southWest.getLng())
  const convertedNorthEast = fromAmapCoordinate(northEast.getLat(), northEast.getLng())

  if (!convertedSouthWest || !convertedNorthEast) {
    return
  }

  try {
    await poiStore.fetchInBounds(
      convertedSouthWest.lat,
      convertedNorthEast.lat,
      convertedSouthWest.lng,
      convertedNorthEast.lng
    )
  } catch (error) {
    // Keep the current interaction responsive if viewport sync fails.
  }
}

onMounted(async () => {
  try {
    await initMap()
    await loadPOIs()
    drawRoute()
  } catch (error) {
    sdkError.value = error.message || 'Failed to initialize AMap'
    ElMessage.error(sdkError.value)
  }
})

onUnmounted(() => {
  if (map) {
    map.off('moveend', handleMapMove)
    map.off('zoomend', syncMapCenterToStore)
    map.destroy()
    map = null
  }

  poiMarkers = []
  routePolyline = null
  routeEndpointMarkers = []
})

watch(
  () => poiStore.poiList,
  () => {
    renderMarkers()
  },
  { deep: true }
)

watch(
  () => mapStore.routeResult,
  () => {
    drawRoute()
  },
  { deep: true }
)

watch(
  () => [mapStore.routeStart, mapStore.routeEnd],
  () => {
    drawRouteEndpoints()
  },
  { deep: true }
)

watch(
  () => mapStore.selectedPOI,
  (poi) => {
    if (poi) {
      focusSelectedPoi(poi)
    }
  },
  { deep: true }
)
</script>

<style scoped>
.map-container {
  position: relative;
  width: 100%;
  height: calc(100vh - 60px);
  background:
    radial-gradient(circle at top right, rgba(14, 165, 233, 0.15), transparent 28%),
    linear-gradient(180deg, #f8fafc, #e2e8f0);
}

.map-view {
  width: 100%;
  height: 100%;
}

.map-error {
  position: absolute;
  left: 20px;
  bottom: 20px;
  z-index: 1200;
  max-width: 360px;
  padding: 16px 18px;
  border-radius: 16px;
  background: rgba(15, 23, 42, 0.92);
  color: #e2e8f0;
  box-shadow: 0 20px 40px rgba(15, 23, 42, 0.28);
  backdrop-filter: blur(10px);
}

.map-error h3 {
  margin: 0 0 8px;
  font-size: 16px;
}

.map-error p {
  margin: 0;
  line-height: 1.6;
  font-size: 13px;
}

.map-error p + p {
  margin-top: 6px;
}

.poi-detail p {
  margin: 10px 0;
  line-height: 1.6;
}

.dialog-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

@media (max-width: 768px) {
  .map-container {
    height: calc(100vh - 56px);
  }

  .map-error {
    left: 12px;
    right: 12px;
    bottom: 84px;
    max-width: none;
  }
}
</style>
