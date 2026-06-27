<template>
  <div class="auth-page">

    <!-- 左侧：深翠绿编辑面板 -->
    <section class="auth-intro">
      <div class="intro-coord-strip">
        <span class="intro-coord-dot" />
        <span>FIELD · 地点探索</span>
        <span style="opacity:0.5">·</span>
        <span>34.3416°N · 108.9398°E</span>
      </div>

      <h1 class="intro-title">
        把<em>地点</em>、<br/>
        体验与<em>路线</em>，<br/>
        放进同一张地图。
      </h1>

      <p class="intro-description">
        登录后可以探索地点、查看推荐内容、参与分享互动，并维护自己的路线与足迹记录。
      </p>

      <div class="intro-grid">
        <article class="intro-card">
          <span>DISCOVERY</span>
          <strong>从地图进入内容</strong>
          <p>搜索地点、筛选分类，直接查看对应介绍与互动信息。</p>
        </article>
        <article class="intro-card">
          <span>SHARING</span>
          <strong>沉淀真实体验</strong>
          <p>把图文分享和推荐内容关联到地点，而不是停留在单纯导航。</p>
        </article>
        <article class="intro-card">
          <span>ROUTE</span>
          <strong>把多个地点串联</strong>
          <p>基于地点组织路线，形成完整的探索路径与推荐体验。</p>
        </article>
      </div>

      <div class="intro-bottom">
        <div class="intro-stat"><strong>4,776</strong><span>收录地点</span></div>
        <div class="intro-divider" />
        <div class="intro-stat"><strong>2,184</strong><span>社区分享</span></div>
        <div class="intro-divider" />
        <div class="intro-stat"><strong>386</strong><span>原创路线</span></div>
      </div>
    </section>

    <!-- 右侧：极简表单 -->
    <section class="auth-panel">
      <div class="auth-panel-inner">

        <div class="panel-top">
          <router-link to="/" class="panel-logo">
            <span class="panel-logo-mark">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M12 22s-8-7.5-8-13a8 8 0 1 1 16 0c0 5.5-8 13-8 13Z"/>
                <circle cx="12" cy="9" r="2.5"/>
              </svg>
            </span>
            <span class="panel-logo-text">地点探索</span>
          </router-link>
          <router-link to="/" class="back-home">← 返回首页</router-link>
        </div>

        <!-- 登录 / 注册 -->
        <template v-if="!showForgot">
          <div class="panel-head">
            <span class="front-kicker">{{ isLogin ? '账号登录' : '创建账号' }}</span>
            <h2>{{ isLogin ? '登录你的探索空间' : '注册并开始探索' }}</h2>
            <p>{{ isLogin ? '使用账号或邮箱继续进入平台。' : '创建账号后将自动登录并进入首页。' }}</p>
          </div>

          <el-form ref="formRef" :model="form" :rules="rules" label-position="top" class="auth-form">
            <el-form-item label="用户名" prop="username">
              <el-input v-model="form.username" :placeholder="isLogin ? '用户名 / 邮箱' : '请输入用户名'" autocomplete="username" />
            </el-form-item>
            <el-form-item label="密码" prop="password">
              <el-input v-model="form.password" type="password" placeholder="请输入密码" autocomplete="current-password" show-password />
            </el-form-item>
            <template v-if="!isLogin">
              <el-form-item label="确认密码" prop="confirmPassword">
                <el-input v-model="form.confirmPassword" type="password" placeholder="请再次输入密码" autocomplete="new-password" show-password />
              </el-form-item>
              <el-form-item label="邮箱" prop="email">
                <el-input v-model="form.email" placeholder="请输入邮箱（用于找回密码）" autocomplete="email" />
              </el-form-item>
              <el-form-item label="邮箱验证码" prop="emailCode">
                <div class="code-row">
                  <el-input v-model="form.emailCode" placeholder="请输入验证码" class="code-input" />
                  <el-button :disabled="registerCountdown > 0 || !form.email || sendingRegisterCode" :loading="sendingRegisterCode" class="code-btn" @click="handleSendRegisterCode">
                    {{ registerCountdown > 0 ? `${registerCountdown}s 后重发` : '获取验证码' }}
                  </el-button>
                </div>
              </el-form-item>
            </template>
            <div class="submit-zone">
              <el-button type="primary" :loading="isLoading" class="submit-button" @click="handleSubmit">
                {{ isLogin ? '登录并进入首页' : '注册并开始探索' }}
              </el-button>
              <div class="action-links">
                <button type="button" class="mode-switch" @click="toggleMode">
                  {{ isLogin ? '没有账号？切换到注册' : '已有账号？切换到登录' }}
                </button>
                <button v-if="isLogin" type="button" class="mode-switch forgot-link" @click="showForgot = true">忘记密码？</button>
              </div>
            </div>
          </el-form>
        </template>

        <!-- 忘记密码 -->
        <template v-else>
          <div class="panel-head">
            <span class="front-kicker">找回密码</span>
            <h2>重置你的账号密码</h2>
            <p>通过绑定邮箱接收验证码，完成密码重置。</p>
          </div>

          <template v-if="resetStep === 1">
            <el-form ref="resetForm1Ref" :model="resetForm" :rules="resetRules1" label-position="top" class="auth-form">
              <el-form-item label="注册邮箱" prop="email">
                <el-input v-model="resetForm.email" placeholder="请输入注册时使用的邮箱" />
              </el-form-item>
              <div class="submit-zone">
                <el-button type="primary" :loading="sendingResetCode" class="submit-button" @click="handleSendResetCode">发送验证码</el-button>
                <button type="button" class="mode-switch" @click="showForgot = false">← 返回登录</button>
              </div>
            </el-form>
          </template>

          <template v-if="resetStep === 2">
            <el-form ref="resetForm2Ref" :model="resetForm" :rules="resetRules2" label-position="top" class="auth-form">
              <el-form-item label="邮箱验证码" prop="code">
                <div class="code-row">
                  <el-input v-model="resetForm.code" placeholder="请输入验证码" class="code-input" />
                  <el-button :disabled="resetCountdown > 0 || sendingResetCode" :loading="sendingResetCode" class="code-btn" @click="handleSendResetCode">
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
                <el-button type="primary" :loading="resetting" class="submit-button" @click="handleResetPassword">确认重置密码</el-button>
              </div>
            </el-form>
          </template>

          <template v-if="resetStep === 3">
            <div class="reset-success">
              <div class="success-icon">✓</div>
              <p>密码重置成功！{{ resetRedirectCount }}s 后自动返回登录…</p>
              <el-button type="primary" class="submit-button" @click="backToLogin">立即登录</el-button>
            </div>
          </template>
        </template>

      </div>
    </section>
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
/* =========================================================
   登录页 — 左深色面板 · 右极简表单
   ========================================================= */
.auth-page {
  min-height: 100vh;
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(360px, 0.72fr);
}

/* 左侧：深翠绿编辑面板 */
.auth-intro {
  position: relative;
  overflow: hidden;
  background: var(--forest-800);
  padding: 56px 52px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 36px;
  color: var(--forest-100);
}

.auth-intro::before {
  content: '';
  position: absolute;
  inset: 0;
  background-image:
    repeating-radial-gradient(circle at 20% 35%,
      transparent 0, transparent 40px,
      rgba(74,196,154,0.06) 40px, rgba(74,196,154,0.06) 41px),
    repeating-radial-gradient(circle at 80% 75%,
      transparent 0, transparent 32px,
      rgba(58,155,210,0.05) 32px, rgba(58,155,210,0.05) 33px);
  pointer-events: none;
}

.intro-coord-strip {
  display: flex;
  align-items: center;
  gap: 10px;
  font-family: var(--font-mono);
  font-size: 10px;
  letter-spacing: 0.16em;
  color: var(--forest-400);
  text-transform: uppercase;
}

.intro-coord-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--clay-500);
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.4; }
}

.intro-title {
  margin: 0;
  font-family: var(--font-serif);
  font-size: clamp(28px, 3.2vw, 48px);
  line-height: 1.1;
  letter-spacing: -0.025em;
  color: var(--forest-50);
  font-weight: 500;
}

.intro-title em {
  font-style: italic;
  color: var(--clay-400);
}

.intro-description {
  margin: 0;
  font-size: 14.5px;
  line-height: 1.85;
  color: var(--forest-300);
  max-width: 460px;
}

.intro-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
}

.intro-card {
  padding: 16px;
  border-radius: 10px;
  border: 1px solid rgba(74,196,154,0.15);
  background: rgba(255,255,255,0.04);
}

.intro-card span {
  display: block;
  font-family: var(--font-mono);
  font-size: 10px;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: var(--clay-400);
}

.intro-card strong {
  display: block;
  margin: 10px 0 8px;
  font-family: var(--font-serif);
  font-size: 15px;
  color: var(--forest-50);
  font-weight: 500;
}

.intro-card p {
  margin: 0;
  font-size: 12.5px;
  line-height: 1.7;
  color: var(--forest-300);
}

.intro-bottom {
  display: flex;
  align-items: center;
  gap: 16px;
  padding-top: 8px;
  border-top: 1px solid rgba(74,196,154,0.15);
}

.intro-stat {
  text-align: left;
}

.intro-stat strong {
  display: block;
  font-family: var(--font-serif);
  font-size: 22px;
  color: var(--forest-100);
  letter-spacing: -0.02em;
}

.intro-stat span {
  display: block;
  font-family: var(--font-mono);
  font-size: 10px;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: var(--forest-400);
  margin-top: 3px;
}

.intro-divider {
  width: 1px;
  height: 36px;
  background: rgba(74,196,154,0.2);
}

/* 右侧：极简表单 */
.auth-panel {
  background: var(--front-bg);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 48px 44px;
}

.auth-panel-inner {
  width: 100%;
  max-width: 380px;
  display: flex;
  flex-direction: column;
}

.panel-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 32px;
}

.panel-logo {
  display: inline-flex;
  align-items: center;
  gap: 9px;
  text-decoration: none;
}

.panel-logo-mark {
  width: 34px;
  height: 34px;
  border-radius: 9px;
  background: var(--forest-700);
  display: grid;
  place-items: center;
  color: #fff;
}

.panel-logo-text {
  font-family: var(--font-serif);
  font-size: 15px;
  font-weight: 500;
  color: var(--ink-900);
}

.back-home {
  font-family: var(--font-mono);
  font-size: 11px;
  letter-spacing: 0.08em;
  color: var(--ink-500);
  text-decoration: none;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  transition: color 0.15s;
}
.back-home:hover { color: var(--forest-700); }

.panel-head {
  margin-bottom: 24px;
}

.panel-head h2 {
  margin: 8px 0 6px;
  font-family: var(--font-serif);
  font-size: 24px;
  font-weight: 500;
  color: var(--ink-900);
  letter-spacing: -0.02em;
}

.panel-head p {
  margin: 0;
  font-size: 13px;
  color: var(--ink-500);
  line-height: 1.65;
}

/* 表单：底边线风格 */
.auth-form :deep(.el-form-item) {
  margin-bottom: 18px;
}

.auth-form :deep(.el-form-item__label) {
  font-family: var(--font-mono);
  font-size: 10.5px;
  font-weight: 500;
  letter-spacing: 0.1em;
  text-transform: uppercase;
  color: var(--ink-600);
  padding-bottom: 6px;
}

.auth-form :deep(.el-input__wrapper) {
  background: transparent;
  border: none;
  border-bottom: 1.5px solid var(--front-border);
  border-radius: 0;
  box-shadow: none;
  padding: 6px 0;
  transition: border-color 0.15s;
}

.auth-form :deep(.el-input__wrapper):hover,
.auth-form :deep(.el-input__wrapper.is-focus) {
  border-bottom-color: var(--forest-600);
  box-shadow: 0 1.5px 0 0 var(--forest-600);
}

.auth-form :deep(.el-input__inner) {
  font-family: var(--font-sans);
  font-size: 14px;
  color: var(--ink-900);
  background: transparent;
}

.submit-zone {
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding-top: 8px;
}

.submit-button {
  width: 100%;
  height: 44px;
  border-radius: 10px;
  font-family: var(--font-sans);
  font-size: 14px;
  font-weight: 500;
}

.action-links {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.mode-switch {
  border: none;
  background: transparent;
  color: var(--forest-700);
  font-family: var(--font-sans);
  font-size: 12.5px;
  font-weight: 500;
  cursor: pointer;
  padding: 0;
  transition: color 0.15s;
}
.mode-switch:hover { color: var(--forest-800); }

.forgot-link {
  color: var(--ink-400);
  font-weight: 400;
}
.forgot-link:hover { color: var(--ink-600); }

.code-row {
  display: flex;
  gap: 10px;
  width: 100%;
}

.code-input { flex: 1; }

.code-btn {
  flex-shrink: 0;
  width: 112px;
  border-radius: 8px;
}

.reset-success {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  padding: 32px 0;
}

.success-icon {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  background: var(--forest-600);
  color: #fff;
  font-size: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.reset-success p {
  color: var(--ink-500);
  font-size: 13.5px;
  text-align: center;
  line-height: 1.65;
}

/* 响应式 */
@media (max-width: 960px) {
  .auth-page {
    grid-template-columns: 1fr;
    min-height: auto;
  }
  .auth-intro {
    padding: 40px 32px;
    min-height: 280px;
  }
  .intro-grid { display: none; }
  .intro-bottom { display: none; }
  .auth-panel { padding: 36px 24px; }
}

@media (max-width: 520px) {
  .auth-intro { padding: 28px 20px; }
  .auth-panel { padding: 24px 16px; }
  .panel-head h2 { font-size: 20px; }
  .intro-title { font-size: 26px; }
}
</style>
