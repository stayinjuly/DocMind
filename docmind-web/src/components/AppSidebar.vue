<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import { useAppStore } from '../stores/app'
import {
  ChatDotRound,
  Document,
  Plus,
  Fold,
  Expand,
  SwitchButton,
} from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const appStore = useAppStore()

const collapsed = computed(() => appStore.sidebarCollapsed)
const userInitial = computed(() => userStore.email.charAt(0).toUpperCase())

function handleLogout() {
  userStore.logout()
  router.push('/login')
}
</script>

<template>
  <aside :class="['sidebar', { collapsed }]">
    <div class="sidebar-top">
      <div class="sidebar-logo">
        <img src="@/assets/logo.svg" alt="DocMind" class="logo-icon" />
        <span v-show="!collapsed" class="logo-text">DocMind</span>
      </div>

      <button class="new-chat-btn" @click="router.push('/')">
        <el-icon><Plus /></el-icon>
        <span v-show="!collapsed">新建对话</span>
      </button>

      <nav class="sidebar-nav">
        <router-link
          to="/"
          :class="['nav-item', { active: route.path === '/' }]"
        >
          <el-icon><ChatDotRound /></el-icon>
          <span v-show="!collapsed">对话问答</span>
        </router-link>
        <router-link
          to="/documents"
          :class="['nav-item', { active: route.path === '/documents' }]"
        >
          <el-icon><Document /></el-icon>
          <span v-show="!collapsed">文档管理</span>
        </router-link>
      </nav>
    </div>

    <div class="sidebar-bottom">
      <button class="collapse-btn" @click="appStore.toggleSidebar">
        <el-icon><Fold v-if="!collapsed" /><Expand v-else /></el-icon>
      </button>

      <div class="sidebar-user">
        <div class="user-avatar">{{ userInitial }}</div>
        <div v-show="!collapsed" class="user-details">
          <span class="user-email">{{ userStore.email }}</span>
          <button class="logout-btn" @click="handleLogout">
            <el-icon><SwitchButton /></el-icon>
            退出
          </button>
        </div>
      </div>
    </div>
  </aside>
</template>

<style scoped>
.sidebar {
  width: var(--sidebar-width);
  height: 100vh;
  background: var(--sidebar-bg);
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  transition: width var(--transition-normal);
  overflow: hidden;
  flex-shrink: 0;
}

.sidebar.collapsed {
  width: var(--sidebar-collapsed-width);
}

.sidebar-top {
  display: flex;
  flex-direction: column;
  padding: var(--spacing-md);
  gap: var(--spacing-md);
}

.sidebar-logo {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  padding: var(--spacing-sm) 0;
}

.logo-icon {
  width: 32px;
  height: 32px;
  border-radius: var(--radius-sm);
  flex-shrink: 0;
}

.logo-text {
  font-size: var(--text-lg);
  font-weight: 700;
  color: var(--color-text-inverse);
  white-space: nowrap;
}

.new-chat-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--spacing-sm);
  width: 100%;
  padding: 10px;
  border: 1px solid var(--sidebar-border);
  border-radius: var(--radius-sm);
  background: transparent;
  color: var(--sidebar-text);
  cursor: pointer;
  font-size: var(--text-sm);
  transition: all var(--transition-fast);
}

.new-chat-btn:hover {
  background: var(--sidebar-hover-bg);
  color: var(--sidebar-text-active);
}

.sidebar-nav {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-xs);
}

.nav-item {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  padding: 10px 12px;
  border-radius: var(--radius-sm);
  color: var(--sidebar-text);
  font-size: var(--text-sm);
  transition: all var(--transition-fast);
  text-decoration: none;
}

.nav-item:hover {
  background: var(--sidebar-hover-bg);
  color: var(--sidebar-text-active);
}

.nav-item.active {
  background: var(--sidebar-active-bg);
  color: var(--sidebar-text-active);
}

.sidebar-bottom {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-sm);
  padding: var(--spacing-md);
  border-top: 1px solid var(--sidebar-border);
}

.collapse-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  padding: 8px;
  border: none;
  border-radius: var(--radius-sm);
  background: transparent;
  color: var(--sidebar-text);
  cursor: pointer;
  transition: all var(--transition-fast);
}

.collapse-btn:hover {
  background: var(--sidebar-hover-bg);
  color: var(--sidebar-text-active);
}

.sidebar-user {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
}

.user-avatar {
  width: 32px;
  height: 32px;
  border-radius: var(--radius-full);
  background: var(--color-primary);
  color: var(--color-text-inverse);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: var(--text-sm);
  font-weight: 600;
  flex-shrink: 0;
}

.user-details {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex: 1;
  min-width: 0;
}

.user-email {
  font-size: var(--text-xs);
  color: var(--sidebar-text);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.logout-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  border: none;
  background: transparent;
  color: var(--sidebar-text);
  font-size: var(--text-xs);
  cursor: pointer;
  padding: 2px 4px;
  border-radius: var(--radius-sm);
  transition: all var(--transition-fast);
  white-space: nowrap;
}

.logout-btn:hover {
  color: var(--color-danger);
}
</style>
