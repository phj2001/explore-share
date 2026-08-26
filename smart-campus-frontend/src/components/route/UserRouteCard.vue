<template>
  <router-link :to="'/route/' + route.id" class="route-card">
    <div v-if="route.coverImageUrl" class="route-cover">
      <el-image :src="route.coverImageUrl" fit="cover" lazy class="cover-img" />
    </div>
    <div class="route-body">
      <h3>{{ route.title }}</h3>
      <p v-if="route.summary">{{ route.summary }}</p>
      <div class="route-meta">
        <span class="route-author">
          <el-avatar :size="22" :src="route.avatarUrl || undefined" class="meta-avatar">
            {{ (route.displayName || 'U').slice(0, 1).toUpperCase() }}
          </el-avatar>
          {{ route.displayName || route.username }}
        </span>
        <span class="route-stats">
          {{ route.waypointCount }} 站 · {{ route.likeCount }} 赞 · {{ route.favoriteCount }} 收藏
        </span>
      </div>
      <span class="route-mode">{{ modeLabel }}</span>
    </div>
  </router-link>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  route: { type: Object, required: true }
})

const modeLabels = { walking: '步行', cycling: '骑行', driving: '驾车' }
const modeLabel = computed(() => modeLabels[props.route.defaultMode] || props.route.defaultMode)
</script>

<style scoped lang="scss">
/* ── UserRouteCard 新设计系统 ── */
.route-card {
  display: flex;
  flex-direction: column;
  border-radius: 14px;
  background: #fff;
  border: 1px solid var(--front-border);
  box-shadow: var(--front-shadow);
  overflow: hidden;
  text-decoration: none;
  color: inherit;
  transition: border-color 0.2s, transform 0.2s, box-shadow 0.2s;
}

.route-card:hover {
  border-color: var(--forest-500);
  transform: translateY(-2px);
  box-shadow: 0 4px 20px rgba(20, 80, 55, 0.12);
}

/* 封面 */
.route-cover {
  width: 100%;
  height: 160px;
  overflow: hidden;
}

.cover-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

/* 卡片主体 */
.route-body {
  padding: 16px 18px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.route-body h3 {
  margin: 0;
  font-family: var(--font-serif);
  font-size: 16px;
  font-weight: 700;
  color: var(--ink-900);
  letter-spacing: -0.01em;
  line-height: 1.3;
}

.route-body p {
  margin: 0;
  font-family: var(--font-sans);
  color: var(--ink-600);
  font-size: 12.5px;
  line-height: 1.6;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

/* 作者与数据 */
.route-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  flex-wrap: wrap;
  padding-top: 6px;
  border-top: 1px dashed var(--front-border);
}

.route-author {
  display: flex;
  align-items: center;
  gap: 6px;
  font-family: var(--font-sans);
  font-size: 12.5px;
  color: var(--ink-600);
  font-weight: 500;
}

.meta-avatar {
  background: linear-gradient(135deg, var(--forest-500), var(--forest-700));
  color: #fff;
  font-weight: 700;
  flex-shrink: 0;
}

.route-stats {
  font-family: var(--font-mono);
  font-size: 11px;
  color: var(--ink-400);
}

/* 出行模式 pill */
.route-mode {
  display: inline-flex;
  width: fit-content;
  padding: 3px 10px;
  border-radius: 999px;
  background: rgba(31, 140, 105, 0.10);
  color: var(--forest-700);
  font-family: var(--font-mono);
  font-size: 10.5px;
  font-weight: 600;
}
</style>
