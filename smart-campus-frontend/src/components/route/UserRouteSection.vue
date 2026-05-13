<template>
  <section class="front-shell user-route-section">
    <div class="section-head">
      <div class="head-left">
        <span class="section-kicker">社区路线</span>
        <h2>用户自创路线</h2>
      </div>
      <router-link v-if="isLoggedIn" to="/route/create">
        <el-button type="primary" size="small">创建路线</el-button>
      </router-link>
    </div>

    <el-skeleton v-if="loading && !routes.length" :rows="4" animated />

    <div v-else-if="routes.length" class="routes-grid">
      <UserRouteCard v-for="route in routes" :key="route.id" :route="route" />
    </div>

    <div v-else class="empty-cta">
      <p>还没有用户创建的路线，成为第一个探索者</p>
      <router-link v-if="isLoggedIn" to="/route/create">
        <el-button type="primary">创建第一条路线</el-button>
      </router-link>
      <router-link v-else to="/login">
        <el-button type="primary">登录后创建</el-button>
      </router-link>
    </div>

    <div v-if="hasMore" class="load-more">
      <el-button :loading="loadingMore" @click="loadMore">加载更多</el-button>
    </div>
  </section>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useUserStore } from '@/stores/user'
import { getPublicRoutes } from '@/api/userRoute'
import UserRouteCard from './UserRouteCard.vue'

const userStore = useUserStore()
const isLoggedIn = computed(() => userStore.isLoggedIn)

const PAGE_SIZE = 6
const loading = ref(false)
const loadingMore = ref(false)
const routes = ref([])
const page = ref(0)
const hasMore = ref(false)

const loadData = async (reset = false) => {
  const nextPage = reset ? 0 : page.value + 1
  const loadingRef = reset ? loading : loadingMore
  loadingRef.value = true

  try {
    const data = await getPublicRoutes({ page: nextPage, size: PAGE_SIZE })
    const records = data?.records || []
    routes.value = reset ? records : [...routes.value, ...records]
    page.value = data?.page || nextPage
    hasMore.value = Boolean(data?.hasNext)
  } catch {
    // 静默
  } finally {
    loadingRef.value = false
  }
}

const loadMore = () => loadData(false)

onMounted(() => loadData(true))
</script>

<style scoped>
.user-route-section {
  padding: 24px 0;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
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
  background: #fef3c7;
  color: #d97706;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.06em;
  flex-shrink: 0;
}

.section-head h2 {
  margin: 0;
  font-size: 20px;
  font-weight: 700;
  color: #0f172a;
}

.routes-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 18px;
}

.empty-cta {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 14px;
  padding: 28px 20px;
  border-radius: 20px;
  background: #fffbeb;
  border: 1px dashed rgba(217, 119, 6, 0.3);
  text-align: center;
}

.empty-cta p {
  margin: 0;
  color: var(--front-text-soft);
  font-size: 13px;
}

.load-more {
  display: flex;
  justify-content: center;
  padding-top: 4px;
}

@media (max-width: 900px) {
  .routes-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 560px) {
  .user-route-section {
    padding: 20px 0;
  }

  .routes-grid {
    grid-template-columns: 1fr;
  }

  .section-head h2 {
    font-size: 17px;
  }
}
</style>
