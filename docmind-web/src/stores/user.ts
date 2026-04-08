import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || '')
  const email = ref(localStorage.getItem('email') || '')

  const isLoggedIn = computed(() => !!token.value)

  // 向后兼容：现有代码中引用 userStore.userId 的地方会自动解析为 email
  const userId = computed(() => email.value)

  function setAuth(newToken: string, newEmail: string) {
    token.value = newToken
    email.value = newEmail
    localStorage.setItem('token', newToken)
    localStorage.setItem('email', newEmail)
  }

  function logout() {
    token.value = ''
    email.value = ''
    localStorage.removeItem('token')
    localStorage.removeItem('email')
  }

  return { token, email, isLoggedIn, userId, setAuth, logout }
})
