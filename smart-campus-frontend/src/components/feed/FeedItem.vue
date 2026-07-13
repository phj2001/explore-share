<template>
  <div class="feed-item">
    <router-link :to="'/user/' + item.userId" class="feed-author">
      <el-avatar :size="36" :src="item.avatarUrl || undefined" class="author-avatar">
        {{ (item.displayName || 'U').slice(0, 1).toUpperCase() }}
      </el-avatar>
      <div class="author-info">
        <strong>{{ item.displayName || item.username }}</strong>
        <time>{{ formatTime(item.createdAt) }}</time>
      </div>
    </router-link>

    <div class="feed-body">
      <router-link :to="{ name: 'Home', query: { poiId: item.poiId } }" class="feed-poi">
        {{ item.poiName }}
        <PoiCategoryBadge :category="item.poiCategory" size="small" />
      </router-link>

      <p v-if="item.content" class="feed-content">{{ item.content }}</p>

      <div v-if="item.imageUrls?.length" class="feed-images">
        <el-image
          v-for="(url, i) in item.imageUrls"
          :key="i"
          :src="url"
          :preview-src-list="item.imageUrls"
          fit="cover"
          preview-teleported
          class="feed-image"
        />
      </div>

      <div class="feed-stats">
        <span>{{ item.likeCount || 0 }} 赞</span>
        <span>{{ item.replyCount || 0 }} 回复</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import PoiCategoryBadge from '@/components/common/PoiCategoryBadge.vue'

defineProps({
  item: { type: Object, required: true }
})

const dateTimeFormatter = new Intl.DateTimeFormat('zh-CN', {
  year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit'
})

const formatTime = (value) => value ? dateTimeFormatter.format(new Date(value)) : ''
</script>

<style scoped>
/* ── FeedItem 新设计系统 ── */
.feed-item {
  padding: 18px 20px;
  border-radius: 14px;
  background: #fff;
  border: 1px solid var(--front-border);
  box-shadow: var(--front-shadow);
  display: flex;
  flex-direction: column;
  gap: 14px;
  transition: border-color 0.2s, box-shadow 0.2s;
}

.feed-item:hover {
  border-color: var(--forest-500);
  box-shadow: 0 4px 16px rgba(20, 80, 55, 0.10);
}

/* 作者行 */
.feed-author {
  display: flex;
  align-items: center;
  gap: 12px;
  text-decoration: none;
  color: inherit;
  border-radius: 8px;
  padding: 4px;
  transition: background 0.15s;
}

.feed-author:hover {
  background: var(--paper-100);
}

.author-avatar {
  background: linear-gradient(135deg, var(--forest-500), var(--forest-700));
  color: #fff;
  font-weight: 700;
  flex-shrink: 0;
}

.author-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.author-info strong {
  font-family: var(--font-sans);
  font-size: 14px;
  font-weight: 600;
  color: var(--ink-900);
}

.author-info time {
  font-family: var(--font-mono);
  font-size: 11px;
  color: var(--ink-400);
}

/* 内容区 */
.feed-body {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.feed-poi {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-family: var(--font-sans);
  font-size: 14px;
  font-weight: 600;
  color: var(--forest-700);
  text-decoration: none;
}

.feed-poi:hover {
  text-decoration: underline;
  color: var(--forest-800);
}

.poi-category {
  padding: 2px 8px;
  border-radius: 999px;
  background: rgba(31, 140, 105, 0.10);
  color: var(--forest-700);
  font-family: var(--font-mono);
  font-size: 10.5px;
  font-weight: 600;
}

.feed-content {
  margin: 0;
  font-family: var(--font-sans);
  color: var(--ink-700);
  font-size: 14px;
  line-height: 1.7;
  white-space: pre-wrap;
}

/* 图片组 */
.feed-images {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
}

.feed-image {
  width: 100%;
  height: 120px;
  border-radius: 10px;
  overflow: hidden;
}

/* 统计 */
.feed-stats {
  display: flex;
  gap: 16px;
  font-family: var(--font-mono);
  font-size: 11.5px;
  color: var(--ink-400);
}

/* 响应式 */
@media (max-width: 480px) {
  .feed-item {
    padding: 14px 16px;
    gap: 10px;
  }

  .feed-images {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .feed-image {
    height: 100px;
  }
}
</style>
