<template>
  <div class="map-container">
    <!-- 地图区域 -->
    <div id="map" class="map-view"></div>

    <!-- POI 详情弹窗 -->
    <el-dialog
      v-model="showDetailDialog"
      :title="selectedPOI?.name"
      width="400px"
    >
      <div v-if="selectedPOI" class="poi-detail">
        <p><strong>分类：</strong>{{ selectedPOI.category }}</p>
        <p><strong>坐标：</strong>{{ selectedPOI.latitude }}, {{ selectedPOI.longitude }}</p>
        <p><strong>描述：</strong>{{ selectedPOI.description || '暂无描述' }}</p>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, watch } from 'vue'
import { usePOIStore } from '@/stores/poi'
import { useMapStore } from '@/stores/map'
import { ElMessage } from 'element-plus'
import L from 'leaflet'

const poiStore = usePOIStore()
const mapStore = useMapStore()

const showDetailDialog = ref(false)
const selectedPOI = ref(null)

let map = null
let markers = null

onMounted(async () => {
  await initMap()
  await loadPOIs()
})

onUnmounted(() => {
  if (map) {
    map.remove()
    map = null
  }
})

// 监听 POI 列表变化，重新渲染标记
watch(() => poiStore.poiList, () => {
  renderMarkers()
}, { deep: true })

const initMap = () => {
  return new Promise((resolve) => {
    // 初始化地图，禁用缩放控件
    map = L.map('map', {
      zoomControl: false
    }).setView(
      [mapStore.center.lat, mapStore.center.lng],
      mapStore.zoom
    )

    // 添加地图图层
    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      attribution: '&copy; OpenStreetMap contributors'
    }).addTo(map)

    // 监听地图移动事件
    map.on('moveend', handleMapMove)

    resolve()
  })
}

const loadPOIs = async () => {
  try {
    await Promise.all([
      poiStore.fetchAllPOIs(),
      poiStore.fetchCategories()
    ])
    renderMarkers()
  } catch (error) {
    ElMessage.error('加载 POI 失败')
  }
}

const renderMarkers = () => {
  if (!map) return

  // 清除已有标记
  if (markers) {
    map.removeLayer(markers)
  }

  // 创建标记组
  markers = L.layerGroup()

  // 添加 POI 标记
  poiStore.poiList.forEach((poi) => {
    const marker = L.marker([poi.latitude, poi.longitude])

    // 点击标记显示详情
    marker.on('click', () => {
      selectedPOI.value = poi
      showDetailDialog.value = true
    })

    markers.addLayer(marker)
  })

  markers.addTo(map)
}

const handleMapMove = async () => {
  const bounds = map.getBounds()
  const minLat = bounds.getSouth()
  const maxLat = bounds.getNorth()
  const minLng = bounds.getWest()
  const maxLng = bounds.getEast()

  try {
    await poiStore.fetchInBounds(minLat, maxLat, minLng, maxLng)
  } catch (error) {
    // 静默失败，避免频繁提示
  }
}
</script>

<style scoped>
.map-container {
  position: relative;
  width: 100%;
  height: calc(100vh - 60px);
}

.map-view {
  width: 100%;
  height: 100%;
}

.poi-detail p {
  margin: 10px 0;
  line-height: 1.6;
}
</style>
