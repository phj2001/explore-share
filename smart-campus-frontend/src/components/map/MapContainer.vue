<template>
  <section class="map-shell" :class="{ 'mobile-route-panel-open': isMobileViewport && isMobileRoutePanelExpanded }">
    <div class="map-container">
      <div ref="mapRoot" class="map-view"></div>

      <RoutePolyline @visibility-change="handleRoutePanelVisibilityChange" />

      <div v-if="mapStore.isPickingRoutePoint" class="map-pick-tip">
        请在地图中点击，设置{{ mapStore.routePickMode === 'start' ? '起点' : '终点' }}。
      </div>

      <div v-if="sdkError" class="map-error">
        <h3>地图加载失败</h3>
        <p>{{ sdkError }}</p>
        <p>请检查 `VITE_AMAP_JS_KEY` 与高德地图白名单配置。</p>
      </div>

      <div v-if="!sdkError" class="map-toolbar">
        <el-button plain class="map-toolbar-button" @click="clearAllPoiLabels">
          清除名称
        </el-button>
        <el-button
          v-if="userStore.isLoggedIn"
          plain
          class="map-toolbar-button"
          @click="router.push({ name: 'RouteCreate' })"
        >
          创建路线
        </el-button>
        <el-button
          v-if="userStore.isLoggedIn"
          type="primary"
          plain
          class="map-toolbar-button"
          @click="showPOIApplyDialog = true"
        >
          申请添加地点
        </el-button>
      </div>

      <!-- 性能截断提示已下线（用户反馈：不需要该提示）
      <div v-if="!sdkError && activeResultSummary.truncated" class="map-limit-tip">
        当前视野内共有 {{ activeResultSummary.total }} 个地点，为保证性能，当前仅展示前
        {{ activeResultSummary.limit }} 个。请继续放大地图查看更多。
      </div>
      -->
    </div>

    <PoiDetailDialog
      v-model="showDetailDialog"
      :poi="selectedPOI"
      :mobile="isMobileViewport"
      @closed="handlePoiDialogClosed"
    />

    <POIApplicationDialog
      v-if="showPOIApplyDialog"
      @close="showPOIApplyDialog = false"
    />
  </section>
</template>

<script setup>
import { computed, defineAsyncComponent, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import RoutePolyline from '@/components/map/RoutePolyline.vue'
import { useUserStore } from '@/stores/user'
import { usePOIStore } from '@/stores/poi'
import { useMapStore } from '@/stores/map'
import {
  fromAmapCoordinate,
  loadAmapSdk,
  normalizePoiForAmap,
  toAmapCoordinate
} from '@/utils/amap'
import { resolvePoiSymbol, renderPoiIconSvg } from '@/utils/poiSymbol'
import { useViewport } from '@/composables/useViewport'

const PoiDetailDialog = defineAsyncComponent(() => import('@/components/map/PoiDetailDialog.vue'))
const POIApplicationDialog = defineAsyncComponent(() => import('@/components/map/POIApplicationDialog.vue'))

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const poiStore = usePOIStore()
const mapStore = useMapStore()

const mapRoot = ref(null)
const showDetailDialog = ref(false)
const showPOIApplyDialog = ref(false)
const selectedPOI = ref(null)
const sdkError = ref('')
const { isMobile } = useViewport()
const isMobileViewport = computed(() => isMobile.value)
const isMobileRoutePanelExpanded = ref(false)
const hasShownBoundsLimitMessage = ref(false)
const pendingPoiResultViewportAdjustment = ref(false)
const renderedPoiList = computed(() => poiStore.visiblePoiList || [])
const activeResultSummary = computed(() =>
  poiStore.activeSource === 'search' ? poiStore.searchSummary : poiStore.boundsSummary
)

let AMapRef = null
let map = null
let poiMarkers = []
// 自研网格聚合的气泡 marker 池：key 为像素网格坐标 `gx:gy`，按桶复用实例避免平移时闪烁
let poiClusterMarkerMap = new Map()
let poiMarkerMap = new Map()
let poiLabelMarkers = new Map()
let routePolyline = null
let routeEndpointMarkers = []
let boundsRequestId = 0
let boundsFetchTimer = null
let lastReusableBounds = null
let lastRenderedPoiSignature = ''
let hasAppliedEmptyStateFallback = false
let poiResultViewportFrame = null

const BOUNDS_FETCH_DEBOUNCE_MS = 300
const POI_LABEL_HOVER_DELAY_MS = 500
// 视野拉取上限：配合点聚合整体下调，缓解手机端 POI 密密麻麻的问题。
// 最低档 100 与后端 normalizeMapPointLimit 下限对齐（前端传更低也会被钳到 100），无需动后端。
const MAP_BOUNDS_LIMIT = 100
const MAP_BOUNDS_LIMIT_MAX = 900
const BOUNDS_REUSE_EPSILON = 0.0001
const EMPTY_STATE_FALLBACK_CENTER = { lat: 35.8617, lng: 104.1954 }
const EMPTY_STATE_FALLBACK_ZOOM = 5

// 切回桌面时收起移动端路线面板
watch(isMobile, (mobile) => {
  if (!mobile) {
    isMobileRoutePanelExpanded.value = false
  }
})

const handleRoutePanelVisibilityChange = (visible) => {
  isMobileRoutePanelExpanded.value = !!visible
}

const getFitViewPadding = () => {
  if (isMobile.value) {
    return [64, 44, 64, 44]
  }

  return [80, 420, 80, 80]
}

const getPoiResultFitViewPadding = () => {
  if (isMobile.value) {
    return [92, 44, 72, 44]
  }

  return [140, 120, 100, 120]
}

const getBoundsLimitByZoom = () => {
  if (!map) {
    return MAP_BOUNDS_LIMIT
  }

  const zoom = map.getZoom()
  if (zoom <= 5) return 100
  if (zoom <= 7) return 150
  if (zoom <= 9) return 220
  if (zoom <= 11) return 320
  if (zoom <= 13) return 480
  if (zoom <= 15) return 700
  return MAP_BOUNDS_LIMIT_MAX
}

const getClusterGridSizeByZoom = () => {
  if (!map) {
    return 80
  }

  const zoom = map.getZoom()
  if (zoom <= 5) return 104
  if (zoom <= 7) return 96
  if (zoom <= 9) return 88
  if (zoom <= 11) return 80
  if (zoom <= 13) return 72
  if (zoom <= 15) return 64
  return 56
}

// ── POI 点聚合（AMap.MarkerCluster）──
// 超过该缩放级别不再聚合，所有点位直接独立展示——保证"点击看详情/悬停出名称"
// 在街景级缩放下始终作用于单个 marker（focusSelectedPoi 会 zoom 到 17）
const POI_CLUSTER_MAX_ZOOM = 15
// 桶内点位数达到该阈值才收拢为数量气泡，单个点保持原有单点 marker 交互
const POI_CLUSTER_MIN_BUCKET_SIZE = 2

const createClusterMarkerContent = (count) => {
  // 数量分级控制气泡尺寸，避免三位数撑爆小气泡
  const size = count >= 100 ? 56 : count >= 10 ? 48 : 40
  return `
    <div class="poi-cluster-bubble" style="width:${size}px;height:${size}px;">
      <span class="poi-cluster-bubble__count">${count}</span>
    </div>
  `
}

const createPoiMarkerContent = (poi) => {
  const symbol = resolvePoiSymbol(poi?.category)
  const icon = renderPoiIconSvg(symbol, { size: 11, color: '#ffffff' })
  const poiId = poi?.id != null ? String(poi.id) : ''
  // 尺寸/描边/阴影等静态样式交给 :deep(.poi-marker)；inline 只保留分类色（背景+选中态发光环色）
  return `
    <div class="poi-marker" data-poi-id="${poiId}" data-cat-key="${symbol.key}" style="background:${symbol.hex}; --poi-ring:${symbol.hex};">
      <span class="poi-marker__icon">${icon}</span>
    </div>
  `
}

const createPoiLabelContent = (name) => {
  return `
    <div style="
      padding:4px 10px;
      border-radius:999px;
      background:rgba(15,23,42,0.88);
      color:#ffffff;
      font-size:12px;
      font-weight:600;
      white-space:nowrap;
      box-shadow:0 8px 18px rgba(15,23,42,0.22);
      border:none;
      outline:none;
      pointer-events:none;
      user-select:none;
      -webkit-user-select:none;
      caret-color:transparent;
    ">${name}</div>
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

// 切换 POI marker 的选中态 class（放大 + 分类色发光环）
// poiId 为 null 时清除所有选中态（关闭弹层时调用）
const applyPoiMarkerActiveState = (poiId) => {
  const root = mapRoot.value
  if (!root) return
  root.querySelectorAll('.poi-marker.is-active').forEach((el) => el.classList.remove('is-active'))
  if (poiId != null) {
    const target = root.querySelector(`.poi-marker[data-poi-id="${poiId}"]`)
    if (target) target.classList.add('is-active')
  }
}

const clearPoiMarkers = () => {
  clearPoiClusterBubbles()

  const allMarkers = [...new Set([...poiMarkers, ...poiMarkerMap.values()])]
  if (map && allMarkers.length) {
    map.remove(allMarkers)
  }
  poiMarkers = []
  poiMarkerMap = new Map()
  clearAllPoiLabels()
  lastRenderedPoiSignature = ''
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

const getPoiRenderSignature = () => {
  // 缩放级别（取整）纳入签名：纯缩放不改变点位列表，但聚合桶按屏幕像素划分，
  // 必须触发一次重聚合；平移同样改变像素坐标，由 moveend 路径调用 renderMarkers 覆盖
  const zoomKey = map ? Math.round(map.getZoom()) : 'init'
  return `${zoomKey}|${renderedPoiList.value
    .map((poi) => `${poi.id}:${poi.latitude}:${poi.longitude}:${poi.name}`)
    .join('|')}`
}

const showPoiMarkerLabel = (marker, poi) => {
  const poiId = poi?.id ?? marker?.getExtData?.()?.poiId
  if (!map || !marker || !poiId || !poi?.name || poiLabelMarkers.has(poiId)) {
    return
  }

  const position = marker.getPosition?.()
  if (!position) {
    return
  }

  const labelMarker = new AMapRef.Marker({
    position,
    anchor: 'bottom-center',
    offset: new AMapRef.Pixel(0, -14),
    content: createPoiLabelContent(poi.name),
    clickable: false,
    bubble: false,
    topWhenClick: false,
    zIndex: 130
  })

  poiLabelMarkers.set(poiId, labelMarker)
  map.add(labelMarker)
}

const hidePoiMarkerLabel = (marker) => {
  const poiId = marker?.getExtData?.()?.poiId
  if (!poiId) {
    return
  }

  const labelMarker = poiLabelMarkers.get(poiId)
  if (labelMarker && map) {
    map.remove(labelMarker)
  }
  poiLabelMarkers.delete(poiId)
}

const removeAllRenderedPoiLabelDom = () => {
  document
    ?.querySelectorAll?.('.amap-marker-label')
    ?.forEach((labelNode) => labelNode.remove())
}

const clearAllPoiLabels = () => {
  if (map && poiLabelMarkers.size) {
    map.remove([...poiLabelMarkers.values()])
  }
  poiLabelMarkers.clear()
  removeAllRenderedPoiLabelDom()
}

const bindPoiMarkerInteractions = (marker, poi) => {
  if (!marker || marker.__poiInteractionsBound) {
    return
  }

  marker.on('mouseover', () => {
    const currentPoi = marker.getExtData?.()?.poi || poi
    if (marker.__poiHoverTimer) {
      window.clearTimeout(marker.__poiHoverTimer)
    }

    marker.__poiHoverTimer = window.setTimeout(() => {
      marker.__poiHoverTimer = null
      showPoiMarkerLabel(marker, currentPoi)
    }, POI_LABEL_HOVER_DELAY_MS)
  })

  marker.on('mouseout', () => {
    if (marker.__poiHoverTimer) {
      window.clearTimeout(marker.__poiHoverTimer)
      marker.__poiHoverTimer = null
    }
  })

  marker.on('touchend', () => {
    if (marker.__poiHoverTimer) {
      window.clearTimeout(marker.__poiHoverTimer)
      marker.__poiHoverTimer = null
    }
  })

  marker.on('click', () => {
    const currentPoi = marker.getExtData?.()?.poi || poi
    if (marker.__poiHoverTimer) {
      window.clearTimeout(marker.__poiHoverTimer)
      marker.__poiHoverTimer = null
    }
    clearMarkerFocusArtifacts()
    selectedPOI.value = currentPoi
    showDetailDialog.value = true
    mapStore.selectPOI(currentPoi)
    focusSelectedPoi(currentPoi)
  })

  marker.__poiInteractionsBound = true
}

const applyPoiMarkerPresentation = (marker, poi, position) => {
  marker.setPosition(position)
  marker.setTitle?.('')
  marker.setContent?.(createPoiMarkerContent(poi))
  marker.setOffset?.(new AMapRef.Pixel(-11, -11))
  marker.setTopWhenClick?.(true)
  marker.setExtData?.({
    poiId: poi.id,
    poi
  })
}

const createPoiMarker = (poi, position) => {
  const marker = new AMapRef.Marker({
    position,
    title: '',
    anchor: 'center',
    content: createPoiMarkerContent(poi),
    offset: new AMapRef.Pixel(-11, -11),
    topWhenClick: true,
  })
  applyPoiMarkerPresentation(marker, poi, position)
  bindPoiMarkerInteractions(marker, poi)

  return marker
}

const syncPoiMarkerInstances = () => {
  const nextMarkers = []
  const activePoiIds = new Set()

  for (const poi of renderedPoiList.value) {
    const position = getPoiMapPosition(poi)
    if (!position) {
      continue
    }

    activePoiIds.add(poi.id)

    let marker = poiMarkerMap.get(poi.id)
    if (!marker) {
      marker = createPoiMarker(poi, position)
      poiMarkerMap.set(poi.id, marker)
    } else {
      applyPoiMarkerPresentation(marker, poi, position)
    }

    nextMarkers.push(marker)
  }

  const staleMarkers = []
  for (const [poiId, marker] of poiMarkerMap.entries()) {
    if (activePoiIds.has(poiId)) {
      const labelMarker = poiLabelMarkers.get(poiId)
      if (labelMarker) {
        labelMarker.setPosition?.(marker.getPosition?.())
      }
      continue
    }
    staleMarkers.push(marker)
    if (marker.__poiHoverTimer) {
      window.clearTimeout(marker.__poiHoverTimer)
      marker.__poiHoverTimer = null
    }
    hidePoiMarkerLabel(marker)
    poiMarkerMap.delete(poiId)
  }

  return {
    nextMarkers,
    staleMarkers
  }
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

  applyPoiMarkerActiveState(poi?.id)
  map.setZoomAndCenter(Math.max(map.getZoom(), 17), position)
}

const applyPoiResultViewportAdjustment = () => {
  if (!map || !pendingPoiResultViewportAdjustment.value) {
    return
  }

  pendingPoiResultViewportAdjustment.value = false

  if (!renderedPoiList.value.length) {
    return
  }

  renderMarkers()

  const markerEntries = renderedPoiList.value
    .map((poi) => ({
      poi,
      marker: poiMarkerMap.get(poi.id)
    }))
    .filter((item) => item.marker)

  if (!markerEntries.length) {
    return
  }

  if (markerEntries.length === 1) {
    focusSelectedPoi(markerEntries[0].poi)
    return
  }

  // 聚合模式下部分结果 marker 未上图（被收拢进气泡），setFitView 只传已上图实例；
  // 全部入桶时当前视野本身就是气泡概览，无需再调整
  const onMapMarkers = markerEntries
    .map((item) => item.marker)
    .filter((marker) => marker.getMap?.())

  if (!onMapMarkers.length) {
    return
  }

  map.setFitView(onMapMarkers, false, getPoiResultFitViewPadding())
}

const schedulePoiResultViewportAdjustment = () => {
  if (!pendingPoiResultViewportAdjustment.value || !map) {
    return
  }

  if (poiResultViewportFrame) {
    window.cancelAnimationFrame(poiResultViewportFrame)
  }

  poiResultViewportFrame = window.requestAnimationFrame(() => {
    poiResultViewportFrame = null
    applyPoiResultViewportAdjustment()
  })
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
  ElMessage.success(`已设置 ${point.name}`)
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

  map.on('moveend', handleMapBoundsChange)
  map.on('zoomend', handleMapBoundsChange)
  map.on('click', handleMapClick)
  updateMapCursor()
}

const getCurrentBoundsPayload = () => {
  if (!map) return
  const bounds = map.getBounds()
  if (!bounds) return

  const southWest = bounds.getSouthWest()
  const northEast = bounds.getNorthEast()
  const convertedSouthWest = fromAmapCoordinate(southWest.getLat(), southWest.getLng())
  const convertedNorthEast = fromAmapCoordinate(northEast.getLat(), northEast.getLng())

  if (!convertedSouthWest || !convertedNorthEast) {
    return
  }

   return {
    minLat: convertedSouthWest.lat,
    maxLat: convertedNorthEast.lat,
    minLng: convertedSouthWest.lng,
    maxLng: convertedNorthEast.lng
  }
}

const canReuseBoundsData = (boundsPayload) => {
  if (!boundsPayload || !lastReusableBounds) {
    return false
  }

  return (
    boundsPayload.minLat >= lastReusableBounds.minLat - BOUNDS_REUSE_EPSILON &&
    boundsPayload.maxLat <= lastReusableBounds.maxLat + BOUNDS_REUSE_EPSILON &&
    boundsPayload.minLng >= lastReusableBounds.minLng - BOUNDS_REUSE_EPSILON &&
    boundsPayload.maxLng <= lastReusableBounds.maxLng + BOUNDS_REUSE_EPSILON
  )
}

const fetchPoisInCurrentBounds = async ({ silent = false, force = false } = {}) => {
  const boundsPayload = getCurrentBoundsPayload()
  if (!boundsPayload) return

  if (!force && canReuseBoundsData(boundsPayload)) {
      return {
        reused: true,
        records: poiStore.mapPoiList,
        ...poiStore.boundsSummary
      }
  }

  const requestId = ++boundsRequestId
  const boundsLimit = getBoundsLimitByZoom()

  try {
    const data = await poiStore.fetchInBounds(
      boundsPayload.minLat,
      boundsPayload.maxLat,
      boundsPayload.minLng,
      boundsPayload.maxLng,
      boundsLimit
    )

    if (requestId !== boundsRequestId) {
      return
    }

    if (data?.truncated && !silent && !hasShownBoundsLimitMessage.value) {
      hasShownBoundsLimitMessage.value = true
      ElMessage.warning(`当前视野内共有 ${data.total} 个地点，已限制显示前 ${data.limit} 个，请继续放大地图查看更多。`)
    }

    if (!data?.truncated) {
      hasShownBoundsLimitMessage.value = false
      lastReusableBounds = boundsPayload
    } else {
      lastReusableBounds = null
    }

    return data
  } catch (error) {
    if (!silent) {
      ElMessage.error(error?.message || '加载当前视野地点失败')
    }
    throw error
  }
}

const triggerBoundsFetch = async ({ silent = true } = {}) => {
  syncMapCenterToStore()

  try {
    const data = await fetchPoisInCurrentBounds({ silent })

    if (
      !hasAppliedEmptyStateFallback &&
      !activeResultSummary.value.truncated &&
      !(data?.records?.length > 0)
    ) {
      const currentZoom = map?.getZoom?.() ?? mapStore.zoom
      if (currentZoom > EMPTY_STATE_FALLBACK_ZOOM) {
        hasAppliedEmptyStateFallback = true
        mapStore.flyTo(EMPTY_STATE_FALLBACK_CENTER.lat, EMPTY_STATE_FALLBACK_CENTER.lng, EMPTY_STATE_FALLBACK_ZOOM)
        map?.setZoomAndCenter(
          EMPTY_STATE_FALLBACK_ZOOM,
          [EMPTY_STATE_FALLBACK_CENTER.lng, EMPTY_STATE_FALLBACK_CENTER.lat]
        )
        scheduleBoundsFetch({ silent: true })
      }
    }
  } catch {
    // 下层已经处理可见错误提示，这里静默即可
  }
}

const scheduleBoundsFetch = ({ silent = true } = {}) => {
  if (boundsFetchTimer) {
    window.clearTimeout(boundsFetchTimer)
  }

  boundsFetchTimer = window.setTimeout(() => {
    boundsFetchTimer = null
    void triggerBoundsFetch({ silent })
  }, BOUNDS_FETCH_DEBOUNCE_MS)
}

const loadInitialMapData = async () => {
  try {
    lastReusableBounds = null
    hasAppliedEmptyStateFallback = false
    await poiStore.fetchCategories()

    const data = await fetchPoisInCurrentBounds({ force: true })
    if (
      !hasAppliedEmptyStateFallback &&
      !activeResultSummary.value.truncated &&
      !(data?.records?.length > 0)
    ) {
      hasAppliedEmptyStateFallback = true
      mapStore.flyTo(EMPTY_STATE_FALLBACK_CENTER.lat, EMPTY_STATE_FALLBACK_CENTER.lng, EMPTY_STATE_FALLBACK_ZOOM)
      map?.setZoomAndCenter(
        EMPTY_STATE_FALLBACK_ZOOM,
        [EMPTY_STATE_FALLBACK_CENTER.lng, EMPTY_STATE_FALLBACK_CENTER.lat]
      )
      await fetchPoisInCurrentBounds({ force: true })
    }
  } catch {
    // 下层已经处理错误提示
  }
}

// —— 自研网格点聚合 ——
// 背景：本环境高德 JS API 2.0 的 MarkerCluster 插件只认 setData 数据对象模式，
// 无法托管已有 Marker 实例（setMarkers / 构造函数传 Marker 数组均不生效），迁就它
// 会丢掉 hover 标签、点击详情、active 态等现成交互，因此按屏幕像素网格自行分桶：
// 桶内点位数达标时只渲染一个数量气泡，桶内单点 marker 保留实例但不上图；
// 达标阈值以下的桶照旧平铺单点，全部单点交互完整保留
const isPoiClusteringActive = () => {
  const zoom = map?.getZoom?.()
  return zoom != null && zoom < POI_CLUSTER_MAX_ZOOM
}

const computePoiClusters = () => {
  if (!isPoiClusteringActive()) {
    return null
  }

  const gridSize = getClusterGridSizeByZoom()
  const buckets = new Map()

  renderedPoiList.value.forEach((poi) => {
    const position = getPoiMapPosition(poi)
    if (!position) return

    const point = map.lngLatToContainer(new AMapRef.LngLat(position[0], position[1]))
    const key = `${Math.floor(point.getX() / gridSize)}:${Math.floor(point.getY() / gridSize)}`
    const bucket = buckets.get(key)
    if (bucket) {
      bucket.count += 1
      bucket.lng += position[0]
      bucket.lat += position[1]
      bucket.poiIds.add(poi.id)
    } else {
      buckets.set(key, {
        key,
        count: 1,
        lng: position[0],
        lat: position[1],
        poiIds: new Set([poi.id])
      })
    }
  })

  const clusters = []
  const clusteredPoiIds = new Set()
  buckets.forEach((bucket) => {
    if (bucket.count < POI_CLUSTER_MIN_BUCKET_SIZE) {
      return
    }
    // 桶中心取成员经纬度均值（averageCenter 语义）
    bucket.lng /= bucket.count
    bucket.lat /= bucket.count
    clusters.push(bucket)
    bucket.poiIds.forEach((id) => clusteredPoiIds.add(id))
  })

  return { clusters, clusteredPoiIds }
}

const handleClusterBubbleClick = (bucket) => {
  if (!map || !bucket) return
  map.setZoomAndCenter(Math.min(map.getZoom() + 2, 20), [bucket.lng, bucket.lat])
}

const clearPoiClusterBubbles = () => {
  if (map && poiClusterMarkerMap.size) {
    map.remove([...poiClusterMarkerMap.values()])
  }
  poiClusterMarkerMap = new Map()
}

// 按桶 key 复用气泡 marker：平移/缩放时命中同 key 的桶只更新位置与数量，避免整批重建闪烁
const syncPoiClusterBubbles = (clusters) => {
  if (!map) return

  const nextKeys = new Set(clusters.map((bucket) => bucket.key))
  const staleKeys = [...poiClusterMarkerMap.keys()].filter((key) => !nextKeys.has(key))
  if (staleKeys.length) {
    map.remove(staleKeys.map((key) => poiClusterMarkerMap.get(key)))
    staleKeys.forEach((key) => poiClusterMarkerMap.delete(key))
  }

  clusters.forEach((bucket) => {
    const position = [bucket.lng, bucket.lat]
    const existing = poiClusterMarkerMap.get(bucket.key)

    if (existing) {
      existing.setPosition(position)
      existing.setContent(createClusterMarkerContent(bucket.count))
      existing.__poiClusterBucket = bucket
      return
    }

    const bubble = new AMapRef.Marker({
      position,
      anchor: 'center',
      content: createClusterMarkerContent(bucket.count),
      zIndex: 120,
      bubble: false,
      cursor: 'pointer'
    })
    bubble.__poiClusterBucket = bucket
    // 点击气泡逐级下钻：以桶中心放大两级，散开为更小的聚合或单点
    bubble.on('click', () => handleClusterBubbleClick(bubble.__poiClusterBucket))
    poiClusterMarkerMap.set(bucket.key, bubble)
    map.add(bubble)
  })
}

const renderMarkers = () => {
  if (!map || !AMapRef) return

  const nextSignature = getPoiRenderSignature()
  if (nextSignature === lastRenderedPoiSignature) {
    return
  }

  lastRenderedPoiSignature = nextSignature

  if (!renderedPoiList.value.length) {
    clearPoiClusterBubbles()

    if (poiMarkers.length) {
      poiMarkers.forEach((marker) => {
        if (marker.__poiHoverTimer) {
          window.clearTimeout(marker.__poiHoverTimer)
          marker.__poiHoverTimer = null
        }
      })
      map.remove(poiMarkers)
    }

    poiMarkers = []
    poiMarkerMap = new Map()
    return
  }

  const { nextMarkers, staleMarkers } = syncPoiMarkerInstances()

  if (staleMarkers.length) {
    staleMarkers.forEach((marker) => hidePoiMarkerLabel(marker))
    map.remove(staleMarkers)
  }

  poiMarkers = nextMarkers

  // 低缩放级别先分桶：入桶点位的单点 marker 不上图（已上图的摘下并收起 hover 标签），
  // 其余点位照旧平铺
  const clusterResult = computePoiClusters()
  const clusteredPoiIds = clusterResult?.clusteredPoiIds

  const markersToAdd = []
  const markersToRemove = []
  poiMarkers.forEach((marker) => {
    const poiId = marker.getExtData?.()?.poiId
    const shouldShow = clusteredPoiIds ? !clusteredPoiIds.has(poiId) : true

    if (!shouldShow && marker.getMap?.()) {
      hidePoiMarkerLabel(marker)
      markersToRemove.push(marker)
    } else if (shouldShow && !marker.getMap?.()) {
      markersToAdd.push(marker)
    }
  })

  if (markersToRemove.length) {
    map.remove(markersToRemove)
  }
  if (markersToAdd.length) {
    map.add(markersToAdd)
  }

  syncPoiClusterBubbles(clusterResult?.clusters ?? [])
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
        : ['#1f8c69', '#2d9e7a']

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
    strokeColor: '#1f8c69',
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

// POI 详情弹窗关闭后的地图侧清理（弹窗内部互动状态由 PoiDetailDialog 自行重置）
const handlePoiDialogClosed = () => {
  selectedPOI.value = null
  mapStore.clearSelectedPOI()
  applyPoiMarkerActiveState(null)
}

const handleMapBoundsChange = () => {
  if (!map) return

  // 平移/缩放后屏幕像素坐标变化会改变聚合桶划分，先按当前视野重聚合再走拉取流程
  renderMarkers()

  if (poiStore.activeSource === 'search') {
    syncMapCenterToStore()
    return
  }

  scheduleBoundsFetch({ silent: true })
}

const handleResetToBounds = () => {
  if (!map) return
  scheduleBoundsFetch({ silent: true })
}

const handleFitSearchResults = () => {
  pendingPoiResultViewportAdjustment.value = true
  schedulePoiResultViewportAdjustment()
}

onMounted(async () => {
  try {
    await initMap()
    window.addEventListener('poi:reset-to-bounds', handleResetToBounds)
    window.addEventListener('poi:fit-search-results', handleFitSearchResults)
    await loadInitialMapData()
    drawRoute()

    const targetPoiId = Number(route.query.poiId)
    if (targetPoiId) {
      try {
        const poi = await poiStore.fetchPOIById(targetPoiId)
        if (poi) {
          mapStore.selectPOI(poi)
        }
      } catch {
        // 静默，不影响主流程
      }
    }
  } catch (error) {
    sdkError.value = error.message || '高德地图初始化失败'
    ElMessage.error(sdkError.value)
  }
})

onUnmounted(() => {
  if (boundsFetchTimer) {
    window.clearTimeout(boundsFetchTimer)
    boundsFetchTimer = null
  }

  if (poiResultViewportFrame) {
    window.cancelAnimationFrame(poiResultViewportFrame)
    poiResultViewportFrame = null
  }

  if (map) {
    map.off('moveend', handleMapBoundsChange)
    map.off('zoomend', handleMapBoundsChange)
    map.off('click', handleMapClick)
    map.destroy()
    map = null
  }

  window.removeEventListener('poi:reset-to-bounds', handleResetToBounds)
  window.removeEventListener('poi:fit-search-results', handleFitSearchResults)

  poiMarkers = []
  poiClusterMarkerMap = new Map()
  poiMarkerMap = new Map()
  routePolyline = null
  routeEndpointMarkers = []
  lastRenderedPoiSignature = ''
  poiLabelMarkers = new Map()
})

watch(
  () => poiStore.visiblePoiList,
  () => {
    renderMarkers()
    schedulePoiResultViewportAdjustment()
  }
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

<style scoped lang="scss">
.map-container {
  position: relative;
  width: 100%;
  min-height: calc(100vh - 96px);
  overflow: hidden;
  overscroll-behavior: contain;
  background:
    radial-gradient(circle at top right, rgba(31, 140, 105, 0.1), transparent 28%),
    linear-gradient(180deg, #f7faf7, #e8f2ed);
}

.map-view {
  width: 100%;
  height: calc(100vh - 96px);
  touch-action: pan-x pan-y pinch-zoom;
  background:
    radial-gradient(circle at top right, rgba(31, 140, 105, 0.12), transparent 28%),
    linear-gradient(180deg, #f7faf7, #e8f2ed);
}

@supports (height: 100svh) {
  .map-container {
    min-height: calc(100svh - 96px);
  }

  .map-view {
    height: calc(100svh - 96px);
  }
}

:deep(.amap-marker-label) {
  padding: 0 !important;
  border: none !important;
  outline: none !important;
  background: transparent !important;
  box-shadow: none !important;
  pointer-events: none !important;
}

:deep(.amap-marker-label::before),
:deep(.amap-marker-label::after) {
  display: none !important;
}

.map-pick-tip {
  position: absolute;
  top: 20px;
  left: 20px;
  z-index: 1100;
  padding: 11px 14px;
  border-radius: 14px;
  background: rgba(13, 42, 35, 0.88);
  color: var(--forest-100);
  font-size: 13px;
  box-shadow: 0 18px 36px rgba(13, 42, 35, 0.18);
}

.map-toolbar {
  position: absolute;
  right: 20px;
  bottom: 20px;
  z-index: 1200;
  display: flex;
  align-items: center;
  gap: 10px;
}

.map-toolbar-button {
  border-radius: 999px;
  border-color: var(--front-border);
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(10px);
  box-shadow: 0 12px 28px rgba(31, 140, 105, 0.1);
}

.map-toolbar-button:hover {
  background: rgba(255, 255, 255, 0.98);
}

/* ── POI 分类符号 marker ── */
/* marker content 注入到 .map-view 子树内，用 :deep() 命中 */
:deep(.poi-marker) {
  width: 22px;
  height: 22px;
  border-radius: 999px;
  border: 2.5px solid #ffffff;
  box-shadow: 0 6px 14px rgba(15, 42, 35, 0.28);
  display: flex;
  align-items: center;
  justify-content: center;
  transition: transform 0.18s ease, box-shadow 0.18s ease;
  user-select: none;
  -webkit-user-select: none;
  caret-color: transparent;
  outline: none;
  cursor: pointer;
}

:deep(.poi-marker__icon) {
  display: flex;
  align-items: center;
  justify-content: center;
  line-height: 0;
  pointer-events: none;
}

/* 选中态：放大 + 白色硬环 + 分类色软环（--poi-ring 由 content inline 提供） */
:deep(.poi-marker.is-active) {
  transform: scale(1.32);
  box-shadow:
    0 0 0 3px #ffffff,
    0 0 0 6px color-mix(in srgb, var(--poi-ring, #1f8c69) 50%, transparent),
    0 10px 22px rgba(15, 42, 35, 0.4);
  z-index: 5;
}

/* ── POI 聚合气泡（MarkerCluster renderClusterMarker 注入的 content，同样在 .map-view 子树内）── */
:deep(.poi-cluster-bubble) {
  border-radius: 999px;
  background: linear-gradient(135deg, #1f8c69, #2d9e7a);
  border: 2px solid #ffffff;
  box-shadow: 0 6px 16px rgba(15, 42, 35, 0.35);
  display: flex;
  align-items: center;
  justify-content: center;
  user-select: none;
  -webkit-user-select: none;
  caret-color: transparent;
  outline: none;
  cursor: pointer;
}

:deep(.poi-cluster-bubble__count) {
  color: #ffffff;
  font-size: 13px;
  font-weight: 700;
  line-height: 1;
  font-family: 'Segoe UI', 'PingFang SC', 'Microsoft YaHei', sans-serif;
}

.map-error {
  position: absolute;
  left: 20px;
  bottom: 20px;
  z-index: 1200;
  max-width: 360px;
  padding: 16px 18px;
  border-radius: 18px;
  background: rgba(13, 42, 35, 0.92);
  color: var(--forest-100);
  box-shadow: 0 20px 40px rgba(13, 42, 35, 0.18);
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

/* 性能截断提示已下线（用户反馈：不需要该提示）
.map-limit-tip {
  position: absolute;
  right: 20px;
  bottom: 84px;
  z-index: 1200;
  max-width: 360px;
  padding: 14px 16px;
  border-radius: 16px;
  background: rgba(255, 248, 235, 0.94);
  color: #8a5a00;
  font-size: 13px;
  line-height: 1.6;
  border: 1px solid rgba(217, 119, 6, 0.18);
  box-shadow: 0 18px 36px rgba(15, 23, 42, 0.12);
}
*/

@include respond-to(md) {
  .map-shell.mobile-route-panel-open {
    padding-bottom: 60vh;
    padding-bottom: 60svh;
  }

  .map-container {
    min-height: 62vh;
    min-height: 62svh;
    overflow: visible;
  }

  .map-view {
    height: 62vh;
    height: 62svh;
  }

  .map-pick-tip {
    left: 12px;
    right: 12px;
    top: 12px;
    padding: 10px 12px;
    font-size: 12px;
  }

  .map-error {
    left: 12px;
    right: 12px;
    bottom: 78px;
    max-width: none;
  }

  /* 窄屏三按钮可能换行：左右锚定 + 允许 wrap，避免溢出 */
  .map-toolbar {
    left: 12px;
    right: 12px;
    bottom: max(12px, env(safe-area-inset-bottom));
    justify-content: flex-end;
    flex-wrap: wrap;
  }

  .map-toolbar-button {
    min-height: 36px;
    padding-inline: 12px;
  }

  /* 手机端 POI 点位再降一档（用户反馈仍偏大）：16px 与 8px 图标保持 0.5 比例，
     marker anchor 居中，对称缩放不产生点位偏移 */
  :deep(.poi-marker) {
    width: 16px;
    height: 16px;
    border-width: 1.5px;
    box-shadow: 0 3px 8px rgba(15, 42, 35, 0.28);
  }

  :deep(.poi-marker__icon svg) {
    width: 8px;
    height: 8px;
  }

  /* 性能截断提示已下线（用户反馈：不需要该提示）
  .map-limit-tip {
    left: 12px;
    right: 12px;
    bottom: calc(60px + max(12px, env(safe-area-inset-bottom)));
    max-width: none;
    padding: 12px 14px;
    font-size: 12px;
  }
  */
}

@include respond-to(xs) {
  .map-container {
    min-height: 58vh;
    min-height: 58svh;
  }

  .map-view {
    height: 58vh;
    height: 58svh;
  }
}
</style>

