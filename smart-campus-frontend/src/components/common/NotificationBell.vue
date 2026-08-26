<template>
  <el-popover
    placement="bottom-end"
    width="min(92vw, 380px)"
    trigger="click"
    @before-enter="onPopoverOpen"
  >
    <template #reference>
      <el-badge :value="unreadCount" :hidden="unreadCount === 0" :max="99" class="notification-badge">
        <el-button class="notification-bell" aria-label="通知">
          <el-icon :size="20"><Bell /></el-icon>
        </el-button>
      </el-badge>
    </template>

    <div class="notification-panel">
      <div class="notification-header">
        <span class="notification-title">通知</span>
        <el-button v-if="unreadCount > 0" link type="primary" size="small" @click="handleMarkAllRead">
          全部已读
        </el-button>
      </div>

      <div v-loading="loading" class="notification-list">
        <template v-if="notifications.length">
          <div
            v-for="item in notifications"
            :key="item.id"
            class="notification-item"
            :class="{ unread: !item.read }"
            @click="handleClick(item)"
          >
            <el-avatar
              v-if="item.actorAvatarUrl"
              :size="36"
              :src="resolveMediaUrl(item.actorAvatarUrl)"
              class="notification-avatar"
            >
              {{ (item.actorDisplayName || '?').slice(0, 1) }}
            </el-avatar>
            <div v-else class="notification-icon-placeholder">
              <el-icon :size="18">
                <component :is="typeIcon(item.type)" />
              </el-icon>
            </div>

            <div class="notification-body">
              <div class="notification-text">{{ item.title }}</div>
              <div v-if="item.content" class="notification-content">{{ item.content }}</div>
              <div class="notification-time">{{ formatTime(item.createdAt) }}</div>
            </div>

            <div v-if="!item.read" class="notification-dot" />
          </div>

          <div v-if="hasMore" class="notification-more">
            <el-button link type="primary" :loading="loadingMore" @click="loadMore">
              加载更多
            </el-button>
          </div>
        </template>

        <div v-else class="notification-empty">
          <el-empty description="暂无通知" :image-size="60" />
        </div>
      </div>
    </div>
  </el-popover>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElNotification } from 'element-plus'
import { Bell, Star, ChatDotRound, Trophy, User, Location, Warning, CircleCheck, CircleClose } from '@element-plus/icons-vue'
import { getNotifications, getUnreadCount, markAsRead, markAllAsRead } from '@/api/notification'
import { API_ORIGIN } from '@/utils/request'

const resolveMediaUrl = (value) => {
  if (!value) return ''
  if (/^https?:\/\//i.test(value)) return value
  return `${API_ORIGIN}${value.startsWith('/') ? value : `/${value}`}`
}

const router = useRouter()

const loading = ref(false)
const loadingMore = ref(false)
const notifications = ref([])
const unreadCount = ref(0)
const page = ref(0)
const hasMore = ref(false)

let pollTimer = null
let prevUnreadCount = -1

const typeIcon = (type) => {
  const map = {
    LIKE: Star,
    REPLY: ChatDotRound,
    FOLLOW: User,
    ACHIEVEMENT: Trophy,
    POI_APPROVED: Location,
    POI_REJECTED: Warning,
    ROUTE_APPROVED: CircleCheck,
    ROUTE_REJECTED: CircleClose
  }
  return map[type] || Bell
}

const formatTime = (dateStr) => {
  if (!dateStr) return ''
  const diff = Date.now() - new Date(dateStr).getTime()
  const minutes = Math.floor(diff / 60000)
  if (minutes < 1) return '刚刚'
  if (minutes < 60) return `${minutes}分钟前`
  const hours = Math.floor(minutes / 60)
  if (hours < 24) return `${hours}小时前`
  const days = Math.floor(hours / 24)
  if (days < 30) return `${days}天前`
  return new Date(dateStr).toLocaleDateString()
}

const showLatestToast = async () => {
  try {
    const notifRes = await getNotifications({ page: 0, size: 1 })
    const latest = notifRes?.records?.[0]
    if (latest && !latest.read) {
      const isRejected = latest.type?.includes('REJECTED')
      ElNotification({
        title: latest.title,
        message: latest.content || '',
        type: isRejected ? 'warning' : 'success',
        duration: 6000,
        position: 'top-right'
      })
    }
  } catch {}
}

const fetchUnreadCount = async () => {
  try {
    const res = await getUnreadCount()
    const newCount = res?.count || 0

    if (prevUnreadCount === -1) {
      // 首次加载：只要有未读就弹提示（包含登录后已有的路线审核结果）
      if (newCount > 0) {
        await showLatestToast()
      }
    } else if (newCount > prevUnreadCount) {
      // 浏览过程中新到的通知
      await showLatestToast()
    }

    prevUnreadCount = newCount
    unreadCount.value = newCount
  } catch {}
}

const fetchNotifications = async (append = false) => {
  if (append) {
    loadingMore.value = true
  } else {
    loading.value = true
  }
  try {
    const res = await getNotifications({ page: page.value, size: 10 })
    if (append) {
      notifications.value.push(...(res?.records || []))
    } else {
      notifications.value = res?.records || []
    }
    hasMore.value = res?.hasNext || false
  } catch {} finally {
    loading.value = false
    loadingMore.value = false
  }
}

const loadMore = () => {
  page.value++
  fetchNotifications(true)
}

const handleClick = async (item) => {
  if (!item.read) {
    try {
      await markAsRead(item.id)
      item.read = true
      unreadCount.value = Math.max(0, unreadCount.value - 1)
    } catch {}
  }

  if (item.targetType === 'SHARE' && item.targetId) {
    router.push('/')
    window.dispatchEvent(new CustomEvent('poi:navigate-to-share', { detail: { shareId: item.targetId } }))
  } else if (item.targetType === 'USER' && item.targetId) {
    router.push(`/user/${item.targetId}`)
  } else if (item.targetType === 'ROUTE' && item.targetId) {
    router.push(`/route/${item.targetId}`)
  }
}

const handleMarkAllRead = async () => {
  try {
    await markAllAsRead()
    notifications.value.forEach(n => { n.read = true })
    unreadCount.value = 0
  } catch {}
}

const onPopoverOpen = () => {
  page.value = 0
  fetchNotifications()
}

onMounted(() => {
  fetchUnreadCount()
  pollTimer = setInterval(fetchUnreadCount, 30000)
})

onUnmounted(() => {
  if (pollTimer) clearInterval(pollTimer)
})
</script>

<style scoped lang="scss">
.notification-badge {
  line-height: 1;
}

.notification-bell {
  padding: 8px;
  border-radius: 12px;
  border: none;
  background: transparent;
  color: var(--front-text-soft);
}

.notification-bell:hover {
  background: var(--front-accent-soft);
  color: var(--front-accent-strong);
}

.notification-panel {
  margin: -12px;
}

.notification-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid var(--el-border-color-lighter);
}

.notification-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--front-text);
}

.notification-list {
  max-height: 400px;
  overflow-y: auto;
}

.notification-item {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 12px 16px;
  cursor: pointer;
  transition: background-color 0.15s;
}

.notification-item:hover {
  background: var(--el-fill-color-light);
}

.notification-item.unread {
  background: var(--el-color-primary-light-9);
}

.notification-avatar {
  flex-shrink: 0;
  background: linear-gradient(135deg, var(--front-accent), var(--front-accent-strong));
  color: #fff;
  font-weight: 700;
  font-size: 13px;
}

.notification-icon-placeholder {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: grid;
  place-items: center;
  background: var(--el-fill-color);
  color: var(--front-text-soft);
  flex-shrink: 0;
}

.notification-body {
  flex: 1;
  min-width: 0;
}

.notification-text {
  font-size: 13px;
  color: var(--front-text);
  line-height: 1.5;
}

.notification-content {
  font-size: 12px;
  color: var(--front-text-muted);
  margin-top: 2px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.notification-time {
  font-size: 11px;
  color: var(--front-text-muted);
  margin-top: 4px;
}

.notification-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--el-color-primary);
  flex-shrink: 0;
  margin-top: 6px;
}

.notification-more {
  text-align: center;
  padding: 8px;
}

.notification-empty {
  padding: 24px 0;
}

/* 窄屏：下拉列表限高（dvh 随地址栏收放），避免小屏/横屏溢出 */
@include respond-to(xs) {
  .notification-list {
    max-height: 60vh;
    max-height: 60dvh;
  }
}

/* 触屏：铃铛入口与"加载更多"撑足热区 */
@include coarse-pointer {
  .notification-bell {
    min-width: 40px;
    min-height: 40px;
  }

  .notification-more :deep(.el-button) {
    min-height: 40px;
  }
}
</style>
