<template>
  <div class="footprint-map">
    <!-- v-show 而非 v-if：AMap 初始化要求容器已在 DOM 且有尺寸 -->
    <div v-show="hasPoints && !mapFailed" ref="mapRef" class="map-canvas"></div>

    <div v-if="mapFailed" class="map-placeholder">地图加载失败，请稍后重试</div>
    <div v-else-if="!hasPoints" class="map-placeholder">暂无打卡足迹</div>
    <div v-else-if="!mapReady" class="map-placeholder">地图加载中…</div>
  </div>
</template>

<script setup>
import { computed, onUnmounted, ref, watch } from 'vue'
import { loadAmapSdk, toAmapCoordinate } from '@/utils/amap'
import { resolvePoiSymbol, renderPoiIconSvg } from '@/utils/poiSymbol'

/**
 * 打卡足迹地图：轻量独立组件，不复用强耦合的 MapContainer。
 * props.checkins：UserCheckInResponse 列表（poiId/poiName/category/latitude/longitude 均已返回）
 * 同一 POI 多次打卡只打一个 marker，并以 ×N 角标展示次数。
 */
const props = defineProps({
  checkins: { type: Array, default: () => [] }
})

const mapRef = ref(null)
const mapReady = ref(false)
const mapFailed = ref(false)

let map = null
let markers = []

const hasPoints = computed(() =>
  props.checkins.some((item) => toAmapCoordinate(item?.latitude, item?.longitude))
)

const escapeHtml = (value) => String(value ?? '')
  .replaceAll('&', '&amp;')
  .replaceAll('<', '&lt;')
  .replaceAll('>', '&gt;')
  .replaceAll('"', '&quot;')

const createMarkerContent = (item, count) => {
  const symbol = resolvePoiSymbol(item.category)
  const icon = renderPoiIconSvg(symbol, { size: 12, color: '#ffffff' })
  // marker content 由 AMap 注入地图容器，脱离本组件 scoped 样式，全部 inline
  const countBadge = count > 1
    ? `<span style="position:absolute;top:-7px;right:-11px;min-width:18px;height:18px;padding:0 4px;box-sizing:border-box;border-radius:9px;background:#1f8c69;border:1.5px solid #fff;color:#fff;font-size:10px;font-weight:600;line-height:15px;text-align:center;">×${count}</span>`
    : ''
  const name = escapeHtml(item.poiName || '未命名地点')
  return `
    <div style="display:flex;flex-direction:column;align-items:center;gap:3px;">
      <div style="position:relative;display:flex;align-items:center;justify-content:center;width:28px;height:28px;border-radius:999px;background:${symbol.hex};border:2px solid #fff;box-shadow:0 3px 8px rgba(15,40,30,0.35);">
        ${icon}${countBadge}
      </div>
      <div style="max-width:110px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;padding:1px 7px;border-radius:6px;background:rgba(255,255,255,0.94);color:#25433a;font-size:11px;line-height:1.5;box-shadow:0 1px 4px rgba(15,40,30,0.15);" title="${name}">${name}</div>
    </div>
  `
}

const drawMarkers = () => {
  if (!map) return
  if (markers.length) {
    map.remove(markers)
    markers = []
  }

  // 按 poiId 去重（无 poiId 时退化为经纬度组合键），同点累计打卡次数
  const deduped = new Map()
  for (const item of props.checkins) {
    const key = item?.poiId != null
      ? `poi-${item.poiId}`
      : `${item?.latitude},${item?.longitude}`
    const existing = deduped.get(key)
    if (existing) {
      existing.count += 1
    } else {
      deduped.set(key, { item, count: 1 })
    }
  }

  const AMap = window.AMap
  for (const { item, count } of deduped.values()) {
    // ⚠️ toAmapCoordinate(lat, lng) 入参顺序与 marker position 的 [lng, lat] 相反
    const coordinate = toAmapCoordinate(item?.latitude, item?.longitude)
    if (!coordinate) continue

    const marker = new AMap.Marker({
      position: [coordinate.lng, coordinate.lat],
      content: createMarkerContent(item, count),
      anchor: 'center',
      title: item?.poiName || '打卡地点'
    })
    markers.push(marker)
  }

  if (markers.length) {
    map.add(markers)
    map.setFitView(markers, false, [50, 50, 50, 50])
  }
}

const initMap = async () => {
  if (map || mapFailed.value) return
  try {
    await loadAmapSdk()
    if (!mapRef.value) return
    map = new window.AMap.Map(mapRef.value, { zoom: 14, resizeEnable: true })
    mapReady.value = true
    drawMarkers()
  } catch {
    mapFailed.value = true
  }
}

// checkins 整体替换（首页加载/加载更多），浅监听即可触发重绘
watch(() => props.checkins, () => {
  if (!hasPoints.value) return
  if (!map) {
    initMap()
    return
  }
  drawMarkers()
})

onUnmounted(() => {
  markers = []
  if (map) {
    map.destroy()
    map = null
  }
})
</script>

<style scoped lang="scss">
.footprint-map {
  position: relative;
  height: 300px;
  border-radius: 10px;
  overflow: hidden;
  border: 1px solid var(--front-border);
  background: var(--paper-100, #f6f4ee);
}

.map-canvas {
  width: 100%;
  height: 100%;
}

.map-placeholder {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-family: var(--font-mono);
  font-size: 11px;
  letter-spacing: 0.08em;
  color: var(--ink-400);
}
</style>
