import { BASE_URL } from '@/utils/request.js'
import { getToken } from '@/utils/auth.js'

/**
 * AI 探索助手 · 流式对话（M5）。
 *
 * 后端 POST /api/assistant/chat 返回 SSE（text/event-stream），且需要 JWT 鉴权。
 * 浏览器原生 EventSource 只能 GET、无法带 Authorization 头，故用 fetch + ReadableStream
 * 手动消费 SSE。
 *
 * @param payload  { message, lat, lng, radius?, conversationId? }  conversationId 由调用方生成
 *                 （同一次面板会话内复用同一个 ID），用于后端多轮对话记忆；不传则为无历史单轮请求。
 * @param handlers { onChunk(text), onError(msg, meta?)：meta.code === 'DISABLED' 表示后端功能整体未开启, onDone(), signal }
 */
export async function streamChat({ message, lat, lng, radius, conversationId }, { onChunk, onError, onDone, signal } = {}) {
  const token = getToken()
  let resp
  try {
    resp = await fetch(`${BASE_URL}/assistant/chat`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        // SSE 优先（正常流式），application/json 兜底：controller 卸载/异常时 @ExceptionHandler
        // 只能返回 JSON 错误体，若 Accept 仅限 text/event-stream 会触发内容协商失败(406)，
        // 前端反而拿不到 404 降级信号。
        Accept: 'text/event-stream, application/json',
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
      },
      body: JSON.stringify({ message, lat, lng, radius, conversationId }),
      signal,
    })
  } catch (e) {
    onError?.(e?.message || '无法连接到服务')
    return
  }

  if (!resp.ok || !resp.body) {
    // 404：后端 @ConditionalOnProperty(app.assistant.enabled=true) 未装配 controller，
    // 说明助手功能当前被后端整体关闭（而非网络故障），单独给出可辨识的提示与错误码，
    // 便于前端据此展示"暂不可用"横幅而不是每次都当普通失败重试。
    if (resp.status === 404) {
      onError?.('AI 助手暂未开启，敬请期待', { code: 'DISABLED' })
      return
    }
    // 限流(429)/护栏(400)等业务错误的 body 是 JSON Result，读取其中 message 给出具体提示
    let message = resp.status === 401 ? '登录已失效，请重新登录' : `请求失败 (${resp.status})`
    try {
      const body = await resp.json()
      if (body?.message) {
        message = body.message
      }
    } catch {
      // body 不是 JSON（如网关错误页），保留默认提示
    }
    onError?.(message)
    return
  }

  const reader = resp.body.getReader()
  const decoder = new TextDecoder('utf-8')
  let buffer = ''

  try {
    // eslint-disable-next-line no-constant-condition
    while (true) {
      const { done, value } = await reader.read()
      if (done) break
      buffer += decoder.decode(value, { stream: true })
      let sep
      while ((sep = buffer.indexOf('\n\n')) >= 0) {
        const rawEvent = buffer.slice(0, sep)
        buffer = buffer.slice(sep + 2)
        parseEvent(rawEvent, onChunk, onError)
      }
    }
    if (buffer.trim()) {
      parseEvent(buffer, onChunk, onError)
    }
    onDone?.()
  } catch (e) {
    if (e?.name === 'AbortError') {
      return
    }
    onError?.(e?.message || '流式读取失败')
  }
}

/** 解析单个 SSE 事件块（兼容 SseEmitter 的 data: / event: 行）。 */
function parseEvent(raw, onChunk, onError) {
  let eventName = 'message'
  const dataLines = []
  for (const line of raw.split('\n')) {
    if (line.startsWith('data:')) {
      dataLines.push(line.slice(5).replace(/^ /, ''))
    } else if (line.startsWith('event:')) {
      eventName = line.slice(6).trim()
    }
  }
  if (dataLines.length === 0) {
    return
  }
  const data = dataLines.join('\n')
  if (eventName === 'error') {
    onError?.(data)
  } else {
    onChunk?.(data)
  }
}
