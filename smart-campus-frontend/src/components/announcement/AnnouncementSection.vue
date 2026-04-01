<template>
  <div class="announcement-layer">
    <template v-if="!isMobile">
      <button
        v-if="isCollapsed"
        type="button"
        class="collapsed-trigger"
        aria-label="展开平台公告"
        @click="toggleDesktopRail"
      >
        <el-icon><ArrowRightBold /></el-icon>
        <span>平台公告</span>
      </button>

      <aside v-else class="notice-rail">
        <button
          type="button"
          class="rail-toggle"
          aria-label="收起平台公告"
          @click="toggleDesktopRail"
        >
          <el-icon><ArrowLeftBold /></el-icon>
          <span>收起</span>
        </button>

        <div class="rail-body">
          <div class="rail-head">
            <div>
              <span class="rail-kicker">平台公告</span>
              <h2>平台信息栏</h2>
            </div>
            <el-button text class="refresh-button" @click="loadAnnouncements">刷新</el-button>
          </div>

          <div class="rail-scroll" v-loading="loading">
            <template v-if="announcements.length">
              <article
                v-if="featuredAnnouncement"
                class="featured-card"
                @click="openDetail(featuredAnnouncement.id)"
              >
                <div class="featured-header">
                  <span class="featured-tag">{{ featuredAnnouncement.pinned ? '置顶' : '最新' }}</span>
                  <span class="featured-time">{{ formatDate(featuredAnnouncement.publishedAt) }}</span>
                </div>
                <h3>{{ featuredAnnouncement.title }}</h3>
                <p>{{ featuredAnnouncement.summary }}</p>
              </article>

              <div class="notice-list">
                <button
                  v-for="item in listAnnouncements"
                  :key="item.id"
                  type="button"
                  class="notice-card"
                  @click="openDetail(item.id)"
                >
                  <div class="notice-meta">
                    <span>{{ formatDate(item.publishedAt) }}</span>
                    <span v-if="item.pinned" class="meta-pill">置顶</span>
                  </div>
                  <strong>{{ item.title }}</strong>
                  <p>{{ item.summary }}</p>
                </button>
              </div>
            </template>

            <el-empty v-else description="暂时还没有已发布的平台公告" />
          </div>

          <div class="rail-footer">
            <span>已发布的平台公告会展示在这里</span>
          </div>
        </div>
      </aside>
    </template>

    <template v-else>
      <button type="button" class="mobile-trigger" @click="mobileDrawerVisible = true">
        <el-icon><Bell /></el-icon>
        <span>平台公告</span>
      </button>

      <el-drawer
        v-model="mobileDrawerVisible"
        direction="ltr"
        size="86vw"
        :with-header="false"
        destroy-on-close
      >
        <div class="mobile-shell">
          <div class="rail-head mobile-head">
            <div>
              <span class="rail-kicker">平台公告</span>
              <h2>平台信息栏</h2>
            </div>
            <el-button text class="refresh-button" @click="loadAnnouncements">刷新</el-button>
          </div>

          <div class="rail-scroll mobile-scroll" v-loading="loading">
            <template v-if="announcements.length">
              <article
                v-if="featuredAnnouncement"
                class="featured-card"
                @click="openDetail(featuredAnnouncement.id)"
              >
                <div class="featured-header">
                  <span class="featured-tag">{{ featuredAnnouncement.pinned ? '置顶' : '最新' }}</span>
                  <span class="featured-time">{{ formatDate(featuredAnnouncement.publishedAt) }}</span>
                </div>
                <h3>{{ featuredAnnouncement.title }}</h3>
                <p>{{ featuredAnnouncement.summary }}</p>
              </article>

              <div class="notice-list">
                <button
                  v-for="item in listAnnouncements"
                  :key="item.id"
                  type="button"
                  class="notice-card"
                  @click="openDetail(item.id)"
                >
                  <div class="notice-meta">
                    <span>{{ formatDate(item.publishedAt) }}</span>
                    <span v-if="item.pinned" class="meta-pill">置顶</span>
                  </div>
                  <strong>{{ item.title }}</strong>
                  <p>{{ item.summary }}</p>
                </button>
              </div>
            </template>

            <el-empty v-else description="暂时还没有已发布的平台公告" />
          </div>
        </div>
      </el-drawer>
    </template>

    <el-dialog
      v-model="detailVisible"
      width="760px"
      append-to-body
      destroy-on-close
      :close-on-click-modal="true"
      :close-on-press-escape="true"
      :title="selectedAnnouncement?.title || '公告详情'"
      @closed="selectedAnnouncement = null"
    >
      <div v-loading="detailLoading" class="detail-body">
        <template v-if="selectedAnnouncement">
          <el-image
            v-if="selectedAnnouncement.coverImageUrl"
            :src="resolveAssetUrl(selectedAnnouncement.coverImageUrl)"
            fit="cover"
            class="detail-cover"
          />

          <div class="detail-meta">
            <el-tag :type="selectedAnnouncement.pinned ? 'warning' : 'info'" effect="plain">
              {{ selectedAnnouncement.pinned ? '置顶公告' : '平台公告' }}
            </el-tag>
            <span>{{ formatDate(selectedAnnouncement.publishedAt) }}</span>
          </div>

          <p class="detail-summary">{{ selectedAnnouncement.summary }}</p>
          <div class="detail-content">{{ selectedAnnouncement.content }}</div>
        </template>
      </div>

      <template #footer>
        <div class="detail-footer">
          <el-button @click="detailVisible = false">关闭</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { ArrowLeftBold, ArrowRightBold, Bell } from '@element-plus/icons-vue'
import { getAnnouncementDetail, getAnnouncementList } from '@/api/announcement'
import { getPublicSystemConfigs } from '@/api/systemConfig'
import { API_ORIGIN } from '@/utils/request'

const MOBILE_BREAKPOINT = 900
const HOME_ANNOUNCEMENT_DEFAULT_COLLAPSED = 'home.announcement.defaultCollapsed'

const loading = ref(false)
const detailLoading = ref(false)
const detailVisible = ref(false)
const mobileDrawerVisible = ref(false)
const announcements = ref([])
const selectedAnnouncement = ref(null)
const windowWidth = ref(typeof window === 'undefined' ? 1440 : window.innerWidth)
const isCollapsed = ref(false)

const isMobile = computed(() => windowWidth.value <= MOBILE_BREAKPOINT)
const featuredAnnouncement = computed(() => announcements.value[0] || null)
const listAnnouncements = computed(() => announcements.value.slice(1))

const handleResize = () => {
  windowWidth.value = window.innerWidth
  if (isMobile.value) {
    mobileDrawerVisible.value = false
  }
}

const loadPublicConfigs = async () => {
  try {
    const configMap = await getPublicSystemConfigs()
    isCollapsed.value = configMap?.[HOME_ANNOUNCEMENT_DEFAULT_COLLAPSED] === 'true'
  } catch {
    isCollapsed.value = false
  }
}

const loadAnnouncements = async () => {
  loading.value = true
  try {
    announcements.value = await getAnnouncementList()
  } catch (error) {
    ElMessage.error(error.message || '加载公告失败')
  } finally {
    loading.value = false
  }
}

const openDetail = async (announcementId) => {
  detailVisible.value = true
  detailLoading.value = true
  try {
    selectedAnnouncement.value = await getAnnouncementDetail(announcementId)
    mobileDrawerVisible.value = false
  } catch (error) {
    detailVisible.value = false
    selectedAnnouncement.value = null
    ElMessage.error(error.message || '加载公告详情失败')
  } finally {
    detailLoading.value = false
  }
}

const toggleDesktopRail = () => {
  isCollapsed.value = !isCollapsed.value
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

const formatDate = (value) => {
  if (!value) {
    return '暂未发布'
  }

  return new Intl.DateTimeFormat('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  }).format(new Date(value))
}

onMounted(async () => {
  window.addEventListener('resize', handleResize)
  await loadPublicConfigs()
  await loadAnnouncements()
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
})
</script>

<style scoped>
.notice-rail,
.mobile-trigger,
.collapsed-trigger {
  pointer-events: auto;
}

.notice-rail {
  --rail-width: clamp(320px, 17vw, 380px);
  position: fixed;
  top: 84px;
  left: 0;
  bottom: 24px;
  width: var(--rail-width);
  z-index: 920;
  display: flex;
  align-items: stretch;
  animation: rail-enter 0.24s ease;
}

.rail-toggle {
  width: 34px;
  border: none;
  border-radius: 0 18px 18px 0;
  background: linear-gradient(180deg, #f97316, #ea580c);
  color: #fff7ed;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 14px;
  cursor: pointer;
  box-shadow: 12px 16px 30px rgba(234, 88, 12, 0.28);
}

.rail-toggle span {
  writing-mode: vertical-rl;
  text-orientation: mixed;
  letter-spacing: 0.2em;
  font-size: 11px;
  font-weight: 700;
}

.rail-body,
.mobile-shell {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  border: 1px solid rgba(251, 191, 36, 0.18);
  border-left: none;
  background:
    linear-gradient(180deg, rgba(255, 251, 235, 0.96), rgba(255, 255, 255, 0.94)),
    radial-gradient(circle at top left, rgba(249, 115, 22, 0.14), transparent 28%);
  backdrop-filter: blur(18px);
  box-shadow: 18px 22px 48px rgba(15, 23, 42, 0.12);
  overflow: hidden;
}

.mobile-shell {
  height: 100%;
  border: none;
  box-shadow: none;
  background:
    linear-gradient(180deg, rgba(255, 251, 235, 0.96), rgba(255, 255, 255, 0.96)),
    radial-gradient(circle at top left, rgba(249, 115, 22, 0.14), transparent 28%);
}

.rail-head {
  padding: 22px 22px 16px;
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  border-bottom: 1px solid rgba(148, 163, 184, 0.12);
}

.mobile-head {
  padding-left: 0;
  padding-right: 0;
}

.rail-kicker {
  display: inline-flex;
  padding: 6px 12px;
  border-radius: 999px;
  background: rgba(249, 115, 22, 0.12);
  color: #c2410c;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
}

.rail-head h2 {
  margin: 14px 0 0;
  color: #0f172a;
  font-size: 26px;
  line-height: 1.05;
}

.refresh-button {
  color: #c2410c;
}

.rail-scroll {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 18px 18px 22px;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.mobile-scroll {
  padding-left: 0;
  padding-right: 0;
}

.featured-card,
.notice-card {
  border: 1px solid rgba(148, 163, 184, 0.14);
  box-shadow: 0 14px 30px rgba(15, 23, 42, 0.08);
}

.featured-card {
  padding: 18px;
  border-radius: 24px;
  background: linear-gradient(135deg, rgba(255, 237, 213, 0.92), rgba(255, 255, 255, 0.98));
  cursor: pointer;
}

.featured-header,
.notice-meta,
.detail-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}

.featured-tag,
.meta-pill {
  display: inline-flex;
  padding: 5px 10px;
  border-radius: 999px;
  background: rgba(249, 115, 22, 0.12);
  color: #c2410c;
  font-size: 12px;
  font-weight: 700;
}

.featured-time,
.notice-meta {
  color: #64748b;
  font-size: 12px;
}

.featured-card h3,
.notice-card strong {
  color: #0f172a;
}

.featured-card h3 {
  margin: 14px 0 10px;
  font-size: 24px;
  line-height: 1.2;
}

.featured-card p,
.notice-card p,
.detail-summary,
.detail-content {
  color: #475569;
  line-height: 1.7;
}

.featured-card p {
  margin: 0;
}

.notice-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.notice-card {
  border: none;
  width: 100%;
  padding: 16px;
  text-align: left;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.94);
  cursor: pointer;
  transition: transform 0.18s ease, box-shadow 0.18s ease;
}

.notice-card:hover,
.featured-card:hover {
  transform: translateX(2px);
  box-shadow: 0 20px 36px rgba(15, 23, 42, 0.12);
}

.notice-card strong {
  display: block;
  margin-top: 10px;
  font-size: 16px;
  line-height: 1.45;
}

.notice-card p {
  margin: 8px 0 0;
  font-size: 13px;
}

.rail-footer {
  padding: 14px 18px 18px;
  border-top: 1px solid rgba(148, 163, 184, 0.12);
  color: #64748b;
  font-size: 12px;
}

.mobile-trigger {
  position: fixed;
  left: 12px;
  top: 86px;
  z-index: 930;
  border: none;
  border-radius: 999px;
  padding: 12px 16px;
  display: inline-flex;
  align-items: center;
  gap: 10px;
  background: linear-gradient(135deg, #f97316, #ea580c);
  color: #fff7ed;
  box-shadow: 0 18px 36px rgba(234, 88, 12, 0.28);
}

.collapsed-trigger {
  position: fixed;
  left: 0;
  top: 116px;
  z-index: 930;
  width: 42px;
  border: none;
  border-radius: 0 18px 18px 0;
  padding: 14px 8px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  background: linear-gradient(180deg, #f97316, #ea580c);
  color: #fff7ed;
  cursor: pointer;
  box-shadow: 12px 16px 30px rgba(234, 88, 12, 0.28);
  animation: trigger-enter 0.2s ease;
}

.collapsed-trigger span {
  writing-mode: vertical-rl;
  text-orientation: mixed;
  letter-spacing: 0.16em;
  font-size: 11px;
  font-weight: 700;
}

.detail-body {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.detail-footer {
  display: flex;
  justify-content: flex-end;
}

.detail-cover {
  width: 100%;
  height: 260px;
  border-radius: 24px;
  overflow: hidden;
}

.detail-meta {
  color: #64748b;
  font-size: 13px;
}

.detail-summary {
  margin: 0;
  font-size: 15px;
}

.detail-content {
  white-space: pre-wrap;
}

@keyframes rail-enter {
  from {
    opacity: 0;
    transform: translateX(-12px);
  }

  to {
    opacity: 1;
    transform: translateX(0);
  }
}

@keyframes trigger-enter {
  from {
    opacity: 0;
    transform: translateX(-8px);
  }

  to {
    opacity: 1;
    transform: translateX(0);
  }
}

@media (max-width: 900px) {
  .detail-footer {
    justify-content: stretch;
  }
}
</style>
