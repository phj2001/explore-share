<template>
  <router-link :to="'/route/' + route.id" class="route-card">
    <div v-if="route.coverImageUrl" class="route-cover">
      <el-image :src="route.coverImageUrl" fit="cover" class="cover-img" />
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

<style scoped>
.route-card {
  display: flex;
  flex-direction: column;
  border-radius: 22px;
  background: #fff;
  box-shadow: 0 6px 20px rgba(15, 23, 42, 0.05);
  overflow: hidden;
  text-decoration: none;
  color: inherit;
  transition: transform 0.15s, box-shadow 0.15s;
}

.route-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 12px 32px rgba(15, 23, 42, 0.1);
}

.route-cover {
  width: 100%;
  height: 160px;
}

.cover-img {
  width: 100%;
  height: 100%;
}

.route-body {
  padding: 16px 18px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.route-body h3 {
  margin: 0;
  font-size: 16px;
  color: #0f172a;
}

.route-body p {
  margin: 0;
  color: #64748b;
  font-size: 13px;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.route-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  flex-wrap: wrap;
}

.route-author {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #475569;
}

.meta-avatar {
  background: linear-gradient(135deg, #38bdf8, #2563eb);
  color: #fff;
  font-weight: 700;
}

.route-stats {
  font-size: 12px;
  color: #94a3b8;
}

.route-mode {
  display: inline-flex;
  width: fit-content;
  padding: 3px 10px;
  border-radius: 999px;
  background: #eff6ff;
  color: #2563eb;
  font-size: 11px;
  font-weight: 700;
}
</style>
