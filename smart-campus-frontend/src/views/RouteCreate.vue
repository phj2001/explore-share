<template>
  <div class="route-create-page front-page">
    <Header />

    <main class="create-main">
      <div class="front-shell create-shell">
        <div class="create-layout">

          <!-- 左侧：表单 + 途经点列表 -->
          <section class="create-form-panel front-panel">
            <div class="card-head">
              <span class="front-kicker">路线规划</span>
              <h2>创建新路线</h2>
            </div>

            <el-form ref="formRef" :model="form" :rules="rules" label-position="top" class="create-form">
              <el-form-item label="路线标题" prop="title">
                <el-input v-model="form.title" maxlength="100" show-word-limit placeholder="给路线起个名字" />
              </el-form-item>

              <el-form-item label="路线摘要" prop="summary">
                <el-input v-model="form.summary" maxlength="200" show-word-limit placeholder="一句话描述路线亮点" />
              </el-form-item>

              <el-form-item label="详细描述">
                <el-input v-model="form.description" type="textarea" :rows="3" placeholder="路线的详细说明" />
              </el-form-item>

              <el-form-item label="出行方式">
                <el-radio-group v-model="form.defaultMode">
                  <el-radio value="walking">步行</el-radio>
                  <el-radio value="cycling">骑行</el-radio>
                  <el-radio value="driving">驾车</el-radio>
                </el-radio-group>
              </el-form-item>
            </el-form>

            <div class="waypoints-section">
              <div class="waypoints-head">
                <h3>途经点<span v-if="form.waypoints.length" class="wp-count">（{{ form.waypoints.length }}）</span></h3>
                <span class="wp-hint">在右侧地图点击添加</span>
              </div>

              <div v-if="form.waypoints.length" class="waypoints-list">
                <div v-for="(wp, idx) in form.waypoints" :key="idx" class="waypoint-item">
                  <div class="wp-seq">{{ idx + 1 }}</div>
                  <div class="wp-body">
                    <el-input v-model="wp.waypointName" size="small" placeholder="途经点名称" class="wp-name-input" />
                    <el-select
                      v-model="wp.poiId"
                      size="small"
                      placeholder="关联附近地点（可选）"
                      clearable
                      :loading="waypointNearbyLoading[idx]"
                      class="wp-poi-select"
                    >
                      <el-option
                        v-for="poi in (waypointNearbyPois[idx] || [])"
                        :key="poi.id"
                        :label="poi.name"
                        :value="poi.id"
                      >
                        <span class="poi-option-name">{{ poi.name }}</span>
                        <span class="poi-option-cat">{{ poi.category }}</span>
                      </el-option>
                      <template v-if="!waypointNearbyLoading[idx] && !(waypointNearbyPois[idx]?.length)" #empty>
                        <p class="poi-option-empty">附近 1km 内无可关联地点</p>
                      </template>
                    </el-select>
                    <span class="wp-coords">{{ wp.latitude?.toFixed(5) }}, {{ wp.longitude?.toFixed(5) }}</span>
                  </div>
                  <el-button text type="danger" size="small" @click="removeWaypoint(idx)">删除</el-button>
                </div>
              </div>

              <div v-else class="wp-empty">
                在右侧地图点击位置来添加途经点
              </div>
            </div>

            <div class="form-actions">
              <el-button @click="router.back()">取消</el-button>
              <el-button type="primary" :loading="submitting" @click="handleSubmit">提交审核</el-button>
            </div>
          </section>

          <!-- 右侧：地图 -->
          <section class="create-map-panel">
            <div class="map-tip">
              <span>{{ form.waypoints.length ? `已添加 ${form.waypoints.length} 个途经点，继续点击地图添加更多` : '点击地图添加途经点' }}</span>
            </div>
            <div ref="mapRoot" class="create-map-view"></div>
            <div v-if="mapError" class="map-error-tip">{{ mapError }}</div>
          </section>

        </div>
      </div>
    </main>

    <Footer />
  </div>
</template>

<script setup>
import { onMounted, onUnmounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import Header from '@/components/common/Header.vue'
import Footer from '@/components/common/Footer.vue'
import { createRoute } from '@/api/userRoute'
import { getPOIsInBounds } from '@/api/poi'
import { fromAmapCoordinate, loadAmapSdk, toAmapCoordinate } from '@/utils/amap'
import { useMapStore } from '@/stores/map'

const NEARBY_DELTA = 0.009 // 约 1km

const router = useRouter()
const mapStore = useMapStore()
const formRef = ref(null)
const mapRoot = ref(null)
const submitting = ref(false)
const mapError = ref('')

const waypointNearbyPois = ref([])    // 每个途经点对应的附近 POI 列表
const waypointNearbyLoading = ref([]) // 每个途经点的加载状态

let AMapRef = null
let map = null
let waypointMarkers = []
let routePolyline = null

const form = reactive({
  title: '',
  summary: '',
  description: '',
  defaultMode: 'walking',
  waypoints: []
})

const rules = {
  title: [{ required: true, message: '请输入路线标题', trigger: 'blur' }]
}

const createWaypointMarkerContent = (index) => {
  return `
    <div style="
      width: 32px;
      height: 32px;
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 13px;
      font-weight: 700;
      color: #ffffff;
      border: 3px solid rgba(255, 255, 255, 0.92);
      background: linear-gradient(135deg, #1f8c69, #2d9e7a);
      box-shadow: 0 8px 18px rgba(31, 140, 105, 0.35);
      user-select: none;
      -webkit-user-select: none;
    ">${index}</div>
  `
}

const redrawMarkers = () => {
  if (!map || !AMapRef) return

  if (waypointMarkers.length) {
    map.remove(waypointMarkers)
    waypointMarkers = []
  }

  if (routePolyline) {
    map.remove(routePolyline)
    routePolyline = null
  }

  const positions = []
  form.waypoints.forEach((wp, idx) => {
    const mapCoord = toAmapCoordinate(wp.latitude, wp.longitude)
    if (!mapCoord) return

    const pos = [mapCoord.lng, mapCoord.lat]
    positions.push(pos)

    const marker = new AMapRef.Marker({
      position: pos,
      content: createWaypointMarkerContent(idx + 1),
      anchor: 'center',
      title: wp.waypointName || `途经点 ${idx + 1}`
    })
    map.add(marker)
    waypointMarkers.push(marker)
  })

  if (positions.length >= 2) {
    routePolyline = new AMapRef.Polyline({
      path: positions,
      strokeColor: '#1f8c69',
      strokeWeight: 4,
      strokeOpacity: 0.85,
      strokeStyle: 'solid',
      lineJoin: 'round',
      lineCap: 'round'
    })
    map.add(routePolyline)
  }
}

const fetchNearbyPois = async (idx, rawLat, rawLng) => {
  waypointNearbyLoading.value[idx] = true
  try {
    const delta = NEARBY_DELTA
    const data = await getPOIsInBounds(
      rawLat - delta, rawLat + delta,
      rawLng - delta, rawLng + delta,
      50
    )
    waypointNearbyPois.value[idx] = data?.records || []
  } catch {
    waypointNearbyPois.value[idx] = []
  } finally {
    waypointNearbyLoading.value[idx] = false
  }
}

const handleMapClick = (event) => {
  const clickedLat = event.lnglat.getLat()
  const clickedLng = event.lnglat.getLng()

  const converted = fromAmapCoordinate(clickedLat, clickedLng)
  const rawLat = converted?.lat ?? clickedLat
  const rawLng = converted?.lng ?? clickedLng

  const idx = form.waypoints.length
  form.waypoints.push({
    poiId: null,
    latitude: rawLat,
    longitude: rawLng,
    waypointName: `途经点 ${idx + 1}`,
    sortOrder: idx
  })
  waypointNearbyPois.value.push([])
  waypointNearbyLoading.value.push(false)
  fetchNearbyPois(idx, rawLat, rawLng)

  redrawMarkers()
}

const removeWaypoint = (idx) => {
  form.waypoints.splice(idx, 1)
  waypointNearbyPois.value.splice(idx, 1)
  waypointNearbyLoading.value.splice(idx, 1)
  form.waypoints.forEach((wp, i) => { wp.sortOrder = i })
  redrawMarkers()
}

const initMap = async () => {
  try {
    AMapRef = await loadAmapSdk()

    const center = toAmapCoordinate(mapStore.center.lat, mapStore.center.lng)
    map = new AMapRef.Map(mapRoot.value, {
      viewMode: '2D',
      zoom: mapStore.zoom,
      center: center ? [center.lng, center.lat] : [116.397428, 39.90923],
      resizeEnable: true,
      zooms: [3, 20]
    })

    map.on('click', handleMapClick)
    mapRoot.value.style.cursor = 'crosshair'
  } catch (e) {
    mapError.value = e.message || '地图加载失败，请检查 VITE_AMAP_JS_KEY 配置'
  }
}

const handleSubmit = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  if (!form.waypoints.length) {
    ElMessage.warning('请在地图上添加至少一个途经点')
    return
  }

  submitting.value = true
  try {
    const result = await createRoute({
      title: form.title,
      summary: form.summary,
      description: form.description,
      defaultMode: form.defaultMode,
      waypoints: form.waypoints
    })
    ElMessage.success('路线已提交，等待管理员审核后发布')
    router.push('/')
  } catch (error) {
    ElMessage.error(error.message || '创建失败')
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  await initMap()
})

onUnmounted(() => {
  if (map) {
    map.off('click', handleMapClick)
    map.destroy()
    map = null
  }
  waypointMarkers = []
})
</script>

<style scoped>
.route-create-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.create-main {
  flex: 1;
  padding: 22px 0 30px;
}

.create-shell {
  max-width: 1280px;
}

.create-layout {
  display: grid;
  grid-template-columns: 420px 1fr;
  gap: 20px;
  align-items: start;
}

.create-form-panel {
  padding: 26px;
  border-radius: 28px;
  display: flex;
  flex-direction: column;
  gap: 0;
}

.card-head {
  margin-bottom: 20px;
}

.card-head h2 {
  margin: 10px 0 0;
  font-family: var(--font-serif);
  color: var(--ink-900);
  font-size: 22px;
}

.create-form :deep(.el-form-item__label) {
  font-weight: 600;
  color: var(--ink-700);
}

.waypoints-section {
  margin-top: 20px;
  border-top: 1px solid var(--front-border);
  padding-top: 18px;
}

.waypoints-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
}

.waypoints-head h3 {
  margin: 0;
  font-size: 15px;
  font-weight: 700;
  color: var(--ink-900);
}

.wp-count {
  color: var(--ink-500);
  font-weight: 500;
}

.wp-hint {
  font-size: 12px;
  color: var(--ink-400);
}

.waypoints-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  max-height: 340px;
  overflow-y: auto;
  padding-right: 4px;
}

.waypoint-item {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 10px 12px;
  border-radius: 14px;
  background: var(--paper-50);
  border: 1px solid var(--front-border);
}

.wp-seq {
  width: 26px;
  height: 26px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--forest-600), var(--forest-800));
  color: #fff;
  font-size: 12px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  margin-top: 2px;
}

.wp-body {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.wp-name-input,
.wp-poi-select {
  width: 100%;
}

.wp-coords {
  font-family: var(--font-mono);
  font-size: 11px;
  color: var(--ink-400);
}

.poi-option-name {
  font-size: 13px;
  color: var(--ink-900);
}

.poi-option-cat {
  margin-left: 8px;
  font-size: 11px;
  color: var(--ink-400);
}

.poi-option-empty {
  margin: 0;
  padding: 10px 0;
  text-align: center;
  color: var(--ink-400);
  font-size: 13px;
}

.wp-empty {
  padding: 18px 0;
  text-align: center;
  color: var(--ink-400);
  font-size: 13px;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding-top: 20px;
  border-top: 1px solid var(--front-border);
  margin-top: 20px;
}

/* 右侧地图 */
.create-map-panel {
  position: sticky;
  top: 20px;
  display: flex;
  flex-direction: column;
  gap: 0;
  border-radius: 28px;
  overflow: hidden;
  border: 1px solid var(--front-border);
  box-shadow: var(--front-shadow-md, 0 8px 28px rgba(31, 140, 105, 0.08));
}

.map-tip {
  padding: 12px 18px;
  background: var(--forest-900);
  color: var(--forest-100);
  font-size: 13px;
  text-align: center;
}

.create-map-view {
  width: 100%;
  height: calc(100vh - 200px);
  min-height: 480px;
  cursor: crosshair;
}

.map-error-tip {
  padding: 14px 18px;
  background: rgba(254, 226, 226, 0.96);
  color: #991b1b;
  font-size: 13px;
}

@media (max-width: 960px) {
  .create-layout {
    grid-template-columns: 1fr;
  }

  .create-map-panel {
    position: static;
  }

  .create-map-view {
    height: 50svh;
    min-height: 320px;
  }
}

@media (max-width: 640px) {
  .create-form-panel {
    padding: 18px;
    border-radius: 24px;
  }

  .create-map-view {
    height: 42svh;
  }
}
</style>
