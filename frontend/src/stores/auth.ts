import { defineStore } from 'pinia'
import { userApi } from '@/api/user'
import { TOKEN_KEY } from '@/constants'
import type {
  ChangePasswordRequest,
  LoginRequest,
  RegisterRequest,
  UpdateUserRequest,
  UserInfo,
} from '@/types/user'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem(TOKEN_KEY) || '',
    user: null as UserInfo | null,
    initialized: false,
  }),

  getters: {
    isLoggedIn: (state) => Boolean(state.token),
    displayName: (state) => state.user?.nickname || state.user?.username || '未登录',
  },

  actions: {
    setToken(token: string) {
      this.token = token
      localStorage.setItem(TOKEN_KEY, token)
    },

    clearSession() {
      this.token = ''
      this.user = null
      this.initialized = false
      localStorage.removeItem(TOKEN_KEY)
    },

    async login(data: LoginRequest) {
      const result = await userApi.login(data)
      this.setToken(result.token)
      this.user = result.user
      this.initialized = true
      return result
    },

    async register(data: RegisterRequest) {
      return userApi.register(data)
    },

    async fetchCurrentUser(force = false) {
      if (!this.token) {
        this.initialized = true
        return null
      }

      if (this.user && !force) {
        this.initialized = true
        return this.user
      }

      try {
        this.user = await userApi.getCurrentUser()
        this.initialized = true
        return this.user
      } catch (error) {
        this.clearSession()
        throw error
      }
    },

    async updateUser(data: UpdateUserRequest) {
      this.user = await userApi.updateUser(data)
      return this.user
    },

    async changePassword(data: ChangePasswordRequest) {
      await userApi.changePassword(data)
    },

    logout() {
      this.clearSession()
    },
  },
})
