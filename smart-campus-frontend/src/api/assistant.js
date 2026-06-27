import { BASE_URL } from '@/utils/request.js'
import { getToken } from '@/utils/auth.js'

/**
 * AI 探索助手 · 流式对话（M5）。
 *
 * 后端 POST /api/assistant/chat 返回 SSE（text/event-stream），且需要 JWT 鉴权。
 * 浏览器原生 EventSource 只能 GET、无法带 Authorization 头，故用 fetch + ReadableStream
 * 手动消费 SSE。
 *
 * @param payload  { message, lat, lng, radius? }
 * @param handlers { onChunk(text), onError(msg), onDone(), signal }
 */
export async function streamChat({ message, lat, lng, radius }, { onChunk, onError, onDone, signal } = {}) {
  const token = getToken()
  let resp
  try {
    resp = await fetch(`${BASE_URL}/assistant/chat`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Accept: 'text/event-stream',
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
      },
      body: JSON.stringify({ message, lat, lng, radius }),
      signal,
    })
  } catch (e) {
    onError?.(e?.message || '无法连接到服务')
    return
  }

  if (!resp.ok || !resp.body) {
    onError?.(resp.status === 401 ? '登录已失效，请重新登录' : `请求失败 (${resp.status})`)
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
