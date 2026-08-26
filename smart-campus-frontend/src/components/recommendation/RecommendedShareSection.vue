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

<style scoped lang="scss">
/* ── RecommendedShareSection 新设计系统 ── */
.recommendation-section {
  padding: 48px 0;
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

/* 刷新按钮 */
.refresh-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  border: 1px solid var(--front-border);
  background: transparent;
  color: var(--ink-500);
  cursor: pointer;
  flex-shrink: 0;
  transition: background 0.2s, color 0.2s, border-color 0.2s;
}

.refresh-btn:hover:not(:disabled) {
  background: var(--forest-50);
  color: var(--forest-700);
  border-color: var(--forest-700);
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

/* 推荐网格 */
.recommendation-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 20px;
}

/* 推荐卡片 */
.recommendation-card {
  overflow: hidden;
  border-radius: 14px;
  background: #fff;
  border: 1px solid var(--front-border);
  box-shadow: var(--front-shadow);
  cursor: pointer;
  transition: border-color 0.2s, transform 0.2s, box-shadow 0.2s;
}

.recommendation-card:hover {
  border-color: var(--forest-500);
  transform: translateY(-2px);
  box-shadow: 0 4px 20px rgba(20, 80, 55, 0.12);
}

/* 封面 */
.card-cover-wrap {
  position: relative;
  height: 200px;
  overflow: hidden;
}

.card-cover {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.card-cover-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--paper-100);
  color: var(--ink-500);
  font-family: var(--font-mono);
  font-size: 12px;
  height: 100%;
}

/* 徽章 */
.card-badges {
  position: absolute;
  bottom: 10px;
  left: 10px;
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.badge {
  display: inline-flex;
  padding: 4px 9px;
  border-radius: 999px;
  background: rgba(0, 0, 0, 0.6);
  color: #fff;
  font-family: var(--font-mono);
  font-size: 10.5px;
  backdrop-filter: blur(6px);
}

.badge-soft {
  background: rgba(31, 140, 105, 0.8);
  color: #fff;
}

/* 卡片正文 */
.card-body {
  padding: 16px 18px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

/* 作者行 */
.author-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.author-avatar {
  background: linear-gradient(135deg, var(--forest-500), var(--forest-700));
  color: #fff;
  font-weight: 700;
  flex-shrink: 0;
}

.author-row strong {
  display: block;
  font-family: var(--font-sans);
  font-size: 13.5px;
  color: var(--ink-900);
  font-weight: 600;
}

.author-row p {
  margin: 0;
  font-family: var(--font-mono);
  font-size: 10px;
  color: var(--ink-500);
}

/* 推荐语 */
.recommend-text {
  margin: 0;
  font-family: var(--font-serif);
  font-size: 13.5px;
  color: var(--ink-700);
  line-height: 1.6;
  font-style: italic;
}

/* 内容预览 */
.content-preview {
  margin: 0;
  font-family: var(--font-sans);
  font-size: 12.5px;
  color: var(--ink-600);
  line-height: 1.65;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

/* 卡片底栏 */
.card-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-top: 10px;
  border-top: 1px dashed var(--front-border);
}

.metric-group {
  display: flex;
  gap: 12px;
  font-family: var(--font-mono);
  font-size: 11px;
  color: var(--ink-400);
}

.card-action {
  font-family: var(--font-sans);
  font-size: 12px;
  color: var(--forest-700);
  font-weight: 500;
  cursor: pointer;
}

/* 响应式 */
@include respond-to(lg) {
  .recommendation-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@include respond-to(md) {
  .recommendation-section {
    padding: 32px 0;
  }

  .recommendation-grid {
    grid-template-columns: 1fr;
  }
}

@include respond-to(sm) {
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

  .recommendation-grid {
    gap: 12px;
  }

  .card-cover-wrap {
    height: 160px;
  }

  .card-body {
    padding: 12px 14px;
    gap: 8px;
  }
}

/* 触屏：刷新按钮撑足热区 */
@include coarse-pointer {
  .refresh-btn {
    width: 40px;
    height: 40px;
  }
}
</style>
