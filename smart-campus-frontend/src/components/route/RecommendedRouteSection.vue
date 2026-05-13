<template>
  <section v-if="!dataLoaded || routes.length > 0" class="front-shell route-section">
    <div class="section-header">
      <div class="head-left">
        <span class="section-kicker">精选路线</span>
        <h2>按路线去探索地点</h2>
      </div>
      <button class="refresh-btn" :disabled="loading" title="刷新路线" @click="loadRoutes(true)">
        <svg class="refresh-icon" :class="{ spinning: loading }" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M23 4v6h-6M1 20v-6h6"/>
          <path d="M3.51 9a9 9 0 0114.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0020.49 15"/>
        </svg>
      </button>
    </div>

    <el-skeleton v-if="loading && !routes.length" :rows="4" animated />

    <div v-else-if="routes.length" class="route-grid">
      <article v-for="item in routes" :key="item.id" class="route-card">
        <div class="route-cover-wrap">
          <img v-if="item.coverImageUrl" :src="resolveAssetUrl(item.coverThumbnailUrl || item.coverImageUrl)" :alt="item.title" class="route-cover" loading="lazy">
          <div v-else class="route-cover route-cover-fallback">
            <span>{{ item.defaultModeLabel }}</span>
          </div>

          <div class="route-glance">
            <span>{{ item.waypointCount }} 个地点</span>
            <span>{{ item.startPoiName }} → {{ item.endPoiName }}</span>
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
            <span v-if="item.recommendationText" class="meta-note">{{ item.recommendationText }}</span>
          </div>

          <div class="route-actions">
            <el-button plain size="small" @click="openDetail(item.id)">查看详情</el-button>
            <el-button type="primary" size="small" :loading="activatingId === item.id" @click="activateRoute(item.id)">
              在地图中查看
            </el-button>
          </div>
        </div>
      </article>
    </div>

    <el-dialog v-model="dialogVisible" title="推荐路线详情" width="760px" destroy-on-close>
      <template v-if="selectedRoute">
        <div class="detail-layout">
          <img
            v-if="selectedRoute.coverImageUrl"
            :src="resolveAssetUrl(selectedRoute.coverImageUrl)"
            :alt="selectedRoute.title"
            class="detail-cover"
            loading="lazy"
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
const dataLoaded = ref(false)
const routes = ref([])
const dialogVisible = ref(false)
const selectedRoute = ref(null)
const activatingId = ref(null)

const resolveAssetUrl = (value) => {
  if (!value) return ''
  if (/^https?:\/\//i.test(value)) return value
  return `${API_ORIGIN}${value.startsWith('/') ? value : `/${value}`}`
}

const getRouteTempoLabel = (item) => {
  if ((item.waypointCount || 0) >= 6) return '长线探索'
  if ((item.waypointCount || 0) >= 4) return '半日路线'
  return '轻量路线'
}

const loadRoutes = async (forceRefresh = false) => {
  loading.value = true
  try {
    routes.value = await getRecommendedRouteList({ limit: 4 }, { forceRefresh })
  } catch (error) {
    ElMessage.error(error.message || '加载推荐路线失败')
  } finally {
    loading.value = false
    dataLoaded.value = true
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
    ElMessage.success(`已切换到路线"${detail.title}"`)
  } catch (error) {
    ElMessage.error(error.message || '加载路线失败')
  } finally {
    activatingId.value = null
  }
}

const activateSelectedRoute = async () => {
  if (!selectedRoute.value) return
  activatingId.value = selectedRoute.value.id
  try {
    await mapStore.applyRecommendedRoute(selectedRoute.value)
    dialogVisible.value = false
    window.scrollTo({ top: 0, behavior: 'smooth' })
    ElMessage.success(`已切换到路线"${selectedRoute.value.title}"`)
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

onMounted(() => loadRoutes(false))
</script>

<style scoped>
.route-section {
  padding: 24px 0;
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.head-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.section-kicker {
  display: inline-flex;
  padding: 4px 10px;
  border-radius: 999px;
  background: rgba(20, 184, 166, 0.1);
  color: #0f766e;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.06em;
  flex-shrink: 0;
}

.section-header h2 {
  margin: 0;
  color: var(--front-text);
  font-size: 20px;
  font-weight: 700;
}

.refresh-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  border: 1px solid var(--front-border);
  background: transparent;
  color: var(--front-text-muted);
  cursor: pointer;
  flex-shrink: 0;
  transition: color 0.15s, background 0.15s;
}

.refresh-btn:hover:not(:disabled) {
  background: rgba(20, 184, 166, 0.1);
  color: #0f766e;
  border-color: transparent;
}

.refresh-btn:disabled {
  opacity: 0.4;
  cursor: default;
}

.refresh-icon {
  width: 14px;
  height: 14px;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.spinning {
  animation: spin 0.8s linear infinite;
}

.route-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 18px;
}

.route-card {
  overflow: hidden;
  border-radius: 22px;
  border: 1px solid var(--front-border);
  background: rgba(255, 255, 255, 0.92);
  box-shadow: var(--front-shadow-soft);
}

.route-cover-wrap {
  position: relative;
  height: 180px;
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
    radial-gradient(circle at top right, rgba(20, 184, 166, 0.18), transparent 34%),
    linear-gradient(135deg, #14303a, #0f766e);
  color: #f8fafc;
  font-size: 18px;
  font-weight: 700;
}

.route-glance {
  position: absolute;
  left: 12px;
  right: 12px;
  bottom: 12px;
  display: flex;
  flex-direction: column;
  gap: 3px;
  padding: 8px 10px;
  border-radius: 12px;
  background: rgba(15, 23, 42, 0.6);
  color: #f8fafc;
  backdrop-filter: blur(10px);
  font-size: 11px;
}

.route-card-body {
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.route-badges {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.badge {
  display: inline-flex;
  padding: 4px 9px;
  border-radius: 999px;
  background: rgba(229, 239, 242, 0.84);
  color: var(--front-text-soft);
  font-size: 11px;
}

.badge-mode {
  background: rgba(20, 184, 166, 0.12);
  color: #0f766e;
}

.route-card h3,
.detail-copy h3 {
  margin: 0;
  color: var(--front-text);
  font-size: 17px;
}

.route-summary,
.route-description {
  margin: 0;
  color: var(--front-text-soft);
  font-size: 13px;
  line-height: 1.7;
}

.route-meta {
  display: flex;
  flex-direction: column;
  gap: 4px;
  color: var(--front-text-muted);
  font-size: 12px;
}

.meta-note {
  color: var(--front-accent-strong);
}

.route-actions,
.dialog-footer {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.detail-layout {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.detail-cover {
  width: 100%;
  height: 240px;
  object-fit: cover;
  border-radius: 20px;
}

.detail-copy {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.timeline {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-top: 4px;
}

.timeline-item {
  display: grid;
  grid-template-columns: 28px minmax(0, 1fr) auto;
  gap: 10px;
  align-items: center;
  padding: 10px 12px;
  border-radius: 16px;
  background: rgba(247, 251, 252, 0.92);
  border: 1px solid var(--front-border);
}

.timeline-index {
  width: 26px;
  height: 26px;
  border-radius: 50%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: rgba(23, 135, 166, 0.14);
  color: var(--front-accent-strong);
  font-weight: 700;
  font-size: 12px;
}

.timeline-copy strong {
  color: var(--front-text);
  font-size: 13px;
}

.timeline-copy p {
  margin: 4px 0 0;
  color: var(--front-text-muted);
  font-size: 12px;
}

@media (max-width: 1280px) {
  .route-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 760px) {
  .route-section {
    padding: 20px 0;
  }

  .section-header,
  .route-actions,
  .dialog-footer {
    flex-direction: column;
    align-items: stretch;
  }

  .section-header {
    flex-direction: row;
    align-items: center;
  }

  .route-grid {
    grid-template-columns: 1fr;
  }

  .timeline-item {
    grid-template-columns: 28px minmax(0, 1fr);
  }
}

@media (max-width: 560px) {
  .section-header h2 {
    font-size: 17px;
  }

  .route-grid {
    gap: 12px;
  }

  .route-card {
    border-radius: 18px;
  }

  .route-cover-wrap {
    height: 150px;
  }

  .route-card-body {
    padding: 12px;
    gap: 10px;
  }

  .detail-cover {
    height: 200px;
    border-radius: 16px;
  }
}
</style>
