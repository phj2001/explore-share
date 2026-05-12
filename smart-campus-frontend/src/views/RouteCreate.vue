<template>
  <div class="route-create-page front-page">
    <Header />

    <main class="create-main">
      <div class="front-shell create-shell">
        <section class="create-card front-panel">
          <div class="card-head">
            <div>
              <span class="front-kicker">路线规划</span>
              <h2>创建新路线</h2>
            </div>
          </div>

          <el-form ref="formRef" :model="form" :rules="rules" label-position="top" class="create-form">
            <el-form-item label="路线标题" prop="title">
              <el-input v-model="form.title" maxlength="100" show-word-limit placeholder="给路线起个名字" />
            </el-form-item>

            <el-form-item label="路线摘要" prop="summary">
              <el-input v-model="form.summary" maxlength="200" show-word-limit placeholder="一句话描述路线亮点" />
            </el-form-item>

            <el-form-item label="详细描述">
              <el-input v-model="form.description" type="textarea" :rows="4" placeholder="路线的详细说明" />
            </el-form-item>

            <el-form-item label="出行方式">
              <el-radio-group v-model="form.defaultMode">
                <el-radio value="walking">步行</el-radio>
                <el-radio value="cycling">骑行</el-radio>
                <el-radio value="driving">驾车</el-radio>
              </el-radio-group>
            </el-form-item>

            <div class="waypoints-section">
              <div class="waypoints-head">
                <h3>途经点</h3>
                <el-button type="primary" size="small" @click="addWaypoint">添加途经点</el-button>
              </div>

              <div v-for="(wp, idx) in form.waypoints" :key="idx" class="waypoint-row">
                <span class="wp-index">{{ idx + 1 }}</span>
                <el-input v-model="wp.waypointName" placeholder="站点名称" class="wp-input" />
                <el-input v-model.number="wp.latitude" placeholder="纬度" class="wp-coord" />
                <el-input v-model.number="wp.longitude" placeholder="经度" class="wp-coord" />
                <el-select v-model="wp.poiId" placeholder="关联POI" clearable class="wp-poi">
                  <el-option v-for="poi in pois" :key="poi.id" :label="poi.name" :value="poi.id" />
                </el-select>
                <el-button text type="danger" @click="removeWaypoint(idx)">删除</el-button>
              </div>

              <el-empty v-if="!form.waypoints.length" description="请添加至少一个途经点" :image-size="60" />
            </div>
          </el-form>

          <div class="form-actions">
            <el-button @click="router.back()">取消</el-button>
            <el-button type="primary" :loading="submitting" @click="handleSubmit">发布路线</el-button>
          </div>
        </section>
      </div>
    </main>

    <Footer />
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import Header from '@/components/common/Header.vue'
import Footer from '@/components/common/Footer.vue'
import { createRoute } from '@/api/userRoute'
import { getPOIOptions } from '@/api/poi'

const router = useRouter()
const formRef = ref(null)
const submitting = ref(false)
const pois = ref([])

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

const addWaypoint = () => {
  form.waypoints.push({
    poiId: null,
    latitude: null,
    longitude: null,
    waypointName: '',
    sortOrder: form.waypoints.length
  })
}

const removeWaypoint = (idx) => {
  form.waypoints.splice(idx, 1)
  form.waypoints.forEach((wp, i) => { wp.sortOrder = i })
}

const loadPois = async () => {
  try {
    const data = await getPOIOptions({ limit: 200 })
    pois.value = data || []
  } catch {
    // 静默
  }
}

const handleSubmit = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  if (!form.waypoints.length) {
    ElMessage.warning('请添加至少一个途经点')
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
    ElMessage.success('路线创建成功')
    router.push('/route/' + result.id)
  } catch (error) {
    ElMessage.error(error.message || '创建失败')
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  loadPois()
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
  max-width: 800px;
  margin: 0 auto;
}

.create-card {
  padding: 26px;
  border-radius: 28px;
}

.card-head {
  margin-bottom: 20px;
}

.card-head h2 {
  margin: 10px 0 0;
  color: #0f172a;
  font-size: 22px;
}

.create-form :deep(.el-form-item__label) {
  font-weight: 600;
  color: #334155;
}

.waypoints-section {
  margin-top: 8px;
}

.waypoints-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
}

.waypoints-head h3 {
  margin: 0;
  font-size: 16px;
  color: #0f172a;
}

.waypoint-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
  flex-wrap: wrap;
}

.wp-index {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: #eff6ff;
  color: #2563eb;
  font-size: 13px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.wp-input {
  flex: 1;
  min-width: 120px;
}

.wp-coord {
  width: 110px;
}

.wp-poi {
  width: 160px;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding-top: 20px;
  border-top: 1px solid #e2e8f0;
  margin-top: 20px;
}

@media (max-width: 640px) {
  .create-card {
    padding: 18px;
    border-radius: 24px;
  }

  .waypoint-row {
    flex-direction: column;
    align-items: stretch;
  }

  .wp-input,
  .wp-coord,
  .wp-poi {
    width: 100%;
  }
}
</style>
