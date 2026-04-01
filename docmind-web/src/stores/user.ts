import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useUserStore = defineStore('user', () => {
  const userId = ref(localStorage.getItem('userId') || '')

  function setUserId(id: string) {
    userId.value = id
    localStorage.setItem('userId', id)
  }

  function isLoggedIn() {
    return userId.value.trim().length > 0
  }

  return { userId, setUserId, isLoggedIn }
})
