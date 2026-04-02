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
import { ElMessage } from 'element-plus'
import Header from '@/components/common/Header.vue'
import Footer from '@/components/common/Footer.vue'
import AvatarCropperDialog from '@/components/user/AvatarCropperDialog.vue'
import { changeMyPassword, updateMyProfile, uploadMyAvatar } from '@/api/user.js'
import { useUserStore } from '@/stores/user'

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

const revokeCropperUrl = () => {
  if (cropperImageUrl.value) {
    URL.revokeObjectURL(cropperImageUrl.value)
    cropperImageUrl.value = ''
  }
}

onMounted(async () => {
  await loadProfile()
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
}
</style>
