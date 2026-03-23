<template>
  <el-tooltip v-if="isCollapsed" content="点击进行导航" placement="left">
    <button type="button" class="route-trigger" @click="handleExpandPanel">
      <el-icon><Guide /></el-icon>
    </button>
  </el-tooltip>

  <div v-else class="route-panel" :class="{ 'has-route': routeResult?.steps?.length }">
    <div class="panel-header">
      <div>
        <h3>{{ currentModeLabel }} Route</h3>
        <p>{{ panelDescription }}</p>
      </div>
      <div class="header-actions">
        <el-button :icon="Minus" text @click="handleCollapsePanel" />
        <el-button
          v-if="routeStart || routeEnd || routeResult"
          :icon="Delete"
          text
          @click="handleClearRoute"
        />
      </div>
    </div>

    <div class="panel-body">
      <el-segmented
        :model-value="routeMode"
        :options="modeOptions"
        size="default"
        @change="handleModeChange"
      />

      <div v-if="isPickingRoutePoint" class="pick-banner">
        <span>Click on the map to set {{ routePickMode === 'start' ? 'the start point' : 'the end point' }}.</span>
        <el-button text @click="handleCancelPickMode">Cancel</el-button>
      </div>

      <div class="route-point">
        <button
          type="button"
          class="point-tag point-tag-button start"
          :class="{ 'is-active': routePickMode === 'start' }"
          @click="handlePickPoint('start')"
        >
          S
        </button>
        <div class="point-text">
          <strong>{{ routeStart?.name || 'No start point' }}</strong>
          <span v-if="routeStart">{{ formatPoint(routeStart) }}</span>
        </div>
      </div>

      <div class="route-actions">
        <el-button
          :icon="Sort"
          circle
          size="small"
          :disabled="!routeStart || !routeEnd"
          @click="handleSwapPoints"
        />
        <el-button
          type="primary"
          size="small"
          :loading="mapStore.isLoading"
          :disabled="!routeStart || !routeEnd"
          @click="handlePlanRoute"
        >
          Plan Route
        </el-button>
      </div>

      <div class="route-point">
        <button
          type="button"
          class="point-tag point-tag-button end"
          :class="{ 'is-active': routePickMode === 'end' }"
          @click="handlePickPoint('end')"
        >
          E
        </button>
        <div class="point-text">
          <strong>{{ routeEnd?.name || 'No end point' }}</strong>
          <span v-if="routeEnd">{{ formatPoint(routeEnd) }}</span>
        </div>
      </div>
    </div>

    <div v-if="routeResult" class="result-summary">
      <div class="summary-item">
        <span>Mode</span>
        <strong>{{ routeResult.modeLabel || currentModeLabel }}</strong>
      </div>
      <div class="summary-item">
        <span>Distance</span>
        <strong>{{ routeResult.distanceText }}</strong>
      </div>
      <div class="summary-item">
        <span>ETA</span>
        <strong>{{ routeResult.durationText }}</strong>
      </div>
    </div>

    <el-scrollbar v-if="routeResult?.steps?.length" class="step-list">
      <div
        v-for="(step, index) in routeResult.steps"
        :key="`${routeResult.mode}-${index}-${step.instruction}`"
        class="step-item"
      >
        <div class="step-index">{{ index + 1 }}</div>
        <div class="step-content">
          <p>{{ step.instruction || 'Continue along the current road.' }}</p>
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
const modeOptions = computed(() =>
  mapStore.routeModes.map((mode) => ({ label: mode.label, value: mode.value }))
)

const currentModeLabel = computed(() => {
  return mapStore.routeModes.find((mode) => mode.value === mapStore.routeMode)?.label || 'Walk'
})

const panelDescription = computed(() => {
  if (routePickMode.value === 'start') {
    return 'Map click mode: choose a temporary start point.'
  }

  if (routePickMode.value === 'end') {
    return 'Map click mode: choose a temporary end point.'
  }

  return 'Pick points from a POI dialog or directly on the map.'
})

const formatPoint = (point) => {
  const lat = Number(point.rawLat ?? point.lat)
  const lng = Number(point.rawLng ?? point.lng)
  return `${lat.toFixed(6)}, ${lng.toFixed(6)}`
}

const formatStepMeta = (distanceMeters = 0, durationSeconds = 0) => {
  const distance =
    distanceMeters >= 1000
      ? `${(distanceMeters / 1000).toFixed(1)} km`
      : `${distanceMeters} m`
  const minutes = Math.max(Math.ceil(durationSeconds / 60), 1)
  return `${distance} · ${minutes} min`
}

const handlePickPoint = (mode) => {
  if (routePickMode.value === mode) {
    mapStore.cancelPickingRoutePoint()
    return
  }

  mapStore.startPickingRoutePoint(mode)
  ElMessage.info(`Click on the map to set the ${mode} point`)
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
    ElMessage.success(`${currentModeLabel.value} route updated`)
  } catch (error) {
    ElMessage.error(error.message || 'Failed to plan route')
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
}

.panel-body {
  padding: 16px 18px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.pick-banner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 10px 12px;
  border-radius: 12px;
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
    max-height: 52vh;
  }

  .route-panel.has-route {
    height: 52vh;
  }

  .pick-banner {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
