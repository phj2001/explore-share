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

    <el-dialog v-model="dialogVisible" title="推荐路线详情" width="min(92vw, 760px)" destroy-on-close>
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

<style scoped lang="scss">
/* ── RecommendedRouteSection 新设计系统 ── */
.route-section {
  padding: 48px 0;
  display: flex;
  flex-direction: column;
  gap: 0;
}

.section-header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  padding-bottom: 24px;
  border-bottom: 1px solid var(--front-border);
  margin-bottom: 28px;
}

.head-left {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.section-kicker {
  display: inline-flex;
  padding: 3px 10px;
  border-radius: 999px;
  background: rgba(31, 140, 105, 0.10);
  color: var(--forest-700);
  font-family: var(--font-mono);
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.14em;
  text-transform: uppercase;
  width: fit-content;
}

.section-header h2 {
  margin: 0;
  font-family: var(--font-serif);
  font-size: 24px;
  font-weight: 700;
  color: var(--ink-900);
  letter-spacing: -0.02em;
  line-height: 1.25;
}

/* 刷新按钮 */
.refresh-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  border: 1px solid var(--front-border);
  background: transparent;
  color: var(--ink-500);
  cursor: pointer;
  flex-shrink: 0;
  transition: background 0.2s, color 0.2s, border-color 0.2s;
}

.refresh-btn:hover:not(:disabled) {
  background: var(--forest-50);
  color: var(--forest-700);
  border-color: var(--forest-700);
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

/* 路线网格 */
.route-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 20px;
}

/* 路线卡片 */
.route-card {
  overflow: hidden;
  border-radius: 14px;
  border: 1px solid var(--front-border);
  background: #fff;
  box-shadow: var(--front-shadow);
  transition: border-color 0.2s, transform 0.2s, box-shadow 0.2s;
}

.route-card:hover {
  border-color: var(--forest-500);
  transform: translateY(-2px);
  box-shadow: 0 4px 20px rgba(20, 80, 55, 0.12);
}

/* 封面 */
.route-cover-wrap {
  position: relative;
  height: 180px;
  overflow: hidden;
}

.route-cover {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.route-cover-fallback {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  background: linear-gradient(135deg, var(--forest-800), var(--forest-600));
  color: rgba(255, 255, 255, 0.9);
  font-family: var(--font-mono);
  font-size: 13px;
  font-weight: 600;
}

/* 封面信息覆盖层 */
.route-glance {
  position: absolute;
  left: 10px;
  right: 10px;
  bottom: 10px;
  display: flex;
  flex-direction: column;
  gap: 3px;
  padding: 8px 10px;
  border-radius: 10px;
  background: rgba(0, 0, 0, 0.58);
  color: #fff;
  backdrop-filter: blur(8px);
  font-family: var(--font-mono);
  font-size: 10.5px;
}

/* 卡片主体 */
.route-card-body {
  padding: 16px 18px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

/* 徽章 */
.route-badges {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.badge {
  display: inline-flex;
  padding: 3px 9px;
  border-radius: 999px;
  background: var(--paper-100);
  color: var(--ink-500);
  font-family: var(--font-mono);
  font-size: 10.5px;
  font-weight: 500;
}

.badge-mode {
  background: rgba(31, 140, 105, 0.10);
  color: var(--forest-700);
}

/* 标题 */
.route-card h3,
.detail-copy h3 {
  margin: 0;
  font-family: var(--font-serif);
  font-size: 16px;
  font-weight: 700;
  color: var(--ink-900);
  letter-spacing: -0.01em;
  line-height: 1.3;
}

/* 摘要 & 描述 */
.route-summary,
.route-description {
  margin: 0;
  font-family: var(--font-sans);
  font-size: 12.5px;
  color: var(--ink-600);
  line-height: 1.65;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.route-description {
  -webkit-line-clamp: unset;
}

/* 元信息 */
.route-meta {
  display: flex;
  flex-direction: column;
  gap: 4px;
  font-family: var(--font-mono);
  font-size: 11px;
  color: var(--ink-400);
}

.meta-note {
  color: var(--forest-700);
  font-style: italic;
}

/* 操作按钮 */
.route-actions,
.dialog-footer {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

/* 详情弹窗布局 */
.detail-layout {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.detail-cover {
  width: 100%;
  height: 240px;
  object-fit: cover;
  border-radius: 12px;
}

.detail-copy {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

/* 路线点时间轴 */
.timeline {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 4px;
}

.timeline-item {
  display: grid;
  grid-template-columns: 28px minmax(0, 1fr) auto;
  gap: 10px;
  align-items: center;
  padding: 10px 14px;
  border-radius: 10px;
  background: var(--paper-50);
  border: 1px solid var(--front-border);
  transition: border-color 0.15s;
}

.timeline-item:hover {
  border-color: var(--forest-500);
}

.timeline-index {
  width: 26px;
  height: 26px;
  border-radius: 50%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: rgba(31, 140, 105, 0.12);
  color: var(--forest-700);
  font-family: var(--font-mono);
  font-weight: 700;
  font-size: 12px;
}

.timeline-copy strong {
  display: block;
  font-family: var(--font-sans);
  font-size: 13px;
  font-weight: 600;
  color: var(--ink-900);
}

.timeline-copy p {
  margin: 3px 0 0;
  font-family: var(--font-mono);
  font-size: 11px;
  color: var(--ink-400);
}

/* 响应式 */
@include respond-to(xl) {
  .route-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@include respond-to(md) {
  .route-section {
    padding: 32px 0;
  }

  .route-grid {
    grid-template-columns: 1fr;
  }

  /* 两列布局下"查看地点"按钮换到第二行右列，不与文字挤同一列 */
  .timeline-item {
    grid-template-columns: 28px minmax(0, 1fr);

    :deep(.el-button) {
      grid-column: 2;
      justify-self: start;
      margin-left: 0;
    }
  }

  .route-actions,
  .dialog-footer {
    flex-direction: column;
    align-items: stretch;
  }
}

@include respond-to(sm) {
  .section-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 16px;
    padding-bottom: 16px;
    margin-bottom: 20px;
  }

  .section-header h2 {
    font-size: 20px;
  }

  .route-grid {
    gap: 12px;
  }

  .route-cover-wrap {
    height: 150px;
  }

  .route-card-body {
    padding: 12px 14px;
    gap: 8px;
  }

  .detail-cover {
    height: 200px;
    border-radius: 10px;
  }
}

/* 触屏：刷新圆钮与操作按钮热区 ≥40px（primary 核心操作 44px） */
@include coarse-pointer {
  .refresh-btn {
    width: 40px;
    height: 40px;
  }

  .route-actions :deep(.el-button),
  .dialog-footer :deep(.el-button),
  .timeline-item :deep(.el-button) {
    min-height: 40px;
  }

  .route-actions :deep(.el-button--primary),
  .dialog-footer :deep(.el-button--primary) {
    min-height: 44px;
  }
}
</style>
