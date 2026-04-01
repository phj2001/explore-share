<template>
  <section class="recommendation-section">
    <div class="section-head">
      <div>
        <span class="section-kicker">推荐打卡</span>
        <h2>精选地点分享</h2>
        <p>后台精选的优质分享会在这里展示，点击卡片可直接在地图中查看对应地点。</p>
      </div>
      <el-button text :loading="loading" @click="loadRecommendations">刷新推荐</el-button>
    </div>

    <div v-if="recommendations.length" class="recommendation-grid">
      <article
        v-for="item in recommendations"
        :key="item.recommendationId"
        class="recommendation-card"
        @click="focusPoi(item)"
      >
        <div class="card-cover-wrap">
          <el-image
            v-if="item.coverImageUrl"
            :src="resolveAssetUrl(item.coverImageUrl)"
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
            <el-avatar :size="36" :src="resolveAssetUrl(item.authorAvatarUrl) || undefined" class="author-avatar">
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

    <el-empty v-else v-loading="loading" description="当前还没有推荐内容" />
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

const poiStore = usePOIStore()
const mapStore = useMapStore()

const loadRecommendations = async () => {
  loading.value = true
  try {
    recommendations.value = await getRecommendedShareList()
  } catch (error) {
    ElMessage.error(error.message || '加载推荐内容失败')
  } finally {
    loading.value = false
  }
}

const focusPoi = async (item) => {
  try {
    let poi = poiStore.poiList.find((candidate) => candidate.id === item.poiId)
    if (!poi) {
      poi = await poiStore.fetchPOIById(item.poiId)
    }

    mapStore.selectPOI(poi)
    window.scrollTo({ top: 0, behavior: 'smooth' })
  } catch (error) {
    ElMessage.error(error.message || '定位推荐内容失败')
  }
}

const resolveAssetUrl = (value) => {
  if (!value) {
    return ''
  }

  if (/^https?:\/\//i.test(value)) {
    return value
  }

  return `${API_ORIGIN}${value.startsWith('/') ? value : `/${value}`}`
}

const getNameInitial = (value) => (value || 'U').slice(0, 1).toUpperCase()

onMounted(async () => {
  await loadRecommendations()
})
</script>

<style scoped>
.recommendation-section {
  padding: 32px 28px 42px;
  background:
    radial-gradient(circle at top right, rgba(14, 165, 233, 0.14), transparent 26%),
    linear-gradient(180deg, rgba(248, 250, 252, 0.95), rgba(241, 245, 249, 0.98));
}

.section-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 20px;
}

.section-kicker {
  display: inline-flex;
  padding: 6px 12px;
  border-radius: 999px;
  background: rgba(14, 165, 233, 0.1);
  color: #0284c7;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
}

.section-head h2 {
  margin: 14px 0 10px;
  color: #0f172a;
  font-size: clamp(28px, 4vw, 38px);
}

.section-head p {
  margin: 0;
  max-width: 720px;
  color: #64748b;
  line-height: 1.8;
}

.recommendation-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 18px;
}

.recommendation-card {
  overflow: hidden;
  border-radius: 28px;
  background: rgba(255, 255, 255, 0.94);
  border: 1px solid rgba(148, 163, 184, 0.16);
  box-shadow: 0 22px 48px rgba(15, 23, 42, 0.08);
  cursor: pointer;
  transition: transform 0.22s ease, box-shadow 0.22s ease;
}

.recommendation-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 28px 56px rgba(15, 23, 42, 0.14);
}

.card-cover-wrap {
  position: relative;
}

.card-cover {
  width: 100%;
  height: 220px;
  display: block;
}

.card-cover-placeholder {
  display: grid;
  place-items: center;
  background: linear-gradient(135deg, rgba(14, 165, 233, 0.18), rgba(37, 99, 235, 0.16));
  color: #0f172a;
  font-size: 22px;
  font-weight: 700;
}

.card-badges {
  position: absolute;
  left: 16px;
  right: 16px;
  bottom: 14px;
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.badge {
  display: inline-flex;
  padding: 6px 10px;
  border-radius: 999px;
  background: rgba(15, 23, 42, 0.78);
  color: #f8fafc;
  font-size: 12px;
}

.badge-soft {
  background: rgba(255, 255, 255, 0.92);
  color: #334155;
}

.card-body {
  padding: 18px;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.author-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.author-avatar {
  background: linear-gradient(135deg, #38bdf8, #2563eb);
  color: #fff;
  font-weight: 700;
}

.author-row strong {
  color: #0f172a;
}

.author-row p {
  margin: 4px 0 0;
  color: #64748b;
  font-size: 13px;
}

.recommend-text {
  margin: 0;
  padding: 12px 14px;
  border-radius: 18px;
  background: rgba(250, 204, 21, 0.12);
  color: #92400e;
  line-height: 1.65;
}

.content-preview {
  margin: 0;
  color: #334155;
  line-height: 1.75;
  min-height: 88px;
}

.card-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  color: #64748b;
  font-size: 13px;
}

.metric-group {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.card-action {
  color: #2563eb;
  font-weight: 600;
}

@media (max-width: 1200px) {
  .recommendation-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 720px) {
  .recommendation-section {
    padding: 22px 16px 30px;
  }

  .section-head,
  .card-footer {
    flex-direction: column;
    align-items: stretch;
  }

  .recommendation-grid {
    grid-template-columns: 1fr;
  }
}
</style>
