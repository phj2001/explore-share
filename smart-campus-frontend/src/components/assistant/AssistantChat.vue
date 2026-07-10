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
        <div class="header-actions">
          <button
            v-if="messages.length > 0"
            class="clear-btn"
            title="清空对话（结束当前多轮上下文）"
            @click="clearConversation"
          >清空</button>
          <button class="close-btn" @click="toggle">✕</button>
        </div>
      </div>

      <div ref="bodyRef" class="panel-body">
        <div v-if="disabled" class="disabled-tip">
          🚧 AI 助手暂未开启，敬请期待。
        </div>
        <div v-else-if="messages.length === 0" class="empty-tip">
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
          :placeholder="disabled ? 'AI 助手暂未开启' : '问问附近有什么好去处…（Enter 发送，Shift+Enter 换行）'"
          :disabled="loading || disabled"
          @keydown.enter.exact.prevent="send"
        ></textarea>
        <div class="input-actions">
          <button v-if="loading" class="btn btn-stop" @click="stop">停止</button>
          <button v-else class="btn btn-send" :disabled="!input.trim() || disabled" @click="send">发送</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, nextTick, onBeforeUnmount } from 'vue'
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
// 后端 app.assistant.enabled=false 时 /api/assistant/chat 返回 404；一旦探测到就整面板转为
// "暂未开启"提示，避免用户每次发消息都白白发起一次注定失败的请求
const disabled = ref(false)
// 多轮对话记忆（后端 M7/P1）：同一次面板会话复用同一个 conversationId，后端据此串联上下文；
// 关闭面板/清空对话时丢弃，重新打开视为新会话——符合这类轻量小组件"不做跨会话持久化"的预期。
const conversationId = ref(null)
let controller = null

function ensureConversationId() {
  if (!conversationId.value) {
    conversationId.value = crypto.randomUUID()
  }
}

function toggle() {
  if (open.value && loading.value) {
    // 关闭面板时中断进行中的对话，避免后台继续占用连接与带宽
    stop()
  }
  open.value = !open.value
}

function clearConversation() {
  stop()
  messages.value = []
  conversationId.value = null
}

// 组件卸载（如路由切走）时中断进行中的 SSE，避免连接与后端资源泄漏
onBeforeUnmount(() => {
  if (controller) {
    controller.abort()
    controller = null
  }
})

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

  ensureConversationId()

  messages.value.push({ role: 'user', content: text })
  messages.value.push({ role: 'assistant', content: '' })
  const assistantIndex = messages.value.length - 1
  input.value = ''
  loading.value = true
  scrollToBottom()

  controller = new AbortController()
  streamChat(
    { message: text, lat, lng, conversationId: conversationId.value },
    {
      signal: controller.signal,
      onChunk: (chunk) => {
        // 通过响应式数组的代理引用修改，确保每个 chunk 都能触发视图更新（流式渲染）
        messages.value[assistantIndex].content += chunk
        scrollToBottom()
      },
      onError: (msg, meta) => {
        if (meta?.code === 'DISABLED') {
          disabled.value = true
          // 撤回本轮占位的用户/助手消息，改为展示统一的"暂未开启"提示，避免和正常对话混在一起
          messages.value.splice(assistantIndex - 1, 2)
          loading.value = false
          return
        }
        const m = messages.value[assistantIndex]
        m.content = m.content || `（出错了：${msg}）`
        loading.value = false
      },
      onDone: () => {
        const m = messages.value[assistantIndex]
        if (!m.content) {
          m.content = '（没有返回内容）'
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
.header-actions { display: flex; align-items: center; gap: 10px; }
.clear-btn {
  background: rgba(255, 255, 255, 0.18);
  border: none;
  border-radius: 4px;
  color: #fff;
  font-size: 12px;
  padding: 3px 8px;
  cursor: pointer;
}
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
.disabled-tip {
  color: #b9852c;
  background: #fdf3e2;
  border: 1px solid #f3dfb0;
  border-radius: 8px;
  font-size: 13px;
  line-height: 1.6;
  padding: 10px 12px;
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
