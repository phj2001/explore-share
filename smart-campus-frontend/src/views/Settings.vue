<template>
  <div class="settings-page">
    <Header />

    <main class="settings-main">
      <section class="settings-shell">
        <div class="hero-card">
          <div class="hero-info">
            <el-avatar :size="88" :src="userStore.avatarUrl || undefined" class="hero-avatar">
              {{ displayName.slice(0, 1).toUpperCase() }}
            </el-avatar>
            <div>
              <h1>用户设置</h1>
              <p>管理你的头像、展示名、个性签名和账号安全信息。</p>
            </div>
          </div>
          <div class="hero-actions">
            <el-upload
              class="hidden-uploader"
              :show-file-list="false"
              :auto-upload="false"
              :on-change="handleAvatarSelect"
              accept=".png,.jpg,.jpeg,.webp,image/png,image/jpeg,image/webp"
            >
              <template #trigger>
                <el-button type="primary" :loading="avatarUploading">更换头像</el-button>
              </template>
            </el-upload>
          </div>
        </div>

        <div class="settings-grid">
          <el-card shadow="hover" class="settings-card">
            <template #header>
              <div class="card-header">
                <span>基本资料</span>
                <el-button type="primary" :loading="profileSaving" @click="submitProfile">
                  保存资料
                </el-button>
              </div>
            </template>

            <el-form
              ref="profileFormRef"
              :model="profileForm"
              :rules="profileRules"
              label-position="top"
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
                  :rows="4"
                  maxlength="150"
                  show-word-limit
                  placeholder="写一句介绍自己的话"
                />
              </el-form-item>
            </el-form>
          </el-card>

          <el-card shadow="hover" class="settings-card">
            <template #header>
              <div class="card-header">
                <span>账号安全</span>
                <el-button type="danger" :loading="passwordSaving" @click="submitPassword">
                  修改密码
                </el-button>
              </div>
            </template>

            <el-alert
              title="修改密码成功后会自动退出登录，请使用新密码重新登录。"
              type="warning"
              :closable="false"
              show-icon
              class="password-tip"
            />

            <el-form
              ref="passwordFormRef"
              :model="passwordForm"
              :rules="passwordRules"
              label-position="top"
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
          </el-card>
        </div>
      </section>
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
  displayName: [{ max: 100, message: '展示名不能超过100个字符', trigger: 'blur' }],
  bio: [{ max: 150, message: '个性签名不能超过150个字符', trigger: 'blur' }]
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
    { min: 6, max: 100, message: '新密码长度必须在6到100个字符之间', trigger: 'blur' }
  ],
  confirmPassword: [{ validator: validateConfirmPassword, trigger: 'blur' }]
}

onMounted(async () => {
  await loadProfile()
})

onBeforeUnmount(() => {
  revokeCropperUrl()
})

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
    ElMessage.error('头像大小不能超过2MB')
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
</script>

<style scoped>
.settings-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background:
    radial-gradient(circle at top left, rgba(125, 211, 252, 0.18), transparent 28%),
    linear-gradient(180deg, #f8fbff 0%, #eef4ff 100%);
}

.settings-main {
  flex: 1;
  padding: 40px 20px 56px;
}

.settings-shell {
  max-width: 1180px;
  margin: 0 auto;
}

.hero-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  padding: 28px 32px;
  border-radius: 28px;
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(14px);
  box-shadow: 0 24px 60px rgba(37, 99, 235, 0.12);
}

.hero-info {
  display: flex;
  align-items: center;
  gap: 20px;
}

.hero-avatar {
  background: linear-gradient(135deg, #60a5fa, #2563eb);
  color: #fff;
  font-size: 28px;
  font-weight: 700;
  border: 4px solid rgba(255, 255, 255, 0.95);
}

.hero-info h1 {
  margin: 0 0 6px;
  font-size: 32px;
  color: #0f172a;
}

.hero-info p {
  margin: 0;
  color: #64748b;
}

.hidden-uploader {
  display: inline-flex;
}

.settings-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 24px;
  margin-top: 28px;
}

.settings-card {
  border-radius: 24px;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.password-tip {
  margin-bottom: 20px;
}

@media (max-width: 960px) {
  .hero-card {
    flex-direction: column;
    align-items: flex-start;
  }

  .settings-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .settings-main {
    padding: 24px 12px 40px;
  }

  .hero-card {
    padding: 24px 20px;
  }

  .hero-info {
    flex-direction: column;
    align-items: flex-start;
  }

  .hero-info h1 {
    font-size: 28px;
  }

  .card-header {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
