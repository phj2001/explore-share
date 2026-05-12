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

          <div v-if="detail.description" class="route-desc">
            <h3>路线介绍</h3>
            <p>{{ detail.description }}</p>
          </div>

          <div class="waypoints-section">
            <h3>途经点 ({{ detail.waypoints?.length || 0 }})</h3>
            <div class="waypoints-list">
              <div v-for="(wp, idx) in detail.waypoints" :key="wp.id" class="waypoint-item">
                <span class="wp-marker">{{ idx + 1 }}</span>
                <div class="wp-body">
                  <strong>{{ wp.poiName || wp.waypointName || '未命名站点' }}</strong>
                  <span class="wp-coords">{{ wp.latitude }}, {{ wp.longitude }}</span>
                  <router-link
                    v-if="wp.poiId"
                    :to="{ name: 'Home', query: { poiId: wp.poiId } }"
                    class="wp-link"
                  >
                    查看POI详情
                  </router-link>
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
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import Header from '@/components/common/Header.vue'
import Footer from '@/components/common/Footer.vue'
import { useUserStore } from '@/stores/user'
import { getRouteDetail, toggleRouteLike, toggleRouteFavorite } from '@/api/userRoute'

const vueRoute = useRoute()
const userStore = useUserStore()
const loading = ref(true)
const detail = ref(null)

const loadRoute = async (id) => {
  loading.value = true
  try {
    detail.value = await getRouteDetail(id)
  } catch (error) {
    ElMessage.error(error.message || '加载路线失败')
  } finally {
    loading.value = false
  }
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
  color: #0f172a;
}

.hero-summary {
  margin: 10px 0 0;
  color: #64748b;
  font-size: 14px;
  line-height: 1.6;
}

.hero-author {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  margin-top: 14px;
  text-decoration: none;
  color: #475569;
  font-size: 14px;
}

.hero-actions {
  display: flex;
  gap: 10px;
  flex-shrink: 0;
}

.route-desc {
  padding: 20px;
  border-radius: 22px;
  background: #fff;
  box-shadow: 0 6px 20px rgba(15, 23, 42, 0.05);
}

.route-desc h3 {
  margin: 0 0 12px;
  font-size: 18px;
  color: #0f172a;
}

.route-desc p {
  margin: 0;
  color: #475569;
  line-height: 1.7;
  white-space: pre-wrap;
}

.waypoints-section {
  padding: 20px;
  border-radius: 22px;
  background: #fff;
  box-shadow: 0 6px 20px rgba(15, 23, 42, 0.05);
}

.waypoints-section h3 {
  margin: 0 0 16px;
  font-size: 18px;
  color: #0f172a;
}

.waypoints-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.waypoint-item {
  display: flex;
  align-items: flex-start;
  gap: 14px;
  padding: 14px 16px;
  border-radius: 16px;
  background: #f8fafc;
}

.wp-marker {
  width: 30px;
  height: 30px;
  border-radius: 50%;
  background: linear-gradient(135deg, #38bdf8, #2563eb);
  color: #fff;
  font-size: 13px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.wp-body {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.wp-body strong {
  color: #0f172a;
  font-size: 15px;
}

.wp-coords {
  color: #94a3b8;
  font-size: 12px;
  font-family: monospace;
}

.wp-link {
  color: #0ea5e9;
  font-size: 13px;
  text-decoration: none;
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
}
</style>
