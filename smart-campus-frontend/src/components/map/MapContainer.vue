<template>
  <section class="map-shell">
    <div class="map-container">
      <div ref="mapRoot" class="map-view"></div>

      <RoutePolyline />

      <div v-if="mapStore.isPickingRoutePoint" class="map-pick-tip">
        请在地图中点击，设置{{ mapStore.routePickMode === 'start' ? '起点' : '终点' }}。
      </div>

      <div v-if="sdkError" class="map-error">
        <h3>地图加载失败</h3>
        <p>{{ sdkError }}</p>
        <p>请检查 `VITE_AMAP_JS_KEY` 与高德地图白名单配置。</p>
      </div>
    </div>

    <el-dialog
      v-model="showDetailDialog"
      :title="selectedPOI?.name"
      width="920px"
      destroy-on-close
      class="poi-dialog"
      @closed="handleDialogClosed"
    >
      <div v-if="selectedPOI" class="poi-dialog-content">
        <section class="poi-overview">
          <div class="poi-overview-card">
            <span class="poi-category">{{ selectedPOI.category }}</span>
            <h3>{{ selectedPOI.name }}</h3>
            <p>{{ selectedPOI.description || '这个地点暂时还没有详细介绍。' }}</p>
          </div>

          <div class="poi-meta-grid">
            <div class="meta-card">
              <span>纬度</span>
              <strong>{{ selectedPOI.latitude }}</strong>
            </div>
            <div class="meta-card">
              <span>经度</span>
              <strong>{{ selectedPOI.longitude }}</strong>
            </div>
          </div>

          <div class="poi-actions">
            <el-button @click="setAsRoutePoint('start')">设为起点</el-button>
            <el-button type="primary" plain @click="setAsRoutePoint('end')">设为终点</el-button>
          </div>
        </section>

        <PoiSharePanel :poi="selectedPOI" />
      </div>

      <template #footer>
        <div class="dialog-actions">
          <el-button @click="showDetailDialog = false">关闭</el-button>
        </div>
      </template>
    </el-dialog>
  </section>
</template>

<script setup>
import { ref, onMounted, onUnmounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import RoutePolyline from '@/components/map/RoutePolyline.vue'
import PoiSharePanel from '@/components/map/PoiSharePanel.vue'
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
  return window.innerWidth <= 768 ? [80, 80, 340, 80] : [80, 420, 80, 80]
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
      user-select: none;
      -webkit-user-select: none;
      caret-color: transparent;
      outline: none;
    "></div>
  `
}

const createEndpointMarkerContent = (label, colors, size = 36, pointName = '') => {
  const compactName = pointName
    ? `<span style="max-width:${size * 2.4}px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;font-size:10px;font-weight:600;opacity:0.95;">${pointName}</span>`
    : ''
  return `
    <div style="
      min-width: ${size}px;
      min-height: ${size}px;
      padding: 0 10px;
      border-radius: 999px;
      display: inline-flex;
      align-items: center;
      justify-content: center;
      gap: 8px;
      color: #ffffff;
      font-size: 14px;
      font-weight: 700;
      border: 3px solid rgba(255, 255, 255, 0.92);
      background: linear-gradient(135deg, ${colors[0]}, ${colors[1]});
      box-shadow: 0 14px 24px rgba(15, 23, 42, 0.24);
      user-select: none;
      -webkit-user-select: none;
      caret-color: transparent;
      outline: none;
    ">
      <span>${label}</span>
      ${compactName}
    </div>
  `
}

const clearMarkerFocusArtifacts = () => {
  if (document.activeElement instanceof HTMLElement) {
    document.activeElement.blur()
  }

  const selection = window.getSelection?.()
  if (selection && selection.rangeCount > 0) {
    selection.removeAllRanges()
  }
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

  const currentCenter = map.getCenter()
  const convertedCenter = fromAmapCoordinate(currentCenter.getLat(), currentCenter.getLng())
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

const buildTemporaryRoutePoint = (mode, lat, lng) => {
  const convertedCoordinate = fromAmapCoordinate(lat, lng)
  const rawLat = convertedCoordinate?.lat ?? lat
  const rawLng = convertedCoordinate?.lng ?? lng
  const pointName = mode === 'start' ? '自定义起点' : '自定义终点'

  return {
    poiId: null,
    name: pointName,
    lat,
    lng,
    rawLat,
    rawLng,
    isTemporary: true
  }
}

const applyTemporaryRoutePoint = (mode, lat, lng) => {
  const point = buildTemporaryRoutePoint(mode, lat, lng)
  if (mode === 'start') {
    mapStore.setRouteStart(point)
  } else {
    mapStore.setRouteEnd(point)
  }

  mapStore.cancelPickingRoutePoint()
  ElMessage.success(`已设置${point.name}`)
}

const updateMapCursor = () => {
  if (!map || !mapRoot.value) return

  mapRoot.value.style.cursor = mapStore.isPickingRoutePoint ? 'crosshair' : ''
}

const handleMapClick = (event) => {
  if (!mapStore.routePickMode) {
    return
  }

  const clickedLat = event.lnglat.getLat()
  const clickedLng = event.lnglat.getLng()
  applyTemporaryRoutePoint(mapStore.routePickMode, clickedLat, clickedLng)
}

const initMap = async () => {
  AMapRef = await loadAmapSdk()

  const currentCenter = toAmapCoordinate(mapStore.center.lat, mapStore.center.lng)
  if (!currentCenter) {
    throw new Error('默认地图中心坐标无效')
  }

  map = new AMapRef.Map(mapRoot.value, {
    viewMode: '2D',
    zoom: mapStore.zoom,
    center: [currentCenter.lng, currentCenter.lat],
    resizeEnable: true,
    zooms: [3, 20]
  })

  map.on('moveend', handleMapMove)
  map.on('zoomend', syncMapCenterToStore)
  map.on('click', handleMapClick)
  updateMapCursor()
}

const loadPOIs = async () => {
  try {
    await Promise.all([poiStore.fetchAllPOIs(), poiStore.fetchCategories()])
    renderMarkers()
  } catch {
    ElMessage.error('加载地点失败')
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
        clearMarkerFocusArtifacts()
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

  const points = mapStore.activeRoutePoints
  if (!points.length) {
    return
  }

  routeEndpointMarkers = points.map((point, index) => {
    const isStart = index === 0
    const isEnd = index === points.length - 1
    const label = isStart ? '起' : isEnd ? '终' : String(index)
    const colors = isStart
      ? ['#15803d', '#22c55e']
      : isEnd
        ? ['#b91c1c', '#ef4444']
        : ['#0f766e', '#0ea5e9']

    const size = isStart || isEnd ? 36 : 32
    const offset = isStart || isEnd ? -18 : -16

    return new AMapRef.Marker({
      position: [Number(point.lng), Number(point.lat)],
      anchor: 'center',
      content: createEndpointMarkerContent(label, colors, size, point.name),
      offset: new AMapRef.Pixel(offset, offset),
      title: point.name || (isStart ? '起点' : isEnd ? '终点' : '途经点')
    })
  })

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
    ElMessage.error('地点坐标无效')
    return
  }

  const point = {
    poiId: selectedPOI.value.id,
    name: selectedPOI.value.name,
    lat: mapCoordinate.lat,
    lng: mapCoordinate.lng,
    rawLat: Number(selectedPOI.value.latitude),
    rawLng: Number(selectedPOI.value.longitude),
    isTemporary: false
  }

  if (type === 'start') {
    mapStore.setRouteStart(point)
    ElMessage.success(`已将 ${point.name} 设为起点`)
  } else {
    mapStore.setRouteEnd(point)
    ElMessage.success(`已将 ${point.name} 设为终点`)
  }

  showDetailDialog.value = false
}

const handleDialogClosed = () => {
  selectedPOI.value = null
  mapStore.clearSelectedPOI()
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
  } catch {
    // 保持地图交互流畅，这里不打断用户操作。
  }
}

onMounted(async () => {
  try {
    await initMap()
    await loadPOIs()
    drawRoute()
  } catch (error) {
    sdkError.value = error.message || '高德地图初始化失败'
    ElMessage.error(sdkError.value)
  }
})

onUnmounted(() => {
  if (map) {
    map.off('moveend', handleMapMove)
    map.off('zoomend', syncMapCenterToStore)
    map.off('click', handleMapClick)
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
  () => mapStore.activeRoutePoints,
  () => {
    drawRouteEndpoints()
  },
  { deep: true }
)

watch(
  () => mapStore.selectedPOI,
  (poi) => {
    if (poi) {
      selectedPOI.value = poi
      showDetailDialog.value = true
      focusSelectedPoi(poi)
    } else {
      selectedPOI.value = null
    }
  },
  { deep: true }
)

watch(
  () => mapStore.routePickMode,
  () => {
    updateMapCursor()
  }
)
</script>

<style scoped>
.map-container {
  position: relative;
  width: 100%;
  min-height: calc(100vh - 96px);
  overflow: hidden;
  background:
    radial-gradient(circle at top right, rgba(23, 135, 166, 0.12), transparent 28%),
    linear-gradient(180deg, #f9fcfd, #e7f0f3);
}

.map-view {
  width: 100%;
  height: calc(100vh - 96px);
  background:
    radial-gradient(circle at top right, rgba(14, 165, 233, 0.15), transparent 28%),
    linear-gradient(180deg, #f8fafc, #e2e8f0);
}

.map-pick-tip {
  position: absolute;
  top: 20px;
  left: 20px;
  z-index: 1100;
  padding: 11px 14px;
  border-radius: 14px;
  background: rgba(23, 50, 60, 0.88);
  color: #eff8fa;
  font-size: 13px;
  box-shadow: 0 18px 36px rgba(15, 23, 42, 0.18);
}

.map-error {
  position: absolute;
  left: 20px;
  bottom: 20px;
  z-index: 1200;
  max-width: 360px;
  padding: 16px 18px;
  border-radius: 18px;
  background: rgba(23, 50, 60, 0.92);
  color: #eff8fa;
  box-shadow: 0 20px 40px rgba(15, 23, 42, 0.18);
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

.poi-dialog-content {
  display: flex;
  flex-direction: column;
  gap: 22px;
}

.poi-overview {
  padding: 24px;
  border-radius: 28px;
  background: linear-gradient(135deg, rgba(216, 238, 244, 0.92), rgba(255, 255, 255, 0.96));
}

.poi-overview-card h3 {
  margin: 14px 0 8px;
  font-size: 24px;
  color: var(--front-text);
}

.poi-overview-card p {
  margin: 0;
  color: var(--front-text-soft);
  line-height: 1.7;
}

.poi-category {
  display: inline-flex;
  padding: 6px 12px;
  border-radius: 999px;
  background: var(--front-accent-soft);
  color: var(--front-accent-strong);
  font-size: 12px;
  font-weight: 700;
}

.poi-meta-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
  margin-top: 18px;
}

.meta-card {
  padding: 16px 18px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.84);
  border: 1px solid var(--front-border);
}

.meta-card span {
  display: block;
  color: var(--front-text-muted);
  font-size: 12px;
}

.meta-card strong {
  display: block;
  margin-top: 8px;
  color: var(--front-text);
  font-size: 16px;
}

.poi-actions {
  margin-top: 18px;
  display: flex;
  gap: 12px;
}

.dialog-actions {
  display: flex;
  justify-content: flex-end;
}

@media (max-width: 768px) {
  .map-container {
    min-height: calc(100vh - 88px);
  }

  .map-view {
    height: calc(100vh - 88px);
  }

  .map-pick-tip {
    left: 12px;
    right: 12px;
    top: 12px;
  }

  .map-error {
    left: 12px;
    right: 12px;
    bottom: 84px;
    max-width: none;
  }

  .poi-meta-grid {
    grid-template-columns: 1fr;
  }

  .poi-actions {
    flex-direction: column;
  }
}
</style>
