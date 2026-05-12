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
        <span class="poi-category">{{ item.poiCategory }}</span>
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
defineProps({
  item: { type: Object, required: true }
})

const dateTimeFormatter = new Intl.DateTimeFormat('zh-CN', {
  year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit'
})

const formatTime = (value) => value ? dateTimeFormatter.format(new Date(value)) : ''
</script>

<style scoped>
.feed-item {
  padding: 18px 20px;
  border-radius: 22px;
  background: #fff;
  box-shadow: 0 6px 20px rgba(15, 23, 42, 0.05);
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.feed-author {
  display: flex;
  align-items: center;
  gap: 12px;
  text-decoration: none;
  color: inherit;
  border-radius: 12px;
  padding: 4px;
  transition: background 0.15s;
}

.feed-author:hover {
  background: #f1f5f9;
}

.author-avatar {
  background: linear-gradient(135deg, #38bdf8, #2563eb);
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
  font-size: 14px;
  color: #0f172a;
}

.author-info time {
  font-size: 12px;
  color: #94a3b8;
}

.feed-body {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.feed-poi {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  font-weight: 600;
  color: #0ea5e9;
  text-decoration: none;
}

.feed-poi:hover {
  text-decoration: underline;
}

.poi-category {
  padding: 2px 8px;
  border-radius: 999px;
  background: #ecfdf5;
  color: #059669;
  font-size: 11px;
  font-weight: 700;
}

.feed-content {
  margin: 0;
  color: #1e293b;
  font-size: 14px;
  line-height: 1.7;
  white-space: pre-wrap;
}

.feed-images {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
}

.feed-image {
  width: 100%;
  height: 120px;
  border-radius: 14px;
  overflow: hidden;
}

.feed-stats {
  display: flex;
  gap: 16px;
  color: #64748b;
  font-size: 13px;
}

@media (max-width: 480px) {
  .feed-images {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .feed-image {
    height: 100px;
  }
}
</style>
