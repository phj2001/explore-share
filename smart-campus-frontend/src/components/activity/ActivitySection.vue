<template>
  <section v-if="!dataLoaded || activities.length > 0" class="front-shell activity-section">
    <div class="section-head">
      <div class="head-left">
        <span class="section-kicker">近期活动</span>
        <h2>值得关注的线下活动</h2>
      </div>
      <button class="refresh-btn" :disabled="loading" title="刷新活动" @click="loadActivities(true)">
        <svg class="refresh-icon" :class="{ spinning: loading }" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M23 4v6h-6M1 20v-6h6"/>
          <path d="M3.51 9a9 9 0 0114.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0020.49 15"/>
        </svg>
      </button>
    </div>

    <el-skeleton v-if="loading && !activities.length" :rows="4" animated />

    <div v-else-if="activities.length" class="activity-grid">
      <article
        v-for="item in activities"
        :key="item.id"
        class="activity-card"
        @click="openDetail(item.id)"
      >
        <el-image
          v-if="item.coverImageUrl"
          :src="resolveAssetUrl(item.coverThumbnailUrl || item.coverImageUrl)"
          lazy
          fit="cover"
          class="card-cover"
        />
        <div v-else class="card-cover card-cover-placeholder">
          <span>{{ item.poiName || '活动' }}</span>
        </div>

        <div class="card-body">
          <div class="card-meta">
            <el-tag :type="getStatusType(item)" effect="plain" size="small">{{ getStatusLabel(item) }}</el-tag>
            <span>{{ formatDateRange(item.startTime, item.endTime) }}</span>
          </div>

          <h3>{{ item.title }}</h3>
          <p class="summary">{{ item.summary }}</p>

          <div class="card-footer">
            <span>{{ item.poiName || '未关联地点' }}</span>
            <span class="card-action">查看详情</span>
          </div>
        </div>
      </article>
    </div>

    <el-dialog
      v-model="detailVisible"
      width="760px"
      destroy-on-close
      append-to-body
      :title="selectedActivity?.title || '活动详情'"
      @closed="selectedActivity = null"
    >
      <div v-loading="detailLoading" class="detail-shell">
        <template v-if="selectedActivity">
          <el-image
            v-if="selectedActivity.coverImageUrl"
            :src="resolveAssetUrl(selectedActivity.coverImageUrl)"
            lazy
            fit="cover"
            class="detail-cover"
          />

          <div class="detail-tags">
            <el-tag :type="getStatusType(selectedActivity)" effect="plain">{{ getStatusLabel(selectedActivity) }}</el-tag>
            <span>{{ formatDateRange(selectedActivity.startTime, selectedActivity.endTime) }}</span>
          </div>

          <p class="detail-summary">{{ selectedActivity.summary }}</p>

          <div class="detail-grid">
            <div class="info-card">
              <span>关联地点</span>
              <strong>{{ selectedActivity.poiName || '未关联地点' }}</strong>
            </div>
            <div class="info-card">
              <span>发布时间</span>
              <strong>{{ formatDateTime(selectedActivity.publishedAt) }}</strong>
            </div>
          </div>

          <div class="detail-content">{{ selectedActivity.content }}</div>
        </template>
      </div>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="detailVisible = false">关闭</el-button>
          <el-button
            v-if="selectedActivity?.poiId"
            type="primary"
            @click="focusPoi(selectedActivity)"
          >
            在地图中查看地点
          </el-button>
        </div>
      </template>
    </el-dialog>
  </section>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getActivityDetail, getActivityList } from '@/api/activity'
import { usePOIStore } from '@/stores/poi'
import { useMapStore } from '@/stores/map'
import { API_ORIGIN } from '@/utils/request'

const activities = ref([])
const loading = ref(false)
const dataLoaded = ref(false)
const detailVisible = ref(false)
const detailLoading = ref(false)
const selectedActivity = ref(null)

const poiStore = usePOIStore()
const mapStore = useMapStore()

const loadActivities = async (forceRefresh = false) => {
  loading.value = true
  try {
    activities.value = await getActivityList({ limit: 4 }, { forceRefresh })
  } catch (error) {
    ElMessage.error(error.message || '加载活动失败')
  } finally {
    loading.value = false
    dataLoaded.value = true
  }
}

const openDetail = async (activityId) => {
  detailVisible.value = true
  detailLoading.value = true
  try {
    selectedActivity.value = await getActivityDetail(activityId)
  } catch (error) {
    selectedActivity.value = null
    detailVisible.value = false
    ElMessage.error(error.message || '加载活动详情失败')
  } finally {
    detailLoading.value = false
  }
}

const focusPoi = async (activity) => {
  try {
    let poi = poiStore.getCachedPOIById(activity.poiId)
    if (!poi) poi = await poiStore.fetchPOIById(activity.poiId)
    mapStore.selectPOI(poi)
    detailVisible.value = false
    window.scrollTo({ top: 0, behavior: 'smooth' })
  } catch (error) {
    ElMessage.error(error.message || '定位活动地点失败')
  }
}

const resolveAssetUrl = (value) => {
  if (!value) return ''
  if (/^https?:\/\//i.test(value)) return value
  return `${API_ORIGIN}${value.startsWith('/') ? value : `/${value}`}`
}

const toDate = (value) => value ? new Date(value) : null

const getStatusLabel = (item) => {
  const now = Date.now()
  const start = toDate(item.startTime)?.getTime()
  const end = toDate(item.endTime)?.getTime()
  if (start == null || end == null) return '时间待定'
  if (now < start) return '即将开始'
  if (now <= end) return '进行中'
  return '已结束'
}

const getStatusType = (item) => {
  const label = getStatusLabel(item)
  if (label === '进行中') return 'success'
  if (label === '即将开始') return 'warning'
  return 'info'
}

const formatDateRange = (startTime, endTime) => {
  const formatter = new Intl.DateTimeFormat('zh-CN', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
  const start = toDate(startTime)
  const end = toDate(endTime)
  if (!start || !end) return '时间待定'
  return `${formatter.format(start)} - ${formatter.format(end)}`
}

const formatDateTime = (value) => {
  const date = toDate(value)
  if (!date) return '未发布'
  return new Intl.DateTimeFormat('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' }).format(date)
}

onMounted(() => loadActivities(false))
</script>

<style scoped>
.activity-section {
  padding: 24px 0;
}

.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 18px;
}

.head-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.section-kicker {
  display: inline-flex;
  padding: 4px 10px;
  border-radius: 999px;
  background: rgba(245, 158, 11, 0.12);
  color: #b45309;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.06em;
  flex-shrink: 0;
}

.section-head h2 {
  margin: 0;
  color: var(--front-text);
  font-size: 20px;
  font-weight: 700;
}

.refresh-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  border: 1px solid var(--front-border);
  background: transparent;
  color: var(--front-text-muted);
  cursor: pointer;
  flex-shrink: 0;
  transition: color 0.15s, background 0.15s;
}

.refresh-btn:hover:not(:disabled) {
  background: rgba(245, 158, 11, 0.1);
  color: #b45309;
  border-color: transparent;
}

.refresh-btn:disabled {
  opacity: 0.4;
  cursor: default;
}

.refresh-icon {
  width: 14px;
  height: 14px;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.spinning {
  animation: spin 0.8s linear infinite;
}

.activity-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 18px;
}

.activity-card {
  overflow: hidden;
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.96);
  border: 1px solid var(--front-border);
  box-shadow: var(--front-shadow-soft);
  cursor: pointer;
  transition: transform 0.22s ease, box-shadow 0.22s ease;
}

.activity-card:hover {
  transform: translateY(-4px);
  box-shadow: var(--front-shadow);
}

.card-cover {
  width: 100%;
  height: 200px;
  display: block;
}

.card-cover-placeholder {
  display: grid;
  place-items: center;
  background: linear-gradient(135deg, rgba(245, 158, 11, 0.16), rgba(251, 191, 36, 0.14));
  color: #7c2d12;
  font-size: 20px;
  font-weight: 700;
}

.card-body {
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.card-meta,
.card-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  flex-wrap: wrap;
  color: var(--front-text-muted);
  font-size: 12px;
}

.card-body h3 {
  margin: 0;
  color: var(--front-text);
  font-size: 17px;
  line-height: 1.35;
}

.summary {
  margin: 0;
  color: var(--front-text-soft);
  font-size: 13px;
  line-height: 1.7;
  min-height: 60px;
}

.card-action {
  color: var(--front-accent-strong);
  font-weight: 600;
}

.detail-shell {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.detail-cover {
  width: 100%;
  height: 260px;
  border-radius: 22px;
  overflow: hidden;
}

.detail-tags {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  color: #64748b;
  font-size: 13px;
}

.detail-summary {
  margin: 0;
  color: var(--front-text-soft);
  line-height: 1.8;
}

.detail-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.info-card {
  padding: 14px 16px;
  border-radius: 16px;
  background: linear-gradient(180deg, rgba(255, 250, 240, 0.94), rgba(255, 255, 255, 0.96));
  border: 1px solid rgba(245, 158, 11, 0.14);
}

.info-card span {
  display: block;
  color: #64748b;
  font-size: 12px;
}

.info-card strong {
  display: block;
  margin-top: 8px;
  color: #0f172a;
}

.detail-content {
  padding: 16px;
  border-radius: 18px;
  background: rgba(248, 250, 252, 0.96);
  color: var(--front-text-soft);
  line-height: 1.85;
  white-space: pre-wrap;
}

.dialog-footer {
  display: flex;
  gap: 10px;
}

@media (max-width: 1200px) {
  .activity-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 760px) {
  .activity-section {
    padding: 20px 0;
  }

  .activity-grid,
  .detail-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 560px) {
  .section-head {
    margin-bottom: 12px;
  }

  .section-head h2 {
    font-size: 17px;
  }

  .activity-grid {
    gap: 12px;
  }

  .activity-card {
    border-radius: 18px;
  }

  .card-cover {
    height: 160px;
  }

  .card-body {
    padding: 12px;
    gap: 10px;
  }

  .summary {
    min-height: auto;
  }

  .detail-cover {
    height: 200px;
    border-radius: 18px;
  }
}
</style>
