<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { openSseStream } from '../services/chat'
import SiteFooter from './SiteFooter.vue'

const TYPING_INTERVAL_MS = 18
const TYPING_BATCH_SIZE = 2

const props = defineProps({
  title: {
    type: String,
    required: true,
  },
  subtitle: {
    type: String,
    required: true,
  },
  endpoint: {
    type: String,
    required: true,
  },
  promptKey: {
    type: String,
    required: true,
  },
  intro: {
    type: String,
    required: true,
  },
  theme: {
    type: String,
    default: 'rose',
  },
  useChatId: {
    type: Boolean,
    default: false,
  },
  placeholder: {
    type: String,
    default: '输入你的问题...',
  },
  assistantBubbleMode: {
    type: String,
    default: 'single',
  },
})

const router = useRouter()
const messageListRef = ref(null)
const draft = ref('')
const isStreaming = ref(false)
const isPaused = ref(false)
const chatId = ref('')
const streamCloser = ref(null)
const messages = ref([])

let typingTimer = null
let typingQueue = []
let pendingFinalizer = null
let hasAssistantOutput = false

const themeMap = {
  rose: {
    accent: '#f97171',
    accentSoft: 'rgba(249, 113, 113, 0.18)',
    accentStrong: 'rgba(249, 113, 113, 0.92)',
    panel: 'rgba(66, 23, 34, 0.66)',
  },
  teal: {
    accent: '#2dd4bf',
    accentSoft: 'rgba(45, 212, 191, 0.16)',
    accentStrong: 'rgba(13, 148, 136, 0.92)',
    panel: 'rgba(11, 47, 56, 0.66)',
  },
}

const themeStyle = computed(() => {
  const current = themeMap[props.theme] ?? themeMap.rose

  return {
    '--accent': current.accent,
    '--accent-soft': current.accentSoft,
    '--accent-strong': current.accentStrong,
    '--panel': current.panel,
  }
})

const showPauseButton = computed(() => isStreaming.value || isPaused.value)
const statusText = computed(() => {
  if (isPaused.value) {
    return '已暂停显示'
  }

  if (isStreaming.value) {
    return '实时回复中'
  }

  return '等待输入'
})

function createSessionId() {
  if (typeof crypto !== 'undefined' && crypto.randomUUID) {
    return crypto.randomUUID()
  }

  return `chat-${Date.now()}-${Math.random().toString(16).slice(2, 10)}`
}

function createAssistantIntro() {
  return reactive({
    id: `intro-${Date.now()}`,
    role: 'assistant',
    content: props.intro,
  })
}

function clearTypingState() {
  if (typingTimer) {
    clearTimeout(typingTimer)
    typingTimer = null
  }

  typingQueue = []
  pendingFinalizer = null
  hasAssistantOutput = false
  isPaused.value = false
}

function scrollToBottom() {
  nextTick(() => {
    const container = messageListRef.value

    if (!container) {
      return
    }

    container.scrollTop = container.scrollHeight
  })
}

function finishStreaming() {
  isStreaming.value = false
  streamCloser.value = null
  scrollToBottom()
}

function flushTypingQueue() {
  typingTimer = null

  if (isPaused.value) {
    return
  }

  const currentTask = typingQueue[0]

  if (currentTask) {
    const nextChunk = currentTask.text.slice(0, TYPING_BATCH_SIZE)
    currentTask.text = currentTask.text.slice(TYPING_BATCH_SIZE)
    currentTask.target.content += nextChunk
    scrollToBottom()

    if (!currentTask.text) {
      typingQueue.shift()
    }

    typingTimer = setTimeout(flushTypingQueue, TYPING_INTERVAL_MS)
  } else if (pendingFinalizer) {
    const finalize = pendingFinalizer
    pendingFinalizer = null
    finalize()
  }
}

function enqueueAssistantText(target, chunk) {
  if (!chunk) {
    return
  }

  typingQueue.push({
    target,
    text: chunk,
  })

  if (!typingTimer && !isPaused.value) {
    flushTypingQueue()
  }
}

function finishAssistantResponse(target, fallbackText = '') {
  pendingFinalizer = () => {
    if (!target.content.trim() && fallbackText) {
      target.content = fallbackText
    }

    finishStreaming()
    clearTypingState()
  }

  if (!typingTimer && typingQueue.length === 0 && !isPaused.value) {
    flushTypingQueue()
  }
}

function toggleReplyPlayback() {
  if (!showPauseButton.value) {
    return
  }

  if (isPaused.value) {
    isPaused.value = false

    if (!typingTimer) {
      flushTypingQueue()
    }

    return
  }

  isPaused.value = true

  if (typingTimer) {
    clearTimeout(typingTimer)
    typingTimer = null
  }
}

function createAssistantMessage() {
  return reactive({
    id: `assistant-${Date.now()}-${Math.random().toString(16).slice(2, 8)}`,
    role: 'assistant',
    content: '',
  })
}

function resetConversation() {
  streamCloser.value?.()
  streamCloser.value = null
  clearTypingState()
  isStreaming.value = false
  messages.value = [createAssistantIntro()]

  if (props.useChatId) {
    chatId.value = createSessionId()
  }
}

function submitMessage() {
  const content = draft.value.trim()

  if (!content || isStreaming.value) {
    return
  }

  clearTypingState()

  const userMessage = {
    id: `user-${Date.now()}`,
    role: 'user',
    content,
  }

  const assistantMessage =
    props.assistantBubbleMode === 'single' ? createAssistantMessage() : null

  messages.value.push(userMessage)

  if (assistantMessage) {
    messages.value.push(assistantMessage)
  }

  draft.value = ''
  isStreaming.value = true
  isPaused.value = false
  hasAssistantOutput = false

  const params = {
    [props.promptKey]: content,
  }

  if (props.useChatId) {
    params.chatId = chatId.value || createSessionId()
    chatId.value = params.chatId
  }

  streamCloser.value = openSseStream(props.endpoint, params, {
    onChunk(chunk) {
      hasAssistantOutput = true

      if (props.assistantBubbleMode === 'per-event') {
        const stepMessage = createAssistantMessage()
        messages.value.push(stepMessage)
        enqueueAssistantText(stepMessage, chunk)
        return
      }

      enqueueAssistantText(assistantMessage, chunk)
    },
    onDone() {
      const fallbackTarget =
        assistantMessage ?? (hasAssistantOutput ? null : createAssistantMessage())

      if (fallbackTarget && !messages.value.includes(fallbackTarget)) {
        messages.value.push(fallbackTarget)
      }

      if (fallbackTarget) {
        finishAssistantResponse(
          fallbackTarget,
          '本次响应已结束，但服务端没有返回可显示的内容。'
        )
        return
      }

      pendingFinalizer = () => {
        finishStreaming()
        clearTypingState()
      }

      if (!typingTimer && typingQueue.length === 0 && !isPaused.value) {
        flushTypingQueue()
      }
    },
    onError() {
      const fallbackTarget =
        assistantMessage ?? (hasAssistantOutput ? null : createAssistantMessage())

      if (fallbackTarget && !messages.value.includes(fallbackTarget)) {
        messages.value.push(fallbackTarget)
      }

      if (fallbackTarget) {
        finishAssistantResponse(
          fallbackTarget,
          '连接中断，请确认后端服务已启动后重试。'
        )
        return
      }

      pendingFinalizer = () => {
        finishStreaming()
        clearTypingState()
      }

      if (!typingTimer && typingQueue.length === 0 && !isPaused.value) {
        flushTypingQueue()
      }
    },
  })

  scrollToBottom()
}

function handleEnter(event) {
  if (event.shiftKey) {
    return
  }

  event.preventDefault()
  submitMessage()
}

onMounted(() => {
  resetConversation()
})

onBeforeUnmount(() => {
  streamCloser.value?.()
  clearTypingState()
})

watch(
  messages,
  () => {
    scrollToBottom()
  },
  { deep: true }
)
</script>

<template>
  <div class="chat-page" :style="themeStyle">
    <div class="chat-backdrop"></div>
    <section class="chat-shell">
      <header class="chat-header">
        <div class="header-main">
          <div class="header-actions">
            <button class="ghost-button" type="button" @click="router.push('/')">
              返回主页
            </button>
            <span class="status-pill">{{ statusText }}</span>
          </div>
          <p class="eyebrow">{{ title }}</p>
          <h1>{{ subtitle }}</h1>
        </div>
        <div class="header-side">
          <div v-if="useChatId" class="session-card">
            <span>聊天室 ID</span>
            <strong>{{ chatId }}</strong>
          </div>
          <button class="reset-button" type="button" @click="resetConversation">
            {{ useChatId ? '新建会话' : '清空对话' }}
          </button>
        </div>
      </header>

      <div ref="messageListRef" class="message-list">
        <article
          v-for="message in messages"
          :key="message.id"
          class="message-row"
          :class="message.role"
        >
          <div class="message-bubble">
            <span class="message-role">
              {{ message.role === 'user' ? '我' : 'AI' }}
            </span>
            <p>{{ message.content }}</p>
          </div>
        </article>
      </div>

      <footer class="composer">
        <textarea
          v-model="draft"
          class="composer-input"
          rows="4"
          :placeholder="placeholder"
          @keydown.enter="handleEnter"
        />
        <div class="composer-actions">
          <span class="hint">Enter 发送，Shift + Enter 换行</span>
          <div class="composer-buttons">
            <button
              v-if="showPauseButton"
              class="reset-button"
              type="button"
              @click="toggleReplyPlayback"
            >
              {{ isPaused ? '继续回复' : '暂停回复' }}
            </button>
            <button
              class="send-button"
              type="button"
              :disabled="isStreaming || !draft.trim()"
              @click="submitMessage"
            >
              {{ isStreaming ? '生成中...' : '发送消息' }}
            </button>
          </div>
        </div>
      </footer>

      <SiteFooter compact />
    </section>
  </div>
</template>
