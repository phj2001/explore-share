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

    <el-dialog
      v-model="showDetailDialog"
      :title="selectedPOI?.name"
      :width="isMobileViewport ? '100%' : '920px'"
      :fullscreen="isMobileViewport"
      :top="isMobileViewport ? '0' : '8vh'"
      destroy-on-close
      class="poi-dialog"
      @closed="handleDialogClosed"
    >
      <div v-if="selectedPOI" class="poi-dialog-content">
        <section class="poi-overview">
          <div class="poi-overview-card">
            <PoiCategoryBadge :category="selectedPOI.category" />
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

          <div class="poi-check-in-card">
            <div class="poi-check-in-meta">
              <span class="poi-check-in-label">打卡状态</span>
              <strong>{{ poiCheckInStatus.checkInCount }} 人打卡</strong>
              <p>
                {{ poiCheckInStatus.checkedIn ? '你已经打卡过这里了。' : '到过这里的话，可以点一下打卡。' }}
              </p>
            </div>

            <el-button
              :type="poiCheckInStatus.checkedIn ? 'success' : 'primary'"
              :plain="poiCheckInStatus.checkedIn"
              :loading="poiCheckInLoading"
              @click="togglePOICheckIn"
            >
              {{ poiCheckInStatus.checkedIn ? '取消打卡' : (userStore.isLoggedIn ? '打卡' : '登录后打卡') }}
            </el-button>
          </div>

          <div class="poi-check-in-card">
            <div class="poi-check-in-meta">
              <span class="poi-check-in-label">收藏状态</span>
              <strong>{{ poiFavoriteStatus.favoriteCount }} 人收藏</strong>
              <p>
                {{ poiFavoriteStatus.favorited ? '你已经收藏了这个地点。' : '感兴趣的话，可以收藏起来。' }}
              </p>
            </div>

            <el-button
              :type="poiFavoriteStatus.favorited ? 'danger' : 'primary'"
              :plain="poiFavoriteStatus.favorited"
              :loading="poiFavoriteLoading"
              @click="togglePOIFavorite"
            >
              {{ poiFavoriteStatus.favorited ? '取消收藏' : (userStore.isLoggedIn ? '收藏' : '登录后收藏') }}
            </el-button>
          </div>
        </section>

        <div class="poi-review-section">
          <div class="poi-review-header">
            <div>
              <h3>评分与评价</h3>
              <p v-if="poiRatingSummary.reviewCount > 0">
                平均 {{ poiRatingSummary.avgRating }} 分 · {{ poiRatingSummary.reviewCount }} 条评价
              </p>
              <p v-else>暂无评价，来做第一个评价的人吧</p>
            </div>
          </div>

          <div v-if="userStore.isLoggedIn" class="poi-review-composer">
            <div class="rating-stars">
              <span class="rating-label">我的评分：</span>
              <span
                v-for="star in 5"
                :key="star"
                class="star-btn"
                :class="{ active: star <= (poiHoverRating || poiUserRating) }"
                @mouseenter="poiHoverRating = star"
                @mouseleave="poiHoverRating = 0"
                @click="poiUserRating = star"
              >&#9733;</span>
              <span class="rating-text">{{ poiUserRating > 0 ? poiUserRating + ' 星' : '点击评分' }}</span>
            </div>
            <el-input
              v-model="poiReviewContent"
              type="textarea"
              :rows="2"
              maxlength="200"
              show-word-limit
              resize="none"
              placeholder="写下你的评价（可选）"
            />
            <div class="review-composer-actions">
              <span></span>
              <el-button
                type="primary"
                :disabled="poiUserRating === 0"
                :loading="poiReviewSubmitting"
                @click="submitReview"
              >
                提交评价
              </el-button>
            </div>
          </div>

          <div v-else class="poi-review-login-tip">
            <span>登录后即可评分和评价</span>
            <el-button type="primary" size="small" @click="router.push({ name: 'Login' })">去登录</el-button>
          </div>

          <div v-if="poiReviewList.length" class="poi-review-list">
            <div v-for="review in poiReviewList" :key="review.id" class="poi-review-item">
              <div class="review-item-head">
                <el-avatar :size="32" :src="review.authorAvatarUrl || undefined" class="review-avatar">
                  {{ (review.authorDisplayName || 'U').slice(0, 1).toUpperCase() }}
                </el-avatar>
                <div class="review-item-meta">
                  <div class="review-item-author">
                    <strong>{{ review.authorDisplayName }}</strong>
                    <span class="review-stars">
                      <span v-for="s in 5" :key="s" :class="{ filled: s <= review.rating }">&#9733;</span>
                    </span>
                  </div>
                  <time>{{ formatReviewTime(review.createdAt) }}</time>
                </div>
                <el-button
                  v-if="review.canDelete"
                  text
                  type="danger"
                  size="small"
                  @click="removeReview(review)"
                >
                  删除
                </el-button>
              </div>
              <p v-if="review.content" class="review-item-content">{{ review.content }}</p>
            </div>

            <div v-if="poiReviewHasMore" class="review-more">
              <el-button text :loading="poiReviewLoading" @click="loadMoreReviews">加载更多评价</el-button>
            </div>
          </div>
        </div>

        <PoiSharePanel v-if="showDetailDialog" :poi="selectedPOI" />

        <div v-if="showDetailDialog" class="poi-gallery-section">
          <div class="gallery-head">
            <h3>图片墙</h3>
            <span class="gallery-count">{{ galleryTotal }} 张图片</span>
          </div>
          <el-skeleton v-if="galleryLoading && !galleryImages.length" :rows="2" animated />
          <div v-else-if="galleryImages.length" class="gallery-grid">
            <el-image
              v-for="img in galleryImages"
              :key="img.imageId"
              :src="resolveMediaUrl(img.imageUrl)"
              :preview-src-list="galleryPreviewUrls"
              :initial-index="galleryImages.indexOf(img)"
              fit="cover"
              preview-teleported
              class="gallery-thumb"
            >
              <template #error>
                <div class="gallery-error">
                  <span>加载失败</span>
                </div>
              </template>
            </el-image>
          </div>
          <el-empty v-else description="暂无图片" :image-size="60" />
          <div v-if="galleryHasMore" class="gallery-more">
            <el-button size="small" :loading="galleryLoadingMore" @click="loadMoreGallery">加载更多</el-button>
          </div>
        </div>
      </div>

      <template #footer>
        <div class="dialog-actions">
          <el-button @click="showDetailDialog = false">关闭</el-button>
        </div>
      </template>
    </el-dialog>

    <POIApplicationDialog
      v-if="showPOIApplyDialog"
      @close="showPOIApplyDialog = false"
    />
  </section>
</template>

<script setup>
import { computed, defineAsyncComponent, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import RoutePolyline from '@/components/map/RoutePolyline.vue'
import { useUserStore } from '@/stores/user'
import { usePOIStore } from '@/stores/poi'
import { useMapStore } from '@/stores/map'
import { cancelCheckInPOI, checkInPOI, getPOICheckInStatus } from '@/api/poiCheckIn'
import { addFavorite, getFavoriteStatus, removeFavorite } from '@/api/poiFavorite'
import { createOrUpdateReview, deleteReview, getPoiReviews, getRatingSummary } from '@/api/poiReview'
import { getPoiGallery } from '@/api/poiGallery'
import {
  fromAmapCoordinate,
  loadAmapSdk,
  normalizePoiForAmap,
  toAmapCoordinate
} from '@/utils/amap'
import { API_ORIGIN } from '@/utils/request'
import { resolvePoiSymbol, renderPoiIconSvg } from '@/utils/poiSymbol'
import PoiCategoryBadge from '@/components/common/PoiCategoryBadge.vue'

const PoiSharePanel = defineAsyncComponent(() => import('@/components/map/PoiSharePanel.vue'))
const POIApplicationDialog = defineAsyncComponent(() => import('@/components/map/POIApplicationDialog.vue'))

const API_ORIGIN_RESOLVED = API_ORIGIN

const resolveMediaUrl = (value) => {
  if (!value) return ''
  if (/^https?:\/\//i.test(value)) return value
  return `${API_ORIGIN_RESOLVED}${value.startsWith('/') ? value : `/${value}`}`
}

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const poiStore = usePOIStore()
const mapStore = useMapStore()

const mapRoot = ref(null)
const showDetailDialog = ref(false)
const showPOIApplyDialog = ref(false)
const selectedPOI = ref(null)
const poiCheckInLoading = ref(false)
const poiCheckInStatus = ref({
  checkedIn: false,
  checkInCount: 0
})
const poiFavoriteLoading = ref(false)
const poiFavoriteStatus = ref({
  favorited: false,
  favoriteCount: 0
})
const poiRatingSummary = ref({ avgRating: 0, reviewCount: 0 })
const poiReviewList = ref([])
const poiReviewTotal = ref(0)
const poiReviewHasMore = ref(false)
const poiReviewPage = ref(0)
const poiReviewLoading = ref(false)
const poiReviewSubmitting = ref(false)
const poiUserRating = ref(0)
const poiReviewContent = ref('')
const poiHoverRating = ref(0)
const sdkError = ref('')
const isMobileViewport = ref(false)
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
let poiCluster = null
let poiMarkerMap = new Map()
let poiLabelMarkers = new Map()
let routePolyline = null
let routeEndpointMarkers = []
let boundsRequestId = 0
let boundsFetchTimer = null
let markerClusterPluginLoaded = false
let lastReusableBounds = null
let lastRenderedPoiSignature = ''
let hasAppliedEmptyStateFallback = false
let poiResultViewportFrame = null

const BOUNDS_FETCH_DEBOUNCE_MS = 300
const POI_LABEL_HOVER_DELAY_MS = 500
const MAP_BOUNDS_LIMIT = 1200
const MAP_BOUNDS_LIMIT_MAX = 1800
const BOUNDS_REUSE_EPSILON = 0.0001
const EMPTY_STATE_FALLBACK_CENTER = { lat: 35.8617, lng: 104.1954 }
const EMPTY_STATE_FALLBACK_ZOOM = 5

const updateViewportState = () => {
  isMobileViewport.value = window.innerWidth <= 768
  if (!isMobileViewport.value) {
    isMobileRoutePanelExpanded.value = false
  }
}

const handleRoutePanelVisibilityChange = (visible) => {
  isMobileRoutePanelExpanded.value = !!visible
}

const getFitViewPadding = () => {
  if (window.innerWidth <= 768) {
    return [64, 44, 64, 44]
  }

  return [80, 420, 80, 80]
}

const getPoiResultFitViewPadding = () => {
  if (window.innerWidth <= 768) {
    return [92, 44, 72, 44]
  }

  return [140, 120, 100, 120]
}

const getBoundsLimitByZoom = () => {
  if (!map) {
    return MAP_BOUNDS_LIMIT
  }

  const zoom = map.getZoom()
  if (zoom <= 5) return 180
  if (zoom <= 7) return 280
  if (zoom <= 9) return 420
  if (zoom <= 11) return 700
  if (zoom <= 13) return 1000
  if (zoom <= 15) return 1400
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
  if (poiCluster) {
    poiCluster.setMap?.(null)
    poiCluster = null
  }

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
  return renderedPoiList.value
    .map((poi) => `${poi.id}:${poi.latitude}:${poi.longitude}:${poi.name}`)
    .join('|')
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

  map.setFitView(
    markerEntries.map((item) => item.marker),
    false,
    getPoiResultFitViewPadding()
  )
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
  await ensureMarkerClusterPlugin()

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

const ensureMarkerClusterPlugin = async () => {
  if (!AMapRef || markerClusterPluginLoaded || typeof AMapRef.plugin !== 'function') {
    return
  }

  await new Promise((resolve) => {
    AMapRef.plugin(['AMap.MarkerCluster'], () => {
      if (AMapRef.MarkerCluster) {
        markerClusterPluginLoaded = true
      } else {
        console.warn('AMap.MarkerCluster 插件加载失败，已回退为普通点位渲染')
      }
      resolve()
    })
  })
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

const renderMarkers = () => {
  if (!map || !AMapRef) return

  const nextSignature = getPoiRenderSignature()
  if (nextSignature === lastRenderedPoiSignature) {
    return
  }

  if (poiCluster) {
    poiCluster.setMap?.(null)
    poiCluster = null
  }

  lastRenderedPoiSignature = nextSignature

  if (!renderedPoiList.value.length) {
  if (map && poiMarkers.length) {
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

  if (map) {
    const { nextMarkers, staleMarkers } = syncPoiMarkerInstances()

    if (staleMarkers.length) {
      staleMarkers.forEach((marker) => hidePoiMarkerLabel(marker))
      map.remove(staleMarkers)
    }

    poiMarkers = nextMarkers
    const markersToAdd = poiMarkers.filter((marker) => !marker.getMap?.())
    if (markersToAdd.length) {
      map.add(markersToAdd)
    }
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

const resetPOICheckInState = () => {
  poiCheckInLoading.value = false
  poiCheckInStatus.value = {
    checkedIn: false,
    checkInCount: 0
  }
}

const galleryImages = ref([])
const galleryLoading = ref(false)
const galleryLoadingMore = ref(false)
const galleryPage = ref(0)
const galleryHasMore = ref(false)
const galleryTotal = ref(0)

const galleryPreviewUrls = computed(() => galleryImages.value.map(img => resolveMediaUrl(img.imageUrl)))

const loadGallery = async (poiId, reset = false) => {
  if (!poiId) return
  const nextPage = reset ? 0 : galleryPage.value + 1
  const loadingRef = reset ? galleryLoading : galleryLoadingMore
  loadingRef.value = true
  try {
    const data = await getPoiGallery(poiId, { page: nextPage, size: 20 })
    const records = data?.records || []
    galleryImages.value = reset ? records : [...galleryImages.value, ...records]
    galleryPage.value = data?.page || nextPage
    galleryHasMore.value = Boolean(data?.hasNext)
    galleryTotal.value = data?.total || 0
  } catch {
    // 静默
  } finally {
    loadingRef.value = false
  }
}

const loadMoreGallery = () => {
  if (selectedPOI.value?.id) loadGallery(selectedPOI.value.id, false)
}

const loadPOICheckInStatus = async (poiId) => {
  if (!poiId) {
    resetPOICheckInState()
    return
  }

  try {
    poiCheckInStatus.value = await getPOICheckInStatus(poiId)
  } catch (error) {
    resetPOICheckInState()
    ElMessage.error(error.message || '打卡状态加载失败')
  }
}

const togglePOICheckIn = async () => {
  if (!selectedPOI.value?.id) {
    return
  }

  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录后再打卡')
    return
  }

  poiCheckInLoading.value = true

  try {
    poiCheckInStatus.value = poiCheckInStatus.value.checkedIn
      ? await cancelCheckInPOI(selectedPOI.value.id)
      : await checkInPOI(selectedPOI.value.id)

    ElMessage.success(poiCheckInStatus.value.checkedIn ? '打卡成功' : '已取消打卡')
  } catch (error) {
    ElMessage.error(error.message || '打卡操作失败')
  } finally {
    poiCheckInLoading.value = false
  }
}

const handleDialogClosed = () => {
  resetPOICheckInState()
  resetPOIFavoriteState()
  resetReviewState()
  selectedPOI.value = null
  mapStore.clearSelectedPOI()
  applyPoiMarkerActiveState(null)
}

const resetPOIFavoriteState = () => {
  poiFavoriteLoading.value = false
  poiFavoriteStatus.value = {
    favorited: false,
    favoriteCount: 0
  }
}

const REVIEW_PAGE_SIZE = 5

const resetReviewState = () => {
  poiRatingSummary.value = { avgRating: 0, reviewCount: 0 }
  poiReviewList.value = []
  poiReviewTotal.value = 0
  poiReviewHasMore.value = false
  poiReviewPage.value = 0
  poiReviewLoading.value = false
  poiReviewSubmitting.value = false
  poiUserRating.value = 0
  poiReviewContent.value = ''
  poiHoverRating.value = 0
}

const reviewTimeFormatter = new Intl.DateTimeFormat('zh-CN', {
  year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit'
})

const formatReviewTime = (value) => value ? reviewTimeFormatter.format(new Date(value)) : ''

const loadRatingSummary = async (poiId) => {
  if (!poiId) return
  try {
    poiRatingSummary.value = await getRatingSummary(poiId)
  } catch {
    // 静默
  }
}

const loadReviews = async (poiId, reset = false) => {
  if (!poiId) return
  const nextPage = reset ? 0 : poiReviewPage.value + 1
  poiReviewLoading.value = true
  try {
    const data = await getPoiReviews(poiId, { page: nextPage, size: REVIEW_PAGE_SIZE })
    const records = data?.records || []
    poiReviewList.value = reset ? records : [...poiReviewList.value, ...records]
    poiReviewTotal.value = data?.total || 0
    poiReviewPage.value = data?.page || nextPage
    poiReviewHasMore.value = Boolean(data?.hasNext)
  } catch {
    // 静默
  } finally {
    poiReviewLoading.value = false
  }
}

const loadMoreReviews = async () => {
  if (!selectedPOI.value?.id) return
  await loadReviews(selectedPOI.value.id, false)
}

const submitReview = async () => {
  if (!selectedPOI.value?.id || poiUserRating.value === 0) return

  poiReviewSubmitting.value = true
  try {
    await createOrUpdateReview(selectedPOI.value.id, {
      rating: poiUserRating.value,
      content: poiReviewContent.value.trim() || undefined
    })
    ElMessage.success('评价提交成功')
    poiReviewContent.value = ''
    await loadRatingSummary(selectedPOI.value.id)
    await loadReviews(selectedPOI.value.id, true)
  } catch (error) {
    ElMessage.error(error.message || '评价提交失败')
  } finally {
    poiReviewSubmitting.value = false
  }
}

const removeReview = async (review) => {
  try {
    await ElMessageBox.confirm('删除后无法恢复，是否继续？', '删除评价', {
      type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消'
    })
  } catch {
    return
  }

  try {
    await deleteReview(review.id)
    ElMessage.success('评价已删除')
    await loadRatingSummary(selectedPOI.value.id)
    await loadReviews(selectedPOI.value.id, true)
  } catch (error) {
    ElMessage.error(error.message || '删除评价失败')
  }
}

const loadPOIFavoriteStatus = async (poiId) => {
  if (!poiId) {
    resetPOIFavoriteState()
    return
  }

  try {
    poiFavoriteStatus.value = await getFavoriteStatus(poiId)
  } catch {
    resetPOIFavoriteState()
  }
}

const togglePOIFavorite = async () => {
  if (!selectedPOI.value?.id) {
    return
  }

  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录后再收藏')
    return
  }

  poiFavoriteLoading.value = true
  try {
    poiFavoriteStatus.value = poiFavoriteStatus.value.favorited
      ? await removeFavorite(selectedPOI.value.id)
      : await addFavorite(selectedPOI.value.id)

    ElMessage.success(poiFavoriteStatus.value.favorited ? '收藏成功' : '已取消收藏')
  } catch (error) {
    ElMessage.error(error.message || '收藏操作失败')
  } finally {
    poiFavoriteLoading.value = false
  }
}

const handleMapBoundsChange = () => {
  if (!map) return

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
    updateViewportState()
    window.addEventListener('resize', updateViewportState)
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
  window.removeEventListener('resize', updateViewportState)

  poiMarkers = []
  poiCluster = null
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
  () => selectedPOI.value?.id,
  (poiId) => {
    if (showDetailDialog.value && poiId) {
      loadPOICheckInStatus(poiId)
      loadPOIFavoriteStatus(poiId)
      loadRatingSummary(poiId)
      loadReviews(poiId, true)
      loadGallery(poiId, true)
      return
    }

    if (!poiId) {
      resetPOICheckInState()
      resetPOIFavoriteState()
      resetReviewState()
    }
  },
  { immediate: true }
)

watch(
  () => showDetailDialog.value,
  (visible) => {
    if (visible && selectedPOI.value?.id) {
      loadPOICheckInStatus(selectedPOI.value.id)
      loadPOIFavoriteStatus(selectedPOI.value.id)
      loadRatingSummary(selectedPOI.value.id)
      loadReviews(selectedPOI.value.id, true)
    }
  }
)

watch(
  () => userStore.isLoggedIn,
  () => {
    if (showDetailDialog.value && selectedPOI.value?.id) {
      loadPOICheckInStatus(selectedPOI.value.id)
      loadPOIFavoriteStatus(selectedPOI.value.id)
    }
  }
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

.poi-dialog-content {
  display: flex;
  flex-direction: column;
  gap: 22px;
}

.poi-overview {
  padding: 24px;
  border-radius: 28px;
  background: linear-gradient(135deg, rgba(209, 237, 224, 0.92), rgba(247, 250, 247, 0.96));
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

.poi-check-in-card {
  margin-top: 18px;
  padding: 18px;
  border-radius: 20px;
  border: 1px solid var(--front-border);
  background: rgba(255, 255, 255, 0.86);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.poi-check-in-meta {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.poi-check-in-label {
  color: var(--front-text-muted);
  font-size: 12px;
}

.poi-check-in-meta strong {
  color: var(--front-text);
  font-size: 18px;
}

.poi-check-in-meta p {
  margin: 0;
  color: var(--front-text-muted);
  font-size: 13px;
  line-height: 1.5;
}

.dialog-actions {
  display: flex;
  justify-content: flex-end;
}

.poi-review-section {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.poi-gallery-section {
  margin-top: 20px;
  padding-top: 18px;
  border-top: 1px solid var(--front-border);
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.gallery-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.gallery-head h3 {
  margin: 0;
  font-size: 16px;
  font-family: var(--font-serif);
  color: var(--ink-900);
}

.gallery-count {
  color: var(--ink-400);
  font-size: 13px;
}

.gallery-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 8px;
}

.gallery-thumb {
  width: 100%;
  height: 100px;
  border-radius: 12px;
  overflow: hidden;
}

.gallery-error {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--paper-100);
  color: var(--ink-400);
  font-size: 12px;
}

.gallery-more {
  display: flex;
  justify-content: center;
}

@media (max-width: 640px) {
  .gallery-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .gallery-thumb {
    height: 80px;
  }
}

.poi-review-header h3 {
  margin: 0;
  font-size: 20px;
  color: var(--front-text);
}

.poi-review-header p {
  margin: 6px 0 0;
  color: var(--front-text-muted);
  font-size: 13px;
}

.poi-review-composer {
  padding: 16px 18px;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.86);
  border: 1px solid var(--front-border);
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.rating-stars {
  display: flex;
  align-items: center;
  gap: 4px;
}

.rating-label {
  color: var(--front-text-soft);
  font-size: 13px;
  margin-right: 6px;
}

.star-btn {
  font-size: 24px;
  color: #d1d5db;
  cursor: pointer;
  transition: color 0.15s;
  user-select: none;
}

.star-btn.active {
  color: #f59e0b;
}

.rating-text {
  margin-left: 8px;
  color: var(--front-text-muted);
  font-size: 13px;
}

.review-composer-actions {
  display: flex;
  justify-content: flex-end;
}

.poi-review-login-tip {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 18px;
  border-radius: 18px;
  background: rgba(248, 250, 252, 1);
  border: 1px solid var(--front-border);
}

.poi-review-login-tip span {
  color: var(--front-text-muted);
  font-size: 13px;
}

.poi-review-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.poi-review-item {
  padding: 14px 16px;
  border-radius: 16px;
  background: var(--paper-50);
}

.review-item-head {
  display: flex;
  align-items: center;
  gap: 10px;
}

.review-avatar {
  background: linear-gradient(135deg, var(--forest-500), var(--forest-700));
  color: #fff;
  font-weight: 700;
}

.review-item-meta {
  flex: 1;
  min-width: 0;
}

.review-item-author {
  display: flex;
  align-items: center;
  gap: 8px;
}

.review-item-author strong {
  color: var(--front-text);
  font-size: 14px;
}

.review-stars span {
  font-size: 14px;
  color: #d1d5db;
}

.review-stars span.filled {
  color: #f59e0b;
}

.review-item-meta time {
  display: block;
  margin-top: 2px;
  color: var(--front-text-muted);
  font-size: 12px;
}

.review-item-content {
  margin: 10px 0 0;
  color: var(--front-text-soft);
  font-size: 14px;
  line-height: 1.6;
  white-space: pre-wrap;
}

.review-more {
  display: flex;
  justify-content: center;
  padding-top: 4px;
}

:deep(.poi-dialog .el-dialog) {
  max-width: min(920px, calc(100vw - 32px));
  border-radius: 28px;
}

:deep(.poi-dialog .el-dialog__body) {
  padding-top: 10px;
}

@media (max-width: 768px) {
  .map-shell.mobile-route-panel-open {
    padding-bottom: 60svh;
  }

  .map-container {
    min-height: 62svh;
    overflow: visible;
  }

  .map-view {
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

  .map-toolbar {
    right: 12px;
    bottom: max(12px, env(safe-area-inset-bottom));
  }

  .map-toolbar-button {
    min-height: 40px;
    padding-inline: 14px;
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

  .poi-overview {
    padding: 18px;
    border-radius: 22px;
  }

  .poi-overview-card h3 {
    font-size: 22px;
  }

  .poi-meta-grid {
    grid-template-columns: 1fr;
  }

  .poi-actions {
    flex-direction: column;
  }

  .poi-actions :deep(.el-button) {
    width: 100%;
    min-height: 42px;
  }

  .poi-check-in-card {
    flex-direction: column;
    align-items: stretch;
  }

  .poi-check-in-card :deep(.el-button) {
    width: 100%;
    min-height: 42px;
  }

  .dialog-actions :deep(.el-button) {
    width: 100%;
    min-height: 42px;
  }

  :deep(.poi-dialog .el-dialog) {
    width: 100% !important;
    max-width: none;
    height: 100svh;
    margin: 0;
    border-radius: 0;
  }

  :deep(.poi-dialog .el-dialog__header) {
    padding: 16px 16px 12px;
  }

  :deep(.poi-dialog .el-dialog__body) {
    padding: 0 16px 16px;
    max-height: calc(100svh - 128px);
    overflow-y: auto;
    -webkit-overflow-scrolling: touch;
  }

  :deep(.poi-dialog .el-dialog__footer) {
    padding: 8px 16px 16px;
  }
}

@media (max-width: 480px) {
  .map-container {
    min-height: 58svh;
  }

  .map-view {
    height: 58svh;
  }

  .poi-overview-card h3 {
    font-size: 20px;
  }

  .poi-category {
    font-size: 11px;
  }
}
</style>

