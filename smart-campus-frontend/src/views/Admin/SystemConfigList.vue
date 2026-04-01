<template>
  <div class="config-page">
    <section class="page-hero">
      <div class="hero-copy">
        <span class="hero-kicker">系统配置</span>
        <h1>集中维护首页展示规则与系统默认参数</h1>
        <p>
          这里统一管理首页公告数量、默认展开状态、热门地点榜单数量等关键参数，
          修改后会直接影响对应模块的默认展示效果。
        </p>
      </div>

      <div class="hero-stats">
        <article v-for="item in heroStats" :key="item.label" class="hero-stat">
          <span>{{ item.label }}</span>
          <strong>{{ item.value }}</strong>
          <em>{{ item.helper }}</em>
        </article>
      </div>
    </section>

    <section class="table-panel">
      <div class="panel-head">
        <div>
          <span class="panel-kicker">配置列表</span>
          <h2>当前系统配置项</h2>
        </div>
        <el-button text @click="loadConfigs">刷新列表</el-button>
      </div>

      <el-table :data="configs" v-loading="loading" stripe>
        <template #empty>
          <el-empty description="当前还没有可编辑的系统配置项" />
        </template>

        <el-table-column prop="label" label="配置名称" min-width="220" />
        <el-table-column label="配置说明" min-width="300">
          <template #default="{ row }">
            <div class="description-cell">{{ row.description }}</div>
          </template>
        </el-table-column>

        <el-table-column label="类型" width="110">
          <template #default="{ row }">
            <el-tag :type="row.valueType === 'BOOLEAN' ? 'warning' : 'primary'" effect="plain">
              {{ row.valueType === 'BOOLEAN' ? '布尔值' : '整数' }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="当前值" width="140">
          <template #default="{ row }">
            <strong class="value-text">{{ formatValue(row) }}</strong>
          </template>
        </el-table-column>

        <el-table-column label="默认值" width="140">
          <template #default="{ row }">
            <span class="muted-text">{{ formatDefaultValue(row) }}</span>
          </template>
        </el-table-column>

        <el-table-column label="公开读取" width="120">
          <template #default="{ row }">
            <el-tag :type="row.publicVisible ? 'success' : 'info'" effect="plain">
              {{ row.publicVisible ? '是' : '否' }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" plain @click="openEditDialog(row)">编辑</el-button>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <el-dialog
      v-model="dialogVisible"
      title="编辑系统配置"
      width="560px"
      destroy-on-close
      @closed="resetDialog"
    >
      <template v-if="editingConfig">
        <div class="dialog-meta">
          <div class="meta-row">
            <span>配置名称</span>
            <strong>{{ editingConfig.label }}</strong>
          </div>
          <div class="meta-row">
            <span>配置键</span>
            <code>{{ editingConfig.configKey }}</code>
          </div>
          <div class="meta-row">
            <span>配置说明</span>
            <p>{{ editingConfig.description }}</p>
          </div>
        </div>

        <el-form label-width="110px">
          <el-form-item label="当前配置值">
            <el-switch
              v-if="editingConfig.valueType === 'BOOLEAN'"
              v-model="booleanValue"
              inline-prompt
              active-text="开启"
              inactive-text="关闭"
            />
            <el-input-number
              v-else
              v-model="integerValue"
              :min="1"
              :max="20"
              controls-position="right"
            />
          </el-form-item>
        </el-form>
      </template>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="saving" @click="handleSave">保存配置</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getAdminSystemConfigList, updateAdminSystemConfig } from '@/api/adminSystemConfig'

const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const configs = ref([])
const editingConfig = ref(null)
const integerValue = ref(1)
const booleanValue = ref(false)

const heroStats = computed(() => {
  const total = configs.value.length
  const publicCount = configs.value.filter((item) => item.publicVisible).length
  const booleanCount = configs.value.filter((item) => item.valueType === 'BOOLEAN').length
  const integerCount = configs.value.filter((item) => item.valueType === 'INTEGER').length

  return [
    { label: '配置总数', value: `${total}`, helper: '当前可在后台直接维护的系统参数' },
    { label: '公开配置', value: `${publicCount}`, helper: '前台页面可直接读取的公开参数' },
    { label: '布尔配置', value: `${booleanCount}`, helper: '适用于开关类策略，例如默认收起' },
    { label: '整数配置', value: `${integerCount}`, helper: '适用于数量阈值，例如榜单数量' }
  ]
})

const loadConfigs = async () => {
  loading.value = true
  try {
    configs.value = await getAdminSystemConfigList()
  } catch (error) {
    ElMessage.error(error.message || '加载系统配置失败')
  } finally {
    loading.value = false
  }
}

const openEditDialog = (config) => {
  editingConfig.value = { ...config }
  booleanValue.value = config.value === 'true'
  integerValue.value = Number(config.value || config.defaultValue || 1)
  dialogVisible.value = true
}

const handleSave = async () => {
  if (!editingConfig.value) {
    return
  }

  const nextValue = editingConfig.value.valueType === 'BOOLEAN'
    ? String(booleanValue.value)
    : String(integerValue.value)

  saving.value = true
  try {
    const updated = await updateAdminSystemConfig(editingConfig.value.configKey, nextValue)
    const index = configs.value.findIndex((item) => item.configKey === updated.configKey)
    if (index >= 0) {
      configs.value.splice(index, 1, updated)
    }
    dialogVisible.value = false
    ElMessage.success('系统配置已更新')
  } catch (error) {
    ElMessage.error(error.message || '更新系统配置失败')
  } finally {
    saving.value = false
  }
}

const resetDialog = () => {
  editingConfig.value = null
  integerValue.value = 1
  booleanValue.value = false
}

const formatValue = (row) => {
  if (row.valueType === 'BOOLEAN') {
    return row.value === 'true' ? '开启' : '关闭'
  }
  return row.value
}

const formatDefaultValue = (row) => {
  if (row.valueType === 'BOOLEAN') {
    return row.defaultValue === 'true' ? '开启' : '关闭'
  }
  return row.defaultValue
}

onMounted(async () => {
  await loadConfigs()
})
</script>

<style scoped>
.config-page {
  --panel-bg: rgba(255, 255, 255, 0.9);
  --panel-border: rgba(148, 163, 184, 0.18);
  --ink: #0f172a;
  --muted: #64748b;
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.page-hero,
.table-panel {
  border: 1px solid var(--panel-border);
  background: var(--panel-bg);
  backdrop-filter: blur(18px);
  box-shadow: 0 18px 42px rgba(15, 23, 42, 0.06);
}

.page-hero {
  border-radius: 30px;
  padding: 28px 30px;
  display: grid;
  grid-template-columns: minmax(0, 1.2fr) minmax(280px, 0.9fr);
  gap: 20px;
  background:
    radial-gradient(circle at top right, rgba(37, 99, 235, 0.16), transparent 26%),
    linear-gradient(135deg, rgba(255, 255, 255, 0.98), rgba(239, 246, 255, 0.92));
}

.hero-kicker,
.panel-kicker {
  display: inline-flex;
  align-items: center;
  padding: 6px 12px;
  border-radius: 999px;
  background: rgba(37, 99, 235, 0.08);
  color: #1d4ed8;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.hero-copy h1 {
  margin: 14px 0 10px;
  font-size: clamp(28px, 4vw, 40px);
  line-height: 1.08;
  color: var(--ink);
}

.hero-copy p {
  margin: 0;
  color: var(--muted);
  line-height: 1.8;
  max-width: 640px;
}

.hero-stats {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.hero-stat {
  padding: 18px;
  border-radius: 22px;
  border: 1px solid rgba(147, 197, 253, 0.55);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.95), rgba(239, 246, 255, 0.92));
}

.hero-stat span,
.hero-stat em {
  display: block;
  color: var(--muted);
  font-style: normal;
}

.hero-stat span {
  font-size: 12px;
}

.hero-stat strong {
  display: block;
  margin: 12px 0 10px;
  font-size: 30px;
  line-height: 1;
  color: var(--ink);
}

.hero-stat em {
  font-size: 12px;
  line-height: 1.5;
}

.table-panel {
  border-radius: 28px;
  padding: 18px 18px 8px;
}

.panel-head {
  margin-bottom: 14px;
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
}

.panel-head h2 {
  margin: 12px 0 0;
  color: var(--ink);
  font-size: 24px;
}

.description-cell {
  color: var(--muted);
  line-height: 1.7;
}

.value-text {
  color: var(--ink);
}

.muted-text {
  color: var(--muted);
}

.dialog-meta {
  display: flex;
  flex-direction: column;
  gap: 14px;
  margin-bottom: 20px;
}

.meta-row {
  border-radius: 18px;
  padding: 16px;
  background: linear-gradient(180deg, rgba(247, 250, 255, 0.96), rgba(255, 255, 255, 0.96));
  border: 1px solid rgba(226, 232, 240, 0.9);
}

.meta-row span {
  display: block;
  color: var(--muted);
  font-size: 13px;
}

.meta-row strong,
.meta-row code,
.meta-row p {
  display: block;
  margin-top: 8px;
  color: var(--ink);
}

.meta-row p {
  margin-bottom: 0;
  line-height: 1.7;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

@media (max-width: 1180px) {
  .page-hero {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 760px) {
  .hero-stats {
    grid-template-columns: 1fr;
  }

  .page-hero,
  .table-panel {
    border-radius: 22px;
  }

  .dialog-footer {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
