<template>
  <div class="announcement-layer">
    <!-- ── 桌面端 ── -->
    <template v-if="!isMobile">
      <!-- 收起状态：左侧细长触发按钮 -->
      <button
        v-if="isCollapsed"
        type="button"
        class="collapsed-trigger"
        aria-label="展开面板"
        @click="toggleDesktopRail"
      >
        <el-icon><ArrowRightBold /></el-icon>
        <span>面板</span>
      </button>

      <!-- 展开状态：合并面板 -->
      <aside v-else class="notice-rail">
        <!-- 右侧收起 tab -->
        <button
          type="button"
          class="rail-toggle"
          aria-label="收起面板"
          @click="toggleDesktopRail"
        >
          <el-icon><ArrowLeftBold /></el-icon>
          <span>收起</span>
        </button>

        <div class="rail-body">
          <!-- ① 页面目录 -->
          <div class="panel-nav">
            <div class="panel-section-head">
              <span class="section-label">页面目录</span>
            </div>
            <div class="nav-links">
              <button
                v-for="item in navItems"
                :key="item.id"
                type="button"
                class="nav-link"
                @click="scrollToSection(item.id)"
              >
                <span class="nav-dot" />
                {{ item.label }}
              </button>
            </div>
          </div>

          <!-- 分隔线 -->
          <div class="panel-divider" />

          <!-- ② 平台公告 -->
          <div class="panel-anno">
            <div class="panel-section-head anno-head">
              <span class="section-label">平台公告</span>
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
        </div>
      </aside>
    </template>

    <!-- ── 移动端 ── -->
    <template v-else>
      <button type="button" class="mobile-trigger" @click="mobileDrawerVisible = true">
        <el-icon><Bell /></el-icon>
        <span>面板</span>
      </button>

      <el-drawer
        v-model="mobileDrawerVisible"
        direction="ltr"
        size="86vw"
        :with-header="false"
        destroy-on-close
      >
        <div class="mobile-shell">
          <!-- 移动端目录 -->
          <div class="panel-nav mobile-nav">
            <div class="panel-section-head">
              <span class="section-label">页面目录</span>
            </div>
            <div class="nav-links">
              <button
                v-for="item in navItems"
                :key="item.id"
                type="button"
                class="nav-link"
                @click="scrollToSection(item.id); mobileDrawerVisible = false"
              >
                <span class="nav-dot" />
                {{ item.label }}
              </button>
            </div>
          </div>

          <div class="panel-divider" />

          <!-- 移动端公告 -->
          <div class="panel-section-head anno-head" style="padding: 12px 0 10px;">
            <span class="section-label">平台公告</span>
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

    <!-- 公告详情弹窗 -->
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
            lazy
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

const navItems = [
  { id: 'recommended-share-section', label: '精选分享' },
  { id: 'activity-section',          label: '近期活动' },
  { id: 'recommended-route-section', label: '路线探索' },
  { id: 'leaderboard-section',       label: '排行榜' },
  { id: 'feed-section',              label: '关注动态' },
  { id: 'user-route-section',        label: '社区路线' },
]

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
  if (isMobile.value) mobileDrawerVisible.value = false
}

const scrollToSection = (id) => {
  document.getElementById(id)?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

const loadPublicConfigs = async () => {
  try {
    const configMap = await getPublicSystemConfigs()
    isCollapsed.value = configMap?.[HOME_ANNOUNCEMENT_DEFAULT_COLLAPSED] === 'true'
  } catch {
    isCollapsed.value = false
  }
}

const loadAnnouncements = async (forceRefreshOrEvent = false) => {
  const forceRefresh = forceRefreshOrEvent === true || typeof forceRefreshOrEvent === 'object'
  loading.value = true
  try {
    announcements.value = await getAnnouncementList(undefined, { forceRefresh })
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
  if (!value) return ''
  if (/^https?:\/\//i.test(value)) return value
  return `${API_ORIGIN}${value.startsWith('/') ? value : `/${value}`}`
}

const formatDate = (value) => {
  if (!value) return '暂未发布'
  return new Intl.DateTimeFormat('zh-CN', {
    month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit'
  }).format(new Date(value))
}

onMounted(async () => {
  window.addEventListener('resize', handleResize)
  await Promise.all([loadPublicConfigs(), loadAnnouncements()])
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

/* ── 展开面板 ── */
.notice-rail {
  --rail-width: clamp(260px, 16vw, 320px);
  position: fixed;
  top: 68px;
  left: 0;
  bottom: 20px;
  width: var(--rail-width);
  z-index: 920;
  display: flex;
  align-items: stretch;
  animation: rail-enter 0.24s ease;
}

/* 右侧收起 tab */
.rail-toggle {
  width: 32px;
  border: none;
  border-radius: 0 16px 16px 0;
  background: linear-gradient(180deg, var(--front-accent), var(--front-accent-strong));
  color: #f2fbfd;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  cursor: pointer;
  flex-shrink: 0;
  box-shadow: 10px 14px 28px rgba(23, 135, 166, 0.22);
}

.rail-toggle span {
  writing-mode: vertical-rl;
  text-orientation: mixed;
  letter-spacing: 0.2em;
  font-size: 11px;
  font-weight: 700;
}

/* 面板主体：flex 列，撑满高度 */
.rail-body {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  border: 1px solid var(--front-border);
  border-left: none;
  background:
    linear-gradient(180deg, rgba(244, 250, 251, 0.98), rgba(255, 255, 255, 0.96)),
    radial-gradient(circle at top left, rgba(23, 135, 166, 0.1), transparent 30%);
  backdrop-filter: blur(18px);
  box-shadow: 16px 20px 44px rgba(15, 23, 42, 0.1);
  overflow: hidden;
}

/* ── 通用 section 头 ── */
.panel-section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 14px 16px 10px;
}

.section-label {
  display: inline-flex;
  padding: 4px 10px;
  border-radius: 999px;
  background: var(--front-accent-soft);
  color: var(--front-accent-strong);
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.08em;
}

/* ── ① 页面目录 ── */
.panel-nav {
  flex-shrink: 0;
  padding-bottom: 4px;
}

.nav-links {
  display: flex;
  flex-direction: column;
  gap: 2px;
  padding: 0 10px 8px;
}

.nav-link {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
  padding: 7px 10px;
  border: none;
  border-radius: 10px;
  background: transparent;
  color: var(--front-text-soft);
  font-size: 13px;
  font-weight: 500;
  text-align: left;
  cursor: pointer;
  transition: background 0.15s, color 0.15s;
}

.nav-link:hover {
  background: var(--front-accent-soft);
  color: var(--front-accent-strong);
}

.nav-dot {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: var(--front-accent);
  flex-shrink: 0;
  opacity: 0.5;
}

.nav-link:hover .nav-dot {
  opacity: 1;
}

/* ── 分隔线 ── */
.panel-divider {
  flex-shrink: 0;
  height: 1px;
  background: rgba(148, 163, 184, 0.18);
  margin: 0 14px;
}

/* ── ② 平台公告 ── */
.panel-anno {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

.anno-head {
  padding-top: 12px;
}

.refresh-button {
  color: var(--front-accent-strong);
}

.rail-scroll {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 4px 14px 14px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.rail-footer {
  flex-shrink: 0;
  padding: 10px 16px 14px;
  border-top: 1px solid rgba(148, 163, 184, 0.12);
  color: #64748b;
  font-size: 12px;
}

/* ── 收起触发按钮 ── */
.collapsed-trigger {
  position: fixed;
  left: 0;
  top: 68px;
  z-index: 930;
  width: 40px;
  border: none;
  border-radius: 0 16px 16px 0;
  padding: 14px 8px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  background: linear-gradient(180deg, var(--front-accent), var(--front-accent-strong));
  color: #f2fbfd;
  cursor: pointer;
  box-shadow: 10px 14px 28px rgba(23, 135, 166, 0.24);
  animation: trigger-enter 0.2s ease;
}

.collapsed-trigger span {
  writing-mode: vertical-rl;
  text-orientation: mixed;
  letter-spacing: 0.16em;
  font-size: 11px;
  font-weight: 700;
}

/* ── 移动端 ── */
.mobile-trigger {
  position: fixed;
  left: 12px;
  top: 80px;
  z-index: 930;
  border: none;
  border-radius: 999px;
  padding: 10px 14px;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  background: linear-gradient(135deg, var(--front-accent), var(--front-accent-strong));
  color: #f2fbfd;
  font-size: 13px;
  box-shadow: 0 14px 30px rgba(23, 135, 166, 0.24);
}

.mobile-shell {
  height: 100%;
  display: flex;
  flex-direction: column;
  background:
    linear-gradient(180deg, rgba(244, 250, 251, 0.98), rgba(255, 255, 255, 0.96)),
    radial-gradient(circle at top left, rgba(23, 135, 166, 0.1), transparent 30%);
  padding: 16px;
  overflow: hidden;
}

.mobile-nav {
  flex-shrink: 0;
}

.mobile-scroll {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 4px 0 16px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

/* ── 公告卡片 ── */
.featured-card {
  padding: 14px;
  border-radius: 20px;
  border: 1px solid var(--front-border);
  background: linear-gradient(135deg, rgba(224, 244, 248, 0.94), rgba(255, 255, 255, 0.98));
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.06);
  cursor: pointer;
}

.featured-card:hover,
.notice-card:hover {
  transform: translateX(2px);
  box-shadow: 0 16px 32px rgba(15, 23, 42, 0.1);
}

.featured-header,
.notice-meta,
.detail-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  flex-wrap: wrap;
}

.featured-tag,
.meta-pill {
  display: inline-flex;
  padding: 4px 9px;
  border-radius: 999px;
  background: var(--front-accent-soft);
  color: var(--front-accent-strong);
  font-size: 11px;
  font-weight: 700;
}

.featured-time,
.notice-meta {
  color: var(--front-text-muted);
  font-size: 11px;
}

.featured-card h3 {
  margin: 9px 0 7px;
  font-size: 15px;
  line-height: 1.3;
  color: var(--front-text);
}

.featured-card p,
.notice-card p {
  margin: 0;
  color: var(--front-text-soft);
  font-size: 12px;
  line-height: 1.65;
}

.notice-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.notice-card {
  width: 100%;
  padding: 12px 14px;
  text-align: left;
  border-radius: 16px;
  border: 1px solid var(--front-border);
  background: rgba(255, 255, 255, 0.92);
  cursor: pointer;
  transition: transform 0.18s ease, box-shadow 0.18s ease;
}

.notice-card strong {
  display: block;
  margin-top: 8px;
  font-size: 13px;
  line-height: 1.4;
  color: var(--front-text);
}

/* ── 详情弹窗 ── */
.detail-body {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.detail-footer {
  display: flex;
  justify-content: flex-end;
}

.detail-cover {
  width: 100%;
  height: 240px;
  border-radius: 20px;
  overflow: hidden;
}

.detail-meta {
  color: #64748b;
  font-size: 13px;
}

.detail-summary {
  margin: 0;
  font-size: 15px;
  color: var(--front-text-soft);
  line-height: 1.75;
}

.detail-content {
  color: var(--front-text-soft);
  line-height: 1.8;
  white-space: pre-wrap;
}

/* ── 动画 ── */
@keyframes rail-enter {
  from { opacity: 0; transform: translateX(-12px); }
  to   { opacity: 1; transform: translateX(0); }
}

@keyframes trigger-enter {
  from { opacity: 0; transform: translateX(-8px); }
  to   { opacity: 1; transform: translateX(0); }
}

@media (max-width: 900px) {
  .detail-footer { justify-content: stretch; }
}

@media (max-width: 560px) {
  .mobile-trigger {
    left: 10px;
    top: 72px;
    padding: 8px 12px;
  }

  .featured-card { padding: 12px; border-radius: 16px; }
  .notice-card   { padding: 10px 12px; border-radius: 14px; }
  .detail-cover  { height: 180px; border-radius: 16px; }

  .detail-footer :deep(.el-button) {
    width: 100%;
    min-height: 42px;
  }
}
</style>
