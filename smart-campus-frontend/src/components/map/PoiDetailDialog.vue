<template>
  <el-dialog
    v-model="visible"
    :title="poi?.name"
    :width="mobile ? '100%' : '920px'"
    :fullscreen="mobile"
    :top="mobile ? '0' : '8vh'"
    destroy-on-close
    class="poi-dialog"
    @closed="handleDialogClosed"
  >
    <div v-if="poi" class="poi-dialog-content">
      <section class="poi-overview">
        <div class="poi-overview-card">
          <PoiCategoryBadge :category="poi.category" />
          <h3>{{ poi.name }}</h3>
          <p>{{ poi.description || '这个地点暂时还没有详细介绍。' }}</p>
        </div>

        <div class="poi-meta-grid">
          <div class="meta-card">
            <span>纬度</span>
            <strong>{{ poi.latitude }}</strong>
          </div>
          <div class="meta-card">
            <span>经度</span>
            <strong>{{ poi.longitude }}</strong>
          </div>
        </div>

        <div class="poi-actions">
          <el-button @click="setAsRoutePoint('start')">设为起点</el-button>
          <el-button type="primary" plain @click="setAsRoutePoint('end')">设为终点</el-button>
        </div>

        <div class="poi-check-in-card">
          <div class="poi-check-in-meta">
            <span class="poi-check-in-label">打卡状态</span>
            <strong>{{ poiCheckInStatus.checkInCount }} 人打卡</strong>
            <p>
              {{ poiCheckInStatus.checkedIn ? '你已经打卡过这里了。' : '到过这里的话，可以点一下打卡。' }}
            </p>
          </div>

          <el-button
            :type="poiCheckInStatus.checkedIn ? 'success' : 'primary'"
            :plain="poiCheckInStatus.checkedIn"
            :loading="poiCheckInLoading"
            @click="togglePOICheckIn"
          >
            {{ poiCheckInStatus.checkedIn ? '取消打卡' : (userStore.isLoggedIn ? '打卡' : '登录后打卡') }}
          </el-button>
        </div>

        <div class="poi-check-in-card">
          <div class="poi-check-in-meta">
            <span class="poi-check-in-label">收藏状态</span>
            <strong>{{ poiFavoriteStatus.favoriteCount }} 人收藏</strong>
            <p>
              {{ poiFavoriteStatus.favorited ? '你已经收藏了这个地点。' : '感兴趣的话，可以收藏起来。' }}
            </p>
          </div>

          <el-button
            :type="poiFavoriteStatus.favorited ? 'danger' : 'primary'"
            :plain="poiFavoriteStatus.favorited"
            :loading="poiFavoriteLoading"
            @click="togglePOIFavorite"
          >
            {{ poiFavoriteStatus.favorited ? '取消收藏' : (userStore.isLoggedIn ? '收藏' : '登录后收藏') }}
          </el-button>
        </div>
      </section>

      <div class="poi-review-section">
        <div class="poi-review-header">
          <div>
            <h3>评分与评价</h3>
            <p v-if="poiRatingSummary.reviewCount > 0">
              平均 {{ poiRatingSummary.avgRating }} 分 · {{ poiRatingSummary.reviewCount }} 条评价
            </p>
            <p v-else>暂无评价，来做第一个评价的人吧</p>
          </div>
        </div>

        <div v-if="userStore.isLoggedIn" class="poi-review-composer">
          <div class="rating-stars">
            <span class="rating-label">我的评分：</span>
            <span
              v-for="star in 5"
              :key="star"
              class="star-btn"
              :class="{ active: star <= (poiHoverRating || poiUserRating) }"
              @mouseenter="poiHoverRating = star"
              @mouseleave="poiHoverRating = 0"
              @click="poiUserRating = star"
            >&#9733;</span>
            <span class="rating-text">{{ poiUserRating > 0 ? poiUserRating + ' 星' : '点击评分' }}</span>
          </div>
          <el-input
            v-model="poiReviewContent"
            type="textarea"
            :rows="2"
            maxlength="200"
            show-word-limit
            resize="none"
            placeholder="写下你的评价（可选）"
          />
          <div class="review-composer-actions">
            <span></span>
            <el-button
              type="primary"
              :disabled="poiUserRating === 0"
              :loading="poiReviewSubmitting"
              @click="submitReview"
            >
              提交评价
            </el-button>
          </div>
        </div>

        <div v-else class="poi-review-login-tip">
          <span>登录后即可评分和评价</span>
          <el-button type="primary" size="small" @click="router.push({ name: 'Login' })">去登录</el-button>
        </div>

        <div v-if="poiReviewList.length" class="poi-review-list">
          <div v-for="review in poiReviewList" :key="review.id" class="poi-review-item">
            <div class="review-item-head">
              <el-avatar :size="32" :src="review.authorAvatarUrl || undefined" class="review-avatar">
                {{ (review.authorDisplayName || 'U').slice(0, 1).toUpperCase() }}
              </el-avatar>
              <div class="review-item-meta">
                <div class="review-item-author">
                  <strong>{{ review.authorDisplayName }}</strong>
                  <span class="review-stars">
                    <span v-for="s in 5" :key="s" :class="{ filled: s <= review.rating }">&#9733;</span>
                  </span>
                </div>
                <time>{{ formatReviewTime(review.createdAt) }}</time>
              </div>
              <el-button
                v-if="review.canDelete"
                text
                type="danger"
                size="small"
                @click="removeReview(review)"
              >
                删除
              </el-button>
            </div>
            <p v-if="review.content" class="review-item-content">{{ review.content }}</p>
          </div>

          <div v-if="poiReviewHasMore" class="review-more">
            <el-button text :loading="poiReviewLoading" @click="loadMoreReviews">加载更多评价</el-button>
          </div>
        </div>
      </div>

      <PoiSharePanel v-if="visible" :poi="poi" />

      <div v-if="visible" class="poi-gallery-section">
        <div class="gallery-head">
          <h3>图片墙</h3>
          <span class="gallery-count">{{ galleryTotal }} 张图片</span>
        </div>
        <el-skeleton v-if="galleryLoading && !galleryImages.length" :rows="2" animated />
        <div v-else-if="galleryImages.length" class="gallery-grid">
          <el-image
            v-for="img in galleryImages"
            :key="img.imageId"
            :src="resolveMediaUrl(img.imageUrl)"
            :preview-src-list="galleryPreviewUrls"
            :initial-index="galleryImages.indexOf(img)"
            fit="cover"
            lazy
            preview-teleported
            class="gallery-thumb"
          >
            <template #error>
              <div class="gallery-error">
                <span>加载失败</span>
              </div>
            </template>
          </el-image>
        </div>
        <el-empty v-else description="暂无图片" :image-size="60" />
        <div v-if="galleryHasMore" class="gallery-more">
          <el-button size="small" :loading="galleryLoadingMore" @click="loadMoreGallery">加载更多</el-button>
        </div>
      </div>
    </div>

    <template #footer>
      <div class="dialog-actions">
        <el-button @click="visible = false">关闭</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed, defineAsyncComponent, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { useMapStore } from '@/stores/map'
import { cancelCheckInPOI, checkInPOI, getPOICheckInStatus } from '@/api/poiCheckIn'
import { addFavorite, getFavoriteStatus, removeFavorite } from '@/api/poiFavorite'
import { createOrUpdateReview, deleteReview, getPoiReviews, getRatingSummary } from '@/api/poiReview'
import { getPoiGallery } from '@/api/poiGallery'
import { toAmapCoordinate } from '@/utils/amap'
import { API_ORIGIN } from '@/utils/request'
import PoiCategoryBadge from '@/components/common/PoiCategoryBadge.vue'

const PoiSharePanel = defineAsyncComponent(() => import('@/components/map/PoiSharePanel.vue'))

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false
  },
  poi: {
    type: Object,
    default: null
  },
  mobile: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['update:modelValue', 'closed'])

const visible = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value)
})

const router = useRouter()
const userStore = useUserStore()
const mapStore = useMapStore()

const API_ORIGIN_RESOLVED = API_ORIGIN

const resolveMediaUrl = (value) => {
  if (!value) return ''
  if (/^https?:\/\//i.test(value)) return value
  return `${API_ORIGIN_RESOLVED}${value.startsWith('/') ? value : `/${value}`}`
}

const poiCheckInLoading = ref(false)
const poiCheckInStatus = ref({
  checkedIn: false,
  checkInCount: 0
})
const poiFavoriteLoading = ref(false)
const poiFavoriteStatus = ref({
  favorited: false,
  favoriteCount: 0
})
const poiRatingSummary = ref({ avgRating: 0, reviewCount: 0 })
const poiReviewList = ref([])
const poiReviewTotal = ref(0)
const poiReviewHasMore = ref(false)
const poiReviewPage = ref(0)
const poiReviewLoading = ref(false)
const poiReviewSubmitting = ref(false)
const poiUserRating = ref(0)
const poiReviewContent = ref('')
const poiHoverRating = ref(0)
const galleryImages = ref([])
const galleryLoading = ref(false)
const galleryLoadingMore = ref(false)
const galleryPage = ref(0)
const galleryHasMore = ref(false)
const galleryTotal = ref(0)

const galleryPreviewUrls = computed(() => galleryImages.value.map(img => resolveMediaUrl(img.imageUrl)))

const setAsRoutePoint = (type) => {
  if (!props.poi) return

  const mapCoordinate = toAmapCoordinate(props.poi.latitude, props.poi.longitude)
  if (!mapCoordinate) {
    ElMessage.error('地点坐标无效')
    return
  }

  const point = {
    poiId: props.poi.id,
    name: props.poi.name,
    lat: mapCoordinate.lat,
    lng: mapCoordinate.lng,
    rawLat: Number(props.poi.latitude),
    rawLng: Number(props.poi.longitude),
    isTemporary: false
  }

  if (type === 'start') {
    mapStore.setRouteStart(point)
    ElMessage.success(`已将 ${point.name} 设为起点`)
  } else {
    mapStore.setRouteEnd(point)
    ElMessage.success(`已将 ${point.name} 设为终点`)
  }

  visible.value = false
}

const resetPOICheckInState = () => {
  poiCheckInLoading.value = false
  poiCheckInStatus.value = {
    checkedIn: false,
    checkInCount: 0
  }
}

const loadGallery = async (poiId, reset = false) => {
  if (!poiId) return
  const nextPage = reset ? 0 : galleryPage.value + 1
  const loadingRef = reset ? galleryLoading : galleryLoadingMore
  loadingRef.value = true
  try {
    const data = await getPoiGallery(poiId, { page: nextPage, size: 20 })
    const records = data?.records || []
    galleryImages.value = reset ? records : [...galleryImages.value, ...records]
    galleryPage.value = data?.page || nextPage
    galleryHasMore.value = Boolean(data?.hasNext)
    galleryTotal.value = data?.total || 0
  } catch {
    // 静默
  } finally {
    loadingRef.value = false
  }
}

const loadMoreGallery = () => {
  if (props.poi?.id) loadGallery(props.poi.id, false)
}

const loadPOICheckInStatus = async (poiId) => {
  if (!poiId) {
    resetPOICheckInState()
    return
  }

  try {
    poiCheckInStatus.value = await getPOICheckInStatus(poiId)
  } catch (error) {
    resetPOICheckInState()
    ElMessage.error(error.message || '打卡状态加载失败')
  }
}

const togglePOICheckIn = async () => {
  if (!props.poi?.id) {
    return
  }

  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录后再打卡')
    return
  }

  poiCheckInLoading.value = true

  try {
    poiCheckInStatus.value = poiCheckInStatus.value.checkedIn
      ? await cancelCheckInPOI(props.poi.id)
      : await checkInPOI(props.poi.id)

    ElMessage.success(poiCheckInStatus.value.checkedIn ? '打卡成功' : '已取消打卡')
  } catch (error) {
    ElMessage.error(error.message || '打卡操作失败')
  } finally {
    poiCheckInLoading.value = false
  }
}

const handleDialogClosed = () => {
  resetPOICheckInState()
  resetPOIFavoriteState()
  resetReviewState()
  emit('closed')
}

const resetPOIFavoriteState = () => {
  poiFavoriteLoading.value = false
  poiFavoriteStatus.value = {
    favorited: false,
    favoriteCount: 0
  }
}

const REVIEW_PAGE_SIZE = 5

const resetReviewState = () => {
  poiRatingSummary.value = { avgRating: 0, reviewCount: 0 }
  poiReviewList.value = []
  poiReviewTotal.value = 0
  poiReviewHasMore.value = false
  poiReviewPage.value = 0
  poiReviewLoading.value = false
  poiReviewSubmitting.value = false
  poiUserRating.value = 0
  poiReviewContent.value = ''
  poiHoverRating.value = 0
}

const reviewTimeFormatter = new Intl.DateTimeFormat('zh-CN', {
  year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit'
})

const formatReviewTime = (value) => value ? reviewTimeFormatter.format(new Date(value)) : ''

const loadRatingSummary = async (poiId) => {
  if (!poiId) return
  try {
    poiRatingSummary.value = await getRatingSummary(poiId)
  } catch {
    // 静默
  }
}

const loadReviews = async (poiId, reset = false) => {
  if (!poiId) return
  const nextPage = reset ? 0 : poiReviewPage.value + 1
  poiReviewLoading.value = true
  try {
    const data = await getPoiReviews(poiId, { page: nextPage, size: REVIEW_PAGE_SIZE })
    const records = data?.records || []
    poiReviewList.value = reset ? records : [...poiReviewList.value, ...records]
    poiReviewTotal.value = data?.total || 0
    poiReviewPage.value = data?.page || nextPage
    poiReviewHasMore.value = Boolean(data?.hasNext)
  } catch {
    // 静默
  } finally {
    poiReviewLoading.value = false
  }
}

const loadMoreReviews = async () => {
  if (!props.poi?.id) return
  await loadReviews(props.poi.id, false)
}

const submitReview = async () => {
  if (!props.poi?.id || poiUserRating.value === 0) return

  poiReviewSubmitting.value = true
  try {
    await createOrUpdateReview(props.poi.id, {
      rating: poiUserRating.value,
      content: poiReviewContent.value.trim() || undefined
    })
    ElMessage.success('评价提交成功')
    poiReviewContent.value = ''
    await loadRatingSummary(props.poi.id)
    await loadReviews(props.poi.id, true)
  } catch (error) {
    ElMessage.error(error.message || '评价提交失败')
  } finally {
    poiReviewSubmitting.value = false
  }
}

const removeReview = async (review) => {
  try {
    await ElMessageBox.confirm('删除后无法恢复，是否继续？', '删除评价', {
      type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消'
    })
  } catch {
    return
  }

  try {
    await deleteReview(review.id)
    ElMessage.success('评价已删除')
    await loadRatingSummary(props.poi.id)
    await loadReviews(props.poi.id, true)
  } catch (error) {
    ElMessage.error(error.message || '删除评价失败')
  }
}

const loadPOIFavoriteStatus = async (poiId) => {
  if (!poiId) {
    resetPOIFavoriteState()
    return
  }

  try {
    poiFavoriteStatus.value = await getFavoriteStatus(poiId)
  } catch {
    resetPOIFavoriteState()
  }
}

const togglePOIFavorite = async () => {
  if (!props.poi?.id) {
    return
  }

  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录后再收藏')
    return
  }

  poiFavoriteLoading.value = true
  try {
    poiFavoriteStatus.value = poiFavoriteStatus.value.favorited
      ? await removeFavorite(props.poi.id)
      : await addFavorite(props.poi.id)

    ElMessage.success(poiFavoriteStatus.value.favorited ? '收藏成功' : '已取消收藏')
  } catch (error) {
    ElMessage.error(error.message || '收藏操作失败')
  } finally {
    poiFavoriteLoading.value = false
  }
}

// 弹窗打开（或切换地点）时加载互动数据；地点清空时重置本地状态
watch(
  () => [visible.value, props.poi?.id],
  ([isVisible, poiId]) => {
    if (isVisible && poiId) {
      loadPOICheckInStatus(poiId)
      loadPOIFavoriteStatus(poiId)
      loadRatingSummary(poiId)
      loadReviews(poiId, true)
      loadGallery(poiId, true)
      return
    }

    if (!poiId) {
      resetPOICheckInState()
      resetPOIFavoriteState()
      resetReviewState()
    }
  }
)

// 登录状态变化时刷新打卡/收藏（评价列表无需重载）
watch(
  () => userStore.isLoggedIn,
  () => {
    if (visible.value && props.poi?.id) {
      loadPOICheckInStatus(props.poi.id)
      loadPOIFavoriteStatus(props.poi.id)
    }
  }
)
</script>

<style scoped lang="scss">
.poi-dialog-content {
  display: flex;
  flex-direction: column;
  gap: 22px;
}

.poi-overview {
  padding: 24px;
  border-radius: 28px;
  background: linear-gradient(135deg, rgba(209, 237, 224, 0.92), rgba(247, 250, 247, 0.96));
}

.poi-overview-card h3 {
  margin: 14px 0 8px;
  font-size: 24px;
  color: var(--front-text);
}

.poi-overview-card p {
  margin: 0;
  color: var(--front-text-soft);
  line-height: 1.7;
}

.poi-category {
  display: inline-flex;
  padding: 6px 12px;
  border-radius: 999px;
  background: var(--front-accent-soft);
  color: var(--front-accent-strong);
  font-size: 12px;
  font-weight: 700;
}

.poi-meta-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
  margin-top: 18px;
}

.meta-card {
  padding: 16px 18px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.84);
  border: 1px solid var(--front-border);
}

.meta-card span {
  display: block;
  color: var(--front-text-muted);
  font-size: 12px;
}

.meta-card strong {
  display: block;
  margin-top: 8px;
  color: var(--front-text);
  font-size: 16px;
}

.poi-actions {
  margin-top: 18px;
  display: flex;
  gap: 12px;
}

.poi-check-in-card {
  margin-top: 18px;
  padding: 18px;
  border-radius: 20px;
  border: 1px solid var(--front-border);
  background: rgba(255, 255, 255, 0.86);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.poi-check-in-meta {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.poi-check-in-label {
  color: var(--front-text-muted);
  font-size: 12px;
}

.poi-check-in-meta strong {
  color: var(--front-text);
  font-size: 18px;
}

.poi-check-in-meta p {
  margin: 0;
  color: var(--front-text-muted);
  font-size: 13px;
  line-height: 1.5;
}

.dialog-actions {
  display: flex;
  justify-content: flex-end;
}

.poi-review-section {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.poi-gallery-section {
  margin-top: 20px;
  padding-top: 18px;
  border-top: 1px solid var(--front-border);
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.gallery-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.gallery-head h3 {
  margin: 0;
  font-size: 16px;
  font-family: var(--font-serif);
  color: var(--ink-900);
}

.gallery-count {
  color: var(--ink-400);
  font-size: 13px;
}

.gallery-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 8px;
}

.gallery-thumb {
  width: 100%;
  height: 100px;
  border-radius: 12px;
  overflow: hidden;
}

.gallery-error {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--paper-100);
  color: var(--ink-400);
  font-size: 12px;
}

.gallery-more {
  display: flex;
  justify-content: center;
}

@include respond-to(sm) {
  .gallery-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .gallery-thumb {
    height: 80px;
  }
}

.poi-review-header h3 {
  margin: 0;
  font-size: 20px;
  color: var(--front-text);
}

.poi-review-header p {
  margin: 6px 0 0;
  color: var(--front-text-muted);
  font-size: 13px;
}

.poi-review-composer {
  padding: 16px 18px;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.86);
  border: 1px solid var(--front-border);
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.rating-stars {
  display: flex;
  align-items: center;
  gap: 4px;
}

.rating-label {
  color: var(--front-text-soft);
  font-size: 13px;
  margin-right: 6px;
}

.star-btn {
  font-size: 24px;
  color: #d1d5db;
  cursor: pointer;
  transition: color 0.15s;
  user-select: none;
}

.star-btn.active {
  color: #f59e0b;
}

.rating-text {
  margin-left: 8px;
  color: var(--front-text-muted);
  font-size: 13px;
}

.review-composer-actions {
  display: flex;
  justify-content: flex-end;
}

.poi-review-login-tip {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 18px;
  border-radius: 18px;
  background: rgba(248, 250, 252, 1);
  border: 1px solid var(--front-border);
}

.poi-review-login-tip span {
  color: var(--front-text-muted);
  font-size: 13px;
}

.poi-review-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.poi-review-item {
  padding: 14px 16px;
  border-radius: 16px;
  background: var(--paper-50);
}

.review-item-head {
  display: flex;
  align-items: center;
  gap: 10px;
}

.review-avatar {
  background: linear-gradient(135deg, var(--forest-500), var(--forest-700));
  color: #fff;
  font-weight: 700;
}

.review-item-meta {
  flex: 1;
  min-width: 0;
}

.review-item-author {
  display: flex;
  align-items: center;
  gap: 8px;
}

.review-item-author strong {
  color: var(--front-text);
  font-size: 14px;
}

.review-stars span {
  font-size: 14px;
  color: #d1d5db;
}

.review-stars span.filled {
  color: #f59e0b;
}

.review-item-meta time {
  display: block;
  margin-top: 2px;
  color: var(--front-text-muted);
  font-size: 12px;
}

.review-item-content {
  margin: 10px 0 0;
  color: var(--front-text-soft);
  font-size: 14px;
  line-height: 1.6;
  white-space: pre-wrap;
}

.review-more {
  display: flex;
  justify-content: center;
  padding-top: 4px;
}

:deep(.poi-dialog .el-dialog) {
  max-width: min(920px, calc(100vw - 32px));
  border-radius: 28px;
}

:deep(.poi-dialog .el-dialog__body) {
  padding-top: 10px;
}

@include respond-to(md) {
  .poi-overview {
    padding: 18px;
    border-radius: 22px;
  }

  .poi-overview-card h3 {
    font-size: 22px;
  }

  .poi-meta-grid {
    grid-template-columns: 1fr;
  }

  .poi-actions {
    flex-direction: column;
  }

  .poi-actions :deep(.el-button) {
    width: 100%;
    min-height: 42px;
  }

  /* Element Plus 相邻按钮自带 margin-left:12px（为横排设计），竖排下会让
     "设为终点"右移错位且溢出容器，这里清零交由 gap 控制间距 */
  .poi-actions :deep(.el-button + .el-button) {
    margin-left: 0;
  }

  .poi-check-in-card {
    flex-direction: column;
    align-items: stretch;
  }

  .poi-check-in-card :deep(.el-button) {
    width: 100%;
    min-height: 42px;
  }

  .dialog-actions :deep(.el-button) {
    width: 100%;
    min-height: 42px;
  }

  :deep(.poi-dialog .el-dialog) {
    width: 100% !important;
    max-width: none;
    height: 100vh;
    height: 100svh;
    margin: 0;
    border-radius: 0;
  }

  :deep(.poi-dialog .el-dialog__header) {
    padding: 16px 16px 12px;
  }

  :deep(.poi-dialog .el-dialog__body) {
    padding: 0 16px 16px;
    max-height: calc(100vh - 128px);
    max-height: calc(100svh - 128px);
    overflow-y: auto;
    -webkit-overflow-scrolling: touch;
  }

  :deep(.poi-dialog .el-dialog__footer) {
    padding: 8px 16px 16px;
  }
}

@include respond-to(xs) {
  .poi-overview-card h3 {
    font-size: 20px;
  }

  .poi-category {
    font-size: 11px;
  }
}

/* 触屏热区：评分星为文字图标，撑大点击区不改变视觉尺寸 */
@include coarse-pointer {
  .star-btn {
    min-width: 40px;
    min-height: 40px;
    display: inline-flex;
    align-items: center;
    justify-content: center;
  }
}
</style>
