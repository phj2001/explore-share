<template>
  <el-dialog
    model-value
    title="申请添加地点"
    :width="560"
    @close="emit('close')"
  >
    <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
      <el-form-item label="地点名称" prop="name">
        <el-input v-model="form.name" placeholder="请输入地点名称" maxlength="100" show-word-limit />
      </el-form-item>

      <el-form-item label="分类" prop="category">
        <el-select v-model="form.category" placeholder="请选择分类" style="width: 100%">
          <el-option
            v-for="cat in poiStore.categories"
            :key="cat"
            :label="cat"
            :value="cat"
          />
        </el-select>
      </el-form-item>

      <el-form-item label="选择位置（点击地图选点）">
        <div ref="miniMapRoot" class="mini-map"></div>
        <div v-if="form.latitude" class="coord-info">
          经度 {{ form.longitude }} , 纬度 {{ form.latitude }}
        </div>
        <div v-else class="coord-hint">请点击上方地图选取位置</div>
      </el-form-item>

      <el-form-item label="地址（选填）">
        <el-input v-model="form.address" placeholder="请输入详细地址" maxlength="255" />
      </el-form-item>

      <el-form-item label="描述（选填）">
        <el-input v-model="form.description" type="textarea" :rows="3" placeholder="请补充地点描述" maxlength="1000" show-word-limit />
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="emit('close')">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="handleSubmit">提交申请</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { submitPOIApplication } from '@/api/poiApplication'
import { usePOIStore } from '@/stores/poi'

const emit = defineEmits(['close'])
const poiStore = usePOIStore()

const formRef = ref(null)
const miniMapRoot = ref(null)
const submitting = ref(false)
let miniMap = null
let marker = null
let AMapLib = null

const form = ref({
  name: '',
  category: '',
  description: '',
  latitude: null,
  longitude: null,
  address: ''
})

const rules = {
  name: [{ required: true, message: '请输入地点名称', trigger: 'blur' }],
  category: [{ required: true, message: '请选择分类', trigger: 'change' }]
}

const initMiniMap = async () => {
  await nextTick()
  if (!miniMapRoot.value) return

  try {
    AMapLib = window.AMap
    if (!AMapLib) return

    miniMap = new AMapLib.Map(miniMapRoot.value, {
      zoom: 15,
      center: [116.397, 39.909],
      resizeEnable: true
    })

    miniMap.on('click', (e) => {
      const lng = e.lnglat.getLng().toFixed(7)
      const lat = e.lnglat.getLat().toFixed(7)
      form.value.longitude = parseFloat(lng)
      form.value.latitude = parseFloat(lat)

      if (marker) miniMap.remove(marker)
      marker = new AMapLib.Marker({
        position: [parseFloat(lng), parseFloat(lat)],
        draggable: true
      })
      marker.on('dragend', (ev) => {
        form.value.longitude = parseFloat(ev.lnglat.getLng().toFixed(7))
        form.value.latitude = parseFloat(ev.lnglat.getLat().toFixed(7))
      })
      miniMap.add(marker)
      miniMap.setCenter([parseFloat(lng), parseFloat(lat)])
    })
  } catch {}
}

const handleSubmit = async () => {
  if (!formRef.value) return
  await formRef.value.validate()

  if (!form.value.latitude || !form.value.longitude) {
    ElMessage.warning('请在地图上选取位置')
    return
  }

  submitting.value = true
  try {
    await submitPOIApplication({
      name: form.value.name,
      category: form.value.category,
      description: form.value.description || undefined,
      latitude: form.value.latitude,
      longitude: form.value.longitude,
      address: form.value.address || undefined
    })
    ElMessage.success('申请已提交，请等待管理员审核')
    emit('close')
  } catch (err) {
    ElMessage.error(err?.message || '提交失败')
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  if (!poiStore.categories?.length) {
    try { await poiStore.fetchCategories() } catch {}
  }
  await initMiniMap()
})

onUnmounted(() => {
  if (miniMap) {
    miniMap.destroy()
    miniMap = null
  }
})
</script>

<style scoped>
.mini-map {
  width: 100%;
  height: 260px;
  border-radius: 12px;
  overflow: hidden;
  border: 1px solid var(--el-border-color-lighter);
}

.coord-info {
  margin-top: 6px;
  font-size: 12px;
  color: var(--front-accent-strong);
}

.coord-hint {
  margin-top: 6px;
  font-size: 12px;
  color: var(--front-text-muted);
}
</style>
