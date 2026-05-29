<script setup lang="ts">
import { computed } from 'vue'
import { useAppStore } from '../stores/app'
import AppSidebar from '../components/AppSidebar.vue'

const appStore = useAppStore()
const collapsed = computed(() => appStore.sidebarCollapsed)

function toggleMobile() {
  appStore.toggleSidebar()
}
</script>

<template>
  <div class="main-layout">
    <div v-if="!collapsed" class="sidebar-overlay mobile-only" @click="toggleMobile" />
    <AppSidebar />
    <main :class="['main-content', { collapsed }]">
      <div class="mobile-topbar">
        <button class="hamburger" @click="toggleMobile">
          <svg width="20" height="20" viewBox="0 0 20 20" fill="currentColor">
            <rect y="3" width="20" height="2" rx="1" />
            <rect y="9" width="20" height="2" rx="1" />
            <rect y="15" width="20" height="2" rx="1" />
          </svg>
        </button>
      </div>
      <router-view v-slot="{ Component }">
        <transition name="fade" mode="out-in">
          <component :is="Component" />
        </transition>
      </router-view>
    </main>
  </div>
</template>

<style scoped>
.main-layout {
  display: flex;
  height: 100vh;
  overflow: hidden;
}

.main-content {
  flex: 1;
  overflow-y: auto;
  background: var(--color-bg);
  transition: margin var(--transition-normal);
}

.mobile-topbar {
  display: none;
  align-items: center;
  padding: var(--spacing-sm) var(--spacing-md);
  background: var(--color-surface);
  border-bottom: 1px solid var(--color-border-light);
}

.hamburger {
  border: none;
  background: transparent;
  cursor: pointer;
  padding: var(--spacing-sm);
  color: var(--color-text-primary);
  border-radius: var(--radius-sm);
}

.hamburger:hover {
  background: var(--color-surface-hover);
}

.mobile-only {
  display: none;
}

@media (max-width: 768px) {
  .mobile-topbar {
    display: flex !important;
  }

  .mobile-only {
    display: block !important;
  }
}
</style>
