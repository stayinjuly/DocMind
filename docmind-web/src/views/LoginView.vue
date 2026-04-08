<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import { authApi } from '../api'
import { ElMessage } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()

const isLogin = ref(true)
const email = ref('')
const password = ref('')
const loading = ref(false)

async function handleSubmit() {
  if (!email.value.trim() || !password.value) {
    ElMessage.warning('请填写邮箱和密码')
    return
  }

  loading.value = true
  try {
    const data: { email: string; password: string } = {
      email: email.value.trim(),
      password: password.value,
    }

    if (isLogin.value) {
      const response = await authApi.login(data)
      userStore.setAuth(response.data.token, response.data.email)
      ElMessage.success('登录成功')
      router.push('/')
    } else {
      await authApi.register(data)
      ElMessage.success('注册成功，请登录')
      isLogin.value = true
      password.value = ''
    }
  } catch (error: any) {
    const msg = error.response?.data?.error || (isLogin.value ? '登录失败' : '注册失败')
    ElMessage.error(msg)
  } finally {
    loading.value = false
  }
}

function toggleMode() {
  isLogin.value = !isLogin.value
}
</script>

<template>
  <div class="login-view">
    <div class="login-card">
      <h1>DocMind</h1>
      <p class="subtitle">企业知识库智能问答系统</p>

      <el-input
        v-model="email"
        placeholder="请输入邮箱"
        size="large"
        type="email"
        @keyup.enter="handleSubmit"
      />

      <el-input
        v-model="password"
        type="password"
        placeholder="请输入密码"
        size="large"
        show-password
        @keyup.enter="handleSubmit"
      />

      <el-button
        type="primary"
        size="large"
        @click="handleSubmit"
        :loading="loading"
        style="width: 100%"
      >
        {{ isLogin ? '登录' : '注册' }}
      </el-button>

      <p class="toggle-text">
        {{ isLogin ? '还没有账号？' : '已有账号？' }}
        <a href="#" @click.prevent="toggleMode">
          {{ isLogin ? '立即注册' : '立即登录' }}
        </a>
      </p>
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

.toggle-text {
  margin-top: 16px;
  color: #909399;
  font-size: 14px;
}

.toggle-text a {
  color: #409eff;
  text-decoration: none;
}

.toggle-text a:hover {
  text-decoration: underline;
}
</style>
