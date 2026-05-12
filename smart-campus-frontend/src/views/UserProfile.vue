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

<style scoped>
.profile-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.profile-main {
  flex: 1;
  padding: 22px 0 30px;
}

.profile-shell {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.profile-hero {
  padding: 28px;
  border-radius: 28px;
  display: flex;
  align-items: center;
  gap: 22px;
}

.hero-avatar {
  background: linear-gradient(135deg, #0ea5e9, #2563eb);
  color: #fff;
  font-size: 28px;
  font-weight: 700;
  border: 4px solid rgba(255, 255, 255, 0.92);
  box-shadow: 0 16px 30px rgba(14, 165, 233, 0.18);
  flex-shrink: 0;
}

.hero-name-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.hero-info h1 {
  margin: 0;
  font-size: 26px;
  color: #0f172a;
}

.hero-username {
  color: #64748b;
  font-size: 14px;
}

.hero-info p {
  margin: 10px 0 0;
  color: #475569;
  font-size: 14px;
  line-height: 1.6;
}

.hero-info time {
  display: block;
  margin-top: 8px;
  color: #94a3b8;
  font-size: 12px;
}

.stats-bar {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 14px;
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  padding: 18px;
  border-radius: 20px;
  background: #fff;
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.06);
}

.stat-item.clickable {
  cursor: pointer;
  transition: box-shadow 0.2s, transform 0.15s;
}

.stat-item.clickable:hover {
  box-shadow: 0 10px 28px rgba(15, 23, 42, 0.1);
  transform: translateY(-1px);
}

.stat-item strong {
  font-size: 24px;
  color: #0f172a;
}

.stat-item span {
  color: #64748b;
  font-size: 13px;
}

.profile-tabs {
  display: flex;
  gap: 4px;
  border-bottom: 2px solid #e2e8f0;
  flex-wrap: wrap;
}

.tab-btn {
  padding: 10px 22px;
  font-size: 15px;
  font-weight: 600;
  color: #64748b;
  background: none;
  border: none;
  border-bottom: 2px solid transparent;
  margin-bottom: -2px;
  cursor: pointer;
  transition: color 0.2s, border-color 0.2s;
}

.tab-btn:hover {
  color: #0f172a;
}

.tab-btn.active {
  color: #0ea5e9;
  border-bottom-color: #0ea5e9;
}

.tab-content {
  min-height: 200px;
}

.shares-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.share-card {
  padding: 18px 20px;
  border-radius: 22px;
  background: #fff;
  box-shadow: 0 8px 22px rgba(15, 23, 42, 0.06);
}

.share-card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.share-card-head strong {
  color: #0f172a;
}

.share-card-head time {
  color: #94a3b8;
  font-size: 12px;
}

.share-content {
  margin: 12px 0 0;
  color: #1e293b;
  line-height: 1.7;
  white-space: pre-wrap;
}

.share-images {
  margin-top: 12px;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.share-image {
  width: 100%;
  height: 140px;
  border-radius: 16px;
  overflow: hidden;
}

.share-stats {
  margin-top: 12px;
  display: flex;
  gap: 16px;
  color: #64748b;
  font-size: 13px;
}

.checkins-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.checkin-card {
  padding: 16px 18px;
  border-radius: 18px;
  background: #fff;
  box-shadow: 0 8px 22px rgba(15, 23, 42, 0.06);
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.checkin-category {
  display: inline-flex;
  width: fit-content;
  padding: 3px 10px;
  border-radius: 999px;
  background: #ecfdf5;
  color: #059669;
  font-size: 11px;
  font-weight: 700;
}

.checkin-card strong {
  color: #0f172a;
  font-size: 15px;
}

.checkin-card time {
  color: #94a3b8;
  font-size: 12px;
}

.follow-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.follow-item {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px 18px;
  border-radius: 18px;
  background: #fff;
  box-shadow: 0 6px 20px rgba(15, 23, 42, 0.05);
  text-decoration: none;
  color: inherit;
  transition: transform 0.15s, box-shadow 0.15s;
}

.follow-item:hover {
  transform: translateY(-1px);
  box-shadow: 0 10px 28px rgba(15, 23, 42, 0.09);
}

.follow-avatar {
  background: linear-gradient(135deg, #38bdf8, #2563eb);
  color: #fff;
  font-weight: 700;
  flex-shrink: 0;
}

.follow-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.follow-info strong {
  color: #0f172a;
  font-size: 15px;
}

.follow-bio {
  color: #64748b;
  font-size: 13px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.load-more {
  display: flex;
  justify-content: center;
  padding-top: 18px;
}

.achievements-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.achievement-badge {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 16px 18px;
  border-radius: 18px;
  background: #fff;
  box-shadow: 0 6px 20px rgba(15, 23, 42, 0.05);
}

.achievement-badge.locked {
  opacity: 0.5;
}

.badge-icon {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
  flex-shrink: 0;
  background: linear-gradient(135deg, #fbbf24, #f59e0b);
  color: #fff;
}

.achievement-badge.locked .badge-icon {
  background: #e2e8f0;
  color: #94a3b8;
}

.badge-info {
  display: flex;
  flex-direction: column;
  gap: 3px;
  min-width: 0;
}

.badge-info strong {
  color: #0f172a;
  font-size: 14px;
}

.badge-info span {
  color: #64748b;
  font-size: 12px;
}

.badge-info time {
  color: #94a3b8;
  font-size: 11px;
}

@media (max-width: 768px) {
  .profile-hero {
    flex-direction: column;
    align-items: flex-start;
    padding: 20px;
    border-radius: 24px;
    gap: 16px;
  }

  .hero-avatar {
    width: 72px;
    height: 72px;
    font-size: 24px;
  }

  .hero-info h1 {
    font-size: 22px;
  }

  .stats-bar {
    grid-template-columns: repeat(3, 1fr);
  }

  .stat-item {
    padding: 14px;
  }

  .stat-item strong {
    font-size: 20px;
  }

  .checkins-grid {
    grid-template-columns: 1fr;
  }

  .achievements-grid {
    grid-template-columns: 1fr;
  }

  .share-images {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
