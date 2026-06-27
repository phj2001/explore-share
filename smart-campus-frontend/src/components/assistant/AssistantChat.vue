<template>
  <div class="assistant-widget">
    <!-- 悬浮按钮 -->
    <button v-if="!open" class="assistant-fab" @click="toggle" title="AI 探索助手">
      <span class="fab-icon">🧭</span>
      <span class="fab-text">AI 助手</span>
    </button>

    <!-- 对话面板 -->
    <div v-else class="assistant-panel">
      <div class="panel-header">
        <span class="title">🧭 AI 探索助手</span>
        <button class="close-btn" @click="toggle">✕</button>
      </div>

      <div ref="bodyRef" class="panel-body">
        <div v-if="messages.length === 0" class="empty-tip">
          试试问我：“附近适合周末带娃的地方”“安静的咖啡馆”“从这里到图书馆怎么走”
        </div>
        <div
          v-for="(m, i) in messages"
          :key="i"
          :class="['msg', m.role === 'user' ? 'msg-user' : 'msg-assistant']"
        >
          <div class="bubble">
            <span v-if="m.role === 'assistant' && !m.content && loading" class="typing">正在思考…</span>
            <span v-else style="white-space: pre-wrap">{{ m.content }}</span>
          </div>
        </div>
      </div>

      <div class="panel-input">
        <textarea
          v-model="input"
          class="input-area"
          rows="2"
          placeholder="问问附近有什么好去处…（Enter 发送，Shift+Enter 换行）"
          :disabled="loading"
          @keydown.enter.exact.prevent="send"
        ></textarea>
        <div class="input-actions">
          <button v-if="loading" class="btn btn-stop" @click="stop">停止</button>
          <button v-else class="btn btn-send" :disabled="!input.trim()" @click="send">发送</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { useMapStore } from '@/stores/map.js'
import { useUserStore } from '@/stores/user.js'
import { streamChat } from '@/api/assistant.js'

const mapStore = useMapStore()
const userStore = useUserStore()

const open = ref(false)
const input = ref('')
const loading = ref(false)
const messages = ref([])
const bodyRef = ref(null)
let controller = null

function toggle() {
  open.value = !open.value
}

async function scrollToBottom() {
  await nextTick()
  if (bodyRef.value) {
    bodyRef.value.scrollTop = bodyRef.value.scrollHeight
  }
}

function send() {
  const text = input.value.trim()
  if (!text || loading.value) {
    return
  }
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录后使用 AI 助手')
    return
  }

  // 当前地图中心作为用户位置
  const center = mapStore.center || {}
  const lat = Number(center.lat)
  const lng = Number(center.lng)
  if (!Number.isFinite(lat) || !Number.isFinite(lng)) {
    ElMessage.warning('无法获取当前地图位置，请先在地图上选择区域')
    return
  }

  messages.value.push({ role: 'user', content: text })
  const assistantMsg = { role: 'assistant', content: '' }
  messages.value.push(assistantMsg)
  input.value = ''
  loading.value = true
  scrollToBottom()

  controller = new AbortController()
  streamChat(
    { message: text, lat, lng },
    {
      signal: controller.signal,
      onChunk: (chunk) => {
        assistantMsg.content += chunk
        scrollToBottom()
      },
      onError: (msg) => {
        assistantMsg.content = assistantMsg.content || `（出错了：${msg}）`
        loading.value = false
      },
      onDone: () => {
        if (!assistantMsg.content) {
          assistantMsg.content = '（没有返回内容）'
        }
        loading.value = false
        scrollToBottom()
      },
    },
  )
}

function stop() {
  if (controller) {
    controller.abort()
    controller = null
  }
  loading.value = false
}
</script>

<style scoped>
.assistant-fab {
  position: fixed;
  right: 24px;
  bottom: 24px;
  z-index: 2000;
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 16px;
  border: none;
  border-radius: 24px;
  background: linear-gradient(135deg, #1f8c69, #3a9bd2);
  color: #fff;
  font-size: 14px;
  cursor: pointer;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.2);
}
.fab-icon { font-size: 18px; }

.assistant-panel {
  position: fixed;
  right: 24px;
  bottom: 24px;
  z-index: 2000;
  width: 360px;
  max-width: calc(100vw - 32px);
  height: 520px;
  max-height: calc(100vh - 100px);
  display: flex;
  flex-direction: column;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.25);
  overflow: hidden;
}
.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  background: linear-gradient(135deg, #1f8c69, #3a9bd2);
  color: #fff;
}
.panel-header .title { font-weight: 600; }
.close-btn {
  background: transparent;
  border: none;
  color: #fff;
  font-size: 16px;
  cursor: pointer;
}
.panel-body {
  flex: 1;
  overflow-y: auto;
  padding: 12px;
  background: #f7f8fa;
}
.empty-tip {
  color: #999;
  font-size: 13px;
  line-height: 1.6;
  padding: 8px;
}
.msg { display: flex; margin-bottom: 10px; }
.msg-user { justify-content: flex-end; }
.msg-assistant { justify-content: flex-start; }
.bubble {
  max-width: 78%;
  padding: 8px 12px;
  border-radius: 10px;
  font-size: 14px;
  line-height: 1.5;
  word-break: break-word;
}
.msg-user .bubble { background: #1f8c69; color: #fff; border-bottom-right-radius: 2px; }
.msg-assistant .bubble { background: #fff; color: #333; border: 1px solid #eee; border-bottom-left-radius: 2px; }
.typing { color: #999; }
.panel-input {
  border-top: 1px solid #eee;
  padding: 8px;
  background: #fff;
}
.input-area {
  width: 100%;
  border: 1px solid #ddd;
  border-radius: 8px;
  padding: 8px;
  font-size: 14px;
  resize: none;
  outline: none;
  box-sizing: border-box;
}
.input-actions { display: flex; justify-content: flex-end; margin-top: 6px; }
.btn {
  border: none;
  border-radius: 6px;
  padding: 6px 16px;
  font-size: 13px;
  cursor: pointer;
}
.btn-send { background: #1f8c69; color: #fff; }
.btn-send:disabled { background: #b9c7c2; cursor: not-allowed; }
.btn-stop { background: #e6a23c; color: #fff; }
</style>
