<template>
  <div class="settings-page front-page">
    <Header />

    <main class="settings-main">
      <div class="front-shell settings-shell">
        <section class="profile-hero front-panel">
          <div class="hero-main">
            <el-avatar :size="94" :src="userStore.avatarUrl || undefined" class="hero-avatar">
              {{ displayName.slice(0, 1).toUpperCase() }}
            </el-avatar>

            <div class="hero-copy">
              <span class="front-kicker">个人中心</span>
              <h1 class="front-title">管理你的探索身份、展示资料与账号安全</h1>
              <p class="front-description">
                在这里维护头像、展示名、个性签名和密码信息，让你在地点探索与分享中的个人形象保持完整一致。
              </p>
            </div>
          </div>

          <div class="hero-side">
            <div class="hero-info-card">
              <span>当前账号</span>
              <strong>{{ userStore.username || '未登录账号' }}</strong>
              <p>{{ profileForm.bio || '还没有填写个性签名，可以写一句你的探索偏好。' }}</p>
            </div>

            <el-upload
              class="hidden-uploader"
              :show-file-list="false"
              :auto-upload="false"
              :on-change="handleAvatarSelect"
              accept=".png,.jpg,.jpeg,.webp,image/png,image/jpeg,image/webp"
            >
              <template #trigger>
                <el-button type="primary" :loading="avatarUploading">更新头像</el-button>
              </template>
            </el-upload>
          </div>
        </section>

        <div class="settings-grid">
          <section class="settings-card front-panel">
            <div class="card-head">
              <div>
                <span class="front-kicker">资料编辑</span>
                <h2>基本资料</h2>
              </div>
              <el-button type="primary" :loading="profileSaving" @click="submitProfile">保存资料</el-button>
            </div>

            <el-form
              ref="profileFormRef"
              :model="profileForm"
              :rules="profileRules"
              label-position="top"
              class="settings-form"
            >
              <el-form-item label="登录账号">
                <el-input :model-value="userStore.username" disabled />
              </el-form-item>

              <el-form-item label="绑定邮箱">
                <el-input
                  :model-value="userStore.userInfo?.email || '暂未绑定邮箱'"
                  disabled
                >
                  <template v-if="userStore.userInfo?.email" #suffix>
                    <el-tag size="small" type="success" effect="plain">已验证</el-tag>
                  </template>
                </el-input>
              </el-form-item>

              <el-form-item label="展示名" prop="displayName">
                <el-input
                  v-model="profileForm.displayName"
                  maxlength="100"
                  show-word-limit
                  placeholder="未设置时默认显示用户名"
                />
              </el-form-item>

              <el-form-item label="个性签名" prop="bio">
                <el-input
                  v-model="profileForm.bio"
                  type="textarea"
                  :rows="5"
                  maxlength="150"
                  show-word-limit
                  placeholder="写一句你想留在个人资料里的探索说明"
                />
              </el-form-item>
            </el-form>
          </section>

          <section class="settings-card front-panel">
            <div class="card-head">
              <div>
                <span class="front-kicker">安全设置</span>
                <h2>账号安全</h2>
              </div>
              <el-button type="danger" :loading="passwordSaving" @click="submitPassword">更新密码</el-button>
            </div>

            <div class="safety-note">
              <strong>修改密码后将自动退出登录</strong>
              <p>请使用新密码重新登录，以确保账号安全状态即时生效。</p>
            </div>

            <el-form
              ref="passwordFormRef"
              :model="passwordForm"
              :rules="passwordRules"
              label-position="top"
              class="settings-form"
            >
              <el-form-item label="旧密码" prop="oldPassword">
                <el-input v-model="passwordForm.oldPassword" type="password" show-password />
              </el-form-item>

              <el-form-item label="新密码" prop="newPassword">
                <el-input v-model="passwordForm.newPassword" type="password" show-password />
              </el-form-item>

              <el-form-item label="确认新密码" prop="confirmPassword">
                <el-input v-model="passwordForm.confirmPassword" type="password" show-password />
              </el-form-item>
            </el-form>
          </section>
        </div>

        <section class="settings-card front-panel favorites-section">
          <div class="card-head">
            <div>
              <span class="front-kicker">我的收藏</span>
              <h2>收藏的地点</h2>
            </div>
          </div>

          <el-skeleton v-if="favoritesLoading && !favorites.length" :rows="3" animated />

          <div v-else-if="favorites.length" class="favorites-grid">
            <div v-for="item in favorites" :key="item.poiId" class="favorite-card" @click="goToPoi(item)">
              <div class="favorite-card-main">
                <span class="favorite-category">{{ item.category }}</span>
                <strong class="favorite-name">{{ item.poiName }}</strong>
                <p v-if="item.description" class="favorite-desc">{{ item.description }}</p>
              </div>
              <div class="favorite-meta">
                <span>{{ item.latitude }}, {{ item.longitude }}</span>
                <time>收藏于 {{ formatTime(item.favoritedAt) }}</time>
              </div>
            </div>
          </div>

          <el-empty v-else description="还没有收藏任何地点，去地图上探索并收藏你感兴趣的地点吧" />

          <div v-if="favoritesHasMore" class="favorites-footer">
            <el-button :loading="favoritesLoadingMore" @click="loadMoreFavorites">加载更多</el-button>
          </div>
        </section>

        <section class="settings-card front-panel reviews-section">
          <div class="card-head">
            <div>
              <span class="front-kicker">我的评价</span>
              <h2>评价记录</h2>
            </div>
          </div>

          <el-skeleton v-if="reviewsLoading && !reviews.length" :rows="3" animated />

          <div v-else-if="reviews.length" class="reviews-list">
            <div v-for="review in reviews" :key="review.id" class="review-item-card">
              <div class="review-item-head">
                <strong class="review-poi-name">{{ review.poiName }}</strong>
                <span class="review-stars">
                  <span v-for="s in 5" :key="s" :class="{ filled: s <= review.rating }">&#9733;</span>
                </span>
                <el-button
                  text
                  type="danger"
                  size="small"
                  :loading="review.deleting"
                  @click="handleDeleteReview(review)"
                >
                  删除
                </el-button>
              </div>
              <p v-if="review.content" class="review-item-text">{{ review.content }}</p>
              <time class="review-item-time">{{ formatTime(review.createdAt) }}</time>
            </div>
          </div>

          <el-empty v-else description="还没有发表过评价，去给去过的地点打个分吧" />

          <div v-if="reviewsHasMore" class="favorites-footer">
            <el-button :loading="reviewsLoadingMore" @click="loadMoreReviews">加载更多</el-button>
          </div>
        </section>

        <section class="settings-card front-panel achievements-overview">
          <div class="card-head">
            <div>
              <span class="front-kicker">成就</span>
              <h2>我的成就</h2>
            </div>
          </div>

          <el-skeleton v-if="achLoading && !achList.length" :rows="3" animated />

          <div v-else-if="achList.length" class="ach-grid">
            <div
              v-for="ach in achList"
              :key="ach.id"
              :class="['ach-badge', { locked: !ach.unlocked }]"
            >
              <span class="ach-icon">{{ ach.unlocked ? '★' : '?' }}</span>
              <div class="ach-info">
                <strong>{{ ach.name }}</strong>
                <span>{{ ach.description }}</span>
                <time v-if="ach.unlocked && ach.unlockedAt">{{ formatTime(ach.unlockedAt) }}</time>
              </div>
            </div>
          </div>

          <el-empty v-else description="暂无成就" />
        </section>

        <section class="settings-card front-panel my-applications-section">
          <div class="card-head">
            <div>
              <span class="front-kicker">地点共建</span>
              <h2>我的地点申请</h2>
            </div>
          </div>

          <el-skeleton v-if="myAppsLoading && !myApps.length" :rows="2" animated />

          <div v-else-if="myApps.length" class="applications-list">
            <div v-for="app in myApps" :key="app.id" class="application-item">
              <div class="application-main">
                <strong class="application-name">{{ app.name }}</strong>
                <el-tag size="small" :type="appStatusType(app.status)" effect="plain">{{ appStatusLabel(app.status) }}</el-tag>
              </div>
              <div class="application-meta">
                <span class="application-category">{{ app.category }}</span>
                <span class="application-time">{{ formatAppTime(app.createdAt) }}</span>
              </div>
              <div v-if="app.reviewNote" class="application-note">审核意见：{{ app.reviewNote }}</div>
            </div>
            <div v-if="myAppsHasMore" class="load-more">
              <el-button link type="primary" :loading="myAppsLoadingMore" @click="loadMoreApps">加载更多</el-button>
            </div>
          </div>

          <el-empty v-else description="暂无申请记录" :image-size="48" />
        </section>

        <section class="settings-card front-panel my-routes-section">
          <div class="card-head">
            <div>
              <span class="front-kicker">路线管理</span>
              <h2>我创建的路线</h2>
            </div>
            <router-link to="/route/create">
              <el-button type="primary" size="small">创建路线</el-button>
            </router-link>
          </div>

          <el-skeleton v-if="myRoutesLoading && !myRoutes.length" :rows="3" animated />

          <div v-else-if="myRoutes.length" class="routes-grid">
            <router-link v-for="r in myRoutes" :key="r.id" :to="'/route/' + r.id" class="route-card-sm">
              <strong>{{ r.title }}</strong>
              <span class="route-meta-sm">{{ r.waypointCount }} 站 · {{ r.likeCount }} 赞</span>
              <time>{{ formatTime(r.createdAt) }}</time>
            </router-link>
          </div>

          <el-empty v-else description="还没有创建路线" />

          <div v-if="myRoutesHasMore" class="favorites-footer">
            <el-button :loading="myRoutesLoadingMore" @click="loadMoreMyRoutes">加载更多</el-button>
          </div>
        </section>

        <section class="settings-card front-panel fav-routes-section">
          <div class="card-head">
            <div>
              <span class="front-kicker">路线收藏</span>
              <h2>收藏的路线</h2>
            </div>
          </div>

          <el-skeleton v-if="favRoutesLoading && !favRoutes.length" :rows="3" animated />

          <div v-else-if="favRoutes.length" class="routes-grid">
            <router-link v-for="r in favRoutes" :key="r.id" :to="'/route/' + r.id" class="route-card-sm">
              <strong>{{ r.title }}</strong>
              <span class="route-meta-sm">{{ r.waypointCount }} 站 · {{ r.likeCount }} 赞</span>
              <time>{{ formatTime(r.createdAt) }}</time>
            </router-link>
          </div>

          <el-empty v-else description="还没有收藏路线" />

          <div v-if="favRoutesHasMore" class="favorites-footer">
            <el-button :loading="favRoutesLoadingMore" @click="loadMoreFavRoutes">加载更多</el-button>
          </div>
        </section>

        <section class="settings-card front-panel social-section">
          <div class="card-head">
            <div>
              <span class="front-kicker">社交关系</span>
              <h2>我的关注</h2>
            </div>
          </div>

          <el-skeleton v-if="followingLoading && !followingList.length" :rows="3" animated />

          <div v-else-if="followingList.length" class="follow-list">
            <router-link
              v-for="item in followingList"
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

          <div v-if="followingHasMore" class="favorites-footer">
            <el-button :loading="followingLoadingMore" @click="loadMoreFollowing">加载更多</el-button>
          </div>
        </section>

        <section class="settings-card front-panel followers-section">
          <div class="card-head">
            <div>
              <span class="front-kicker">社交关系</span>
              <h2>我的粉丝</h2>
            </div>
          </div>

          <el-skeleton v-if="followersLoading && !followersList.length" :rows="3" animated />

          <div v-else-if="followersList.length" class="follow-list">
            <router-link
              v-for="item in followersList"
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

          <div v-if="followersHasMore" class="favorites-footer">
            <el-button :loading="followersLoadingMore" @click="loadMoreFollowers">加载更多</el-button>
          </div>
        </section>
      </div>
    </main>

    <Footer />

    <AvatarCropperDialog
      :visible="cropperVisible"
      :image-url="cropperImageUrl"
      @cancel="closeCropper"
      @confirm="uploadCroppedAvatar"
    />
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import Header from '@/components/common/Header.vue'
import Footer from '@/components/common/Footer.vue'
import AvatarCropperDialog from '@/components/user/AvatarCropperDialog.vue'
import { changeMyPassword, updateMyProfile, uploadMyAvatar } from '@/api/user.js'
import { useUserStore } from '@/stores/user'
import { getUserFavorites } from '@/api/poiFavorite'
import { getUserReviews, deleteReview } from '@/api/poiReview'
import { getFollowingList, getFollowerList } from '@/api/userFollow'
import { getMyRoutes, getMyFavoriteRoutes } from '@/api/userRoute'
import { getMyAchievements } from '@/api/achievement'
import { getMyApplications } from '@/api/poiApplication'

const router = useRouter()
const userStore = useUserStore()

const profileFormRef = ref(null)
const passwordFormRef = ref(null)
const cropperVisible = ref(false)
const cropperImageUrl = ref('')
const avatarUploading = ref(false)
const profileSaving = ref(false)
const passwordSaving = ref(false)

const profileForm = reactive({
  displayName: '',
  bio: ''
})

const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const displayName = computed(() => userStore.displayName || '当前用户')

const FAVORITES_PAGE_SIZE = 10
const favorites = ref([])
const favoritesLoading = ref(false)
const favoritesLoadingMore = ref(false)
const favoritesPage = ref(0)
const favoritesHasMore = ref(false)
const favoritesTotal = ref(0)

const dateTimeFormatter = new Intl.DateTimeFormat('zh-CN', {
  year: 'numeric',
  month: '2-digit',
  day: '2-digit',
  hour: '2-digit',
  minute: '2-digit'
})

const formatTime = (value) => {
  if (!value) return ''
  return dateTimeFormatter.format(new Date(value))
}

const loadFavorites = async (reset = false) => {
  const nextPage = reset ? 0 : favoritesPage.value + 1
  const loadingRef = reset ? favoritesLoading : favoritesLoadingMore
  loadingRef.value = true

  try {
    const data = await getUserFavorites({ page: nextPage, size: FAVORITES_PAGE_SIZE })
    const records = data?.records || []
    favorites.value = reset ? records : [...favorites.value, ...records]
    favoritesTotal.value = data?.total || 0
    favoritesPage.value = data?.page || nextPage
    favoritesHasMore.value = Boolean(data?.hasNext)
  } catch {
    // 静默处理
  } finally {
    loadingRef.value = false
  }
}

const loadMoreFavorites = async () => {
  await loadFavorites(false)
}

const goToPoi = (item) => {
  router.push({ name: 'Home', query: { poiId: item.poiId } })
}

const REVIEWS_PAGE_SIZE = 10
const reviews = ref([])
const reviewsLoading = ref(false)
const reviewsLoadingMore = ref(false)
const reviewsPage = ref(0)
const reviewsHasMore = ref(false)

const loadReviews = async (reset = false) => {
  const nextPage = reset ? 0 : reviewsPage.value + 1
  const loadingRef = reset ? reviewsLoading : reviewsLoadingMore
  loadingRef.value = true

  try {
    const data = await getUserReviews({ page: nextPage, size: REVIEWS_PAGE_SIZE })
    const records = (data?.records || []).map(r => ({ ...r, deleting: false }))
    reviews.value = reset ? records : [...reviews.value, ...records]
    reviewsPage.value = data?.page || nextPage
    reviewsHasMore.value = Boolean(data?.hasNext)
  } catch {
    // 静默
  } finally {
    loadingRef.value = false
  }
}

const loadMoreReviews = async () => {
  await loadReviews(false)
}

const handleDeleteReview = async (review) => {
  try {
    await ElMessageBox.confirm('删除后无法恢复，是否继续？', '删除评价', {
      type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消'
    })
  } catch {
    return
  }

  review.deleting = true
  try {
    await deleteReview(review.id)
    reviews.value = reviews.value.filter(r => r.id !== review.id)
    ElMessage.success('评价已删除')
  } catch (error) {
    ElMessage.error(error.message || '删除失败')
  } finally {
    review.deleting = false
  }
}

const profileRules = {
  displayName: [{ max: 100, message: '展示名不能超过 100 个字符', trigger: 'blur' }],
  bio: [{ max: 150, message: '个性签名不能超过 150 个字符', trigger: 'blur' }]
}

const validateConfirmPassword = (rule, value, callback) => {
  if (!value) {
    callback(new Error('请再次输入新密码'))
    return
  }

  if (value !== passwordForm.newPassword) {
    callback(new Error('两次输入的新密码不一致'))
    return
  }

  callback()
}

const passwordRules = {
  oldPassword: [{ required: true, message: '请输入旧密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 100, message: '新密码长度必须在 6 到 100 个字符之间', trigger: 'blur' }
  ],
  confirmPassword: [{ validator: validateConfirmPassword, trigger: 'blur' }]
}

const loadProfile = async () => {
  try {
    const profile = await userStore.fetchMyProfile()
    profileForm.displayName = profile?.displayName || ''
    profileForm.bio = profile?.bio || ''
  } catch (error) {
    ElMessage.error(error.message || '加载用户资料失败')
  }
}

const submitProfile = async () => {
  const valid = await profileFormRef.value?.validate().catch(() => false)
  if (!valid) {
    return
  }

  profileSaving.value = true
  try {
    const profile = await updateMyProfile({
      displayName: profileForm.displayName,
      bio: profileForm.bio
    })
    userStore.updateUserInfo(profile)
    profileForm.displayName = profile.displayName || ''
    profileForm.bio = profile.bio || ''
    ElMessage.success('资料已更新')
  } catch (error) {
    ElMessage.error(error.message || '资料更新失败')
  } finally {
    profileSaving.value = false
  }
}

const submitPassword = async () => {
  const valid = await passwordFormRef.value?.validate().catch(() => false)
  if (!valid) {
    return
  }

  passwordSaving.value = true
  try {
    await changeMyPassword({ ...passwordForm })
    ElMessage.success('密码修改成功，请重新登录')
    userStore.logout()
    router.push('/login')
  } catch (error) {
    ElMessage.error(error.message || '密码修改失败')
  } finally {
    passwordSaving.value = false
  }
}

const handleAvatarSelect = (uploadFile) => {
  const rawFile = uploadFile.raw
  if (!rawFile) {
    return
  }

  if (rawFile.size > 2 * 1024 * 1024) {
    ElMessage.error('头像大小不能超过 2MB')
    return
  }

  revokeCropperUrl()
  cropperImageUrl.value = URL.createObjectURL(rawFile)
  cropperVisible.value = true
}

const uploadCroppedAvatar = async (file) => {
  avatarUploading.value = true
  try {
    const profile = await uploadMyAvatar(file)
    userStore.updateUserInfo(profile)
    ElMessage.success('头像已更新')
    closeCropper()
  } catch (error) {
    ElMessage.error(error.message || '头像上传失败')
  } finally {
    avatarUploading.value = false
  }
}

const closeCropper = () => {
  cropperVisible.value = false
  revokeCropperUrl()
}

const FOLLOW_PAGE_SIZE = 10
const followingList = ref([])
const followingLoading = ref(false)
const followingLoadingMore = ref(false)
const followingPage = ref(0)
const followingHasMore = ref(false)

const followersList = ref([])
const followersLoading = ref(false)
const followersLoadingMore = ref(false)
const followersPage = ref(0)
const followersHasMore = ref(false)

const loadFollowing = async (userId, reset = false) => {
  if (!userId) return
  const nextPage = reset ? 0 : followingPage.value + 1
  const loadingRef = reset ? followingLoading : followingLoadingMore
  loadingRef.value = true

  try {
    const data = await getFollowingList(userId, { page: nextPage, size: FOLLOW_PAGE_SIZE })
    const records = data?.records || []
    followingList.value = reset ? records : [...followingList.value, ...records]
    followingPage.value = data?.page || nextPage
    followingHasMore.value = Boolean(data?.hasNext)
  } catch {
    // 静默
  } finally {
    loadingRef.value = false
  }
}

const loadMoreFollowing = () => loadFollowing(userStore.userInfo?.id, false)

const loadFollowers = async (userId, reset = false) => {
  if (!userId) return
  const nextPage = reset ? 0 : followersPage.value + 1
  const loadingRef = reset ? followersLoading : followersLoadingMore
  loadingRef.value = true

  try {
    const data = await getFollowerList(userId, { page: nextPage, size: FOLLOW_PAGE_SIZE })
    const records = data?.records || []
    followersList.value = reset ? records : [...followersList.value, ...records]
    followersPage.value = data?.page || nextPage
    followersHasMore.value = Boolean(data?.hasNext)
  } catch {
    // 静默
  } finally {
    loadingRef.value = false
  }
}

const loadMoreFollowers = () => loadFollowers(userStore.userInfo?.id, false)

const achList = ref([])
const achLoading = ref(false)

const loadAchievements = async () => {
  achLoading.value = true
  try {
    achList.value = await getMyAchievements()
  } catch {
    // 静默
  } finally {
    achLoading.value = false
  }
}

const ROUTE_PAGE_SIZE = 6
const myRoutes = ref([])
const myRoutesLoading = ref(false)
const myRoutesLoadingMore = ref(false)
const myRoutesPage = ref(0)
const myRoutesHasMore = ref(false)

const favRoutes = ref([])
const favRoutesLoading = ref(false)
const favRoutesLoadingMore = ref(false)
const favRoutesPage = ref(0)
const favRoutesHasMore = ref(false)

const loadMyRoutes = async (reset = false) => {
  const nextPage = reset ? 0 : myRoutesPage.value + 1
  const loadingRef = reset ? myRoutesLoading : myRoutesLoadingMore
  loadingRef.value = true

  try {
    const data = await getMyRoutes({ page: nextPage, size: ROUTE_PAGE_SIZE })
    const records = data?.records || []
    myRoutes.value = reset ? records : [...myRoutes.value, ...records]
    myRoutesPage.value = data?.page || nextPage
    myRoutesHasMore.value = Boolean(data?.hasNext)
  } catch {
    // 静默
  } finally {
    loadingRef.value = false
  }
}

const loadMoreMyRoutes = () => loadMyRoutes(false)

const loadFavRoutes = async (reset = false) => {
  const nextPage = reset ? 0 : favRoutesPage.value + 1
  const loadingRef = reset ? favRoutesLoading : favRoutesLoadingMore
  loadingRef.value = true

  try {
    const data = await getMyFavoriteRoutes({ page: nextPage, size: ROUTE_PAGE_SIZE })
    const records = data?.records || []
    favRoutes.value = reset ? records : [...favRoutes.value, ...records]
    favRoutesPage.value = data?.page || nextPage
    favRoutesHasMore.value = Boolean(data?.hasNext)
  } catch {
    // 静默
  } finally {
    loadingRef.value = false
  }
}

const loadMoreFavRoutes = () => loadFavRoutes(false)

const revokeCropperUrl = () => {
  if (cropperImageUrl.value) {
    URL.revokeObjectURL(cropperImageUrl.value)
    cropperImageUrl.value = ''
  }
}

// ---- 我的地点申请 ----
const myApps = ref([])
const myAppsLoading = ref(false)
const myAppsLoadingMore = ref(false)
const myAppsPage = ref(0)
const myAppsHasMore = ref(false)

const appStatusLabel = (s) => ({ 1: '待审核', 2: '已通过', 3: '已驳回' }[s] || '未知')
const appStatusType = (s) => ({ 1: 'warning', 2: 'success', 3: 'danger' }[s] || 'info')
const formatAppTime = (dt) => {
  if (!dt) return ''
  return new Date(dt).toLocaleDateString('zh-CN')
}

const loadMyApps = async (reset = false) => {
  const nextPage = reset ? 0 : myAppsPage.value + 1
  const loadingRef = reset ? myAppsLoading : myAppsLoadingMore
  loadingRef.value = true
  try {
    const data = await getMyApplications({ page: nextPage, size: 10 })
    const records = data?.records || []
    myApps.value = reset ? records : [...myApps.value, ...records]
    myAppsPage.value = data?.page || nextPage
    myAppsHasMore.value = Boolean(data?.hasNext)
  } catch {} finally {
    loadingRef.value = false
  }
}

const loadMoreApps = () => loadMyApps(false)

onMounted(async () => {
  await loadProfile()
  if (userStore.isLoggedIn) {
    const myId = userStore.userInfo?.id
    await Promise.all([
      loadFavorites(true),
      loadReviews(true),
      loadFollowing(myId, true),
      loadFollowers(myId, true),
      loadMyRoutes(true),
      loadFavRoutes(true),
      loadAchievements(),
      loadMyApps(true)
    ])
  }
})

onBeforeUnmount(() => {
  revokeCropperUrl()
})
</script>

<style scoped>
.settings-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.settings-main {
  flex: 1;
  padding: 22px 0 30px;
}

.settings-shell {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.profile-hero {
  padding: 26px;
  border-radius: var(--front-radius-xl);
  display: grid;
  grid-template-columns: minmax(0, 1.2fr) minmax(280px, 0.68fr);
  gap: 20px;
  align-items: stretch;
}

.hero-main {
  display: flex;
  align-items: center;
  gap: 18px;
}

.hero-avatar {
  background: linear-gradient(135deg, var(--front-accent), var(--front-accent-strong));
  color: #fff;
  font-size: 30px;
  font-weight: 700;
  border: 4px solid rgba(255, 255, 255, 0.94);
  box-shadow: 0 16px 30px rgba(23, 135, 166, 0.16);
}

.hero-side {
  display: flex;
  flex-direction: column;
  gap: 12px;
  align-items: flex-start;
}

.hero-info-card,
.safety-note {
  width: 100%;
  padding: 18px;
  border-radius: 22px;
  border: 1px solid var(--front-border);
  background: rgba(255, 255, 255, 0.72);
}

.hero-info-card span,
.safety-note strong {
  display: block;
}

.hero-info-card span {
  color: var(--front-text-muted);
  font-size: 12px;
}

.hero-info-card strong {
  margin-top: 10px;
  color: var(--front-text);
  font-size: 18px;
}

.hero-info-card p,
.safety-note p {
  margin: 10px 0 0;
  color: var(--front-text-soft);
  font-size: 13px;
  line-height: 1.75;
}

.safety-note {
  background: rgba(255, 247, 237, 0.78);
  border-color: rgba(245, 158, 11, 0.16);
}

.safety-note strong {
  color: #9a3412;
  font-size: 14px;
}

.settings-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 20px;
}

.settings-card {
  padding: 22px;
  border-radius: 28px;
}

.favorites-section {
  margin-top: 20px;
}

.favorites-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.favorite-card {
  padding: 16px 18px;
  border-radius: 18px;
  border: 1px solid var(--front-border);
  background: rgba(255, 255, 255, 0.72);
  cursor: pointer;
  transition: box-shadow 0.2s, border-color 0.2s;
}

.favorite-card:hover {
  border-color: var(--front-accent);
  box-shadow: 0 8px 20px rgba(14, 165, 233, 0.1);
}

.favorite-card-main {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.favorite-category {
  display: inline-flex;
  width: fit-content;
  padding: 3px 10px;
  border-radius: 999px;
  background: var(--front-accent-soft);
  color: var(--front-accent-strong);
  font-size: 11px;
  font-weight: 700;
}

.favorite-name {
  color: var(--front-text);
  font-size: 16px;
}

.favorite-desc {
  margin: 0;
  color: var(--front-text-soft);
  font-size: 13px;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.favorite-meta {
  margin-top: 10px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  flex-wrap: wrap;
}

.favorite-meta span {
  color: var(--front-text-muted);
  font-size: 12px;
  font-family: monospace;
}

.favorite-meta time {
  color: var(--front-text-muted);
  font-size: 12px;
}

.favorites-footer {
  display: flex;
  justify-content: center;
  padding-top: 16px;
}

.reviews-section {
  margin-top: 20px;
}

.reviews-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.review-item-card {
  padding: 16px 18px;
  border-radius: 18px;
  border: 1px solid var(--front-border);
  background: rgba(255, 255, 255, 0.72);
}

.review-item-head {
  display: flex;
  align-items: center;
  gap: 10px;
}

.review-poi-name {
  color: var(--front-text);
  font-size: 15px;
}

.review-stars span {
  font-size: 14px;
  color: #d1d5db;
}

.review-stars span.filled {
  color: #f59e0b;
}

.review-item-text {
  margin: 10px 0 0;
  color: var(--front-text-soft);
  font-size: 14px;
  line-height: 1.6;
  white-space: pre-wrap;
}

.review-item-time {
  display: block;
  margin-top: 8px;
  color: var(--front-text-muted);
  font-size: 12px;
}

.social-section {
  margin-top: 20px;
}

.followers-section {
  margin-top: 20px;
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
  border: 1px solid var(--front-border);
  background: rgba(255, 255, 255, 0.72);
  text-decoration: none;
  color: inherit;
  transition: box-shadow 0.2s, border-color 0.2s;
}

.follow-item:hover {
  border-color: var(--front-accent);
  box-shadow: 0 8px 20px rgba(14, 165, 233, 0.1);
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
  color: var(--front-text);
  font-size: 15px;
}

.follow-bio {
  color: var(--front-text-muted);
  font-size: 13px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.achievements-overview {
  margin-top: 20px;
}

.ach-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.ach-badge {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  border-radius: 16px;
  border: 1px solid var(--front-border);
  background: rgba(255, 255, 255, 0.72);
}

.ach-badge.locked {
  opacity: 0.45;
}

.ach-icon {
  width: 38px;
  height: 38px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  flex-shrink: 0;
  background: linear-gradient(135deg, #fbbf24, #f59e0b);
  color: #fff;
}

.ach-badge.locked .ach-icon {
  background: #e2e8f0;
  color: #94a3b8;
}

.ach-info {
  display: flex;
  flex-direction: column;
  gap: 3px;
  min-width: 0;
}

.ach-info strong {
  color: var(--front-text);
  font-size: 14px;
}

.ach-info span {
  color: var(--front-text-muted);
  font-size: 12px;
}

.ach-info time {
  color: var(--front-text-muted);
  font-size: 11px;
}

.my-routes-section {
  margin-top: 20px;
}

.fav-routes-section {
  margin-top: 20px;
}

.routes-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.route-card-sm {
  padding: 14px 16px;
  border-radius: 16px;
  border: 1px solid var(--front-border);
  background: rgba(255, 255, 255, 0.72);
  text-decoration: none;
  color: inherit;
  display: flex;
  flex-direction: column;
  gap: 6px;
  transition: border-color 0.2s, box-shadow 0.2s;
}

.route-card-sm:hover {
  border-color: var(--front-accent);
  box-shadow: 0 6px 18px rgba(14, 165, 233, 0.1);
}

.route-card-sm strong {
  color: var(--front-text);
  font-size: 15px;
}

.route-meta-sm {
  color: var(--front-text-muted);
  font-size: 12px;
}

.route-card-sm time {
  color: var(--front-text-muted);
  font-size: 12px;
}

.card-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px;
  margin-bottom: 20px;
}

.card-head h2 {
  margin: 12px 0 0;
  color: var(--front-text);
  font-size: 22px;
}

.settings-form :deep(.el-form-item__label) {
  color: var(--front-text);
  font-size: 13px;
  font-weight: 700;
}

.hidden-uploader {
  display: inline-flex;
}

@media (max-width: 1080px) {
  .profile-hero,
  .settings-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 720px) {
  .settings-main {
    padding-top: 16px;
  }

  .profile-hero,
  .settings-card {
    padding: 18px;
    border-radius: 24px;
  }

  .hero-main,
  .card-head {
    flex-direction: column;
    align-items: flex-start;
  }

  .card-head :deep(.el-button),
  .hero-side :deep(.el-button) {
    min-height: 42px;
  }

  .favorites-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 560px) {
  .settings-main {
    padding: 12px 0 22px;
  }

  .settings-shell {
    gap: 14px;
  }

  .profile-hero,
  .settings-card {
    padding: 16px;
    border-radius: 20px;
  }

  .profile-hero {
    gap: 14px;
  }

  .hero-main {
    gap: 14px;
  }

  .hero-avatar {
    width: 78px;
    height: 78px;
    font-size: 24px;
  }

  .hero-copy :deep(.front-title) {
    font-size: 28px;
    line-height: 1.14;
  }

  .hero-copy :deep(.front-description) {
    font-size: 13px;
    line-height: 1.7;
  }

  .hero-info-card,
  .safety-note {
    padding: 14px;
    border-radius: 18px;
  }

  .hero-side {
    align-items: stretch;
  }

  .hidden-uploader,
  .hidden-uploader :deep(.el-button),
  .card-head :deep(.el-button) {
    width: 100%;
  }

  .card-head {
    gap: 10px;
    margin-bottom: 16px;
  }

  .card-head h2 {
    font-size: 20px;
  }

  .settings-form :deep(.el-input__wrapper),
  .settings-form :deep(.el-textarea__inner) {
    border-radius: 14px;
  }

  .settings-form :deep(.el-input__wrapper) {
    min-height: 42px;
  }
}

@media (max-width: 420px) {
  .hero-copy :deep(.front-title) {
    font-size: 24px;
  }

  .hero-copy :deep(.front-description) {
    display: none;
  }

  .hero-info-card strong {
    font-size: 16px;
  }

  .safety-note strong {
    font-size: 13px;
  }
}

.applications-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.application-item {
  padding: 12px;
  border-radius: 10px;
  background: var(--el-fill-color-lighter);
}

.application-main {
  display: flex;
  align-items: center;
  gap: 8px;
}

.application-name {
  font-size: 14px;
  color: var(--front-text);
}

.application-meta {
  display: flex;
  gap: 12px;
  margin-top: 4px;
  font-size: 12px;
  color: var(--front-text-muted);
}

.application-note {
  margin-top: 4px;
  font-size: 12px;
  color: var(--front-text-muted);
}
</style>
