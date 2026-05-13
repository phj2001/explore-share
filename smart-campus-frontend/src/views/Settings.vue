<template>
  <div class="settings-page front-page">
    <Header />

    <main class="settings-main">
      <div class="front-shell settings-shell">

        <!-- 用户身份条 -->
        <div class="profile-strip front-panel">
          <div class="profile-identity">
            <div class="avatar-wrap" @click="triggerAvatarUpload">
              <el-avatar :size="72" :src="userStore.avatarUrl || undefined" class="strip-avatar">
                {{ displayName.slice(0, 1).toUpperCase() }}
              </el-avatar>
              <div class="avatar-overlay"><span>更换头像</span></div>
            </div>
            <input
              ref="fileInputRef"
              type="file"
              accept=".png,.jpg,.jpeg,.webp,image/png,image/jpeg,image/webp"
              class="hidden-file-input"
              @change="handleFileChange"
            />
            <div class="profile-text">
              <strong class="profile-name">{{ displayName }}</strong>
              <span class="profile-username">@{{ userStore.username }}</span>
              <p class="profile-bio">{{ profileForm.bio || '暂无个性签名' }}</p>
            </div>
          </div>
          <span v-if="avatarUploading" class="avatar-uploading-hint">头像上传中…</span>
        </div>

        <!-- 功能标签页 -->
        <div class="tabs-container front-panel">
          <el-tabs v-model="activeTab" class="settings-tabs" @tab-change="onTabChange">

            <!-- 账号设置 -->
            <el-tab-pane label="账号设置" name="account">
              <div class="tab-grid">
                <section class="settings-card front-panel">
                  <div class="card-head">
                    <div>
                      <span class="front-kicker">资料编辑</span>
                      <h2>基本资料</h2>
                    </div>
                    <el-button type="primary" :loading="profileSaving" @click="submitProfile">保存资料</el-button>
                  </div>
                  <el-form ref="profileFormRef" :model="profileForm" :rules="profileRules" label-position="top" class="settings-form">
                    <el-form-item label="登录账号">
                      <el-input :model-value="userStore.username" disabled />
                    </el-form-item>
                    <el-form-item label="绑定邮箱">
                      <el-input :model-value="userStore.userInfo?.email || '暂未绑定邮箱'" disabled>
                        <template v-if="userStore.userInfo?.email" #suffix>
                          <el-tag size="small" type="success" effect="plain">已验证</el-tag>
                        </template>
                      </el-input>
                    </el-form-item>
                    <el-form-item label="展示名" prop="displayName">
                      <el-input v-model="profileForm.displayName" maxlength="100" show-word-limit placeholder="未设置时默认显示用户名" />
                    </el-form-item>
                    <el-form-item label="个性签名" prop="bio">
                      <el-input v-model="profileForm.bio" type="textarea" :rows="4" maxlength="150" show-word-limit placeholder="写一句你想留在个人资料里的探索说明" />
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
                  <el-form ref="passwordFormRef" :model="passwordForm" :rules="passwordRules" label-position="top" class="settings-form">
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
            </el-tab-pane>

            <!-- 我的收藏 -->
            <el-tab-pane label="我的收藏" name="favorites">
              <div class="sub-tab-bar">
                <button :class="['sub-tab-btn', { active: favSubTab === 'pois' }]" @click="favSubTab = 'pois'">地点收藏</button>
                <button :class="['sub-tab-btn', { active: favSubTab === 'routes' }]" @click="favSubTab = 'routes'">路线收藏</button>
              </div>

              <template v-if="favSubTab === 'pois'">
                <el-skeleton v-if="favoritesLoading && !favorites.length" :rows="4" animated />
                <div v-else-if="favorites.length" class="favorites-grid">
                  <div v-for="item in favorites" :key="item.poiId" class="favorite-card" @click="goToPoi(item)">
                    <div class="favorite-card-main">
                      <span class="favorite-category">{{ item.category }}</span>
                      <strong class="favorite-name">{{ item.poiName }}</strong>
                      <p v-if="item.description" class="favorite-desc">{{ item.description }}</p>
                    </div>
                    <time class="favorite-time">{{ formatTime(item.favoritedAt) }}</time>
                  </div>
                </div>
                <div v-else class="tab-empty">
                  <p>还没有收藏地点，去地图上探索吧</p>
                  <router-link to="/"><el-button type="primary" size="small">去探索</el-button></router-link>
                </div>
                <div v-if="favoritesHasMore" class="load-more-row">
                  <el-button :loading="favoritesLoadingMore" @click="loadMoreFavorites">加载更多</el-button>
                </div>
              </template>

              <template v-else>
                <el-skeleton v-if="favRoutesLoading && !favRoutes.length" :rows="4" animated />
                <div v-else-if="favRoutes.length" class="routes-grid">
                  <router-link v-for="r in favRoutes" :key="r.id" :to="'/route/' + r.id" class="route-card-sm">
                    <strong>{{ r.title }}</strong>
                    <span class="route-meta-sm">{{ r.waypointCount }} 站 · {{ r.likeCount }} 赞</span>
                    <time>{{ formatTime(r.createdAt) }}</time>
                  </router-link>
                </div>
                <div v-else class="tab-empty"><p>还没有收藏路线</p></div>
                <div v-if="favRoutesHasMore" class="load-more-row">
                  <el-button :loading="favRoutesLoadingMore" @click="loadMoreFavRoutes">加载更多</el-button>
                </div>
              </template>
            </el-tab-pane>

            <!-- 评价记录 -->
            <el-tab-pane label="评价记录" name="reviews">
              <el-skeleton v-if="reviewsLoading && !reviews.length" :rows="4" animated />
              <div v-else-if="reviews.length" class="reviews-list">
                <div v-for="review in reviews" :key="review.id" class="review-item-card">
                  <div class="review-item-head">
                    <strong class="review-poi-name">{{ review.poiName }}</strong>
                    <span class="review-stars">
                      <span v-for="s in 5" :key="s" :class="{ filled: s <= review.rating }">&#9733;</span>
                    </span>
                    <el-button text type="danger" size="small" :loading="review.deleting" @click="handleDeleteReview(review)">删除</el-button>
                  </div>
                  <p v-if="review.content" class="review-item-text">{{ review.content }}</p>
                  <time class="review-item-time">{{ formatTime(review.createdAt) }}</time>
                </div>
              </div>
              <div v-else class="tab-empty"><p>还没有发表过评价，去地点打个分吧</p></div>
              <div v-if="reviewsHasMore" class="load-more-row">
                <el-button :loading="reviewsLoadingMore" @click="loadMoreReviews">加载更多</el-button>
              </div>
            </el-tab-pane>

            <!-- 路线 & 申请 -->
            <el-tab-pane label="路线 & 申请" name="routes">
              <div class="sub-tab-bar">
                <button :class="['sub-tab-btn', { active: routeSubTab === 'mine' }]" @click="routeSubTab = 'mine'">我的路线</button>
                <button :class="['sub-tab-btn', { active: routeSubTab === 'apps' }]" @click="routeSubTab = 'apps'">申请记录</button>
              </div>

              <template v-if="routeSubTab === 'mine'">
                <div class="sub-tab-action">
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
                <div v-else class="tab-empty">
                  <p>还没有创建路线</p>
                  <router-link to="/route/create"><el-button type="primary" size="small">立即创建</el-button></router-link>
                </div>
                <div v-if="myRoutesHasMore" class="load-more-row">
                  <el-button :loading="myRoutesLoadingMore" @click="loadMoreMyRoutes">加载更多</el-button>
                </div>
              </template>

              <template v-else>
                <el-skeleton v-if="myAppsLoading && !myApps.length" :rows="3" animated />
                <div v-else-if="myApps.length" class="applications-list">
                  <div v-for="app in myApps" :key="app.id" class="application-item">
                    <div class="application-main">
                      <strong class="application-name">{{ app.name }}</strong>
                      <el-tag size="small" :type="appStatusType(app.status)" effect="plain">{{ appStatusLabel(app.status) }}</el-tag>
                    </div>
                    <div class="application-meta">
                      <span>{{ app.category }}</span>
                      <span>{{ formatAppTime(app.createdAt) }}</span>
                    </div>
                    <div v-if="app.reviewNote" class="application-note">审核意见：{{ app.reviewNote }}</div>
                  </div>
                  <div v-if="myAppsHasMore" class="load-more-row">
                    <el-button link type="primary" :loading="myAppsLoadingMore" @click="loadMoreApps">加载更多</el-button>
                  </div>
                </div>
                <div v-else class="tab-empty"><p>暂无地点申请记录</p></div>
              </template>
            </el-tab-pane>

            <!-- 社交 -->
            <el-tab-pane label="社交" name="social">
              <div class="social-grid">
                <div class="social-col">
                  <div class="social-col-head">
                    <h3>我的关注</h3>
                    <span v-if="followingList.length" class="social-count">{{ followingList.length }}{{ followingHasMore ? '+' : '' }}</span>
                  </div>
                  <el-skeleton v-if="followingLoading && !followingList.length" :rows="3" animated />
                  <div v-else-if="followingList.length" class="follow-list">
                    <router-link v-for="item in followingList" :key="item.userId" :to="'/user/' + item.userId" class="follow-item">
                      <el-avatar :size="36" :src="item.avatarUrl || undefined" class="follow-avatar">
                        {{ (item.displayName || 'U').slice(0, 1).toUpperCase() }}
                      </el-avatar>
                      <div class="follow-info">
                        <strong>{{ item.displayName || item.username }}</strong>
                        <span v-if="item.bio" class="follow-bio">{{ item.bio }}</span>
                      </div>
                    </router-link>
                    <div v-if="followingHasMore" class="load-more-row">
                      <el-button size="small" :loading="followingLoadingMore" @click="loadMoreFollowing">加载更多</el-button>
                    </div>
                  </div>
                  <div v-else class="tab-empty-sm">还没有关注任何人</div>
                </div>

                <div class="social-col">
                  <div class="social-col-head">
                    <h3>我的粉丝</h3>
                    <span v-if="followersList.length" class="social-count">{{ followersList.length }}{{ followersHasMore ? '+' : '' }}</span>
                  </div>
                  <el-skeleton v-if="followersLoading && !followersList.length" :rows="3" animated />
                  <div v-else-if="followersList.length" class="follow-list">
                    <router-link v-for="item in followersList" :key="item.userId" :to="'/user/' + item.userId" class="follow-item">
                      <el-avatar :size="36" :src="item.avatarUrl || undefined" class="follow-avatar">
                        {{ (item.displayName || 'U').slice(0, 1).toUpperCase() }}
                      </el-avatar>
                      <div class="follow-info">
                        <strong>{{ item.displayName || item.username }}</strong>
                        <span v-if="item.bio" class="follow-bio">{{ item.bio }}</span>
                      </div>
                    </router-link>
                    <div v-if="followersHasMore" class="load-more-row">
                      <el-button size="small" :loading="followersLoadingMore" @click="loadMoreFollowers">加载更多</el-button>
                    </div>
                  </div>
                  <div v-else class="tab-empty-sm">还没有粉丝</div>
                </div>
              </div>
            </el-tab-pane>

            <!-- 成就 -->
            <el-tab-pane label="成就" name="achievements">
              <el-skeleton v-if="achLoading && !achList.length" :rows="4" animated />
              <div v-else-if="achList.length" class="ach-grid">
                <div v-for="ach in achList" :key="ach.id" :class="['ach-badge', { locked: !ach.unlocked }]">
                  <span class="ach-icon">{{ ach.unlocked ? '★' : '?' }}</span>
                  <div class="ach-info">
                    <strong>{{ ach.name }}</strong>
                    <span>{{ ach.description }}</span>
                    <time v-if="ach.unlocked && ach.unlockedAt">{{ formatTime(ach.unlockedAt) }}</time>
                  </div>
                </div>
              </div>
              <div v-else class="tab-empty"><p>暂无成就记录</p></div>
            </el-tab-pane>

          </el-tabs>
        </div>

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
const fileInputRef = ref(null)
const cropperVisible = ref(false)
const cropperImageUrl = ref('')
const avatarUploading = ref(false)
const profileSaving = ref(false)
const passwordSaving = ref(false)

const activeTab = ref('account')
const loadedTabs = ref(new Set(['account']))
const favSubTab = ref('pois')
const routeSubTab = ref('mine')

const profileForm = reactive({ displayName: '', bio: '' })
const passwordForm = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })
const displayName = computed(() => userStore.displayName || userStore.username || '当前用户')

// ---- Avatar ----
const triggerAvatarUpload = () => fileInputRef.value?.click()

const handleFileChange = (event) => {
  const rawFile = event.target.files?.[0]
  event.target.value = ''
  if (!rawFile) return
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

const revokeCropperUrl = () => {
  if (cropperImageUrl.value) {
    URL.revokeObjectURL(cropperImageUrl.value)
    cropperImageUrl.value = ''
  }
}

// ---- Tab lazy loading ----
const onTabChange = (name) => {
  if (loadedTabs.value.has(name)) return
  loadedTabs.value.add(name)
  const myId = userStore.userInfo?.id
  const loaders = {
    favorites:    () => { loadFavorites(true); loadFavRoutes(true) },
    reviews:      () => loadReviews(true),
    routes:       () => { loadMyRoutes(true); loadMyApps(true) },
    social:       () => { loadFollowing(myId, true); loadFollowers(myId, true) },
    achievements: () => loadAchievements(),
  }
  loaders[name]?.()
}

// ---- Profile ----
const profileRules = {
  displayName: [{ max: 100, message: '展示名不能超过 100 个字符', trigger: 'blur' }],
  bio:         [{ max: 150, message: '个性签名不能超过 150 个字符', trigger: 'blur' }]
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
  if (!valid) return
  profileSaving.value = true
  try {
    const profile = await updateMyProfile({ displayName: profileForm.displayName, bio: profileForm.bio })
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

// ---- Password ----
const validateConfirmPassword = (rule, value, callback) => {
  if (!value) { callback(new Error('请再次输入新密码')); return }
  if (value !== passwordForm.newPassword) { callback(new Error('两次输入的新密码不一致')); return }
  callback()
}

const passwordRules = {
  oldPassword:     [{ required: true, message: '请输入旧密码', trigger: 'blur' }],
  newPassword:     [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 100, message: '新密码长度必须在 6 到 100 个字符之间', trigger: 'blur' }
  ],
  confirmPassword: [{ validator: validateConfirmPassword, trigger: 'blur' }]
}

const submitPassword = async () => {
  const valid = await passwordFormRef.value?.validate().catch(() => false)
  if (!valid) return
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

// ---- Shared ----
const PAGE_SIZE = 10
const ROUTE_PAGE_SIZE = 6

const dateTimeFormatter = new Intl.DateTimeFormat('zh-CN', {
  year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit'
})
const formatTime = (value) => value ? dateTimeFormatter.format(new Date(value)) : ''
const formatAppTime = (dt) => dt ? new Date(dt).toLocaleDateString('zh-CN') : ''

// ---- Favorites POI ----
const favorites = ref([])
const favoritesLoading = ref(false)
const favoritesLoadingMore = ref(false)
const favoritesPage = ref(0)
const favoritesHasMore = ref(false)

const loadFavorites = async (reset = false) => {
  const nextPage = reset ? 0 : favoritesPage.value + 1
  const loadingRef = reset ? favoritesLoading : favoritesLoadingMore
  loadingRef.value = true
  try {
    const data = await getUserFavorites({ page: nextPage, size: PAGE_SIZE })
    const records = data?.records || []
    favorites.value = reset ? records : [...favorites.value, ...records]
    favoritesPage.value = data?.page || nextPage
    favoritesHasMore.value = Boolean(data?.hasNext)
  } catch {} finally { loadingRef.value = false }
}

const loadMoreFavorites = () => loadFavorites(false)
const goToPoi = (item) => router.push({ name: 'Home', query: { poiId: item.poiId } })

// ---- Reviews ----
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
    const data = await getUserReviews({ page: nextPage, size: PAGE_SIZE })
    const records = (data?.records || []).map(r => ({ ...r, deleting: false }))
    reviews.value = reset ? records : [...reviews.value, ...records]
    reviewsPage.value = data?.page || nextPage
    reviewsHasMore.value = Boolean(data?.hasNext)
  } catch {} finally { loadingRef.value = false }
}

const loadMoreReviews = () => loadReviews(false)

const handleDeleteReview = async (review) => {
  try {
    await ElMessageBox.confirm('删除后无法恢复，是否继续？', '删除评价', {
      type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消'
    })
  } catch { return }
  review.deleting = true
  try {
    await deleteReview(review.id)
    reviews.value = reviews.value.filter(r => r.id !== review.id)
    ElMessage.success('评价已删除')
  } catch (error) {
    ElMessage.error(error.message || '删除失败')
  } finally { review.deleting = false }
}

// ---- My Routes ----
const myRoutes = ref([])
const myRoutesLoading = ref(false)
const myRoutesLoadingMore = ref(false)
const myRoutesPage = ref(0)
const myRoutesHasMore = ref(false)

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
  } catch {} finally { loadingRef.value = false }
}

const loadMoreMyRoutes = () => loadMyRoutes(false)

// ---- Fav Routes ----
const favRoutes = ref([])
const favRoutesLoading = ref(false)
const favRoutesLoadingMore = ref(false)
const favRoutesPage = ref(0)
const favRoutesHasMore = ref(false)

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
  } catch {} finally { loadingRef.value = false }
}

const loadMoreFavRoutes = () => loadFavRoutes(false)

// ---- Social ----
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
    const data = await getFollowingList(userId, { page: nextPage, size: PAGE_SIZE })
    const records = data?.records || []
    followingList.value = reset ? records : [...followingList.value, ...records]
    followingPage.value = data?.page || nextPage
    followingHasMore.value = Boolean(data?.hasNext)
  } catch {} finally { loadingRef.value = false }
}

const loadMoreFollowing = () => loadFollowing(userStore.userInfo?.id, false)

const loadFollowers = async (userId, reset = false) => {
  if (!userId) return
  const nextPage = reset ? 0 : followersPage.value + 1
  const loadingRef = reset ? followersLoading : followersLoadingMore
  loadingRef.value = true
  try {
    const data = await getFollowerList(userId, { page: nextPage, size: PAGE_SIZE })
    const records = data?.records || []
    followersList.value = reset ? records : [...followersList.value, ...records]
    followersPage.value = data?.page || nextPage
    followersHasMore.value = Boolean(data?.hasNext)
  } catch {} finally { loadingRef.value = false }
}

const loadMoreFollowers = () => loadFollowers(userStore.userInfo?.id, false)

// ---- Achievements ----
const achList = ref([])
const achLoading = ref(false)

const loadAchievements = async () => {
  achLoading.value = true
  try { achList.value = await getMyAchievements() }
  catch {} finally { achLoading.value = false }
}

// ---- My Applications ----
const myApps = ref([])
const myAppsLoading = ref(false)
const myAppsLoadingMore = ref(false)
const myAppsPage = ref(0)
const myAppsHasMore = ref(false)

const appStatusLabel = (s) => ({ 1: '待审核', 2: '已通过', 3: '已驳回' }[s] || '未知')
const appStatusType  = (s) => ({ 1: 'warning', 2: 'success', 3: 'danger'  }[s] || 'info')

const loadMyApps = async (reset = false) => {
  const nextPage = reset ? 0 : myAppsPage.value + 1
  const loadingRef = reset ? myAppsLoading : myAppsLoadingMore
  loadingRef.value = true
  try {
    const data = await getMyApplications({ page: nextPage, size: PAGE_SIZE })
    const records = data?.records || []
    myApps.value = reset ? records : [...myApps.value, ...records]
    myAppsPage.value = data?.page || nextPage
    myAppsHasMore.value = Boolean(data?.hasNext)
  } catch {} finally { loadingRef.value = false }
}

const loadMoreApps = () => loadMyApps(false)

// ---- Lifecycle ----
onMounted(() => loadProfile())
onBeforeUnmount(() => revokeCropperUrl())
</script>

<style scoped>
.settings-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.settings-main {
  flex: 1;
  padding: 22px 0 36px;
}

.settings-shell {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

/* ---- Profile Strip ---- */
.profile-strip {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 24px;
  gap: 16px;
}

.profile-identity {
  display: flex;
  align-items: center;
  gap: 16px;
}

.avatar-wrap {
  position: relative;
  display: inline-flex;
  cursor: pointer;
  border-radius: 50%;
  flex-shrink: 0;
}

.strip-avatar {
  background: linear-gradient(135deg, var(--front-accent), var(--front-accent-strong));
  color: #fff;
  font-size: 26px;
  font-weight: 700;
  border: 3px solid rgba(255, 255, 255, 0.9);
  box-shadow: 0 8px 20px rgba(23, 135, 166, 0.18);
}

.avatar-overlay {
  position: absolute;
  inset: 0;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.48);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 12px;
  font-weight: 600;
  opacity: 0;
  transition: opacity 0.18s;
  pointer-events: none;
}

.avatar-wrap:hover .avatar-overlay {
  opacity: 1;
}

.hidden-file-input {
  display: none;
}

.profile-text {
  display: flex;
  flex-direction: column;
  gap: 3px;
  min-width: 0;
}

.profile-name {
  color: var(--front-text);
  font-size: 20px;
  font-weight: 700;
  line-height: 1.2;
}

.profile-username {
  color: var(--front-text-muted);
  font-size: 13px;
}

.profile-bio {
  margin: 2px 0 0;
  color: var(--front-text-soft);
  font-size: 13px;
  line-height: 1.5;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 480px;
}

.avatar-uploading-hint {
  font-size: 12px;
  color: var(--front-text-muted);
  flex-shrink: 0;
}

/* ---- Tabs Container ---- */
.tabs-container {
  padding: 0;
  overflow: hidden;
}

.settings-tabs :deep(.el-tabs__header) {
  margin: 0;
  padding: 0 20px;
  background: transparent;
}

.settings-tabs :deep(.el-tabs__nav-wrap::after) {
  height: 1px;
  background: #e2e8f0;
}

.settings-tabs :deep(.el-tabs__item) {
  font-size: 14px;
  font-weight: 600;
  color: #64748b;
  padding: 14px 20px;
  transition: color 0.2s;
}

.settings-tabs :deep(.el-tabs__item:hover) {
  color: #0f172a;
}

.settings-tabs :deep(.el-tabs__item.is-active) {
  color: var(--front-accent, #0ea5e9);
}

.settings-tabs :deep(.el-tabs__active-bar) {
  background: var(--front-accent, #0ea5e9);
  height: 2px;
}

.settings-tabs :deep(.el-tabs__content) {
  padding: 20px 24px 24px;
  overflow: visible;
}

/* ---- Account Tab ---- */
.tab-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 20px;
}

.settings-card {
  padding: 20px;
  border-radius: 20px;
}

.card-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px;
  margin-bottom: 18px;
}

.card-head h2 {
  margin: 8px 0 0;
  color: var(--front-text);
  font-size: 18px;
  font-weight: 700;
}

.settings-form :deep(.el-form-item__label) {
  color: var(--front-text);
  font-size: 13px;
  font-weight: 700;
}

.safety-note {
  padding: 12px 14px;
  border-radius: 14px;
  border: 1px solid rgba(245, 158, 11, 0.18);
  background: rgba(255, 247, 237, 0.78);
  margin-bottom: 14px;
}

.safety-note strong {
  display: block;
  color: #9a3412;
  font-size: 13px;
}

.safety-note p {
  margin: 4px 0 0;
  color: var(--front-text-soft);
  font-size: 12px;
  line-height: 1.6;
}

/* ---- Sub-tab bar ---- */
.sub-tab-bar {
  display: flex;
  gap: 6px;
  margin-bottom: 16px;
}

.sub-tab-btn {
  padding: 5px 16px;
  border-radius: 999px;
  border: 1px solid var(--front-border);
  background: transparent;
  color: #64748b;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.18s;
}

.sub-tab-btn:hover {
  border-color: var(--front-accent);
  color: var(--front-accent);
}

.sub-tab-btn.active {
  background: var(--front-accent-soft);
  border-color: var(--front-accent);
  color: var(--front-accent-strong);
}

.sub-tab-action {
  margin-bottom: 14px;
}

/* ---- Favorites grid ---- */
.favorites-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.favorite-card {
  padding: 14px 16px;
  border-radius: 14px;
  border: 1px solid var(--front-border);
  background: rgba(255, 255, 255, 0.72);
  cursor: pointer;
  transition: box-shadow 0.18s, border-color 0.18s;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.favorite-card:hover {
  border-color: var(--front-accent);
  box-shadow: 0 6px 16px rgba(14, 165, 233, 0.1);
}

.favorite-card-main {
  display: flex;
  flex-direction: column;
  gap: 5px;
  flex: 1;
}

.favorite-category {
  display: inline-flex;
  width: fit-content;
  padding: 2px 8px;
  border-radius: 999px;
  background: var(--front-accent-soft);
  color: var(--front-accent-strong);
  font-size: 11px;
  font-weight: 700;
}

.favorite-name {
  color: var(--front-text);
  font-size: 14px;
}

.favorite-desc {
  margin: 0;
  color: var(--front-text-soft);
  font-size: 12px;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.favorite-time {
  color: var(--front-text-muted);
  font-size: 11px;
}

/* ---- Routes grid ---- */
.routes-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.route-card-sm {
  padding: 14px 16px;
  border-radius: 14px;
  border: 1px solid var(--front-border);
  background: rgba(255, 255, 255, 0.72);
  text-decoration: none;
  color: inherit;
  display: flex;
  flex-direction: column;
  gap: 5px;
  transition: border-color 0.18s, box-shadow 0.18s;
}

.route-card-sm:hover {
  border-color: var(--front-accent);
  box-shadow: 0 6px 16px rgba(14, 165, 233, 0.1);
}

.route-card-sm strong {
  color: var(--front-text);
  font-size: 14px;
}

.route-meta-sm,
.route-card-sm time {
  color: var(--front-text-muted);
  font-size: 12px;
}

/* ---- Reviews ---- */
.reviews-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.review-item-card {
  padding: 14px 18px;
  border-radius: 14px;
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
  font-size: 14px;
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.review-stars span { font-size: 13px; color: #d1d5db; }
.review-stars span.filled { color: #f59e0b; }

.review-item-text {
  margin: 8px 0 0;
  color: var(--front-text-soft);
  font-size: 13px;
  line-height: 1.6;
  white-space: pre-wrap;
}

.review-item-time {
  display: block;
  margin-top: 6px;
  color: var(--front-text-muted);
  font-size: 12px;
}

/* ---- Applications ---- */
.applications-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.application-item {
  padding: 12px 16px;
  border-radius: 14px;
  border: 1px solid var(--front-border);
  background: rgba(255, 255, 255, 0.72);
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
  font-style: italic;
}

/* ---- Social ---- */
.social-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 24px;
}

.social-col {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.social-col-head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}

.social-col-head h3 {
  margin: 0;
  font-size: 15px;
  font-weight: 700;
  color: var(--front-text);
}

.social-count {
  display: inline-flex;
  padding: 1px 8px;
  border-radius: 999px;
  background: var(--front-accent-soft);
  color: var(--front-accent-strong);
  font-size: 12px;
  font-weight: 700;
}

.follow-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.follow-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 14px;
  border-radius: 14px;
  border: 1px solid var(--front-border);
  background: rgba(255, 255, 255, 0.72);
  text-decoration: none;
  color: inherit;
  transition: box-shadow 0.18s, border-color 0.18s;
}

.follow-item:hover {
  border-color: var(--front-accent);
  box-shadow: 0 4px 12px rgba(14, 165, 233, 0.08);
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
  font-size: 13px;
}

.follow-bio {
  color: var(--front-text-muted);
  font-size: 12px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* ---- Achievements ---- */
.ach-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.ach-badge {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  border-radius: 14px;
  border: 1px solid var(--front-border);
  background: rgba(255, 255, 255, 0.72);
}

.ach-badge.locked { opacity: 0.4; }

.ach-icon {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
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
  gap: 2px;
  min-width: 0;
}

.ach-info strong { color: var(--front-text); font-size: 13px; }
.ach-info span   { color: var(--front-text-muted); font-size: 12px; }
.ach-info time   { color: var(--front-text-muted); font-size: 11px; }

/* ---- Empty states ---- */
.tab-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 32px 20px;
  border-radius: 14px;
  background: #f8fafc;
  border: 1px dashed var(--front-border);
  text-align: center;
}

.tab-empty p {
  margin: 0;
  color: var(--front-text-soft);
  font-size: 13px;
}

.tab-empty-sm {
  padding: 16px 0;
  color: var(--front-text-muted);
  font-size: 13px;
  text-align: center;
}

/* ---- Load more ---- */
.load-more-row {
  display: flex;
  justify-content: center;
  padding-top: 14px;
}

/* ---- Responsive ---- */
@media (max-width: 1080px) {
  .tab-grid,
  .favorites-grid,
  .routes-grid,
  .ach-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 720px) {
  .settings-main { padding-top: 16px; }
  .profile-strip { padding: 16px 18px; }
  .social-grid { grid-template-columns: 1fr; }
  .card-head { flex-direction: column; align-items: flex-start; }
}

@media (max-width: 560px) {
  .settings-main { padding: 12px 0 22px; }
  .settings-shell { gap: 14px; }
  .profile-strip { padding: 14px 16px; }
  .profile-name { font-size: 18px; }
  .profile-bio { max-width: 200px; }
  .favorites-grid,
  .routes-grid,
  .ach-grid {
    grid-template-columns: 1fr;
  }
  .settings-tabs :deep(.el-tabs__item) {
    padding: 10px 12px;
    font-size: 13px;
  }
  .settings-tabs :deep(.el-tabs__content) {
    padding: 16px;
  }
}
</style>
