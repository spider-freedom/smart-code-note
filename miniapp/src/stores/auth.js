import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { getToken, setToken, removeToken } from '@/utils/auth'

export const useAuthStore = defineStore('auth', () => {
  const userInfo = ref(null)
  const token = ref(getToken() || '')

  const isLoggedIn = computed(() => !!token.value)

  function setAuth(authToken, user) {
    token.value = authToken
    userInfo.value = user
    setToken(authToken)
  }

  function logout() {
    token.value = ''
    userInfo.value = null
    removeToken()
    uni.reLaunch({ url: '/pages/login/login' })
  }

  return { userInfo, token, isLoggedIn, setAuth, logout }
})
