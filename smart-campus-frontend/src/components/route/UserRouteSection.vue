<template>
  <section class="front-shell user-route-section">
    <div class="section-head">
      <div>
        <span class="section-kicker">社区路线</span>
        <h2>用户自创路线</h2>
        <p>发现其他探索者精心规划的游玩路线。</p>
      </div>
      <router-link v-if="isLoggedIn" to="/route/create">
        <el-button type="primary" size="small">创建路线</el-button>
      </router-link>
    </div>

    <el-skeleton v-if="loading && !routes.length" :rows="4" animated />

    <div v-else-if="routes.length" class="routes-grid">
      <UserRouteCard v-for="route in routes" :key="route.id" :route="route" />
    </div>

    <el-empty v-else description="还没有用户创建路线" />

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

onMounted(() => {
  loadData(true)
})
</script>

<style scoped>
.user-route-section {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.section-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px;
}

.section-head .section-kicker {
  display: inline-block;
  margin-bottom: 6px;
  padding: 3px 12px;
  border-radius: 999px;
  background: #fef3c7;
  color: #d97706;
  font-size: 12px;
  font-weight: 700;
}

.section-head h2 {
  margin: 0;
  font-size: 24px;
  color: #0f172a;
}

.section-head p {
  margin: 6px 0 0;
  color: #64748b;
  font-size: 14px;
}

.routes-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 18px;
}

.load-more {
  display: flex;
  justify-content: center;
  padding-top: 12px;
}

@media (max-width: 900px) {
  .routes-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 560px) {
  .routes-grid {
    grid-template-columns: 1fr;
  }

  .section-head h2 {
    font-size: 20px;
  }

  .section-head {
    flex-direction: column;
  }
}
</style>
