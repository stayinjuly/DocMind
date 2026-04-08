<script setup lang="ts">
import { useUserStore } from './stores/user'
import { useRouter } from 'vue-router'

const userStore = useUserStore()
const router = useRouter()

function logout() {
  userStore.logout()
  router.push('/login')
}
</script>

<template>
  <div class="app">
    <el-container v-if="userStore.isLoggedIn">
      <el-header class="app-header">
        <div class="logo">DocMind</div>
        <el-menu mode="horizontal" router :ellipsis="false" class="nav-menu">
          <el-menu-item index="/">对话问答</el-menu-item>
          <el-menu-item index="/documents">文档管理</el-menu-item>
        </el-menu>
        <div class="user-info">
          <span>{{ userStore.email }}</span>
          <el-button text @click="logout">退出</el-button>
        </div>
      </el-header>
      <el-main class="app-main">
        <router-view />
      </el-main>
    </el-container>

    <router-view v-else />
  </div>
</template>

<style scoped>
.app {
  min-height: 100vh;
  background: #f5f7fa;
}

.app-header {
  display: flex;
  align-items: center;
  background: white;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  padding: 0 24px;
  height: 60px;
}

.logo {
  font-size: 20px;
  font-weight: bold;
  color: #409eff;
  margin-right: 40px;
}

.nav-menu {
  flex: 1;
  border-bottom: none;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 12px;
  color: #606266;
}

.app-main {
  padding: 0;
  min-height: calc(100vh - 60px);
}
</style>
