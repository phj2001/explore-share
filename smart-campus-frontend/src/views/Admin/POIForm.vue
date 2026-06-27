<template>
  <div class="poi-form-page">
    <section class="page-hero">
      <div class="hero-copy">
        <span class="hero-kicker">{{ isEdit ? '编辑地点' : '新建地点' }}</span>
        <h1>{{ isEdit ? '更新地点基础信息与坐标' : '录入新的地点信息' }}</h1>
        <p>
          请保持地点名称、分类和经纬度信息准确一致，便于地图展示、路线关联与后台后续维护。
        </p>
      </div>

      <div class="hero-stats">
        <article class="hero-stat">
          <span>当前模式</span>
          <strong>{{ isEdit ? '编辑' : '创建' }}</strong>
          <em>{{ isEdit ? '修改已有地点资料' : '新增一条地图地点记录' }}</em>
        </article>
        <article class="hero-stat">
          <span>分类数量</span>
          <strong>{{ poiStore.categories.length }}</strong>
          <em>可直接选择已有分类，也可创建新分类</em>
        </article>
      </div>
    </section>

    <div class="form-layout">
      <section class="form-panel">
        <div class="panel-head">
          <div>
            <span class="panel-kicker">表单编辑</span>
            <h2>{{ isEdit ? '地点信息维护' : '填写地点资料' }}</h2>
          </div>
          <el-button text @click="handleCancel">返回列表</el-button>
        </div>

        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          label-width="96px"
          class="poi-form"
        >
          <el-form-item label="地点名称" prop="name">
            <el-input v-model="form.name" maxlength="80" show-word-limit placeholder="请输入地点名称" />
          </el-form-item>

          <el-form-item label="地点分类" prop="category">
            <el-select
              v-model="form.category"
              placeholder="请选择或输入分类"
              filterable
              allow-create
              default-first-option
              clearable
              class="full-width"
            >
              <el-option
                v-for="category in poiStore.categories"
                :key="category"
                :label="category"
                :value="category"
              />
            </el-select>
          </el-form-item>

          <div class="coordinate-grid">
            <el-form-item label="纬度" prop="latitude">
              <el-input-number
                v-model="form.latitude"
                :min="-90"
                :max="90"
                :precision="6"
                :step="0.000001"
                class="full-width"
              />
            </el-form-item>

            <el-form-item label="经度" prop="longitude">
              <el-input-number
                v-model="form.longitude"
                :min="-180"
                :max="180"
                :precision="6"
                :step="0.000001"
                class="full-width"
              />
            </el-form-item>
          </div>

          <el-form-item label="地点说明" prop="description">
            <el-input
              v-model="form.description"
              type="textarea"
              :rows="5"
              maxlength="500"
              show-word-limit
              placeholder="请输入地点简介、功能说明或识别信息"
            />
          </el-form-item>

          <div class="form-actions">
            <el-button @click="handleCancel">取消</el-button>
            <el-button type="primary" :loading="poiStore.isLoading" @click="handleSubmit">
              {{ isEdit ? '保存修改' : '创建地点' }}
            </el-button>
          </div>
        </el-form>
      </section>

      <aside class="info-panel">
        <div class="info-card">
          <span>填写建议</span>
          <strong>名称简洁统一</strong>
          <p>优先使用校园内常用名称，避免同一地点出现多种命名方式。</p>
        </div>

        <div class="info-card">
          <span>坐标要求</span>
          <strong>经纬度尽量精确</strong>
          <p>建议保留 6 位小数，便于地图落点、路线起终点和附近地点计算。</p>
        </div>

        <div class="info-card">
          <span>分类管理</span>
          <strong>保持分类可复用</strong>
          <p>如果是新增分类，请尽量使用稳定、可扩展的命名，避免产生重复分类。</p>
        </div>
      </aside>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { usePOIStore } from '@/stores/poi'

const router = useRouter()
const route = useRoute()
const poiStore = usePOIStore()

const formRef = ref(null)

const form = reactive({
  name: '',
  category: '',
  latitude: null,
  longitude: null,
  description: ''
})

const isEdit = computed(() => Boolean(route.params.id))

const rules = {
  name: [{ required: true, message: '请输入地点名称', trigger: 'blur' }],
  category: [{ required: true, message: '请选择或输入分类', trigger: 'change' }],
  latitude: [{ required: true, message: '请输入纬度', trigger: 'blur' }],
  longitude: [{ required: true, message: '请输入经度', trigger: 'blur' }]
}

const loadPOI = async () => {
  try {
    const poi = await poiStore.fetchPOIById(route.params.id)
    Object.assign(form, {
      name: poi.name,
      category: poi.category,
      latitude: poi.latitude,
      longitude: poi.longitude,
      description: poi.description
    })
  } catch (error) {
    ElMessage.error(error.message || '加载地点信息失败')
    router.back()
  }
}

const handleSubmit = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) {
    return
  }

  try {
    const payload = {
      ...form,
      name: form.name.trim(),
      category: form.category.trim(),
      description: form.description?.trim() || ''
    }

    if (isEdit.value) {
      await poiStore.update(route.params.id, payload)
      ElMessage.success('地点信息已更新')
    } else {
      await poiStore.create(payload)
      ElMessage.success('地点创建成功')
    }

    router.push('/admin/poi')
  } catch (error) {
    ElMessage.error(error.message || (isEdit.value ? '更新地点失败' : '创建地点失败'))
  }
}

const handleCancel = () => {
  router.back()
}

onMounted(async () => {
  try {
    await poiStore.fetchCategories()

    if (isEdit.value) {
      await loadPOI()
    }
  } catch (error) {
    ElMessage.error(error.message || '加载分类失败')
  }
})
</script>

<style scoped>
.poi-form-page {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.page-hero {
  display: grid;
  grid-template-columns: minmax(0, 1.3fr) minmax(280px, 0.9fr);
  gap: 18px;
}

.hero-copy p {
  max-width: 620px;
}

.hero-stats {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.hero-stat {
  padding: 18px;
}

.form-layout {
  display: grid;
  grid-template-columns: minmax(0, 1.3fr) minmax(280px, 0.7fr);
  gap: 18px;
}

.panel-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 18px;
}

.poi-form {
  max-width: 760px;
}

.coordinate-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.full-width {
  width: 100%;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding-top: 10px;
}

.info-panel {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.info-card {
  padding: 18px;
}

.info-card span {
  display: block;
  color: var(--admin-text-muted);
  font-size: 12px;
}

.info-card strong {
  display: block;
  margin: 10px 0 8px;
  color: var(--admin-text);
  font-size: 16px;
}

.info-card p {
  margin: 0;
  color: var(--ink-600);
  font-size: 13px;
  line-height: 1.7;
}

@media (max-width: 1100px) {
  .page-hero,
  .form-layout {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 760px) {
  .hero-stats,
  .coordinate-grid {
    grid-template-columns: 1fr;
  }

  .panel-head,
  .form-actions {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
