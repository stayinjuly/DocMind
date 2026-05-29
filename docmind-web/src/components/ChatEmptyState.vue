<script setup lang="ts">
const emit = defineEmits<{
  select: [value: string]
}>()

const suggestions = [
  '帮我总结上传的文档',
  '这些文档讲了什么内容',
  '对比文档中的关键观点',
  '列出文档中的重要数据',
]
</script>

<template>
  <div class="empty-state">
    <div class="empty-icon">
      <svg viewBox="0 0 48 48" width="64" height="64" fill="none">
        <rect width="48" height="48" rx="14" fill="url(#g)" />
        <path d="M14 16h20M14 24h14M14 32h18" stroke="white" stroke-width="2.5" stroke-linecap="round" />
        <defs>
          <linearGradient id="g" x1="0" y1="0" x2="48" y2="48" gradientUnits="userSpaceOnUse">
            <stop stop-color="#667eea" />
            <stop offset="1" stop-color="#764ba2" />
          </linearGradient>
        </defs>
      </svg>
    </div>
    <h2>你好！我是 DocMind 助手</h2>
    <p class="empty-desc">基于你的文档，我可以回答问题、总结内容、分析数据</p>
    <div class="suggestions">
      <button
        v-for="s in suggestions"
        :key="s"
        class="suggestion-chip"
        @click="emit('select', s)"
      >
        {{ s }}
      </button>
    </div>
  </div>
</template>

<style scoped>
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  gap: var(--spacing-md);
  padding: var(--spacing-xl);
}

.empty-icon {
  margin-bottom: var(--spacing-sm);
}

.empty-state h2 {
  font-size: var(--text-xl);
  font-weight: 600;
  color: var(--color-text-primary);
}

.empty-desc {
  color: var(--color-text-secondary);
  font-size: var(--text-sm);
  margin-bottom: var(--spacing-lg);
}

.suggestions {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: var(--spacing-sm);
  max-width: 520px;
}

.suggestion-chip {
  padding: 10px 18px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-full);
  background: var(--color-surface);
  color: var(--color-text-secondary);
  font-size: var(--text-sm);
  cursor: pointer;
  transition: all var(--transition-fast);
}

.suggestion-chip:hover {
  border-color: var(--color-primary-light);
  color: var(--color-primary);
  background: rgba(108, 92, 231, 0.04);
}
</style>
