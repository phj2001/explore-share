<template>
  <div class="auth-page front-page">
    <div class="front-shell auth-shell">
      <section class="auth-intro">
        <span class="front-kicker">地点探索与分享</span>
        <h1 class="intro-title">把地点发现、体验分享和路线探索放进同一张地图里</h1>
        <p class="intro-description">
          登录后可以继续探索地点、查看推荐内容、参与分享互动，并维护自己的个人资料与头像信息。
        </p>

        <div class="intro-grid">
          <article class="intro-card">
            <span>地点发现</span>
            <strong>从地图进入内容</strong>
            <p>搜索地点、筛选分类，直接查看对应介绍与互动信息。</p>
          </article>
          <article class="intro-card">
            <span>内容分享</span>
            <strong>沉淀真实体验</strong>
            <p>把图文分享和推荐内容关联到地点，而不是停留在单纯导航。</p>
          </article>
          <article class="intro-card">
            <span>路线探索</span>
            <strong>把多个地点串联</strong>
            <p>基于地点组织路线，形成完整的探索路径与推荐体验。</p>
          </article>
        </div>
      </section>

      <section class="auth-panel front-panel">
        <!-- 登录 / 注册面板 -->
        <template v-if="!showForgot">
          <div class="panel-head">
            <div>
              <span class="front-kicker">{{ isLogin ? '账号登录' : '创建账号' }}</span>
              <h2>{{ isLogin ? '登录你的探索空间' : '注册并开始探索' }}</h2>
              <p>{{ isLogin ? '使用账号或邮箱继续进入平台。' : '创建账号后将自动登录并进入首页。' }}</p>
            </div>
            <router-link to="/" class="back-home">返回首页</router-link>
          </div>

          <el-form ref="formRef" :model="form" :rules="rules" label-position="top" class="auth-form">
            <el-form-item label="用户名" prop="username">
              <el-input
                v-model="form.username"
                :placeholder="isLogin ? '用户名 / 邮箱' : '请输入用户名'"
                autocomplete="username"
              />
            </el-form-item>

            <el-form-item label="密码" prop="password">
              <el-input
                v-model="form.password"
                type="password"
                placeholder="请输入密码"
                autocomplete="current-password"
                show-password
              />
            </el-form-item>

            <template v-if="!isLogin">
              <el-form-item label="确认密码" prop="confirmPassword">
                <el-input
                  v-model="form.confirmPassword"
                  type="password"
                  placeholder="请再次输入密码"
                  autocomplete="new-password"
                  show-password
                />
              </el-form-item>

              <el-form-item label="邮箱" prop="email">
                <el-input
                  v-model="form.email"
                  placeholder="请输入邮箱（用于找回密码）"
                  autocomplete="email"
                />
              </el-form-item>

              <el-form-item label="邮箱验证码" prop="emailCode">
                <div class="code-row">
                  <el-input
                    v-model="form.emailCode"
                    placeholder="请输入验证码"
                    class="code-input"
                  />
                  <el-button
                    :disabled="registerCountdown > 0 || !form.email || sendingRegisterCode"
                    :loading="sendingRegisterCode"
                    class="code-btn"
                    @click="handleSendRegisterCode"
                  >
                    {{ registerCountdown > 0 ? `${registerCountdown}s 后重发` : '获取验证码' }}
                  </el-button>
                </div>
              </el-form-item>
            </template>

            <div class="submit-zone">
              <el-button
                type="primary"
                :loading="isLoading"
                class="submit-button"
                @click="handleSubmit"
              >
                {{ isLogin ? '登录并进入首页' : '注册并开始探索' }}
              </el-button>

              <div class="action-links">
                <button type="button" class="mode-switch" @click="toggleMode">
                  {{ isLogin ? '没有账号？切换到注册' : '已有账号？切换到登录' }}
                </button>
                <button v-if="isLogin" type="button" class="mode-switch forgot-link" @click="showForgot = true">
                  忘记密码？
                </button>
              </div>
            </div>
          </el-form>
        </template>

        <!-- 忘记密码面板 -->
        <template v-else>
          <div class="panel-head">
            <div>
              <span class="front-kicker">找回密码</span>
              <h2>重置你的账号密码</h2>
              <p>通过绑定邮箱接收验证码，完成密码重置。</p>
            </div>
            <button type="button" class="back-home" style="background:none;border:none;cursor:pointer" @click="showForgot = false">
              返回登录
            </button>
          </div>

          <!-- Step 1：输入邮箱 -->
          <template v-if="resetStep === 1">
            <el-form ref="resetForm1Ref" :model="resetForm" :rules="resetRules1" label-position="top" class="auth-form">
              <el-form-item label="注册邮箱" prop="email">
                <el-input v-model="resetForm.email" placeholder="请输入注册时使用的邮箱" />
              </el-form-item>
              <div class="submit-zone">
                <el-button
                  type="primary"
                  :loading="sendingResetCode"
                  class="submit-button"
                  @click="handleSendResetCode"
                >
                  发送验证码
                </el-button>
              </div>
            </el-form>
          </template>

          <!-- Step 2：输入验证码 + 新密码 -->
          <template v-if="resetStep === 2">
            <el-form ref="resetForm2Ref" :model="resetForm" :rules="resetRules2" label-position="top" class="auth-form">
              <el-form-item label="邮箱验证码" prop="code">
                <div class="code-row">
                  <el-input v-model="resetForm.code" placeholder="请输入验证码" class="code-input" />
                  <el-button
                    :disabled="resetCountdown > 0 || sendingResetCode"
                    :loading="sendingResetCode"
                    class="code-btn"
                    @click="handleSendResetCode"
                  >
                    {{ resetCountdown > 0 ? `${resetCountdown}s 后重发` : '重新发送' }}
                  </el-button>
                </div>
              </el-form-item>
              <el-form-item label="新密码" prop="newPassword">
                <el-input v-model="resetForm.newPassword" type="password" placeholder="请输入新密码（6位以上）" show-password />
              </el-form-item>
              <el-form-item label="确认新密码" prop="confirmNewPassword">
                <el-input v-model="resetForm.confirmNewPassword" type="password" placeholder="请再次输入新密码" show-password />
              </el-form-item>
              <div class="submit-zone">
                <el-button type="primary" :loading="resetting" class="submit-button" @click="handleResetPassword">
                  确认重置密码
                </el-button>
              </div>
            </el-form>
          </template>

          <!-- Step 3：成功 -->
          <template v-if="resetStep === 3">
            <div class="reset-success">
              <div class="success-icon">✓</div>
              <p>密码重置成功！{{ resetRedirectCount }}s 后自动返回登录…</p>
              <el-button type="primary" class="submit-button" @click="backToLogin">立即登录</el-button>
            </div>
          </template>
        </template>
      </section>
    </div>
  </div>
</template>

<script setup>
import { computed, reactive, ref, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { checkUsername, sendRegisterCode, sendResetCode, resetPassword } from '@/api/auth.js'

const router = useRouter()
const userStore = useUserStore()

// ----- 登录/注册 -----
const formRef = ref(null)
const isLoading = ref(false)
const isLogin = ref(true)
const sendingRegisterCode = ref(false)
const registerCountdown = ref(0)
let registerTimer = null

const form = reactive({
  username: '',
  password: '',
  confirmPassword: '',
  email: '',
  emailCode: ''
})

// ----- 忘记密码 -----
const showForgot = ref(false)
const resetStep = ref(1)
const sendingResetCode = ref(false)
const resetting = ref(false)
const resetCountdown = ref(0)
const resetRedirectCount = ref(5)
let resetTimer = null
let redirectTimer = null

const resetForm = reactive({
  email: '',
  code: '',
  newPassword: '',
  confirmNewPassword: ''
})

const resetForm1Ref = ref(null)
const resetForm2Ref = ref(null)

// ----- 验证规则 -----
const normalizeUsername = (value) => value?.trim?.() || ''

const validateUsername = async (rule, value, callback) => {
  const normalized = normalizeUsername(value)
  if (!normalized) { callback(new Error('请输入用户名')); return }
  if (isLogin.value) { callback(); return }
  try {
    const exists = await checkUsername(normalized)
    if (exists) { callback(new Error('用户名已存在，请更换后重试')); return }
    callback()
  } catch {
    callback(new Error('用户名校验失败，请稍后重试'))
  }
}

const validateConfirmPassword = (rule, value, callback) => {
  if (isLogin.value) { callback(); return }
  if (!value) { callback(new Error('请再次输入密码')); return }
  if (value !== form.password) { callback(new Error('两次输入的密码不一致')); return }
  callback()
}

const rules = computed(() => {
  const base = {
    username: [{ required: true, validator: validateUsername, trigger: 'blur' }],
    password: [
      { required: true, message: '请输入密码', trigger: 'blur' },
      { min: 6, message: '密码长度不能少于 6 位', trigger: 'blur' }
    ]
  }
  if (!isLogin.value) {
    base.confirmPassword = [{ required: true, validator: validateConfirmPassword, trigger: 'blur' }]
    base.email = [
      { required: true, message: '请输入邮箱', trigger: 'blur' },
      { type: 'email', message: '邮箱格式不正确', trigger: 'blur' }
    ]
    base.emailCode = [{ required: true, message: '请输入邮箱验证码', trigger: 'blur' }]
  }
  return base
})

const resetRules1 = {
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '邮箱格式不正确', trigger: 'blur' }
  ]
}

const resetRules2 = {
  code: [{ required: true, message: '请输入验证码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码不能少于 6 位', trigger: 'blur' }
  ],
  confirmNewPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== resetForm.newPassword) { callback(new Error('两次输入的密码不一致')); return }
        callback()
      },
      trigger: 'blur'
    }
  ]
}

// ----- 方法 -----
const toggleMode = () => {
  isLogin.value = !isLogin.value
  formRef.value?.clearValidate()
  form.confirmPassword = ''
  form.email = ''
  form.emailCode = ''
}

// 发送注册验证码
const handleSendRegisterCode = async () => {
  if (!form.email) { ElMessage.warning('请先填写邮箱地址'); return }
  const emailReg = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
  if (!emailReg.test(form.email)) { ElMessage.warning('邮箱格式不正确'); return }
  sendingRegisterCode.value = true
  try {
    await sendRegisterCode(form.email)
    ElMessage.success('验证码已发送，请查收邮件')
    startRegisterCountdown()
  } catch (e) {
    ElMessage.error(e.message || '发送失败，请稍后重试')
  } finally {
    sendingRegisterCode.value = false
  }
}

const startRegisterCountdown = () => {
  registerCountdown.value = 60
  registerTimer = setInterval(() => {
    registerCountdown.value--
    if (registerCountdown.value <= 0) clearInterval(registerTimer)
  }, 1000)
}

// 登录/注册提交
const handleSubmit = async () => {
  form.username = normalizeUsername(form.username)
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  isLoading.value = true
  try {
    if (isLogin.value) {
      await userStore.login(form.username, form.password)
      ElMessage.success('登录成功')
    } else {
      const exists = await checkUsername(form.username)
      if (exists) {
        ElMessage.error('用户名已存在，请更换后重试')
        return
      }
      await userStore.register(form.username, form.password, form.email, form.emailCode)
      await userStore.login(form.username, form.password)
      ElMessage.success('注册成功')
    }
    const redirect = router.currentRoute.value.query.redirect || '/'
    router.push(redirect)
  } catch (error) {
    ElMessage.error(error.message || (isLogin.value ? '登录失败' : '注册失败'))
  } finally {
    isLoading.value = false
  }
}

// 发送重置密码验证码
const handleSendResetCode = async () => {
  if (resetStep.value === 1) {
    const valid = await resetForm1Ref.value?.validate().catch(() => false)
    if (!valid) return
  }
  sendingResetCode.value = true
  try {
    await sendResetCode(resetForm.email)
    ElMessage.success('验证码已发送，请查收邮件')
    resetStep.value = 2
    startResetCountdown()
  } catch (e) {
    ElMessage.error(e.message || '发送失败，请检查邮箱是否已注册')
  } finally {
    sendingResetCode.value = false
  }
}

const startResetCountdown = () => {
  resetCountdown.value = 60
  resetTimer = setInterval(() => {
    resetCountdown.value--
    if (resetCountdown.value <= 0) clearInterval(resetTimer)
  }, 1000)
}

// 确认重置密码
const handleResetPassword = async () => {
  const valid = await resetForm2Ref.value?.validate().catch(() => false)
  if (!valid) return
  resetting.value = true
  try {
    await resetPassword(resetForm.email, resetForm.code, resetForm.newPassword)
    ElMessage.success('密码重置成功')
    resetStep.value = 3
    startRedirectCountdown()
  } catch (e) {
    ElMessage.error(e.message || '重置失败，请检查验证码是否正确')
  } finally {
    resetting.value = false
  }
}

const startRedirectCountdown = () => {
  resetRedirectCount.value = 5
  redirectTimer = setInterval(() => {
    resetRedirectCount.value--
    if (resetRedirectCount.value <= 0) {
      clearInterval(redirectTimer)
      backToLogin()
    }
  }, 1000)
}

const backToLogin = () => {
  clearInterval(redirectTimer)
  showForgot.value = false
  resetStep.value = 1
  resetForm.email = ''
  resetForm.code = ''
  resetForm.newPassword = ''
  resetForm.confirmNewPassword = ''
}

onUnmounted(() => {
  clearInterval(registerTimer)
  clearInterval(resetTimer)
  clearInterval(redirectTimer)
})
</script>

<style scoped>
.auth-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  padding: 28px 0;
  background:
    radial-gradient(circle at top left, rgba(23, 135, 166, 0.14), transparent 24%),
    radial-gradient(circle at bottom right, rgba(13, 107, 133, 0.1), transparent 18%),
    linear-gradient(135deg, #edf5f7 0%, #f7fbfc 50%, #eef5f6 100%);
}

.auth-shell {
  display: grid;
  grid-template-columns: minmax(0, 1.1fr) minmax(360px, 0.82fr);
  gap: 22px;
  align-items: stretch;
}

.auth-intro {
  padding: 34px 12px 34px 6px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 18px;
}

.intro-title {
  margin: 0;
  color: var(--front-text);
  font-size: clamp(32px, 4vw, 52px);
  line-height: 1.05;
  letter-spacing: -0.03em;
}

.intro-description {
  margin: 0;
  max-width: 640px;
  color: var(--front-text-soft);
  font-size: 15px;
  line-height: 1.9;
}

.intro-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}

.intro-card {
  min-height: 180px;
  padding: 18px;
  border-radius: 22px;
  border: 1px solid var(--front-border);
  background: rgba(255, 255, 255, 0.62);
  box-shadow: var(--front-shadow-soft);
}

.intro-card span {
  display: block;
  color: var(--front-text-muted);
  font-size: 12px;
}

.intro-card strong {
  display: block;
  margin: 12px 0 10px;
  color: var(--front-text);
  font-size: 18px;
  line-height: 1.3;
}

.intro-card p {
  margin: 0;
  color: var(--front-text-soft);
  font-size: 13px;
  line-height: 1.8;
}

.auth-panel {
  padding: 28px;
  border-radius: 30px;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.panel-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 24px;
}

.panel-head h2 {
  margin: 14px 0 8px;
  color: var(--front-text);
  font-size: 26px;
}

.panel-head p {
  margin: 0;
  color: var(--front-text-soft);
  font-size: 13px;
  line-height: 1.75;
}

.back-home {
  color: var(--front-accent-strong);
  text-decoration: none;
  font-size: 13px;
  font-weight: 700;
}

.auth-form :deep(.el-form-item__label) {
  color: var(--front-text);
  font-size: 13px;
  font-weight: 700;
}

.submit-zone {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding-top: 6px;
}

.submit-button {
  width: 100%;
  height: 44px;
}

.action-links {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.mode-switch {
  border: none;
  background: transparent;
  color: var(--front-accent-strong);
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
  padding: 0;
}

.forgot-link {
  color: #888;
  font-weight: 400;
}

.code-row {
  display: flex;
  gap: 10px;
  width: 100%;
}

.code-input {
  flex: 1;
}

.code-btn {
  flex-shrink: 0;
  width: 110px;
}

.reset-success {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  padding: 32px 0;
}

.success-icon {
  width: 60px;
  height: 60px;
  border-radius: 50%;
  background: #52c41a;
  color: #fff;
  font-size: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.reset-success p {
  color: var(--front-text-soft);
  font-size: 14px;
  text-align: center;
}

@media (max-width: 1120px) {
  .auth-shell {
    grid-template-columns: 1fr;
  }

  .auth-intro {
    padding: 10px 0 0;
  }
}

@media (max-width: 720px) {
  .auth-page {
    padding: 16px 0;
    align-items: stretch;
  }

  .auth-intro {
    order: 2;
    padding: 0;
    gap: 10px;
  }

  .auth-panel {
    order: 1;
  }

  .intro-grid {
    grid-template-columns: 1fr;
  }

  .auth-panel {
    padding: 20px;
    border-radius: 24px;
  }

  .panel-head {
    flex-direction: column;
    align-items: flex-start;
    margin-bottom: 20px;
  }

  .submit-button {
    height: 42px;
  }
}

@media (max-width: 560px) {
  .auth-page {
    padding: 8px 0 16px;
  }

  .auth-shell {
    gap: 10px;
  }

  .auth-intro {
    gap: 8px;
  }

  .intro-title {
    font-size: clamp(20px, 7vw, 26px);
    line-height: 1.18;
  }

  .intro-description {
    max-width: none;
    font-size: 12px;
    line-height: 1.55;
  }

  .intro-grid {
    display: none;
  }

  .auth-panel {
    padding: 16px 14px;
    border-radius: 18px;
  }

  .panel-head h2 {
    margin-top: 8px;
    font-size: 20px;
  }

  .panel-head p {
    font-size: 12px;
    line-height: 1.55;
  }

  .back-home {
    font-size: 12px;
  }

  .auth-form :deep(.el-input__wrapper) {
    min-height: 42px;
  }

  .submit-zone {
    gap: 10px;
  }

  .submit-button {
    height: 44px;
  }

  .mode-switch {
    font-size: 12px;
    padding: 6px 0;
  }
}

@media (max-width: 420px) {
  .auth-page {
    padding-top: 4px;
  }

  .auth-shell {
    gap: 8px;
  }

  .front-kicker {
    font-size: 11px;
  }

  .auth-intro {
    gap: 6px;
  }

  .intro-title {
    font-size: 18px;
  }

  .intro-description {
    display: none;
  }

  .panel-head {
    margin-bottom: 16px;
  }

  .panel-head h2 {
    font-size: 18px;
  }

  .panel-head p {
    display: none;
  }

  .back-home {
    display: none;
  }
}
</style>
