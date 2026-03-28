<template>
  <section class="share-panel">
    <div class="share-header">
      <div>
        <h3>打卡分享</h3>
        <p>{{ totalText }}</p>
      </div>
      <el-tag type="info" effect="plain">最新优先</el-tag>
    </div>

    <div class="composer-card">
      <template v-if="userStore.isLoggedIn">
        <div class="composer-user">
          <el-avatar :size="40" :src="userStore.avatarUrl || undefined" class="composer-avatar">
            {{ currentDisplayName.slice(0, 1).toUpperCase() }}
          </el-avatar>
          <div>
            <strong>{{ currentDisplayName }}</strong>
            <p>在这里写下你的打卡体验，最多 300 字</p>
          </div>
        </div>

        <el-input
          v-model="shareContent"
          type="textarea"
          :rows="4"
          maxlength="300"
          show-word-limit
          resize="none"
          placeholder="可以分享地点感受、风景体验或推荐内容"
        />

        <el-upload
          v-model:file-list="uploadFileList"
          class="share-uploader"
          list-type="picture-card"
          :auto-upload="false"
          :limit="3"
          multiple
          accept=".png,.jpg,.jpeg,.webp,image/png,image/jpeg,image/webp"
          :on-change="handleUploadChange"
          :on-preview="handlePreview"
          :on-remove="handleRemove"
          :on-exceed="handleExceed"
        >
          <div class="upload-trigger">
            <el-icon><Plus /></el-icon>
            <span>上传图片</span>
          </div>
        </el-upload>

        <div class="composer-actions">
          <span class="upload-tip">支持 JPG / PNG / WEBP，单张不超过 5MB，最多 3 张</span>
          <el-button type="primary" :loading="submitting" @click="submitShare">发布分享</el-button>
        </div>
      </template>

      <template v-else>
        <div class="login-tip">
          <div>
            <strong>登录后可参与打卡分享</strong>
            <p>你可以查看其他用户的内容，登录后即可发布、点赞和回复。</p>
          </div>
          <el-button type="primary" @click="goLogin">前往登录</el-button>
        </div>
      </template>
    </div>

    <el-skeleton v-if="loading && !shares.length" :rows="4" animated />

    <div v-else-if="shares.length" class="share-list">
      <article v-for="share in shares" :key="share.id" class="share-card">
        <div class="share-card-head">
          <div class="author-block">
            <el-avatar :size="42" :src="resolveMediaUrl(share.authorAvatarUrl) || undefined" class="author-avatar">
              {{ (share.authorDisplayName || share.authorUsername || 'U').slice(0, 1).toUpperCase() }}
            </el-avatar>
            <div class="author-meta">
              <div class="author-line">
                <strong>{{ share.authorDisplayName }}</strong>
                <span class="author-username">@{{ share.authorUsername }}</span>
              </div>
              <time>{{ formatTime(share.createdAt) }}</time>
            </div>
          </div>

          <div class="card-actions">
            <el-button
              v-if="canReportShare(share)"
              text
              type="warning"
              @click="openShareReportDialog(share)"
            >
              举报
            </el-button>
            <el-button
              v-if="share.canDelete"
              type="danger"
              text
              :loading="deletingId === share.id"
              @click="removeShare(share)"
            >
              删除
            </el-button>
          </div>
        </div>

        <p v-if="share.content" class="share-content">{{ share.content }}</p>

        <div v-if="share.imageUrls?.length" class="share-images">
          <el-image
            v-for="imageUrl in share.imageUrls"
            :key="imageUrl"
            :src="resolveMediaUrl(imageUrl)"
            :preview-src-list="share.imageUrls.map(resolveMediaUrl)"
            fit="cover"
            preview-teleported
            class="share-image"
          />
        </div>

        <div class="share-actions">
          <el-button
            text
            :type="share.likedByCurrentUser ? 'primary' : 'default'"
            :loading="share.likeLoading"
            @click="toggleLike(share)"
          >
            {{ share.likedByCurrentUser ? '已点赞' : '点赞' }} {{ share.likeCount || 0 }}
          </el-button>
          <el-button text @click="toggleReplyComposer(share)">
            {{ share.replyComposerVisible ? '收起回复' : '回复' }} {{ share.replyCount || 0 }}
          </el-button>
        </div>

        <div v-if="share.replyCount || share.replyComposerVisible" class="reply-section">
          <div v-if="share.replies.length" class="reply-list">
            <div v-for="reply in share.replies" :key="reply.id" class="reply-item">
              <div class="reply-main">
                <el-avatar :size="32" :src="resolveMediaUrl(reply.authorAvatarUrl) || undefined" class="reply-avatar">
                  {{ (reply.authorDisplayName || reply.authorUsername || 'U').slice(0, 1).toUpperCase() }}
                </el-avatar>
                <div class="reply-body">
                  <div class="reply-meta">
                    <strong>{{ reply.authorDisplayName }}</strong>
                    <span class="author-username">@{{ reply.authorUsername }}</span>
                    <time>{{ formatTime(reply.createdAt) }}</time>
                  </div>
                  <p>{{ reply.content }}</p>
                </div>
              </div>

              <el-button
                v-if="reply.canDelete"
                text
                type="danger"
                class="reply-delete"
                :loading="share.replyDeletingId === reply.id"
                @click="removeReply(share, reply)"
              >
                删除
              </el-button>
              <el-button
                v-else-if="canReportReply(reply)"
                text
                type="warning"
                class="reply-delete"
                @click="openReplyReportDialog(reply)"
              >
                举报
              </el-button>
            </div>
          </div>

          <div v-if="share.replyHasMore" class="reply-more">
            <el-button text :loading="share.replyLoading" @click="loadMoreReplies(share)">加载更多回复</el-button>
          </div>

          <div v-if="share.replyComposerVisible" class="reply-composer">
            <template v-if="userStore.isLoggedIn">
              <el-input
                v-model="share.replyDraft"
                type="textarea"
                :rows="2"
                maxlength="200"
                show-word-limit
                resize="none"
                placeholder="写下你的回复"
              />
              <div class="reply-composer-actions">
                <span>回复按时间顺序展示</span>
                <el-button type="primary" size="small" :loading="share.replySubmitting" @click="submitReply(share)">
                  发送回复
                </el-button>
              </div>
            </template>

            <div v-else class="reply-login-tip">
              <span>登录后可回复这条分享</span>
              <el-button type="primary" link @click="goLogin">前往登录</el-button>
            </div>
          </div>
        </div>
      </article>

      <div class="share-footer">
        <el-button v-if="hasMore" :loading="loadingMore" @click="loadMore">加载更多</el-button>
        <span v-else>已经到底了</span>
      </div>
    </div>

    <el-empty v-else description="这个 POI 还没有分享，来发布第一条吧" />

    <el-dialog v-model="previewVisible" title="图片预览" width="640px">
      <img :src="previewImageUrl" alt="预览图片" class="preview-image" />
    </el-dialog>

    <el-dialog
      v-model="reportDialogVisible"
      title="举报内容"
      width="520px"
      destroy-on-close
      @closed="resetReportDialog"
    >
      <div class="report-dialog">
        <div class="report-target">
          <span class="report-label">举报对象</span>
          <strong>{{ reportTargetLabel }}</strong>
          <p>{{ reportTargetPreview }}</p>
        </div>

        <el-form label-position="top">
          <el-form-item label="举报理由" required>
            <el-radio-group v-model="reportReasonCode">
              <el-radio
                v-for="option in REPORT_REASON_OPTIONS"
                :key="option.value"
                :value="option.value"
              >
                {{ option.label }}
              </el-radio>
            </el-radio-group>
          </el-form-item>

          <el-form-item label="补充说明">
            <el-input
              v-model="reportReasonDetail"
              type="textarea"
              :rows="4"
              maxlength="200"
              show-word-limit
              resize="none"
              placeholder="可补充违规细节；若选择“其他”则必须填写"
            />
          </el-form-item>
        </el-form>
      </div>

      <template #footer>
        <div class="dialog-actions">
          <el-button @click="reportDialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="reportSubmitting" @click="submitReport">提交举报</el-button>
        </div>
      </template>
    </el-dialog>
  </section>
</template>

<script setup>
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import {
  createPoiShare,
  createPoiShareReply,
  deletePoiShare,
  deletePoiShareReply,
  getPoiSharePage,
  getPoiShareReplies,
  likePoiShare,
  unlikePoiShare
} from '@/api/poiShare'
import { createReplyReport, createShareReport } from '@/api/contentReport'
import { REPORT_REASON_OPTIONS, REPORT_REASON_OTHER } from '@/constants/contentReport'
import { useUserStore } from '@/stores/user'
import { API_ORIGIN } from '@/utils/request'

const props = defineProps({
  poi: {
    type: Object,
    default: null
  }
})

const router = useRouter()
const userStore = useUserStore()

const shares = ref([])
const uploadFileList = ref([])
const shareContent = ref('')
const loading = ref(false)
const loadingMore = ref(false)
const submitting = ref(false)
const deletingId = ref(null)
const total = ref(0)
const page = ref(0)
const hasMore = ref(false)
const previewVisible = ref(false)
const previewImageUrl = ref('')
const reportDialogVisible = ref(false)
const reportSubmitting = ref(false)
const reportTargetType = ref('')
const reportTargetId = ref(null)
const reportTargetLabel = ref('')
const reportTargetPreview = ref('')
const reportReasonCode = ref(REPORT_REASON_OPTIONS[0].value)
const reportReasonDetail = ref('')

const currentDisplayName = computed(() => userStore.displayName || userStore.username || '当前用户')
const currentUserId = computed(() => userStore.userInfo?.id ?? null)
const totalText = computed(() => `共 ${total.value} 条打卡分享`)

const resolveMediaUrl = (value) => {
  if (!value) {
    return ''
  }

  if (/^https?:\/\//i.test(value)) {
    return value
  }

  return `${API_ORIGIN}${value.startsWith('/') ? value : `/${value}`}`
}

const formatTime = (value) => {
  if (!value) {
    return ''
  }

  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  }).format(new Date(value))
}

const createShareState = (share) => ({
  ...share,
  likeCount: share.likeCount || 0,
  likedByCurrentUser: Boolean(share.likedByCurrentUser),
  replyCount: share.replyCount || 0,
  replies: [...(share.previewReplies || [])],
  replyHasMore: (share.replyCount || 0) > (share.previewReplies?.length || 0),
  replyComposerVisible: false,
  replyDraft: '',
  likeLoading: false,
  replyLoading: false,
  replySubmitting: false,
  replyDeletingId: null
})

const clearUploadFiles = () => {
  uploadFileList.value.forEach((file) => {
    if (file.url?.startsWith('blob:')) {
      URL.revokeObjectURL(file.url)
    }
  })
  uploadFileList.value = []
}

const resetPanel = () => {
  shares.value = []
  total.value = 0
  page.value = 0
  hasMore.value = false
  shareContent.value = ''
  previewVisible.value = false
  previewImageUrl.value = ''
  clearUploadFiles()
  resetReportDialog()
}

const sanitizeUploadList = (fileList) => {
  const sanitized = []

  for (const file of fileList) {
    const rawFile = file.raw
    if (!rawFile) {
      continue
    }

    const isValidType = ['image/jpeg', 'image/png', 'image/webp'].includes(rawFile.type)
    if (!isValidType) {
      ElMessage.error('仅支持 JPG、PNG、WEBP 图片')
      if (file.url?.startsWith('blob:')) {
        URL.revokeObjectURL(file.url)
      }
      continue
    }

    if (rawFile.size > 5 * 1024 * 1024) {
      ElMessage.error('单张图片大小不能超过 5MB')
      if (file.url?.startsWith('blob:')) {
        URL.revokeObjectURL(file.url)
      }
      continue
    }

    if (!file.url) {
      file.url = URL.createObjectURL(rawFile)
    }
    sanitized.push(file)
  }

  uploadFileList.value = sanitized.slice(0, 3)
}

const handleUploadChange = (file, fileList) => {
  sanitizeUploadList(fileList)
}

const handleRemove = () => {
  sanitizeUploadList(uploadFileList.value)
}

const handleExceed = () => {
  ElMessage.warning('每次最多上传 3 张图片')
}

const handlePreview = (file) => {
  previewImageUrl.value = file.url || ''
  previewVisible.value = true
}

const applySharePage = (data, reset = false) => {
  const nextRecords = (data.records || []).map(createShareState)
  shares.value = reset ? nextRecords : [...shares.value, ...nextRecords]
  total.value = data.total || 0
  page.value = data.page || 0
  hasMore.value = Boolean(data.hasMore)
}

const loadShares = async (reset = false) => {
  if (!props.poi?.id) {
    return
  }

  const nextPage = reset ? 0 : page.value + 1
  const loadingRef = reset ? loading : loadingMore
  loadingRef.value = true

  try {
    const data = await getPoiSharePage(props.poi.id, { page: nextPage, size: 10 })
    applySharePage(data, reset)
  } catch (error) {
    ElMessage.error(error.message || '加载分享失败')
  } finally {
    loadingRef.value = false
  }
}

const loadMore = async () => {
  await loadShares(false)
}

const refreshReplies = async (share, size) => {
  share.replyLoading = true
  try {
    const data = await getPoiShareReplies(share.id, { page: 0, size })
    share.replies = data.records || []
    share.replyCount = data.total || 0
    share.replyHasMore = Boolean(data.hasMore)
  } finally {
    share.replyLoading = false
  }
}

const loadMoreReplies = async (share) => {
  const nextSize = Math.min((share.replies.length || 0) + 10, share.replyCount || (share.replies.length || 0) + 10)
  try {
    await refreshReplies(share, Math.max(nextSize, 3))
  } catch (error) {
    ElMessage.error(error.message || '加载回复失败')
  }
}

const submitShare = async () => {
  const trimmedContent = shareContent.value.trim()
  const images = uploadFileList.value.map((file) => file.raw).filter(Boolean)

  if (!trimmedContent && !images.length) {
    ElMessage.warning('文字和图片不能同时为空')
    return
  }

  submitting.value = true
  try {
    await createPoiShare(props.poi.id, {
      content: trimmedContent,
      images
    })
    ElMessage.success('分享发布成功')
    shareContent.value = ''
    clearUploadFiles()
    await loadShares(true)
  } catch (error) {
    ElMessage.error(error.message || '分享发布失败')
  } finally {
    submitting.value = false
  }
}

const removeShare = async (share) => {
  try {
    await ElMessageBox.confirm('删除后无法恢复，是否继续？', '删除分享', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }

  deletingId.value = share.id
  try {
    await deletePoiShare(share.id)
    shares.value = shares.value.filter((item) => item.id !== share.id)
    total.value = Math.max(total.value - 1, 0)
    ElMessage.success('分享已删除')
  } catch (error) {
    ElMessage.error(error.message || '删除分享失败')
  } finally {
    deletingId.value = null
  }
}

const ensureLoggedInForInteraction = () => {
  if (userStore.isLoggedIn) {
    return true
  }
  ElMessage.warning('登录后可参与互动')
  return false
}

const canReportShare = (share) => {
  return Boolean(userStore.isLoggedIn && currentUserId.value && share.authorUserId !== currentUserId.value)
}

const canReportReply = (reply) => {
  return Boolean(userStore.isLoggedIn && currentUserId.value && reply.authorUserId !== currentUserId.value)
}

const openShareReportDialog = (share) => {
  if (!ensureLoggedInForInteraction()) {
    return
  }
  reportTargetType.value = 'share'
  reportTargetId.value = share.id
  reportTargetLabel.value = `分享 #${share.id}`
  reportTargetPreview.value = share.content || '该分享未填写文字，仅包含图片'
  reportDialogVisible.value = true
}

const openReplyReportDialog = (reply) => {
  if (!ensureLoggedInForInteraction()) {
    return
  }
  reportTargetType.value = 'reply'
  reportTargetId.value = reply.id
  reportTargetLabel.value = `回复 #${reply.id}`
  reportTargetPreview.value = reply.content || '该回复未填写文字'
  reportDialogVisible.value = true
}

const resetReportDialog = () => {
  reportTargetType.value = ''
  reportTargetId.value = null
  reportTargetLabel.value = ''
  reportTargetPreview.value = ''
  reportReasonCode.value = REPORT_REASON_OPTIONS[0].value
  reportReasonDetail.value = ''
  reportSubmitting.value = false
}

const submitReport = async () => {
  if (!reportTargetType.value || !reportTargetId.value) {
    return
  }

  if (reportReasonCode.value === REPORT_REASON_OTHER && !reportReasonDetail.value.trim()) {
    ElMessage.warning('选择“其他”时请填写补充说明')
    return
  }

  reportSubmitting.value = true
  try {
    const payload = {
      reasonCode: reportReasonCode.value,
      reasonDetail: reportReasonDetail.value.trim() || undefined
    }

    if (reportTargetType.value === 'share') {
      await createShareReport(reportTargetId.value, payload)
    } else {
      await createReplyReport(reportTargetId.value, payload)
    }

    reportDialogVisible.value = false
    ElMessage.success('举报已提交，等待管理员处理')
  } catch (error) {
    ElMessage.error(error.message || '提交举报失败')
  } finally {
    reportSubmitting.value = false
  }
}

const toggleLike = async (share) => {
  if (!ensureLoggedInForInteraction()) {
    return
  }

  share.likeLoading = true
  try {
    const data = share.likedByCurrentUser ? await unlikePoiShare(share.id) : await likePoiShare(share.id)
    share.likeCount = data.likeCount || 0
    share.likedByCurrentUser = Boolean(data.likedByCurrentUser)
  } catch (error) {
    ElMessage.error(error.message || '点赞操作失败')
  } finally {
    share.likeLoading = false
  }
}

const toggleReplyComposer = async (share) => {
  if (!ensureLoggedInForInteraction()) {
    return
  }

  share.replyComposerVisible = !share.replyComposerVisible
  if (share.replyComposerVisible && !share.replies.length && share.replyCount > 0) {
    try {
      await refreshReplies(share, 3)
    } catch (error) {
      ElMessage.error(error.message || '加载回复失败')
    }
  }
}

const submitReply = async (share) => {
  const content = share.replyDraft.trim()
  if (!content) {
    ElMessage.warning('回复内容不能为空')
    return
  }

  share.replySubmitting = true
  try {
    await createPoiShareReply(share.id, { content })
    share.replyDraft = ''
    share.replyComposerVisible = true
    await refreshReplies(share, Math.max(share.replyCount + 1, 3))
    ElMessage.success('回复发送成功')
  } catch (error) {
    ElMessage.error(error.message || '回复发送失败')
  } finally {
    share.replySubmitting = false
  }
}

const removeReply = async (share, reply) => {
  try {
    await ElMessageBox.confirm('删除后无法恢复，是否继续？', '删除回复', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }

  share.replyDeletingId = reply.id
  try {
    await deletePoiShareReply(reply.id)
    const nextTotal = Math.max((share.replyCount || 0) - 1, 0)
    if (nextTotal === 0) {
      share.replies = []
      share.replyCount = 0
      share.replyHasMore = false
    } else {
      const nextSize = Math.min(Math.max(share.replies.length - 1, Math.min(3, nextTotal)), nextTotal)
      await refreshReplies(share, nextSize)
    }
    ElMessage.success('回复已删除')
  } catch (error) {
    ElMessage.error(error.message || '删除回复失败')
  } finally {
    share.replyDeletingId = null
  }
}

const goLogin = () => {
  router.push({
    name: 'Login',
    query: {
      redirect: router.currentRoute.value.fullPath
    }
  })
}

watch(
  () => props.poi?.id,
  async (poiId) => {
    resetPanel()
    if (poiId) {
      await loadShares(true)
    }
  },
  { immediate: true }
)

onBeforeUnmount(() => {
  clearUploadFiles()
})
</script>

<style scoped>
.share-panel {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.share-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.share-header h3 {
  margin: 0;
  font-size: 22px;
  color: #0f172a;
}

.share-header p {
  margin: 6px 0 0;
  color: #64748b;
  font-size: 14px;
}

.composer-card,
.share-card {
  padding: 20px;
  border-radius: 24px;
  background: #fff;
  box-shadow: 0 18px 44px rgba(15, 23, 42, 0.08);
}

.composer-user,
.author-block,
.reply-main {
  display: flex;
  align-items: center;
  gap: 12px;
}

.composer-user {
  margin-bottom: 16px;
}

.composer-avatar,
.author-avatar,
.reply-avatar {
  background: linear-gradient(135deg, #38bdf8, #2563eb);
  color: #fff;
  font-weight: 700;
}

.composer-user strong,
.author-line strong,
.reply-meta strong {
  color: #0f172a;
}

.composer-user p,
.author-meta time,
.login-tip p,
.reply-meta time {
  margin: 4px 0 0;
  color: #64748b;
  font-size: 13px;
}

.share-uploader {
  margin-top: 18px;
}

.upload-trigger {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  color: #475569;
}

.composer-actions,
.reply-composer-actions {
  margin-top: 12px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.upload-tip,
.reply-composer-actions span,
.reply-login-tip span {
  color: #64748b;
  font-size: 13px;
}

.login-tip,
.reply-login-tip {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
}

.share-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.share-card-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.card-actions {
  display: flex;
  align-items: center;
  gap: 4px;
  flex-shrink: 0;
}

.author-meta,
.reply-body {
  min-width: 0;
}

.author-line,
.reply-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.author-username {
  color: #64748b;
  font-size: 13px;
}

.share-content {
  margin: 16px 0 0;
  color: #1e293b;
  line-height: 1.75;
  white-space: pre-wrap;
}

.share-images {
  margin-top: 16px;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.share-image {
  width: 100%;
  height: 160px;
  border-radius: 18px;
  overflow: hidden;
}

.share-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 12px;
  padding-top: 8px;
  border-top: 1px solid #e2e8f0;
}

.reply-section {
  margin-top: 12px;
  padding: 14px 16px;
  border-radius: 18px;
  background: #f8fafc;
}

.reply-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.reply-item {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  padding-bottom: 12px;
  border-bottom: 1px solid #e2e8f0;
}

.reply-item:last-child {
  padding-bottom: 0;
  border-bottom: none;
}

.reply-main {
  align-items: flex-start;
  flex: 1;
}

.reply-body p {
  margin: 6px 0 0;
  color: #1e293b;
  line-height: 1.65;
  white-space: pre-wrap;
}

.reply-delete {
  flex-shrink: 0;
}

.reply-more {
  display: flex;
  justify-content: center;
  padding-top: 8px;
}

.reply-composer {
  margin-top: 12px;
}

.share-footer {
  display: flex;
  justify-content: center;
  padding-top: 4px;
  color: #64748b;
  font-size: 13px;
}

.preview-image {
  width: 100%;
  border-radius: 18px;
  display: block;
}

.report-dialog {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.report-target {
  padding: 16px 18px;
  border-radius: 18px;
  background: #f8fafc;
}

.report-label {
  display: inline-flex;
  margin-bottom: 10px;
  color: #64748b;
  font-size: 12px;
}

.report-target strong {
  display: block;
  color: #0f172a;
}

.report-target p {
  margin: 10px 0 0;
  color: #475569;
  line-height: 1.7;
  white-space: pre-wrap;
}

@media (max-width: 900px) {
  .composer-actions,
  .login-tip,
  .reply-composer-actions,
  .reply-login-tip {
    flex-direction: column;
    align-items: stretch;
  }
}

@media (max-width: 640px) {
  .share-header,
  .share-card-head,
  .reply-item {
    flex-direction: column;
    align-items: stretch;
  }

  .card-actions {
    justify-content: flex-end;
  }

  .share-images {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
