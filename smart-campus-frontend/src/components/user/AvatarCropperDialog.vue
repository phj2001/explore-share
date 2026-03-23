<template>
  <el-dialog
    :model-value="visible"
    title="裁剪头像"
    width="560px"
    destroy-on-close
    @close="emit('cancel')"
    @closed="handleClose"
  >
    <div class="cropper-panel">
      <div
        ref="cropperRef"
        class="cropper-stage"
        @pointerdown="handlePointerDown"
      >
        <img
          v-if="imageUrl"
          :src="imageUrl"
          alt="待裁剪头像"
          class="cropper-image"
          :style="imageStyle"
          draggable="false"
        />
        <div class="cropper-overlay"></div>
        <div class="cropper-frame"></div>
      </div>

      <div class="preview-block">
        <span class="preview-label">预览</span>
        <div class="preview-circle">
          <canvas ref="previewCanvasRef" width="180" height="180"></canvas>
        </div>
      </div>
    </div>

    <div class="slider-row">
      <span>缩放</span>
      <el-slider v-model="scale" :min="1" :max="3" :step="0.01" @input="renderPreview" />
    </div>

    <template #footer>
      <el-button @click="emit('cancel')">取消</el-button>
      <el-button type="primary" @click="confirmCrop">确认裁剪</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, ref, watch } from 'vue'

const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  },
  imageUrl: {
    type: String,
    default: ''
  }
})

const emit = defineEmits(['cancel', 'confirm'])

const cropperRef = ref(null)
const previewCanvasRef = ref(null)
const scale = ref(1)
const imageWidth = ref(0)
const imageHeight = ref(0)
const offsetX = ref(0)
const offsetY = ref(0)
const imageLoaded = ref(false)

const dragging = {
  active: false,
  startX: 0,
  startY: 0,
  originX: 0,
  originY: 0
}

const imageStyle = computed(() => ({
  width: `${imageWidth.value * scale.value}px`,
  height: `${imageHeight.value * scale.value}px`,
  transform: `translate(${offsetX.value}px, ${offsetY.value}px)`
}))

watch(
  () => props.visible,
  async (visible) => {
    if (!visible || !props.imageUrl) {
      return
    }

    await initializeImage()
  }
)

watch(
  () => props.imageUrl,
  async (value) => {
    if (!props.visible || !value) {
      return
    }

    await initializeImage()
  }
)

watch(scale, () => {
  clampOffsets()
  renderPreview()
})

onBeforeUnmount(() => {
  removePointerListeners()
})

const initializeImage = async () => {
  const image = await loadImage(props.imageUrl)
  await nextTick()

  const stageSize = getStageSize()
  if (!stageSize) {
    return
  }

  const ratio = Math.max(stageSize / image.width, stageSize / image.height)
  imageWidth.value = image.width * ratio
  imageHeight.value = image.height * ratio
  scale.value = 1
  offsetX.value = (stageSize - imageWidth.value) / 2
  offsetY.value = (stageSize - imageHeight.value) / 2
  imageLoaded.value = true
  renderPreview()
}

const loadImage = (url) =>
  new Promise((resolve, reject) => {
    const image = new Image()
    image.onload = () => resolve(image)
    image.onerror = reject
    image.src = url
  })

const getStageSize = () => {
  return cropperRef.value?.clientWidth || 0
}

const clampOffsets = () => {
  const stageSize = getStageSize()
  if (!stageSize || !imageLoaded.value) {
    return
  }

  const scaledWidth = imageWidth.value * scale.value
  const scaledHeight = imageHeight.value * scale.value
  const minX = Math.min(0, stageSize - scaledWidth)
  const minY = Math.min(0, stageSize - scaledHeight)

  offsetX.value = Math.min(0, Math.max(minX, offsetX.value))
  offsetY.value = Math.min(0, Math.max(minY, offsetY.value))
}

const drawToCanvas = (context, image, size) => {
  const stageSize = getStageSize()
  if (!stageSize) {
    return
  }

  const scaleFactor = size / stageSize
  context.clearRect(0, 0, size, size)
  context.save()
  context.beginPath()
  context.arc(size / 2, size / 2, size / 2, 0, Math.PI * 2)
  context.clip()
  context.drawImage(
    image,
    offsetX.value * scaleFactor,
    offsetY.value * scaleFactor,
    imageWidth.value * scale.value * scaleFactor,
    imageHeight.value * scale.value * scaleFactor
  )
  context.restore()
}

const renderPreview = () => {
  const canvas = previewCanvasRef.value
  if (!canvas || !imageLoaded.value || !props.imageUrl) {
    return
  }

  const context = canvas.getContext('2d')
  const previewImage = new Image()
  previewImage.onload = () => drawToCanvas(context, previewImage, canvas.width)
  previewImage.src = props.imageUrl
}

const handlePointerDown = (event) => {
  if (!imageLoaded.value) {
    return
  }

  dragging.active = true
  dragging.startX = event.clientX
  dragging.startY = event.clientY
  dragging.originX = offsetX.value
  dragging.originY = offsetY.value

  window.addEventListener('pointermove', handlePointerMove)
  window.addEventListener('pointerup', handlePointerUp)
}

const handlePointerMove = (event) => {
  if (!dragging.active) {
    return
  }

  offsetX.value = dragging.originX + event.clientX - dragging.startX
  offsetY.value = dragging.originY + event.clientY - dragging.startY
  clampOffsets()
  renderPreview()
}

const handlePointerUp = () => {
  dragging.active = false
  removePointerListeners()
}

const removePointerListeners = () => {
  window.removeEventListener('pointermove', handlePointerMove)
  window.removeEventListener('pointerup', handlePointerUp)
}

const confirmCrop = () => {
  const stageSize = getStageSize()
  if (!stageSize || !props.imageUrl || !imageLoaded.value) {
    return
  }

  const canvas = document.createElement('canvas')
  const size = 400
  canvas.width = size
  canvas.height = size

  const outputImage = new Image()
  outputImage.onload = () => {
    const context = canvas.getContext('2d')
    drawToCanvas(context, outputImage, size)
    canvas.toBlob((blob) => {
      if (!blob) {
        return
      }

      emit('confirm', new File([blob], 'avatar.png', { type: 'image/png' }))
    }, 'image/png')
  }
  outputImage.src = props.imageUrl
}

const handleClose = () => {
  imageLoaded.value = false
  removePointerListeners()
}
</script>

<style scoped>
.cropper-panel {
  display: flex;
  gap: 24px;
  align-items: center;
  justify-content: center;
}

.cropper-stage {
  position: relative;
  width: 320px;
  height: 320px;
  border-radius: 24px;
  overflow: hidden;
  background: linear-gradient(135deg, #dbeafe, #eff6ff);
  cursor: grab;
  touch-action: none;
}

.cropper-stage:active {
  cursor: grabbing;
}

.cropper-image {
  position: absolute;
  top: 0;
  left: 0;
  user-select: none;
  pointer-events: none;
  max-width: none;
}

.cropper-overlay {
  position: absolute;
  inset: 0;
  background:
    linear-gradient(rgba(15, 23, 42, 0.25), rgba(15, 23, 42, 0.25)),
    radial-gradient(circle at center, transparent 0 108px, rgba(15, 23, 42, 0.55) 109px);
  pointer-events: none;
}

.cropper-frame {
  position: absolute;
  inset: 52px;
  border-radius: 50%;
  border: 2px solid rgba(255, 255, 255, 0.95);
  box-shadow: 0 0 0 999px rgba(15, 23, 42, 0.08);
  pointer-events: none;
}

.preview-block {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}

.preview-label {
  font-size: 14px;
  color: #475569;
}

.preview-circle {
  width: 180px;
  height: 180px;
  border-radius: 50%;
  overflow: hidden;
  border: 6px solid #fff;
  box-shadow: 0 20px 40px rgba(15, 23, 42, 0.12);
  background: #f8fafc;
}

.slider-row {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-top: 20px;
}

.slider-row span {
  color: #475569;
  white-space: nowrap;
}

@media (max-width: 720px) {
  .cropper-panel {
    flex-direction: column;
  }

  .cropper-stage {
    width: min(320px, 100%);
    height: min(320px, calc(100vw - 96px));
  }
}
</style>
