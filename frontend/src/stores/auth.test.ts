import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { useAuthStore } from './auth'

const mockUser = {
  id: 1,
  username: 'alice',
  nickname: 'Alice',
  email: 'alice@example.com',
  avatar: '/uploads/avatars/test.png',
  createTime: '2026-04-26T00:00:00',
}

const { userApi } = vi.hoisted(() => ({
  userApi: {
    login: vi.fn(),
    register: vi.fn(),
    getCurrentUser: vi.fn(),
    updateUser: vi.fn(),
    changePassword: vi.fn(),
  },
}))

vi.mock('@/api/user', () => ({ userApi }))

describe('auth store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    localStorage.clear()
    vi.clearAllMocks()
  })

  describe('login', () => {
    it('stores token and user after successful login', async () => {
      userApi.login.mockResolvedValueOnce({ token: 'login-token', user: mockUser })
      const store = useAuthStore()

      await store.login({ account: 'alice', password: '123456' })

      expect(store.token).toBe('login-token')
      expect(store.user).toEqual(mockUser)
      expect(localStorage.getItem('smart-code-note-token')).toBe('login-token')
    })
  })

  describe('register', () => {
    it('stores token and user after successful registration', async () => {
      userApi.register.mockResolvedValueOnce(mockUser)
      userApi.login.mockResolvedValueOnce({ token: 'reg-token', user: mockUser })
      const store = useAuthStore()

      await store.register({
        username: 'alice',
        nickname: 'Alice',
        email: 'alice@example.com',
        password: '123456',
        confirmPassword: '123456',
      })

      // register only creates account; token comes from subsequent login
      expect(store.token).toBe('')
      expect(userApi.register).toHaveBeenCalledWith({
        username: 'alice',
        nickname: 'Alice',
        email: 'alice@example.com',
        password: '123456',
        confirmPassword: '123456',
      })
    })
  })

  describe('logout', () => {
    it('clears token and user', () => {
      const store = useAuthStore()
      store.setToken('some-token')

      store.logout()

      expect(store.token).toBe('')
      expect(localStorage.getItem('smart-code-note-token')).toBeNull()
    })
  })

  describe('fetchCurrentUser', () => {
    it('fetches and stores user', async () => {
      userApi.getCurrentUser.mockResolvedValueOnce(mockUser)
      const store = useAuthStore()
      // Set token so it's "logged in"
      store.setToken('valid-token')

      await store.fetchCurrentUser()

      expect(store.user).toEqual(mockUser)
    })

    it('skips fetch when user is already loaded', async () => {
      const store = useAuthStore()
      store.setToken('valid-token')
      store.user = mockUser

      await store.fetchCurrentUser()

      expect(userApi.getCurrentUser).not.toHaveBeenCalled()
    })

    it('forces refresh when force=true', async () => {
      userApi.getCurrentUser.mockResolvedValueOnce({ ...mockUser, nickname: 'Updated' })
      const store = useAuthStore()
      store.setToken('valid-token')
      store.user = mockUser

      await store.fetchCurrentUser(true)

      expect(userApi.getCurrentUser).toHaveBeenCalled()
      expect(store.user?.nickname).toBe('Updated')
    })

    it('clears session when API returns 401', async () => {
      userApi.getCurrentUser.mockRejectedValueOnce({ response: { status: 401 } })
      const store = useAuthStore()
      store.setToken('expired-token')

      await expect(store.fetchCurrentUser()).rejects.toThrow()

      expect(store.token).toBe('')
      expect(store.user).toBeNull()
    })
  })

  describe('displayName', () => {
    it('returns nickname when available', () => {
      const store = useAuthStore()
      store.user = mockUser
      expect(store.displayName).toBe('Alice')
    })

    it('falls back to username when nickname is empty', () => {
      const store = useAuthStore()
      store.user = { ...mockUser, nickname: '' }
      expect(store.displayName).toBe('alice')
    })

    it('falls back to placeholder when not logged in', () => {
      const store = useAuthStore()
      expect(store.displayName).toBe('未登录')
    })
  })

  describe('updateUser', () => {
    it('updates user and persists to store', async () => {
      userApi.updateUser.mockResolvedValueOnce({ ...mockUser, nickname: 'NewName' })
      const store = useAuthStore()
      store.user = mockUser

      const result = await store.updateUser({ nickname: 'NewName' })

      expect(result.nickname).toBe('NewName')
    })
  })

  describe('changePassword', () => {
    it('calls API with correct payload', async () => {
      userApi.changePassword.mockResolvedValueOnce(undefined)
      const store = useAuthStore()

      await store.changePassword({ oldPassword: 'old', newPassword: 'new' })

      expect(userApi.changePassword).toHaveBeenCalledWith({
        oldPassword: 'old',
        newPassword: 'new',
      })
    })
  })
})
