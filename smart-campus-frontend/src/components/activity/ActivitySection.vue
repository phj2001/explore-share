<template>
  <section class="activity-section">
    <div class="section-head">
      <div>
        <span class="section-kicker">近期活动</span>
        <h2>值得关注的线下活动</h2>
        <p>查看即将开始或正在进行的活动安排，感兴趣时还能直接联动到对应地点。</p>
      </div>
      <el-button text :loading="loading" @click="loadActivities">刷新活动</el-button>
    </div>

    <div v-if="activities.length" class="activity-grid">
      <article
        v-for="item in activities"
        :key="item.id"
        class="activity-card"
        @click="openDetail(item.id)"
      >
        <el-image
          v-if="item.coverImageUrl"
          :src="resolveAssetUrl(item.coverImageUrl)"
          fit="cover"
          class="card-cover"
        />
        <div v-else class="card-cover card-cover-placeholder">
          <span>{{ item.poiName || '活动' }}</span>
        </div>

        <div class="card-body">
          <div class="card-meta">
            <el-tag :type="getStatusType(item)" effect="plain">{{ getStatusLabel(item) }}</el-tag>
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

    <el-empty v-else v-loading="loading" description="当前还没有已发布活动" />

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
const detailVisible = ref(false)
const detailLoading = ref(false)
const selectedActivity = ref(null)

const poiStore = usePOIStore()
const mapStore = useMapStore()

const loadActivities = async () => {
  loading.value = true
  try {
    activities.value = await getActivityList({ limit: 4 })
  } catch (error) {
    ElMessage.error(error.message || '加载活动失败')
  } finally {
    loading.value = false
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
    let poi = poiStore.poiList.find((candidate) => candidate.id === activity.poiId)
    if (!poi) {
      poi = await poiStore.fetchPOIById(activity.poiId)
    }

    mapStore.selectPOI(poi)
    detailVisible.value = false
    window.scrollTo({ top: 0, behavior: 'smooth' })
  } catch (error) {
    ElMessage.error(error.message || '定位活动地点失败')
  }
}

const resolveAssetUrl = (value) => {
  if (!value) {
    return ''
  }

  if (/^https?:\/\//i.test(value)) {
    return value
  }

  return `${API_ORIGIN}${value.startsWith('/') ? value : `/${value}`}`
}

const toDate = (value) => value ? new Date(value) : null

const getStatusLabel = (item) => {
  const now = Date.now()
  const start = toDate(item.startTime)?.getTime()
  const end = toDate(item.endTime)?.getTime()

  if (start == null || end == null) {
    return '时间待定'
  }
  if (now < start) {
    return '即将开始'
  }
  if (now <= end) {
    return '进行中'
  }
  return '已结束'
}

const getStatusType = (item) => {
  const label = getStatusLabel(item)
  if (label === '进行中') {
    return 'success'
  }
  if (label === '即将开始') {
    return 'warning'
  }
  return 'info'
}

const formatDateRange = (startTime, endTime) => {
  const formatter = new Intl.DateTimeFormat('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
  const start = toDate(startTime)
  const end = toDate(endTime)
  if (!start || !end) {
    return '时间待定'
  }
  return `${formatter.format(start)} - ${formatter.format(end)}`
}

const formatDateTime = (value) => {
  const date = toDate(value)
  if (!date) {
    return '未发布'
  }
  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  }).format(date)
}

onMounted(async () => {
  await loadActivities()
})
</script>

<style scoped>
.activity-section {
  padding: 32px 28px 42px;
  background:
    radial-gradient(circle at top left, rgba(249, 115, 22, 0.1), transparent 24%),
    linear-gradient(180deg, rgba(255, 251, 235, 0.94), rgba(248, 250, 252, 0.98));
}

.section-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 20px;
}

.section-kicker {
  display: inline-flex;
  padding: 6px 12px;
  border-radius: 999px;
  background: rgba(249, 115, 22, 0.12);
  color: #c2410c;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
}

.section-head h2 {
  margin: 14px 0 10px;
  color: #0f172a;
  font-size: clamp(28px, 4vw, 38px);
}

.section-head p {
  margin: 0;
  max-width: 720px;
  color: #64748b;
  line-height: 1.8;
}

.activity-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 18px;
}

.activity-card {
  overflow: hidden;
  border-radius: 28px;
  background: rgba(255, 255, 255, 0.96);
  border: 1px solid rgba(148, 163, 184, 0.16);
  box-shadow: 0 22px 48px rgba(15, 23, 42, 0.08);
  cursor: pointer;
  transition: transform 0.22s ease, box-shadow 0.22s ease;
}

.activity-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 28px 56px rgba(15, 23, 42, 0.14);
}

.card-cover {
  width: 100%;
  height: 220px;
  display: block;
}

.card-cover-placeholder {
  display: grid;
  place-items: center;
  background: linear-gradient(135deg, rgba(249, 115, 22, 0.2), rgba(251, 191, 36, 0.16));
  color: #7c2d12;
  font-size: 22px;
  font-weight: 700;
}

.card-body {
  padding: 18px;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.card-meta,
.card-footer,
.detail-tags,
.dialog-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}

.card-meta,
.card-footer {
  color: #64748b;
  font-size: 13px;
}

.card-body h3 {
  margin: 0;
  color: #0f172a;
  font-size: 20px;
  line-height: 1.35;
}

.summary {
  margin: 0;
  color: #475569;
  line-height: 1.75;
  min-height: 76px;
}

.card-action {
  color: #2563eb;
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
  border-radius: 24px;
  overflow: hidden;
}

.detail-tags {
  color: #64748b;
  font-size: 13px;
}

.detail-summary {
  margin: 0;
  color: #334155;
  line-height: 1.8;
}

.detail-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.info-card {
  padding: 16px;
  border-radius: 18px;
  background: linear-gradient(180deg, rgba(255, 247, 237, 0.92), rgba(255, 255, 255, 0.96));
  border: 1px solid rgba(251, 191, 36, 0.18);
}

.info-card span {
  display: block;
  color: #64748b;
  font-size: 12px;
}

.info-card strong {
  display: block;
  margin-top: 10px;
  color: #0f172a;
}

.detail-content {
  padding: 18px;
  border-radius: 20px;
  background: rgba(248, 250, 252, 0.96);
  color: #334155;
  line-height: 1.85;
  white-space: pre-wrap;
}

@media (max-width: 1200px) {
  .activity-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 760px) {
  .activity-section {
    padding: 22px 16px 30px;
  }

  .section-head,
  .dialog-footer {
    flex-direction: column;
    align-items: stretch;
  }

  .activity-grid,
  .detail-grid {
    grid-template-columns: 1fr;
  }
}
</style>
