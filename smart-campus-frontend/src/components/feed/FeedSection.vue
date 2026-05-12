<template>
  <section class="front-shell feed-section">
    <div class="section-head">
      <div>
        <span class="section-kicker">关注动态</span>
        <h2>最新动态</h2>
        <p>查看你关注的用户最新打卡分享。</p>
      </div>
    </div>

    <div v-if="!isLoggedIn" class="feed-empty-hint">
      <el-empty description="登录后查看关注用户的动态">
        <router-link to="/login">
          <el-button type="primary">去登录</el-button>
        </router-link>
      </el-empty>
    </div>

    <template v-else>
      <el-skeleton v-if="loading && !items.length" :rows="5" animated />

      <div v-else-if="items.length" class="feed-list">
        <FeedItem v-for="item in items" :key="item.id" :item="item" />
      </div>

      <el-empty v-else-if="!loading" description="还没有动态，去关注一些用户吧" />

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
  if (isLoggedIn.value) {
    loadData(true)
  }
})
</script>

<style scoped>
.feed-section {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.section-head .section-kicker {
  display: inline-block;
  margin-bottom: 6px;
  padding: 3px 12px;
  border-radius: 999px;
  background: #eff6ff;
  color: #2563eb;
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

.feed-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.load-more {
  display: flex;
  justify-content: center;
  padding-top: 12px;
}

@media (max-width: 640px) {
  .section-head h2 {
    font-size: 20px;
  }
}
</style>
