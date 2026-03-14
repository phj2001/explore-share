<template>
  <div class="poi-marker" :class="{ active: isActive }">
    <!-- 标记图标 -->
    <div class="marker-icon" @click="handleClick">
      <el-icon :size="24">
        <LocationFilled />
      </el-icon>
    </div>

    <!-- 标记标签 -->
    <div v-if="showLabel" class="marker-label">
      {{ poi.name }}
    </div>

    <!-- 标记详情弹窗 -->
    <div v-if="showPopup" class="marker-popup">
      <div class="popup-header">
        <strong>{{ poi.name }}</strong>
        <el-button
          :icon="Close"
          size="small"
          text
          @click="handleClosePopup"
        />
      </div>
      <div class="popup-content">
        <p>分类：{{ poi.category }}</p>
        <p>坐标：{{ poi.latitude }}, {{ poi.longitude }}</p>
        <p v-if="poi.description">描述：{{ poi.description }}</p>
      </div>
      <div class="popup-actions">
        <el-button size="small" type="primary" @click="handleNavigate">
          导航到这里
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { LocationFilled, Close } from '@element-plus/icons-vue'

const props = defineProps({
  poi: {
    type: Object,
    required: true
  },
  isActive: {
    type: Boolean,
    default: false
  },
  showLabel: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['click', 'navigate', 'close'])

const showPopup = computed(() => props.isActive)

const handleClick = () => {
  emit('click', props.poi)
}

const handleNavigate = () => {
  emit('navigate', props.poi)
}

const handleClosePopup = () => {
  emit('close', props.poi)
}
</script>

<style scoped>
.poi-marker {
  position: relative;
}

.marker-icon {
  width: 36px;
  height: 36px;
  background: #409eff;
  border-radius: 50% 50% 50% 0;
  transform: rotate(-45deg);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.3);
  transition: all 0.3s;
}

.marker-icon:hover {
  transform: rotate(-45deg) scale(1.1);
  background: #66b1ff;
}

.marker-icon :deep(.el-icon) {
  transform: rotate(45deg);
  color: #fff;
}

.poi-marker.active .marker-icon {
  background: #f56c6c;
  transform: rotate(-45deg) scale(1.2);
}

.marker-label {
  position: absolute;
  top: -30px;
  left: 50%;
  transform: translateX(-50%);
  background: rgba(0, 0, 0, 0.7);
  color: #fff;
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 12px;
  white-space: nowrap;
  pointer-events: none;
}

.marker-popup {
  position: absolute;
  bottom: 50px;
  left: 50%;
  transform: translateX(-50%);
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.15);
  width: 250px;
  z-index: 1000;
}

.popup-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid #ebeef5;
}

.popup-header strong {
  font-size: 16px;
  color: #333;
}

.popup-content {
  padding: 12px 16px;
}

.popup-content p {
  margin: 6px 0;
  font-size: 14px;
  color: #666;
}

.popup-actions {
  padding: 12px 16px;
  border-top: 1px solid #ebeef5;
  display: flex;
  justify-content: flex-end;
}
</style>
