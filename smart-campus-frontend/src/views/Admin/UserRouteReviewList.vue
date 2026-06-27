<template>
  <div class="review-page">
    <section class="page-hero">
      <div class="hero-copy">
        <span class="hero-kicker">路线审核</span>
        <h1>审核用户提交的路线，通过后公开展示</h1>
        <p>支持按状态筛选和关键词搜索，在操作列完成通过或驳回。</p>
      </div>
    </section>

    <section class="filter-panel">
      <div class="filter-main">
        <el-input
          v-model="keyword"
          clearable
          class="filter-input"
          placeholder="搜索路线标题或摘要"
          @keyup.enter="handleSearch"
          @clear="handleSearch"
        >
          <template #append>
            <el-button :icon="Search" @click="handleSearch" />
          </template>
        </el-input>

        <el-select v-model="statusFilter" clearable class="filter-select" placeholder="筛选状态" @change="handleSearch">
          <el-option label="待审核" :value="0" />
          <el-option label="已发布" :value="1" />
          <el-option label="已驳回" :value="2" />
        </el-select>

        <el-button :icon="RefreshRight" @click="resetFilters">重置</el-button>
      </div>
    </section>

    <section class="table-panel">
      <div class="panel-head">
        <div>
          <span class="panel-kicker">审核列表</span>
          <h2>用户路线记录</h2>
        </div>
        <el-button text @click="loadData">刷新列表</el-button>
      </div>

      <el-table :data="routes" v-loading="loading" stripe>
        <template #empty>
          <el-empty description="暂无路线记录" />
        </template>

        <el-table-column prop="id" label="ID" width="60" />

        <el-table-column label="提交人" width="130">
          <template #default="{ row }">
            <div class="user-meta">
              <el-avatar :size="26" :src="row.avatarUrl || undefined">
                {{ (row.displayName || row.username || '?').slice(0, 1) }}
              </el-avatar>
              <span>{{ row.displayName || row.username }}</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="路线标题" min-width="160" show-overflow-tooltip>
          <template #default="{ row }">
            <div>
              <strong>{{ row.title }}</strong>
              <p class="summary-text">{{ row.summary }}</p>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="出行方式" width="80">
          <template #default="{ row }">
            {{ modeLabel(row.defaultMode) }}
          </template>
        </el-table-column>

        <el-table-column label="途经点" width="64" align="center">
          <template #default="{ row }">
            {{ row.waypointCount }}
          </template>
        </el-table-column>

        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tooltip
              v-if="row.status === 2 && row.rejectReason"
              :content="'驳回原因：' + row.rejectReason"
              placement="top"
              :show-after="300"
            >
              <el-tag :type="statusType(row.status)" effect="plain" style="cursor:help;">
                {{ statusLabel(row.status) }}
              </el-tag>
            </el-tooltip>
            <el-tag v-else :type="statusType(row.status)" effect="plain">
              {{ statusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="提交时间" width="150">
          <template #default="{ row }">
            {{ formatDateTime(row.createdAt) }}
          </template>
        </el-table-column>

        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <div class="op-cell">
              <el-button link @click="viewDetail(row)">查看</el-button>
              <template v-if="row.status === 0">
                <el-button
                  link
                  type="success"
                  :loading="reviewingId === row.id && reviewingTo === 1"
                  @click="handleReview(row, 1)"
                >通过</el-button>
                <el-button
                  link
                  type="danger"
                  :loading="reviewingId === row.id && reviewingTo === 2"
                  @click="handleReview(row, 2)"
                >驳回</el-button>
              </template>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-bar">
        <span>共 {{ total }} 条记录</span>
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50]"
          :total="total"
          layout="sizes, prev, pager, next"
          @current-change="loadData"
          @size-change="handlePageSizeChange"
        />
      </div>
    </section>

    <!-- 详情抽屉 -->
    <el-drawer
      v-model="drawerVisible"
      size="700px"
      :with-header="false"
      destroy-on-close
      @close="onDrawerClose"
      @opened="onDrawerOpened"
    >
      <div class="detail" v-if="selectedRoute">
        <!-- 骨架屏：等待详情加载 -->
        <template v-if="detailLoading">
          <div style="padding: 24px">
            <el-skeleton :rows="8" animated />
          </div>
        </template>

        <template v-else>
          <!-- 头部 -->
          <div class="detail-head">
            <el-tag :type="statusType(selectedRoute.status)" effect="plain">
              {{ statusLabel(selectedRoute.status) }}
            </el-tag>
            <h2>{{ selectedRoute.title }}</h2>
            <p class="detail-summary">{{ selectedRoute.summary }}</p>
          </div>

          <!-- 地图预览 -->
          <div class="detail-map-wrap">
            <div ref="detailMapRoot" class="detail-map"></div>
            <div v-if="mapError" class="detail-map-error">{{ mapError }}</div>
          </div>

          <!-- 基本信息 -->
          <el-descriptions :column="2" border class="detail-desc">
            <el-descriptions-item label="提交人">
              {{ selectedRoute.displayName || selectedRoute.username }}
            </el-descriptions-item>
            <el-descriptions-item label="出行方式">
              {{ modeLabel(selectedRoute.defaultMode) }}
            </el-descriptions-item>
            <el-descriptions-item label="途经点数">
              {{ selectedRouteDetail?.waypoints?.length ?? selectedRoute.waypointCount }}
            </el-descriptions-item>
            <el-descriptions-item label="提交时间">
              {{ formatDateTime(selectedRoute.createdAt) }}
            </el-descriptions-item>
            <el-descriptions-item
              v-if="selectedRoute.status === 2 && selectedRoute.rejectReason"
              label="驳回原因"
              :span="2"
            >
              <span class="reject-reason-text">{{ selectedRoute.rejectReason }}</span>
            </el-descriptions-item>
          </el-descriptions>

          <!-- 详细描述 -->
          <div v-if="selectedRouteDetail?.description" class="detail-description">
            <h4>路线介绍</h4>
            <p>{{ selectedRouteDetail.description }}</p>
          </div>

          <!-- 途经点列表（时间轴） -->
          <div v-if="selectedRouteDetail?.waypoints?.length" class="detail-waypoints">
            <h4>
              途经点顺序
              <span class="wp-count-badge">{{ selectedRouteDetail.waypoints.length }}</span>
            </h4>
            <div class="wp-timeline">
              <div
                v-for="(wp, idx) in selectedRouteDetail.waypoints"
                :key="wp.id"
                class="wp-step"
              >
                <div class="wp-step-left">
                  <div
                    class="wp-dot"
                    :class="{
                      'wp-dot--start': idx === 0,
                      'wp-dot--end': idx === selectedRouteDetail.waypoints.length - 1
                    }"
                  >{{ idx + 1 }}</div>
                  <div v-if="idx < selectedRouteDetail.waypoints.length - 1" class="wp-connector" />
                </div>
                <div class="wp-step-body">
                  <strong>{{ wp.poiName || wp.waypointName || '未命名' }}</strong>
                  <span class="wp-coord">{{ Number(wp.latitude).toFixed(6) }}, {{ Number(wp.longitude).toFixed(6) }}</span>
                </div>
              </div>
            </div>
          </div>

          <!-- 审核操作 -->
          <div v-if="selectedRoute.status === 0" class="detail-actions">
            <el-button
              type="success"
              :loading="reviewingId === selectedRoute.id && reviewingTo === 1"
              @click="handleReview(selectedRoute, 1)"
            >通过审核</el-button>
            <el-button
              type="danger"
              :loading="reviewingId === selectedRoute.id && reviewingTo === 2"
              @click="handleReview(selectedRoute, 2)"
            >驳回</el-button>
          </div>
        </template>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, nextTick, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { RefreshRight, Search } from '@element-plus/icons-vue'
import { adminGetUserRoutes, adminReviewUserRoute, getRouteDetail } from '@/api/userRoute'
import { loadAmapSdk, toAmapCoordinate } from '@/utils/amap'

const keyword = ref('')
const statusFilter = ref(null)
const loading = ref(false)
const routes = ref([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(20)
const reviewingId = ref(null)
const reviewingTo = ref(null)
const drawerVisible = ref(false)
const selectedRoute = ref(null)
const selectedRouteDetail = ref(null)
const detailLoading = ref(false)
const detailMapRoot = ref(null)
const mapError = ref('')

let detailMap = null
let detailAMapRef = null

const modeLabel = (mode) => {
  const map = { walking: '步行', cycling: '骑行', driving: '驾车' }
  return map[mode] || mode
}

const statusLabel = (status) => {
  if (status === 0) return '待审核'
  if (status === 1) return '已发布'
  if (status === 2) return '已驳回'
  return '未知'
}

const statusType = (status) => {
  if (status === 0) return 'warning'
  if (status === 1) return 'success'
  if (status === 2) return 'danger'
  return 'info'
}

const formatDateTime = (val) => {
  if (!val) return '-'
  return new Date(val).toLocaleString('zh-CN', {
    year: 'numeric', month: '2-digit', day: '2-digit',
    hour: '2-digit', minute: '2-digit'
  })
}

const createMarkerContent = (index) => `
  <div style="
    width: 30px; height: 30px; border-radius: 50%;
    display: flex; align-items: center; justify-content: center;
    font-size: 12px; font-weight: 700; color: #fff;
    border: 2px solid rgba(255,255,255,0.9);
    background: linear-gradient(135deg, #1f8c69, #2d9e7a);
    box-shadow: 0 4px 12px rgba(31,140,105,0.4);
    user-select: none;
  ">${index}</div>
`

const initDetailMap = async () => {
  if (!detailMapRoot.value) return
  const waypoints = selectedRouteDetail.value?.waypoints
  if (!waypoints?.length) return

  mapError.value = ''
  try {
    detailAMapRef = await loadAmapSdk()
    const firstWp = waypoints[0]
    const center = toAmapCoordinate(Number(firstWp.latitude), Number(firstWp.longitude))

    detailMap = new detailAMapRef.Map(detailMapRoot.value, {
      viewMode: '2D',
      zoom: 15,
      center: center ? [center.lng, center.lat] : [116.397428, 39.90923],
      resizeEnable: true,
      zooms: [3, 20]
    })

    const positions = []
    waypoints.forEach((wp, idx) => {
      const coord = toAmapCoordinate(Number(wp.latitude), Number(wp.longitude))
      if (!coord) return
      const pos = [coord.lng, coord.lat]
      positions.push(pos)
      const marker = new detailAMapRef.Marker({
        position: pos,
        content: createMarkerContent(idx + 1),
        anchor: 'center',
        title: wp.poiName || wp.waypointName || `途经点 ${idx + 1}`
      })
      detailMap.add(marker)
    })

    if (positions.length >= 2) {
      const polyline = new detailAMapRef.Polyline({
        path: positions,
        strokeColor: '#1f8c69',
        strokeWeight: 4,
        strokeOpacity: 0.85,
        strokeStyle: 'solid',
        lineJoin: 'round',
        lineCap: 'round'
      })
      detailMap.add(polyline)
      detailMap.setFitView(null, false, [20, 20, 20, 20])
    }
  } catch (e) {
    mapError.value = '地图加载失败'
  }
}

const onDrawerOpened = async () => {
  if (!detailLoading.value && selectedRouteDetail.value) {
    await nextTick()
    initDetailMap()
  }
}

const onDrawerClose = () => {
  if (detailMap) {
    detailMap.destroy()
    detailMap = null
    detailAMapRef = null
  }
  mapError.value = ''
}

const loadData = async () => {
  loading.value = true
  try {
    const data = await adminGetUserRoutes({
      keyword: keyword.value || undefined,
      status: statusFilter.value ?? undefined,
      page: currentPage.value - 1,
      size: pageSize.value
    })
    routes.value = data.records || []
    total.value = data.total || 0
  } catch (e) {
    ElMessage.error(e.message || '加载失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  currentPage.value = 1
  loadData()
}

const resetFilters = () => {
  keyword.value = ''
  statusFilter.value = null
  currentPage.value = 1
  loadData()
}

const handlePageSizeChange = (val) => {
  pageSize.value = val
  currentPage.value = 1
  loadData()
}

const viewDetail = async (row) => {
  selectedRoute.value = row
  selectedRouteDetail.value = null
  detailLoading.value = true
  drawerVisible.value = true
  try {
    selectedRouteDetail.value = await getRouteDetail(row.id)
  } catch (e) {
    ElMessage.error(e.message || '加载路线详情失败')
  } finally {
    detailLoading.value = false
    // 等待骨架屏切换为真实内容后再初始化地图
    await nextTick()
    setTimeout(initDetailMap, 150)
  }
}

const handleReview = async (row, newStatus) => {
  let rejectReason = null

  if (newStatus === 1) {
    try {
      await ElMessageBox.confirm(`确认通过路线「${row.title}」？`, '通过审核', {
        confirmButtonText: '通过',
        cancelButtonText: '取消',
        type: 'success'
      })
    } catch {
      return
    }
  } else {
    try {
      const { value } = await ElMessageBox.prompt(
        '请填写驳回原因（选填，用户将看到此内容）',
        `驳回路线「${row.title}」`,
        {
          confirmButtonText: '确认驳回',
          cancelButtonText: '取消',
          inputType: 'textarea',
          inputPlaceholder: '例如：路线信息不完整、途经点描述不清晰等',
          inputValidator: () => true,
          type: 'warning'
        }
      )
      rejectReason = value || null
    } catch {
      return
    }
  }

  reviewingId.value = row.id
  reviewingTo.value = newStatus
  try {
    await adminReviewUserRoute(row.id, newStatus, rejectReason)
    ElMessage.success(newStatus === 1 ? '已通过' : '已驳回')
    drawerVisible.value = false
    loadData()
  } catch (e) {
    ElMessage.error(e.message || '操作失败')
  } finally {
    reviewingId.value = null
    reviewingTo.value = null
  }
}

onMounted(loadData)
</script>

<style scoped>
.review-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.page-hero {
  padding: 24px 28px;
  border-radius: 20px;
  background: linear-gradient(135deg, rgba(74, 222, 128, 0.07), rgba(22, 163, 74, 0.04));
  border: 1px solid var(--admin-border);
}

.hero-kicker {
  display: inline-flex;
  align-items: center;
  padding: 4px 12px;
  border-radius: 999px;
  background: var(--admin-accent-soft);
  color: var(--admin-accent-strong);
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.08em;
  margin-bottom: 12px;
}

.hero-copy h1 {
  margin: 0 0 8px;
  font-family: var(--font-serif);
  font-size: 20px;
  color: var(--admin-text);
}

.hero-copy p {
  margin: 0;
  font-size: 13px;
  color: var(--admin-text-muted);
}

.filter-panel {
  padding: 16px 20px;
  border-radius: 16px;
  background: var(--admin-panel);
  border: 1px solid var(--admin-border);
}

.filter-main {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.filter-input {
  width: 280px;
}

.filter-select {
  width: 140px;
}

.table-panel {
  border-radius: 16px;
  background: var(--admin-panel);
  border: 1px solid var(--admin-border);
  overflow: hidden;
}

.panel-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  padding: 18px 20px 14px;
  border-bottom: 1px solid var(--admin-border);
}

.panel-kicker {
  display: inline-block;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.06em;
  color: var(--admin-accent-strong);
  text-transform: uppercase;
  margin-bottom: 4px;
}

.panel-head h2 {
  margin: 0;
  font-family: var(--font-serif);
  font-size: 16px;
  color: var(--admin-text);
}

.user-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
}

.summary-text {
  margin: 2px 0 0;
  font-size: 12px;
  color: var(--admin-text-muted);
  font-weight: 400;
}

.pagination-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 20px;
  border-top: 1px solid var(--admin-border);
  font-size: 13px;
  color: var(--admin-text-muted);
}

/* 抽屉详情 — 不设 height/overflow，由 el-drawer__body 负责滚动 */
.detail {
  display: flex;
  flex-direction: column;
  gap: 18px;
  padding: 24px;
}

/* 让 el-drawer 自带 body 可滚动 */
:deep(.el-drawer__body) {
  overflow-y: auto;
  padding: 0;
}

.detail-head h2 {
  margin: 10px 0 4px;
  font-family: var(--font-serif);
  font-size: 18px;
  color: var(--admin-text);
}

.detail-summary {
  margin: 0;
  font-size: 13px;
  color: var(--admin-text-muted);
  line-height: 1.6;
}

/* 地图 — flex-shrink:0 防止被压缩，高度固定在容器上 */
.detail-map-wrap {
  flex-shrink: 0;
  position: relative;
  height: 260px;
  border-radius: 14px;
  overflow: hidden;
  border: 1px solid var(--admin-border);
}

.detail-map {
  width: 100%;
  height: 100%;
}

.detail-map-error {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--admin-panel);
  font-size: 13px;
  color: var(--ink-400);
}

/* 基本信息 */
.detail-desc {
  margin-top: 0;
}

/* 路线介绍 */
.detail-description {
  padding: 14px 16px;
  border-radius: 12px;
  background: var(--admin-panel);
  border: 1px solid var(--admin-border);
}

.detail-description h4 {
  margin: 0 0 8px;
  font-size: 13px;
  font-weight: 700;
  color: var(--ink-600);
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.detail-description p {
  margin: 0;
  font-size: 14px;
  color: var(--ink-700);
  line-height: 1.7;
  white-space: pre-wrap;
}

/* 途经点时间轴 */
.detail-waypoints h4 {
  margin: 0 0 14px;
  font-size: 13px;
  font-weight: 700;
  color: var(--ink-600);
  text-transform: uppercase;
  letter-spacing: 0.05em;
  display: flex;
  align-items: center;
  gap: 6px;
}

.wp-count-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 20px;
  height: 20px;
  padding: 0 5px;
  border-radius: 999px;
  background: var(--admin-accent-soft);
  color: var(--admin-accent-strong);
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0;
  text-transform: none;
}

.wp-timeline {
  display: flex;
  flex-direction: column;
}

.wp-step {
  display: flex;
  gap: 12px;
  min-height: 40px;
}

.wp-step-left {
  display: flex;
  flex-direction: column;
  align-items: center;
  flex-shrink: 0;
  width: 26px;
}

.wp-dot {
  width: 26px;
  height: 26px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--forest-400), var(--forest-700));
  color: #fff;
  font-size: 11px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  box-shadow: 0 2px 6px rgba(21, 128, 61, 0.25);
}

.wp-dot--start {
  background: linear-gradient(135deg, #34d399, #059669);
  box-shadow: 0 2px 6px rgba(5, 150, 105, 0.3);
}

.wp-dot--end {
  background: linear-gradient(135deg, #fbbf24, #d97706);
  box-shadow: 0 2px 6px rgba(217, 119, 6, 0.3);
}

.wp-connector {
  flex: 1;
  width: 2px;
  min-height: 12px;
  margin: 3px 0;
  background: linear-gradient(180deg, var(--forest-200), var(--forest-100));
  border-radius: 1px;
}

.wp-step-body {
  padding: 3px 0 14px;
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.wp-step-body strong {
  font-size: 13px;
  font-weight: 600;
  color: var(--admin-text);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.wp-coord {
  font-family: var(--font-mono);
  font-size: 11px;
  color: var(--ink-400);
}

/* 操作按钮 */
.detail-actions {
  display: flex;
  gap: 12px;
  padding-top: 4px;
}

.reject-reason-text {
  color: var(--clay-600);
  line-height: 1.6;
}

.op-cell {
  display: flex;
  align-items: center;
  flex-wrap: nowrap;
  gap: 2px;
}
</style>
