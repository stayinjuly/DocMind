<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import { authApi } from '../api'
import { ElMessage } from 'element-plus'
import { Message, Lock } from '@element-plus/icons-vue'
import logoUrl from '@/assets/logo.svg'

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
  <div class="login-card">
    <img :src="logoUrl" alt="DocMind" class="login-logo" />
    <h1>DocMind</h1>
    <p class="subtitle">企业知识库智能问答系统</p>

    <el-input
      v-model="email"
      placeholder="请输入邮箱"
      size="large"
      type="email"
      :prefix-icon="Message"
      @keyup.enter="handleSubmit"
    />

    <el-input
      v-model="password"
      type="password"
      placeholder="请输入密码"
      size="large"
      show-password
      :prefix-icon="Lock"
      @keyup.enter="handleSubmit"
    />

    <el-button
      size="large"
      :loading="loading"
      class="submit-btn"
      @click="handleSubmit"
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
</template>

<style scoped>
.login-card {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(20px);
  padding: 48px 40px;
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-lg);
  width: 400px;
  text-align: center;
}

.login-logo {
  width: 56px;
  height: 56px;
  margin-bottom: var(--spacing-md);
}

.login-card h1 {
  margin: 0 0 var(--spacing-xs);
  color: var(--color-primary);
  font-size: var(--text-2xl);
  font-weight: 700;
}

.subtitle {
  color: var(--color-text-tertiary);
  margin-bottom: var(--spacing-xl);
  font-size: var(--text-sm);
}

.el-input {
  margin-bottom: var(--spacing-md);
}

.submit-btn {
  width: 100%;
  background: var(--color-primary-gradient);
  border: none;
  color: var(--color-text-inverse);
  font-size: var(--text-base);
  border-radius: var(--radius-sm);
  height: 44px;
}

.submit-btn:hover {
  opacity: 0.9;
}

.toggle-text {
  margin-top: var(--spacing-lg);
  color: var(--color-text-tertiary);
  font-size: var(--text-sm);
}

.toggle-text a {
  color: var(--color-primary);
  cursor: pointer;
}

.toggle-text a:hover {
  text-decoration: underline;
}
</style>
