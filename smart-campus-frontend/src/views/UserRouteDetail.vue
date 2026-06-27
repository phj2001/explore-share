<template>
  <div class="route-detail-page front-page">
    <Header />

    <main class="detail-main">
      <div class="front-shell detail-shell">
        <el-skeleton v-if="loading" :rows="8" animated />

        <template v-else-if="detail">
          <section class="route-hero front-panel">
            <div class="hero-content">
              <h1>{{ detail.title }}</h1>
              <p v-if="detail.summary" class="hero-summary">{{ detail.summary }}</p>
              <router-link :to="'/user/' + detail.userId" class="hero-author">
                <el-avatar :size="32" :src="detail.avatarUrl || undefined">
                  {{ (detail.displayName || 'U').slice(0, 1).toUpperCase() }}
                </el-avatar>
                <span>{{ detail.displayName || detail.username }}</span>
              </router-link>
            </div>
            <div class="hero-actions">
              <el-button :type="detail.liked ? 'default' : 'primary'" @click="handleLike">
                {{ detail.liked ? '已赞' : '点赞' }} ({{ detail.likeCount }})
              </el-button>
              <el-button :type="detail.favorited ? 'default' : 'primary'" @click="handleFavorite">
                {{ detail.favorited ? '已收藏' : '收藏' }} ({{ detail.favoriteCount }})
              </el-button>
            </div>
          </section>

          <!-- 路线地图 -->
          <div v-if="detail.waypoints?.length" class="route-map-section">
            <h3>路线地图</h3>
            <div class="route-map-wrap">
              <div ref="mapRoot" class="route-map"></div>
              <div v-if="mapError" class="map-error">{{ mapError }}</div>
            </div>
          </div>

          <div v-if="detail.description" class="route-desc">
            <h3>路线介绍</h3>
            <p>{{ detail.description }}</p>
          </div>

          <div class="waypoints-section">
            <h3>途经点 <span class="wp-count-badge">{{ detail.waypoints?.length || 0 }}</span></h3>
            <div class="wp-timeline">
              <div
                v-for="(wp, idx) in detail.waypoints"
                :key="wp.id"
                class="wp-step"
              >
                <div class="wp-step-left">
                  <div
                    class="wp-dot"
                    :class="{
                      'wp-dot--start': idx === 0,
                      'wp-dot--end': idx === detail.waypoints.length - 1
                    }"
                  >{{ idx + 1 }}</div>
                  <div v-if="idx < detail.waypoints.length - 1" class="wp-connector" />
                </div>
                <div class="wp-step-body">
                  <strong>{{ wp.poiName || wp.waypointName || '未命名站点' }}</strong>
                  <span class="wp-coords">{{ wp.latitude }}, {{ wp.longitude }}</span>
                  <router-link
                    v-if="wp.poiId"
                    :to="{ name: 'Home', query: { poiId: wp.poiId } }"
                    class="wp-link"
                  >查看 POI 详情</router-link>
                </div>
              </div>
            </div>
          </div>
        </template>

        <el-empty v-else description="路线不存在" />
      </div>
    </main>

    <Footer />
  </div>
</template>

<script setup>
import { nextTick, onMounted, onUnmounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import Header from '@/components/common/Header.vue'
import Footer from '@/components/common/Footer.vue'
import { useUserStore } from '@/stores/user'
import { getRouteDetail, toggleRouteLike, toggleRouteFavorite } from '@/api/userRoute'
import { loadAmapSdk, toAmapCoordinate } from '@/utils/amap'

const vueRoute = useRoute()
const userStore = useUserStore()
const loading = ref(true)
const detail = ref(null)
const mapRoot = ref(null)
const mapError = ref('')

let routeMap = null
let routeAMapRef = null

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

const initRouteMap = async () => {
  if (!mapRoot.value || !detail.value?.waypoints?.length) return
  mapError.value = ''
  try {
    routeAMapRef = await loadAmapSdk()
    const firstWp = detail.value.waypoints[0]
    const center = toAmapCoordinate(Number(firstWp.latitude), Number(firstWp.longitude))

    routeMap = new routeAMapRef.Map(mapRoot.value, {
      viewMode: '2D',
      zoom: 14,
      center: center ? [center.lng, center.lat] : [116.397428, 39.90923],
      resizeEnable: true,
      zooms: [3, 20]
    })

    const positions = []
    detail.value.waypoints.forEach((wp, idx) => {
      const coord = toAmapCoordinate(Number(wp.latitude), Number(wp.longitude))
      if (!coord) return
      const pos = [coord.lng, coord.lat]
      positions.push(pos)
      const marker = new routeAMapRef.Marker({
        position: pos,
        content: createMarkerContent(idx + 1),
        anchor: 'center',
        title: wp.poiName || wp.waypointName || `途经点 ${idx + 1}`
      })
      routeMap.add(marker)
    })

    if (positions.length >= 2) {
      const polyline = new routeAMapRef.Polyline({
        path: positions,
        strokeColor: '#1f8c69',
        strokeWeight: 4,
        strokeOpacity: 0.8,
        strokeStyle: 'solid',
        lineJoin: 'round',
        lineCap: 'round'
      })
      routeMap.add(polyline)
      routeMap.setFitView(null, false, [30, 30, 30, 30])
    }
  } catch (e) {
    mapError.value = '地图加载失败'
  }
}

const loadRoute = async (id) => {
  loading.value = true
  try {
    detail.value = await getRouteDetail(id)
  } catch (error) {
    ElMessage.error(error.message || '加载路线失败')
  } finally {
    loading.value = false       // 先关 loading，让地图容器渲染到 DOM
  }
  await nextTick()              // 等 DOM 更新完成
  initRouteMap()                // 此时 mapRoot.value 已有效
}

const handleLike = async () => {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    return
  }
  try {
    const result = await toggleRouteLike(detail.value.id)
    detail.value.liked = result.liked
    detail.value.likeCount += result.liked ? 1 : -1
  } catch (error) {
    ElMessage.error(error.message || '操作失败')
  }
}

const handleFavorite = async () => {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    return
  }
  try {
    const result = await toggleRouteFavorite(detail.value.id)
    detail.value.favorited = result.favorited
    detail.value.favoriteCount += result.favorited ? 1 : -1
  } catch (error) {
    ElMessage.error(error.message || '操作失败')
  }
}

onMounted(() => {
  const id = Number(vueRoute.params.id)
  if (id) loadRoute(id)
})

onUnmounted(() => {
  if (routeMap) {
    routeMap.destroy()
    routeMap = null
  }
})
</script>

<style scoped>
.route-detail-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.detail-main {
  flex: 1;
  padding: 22px 0 30px;
}

.detail-shell {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.route-hero {
  padding: 26px;
  border-radius: 28px;
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 20px;
}

.hero-content h1 {
  margin: 0;
  font-size: 26px;
  font-family: var(--font-serif);
  color: var(--ink-900);
}

.hero-summary {
  margin: 10px 0 0;
  color: var(--ink-500);
  font-size: 14px;
  line-height: 1.6;
}

.hero-author {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  margin-top: 14px;
  text-decoration: none;
  color: var(--ink-600);
  font-size: 14px;
}

.hero-actions {
  display: flex;
  gap: 10px;
  flex-shrink: 0;
}

/* 路线地图 */
.route-map-section {
  padding: 20px;
  border-radius: 22px;
  background: var(--front-panel);
  border: 1px solid var(--front-border);
  box-shadow: var(--front-shadow-soft);
}

.route-map-section h3 {
  margin: 0 0 14px;
  font-size: 18px;
  font-family: var(--font-serif);
  color: var(--ink-900);
}

.route-map-wrap {
  position: relative;
  border-radius: 16px;
  overflow: hidden;
  border: 1px solid var(--front-border);
}

.route-map {
  width: 100%;
  height: 420px;
}

.map-error {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(247, 250, 247, 0.9);
  font-size: 13px;
  color: var(--ink-400);
}

.route-desc {
  padding: 20px;
  border-radius: 22px;
  background: var(--front-panel);
  border: 1px solid var(--front-border);
  box-shadow: var(--front-shadow-soft);
}

.route-desc h3 {
  margin: 0 0 12px;
  font-size: 18px;
  font-family: var(--font-serif);
  color: var(--ink-900);
}

.route-desc p {
  margin: 0;
  color: var(--ink-600);
  line-height: 1.7;
  white-space: pre-wrap;
}

.waypoints-section {
  padding: 20px 20px 8px;
  border-radius: 22px;
  background: var(--front-panel);
  border: 1px solid var(--front-border);
  box-shadow: var(--front-shadow-soft);
}

.waypoints-section h3 {
  margin: 0 0 18px;
  font-size: 18px;
  font-family: var(--font-serif);
  color: var(--ink-900);
  display: flex;
  align-items: center;
  gap: 8px;
}

.wp-count-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 22px;
  height: 22px;
  padding: 0 6px;
  border-radius: 999px;
  background: var(--forest-100);
  color: var(--forest-700);
  font-size: 12px;
  font-weight: 700;
}

/* 时间轴容器 */
.wp-timeline {
  display: flex;
  flex-direction: column;
}

.wp-step {
  display: flex;
  gap: 14px;
  min-height: 44px;
}

/* 左侧：圆点 + 连接线 */
.wp-step-left {
  display: flex;
  flex-direction: column;
  align-items: center;
  flex-shrink: 0;
  width: 28px;
}

.wp-dot {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--forest-500), var(--forest-700));
  color: #fff;
  font-size: 12px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  box-shadow: 0 2px 8px rgba(31, 140, 105, 0.3);
}

.wp-dot--start {
  background: linear-gradient(135deg, #34d399, #059669);
  box-shadow: 0 2px 8px rgba(5, 150, 105, 0.35);
}

.wp-dot--end {
  background: linear-gradient(135deg, #fbbf24, #d97706);
  box-shadow: 0 2px 8px rgba(217, 119, 6, 0.35);
}

.wp-connector {
  flex: 1;
  width: 2px;
  min-height: 16px;
  margin: 3px 0;
  background: linear-gradient(180deg, var(--forest-300), var(--forest-200));
  border-radius: 1px;
}

/* 右侧：内容 */
.wp-step-body {
  padding: 4px 0 18px;
  display: flex;
  flex-direction: column;
  gap: 3px;
  min-width: 0;
}

.wp-step-body strong {
  font-size: 14px;
  font-weight: 600;
  color: var(--ink-900);
  line-height: 1.4;
}

.wp-coords {
  font-size: 11px;
  color: var(--ink-400);
  font-family: var(--font-mono);
}

.wp-link {
  font-size: 12px;
  color: var(--forest-600);
  text-decoration: none;
  width: fit-content;
}

.wp-link:hover {
  text-decoration: underline;
}

@media (max-width: 640px) {
  .route-hero {
    flex-direction: column;
    padding: 20px;
  }

  .hero-content h1 {
    font-size: 22px;
  }

  .hero-actions {
    width: 100%;
  }

  .hero-actions :deep(.el-button) {
    flex: 1;
  }

  .route-map {
    height: 280px;
  }
}
</style>
