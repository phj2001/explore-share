<template>
  <el-tooltip v-if="isCollapsed" content="展开路线面板" placement="left">
    <button type="button" class="route-trigger" @click="handleExpandPanel">
      <el-icon><Guide /></el-icon>
    </button>
  </el-tooltip>

  <div v-else class="route-panel" :class="{ 'has-route': routeResult?.steps?.length }">
    <div class="panel-header">
      <div>
        <h3>{{ currentModeLabel }}路线规划</h3>
        <p>{{ panelDescription }}</p>
      </div>
      <div class="header-actions">
        <el-button :icon="Minus" text @click="handleCollapsePanel" />
        <el-button v-if="routeStart || routeEnd || routeResult" :icon="Delete" text @click="handleClearRoute" />
      </div>
    </div>

    <div class="panel-body">
      <div v-if="activeRecommendedRoute" class="recommended-banner">
        <div class="banner-copy">
          <span class="banner-kicker">推荐路线</span>
          <strong>{{ activeRecommendedRoute.title }}</strong>
          <p>
            {{ activeRecommendedRoute.waypointCount }} 个地点
            <template v-if="activeRecommendedRoute.recommendationText">
              · {{ activeRecommendedRoute.recommendationText }}
            </template>
          </p>
        </div>
      </div>

      <el-segmented :model-value="routeMode" :options="modeOptions" size="default" @change="handleModeChange" />

      <div v-if="isPickingRoutePoint" class="pick-banner">
        <span>请在地图中点击，设置{{ routePickMode === 'start' ? '起点' : '终点' }}。</span>
        <el-button text @click="handleCancelPickMode">取消</el-button>
      </div>

      <div class="route-point">
        <button
          type="button"
          class="point-tag point-tag-button start"
          :class="{ 'is-active': routePickMode === 'start' }"
          @click="handlePickPoint('start')"
        >
          起
        </button>
        <div class="point-text">
          <strong>{{ routeStart?.name || '未设置起点' }}</strong>
          <span v-if="routeStart">{{ formatPoint(routeStart) }}</span>
        </div>
      </div>

      <div class="route-actions">
        <el-button :icon="Sort" circle size="small" :disabled="!routeStart || !routeEnd" @click="handleSwapPoints" />
        <el-button type="primary" size="small" :loading="mapStore.isLoading" :disabled="!routeStart || !routeEnd" @click="handlePlanRoute">
          更新路线
        </el-button>
      </div>

      <div class="route-point">
        <button
          type="button"
          class="point-tag point-tag-button end"
          :class="{ 'is-active': routePickMode === 'end' }"
          @click="handlePickPoint('end')"
        >
          终
        </button>
        <div class="point-text">
          <strong>{{ routeEnd?.name || '未设置终点' }}</strong>
          <span v-if="routeEnd">{{ formatPoint(routeEnd) }}</span>
        </div>
      </div>

      <div v-if="hasIntermediatePoints" class="waypoint-list">
        <div class="waypoint-head">
          <span>途经地点</span>
          <em>{{ routeIntermediatePoints.length }} 个</em>
        </div>
        <div class="waypoint-chip-list">
          <span v-for="point in routeIntermediatePoints" :key="`${point.poiId}-${point.name}`" class="waypoint-chip">
            {{ point.name }}
          </span>
        </div>
      </div>
    </div>

    <div v-if="routeResult" class="result-summary">
      <div class="summary-item">
        <span>出行方式</span>
        <strong>{{ routeResult.modeLabel || currentModeLabel }}</strong>
      </div>
      <div class="summary-item">
        <span>总距离</span>
        <strong>{{ routeResult.distanceText }}</strong>
      </div>
      <div class="summary-item">
        <span>预计耗时</span>
        <strong>{{ routeResult.durationText }}</strong>
      </div>
    </div>

    <el-scrollbar v-if="routeResult?.steps?.length" class="step-list">
      <div v-for="(step, index) in routeResult.steps" :key="`${routeResult.mode}-${index}-${step.instruction}`" class="step-item">
        <div class="step-index">{{ index + 1 }}</div>
        <div class="step-content">
          <p>{{ step.instruction || '沿当前道路继续前进。' }}</p>
          <span>{{ formatStepMeta(step.distanceMeters, step.durationSeconds) }}</span>
        </div>
      </div>
    </el-scrollbar>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Delete, Guide, Minus, Sort } from '@element-plus/icons-vue'
import { useMapStore } from '@/stores/map'

const mapStore = useMapStore()
const isCollapsed = ref(true)

const routeStart = computed(() => mapStore.routeStart)
const routeEnd = computed(() => mapStore.routeEnd)
const routeResult = computed(() => mapStore.routeResult)
const routeMode = computed(() => mapStore.routeMode)
const routePickMode = computed(() => mapStore.routePickMode)
const isPickingRoutePoint = computed(() => mapStore.isPickingRoutePoint)
const routeIntermediatePoints = computed(() => mapStore.routeIntermediatePoints)
const hasIntermediatePoints = computed(() => mapStore.hasIntermediatePoints)
const activeRecommendedRoute = computed(() => mapStore.activeRecommendedRoute)
const modeOptions = computed(() => mapStore.routeModes.map((mode) => ({ label: mode.label, value: mode.value })))

const currentModeLabel = computed(() => {
  return mapStore.routeModes.find((mode) => mode.value === mapStore.routeMode)?.label || '步行'
})

const panelDescription = computed(() => {
  if (routePickMode.value === 'start') {
    return '地图点击模式：正在选择临时起点。'
  }

  if (routePickMode.value === 'end') {
    return '地图点击模式：正在选择临时终点。'
  }

  if (activeRecommendedRoute.value) {
    return '推荐路线已接管当前路径，你可以切换出行方式，系统会按地点顺序重新规划。'
  }

  return '可从地点详情中设置起终点，也可以直接在地图上临时选点。'
})

const formatPoint = (point) => {
  const lat = Number(point.rawLat ?? point.lat)
  const lng = Number(point.rawLng ?? point.lng)
  return `${lat.toFixed(6)}, ${lng.toFixed(6)}`
}

const formatStepMeta = (distanceMeters = 0, durationSeconds = 0) => {
  const distance = distanceMeters >= 1000 ? `${(distanceMeters / 1000).toFixed(1)} 公里` : `${distanceMeters} 米`
  const minutes = Math.max(Math.ceil(durationSeconds / 60), 1)
  return `${distance} · ${minutes} 分钟`
}

const handlePickPoint = (mode) => {
  if (routePickMode.value === mode) {
    mapStore.cancelPickingRoutePoint()
    return
  }

  mapStore.startPickingRoutePoint(mode)
  ElMessage.info(`请在地图中点击设置${mode === 'start' ? '起点' : '终点'}`)
}

const handleCancelPickMode = () => {
  mapStore.cancelPickingRoutePoint()
}

const handleSwapPoints = async () => {
  mapStore.swapRoutePoints()
  if (routeStart.value && routeEnd.value && routeResult.value) {
    await handlePlanRoute()
  }
}

const handlePlanRoute = async () => {
  try {
    await mapStore.planRouteAsync()
    ElMessage.success('路线已更新')
  } catch (error) {
    ElMessage.error(error.message || '路线规划失败')
  }
}

const handleModeChange = async (mode) => {
  mapStore.setRouteMode(mode)
  if (routeStart.value && routeEnd.value) {
    await handlePlanRoute()
  }
}

const handleClearRoute = () => {
  mapStore.clearRoute()
}

const handleExpandPanel = () => {
  isCollapsed.value = false
}

const handleCollapsePanel = () => {
  isCollapsed.value = true
}
</script>

<style scoped>
.route-trigger {
  position: absolute;
  top: 20px;
  right: 20px;
  z-index: 1000;
  width: 56px;
  height: 56px;
  border: none;
  border-radius: 999px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #0f766e, #0ea5e9);
  color: #fff;
  box-shadow: 0 12px 28px rgba(15, 23, 42, 0.22);
  cursor: pointer;
  transition: transform 0.18s ease, box-shadow 0.18s ease;
}

.route-trigger:hover {
  transform: translateY(-1px) scale(1.02);
  box-shadow: 0 16px 32px rgba(15, 23, 42, 0.26);
}

.route-trigger .el-icon {
  font-size: 24px;
}

.route-panel {
  position: absolute;
  top: 20px;
  right: 20px;
  z-index: 1000;
  width: 360px;
  max-height: calc(100vh - 120px);
  display: flex;
  flex-direction: column;
  background: rgba(255, 255, 255, 0.96);
  border-radius: 16px;
  box-shadow: 0 12px 32px rgba(15, 23, 42, 0.18);
  overflow: hidden;
  backdrop-filter: blur(8px);
}

.route-panel.has-route {
  height: calc(100vh - 120px);
}

.panel-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  padding: 16px 18px 12px;
  background: linear-gradient(135deg, #0f766e, #0ea5e9);
  color: #fff;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 4px;
}

.panel-header h3 {
  margin: 0;
  font-size: 18px;
}

.panel-header p {
  margin: 4px 0 0;
  font-size: 12px;
  opacity: 0.9;
  line-height: 1.6;
}

.panel-body {
  padding: 16px 18px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.recommended-banner,
.pick-banner,
.waypoint-list {
  padding: 12px 14px;
  border-radius: 14px;
}

.recommended-banner {
  background: linear-gradient(135deg, rgba(14, 165, 233, 0.12), rgba(16, 185, 129, 0.12));
  border: 1px solid rgba(14, 165, 233, 0.18);
}

.banner-copy {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.banner-kicker {
  font-size: 12px;
  color: #0369a1;
  font-weight: 700;
}

.banner-copy strong {
  color: #0f172a;
  font-size: 15px;
}

.banner-copy p {
  margin: 0;
  color: #475569;
  font-size: 12px;
  line-height: 1.6;
}

.pick-banner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  background: #ecfeff;
  color: #0f766e;
  font-size: 12px;
}

.route-point {
  display: flex;
  gap: 12px;
  align-items: center;
}

.point-tag {
  width: 28px;
  height: 28px;
  border-radius: 999px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 12px;
  font-weight: 700;
  flex-shrink: 0;
}

.point-tag-button {
  border: none;
  cursor: pointer;
  transition: transform 0.18s ease, box-shadow 0.18s ease, opacity 0.18s ease;
}

.point-tag-button:hover {
  transform: translateY(-1px);
  box-shadow: 0 8px 18px rgba(15, 23, 42, 0.18);
}

.point-tag-button.is-active {
  box-shadow: 0 0 0 3px rgba(14, 165, 233, 0.22);
}

.point-tag.start {
  background: #16a34a;
}

.point-tag.end {
  background: #dc2626;
}

.point-text {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.point-text strong {
  color: #0f172a;
  font-size: 14px;
}

.point-text span {
  color: #64748b;
  font-size: 12px;
}

.route-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.waypoint-list {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
}

.waypoint-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
  color: #475569;
  font-size: 12px;
}

.waypoint-head em {
  font-style: normal;
}

.waypoint-chip-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.waypoint-chip {
  padding: 6px 10px;
  border-radius: 999px;
  background: rgba(224, 242, 254, 0.95);
  color: #0369a1;
  font-size: 12px;
}

.result-summary {
  margin: 0 18px 16px;
  padding: 12px 14px;
  border-radius: 12px;
  background: #f8fafc;
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
}

.summary-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.summary-item span {
  color: #64748b;
  font-size: 12px;
}

.summary-item strong {
  color: #0f172a;
  font-size: 14px;
}

.step-list {
  flex: 1 1 auto;
  min-height: 0;
  padding: 0 18px 18px;
}

.step-list :deep(.el-scrollbar__wrap) {
  overflow-y: auto;
}

.step-list :deep(.el-scrollbar__view) {
  padding-bottom: 18px;
}

.step-item {
  display: flex;
  gap: 12px;
  padding: 12px 0;
  border-top: 1px solid #e2e8f0;
}

.step-index {
  width: 26px;
  height: 26px;
  border-radius: 999px;
  background: #e0f2fe;
  color: #0369a1;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 700;
  flex-shrink: 0;
}

.step-content p {
  margin: 0;
  color: #1e293b;
  line-height: 1.5;
  font-size: 13px;
}

.step-content span {
  display: inline-block;
  margin-top: 4px;
  color: #64748b;
  font-size: 12px;
}

@media (max-width: 768px) {
  .route-trigger {
    top: auto;
    right: 12px;
    bottom: 12px;
  }

  .route-panel {
    left: 12px;
    right: 12px;
    top: auto;
    bottom: 12px;
    width: auto;
    max-height: 58vh;
  }

  .route-panel.has-route {
    height: 58vh;
  }

  .pick-banner {
    flex-direction: column;
    align-items: flex-start;
  }

  .result-summary {
    grid-template-columns: 1fr;
  }
}
</style>
