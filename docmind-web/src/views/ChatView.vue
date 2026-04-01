<script setup lang="ts">
import { ref, nextTick } from 'vue'
import { useUserStore } from '../stores/user'
import { createChatStream, qaApi } from '../api'
import { ElMessage } from 'element-plus'
import type { ChatMessage } from '../api/types'

const userStore = useUserStore()
const messages = ref<ChatMessage[]>([])
const inputMessage = ref('')
const loading = ref(false)
const chatContainer = ref<HTMLElement | null>(null)

async function sendMessage() {
  const question = inputMessage.value.trim()
  if (!question || loading.value) return

  loading.value = true  // Set loading immediately to prevent race condition
  messages.value.push({ role: 'user', content: question })
  inputMessage.value = ''

  // Add placeholder for assistant response
  messages.value.push({ role: 'assistant', content: '' })
  const assistantIndex = messages.value.length - 1

  scrollToBottom()

  const eventSource = createChatStream(question, userStore.userId)

  eventSource.onmessage = (event) => {
    if (event.data === '[DONE]') {
      eventSource.close()
      loading.value = false
      return
    }
    messages.value[assistantIndex].content += event.data
    scrollToBottom()
  }

  eventSource.onerror = () => {
    eventSource.close()
    loading.value = false
    if (!messages.value[assistantIndex].content) {
      messages.value[assistantIndex].content = '抱歉，发生了错误，请重试。'
    }
  }
}

async function clearHistory() {
  try {
    await qaApi.clearHistory(userStore.userId)
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
</script>

<template>
  <div class="chat-view">
    <div class="chat-header">
      <h2>DocMind 知识库问答</h2>
      <el-button type="danger" size="small" @click="clearHistory">清除对话</el-button>
    </div>

    <div class="chat-container" ref="chatContainer">
      <div v-if="messages.length === 0" class="empty-state">
        <p>请输入问题，我将基于知识库为您解答</p>
      </div>

      <div
        v-for="(msg, index) in messages"
        :key="index"
        :class="['message', msg.role]"
      >
        <div class="message-content">
          {{ msg.content }}
          <span v-if="loading && index === messages.length - 1 && msg.role === 'assistant' && !msg.content" class="typing">
            正在思考...
          </span>
        </div>
      </div>
    </div>

    <div class="chat-input">
      <el-input
        v-model="inputMessage"
        type="textarea"
        :rows="2"
        placeholder="输入您的问题..."
        @keydown="handleKeydown"
        :disabled="loading"
      />
      <el-button type="primary" @click="sendMessage" :loading="loading">
        发送
      </el-button>
    </div>
  </div>
</template>

<style scoped>
.chat-view {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 120px);
  max-width: 900px;
  margin: 0 auto;
  padding: 20px;
}

.chat-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.chat-header h2 {
  margin: 0;
  color: #303133;
}

.chat-container {
  flex: 1;
  overflow-y: auto;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  padding: 20px;
  background: #fafafa;
  margin-bottom: 20px;
}

.empty-state {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: #909399;
}

.message {
  margin-bottom: 16px;
  max-width: 80%;
}

.message.user {
  margin-left: auto;
  text-align: right;
}

.message.user .message-content {
  background: #409eff;
  color: white;
}

.message.assistant .message-content {
  background: white;
  color: #303133;
  text-align: left;
}

.message-content {
  padding: 12px 16px;
  border-radius: 8px;
  display: inline-block;
  word-break: break-word;
  white-space: pre-wrap;
}

.typing {
  color: #909399;
  animation: blink 1s infinite;
}

@keyframes blink {
  0%, 50% { opacity: 1; }
  51%, 100% { opacity: 0.3; }
}

.chat-input {
  display: flex;
  gap: 12px;
  align-items: flex-end;
}

.chat-input .el-textarea {
  flex: 1;
}
</style>
