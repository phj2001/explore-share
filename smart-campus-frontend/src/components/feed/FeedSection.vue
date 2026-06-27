<template>
  <section class="front-shell feed-section">
    <div class="section-head">
      <div class="head-left">
        <span class="section-kicker">关注动态</span>
        <h2>最新动态</h2>
      </div>
    </div>

    <div v-if="!isLoggedIn" class="feed-login-hint">
      <span>登录后查看关注用户的动态</span>
      <router-link to="/login">
        <el-button type="primary" size="small">去登录</el-button>
      </router-link>
    </div>

    <template v-else>
      <el-skeleton v-if="loading && !items.length" :rows="5" animated />

      <div v-else-if="items.length" class="feed-list">
        <FeedItem v-for="item in items" :key="item.id" :item="item" />
      </div>

      <div v-else-if="!loading" class="feed-empty-hint">
        还没有动态，去关注一些用户吧
      </div>

      <div v-if="hasMore" class="load-more">
        <el-button :loading="loadingMore" @click="loadMore">加载更多</el-button>
      </div>
    </template>
  </section>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useUserStore } from '@/stores/user'
import { getFeed } from '@/api/feed'
import FeedItem from './FeedItem.vue'

const userStore = useUserStore()
const isLoggedIn = computed(() => userStore.isLoggedIn)

const PAGE_SIZE = 10
const loading = ref(false)
const loadingMore = ref(false)
const items = ref([])
const page = ref(0)
const hasMore = ref(false)

const loadData = async (reset = false) => {
  const nextPage = reset ? 0 : page.value + 1
  const loadingRef = reset ? loading : loadingMore
  loadingRef.value = true

  try {
    const data = await getFeed({ page: nextPage, size: PAGE_SIZE })
    const records = data?.records || []
    items.value = reset ? records : [...items.value, ...records]
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
  if (isLoggedIn.value) loadData(true)
})
</script>

<style scoped>
/* ── FeedSection 新设计系统 ── */
.feed-section {
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
  margin-bottom: 24px;
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

/* 未登录提示 */
.feed-login-hint {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  padding: 20px 24px;
  border: 1px solid var(--front-border);
  border-radius: 12px;
  background: #fff;
  font-family: var(--font-sans);
  font-size: 13.5px;
  color: var(--ink-600);
}

/* 动态列表 */
.feed-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

/* 空状态 */
.feed-empty-hint {
  font-family: var(--font-mono);
  font-size: 12px;
  color: var(--ink-400);
  text-align: center;
  padding: 48px 0;
  letter-spacing: 0.08em;
}

/* 加载更多 */
.load-more {
  text-align: center;
  margin-top: 24px;
}

/* 响应式 */
@media (max-width: 640px) {
  .feed-section {
    padding: 32px 0;
  }

  .section-head h2 {
    font-size: 20px;
  }

  .feed-login-hint {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
    padding: 16px 18px;
  }
}
</style>
