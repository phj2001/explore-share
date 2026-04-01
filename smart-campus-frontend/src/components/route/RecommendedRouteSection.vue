<template>
  <section class="route-section">
    <div class="section-header">
      <div>
        <span class="section-kicker">精选路线</span>
        <h2>按路线去探索地点</h2>
        <p>
          把多个地点串成一条完整体验线。打开路线后，地图会按途经顺序自动规划，适合漫步、骑行和轻旅行场景。
        </p>
      </div>
      <el-button text @click="loadRoutes">刷新路线</el-button>
    </div>

    <div v-loading="loading">
      <div v-if="routes.length" class="route-grid">
        <article v-for="item in routes" :key="item.id" class="route-card">
          <div class="route-cover-wrap">
            <img v-if="item.coverImageUrl" :src="resolveAssetUrl(item.coverImageUrl)" :alt="item.title" class="route-cover">
            <div v-else class="route-cover route-cover-fallback">
              <span>{{ item.defaultModeLabel }}</span>
            </div>

            <div class="route-glance">
              <span>{{ item.waypointCount }} 个地点</span>
              <span>{{ item.startPoiName }} -> {{ item.endPoiName }}</span>
            </div>
          </div>

          <div class="route-card-body">
            <div class="route-badges">
              <span class="badge badge-mode">{{ item.defaultModeLabel }}</span>
              <span class="badge">{{ getRouteTempoLabel(item) }}</span>
            </div>

            <h3>{{ item.title }}</h3>
            <p class="route-summary">{{ item.summary }}</p>

            <div class="route-meta">
              <span class="meta-line">起点：{{ item.startPoiName || '未设置' }}</span>
              <span class="meta-line">终点：{{ item.endPoiName || '未设置' }}</span>
              <span v-if="item.recommendationText" class="meta-note">推荐语：{{ item.recommendationText }}</span>
            </div>

            <div class="route-actions">
              <el-button plain @click="openDetail(item.id)">查看详情</el-button>
              <el-button type="primary" :loading="activatingId === item.id" @click="activateRoute(item.id)">
                在地图中查看
              </el-button>
            </div>
          </div>
        </article>
      </div>

      <el-empty v-else description="当前还没有已发布的推荐路线" />
    </div>

    <el-dialog v-model="dialogVisible" title="推荐路线详情" width="760px" destroy-on-close>
      <template v-if="selectedRoute">
        <div class="detail-layout">
          <img
            v-if="selectedRoute.coverImageUrl"
            :src="resolveAssetUrl(selectedRoute.coverImageUrl)"
            :alt="selectedRoute.title"
            class="detail-cover"
          >

          <div class="detail-copy">
            <div class="route-badges">
              <span class="badge badge-mode">{{ selectedRoute.defaultModeLabel }}</span>
              <span class="badge">{{ selectedRoute.waypoints.length }} 个地点</span>
              <span v-if="selectedRoute.recommendationText" class="badge">{{ selectedRoute.recommendationText }}</span>
            </div>

            <h3>{{ selectedRoute.title }}</h3>
            <p class="route-summary">{{ selectedRoute.summary }}</p>
            <p class="route-description">{{ selectedRoute.description }}</p>

            <div class="timeline">
              <div v-for="(point, index) in selectedRoute.waypoints" :key="`${point.poiId}-${index}`" class="timeline-item">
                <span class="timeline-index">{{ index + 1 }}</span>
                <div class="timeline-copy">
                  <strong>{{ point.poiName }}</strong>
                  <p>{{ point.poiCategory || '未分类' }}</p>
                </div>
                <el-button text @click="focusWaypoint(point)">查看地点</el-button>
              </div>
            </div>
          </div>
        </div>
      </template>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dialogVisible = false">关闭</el-button>
          <el-button type="primary" :loading="activatingId === selectedRoute?.id" @click="activateSelectedRoute">
            在地图中查看路线
          </el-button>
        </div>
      </template>
    </el-dialog>
  </section>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { API_ORIGIN } from '@/utils/request'
import { getRecommendedRouteDetail, getRecommendedRouteList } from '@/api/recommendedRoute'
import { useMapStore } from '@/stores/map'

const mapStore = useMapStore()

const loading = ref(false)
const routes = ref([])
const dialogVisible = ref(false)
const selectedRoute = ref(null)
const activatingId = ref(null)

const resolveAssetUrl = (value) => {
  if (!value) {
    return ''
  }
  if (/^https?:\/\//i.test(value)) {
    return value
  }
  return `${API_ORIGIN}${value.startsWith('/') ? value : `/${value}`}`
}

const getRouteTempoLabel = (item) => {
  if ((item.waypointCount || 0) >= 6) {
    return '长线探索'
  }
  if ((item.waypointCount || 0) >= 4) {
    return '半日路线'
  }
  return '轻量路线'
}

const loadRoutes = async () => {
  loading.value = true
  try {
    routes.value = await getRecommendedRouteList({ limit: 4 })
  } catch (error) {
    ElMessage.error(error.message || '加载推荐路线失败')
  } finally {
    loading.value = false
  }
}

const openDetail = async (routeId) => {
  try {
    selectedRoute.value = await getRecommendedRouteDetail(routeId)
    dialogVisible.value = true
  } catch (error) {
    ElMessage.error(error.message || '加载路线详情失败')
  }
}

const activateRoute = async (routeId) => {
  activatingId.value = routeId
  try {
    const detail = await getRecommendedRouteDetail(routeId)
    await mapStore.applyRecommendedRoute(detail)
    window.scrollTo({ top: 0, behavior: 'smooth' })
    ElMessage.success(`已切换到路线“${detail.title}”`)
  } catch (error) {
    ElMessage.error(error.message || '加载路线失败')
  } finally {
    activatingId.value = null
  }
}

const activateSelectedRoute = async () => {
  if (!selectedRoute.value) {
    return
  }
  activatingId.value = selectedRoute.value.id
  try {
    await mapStore.applyRecommendedRoute(selectedRoute.value)
    dialogVisible.value = false
    window.scrollTo({ top: 0, behavior: 'smooth' })
    ElMessage.success(`已切换到路线“${selectedRoute.value.title}”`)
  } catch (error) {
    ElMessage.error(error.message || '加载路线失败')
  } finally {
    activatingId.value = null
  }
}

const focusWaypoint = async (point) => {
  try {
    const poi = await mapStore.fetchPOIById(point.poiId)
    mapStore.selectPOI(poi)
    window.scrollTo({ top: 0, behavior: 'smooth' })
  } catch (error) {
    ElMessage.error(error.message || '加载地点详情失败')
  }
}

onMounted(loadRoutes)
</script>

<style scoped>
.route-section {
  padding: 30px 28px 10px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 20px;
}

.section-kicker {
  display: inline-flex;
  padding: 6px 12px;
  border-radius: 999px;
  background: rgba(14, 165, 233, 0.1);
  color: #0369a1;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
}

.section-header h2 {
  margin: 14px 0 8px;
  color: #0f172a;
  font-size: clamp(28px, 4vw, 38px);
}

.section-header p {
  margin: 0;
  max-width: 760px;
  color: #475569;
  line-height: 1.8;
}

.route-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 18px;
}

.route-card {
  overflow: hidden;
  border-radius: 24px;
  border: 1px solid rgba(148, 163, 184, 0.16);
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 16px 34px rgba(15, 23, 42, 0.08);
}

.route-cover-wrap {
  position: relative;
  height: 188px;
  overflow: hidden;
}

.route-cover {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.route-cover-fallback {
  display: flex;
  align-items: center;
  justify-content: center;
  background:
    radial-gradient(circle at top right, rgba(16, 185, 129, 0.16), transparent 34%),
    linear-gradient(135deg, #0f172a, #0f766e);
  color: #f8fafc;
  font-size: 20px;
  font-weight: 700;
}

.route-glance {
  position: absolute;
  left: 14px;
  right: 14px;
  bottom: 14px;
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 10px 12px;
  border-radius: 16px;
  background: rgba(15, 23, 42, 0.62);
  color: #f8fafc;
  backdrop-filter: blur(10px);
  font-size: 12px;
}

.route-card-body {
  padding: 18px;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.route-badges {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.badge {
  display: inline-flex;
  padding: 5px 10px;
  border-radius: 999px;
  background: rgba(226, 232, 240, 0.8);
  color: #475569;
  font-size: 12px;
}

.badge-mode {
  background: rgba(14, 165, 233, 0.12);
  color: #0369a1;
}

.route-card h3,
.detail-copy h3 {
  margin: 0;
  color: #0f172a;
  font-size: 22px;
}

.route-summary,
.route-description {
  margin: 0;
  color: #475569;
  line-height: 1.75;
}

.route-meta {
  display: flex;
  flex-direction: column;
  gap: 6px;
  color: #64748b;
  font-size: 13px;
}

.meta-note {
  color: #0f766e;
}

.route-actions,
.dialog-footer {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.detail-layout {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.detail-cover {
  width: 100%;
  height: 260px;
  object-fit: cover;
  border-radius: 22px;
}

.detail-copy {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.timeline {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-top: 6px;
}

.timeline-item {
  display: grid;
  grid-template-columns: 28px minmax(0, 1fr) auto;
  gap: 12px;
  align-items: center;
  padding: 12px 14px;
  border-radius: 18px;
  background: rgba(248, 250, 252, 0.9);
}

.timeline-index {
  width: 28px;
  height: 28px;
  border-radius: 999px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: rgba(14, 165, 233, 0.14);
  color: #0369a1;
  font-weight: 700;
}

.timeline-copy strong {
  color: #0f172a;
}

.timeline-copy p {
  margin: 6px 0 0;
  color: #64748b;
}

@media (max-width: 1280px) {
  .route-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 760px) {
  .route-section {
    padding: 22px 16px 4px;
  }

  .section-header,
  .route-actions,
  .dialog-footer {
    flex-direction: column;
    align-items: stretch;
  }

  .route-grid {
    grid-template-columns: 1fr;
  }

  .timeline-item {
    grid-template-columns: 28px minmax(0, 1fr);
  }
}
</style>
