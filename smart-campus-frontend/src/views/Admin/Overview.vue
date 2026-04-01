<template>
  <div class="overview-page" v-loading="loading">
    <section class="hero-panel">
      <div class="hero-copy">
        <span class="eyebrow">运营总览</span>
        <h1>地图探索、用户互动与内容活跃度一屏掌握</h1>
        <p>
          这里展示平台的核心运营指标、分享趋势和热门地点排行，帮助你快速判断当前平台的活跃状态。
        </p>
      </div>

      <div class="range-switch">
        <button
          v-for="option in rangeOptions"
          :key="option.value"
          type="button"
          class="range-chip"
          :class="{ active: rangeDays === option.value }"
          @click="changeRange(option.value)"
        >
          {{ option.label }}
        </button>
      </div>
    </section>

    <section class="metric-grid">
      <article
        v-for="metric in metricCards"
        :key="metric.key"
        class="metric-card"
        :class="{ clickable: Boolean(metric.route) }"
        @click="handleMetricClick(metric)"
      >
        <span class="metric-label">{{ metric.label }}</span>
        <strong class="metric-value">{{ formatNumber(metric.value) }}</strong>
        <div class="metric-foot">
          <span>{{ metric.helper }}</span>
          <span v-if="metric.route" class="metric-link">进入模块</span>
          <span v-else class="metric-link muted">总览指标</span>
        </div>
      </article>
    </section>

    <section class="content-grid">
      <article class="panel trend-panel">
        <div class="panel-head">
          <div>
            <span class="panel-kicker">趋势图</span>
            <h2>最近 {{ rangeDays }} 天分享发布趋势</h2>
          </div>
          <span class="panel-note">按天统计</span>
        </div>

        <div v-if="trendPoints.length" class="trend-chart">
          <svg viewBox="0 0 720 260" class="trend-svg" preserveAspectRatio="none">
            <line
              v-for="line in 5"
              :key="line"
              x1="48"
              :y1="20 + (line - 1) * 50"
              x2="680"
              :y2="20 + (line - 1) * 50"
              class="grid-line"
            />
            <polyline :points="trendPolyline" class="trend-area-shadow" />
            <polyline :points="trendPolyline" class="trend-line" />
            <circle
              v-for="point in trendPlotPoints"
              :key="point.label"
              :cx="point.x"
              :cy="point.y"
              r="5"
              class="trend-dot"
            />
          </svg>

          <div class="trend-axis">
            <div v-for="point in trendPoints" :key="point.date" class="axis-item">
              <span>{{ point.date }}</span>
              <strong>{{ point.value }}</strong>
            </div>
          </div>
        </div>

        <el-empty v-else description="最近没有分享数据" />
      </article>

      <article class="panel hot-panel">
        <div class="panel-head">
          <div>
            <span class="panel-kicker">排行</span>
            <h2>热门地点排行</h2>
          </div>
          <span class="panel-note">按分享数、回复数排序</span>
        </div>

        <div v-if="overview?.hotPois?.length" class="hot-list">
          <div v-for="(item, index) in overview.hotPois" :key="item.poiId" class="hot-row">
            <div class="hot-rank">{{ index + 1 }}</div>
            <div class="hot-main">
              <div class="hot-meta">
                <strong>{{ item.poiName }}</strong>
                <span>{{ item.category }}</span>
              </div>
              <div class="hot-bar">
                <span class="hot-bar-fill" :style="{ width: getHotBarWidth(item.shareCount) }"></span>
              </div>
            </div>
            <div class="hot-stats">
              <span>{{ item.shareCount }} 分享</span>
              <span>{{ item.replyCount }} 回复</span>
            </div>
          </div>
        </div>

        <el-empty v-else description="当前时间范围内暂无热门地点" />
      </article>
    </section>

    <section class="panel recent-panel">
      <div class="panel-head">
        <div>
          <span class="panel-kicker">最近动态</span>
          <h2>最近新增分享</h2>
        </div>
        <span class="panel-note">按最新发布时间排序</span>
      </div>

      <div v-if="overview?.recentShares?.length" class="recent-list">
        <article v-for="share in overview.recentShares" :key="share.shareId" class="recent-card">
          <div class="recent-top">
            <div>
              <span class="recent-poi">{{ share.poiName }}</span>
              <h3>{{ share.authorDisplayName }}</h3>
            </div>
            <span class="recent-time">{{ share.createdAt }}</span>
          </div>
          <p class="recent-preview">{{ share.contentPreview }}</p>
          <div class="recent-meta">
            <span>@{{ share.authorUsername }}</span>
            <span>{{ share.imageCount }} 张图</span>
            <span>{{ share.likeCount }} 点赞</span>
            <span>{{ share.replyCount }} 回复</span>
          </div>
        </article>
      </div>

      <el-empty v-else description="还没有分享内容" />
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getAdminOverview } from '@/api/admin'

const router = useRouter()

const loading = ref(false)
const rangeDays = ref(7)
const overview = ref(null)

const rangeOptions = [
  { label: '最近 7 天', value: 7 },
  { label: '最近 30 天', value: 30 }
]

const metricCards = computed(() => {
  const summary = overview.value?.summary || {}
  return [
    {
      key: 'pois',
      label: '地点总数',
      value: summary.poiCount || 0,
      helper: '地点数据总量',
      route: '/admin/poi'
    },
    {
      key: 'users',
      label: '用户总数',
      value: summary.userCount || 0,
      helper: '已注册用户规模',
      route: '/admin/users'
    },
    {
      key: 'shares',
      label: '分享总数',
      value: summary.shareCount || 0,
      helper: '累计分享内容',
      route: '/admin/shares'
    },
    {
      key: 'replies',
      label: '回复总数',
      value: summary.replyCount || 0,
      helper: '社区互动深度',
      route: '/admin/replies'
    },
    {
      key: 'likes',
      label: '点赞总数',
      value: summary.likeCount || 0,
      helper: '累计互动热度',
      route: ''
    },
    {
      key: 'todayShares',
      label: '今日新增分享',
      value: summary.todayShareCount || 0,
      helper: '当天活跃情况',
      route: ''
    }
  ]
})

const trendPoints = computed(() => overview.value?.shareTrend || [])
const trendMax = computed(() => {
  const values = trendPoints.value.map((item) => item.value)
  const max = values.length ? Math.max(...values) : 0
  return max > 0 ? max : 1
})

const trendPlotPoints = computed(() => {
  if (!trendPoints.value.length) {
    return []
  }

  const width = 632
  const height = 200
  const step = trendPoints.value.length === 1 ? 0 : width / (trendPoints.value.length - 1)

  return trendPoints.value.map((point, index) => ({
    label: point.date,
    value: point.value,
    x: 48 + step * index,
    y: 220 - (point.value / trendMax.value) * height
  }))
})

const trendPolyline = computed(() => trendPlotPoints.value.map((point) => `${point.x},${point.y}`).join(' '))

const hotMax = computed(() => {
  const values = (overview.value?.hotPois || []).map((item) => item.shareCount)
  const max = values.length ? Math.max(...values) : 0
  return max > 0 ? max : 1
})

const loadOverview = async () => {
  loading.value = true
  try {
    overview.value = await getAdminOverview(rangeDays.value)
  } catch (error) {
    ElMessage.error(error.message || '加载运营总览失败')
  } finally {
    loading.value = false
  }
}

const changeRange = async (days) => {
  if (rangeDays.value === days) {
    return
  }
  rangeDays.value = days
  await loadOverview()
}

const handleMetricClick = (metric) => {
  if (!metric.route) {
    return
  }
  router.push(metric.route)
}

const getHotBarWidth = (shareCount) => `${Math.max((shareCount / hotMax.value) * 100, 8)}%`

const formatNumber = (value) => new Intl.NumberFormat('zh-CN').format(value || 0)

onMounted(async () => {
  await loadOverview()
})
</script>

<style scoped>
.overview-page {
  --panel-bg: rgba(255, 255, 255, 0.84);
  --panel-border: rgba(148, 163, 184, 0.22);
  --text-main: #0f172a;
  --text-sub: #64748b;
  --accent: #0ea5e9;
  --accent-strong: #2563eb;
  display: flex;
  flex-direction: column;
  gap: 22px;
}

.hero-panel,
.panel,
.metric-card {
  border: 1px solid var(--panel-border);
  background: var(--panel-bg);
  backdrop-filter: blur(18px);
  box-shadow: 0 18px 42px rgba(15, 23, 42, 0.07);
}

.hero-panel {
  border-radius: 28px;
  padding: 28px 30px;
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 20px;
  background:
    radial-gradient(circle at top right, rgba(14, 165, 233, 0.18), transparent 28%),
    linear-gradient(135deg, rgba(255, 255, 255, 0.92), rgba(243, 248, 255, 0.84));
}

.eyebrow,
.panel-kicker {
  display: inline-flex;
  align-items: center;
  padding: 6px 12px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: var(--accent-strong);
  background: rgba(37, 99, 235, 0.08);
}

.hero-copy {
  max-width: 680px;
}

.hero-copy h1 {
  margin: 14px 0 10px;
  color: var(--text-main);
  font-size: clamp(28px, 4vw, 42px);
  line-height: 1.1;
}

.hero-copy p {
  margin: 0;
  max-width: 620px;
  color: var(--text-sub);
  line-height: 1.75;
}

.range-switch {
  display: flex;
  align-items: center;
  gap: 10px;
}

.range-chip {
  border: none;
  border-radius: 999px;
  padding: 12px 18px;
  font-size: 14px;
  color: #475569;
  background: rgba(255, 255, 255, 0.9);
  cursor: pointer;
  transition: all 0.2s ease;
}

.range-chip.active {
  color: #fff;
  background: linear-gradient(135deg, var(--accent), var(--accent-strong));
  box-shadow: 0 14px 28px rgba(37, 99, 235, 0.22);
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 16px;
}

.metric-card {
  border-radius: 24px;
  padding: 22px 20px;
  min-height: 162px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.metric-card.clickable {
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.metric-card.clickable:hover {
  transform: translateY(-3px);
  box-shadow: 0 24px 48px rgba(15, 23, 42, 0.12);
}

.metric-label {
  font-size: 13px;
  color: var(--text-sub);
}

.metric-value {
  margin-top: 18px;
  font-size: clamp(28px, 2.8vw, 42px);
  line-height: 1;
  color: var(--text-main);
}

.metric-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  font-size: 12px;
  color: var(--text-sub);
}

.metric-link {
  color: var(--accent-strong);
}

.metric-link.muted {
  color: #94a3b8;
}

.content-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.55fr) minmax(360px, 0.95fr);
  gap: 18px;
}

.panel {
  border-radius: 28px;
  padding: 24px;
}

.panel-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 20px;
}

.panel-head h2 {
  margin: 12px 0 0;
  color: var(--text-main);
  font-size: 22px;
}

.panel-note {
  color: #94a3b8;
  font-size: 13px;
}

.trend-chart {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.trend-svg {
  width: 100%;
  height: 260px;
}

.grid-line {
  stroke: rgba(148, 163, 184, 0.22);
  stroke-width: 1;
}

.trend-line {
  fill: none;
  stroke: #0ea5e9;
  stroke-width: 4;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.trend-area-shadow {
  fill: none;
  stroke: rgba(14, 165, 233, 0.18);
  stroke-width: 10;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.trend-dot {
  fill: #fff;
  stroke: #2563eb;
  stroke-width: 3;
}

.trend-axis {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(52px, 1fr));
  gap: 8px;
}

.axis-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
  align-items: center;
  color: var(--text-sub);
  font-size: 12px;
}

.axis-item strong {
  color: var(--text-main);
}

.hot-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.hot-row {
  display: grid;
  grid-template-columns: 44px minmax(0, 1fr) auto;
  align-items: center;
  gap: 14px;
  padding: 14px 0;
  border-bottom: 1px solid rgba(226, 232, 240, 0.9);
}

.hot-row:last-child {
  border-bottom: none;
  padding-bottom: 0;
}

.hot-rank {
  width: 44px;
  height: 44px;
  display: grid;
  place-items: center;
  border-radius: 16px;
  background: linear-gradient(135deg, rgba(14, 165, 233, 0.16), rgba(37, 99, 235, 0.12));
  color: var(--accent-strong);
  font-weight: 800;
}

.hot-main {
  min-width: 0;
}

.hot-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
  flex-wrap: wrap;
}

.hot-meta strong {
  color: var(--text-main);
}

.hot-meta span,
.hot-stats {
  color: var(--text-sub);
  font-size: 13px;
}

.hot-bar {
  height: 10px;
  border-radius: 999px;
  background: rgba(226, 232, 240, 0.86);
  overflow: hidden;
}

.hot-bar-fill {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, #38bdf8, #2563eb);
}

.hot-stats {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 4px;
}

.recent-list {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 14px;
}

.recent-card {
  border-radius: 22px;
  padding: 18px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.92), rgba(247, 250, 255, 0.92));
  border: 1px solid rgba(226, 232, 240, 0.88);
}

.recent-top {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.recent-poi {
  display: inline-flex;
  align-items: center;
  padding: 4px 10px;
  border-radius: 999px;
  background: rgba(14, 165, 233, 0.1);
  color: var(--accent-strong);
  font-size: 12px;
  font-weight: 700;
}

.recent-top h3 {
  margin: 0;
  font-size: 20px;
  color: var(--text-main);
}

.recent-time {
  color: #94a3b8;
  font-size: 12px;
}

.recent-preview {
  margin: 16px 0;
  color: #334155;
  line-height: 1.7;
  min-height: 72px;
}

.recent-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  color: var(--text-sub);
  font-size: 12px;
}

@media (max-width: 1440px) {
  .metric-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .recent-list {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 1080px) {
  .hero-panel,
  .content-grid {
    grid-template-columns: 1fr;
    display: grid;
  }

  .hero-panel {
    gap: 18px;
  }

  .range-switch {
    justify-content: flex-start;
  }

  .content-grid {
    gap: 18px;
  }
}

@media (max-width: 760px) {
  .metric-grid,
  .recent-list {
    grid-template-columns: 1fr;
  }

  .hero-panel,
  .panel {
    padding: 20px;
    border-radius: 22px;
  }

  .hot-row {
    grid-template-columns: 1fr;
    align-items: flex-start;
  }

  .hot-stats {
    align-items: flex-start;
  }
}
</style>
