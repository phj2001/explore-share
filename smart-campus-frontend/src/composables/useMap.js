import { ref, computed } from 'vue'
import { useMapStore } from '@/stores/map'
import { usePOIStore } from '@/stores/poi'
import { ElMessage } from 'element-plus'
import L from 'leaflet'

/**
 * 地图相关的组合式函数
 */
export function useMap() {
  const mapStore = useMapStore()
  const poiStore = usePOIStore()

  const mapInstance = ref(null)
  const markersLayer = ref(null)
  const routeLayer = ref(null)

  // 地图中心
  const center = computed(() => mapStore.center)

  // 地图缩放级别
  const zoom = computed(() => mapStore.zoom)

  // 是否正在加载
  const isLoading = computed(() => mapStore.isLoading)

  // 选中的 POI
  const selectedPOI = computed(() => mapStore.selectedPOI)

  /**
   * 初始化地图
   */
  const initMap = (containerId) => {
    if (mapInstance.value) {
      return mapInstance.value
    }

    // 创建地图实例
    const map = L.map(containerId).setView(
      [center.value.lat, center.value.lng],
      zoom.value
    )

    // 添加 OpenStreetMap 图层
    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      attribution: '&copy; OpenStreetMap contributors',
      maxZoom: 19
    }).addTo(map)

    // 创建标记图层
    markersLayer.value = L.layerGroup().addTo(map)

    // 创建路径图层
    routeLayer.value = L.layerGroup().addTo(map)

    // 监听地图移动事件
    map.on('moveend', () => {
      const bounds = map.getBounds()
      mapStore.setCenter(bounds.getCenter().lat, bounds.getCenter().lng)
    })

    // 监听地图缩放事件
    map.on('zoomend', () => {
      mapStore.setZoom(map.getZoom())
    })

    mapInstance.value = map
    return map
  }

  /**
   * 移动地图到指定位置
   */
  const flyTo = (lat, lng, zoomLevel) => {
    if (!mapInstance.value) return

    mapInstance.value.flyTo([lat, lng], zoomLevel)
    mapStore.flyTo(lat, lng, zoomLevel)
  }

  /**
   * 添加 POI 标记
   */
  const addMarker = (poi, options = {}) => {
    if (!mapInstance.value || !markersLayer.value) return

    const marker = L.marker([poi.latitude, poi.longitude], options)

    // 绑定弹出信息
    if (poi.name) {
      marker.bindPopup(`
        <div>
          <strong>${poi.name}</strong><br>
          ${poi.category || ''}<br>
          ${poi.description || ''}
        </div>
      `)
    }

    // 点击事件
    marker.on('click', () => {
      mapStore.selectPOI(poi)
    })

    markersLayer.value.addLayer(marker)
    return marker
  }

  /**
   * 批量添加 POI 标记
   */
  const addMarkers = (pois) => {
    if (!markersLayer.value) return

    // 清除已有标记
    markersLayer.value.clearLayers()

    // 添加新标记
    pois.forEach(poi => {
      addMarker(poi)
    })
  }

  /**
   * 清除所有标记
   */
  const clearMarkers = () => {
    if (markersLayer.value) {
      markersLayer.value.clearLayers()
    }
  }

  /**
   * 绘制路径
   */
  const drawRoute = (routePoints) => {
    if (!mapInstance.value || !routeLayer.value) return

    // 清除已有路径
    routeLayer.value.clearLayers()

    if (!routePoints || routePoints.length < 2) return

    // 绘制路径线
    const polyline = L.polyline(
      routePoints.map(point => [point.lat, point.lng]),
      {
        color: '#409eff',
        weight: 5,
        opacity: 0.7
      }
    )

    routeLayer.value.addLayer(polyline)

    // 调整地图视野以显示完整路径
    mapInstance.value.fitBounds(polyline.getBounds(), { padding: [50, 50] })
  }

  /**
   * 清除路径
   */
  const clearRoute = () => {
    if (routeLayer.value) {
      routeLayer.value.clearLayers()
    }
    mapStore.clearRoute()
  }

  /**
   * 规划路径
   */
  const planRoute = async (startPoint, endPoint) => {
    try {
      mapStore.setRouteStart(startPoint)
      mapStore.setRouteEnd(endPoint)

      const result = await mapStore.planRouteAsync()

      // 绘制路径
      if (result && result.points) {
        drawRoute(result.points)
      }

      ElMessage.success('路径规划成功')
      return result
    } catch (error) {
      ElMessage.error(error.message || '路径规划失败')
      throw error
    }
  }

  /**
   * 获取当前地图视野边界
   */
  const getBounds = () => {
    if (!mapInstance.value) return null

    const bounds = mapInstance.value.getBounds()
    return {
      minLat: bounds.getSouth(),
      maxLat: bounds.getNorth(),
      minLng: bounds.getWest(),
      maxLng: bounds.getEast()
    }
  }

  /**
   * 在当前视野内查找 POI
   */
  const fetchPOIsInBounds = async () => {
    const bounds = getBounds()
    if (!bounds) return

    try {
      await poiStore.fetchInBounds(
        bounds.minLat,
        bounds.maxLat,
        bounds.minLng,
        bounds.maxLng
      )
      return poiStore.poiList
    } catch (error) {
      ElMessage.error('加载附近 POI 失败')
      throw error
    }
  }

  /**
   * 销毁地图实例
   */
  const destroy = () => {
    if (mapInstance.value) {
      mapInstance.value.remove()
      mapInstance.value = null
      markersLayer.value = null
      routeLayer.value = null
    }
  }

  return {
    center,
    zoom,
    isLoading,
    selectedPOI,
    mapInstance,
    initMap,
    flyTo,
    addMarker,
    addMarkers,
    clearMarkers,
    drawRoute,
    clearRoute,
    planRoute,
    getBounds,
    fetchPOIsInBounds,
    destroy
  }
}
