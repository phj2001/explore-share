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
.feed-section {
  padding: 24px 0;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
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
  background: #eff6ff;
  color: #2563eb;
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

.feed-login-hint {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px 18px;
  border-radius: 16px;
  background: #f8fafc;
  border: 1px solid var(--front-border);
  font-size: 13px;
  color: var(--front-text-soft);
}

.feed-empty-hint {
  padding: 20px;
  text-align: center;
  font-size: 13px;
  color: var(--front-text-muted);
}

.feed-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.load-more {
  display: flex;
  justify-content: center;
  padding-top: 8px;
}

@media (max-width: 640px) {
  .feed-section {
    padding: 20px 0;
  }

  .section-head h2 {
    font-size: 17px;
  }
}
</style>
