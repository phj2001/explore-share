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

<style scoped lang="scss">
/* ── UserRouteSection 新设计系统 ── */
.user-route-section {
  padding: 48px 0;
  display: flex;
  flex-direction: column;
  gap: 0;
}

.section-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  padding-bottom: 24px;
  border-bottom: 1px solid var(--front-border);
  margin-bottom: 28px;
}

.head-left {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.section-kicker {
  display: inline-flex;
  padding: 3px 10px;
  border-radius: 999px;
  background: rgba(31, 140, 105, 0.10);
  color: var(--forest-700);
  font-family: var(--font-mono);
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.14em;
  text-transform: uppercase;
  width: fit-content;
}

.section-head h2 {
  margin: 0;
  font-family: var(--font-serif);
  font-size: 24px;
  font-weight: 700;
  color: var(--ink-900);
  letter-spacing: -0.02em;
  line-height: 1.25;
}

/* 路线网格 */
.routes-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 20px;
}

/* 空状态 CTA */
.empty-cta {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  padding: 48px 24px;
  border-radius: 14px;
  background: var(--paper-50);
  border: 1px dashed var(--front-border);
  text-align: center;
}

.empty-cta p {
  margin: 0;
  font-family: var(--font-mono);
  font-size: 12px;
  color: var(--ink-400);
  letter-spacing: 0.06em;
}

/* 加载更多 */
.load-more {
  text-align: center;
  margin-top: 24px;
}

/* 响应式 */
@include respond-to(md) {
  .routes-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@include respond-to(sm) {
  .user-route-section {
    padding: 32px 0;
  }

  .section-head {
    flex-direction: column;
    align-items: flex-start;
    gap: 16px;
    padding-bottom: 16px;
    margin-bottom: 20px;
  }

  .section-head h2 {
    font-size: 20px;
  }

  .routes-grid {
    grid-template-columns: 1fr;
    gap: 12px;
  }
}

/* 触屏：创建/加载更多等操作按钮热区 ≥40px（创建路线为核心入口） */
@include coarse-pointer {
  .section-head :deep(.el-button),
  .empty-cta :deep(.el-button),
  .load-more :deep(.el-button) {
    min-height: 40px;
  }

  .section-head :deep(.el-button--primary),
  .empty-cta :deep(.el-button--primary) {
    min-height: 44px;
  }
}
</style>
