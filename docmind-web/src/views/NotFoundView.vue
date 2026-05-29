<template>
  <div class="not-found">
    <h1>404</h1>
    <p>页面不存在</p>
    <router-link to="/" class="back-link">返回首页</router-link>
  </div>
</template>

<style scoped>
.not-found {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100vh;
  gap: var(--spacing-md);
}

.not-found h1 {
  font-size: 96px;
  font-weight: 700;
  color: var(--color-primary);
  line-height: 1;
}

.not-found p {
  font-size: var(--text-lg);
  color: var(--color-text-secondary);
}

.back-link {
  margin-top: var(--spacing-md);
  padding: 10px 24px;
  background: var(--color-primary);
  color: var(--color-text-inverse);
  border-radius: var(--radius-sm);
  transition: opacity var(--transition-fast);
}

.back-link:hover {
  opacity: 0.9;
}
</style>
