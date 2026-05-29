<script setup lang="ts">
import { ref, nextTick, onUnmounted } from 'vue'
import { createChatStream, qaApi } from '../api'
import { ElMessage } from 'element-plus'
import { Delete } from '@element-plus/icons-vue'
import type { ChatMessage } from '../api/types'
import ChatMessageBubble from '../components/ChatMessage.vue'
import ChatEmptyState from '../components/ChatEmptyState.vue'

const messages = ref<ChatMessage[]>([])
const inputMessage = ref('')
const loading = ref(false)
const chatContainer = ref<HTMLElement | null>(null)
let activeEventSource: EventSource | null = null

onUnmounted(() => {
  activeEventSource?.close()
  activeEventSource = null
})

function applySuggestion(text: string) {
  inputMessage.value = text
}

async function sendMessage() {
  const question = inputMessage.value.trim()
  if (!question || loading.value) return

  activeEventSource?.close()

  loading.value = true
  messages.value.push({ role: 'user', content: question })
  inputMessage.value = ''

  messages.value.push({ role: 'assistant', content: '' })
  const assistantIndex = messages.value.length - 1

  scrollToBottom()

  activeEventSource = createChatStream(question)

  activeEventSource.onmessage = (event) => {
    if (event.data === '[DONE]') {
      activeEventSource?.close()
      activeEventSource = null
      loading.value = false
      return
    }
    if (event.data.startsWith('[ERROR] ')) {
      messages.value[assistantIndex].content = '抱歉，发生了错误：' + event.data.substring(8)
      activeEventSource?.close()
      activeEventSource = null
      loading.value = false
      return
    }
    messages.value[assistantIndex].content += event.data
    scrollToBottom()
  }

  activeEventSource.onerror = () => {
    activeEventSource?.close()
    activeEventSource = null
    loading.value = false
    if (!messages.value[assistantIndex].content) {
      messages.value[assistantIndex].content = '抱歉，发生了错误，请重试。'
    }
  }
}

async function clearHistory() {
  try {
    await qaApi.clearHistory()
    messages.value = []
    ElMessage.success('对话历史已清除')
  } catch {
    ElMessage.error('清除历史失败')
  }
}

function scrollToBottom() {
  nextTick(() => {
    if (chatContainer.value) {
      chatContainer.value.scrollTop = chatContainer.value.scrollHeight
    }
  })
}

function handleKeydown(e: KeyboardEvent) {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
    sendMessage()
  }
}

function autoResize(e: Event) {
  const el = e.target as HTMLTextAreaElement
  el.style.height = 'auto'
  el.style.height = Math.min(el.scrollHeight, 160) + 'px'
}
</script>

<template>
  <div class="chat-view">
    <div class="chat-topbar">
      <span class="topbar-title">对话问答</span>
      <el-button
        v-if="messages.length > 0"
        text
        size="small"
        :icon="Delete"
        @click="clearHistory"
      >
        清除对话
      </el-button>
    </div>

    <div class="chat-messages" ref="chatContainer">
      <ChatEmptyState
        v-if="messages.length === 0"
        @select="applySuggestion"
      />

      <div class="messages-inner">
        <ChatMessageBubble
          v-for="(msg, index) in messages"
          :key="index"
          :message="msg"
          :is-streaming="loading && index === messages.length - 1 && msg.role === 'assistant'"
        />
      </div>
    </div>

    <div class="chat-input-area">
      <div class="input-wrapper">
        <textarea
          v-model="inputMessage"
          class="chat-textarea"
          placeholder="输入您的问题..."
          rows="1"
          :disabled="loading"
          @keydown="handleKeydown"
          @input="autoResize"
        />
        <button
          class="send-btn"
          :disabled="!inputMessage.trim() || loading"
          @click="sendMessage"
        >
          <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <line x1="22" y1="2" x2="11" y2="13" />
            <polygon points="22 2 15 22 11 13 2 9 22 2" />
          </svg>
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.chat-view {
  display: flex;
  flex-direction: column;
  height: 100vh;
}

.chat-topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--spacing-md) var(--spacing-lg);
  border-bottom: 1px solid var(--color-border-light);
  background: var(--color-surface);
}

.topbar-title {
  font-size: var(--text-base);
  font-weight: 600;
  color: var(--color-text-primary);
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: var(--spacing-lg);
}

.messages-inner {
  max-width: 800px;
  margin: 0 auto;
}

.chat-input-area {
  padding: var(--spacing-md) var(--spacing-lg) var(--spacing-lg);
  background: var(--color-bg);
}

.input-wrapper {
  max-width: 800px;
  margin: 0 auto;
  display: flex;
  align-items: flex-end;
  gap: var(--spacing-sm);
  background: var(--chat-input-bg);
  border: 1px solid var(--chat-input-border);
  border-radius: var(--radius-md);
  padding: var(--spacing-sm) var(--spacing-sm) var(--spacing-sm) var(--spacing-md);
  transition: border-color var(--transition-fast);
}

.input-wrapper:focus-within {
  border-color: var(--color-primary-light);
}

.chat-textarea {
  flex: 1;
  border: none;
  outline: none;
  resize: none;
  font-family: var(--font-sans);
  font-size: var(--text-sm);
  line-height: 1.5;
  color: var(--color-text-primary);
  background: transparent;
  max-height: 160px;
  padding: 6px 0;
}

.chat-textarea::placeholder {
  color: var(--color-text-tertiary);
}

.send-btn {
  width: 36px;
  height: 36px;
  border-radius: var(--radius-sm);
  border: none;
  background: var(--color-primary);
  color: white;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  transition: opacity var(--transition-fast);
}

.send-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.send-btn:not(:disabled):hover {
  opacity: 0.85;
}
</style>
