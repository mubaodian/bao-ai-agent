import { buildApiUrl } from './api'

const DONE_MARKERS = new Set(['[DONE]', 'DONE', 'done'])

function extractText(payload) {
  if (payload == null) {
    return ''
  }

  if (typeof payload === 'string') {
    return payload
  }

  if (Array.isArray(payload)) {
    return payload.map(extractText).join('')
  }

  if (typeof payload === 'object') {
    if (payload.done === true) {
      return '[DONE]'
    }

    const directKeys = [
      'data',
      'content',
      'answer',
      'text',
      'message',
      'output',
      'delta',
      'token',
      'result',
    ]

    for (const key of directKeys) {
      if (key in payload) {
        return extractText(payload[key])
      }
    }

    if ('choices' in payload) {
      return extractText(payload.choices)
    }
  }

  return ''
}

function normalizeChunk(raw) {
  const trimmed = `${raw}`.trim()

  if (!trimmed) {
    return null
  }

  if (DONE_MARKERS.has(trimmed)) {
    return { done: true, text: '' }
  }

  try {
    const parsed = JSON.parse(trimmed)
    const text = extractText(parsed)

    if (DONE_MARKERS.has(text.trim())) {
      return { done: true, text: '' }
    }

    return { done: false, text }
  } catch {
    return { done: false, text: raw }
  }
}

export function openSseStream(url, params, handlers = {}) {
  const source = new EventSource(buildApiUrl(url, params))
  let closed = false
  let opened = false
  let doneReceived = false

  const close = () => {
    if (closed) {
      return
    }

    closed = true
    source.close()
  }

  source.onopen = () => {
    opened = true
  }

  source.onmessage = (event) => {
    if (closed) {
      return
    }

    const chunk = normalizeChunk(event.data)

    if (!chunk) {
      return
    }

    if (chunk.done) {
      doneReceived = true
      handlers.onDone?.()
      close()
      return
    }

    handlers.onChunk?.(chunk.text)
  }

  source.onerror = () => {
    if (closed) {
      return
    }

    close()

    if (doneReceived) {
      return
    }

    // EventSource 也会在服务端正常结束流时触发 error。
    // 若连接从未建立，视为连接失败；否则按正常结束处理。
    if (!opened) {
      handlers.onError?.(new Error('SSE connection failed'))
      return
    }

    handlers.onDone?.()
  }

  return close
}
