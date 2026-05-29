<script setup lang="ts">
import type { ChatMessage } from '../api/types'
import MarkdownRenderer from './MarkdownRenderer.vue'

defineProps<{
  message: ChatMessage
  isStreaming: boolean
}>()
</script>

<template>
  <div :class="['chat-message', message.role]">
    <div v-if="message.role === 'assistant'" class="avatar assistant-avatar">
      <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2">
        <path d="M12 2a4 4 0 0 1 4 4v2a4 4 0 0 1-8 0V6a4 4 0 0 1 4-4z"/>
        <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/>
      </svg>
    </div>
    <div class="bubble">
      <template v-if="message.role === 'user'">
        {{ message.content }}
      </template>
      <template v-else-if="message.content">
        <MarkdownRenderer :content="message.content" />
      </template>
      <span v-if="isStreaming && !message.content" class="typing-indicator">
        <span /><span /><span />
      </span>
    </div>
  </div>
</template>

<style scoped>
.chat-message {
  display: flex;
  gap: var(--spacing-sm);
  margin-bottom: var(--spacing-lg);
  max-width: 85%;
}

.chat-message.user {
  margin-left: auto;
  flex-direction: row-reverse;
}

.chat-message.assistant {
  margin-right: auto;
}

.avatar {
  width: 32px;
  height: 32px;
  border-radius: var(--radius-full);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.assistant-avatar {
  background: var(--color-primary);
  color: white;
}

.bubble {
  padding: 12px 16px;
  border-radius: var(--radius-md);
  line-height: 1.6;
  word-break: break-word;
}

.chat-message.user .bubble {
  background: var(--chat-user-bubble);
  color: var(--chat-user-text);
  border-bottom-right-radius: var(--radius-xs, 4px);
}

.chat-message.assistant .bubble {
  background: var(--chat-assistant-bubble);
  color: var(--chat-assistant-text);
  border-bottom-left-radius: var(--radius-xs, 4px);
  min-width: 40px;
}

.typing-indicator {
  display: inline-flex;
  gap: 4px;
  padding: 4px 0;
}

.typing-indicator span {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--color-text-tertiary);
  animation: typing-bounce 1.4s infinite ease-in-out both;
}

.typing-indicator span:nth-child(1) { animation-delay: 0s; }
.typing-indicator span:nth-child(2) { animation-delay: 0.16s; }
.typing-indicator span:nth-child(3) { animation-delay: 0.32s; }

@keyframes typing-bounce {
  0%, 80%, 100% { transform: scale(0.6); opacity: 0.4; }
  40% { transform: scale(1); opacity: 1; }
}
</style>
