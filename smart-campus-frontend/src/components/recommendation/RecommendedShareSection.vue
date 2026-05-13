<template>
  <section v-if="!dataLoaded || recommendations.length > 0" class="front-shell recommendation-section">
    <div class="section-head">
      <div class="head-left">
        <span class="section-kicker">推荐打卡</span>
        <h2>精选地点分享</h2>
      </div>
      <button class="refresh-btn" :disabled="loading" :title="'刷新推荐'" @click="loadRecommendations(true)">
        <svg class="refresh-icon" :class="{ spinning: loading }" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M23 4v6h-6M1 20v-6h6"/>
          <path d="M3.51 9a9 9 0 0114.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0020.49 15"/>
        </svg>
      </button>
    </div>

    <el-skeleton v-if="loading && !recommendations.length" :rows="4" animated />

    <div v-else-if="recommendations.length" class="recommendation-grid">
      <article
        v-for="item in recommendations"
        :key="item.recommendationId"
        class="recommendation-card"
        @click="focusPoi(item)"
      >
        <div class="card-cover-wrap">
          <el-image
            v-if="item.coverImageUrl"
            :src="resolveAssetUrl(item.coverThumbnailUrl || item.coverImageUrl)"
            lazy
            fit="cover"
            class="card-cover"
          />
          <div v-else class="card-cover card-cover-placeholder">
            <span>{{ item.poiName }}</span>
          </div>

          <div class="card-badges">
            <span class="badge">{{ item.poiName }}</span>
            <span class="badge badge-soft">{{ item.imageCount }} 张图</span>
          </div>
        </div>

        <div class="card-body">
          <div class="author-row">
            <el-avatar :size="32" :src="resolveAssetUrl(item.authorAvatarUrl) || undefined" class="author-avatar">
              {{ getNameInitial(item.authorDisplayName || item.authorUsername) }}
            </el-avatar>
            <div>
              <strong>{{ item.authorDisplayName || item.authorUsername }}</strong>
              <p>@{{ item.authorUsername }}</p>
            </div>
          </div>

          <p v-if="item.recommendationText" class="recommend-text">{{ item.recommendationText }}</p>
          <p class="content-preview">{{ item.contentPreview }}</p>

          <div class="card-footer">
            <div class="metric-group">
              <span>{{ item.likeCount || 0 }} 点赞</span>
              <span>{{ item.replyCount || 0 }} 回复</span>
            </div>
            <span class="card-action">在地图中查看</span>
          </div>
        </div>
      </article>
    </div>
  </section>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getRecommendedShareList } from '@/api/recommendedShare'
import { usePOIStore } from '@/stores/poi'
import { useMapStore } from '@/stores/map'
import { API_ORIGIN } from '@/utils/request'

const recommendations = ref([])
const loading = ref(false)
const dataLoaded = ref(false)

const poiStore = usePOIStore()
const mapStore = useMapStore()

const loadRecommendations = async (forceRefresh = false) => {
  loading.value = true
  try {
    recommendations.value = await getRecommendedShareList(undefined, { forceRefresh })
  } catch (error) {
    ElMessage.error(error.message || '加载推荐内容失败')
  } finally {
    loading.value = false
    dataLoaded.value = true
  }
}

const focusPoi = async (item) => {
  try {
    let poi = poiStore.getCachedPOIById(item.poiId)
    if (!poi) poi = await poiStore.fetchPOIById(item.poiId)
    mapStore.selectPOI(poi)
    window.scrollTo({ top: 0, behavior: 'smooth' })
  } catch (error) {
    ElMessage.error(error.message || '定位推荐内容失败')
  }
}

const resolveAssetUrl = (value) => {
  if (!value) return ''
  if (/^https?:\/\//i.test(value)) return value
  return `${API_ORIGIN}${value.startsWith('/') ? value : `/${value}`}`
}

const getNameInitial = (value) => (value || 'U').slice(0, 1).toUpperCase()

onMounted(() => loadRecommendations(false))
</script>

<style scoped>
.recommendation-section {
  padding: 24px 0;
}

.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 18px;
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
  background: var(--front-accent-soft);
  color: var(--front-accent-strong);
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.06em;
  flex-shrink: 0;
}

.section-head h2 {
  margin: 0;
  color: var(--front-text);
  font-size: 20px;
  font-weight: 700;
}

.refresh-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  border: 1px solid var(--front-border);
  background: transparent;
  color: var(--front-text-muted);
  cursor: pointer;
  flex-shrink: 0;
  transition: color 0.15s, background 0.15s;
}

.refresh-btn:hover:not(:disabled) {
  background: var(--front-accent-soft);
  color: var(--front-accent-strong);
  border-color: transparent;
}

.refresh-btn:disabled {
  opacity: 0.4;
  cursor: default;
}

.refresh-icon {
  width: 14px;
  height: 14px;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.spinning {
  animation: spin 0.8s linear infinite;
}

.recommendation-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 18px;
}

.recommendation-card {
  overflow: hidden;
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.96);
  border: 1px solid var(--front-border);
  box-shadow: var(--front-shadow-soft);
  cursor: pointer;
  transition: transform 0.22s ease, box-shadow 0.22s ease;
}

.recommendation-card:hover {
  transform: translateY(-4px);
  box-shadow: var(--front-shadow);
}

.card-cover-wrap {
  position: relative;
}

.card-cover {
  width: 100%;
  height: 200px;
  display: block;
}

.card-cover-placeholder {
  display: grid;
  place-items: center;
  background: linear-gradient(135deg, rgba(232, 245, 247, 0.96), rgba(255, 255, 255, 0.98));
  color: var(--front-text-soft);
  font-size: 18px;
  font-weight: 700;
}

.card-badges {
  position: absolute;
  left: 14px;
  right: 14px;
  bottom: 12px;
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.badge {
  display: inline-flex;
  padding: 5px 9px;
  border-radius: 999px;
  background: rgba(18, 41, 49, 0.72);
  color: #f8fafc;
  font-size: 11px;
}

.badge-soft {
  background: rgba(255, 255, 255, 0.9);
  color: var(--front-text-soft);
}

.card-body {
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.author-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.author-avatar {
  background: linear-gradient(135deg, var(--front-accent), var(--front-accent-strong));
  color: #fff;
  font-weight: 700;
  flex-shrink: 0;
}

.author-row strong {
  color: var(--front-text);
  font-size: 13px;
}

.author-row p {
  margin: 3px 0 0;
  color: var(--front-text-muted);
  font-size: 12px;
}

.recommend-text {
  margin: 0;
  padding: 10px 12px;
  border-radius: 14px;
  background: rgba(233, 244, 246, 0.9);
  color: var(--front-accent-strong);
  font-size: 13px;
  line-height: 1.6;
}

.content-preview {
  margin: 0;
  color: var(--front-text-soft);
  font-size: 13px;
  line-height: 1.7;
  min-height: 66px;
}

.card-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  color: var(--front-text-muted);
  font-size: 12px;
}

.metric-group {
  display: flex;
  gap: 10px;
}

.card-action {
  color: var(--front-accent-strong);
  font-weight: 600;
}

@media (max-width: 1200px) {
  .recommendation-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 720px) {
  .recommendation-section {
    padding: 20px 0;
  }

  .recommendation-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 560px) {
  .section-head {
    margin-bottom: 12px;
  }

  .section-head h2 {
    font-size: 17px;
  }

  .recommendation-grid {
    gap: 12px;
  }

  .recommendation-card {
    border-radius: 18px;
  }

  .card-cover {
    height: 160px;
  }

  .card-body {
    padding: 12px;
    gap: 10px;
  }

  .content-preview {
    min-height: auto;
  }
}
</style>
