<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import { ElMessage } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()
const userId = ref('')

function handleLogin() {
  const id = userId.value.trim()
  if (!id) {
    ElMessage.warning('请输入用户 ID')
    return
  }

  userStore.setUserId(id)
  ElMessage.success('欢迎，' + id)
  router.push('/')
}
</script>

<template>
  <div class="login-view">
    <div class="login-card">
      <h1>DocMind</h1>
      <p class="subtitle">企业知识库智能问答系统</p>

      <el-input
        v-model="userId"
        placeholder="请输入用户 ID"
        size="large"
        @keyup.enter="handleLogin"
      />

      <el-button type="primary" size="large" @click="handleLogin" style="width: 100%">
        进入系统
      </el-button>
    </div>
  </div>
</template>

<style scoped>
.login-view {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.login-card {
  background: white;
  padding: 40px;
  border-radius: 12px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
  width: 360px;
  text-align: center;
}

.login-card h1 {
  margin: 0 0 8px;
  color: #303133;
  font-size: 32px;
}

.subtitle {
  color: #909399;
  margin-bottom: 32px;
}

.el-input {
  margin-bottom: 16px;
}
</style>
