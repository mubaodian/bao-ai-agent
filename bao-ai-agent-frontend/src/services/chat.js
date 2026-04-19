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

function emitNormalizedChunk(raw, handlers) {
  const chunk = normalizeChunk(raw)

  if (!chunk) {
    return false
  }

  if (chunk.done) {
    handlers.onDone?.()
    return true
  }

  handlers.onChunk?.(chunk.text)
  return false
}

function parseEventBlock(eventBlock) {
  const lines = eventBlock.split(/\r?\n/)
  const dataLines = []

  for (const line of lines) {
    if (!line || line.startsWith(':')) {
      continue
    }

    if (line.startsWith('data:')) {
      dataLines.push(line.slice(5).trimStart())
    }
  }

  return dataLines.join('\n')
}

function consumeEventBlocks(buffer, handlers, flushAll = false) {
  let rest = buffer

  while (rest.length > 0) {
    const match = rest.match(/\r?\n\r?\n/)

    if (!match || match.index == null) {
      break
    }

    const eventBlock = rest.slice(0, match.index)
    rest = rest.slice(match.index + match[0].length)

    const data = parseEventBlock(eventBlock)

    if (data && emitNormalizedChunk(data, handlers)) {
      return { rest: '', ended: true }
    }
  }

  if (flushAll) {
    const pending = rest.trim()

    if (pending) {
      const data = parseEventBlock(pending) || pending

      if (emitNormalizedChunk(data, handlers)) {
        return { rest: '', ended: true }
      }
    }

    return { rest: '', ended: false }
  }

  return { rest, ended: false }
}

export function openSseStream(url, params, handlers = {}) {
  const controller = new AbortController()
  const decoder = new TextDecoder('utf-8')
  let closed = false

  const close = () => {
    if (closed) {
      return
    }

    closed = true
    controller.abort()
  }

  ;(async () => {
    let buffer = ''

    try {
      const response = await fetch(buildApiUrl(url, params), {
        method: 'GET',
        headers: {
          Accept: 'text/event-stream',
        },
        cache: 'no-store',
        signal: controller.signal,
      })

      if (!response.ok) {
        throw new Error(`HTTP ${response.status}`)
      }

      if (!response.body) {
        throw new Error('ReadableStream not supported')
      }

      const reader = response.body.getReader()

      while (!closed) {
        const { value, done } = await reader.read()

        if (done) {
          break
        }

        buffer += decoder.decode(value, { stream: true })

        const result = consumeEventBlocks(buffer, handlers)
        buffer = result.rest

        if (result.ended) {
          close()
          return
        }
      }

      buffer += decoder.decode()

      if (!closed) {
        const result = consumeEventBlocks(buffer, handlers, true)

        if (result.ended) {
          close()
          return
        }

        handlers.onDone?.()
      }
    } catch (error) {
      if (closed || error?.name === 'AbortError') {
        return
      }

      handlers.onError?.(error)
    }
  })()

  return close
}
