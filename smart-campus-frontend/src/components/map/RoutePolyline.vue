<template>
  <div class="route-polyline-container">
    <!-- 路径规划面板 -->
    <div class="route-panel">
      <div class="panel-header">
        <h3>路径规划</h3>
        <el-button
          v-if="routeResult"
          :icon="Close"
          size="small"
          text
          @click="handleClearRoute"
        />
      </div>

      <div class="panel-content">
        <!-- 起点 -->
        <div class="route-point">
          <el-icon class="point-icon start"><LocationFilled /></el-icon>
          <el-input
            v-model="startText"
            placeholder="输入起点"
            size="small"
            @keyup.enter="handleSearchStart"
          />
        </div>

        <!-- 交换按钮 -->
        <div class="swap-btn">
          <el-button
            :icon="Sort"
            circle
            size="small"
            @click="handleSwapPoints"
          />
        </div>

        <!-- 终点 -->
        <div class="route-point">
          <el-icon class="point-icon end"><LocationFilled /></el-icon>
          <el-input
            v-model="endText"
            placeholder="输入终点"
            size="small"
            @keyup.enter="handleSearchEnd"
          />
        </div>

        <!-- 规划按钮 -->
        <el-button
          type="primary"
          style="width: 100%; margin-top: 10px"
          :loading="mapStore.isLoading"
          @click="handlePlanRoute"
        >
          开始规划
        </el-button>
      </div>

      <!-- 路径结果 -->
      <div v-if="routeResult" class="route-result">
        <div class="result-item">
          <span class="label">距离：</span>
          <span class="value">{{ routeResult.distance || '计算中...' }}</span>
        </div>
        <div class="result-item">
          <span class="label">时长：</span>
          <span class="value">{{ routeResult.duration || '计算中...' }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useMapStore } from '@/stores/map'
import { ElMessage } from 'element-plus'
import { LocationFilled, Sort, Close } from '@element-plus/icons-vue'

const mapStore = useMapStore()

const startText = ref('')
const endText = ref('')

const routeResult = computed(() => mapStore.routeResult)

const handleSearchStart = () => {
  // 搜索起点
  ElMessage.info('搜索起点功能待实现')
}

const handleSearchEnd = () => {
  // 搜索终点
  ElMessage.info('搜索终点功能待实现')
}

const handleSwapPoints = () => {
  const temp = startText.value
  startText.value = endText.value
  endText.value = temp

  // 同时交换地图上的起终点
  const tempPoint = mapStore.routeStart
  mapStore.setRouteStart(mapStore.routeEnd)
  mapStore.setRouteEnd(tempPoint)
}

const handlePlanRoute = async () => {
  if (!mapStore.routeStart || !mapStore.routeEnd) {
    ElMessage.warning('请先在地图上选择起点和终点')
    return
  }

  try {
    await mapStore.planRouteAsync()
    ElMessage.success('路径规划成功')
  } catch (error) {
    ElMessage.error(error.message || '路径规划失败')
  }
}

const handleClearRoute = () => {
  mapStore.clearRoute()
  startText.value = ''
  endText.value = ''
}
</script>

<style scoped>
.route-polyline-container {
  position: absolute;
  top: 20px;
  right: 20px;
  z-index: 1000;
}

.route-panel {
  width: 300px;
  background: rgba(255, 255, 255, 0.95);
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.15);
  overflow: hidden;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: #409eff;
  color: #fff;
}

.panel-header h3 {
  margin: 0;
  font-size: 16px;
}

.panel-content {
  padding: 16px;
}

.route-point {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 8px;
}

.point-icon {
  font-size: 20px;
}

.point-icon.start {
  color: #67c23a;
}

.point-icon.end {
  color: #f56c6c;
}

.swap-btn {
  display: flex;
  justify-content: center;
  margin: 8px 0;
}

.route-result {
  padding: 12px 16px;
  border-top: 1px solid #ebeef5;
  background: #f5f7fa;
}

.result-item {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
}

.result-item:last-child {
  margin-bottom: 0;
}

.result-item .label {
  color: #666;
  font-size: 14px;
}

.result-item .value {
  color: #333;
  font-weight: 500;
}
</style>
