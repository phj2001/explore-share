<template>
  <div class="profile-page front-page">
    <Header />

    <main class="profile-main">
      <div class="front-shell profile-shell">
        <el-skeleton v-if="loading" :rows="6" animated />

        <template v-else-if="profile">
          <section class="profile-hero front-panel">
            <el-avatar :size="88" :src="profile.avatarUrl || undefined" class="hero-avatar">
              {{ (profile.displayName || profile.username || 'U').slice(0, 1).toUpperCase() }}
            </el-avatar>

            <div class="hero-info">
              <div class="hero-name-row">
                <h1>{{ profile.displayName || profile.username }}</h1>
                <el-button
                  v-if="isOtherUser"
                  :type="followData.following ? 'default' : 'primary'"
                  :loading="followLoading"
                  size="small"
                  @click="toggleFollow"
                >
                  {{ followData.following ? '已关注' : '关注' }}
                </el-button>
              </div>
              <span class="hero-username">@{{ profile.username }}</span>
              <p v-if="profile.bio">{{ profile.bio }}</p>
              <time>加入于 {{ formatTime(profile.createdAt) }}</time>
            </div>
          </section>

          <div class="stats-bar">
            <div class="stat-item">
              <strong>{{ profile.checkInCount }}</strong>
              <span>打卡</span>
            </div>
            <div class="stat-item">
              <strong>{{ profile.shareCount }}</strong>
              <span>分享</span>
            </div>
            <div class="stat-item">
              <strong>{{ profile.receivedLikeCount }}</strong>
              <span>获赞</span>
            </div>
            <div class="stat-item">
              <strong>{{ profile.reviewCount }}</strong>
              <span>评价</span>
            </div>
            <div class="stat-item clickable" @click="switchTab('following')">
              <strong>{{ followData.followingCount }}</strong>
              <span>关注</span>
            </div>
            <div class="stat-item clickable" @click="switchTab('followers')">
              <strong>{{ followData.followerCount }}</strong>
              <span>粉丝</span>
            </div>
          </div>

          <div class="profile-tabs">
            <button
              :class="['tab-btn', { active: activeTab === 'shares' }]"
              @click="switchTab('shares')"
            >
              分享
            </button>
            <button
              :class="['tab-btn', { active: activeTab === 'checkins' }]"
              @click="switchTab('checkins')"
            >
              打卡足迹
            </button>
            <button
              :class="['tab-btn', { active: activeTab === 'following' }]"
              @click="switchTab('following')"
            >
              关注
            </button>
            <button
              :class="['tab-btn', { active: activeTab === 'followers' }]"
              @click="switchTab('followers')"
            >
              粉丝
            </button>
            <button
              :class="['tab-btn', { active: activeTab === 'achievements' }]"
              @click="switchTab('achievements')"
            >
              成就
            </button>
          </div>

          <div v-if="activeTab === 'shares'" class="tab-content">
            <el-skeleton v-if="sharesLoading && !shares.length" :rows="4" animated />

            <div v-else-if="shares.length" class="shares-list">
              <div v-for="share in shares" :key="share.id" class="share-card">
                <div class="share-card-head">
                  <strong>{{ profile.displayName || profile.username }}</strong>
                  <time>{{ formatTime(share.createdAt) }}</time>
                </div>
                <p v-if="share.content" class="share-content">{{ share.content }}</p>
                <div v-if="share.imageUrls?.length" class="share-images">
                  <el-image
                    v-for="(url, i) in share.imageUrls"
                    :key="i"
                    :src="url"
                    :preview-src-list="share.imageUrls"
                    fit="cover"
                    lazy
                    preview-teleported
                    class="share-image"
                  />
                </div>
                <div class="share-stats">
                  <span>{{ share.likeCount || 0 }} 赞</span>
                  <span>{{ share.replyCount || 0 }} 回复</span>
                </div>
              </div>
            </div>

            <el-empty v-else description="还没有发布任何分享" />

            <div v-if="sharesHasMore" class="load-more">
              <el-button :loading="sharesLoadingMore" @click="loadMoreShares">加载更多</el-button>
            </div>
          </div>

          <div v-if="activeTab === 'checkins'" class="tab-content">
            <el-skeleton v-if="checkinsLoading && !checkins.length" :rows="4" animated />

            <div v-else-if="checkins.length" class="checkins-grid">
              <div v-for="item in checkins" :key="item.checkInId" class="checkin-card">
                <span class="checkin-category">{{ item.category }}</span>
                <strong>{{ item.poiName }}</strong>
                <time>{{ formatTime(item.checkedInAt) }}</time>
              </div>
            </div>

            <el-empty v-else description="还没有打卡记录" />

            <div v-if="checkinsHasMore" class="load-more">
              <el-button :loading="checkinsLoadingMore" @click="loadMoreCheckins">加载更多</el-button>
            </div>
          </div>

          <div v-if="activeTab === 'following'" class="tab-content">
            <el-skeleton v-if="followListLoading && !followList.length" :rows="4" animated />

            <div v-else-if="followList.length" class="follow-list">
              <router-link
                v-for="item in followList"
                :key="item.userId"
                :to="'/user/' + item.userId"
                class="follow-item"
              >
                <el-avatar :size="36" :src="item.avatarUrl || undefined" class="follow-avatar">
                  {{ (item.displayName || 'U').slice(0, 1).toUpperCase() }}
                </el-avatar>
                <div class="follow-info">
                  <strong>{{ item.displayName || item.username }}</strong>
                  <span v-if="item.bio" class="follow-bio">{{ item.bio }}</span>
                </div>
              </router-link>
            </div>

            <el-empty v-else description="还没有关注任何人" />

            <div v-if="followListHasMore" class="load-more">
              <el-button :loading="followListLoadingMore" @click="loadMoreFollowList">加载更多</el-button>
            </div>
          </div>

          <div v-if="activeTab === 'followers'" class="tab-content">
            <el-skeleton v-if="followerListLoading && !followerList.length" :rows="4" animated />

            <div v-else-if="followerList.length" class="follow-list">
              <router-link
                v-for="item in followerList"
                :key="item.userId"
                :to="'/user/' + item.userId"
                class="follow-item"
              >
                <el-avatar :size="36" :src="item.avatarUrl || undefined" class="follow-avatar">
                  {{ (item.displayName || 'U').slice(0, 1).toUpperCase() }}
                </el-avatar>
                <div class="follow-info">
                  <strong>{{ item.displayName || item.username }}</strong>
                  <span v-if="item.bio" class="follow-bio">{{ item.bio }}</span>
                </div>
              </router-link>
            </div>

            <el-empty v-else description="还没有粉丝" />

            <div v-if="followerListHasMore" class="load-more">
              <el-button :loading="followerListLoadingMore" @click="loadMoreFollowerList">加载更多</el-button>
            </div>
          </div>

          <div v-if="activeTab === 'achievements'" class="tab-content">
            <el-skeleton v-if="achievementsLoading" :rows="4" animated />

            <div v-else-if="achievements.length" class="achievements-grid">
              <div
                v-for="ach in achievements"
                :key="ach.id"
                :class="['achievement-badge', { locked: !ach.unlocked }]"
              >
                <div class="badge-icon">
                  <span v-if="ach.unlocked">&#9733;</span>
                  <span v-else>?</span>
                </div>
                <div class="badge-info">
                  <strong>{{ ach.name }}</strong>
                  <span>{{ ach.description }}</span>
                  <time v-if="ach.unlocked">{{ formatTime(ach.unlockedAt) }}</time>
                </div>
              </div>
            </div>

            <el-empty v-else description="暂无成就" />
          </div>
        </template>

        <el-empty v-else description="用户不存在" />
      </div>
    </main>

    <Footer />
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import Header from '@/components/common/Header.vue'
import Footer from '@/components/common/Footer.vue'
import { useUserStore } from '@/stores/user'
import { getUserCheckIns, getUserPublicProfile, getUserPublicShares } from '@/api/user'
import { followUser, unfollowUser, getFollowStatus, getFollowingList, getFollowerList } from '@/api/userFollow'
import { getUserAchievements } from '@/api/achievement'

const route = useRoute()
const userStore = useUserStore()

const PAGE_SIZE = 10
const dateTimeFormatter = new Intl.DateTimeFormat('zh-CN', {
  year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit'
})

const formatTime = (value) => value ? dateTimeFormatter.format(new Date(value)) : ''

const loading = ref(true)
const profile = ref(null)
const activeTab = ref('shares')

const shares = ref([])
const sharesLoading = ref(false)
const sharesLoadingMore = ref(false)
const sharesHasMore = ref(false)
const sharesPage = ref(0)

const checkins = ref([])
const checkinsLoading = ref(false)
const checkinsLoadingMore = ref(false)
const checkinsHasMore = ref(false)
const checkinsPage = ref(0)

const followData = ref({ following: false, follower: false, followingCount: 0, followerCount: 0 })
const followLoading = ref(false)

const followList = ref([])
const followListLoading = ref(false)
const followListLoadingMore = ref(false)
const followListHasMore = ref(false)
const followListPage = ref(0)

const followerList = ref([])
const followerListLoading = ref(false)
const followerListLoadingMore = ref(false)
const followerListHasMore = ref(false)
const followerListPage = ref(0)

const achievements = ref([])
const achievementsLoading = ref(false)

const isOtherUser = computed(() => {
  if (!userStore.isLoggedIn || !profile.value) return false
  return userStore.userInfo?.id !== profile.value.userId
})

const loadProfile = async (userId) => {
  loading.value = true
  try {
    profile.value = await getUserPublicProfile(userId)
  } catch (error) {
    ElMessage.error(error.message || '加载用户资料失败')
  } finally {
    loading.value = false
  }
}

const loadFollowStatus = async (userId) => {
  try {
    followData.value = await getFollowStatus(userId)
  } catch {
    // 静默
  }
}

const toggleFollow = async () => {
  if (!profile.value || followLoading.value) return
  followLoading.value = true
  try {
    if (followData.value.following) {
      await unfollowUser(profile.value.userId)
      followData.value.following = false
      followData.value.followerCount = Math.max(0, followData.value.followerCount - 1)
    } else {
      await followUser(profile.value.userId)
      followData.value.following = true
      followData.value.followerCount += 1
    }
  } catch (error) {
    ElMessage.error(error.message || '操作失败')
  } finally {
    followLoading.value = false
  }
}

const loadShares = async (userId, reset = false) => {
  const nextPage = reset ? 0 : sharesPage.value + 1
  const loadingRef = reset ? sharesLoading : sharesLoadingMore
  loadingRef.value = true

  try {
    const data = await getUserPublicShares(userId, { page: nextPage, size: PAGE_SIZE })
    const records = data?.records || []
    shares.value = reset ? records : [...shares.value, ...records]
    sharesPage.value = data?.page || nextPage
    sharesHasMore.value = Boolean(data?.hasNext)
  } catch {
    // 静默
  } finally {
    loadingRef.value = false
  }
}

const loadMoreShares = async () => {
  if (profile.value) await loadShares(profile.value.userId, false)
}

const loadCheckins = async (userId, reset = false) => {
  const nextPage = reset ? 0 : checkinsPage.value + 1
  const loadingRef = reset ? checkinsLoading : checkinsLoadingMore
  loadingRef.value = true

  try {
    const data = await getUserCheckIns(userId, { page: nextPage, size: PAGE_SIZE })
    const records = data?.records || []
    checkins.value = reset ? records : [...checkins.value, ...records]
    checkinsPage.value = data?.page || nextPage
    checkinsHasMore.value = Boolean(data?.hasNext)
  } catch {
    // 静默
  } finally {
    loadingRef.value = false
  }
}

const loadMoreCheckins = async () => {
  if (profile.value) await loadCheckins(profile.value.userId, false)
}

const loadFollowListData = async (userId, reset = false) => {
  const nextPage = reset ? 0 : followListPage.value + 1
  const loadingRef = reset ? followListLoading : followListLoadingMore
  loadingRef.value = true

  try {
    const data = await getFollowingList(userId, { page: nextPage, size: PAGE_SIZE })
    const records = data?.records || []
    followList.value = reset ? records : [...followList.value, ...records]
    followListPage.value = data?.page || nextPage
    followListHasMore.value = Boolean(data?.hasNext)
  } catch {
    // 静默
  } finally {
    loadingRef.value = false
  }
}

const loadMoreFollowList = async () => {
  if (profile.value) await loadFollowListData(profile.value.userId, false)
}

const loadFollowerListData = async (userId, reset = false) => {
  const nextPage = reset ? 0 : followerListPage.value + 1
  const loadingRef = reset ? followerListLoading : followerListLoadingMore
  loadingRef.value = true

  try {
    const data = await getFollowerList(userId, { page: nextPage, size: PAGE_SIZE })
    const records = data?.records || []
    followerList.value = reset ? records : [...followerList.value, ...records]
    followerListPage.value = data?.page || nextPage
    followerListHasMore.value = Boolean(data?.hasNext)
  } catch {
    // 静默
  } finally {
    loadingRef.value = false
  }
}

const loadMoreFollowerList = async () => {
  if (profile.value) await loadFollowerListData(profile.value.userId, false)
}

const loadAchievements = async (userId) => {
  achievementsLoading.value = true
  try {
    achievements.value = await getUserAchievements(userId)
  } catch {
    // 静默
  } finally {
    achievementsLoading.value = false
  }
}

const switchTab = (tab) => {
  activeTab.value = tab
  if (!profile.value) return
  const userId = profile.value.userId

  if (tab === 'shares' && !shares.value.length) loadShares(userId, true)
  if (tab === 'checkins' && !checkins.value.length) loadCheckins(userId, true)
  if (tab === 'following' && !followList.value.length) loadFollowListData(userId, true)
  if (tab === 'followers' && !followerList.value.length) loadFollowerListData(userId, true)
  if (tab === 'achievements' && !achievements.value.length) loadAchievements(userId)
}

onMounted(async () => {
  const userId = Number(route.params.userId)
  if (!userId) return

  await loadProfile(userId)
  if (profile.value) {
    await Promise.all([
      loadShares(userId, true),
      loadFollowStatus(userId)
    ])
  }
})
</script>

<style scoped lang="scss">
/* =========================================================
   个人中心 — 护照感封面 · 徽章成就墙
   ========================================================= */
.profile-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--front-bg);
}

.profile-main {
  flex: 1;
  padding: 24px 0 40px;
}

.profile-shell {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

/* 封面 Hero — 护照感深绿 */
.profile-hero {
  padding: 28px 32px;
  border-radius: 16px;
  border: 1px solid var(--front-border);
  background: linear-gradient(135deg, var(--forest-800) 0%, var(--forest-900) 100%);
  display: flex;
  align-items: center;
  gap: 24px;
  position: relative;
  overflow: hidden;
}

.profile-hero::before {
  content: '';
  position: absolute;
  inset: 0;
  background-image:
    repeating-radial-gradient(circle at 15% 50%,
      transparent 0, transparent 30px,
      rgba(74,196,154,0.05) 30px, rgba(74,196,154,0.05) 31px);
  pointer-events: none;
}

.hero-avatar {
  background: linear-gradient(135deg, var(--forest-500), var(--forest-700));
  color: #fff;
  font-size: 28px;
  font-weight: 600;
  border: 3px solid rgba(255,255,255,0.15);
  box-shadow: 0 12px 28px rgba(5,54,37,0.4);
  flex-shrink: 0;
  position: relative;
  z-index: 1;
}

.hero-name-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.hero-info h1 {
  margin: 0;
  font-family: var(--font-serif);
  font-size: 24px;
  font-weight: 500;
  color: var(--forest-50);
  letter-spacing: -0.02em;
  position: relative;
  z-index: 1;
}

.hero-username {
  display: block;
  margin-top: 4px;
  font-family: var(--font-mono);
  font-size: 11px;
  letter-spacing: 0.1em;
  color: var(--forest-400);
  position: relative;
  z-index: 1;
}

.hero-info p {
  margin: 10px 0 0;
  font-size: 13.5px;
  line-height: 1.65;
  color: var(--forest-200);
  position: relative;
  z-index: 1;
}

.hero-info time {
  display: block;
  margin-top: 8px;
  font-family: var(--font-mono);
  font-size: 10.5px;
  letter-spacing: 0.08em;
  color: var(--forest-400);
  position: relative;
  z-index: 1;
}

/* 统计数据行 */
.stats-bar {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 10px;
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 5px;
  padding: 16px 12px;
  border-radius: 12px;
  border: 1px solid var(--front-border);
  background: #fff;
  box-shadow: var(--front-shadow-soft);
}

.stat-item.clickable {
  cursor: pointer;
  transition: border-color 0.2s, transform 0.15s, box-shadow 0.2s;
}

.stat-item.clickable:hover {
  border-color: var(--forest-400);
  transform: translateY(-2px);
  box-shadow: var(--front-shadow);
}

.stat-item strong {
  font-family: var(--font-serif);
  font-size: 22px;
  color: var(--ink-900);
  letter-spacing: -0.02em;
}

.stat-item span {
  font-family: var(--font-mono);
  font-size: 10px;
  letter-spacing: 0.1em;
  text-transform: uppercase;
  color: var(--ink-400);
}

/* Tab 切换 */
.profile-tabs {
  display: flex;
  gap: 4px;
  border-bottom: 1px solid var(--front-border);
  flex-wrap: wrap;
}

.tab-btn {
  padding: 10px 18px;
  font-family: var(--font-sans);
  font-size: 13.5px;
  font-weight: 500;
  color: var(--ink-500);
  background: none;
  border: none;
  border-bottom: 2px solid transparent;
  margin-bottom: -1px;
  cursor: pointer;
  transition: color 0.15s, border-color 0.15s;
}

.tab-btn:hover { color: var(--ink-800); }

.tab-btn.active {
  color: var(--forest-700);
  border-bottom-color: var(--forest-700);
}

.tab-content {
  min-height: 200px;
  padding-top: 16px;
}

/* 分享列表 */
.shares-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.share-card {
  padding: 18px 20px;
  border-radius: 12px;
  border: 1px solid var(--front-border);
  background: #fff;
  box-shadow: var(--front-shadow-soft);
  transition: border-color 0.15s;
}
.share-card:hover { border-color: var(--forest-300); }

.share-card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.share-card-head strong {
  font-size: 14px;
  font-weight: 600;
  color: var(--ink-900);
}

.share-card-head time {
  font-family: var(--font-mono);
  font-size: 10.5px;
  letter-spacing: 0.06em;
  color: var(--ink-400);
}

.share-content {
  margin: 10px 0 0;
  color: var(--ink-700);
  font-size: 13.5px;
  line-height: 1.7;
  white-space: pre-wrap;
}

.share-images {
  margin-top: 12px;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
}

.share-image {
  width: 100%;
  height: 130px;
  border-radius: 8px;
  overflow: hidden;
}

.share-stats {
  margin-top: 10px;
  display: flex;
  gap: 16px;
  font-family: var(--font-mono);
  font-size: 11px;
  letter-spacing: 0.06em;
  color: var(--ink-400);
}

/* 打卡足迹 */
.checkins-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.checkin-card {
  padding: 14px 16px;
  border-radius: 12px;
  border: 1px solid var(--front-border);
  background: #fff;
  display: flex;
  flex-direction: column;
  gap: 5px;
  transition: border-color 0.15s;
}
.checkin-card:hover { border-color: var(--forest-300); }

.checkin-category {
  display: inline-flex;
  width: fit-content;
  padding: 2px 9px;
  border-radius: 999px;
  background: var(--forest-50);
  color: var(--forest-700);
  font-family: var(--font-mono);
  font-size: 10px;
  font-weight: 500;
  letter-spacing: 0.08em;
}

.checkin-card strong {
  color: var(--ink-900);
  font-size: 14px;
  font-weight: 600;
}

.checkin-card time {
  font-family: var(--font-mono);
  font-size: 10.5px;
  color: var(--ink-400);
  letter-spacing: 0.06em;
}

/* 关注/粉丝列表 */
.follow-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.follow-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  border-radius: 12px;
  border: 1px solid var(--front-border);
  background: #fff;
  text-decoration: none;
  color: inherit;
  transition: border-color 0.15s, transform 0.15s;
}

.follow-item:hover {
  border-color: var(--forest-300);
  transform: translateX(2px);
}

.follow-avatar {
  background: linear-gradient(135deg, var(--forest-500), var(--forest-700));
  color: #fff;
  font-weight: 600;
  flex-shrink: 0;
}

.follow-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.follow-info strong {
  font-size: 14px;
  font-weight: 600;
  color: var(--ink-900);
}

.follow-bio {
  font-size: 12.5px;
  color: var(--ink-500);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.load-more {
  display: flex;
  justify-content: center;
  padding-top: 20px;
}

/* 成就徽章墙 */
.achievements-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.achievement-badge {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 16px 18px;
  border-radius: 12px;
  border: 1px solid var(--front-border);
  background: #fff;
  box-shadow: var(--front-shadow-soft);
  transition: border-color 0.15s;
}
.achievement-badge:hover:not(.locked) { border-color: var(--forest-300); }

.achievement-badge.locked {
  opacity: 0.45;
}

.badge-icon {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  flex-shrink: 0;
  background: linear-gradient(135deg, var(--clay-400), var(--clay-600));
  color: #fff;
}

.achievement-badge.locked .badge-icon {
  background: var(--paper-200);
  color: var(--ink-400);
}

.badge-info {
  display: flex;
  flex-direction: column;
  gap: 3px;
  min-width: 0;
}

.badge-info strong {
  font-size: 14px;
  font-weight: 600;
  color: var(--ink-900);
}

.badge-info span {
  font-size: 12px;
  color: var(--ink-500);
  line-height: 1.5;
}

.badge-info time {
  font-family: var(--font-mono);
  font-size: 10.5px;
  letter-spacing: 0.06em;
  color: var(--ink-400);
}

@include respond-to(md) {
  .profile-hero {
    flex-direction: column;
    align-items: flex-start;
    padding: 20px;
    gap: 14px;
  }
  .hero-info h1 { font-size: 20px; }
  .stats-bar { grid-template-columns: repeat(3, 1fr); }
  .stat-item { padding: 12px 8px; }
  .stat-item strong { font-size: 18px; }
  .checkins-grid { grid-template-columns: 1fr; }
  .achievements-grid { grid-template-columns: 1fr; }
  .share-images { grid-template-columns: repeat(2, minmax(0, 1fr)); }
}

@include respond-to(xs) {
  .stats-bar { grid-template-columns: repeat(2, 1fr); gap: 8px; }
  .profile-tabs { flex-wrap: nowrap; overflow-x: auto; scrollbar-width: none; }
  .profile-tabs::-webkit-scrollbar { display: none; }
  .tab-btn { white-space: nowrap; flex-shrink: 0; }
}

/* 触屏：关注为核心操作撑 44px；tab/统计项/加载更多 ≥40px */
@include coarse-pointer {
  .hero-name-row :deep(.el-button) {
    min-height: 44px;
  }

  .tab-btn,
  .stat-item.clickable {
    min-height: 40px;
  }

  .load-more :deep(.el-button) {
    min-height: 40px;
  }
}
</style>
