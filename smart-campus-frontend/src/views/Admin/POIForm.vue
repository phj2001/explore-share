<template>
  <div class="poi-form-container">
    <h2>{{ isEdit ? '编辑 POI' : '新增 POI' }}</h2>

    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-width="120px"
      style="max-width: 600px; margin-top: 30px"
    >
      <el-form-item label="POI 名称" prop="name">
        <el-input v-model="form.name" placeholder="请输入 POI 名称" />
      </el-form-item>

      <el-form-item label="分类" prop="category">
        <el-select
          v-model="form.category"
          placeholder="请选择或输入分类"
          style="width: 100%"
          filterable
          allow-create
          default-first-option
          clearable
        >
          <el-option
            v-for="category in poiStore.categories"
            :key="category"
            :label="category"
            :value="category"
          />
        </el-select>
      </el-form-item>

      <el-form-item label="纬度" prop="latitude">
        <el-input-number
          v-model="form.latitude"
          :min="-90"
          :max="90"
          :precision="6"
          :step="0.000001"
          style="width: 100%"
        />
      </el-form-item>

      <el-form-item label="经度" prop="longitude">
        <el-input-number
          v-model="form.longitude"
          :min="-180"
          :max="180"
          :precision="6"
          :step="0.000001"
          style="width: 100%"
        />
      </el-form-item>

      <el-form-item label="描述" prop="description">
        <el-input
          v-model="form.description"
          type="textarea"
          :rows="4"
          placeholder="请输入描述信息"
        />
      </el-form-item>

      <el-form-item>
        <el-button type="primary" :loading="poiStore.isLoading" @click="handleSubmit">
          {{ isEdit ? '保存' : '创建' }}
        </el-button>
        <el-button @click="handleCancel">取消</el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { usePOIStore } from '@/stores/poi'
import { ElMessage } from 'element-plus'

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

const isEdit = computed(() => !!route.params.id)

const rules = {
  name: [{ required: true, message: '请输入 POI 名称', trigger: 'blur' }],
  category: [{ required: true, message: '请选择或输入分类', trigger: 'change' }],
  latitude: [{ required: true, message: '请输入纬度', trigger: 'blur' }],
  longitude: [{ required: true, message: '请输入经度', trigger: 'blur' }]
}

onMounted(async () => {
  await poiStore.fetchCategories()

  if (isEdit.value) {
    await loadPOI()
  }
})

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
    ElMessage.error('加载 POI 失败')
    router.back()
  }
}

const handleSubmit = async () => {
  await formRef.value.validate()

  try {
    const payload = {
      ...form,
      name: form.name.trim(),
      category: form.category.trim(),
      description: form.description?.trim() || ''
    }

    if (isEdit.value) {
      await poiStore.update(route.params.id, payload)
      ElMessage.success('更新成功')
    } else {
      await poiStore.create(payload)
      ElMessage.success('创建成功')
    }
    router.push('/admin/poi')
  } catch (error) {
    ElMessage.error(isEdit.value ? '更新失败' : '创建失败')
  }
}

const handleCancel = () => {
  router.back()
}
</script>

<style scoped>
.poi-form-container {
  background: #fff;
  padding: 30px;
  border-radius: 4px;
}

.poi-form-container h2 {
  margin: 0 0 20px;
  color: #333;
}
</style>
