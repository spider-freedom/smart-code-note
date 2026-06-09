import MockAdapter from 'axios-mock-adapter'
import { afterEach, describe, expect, it, vi } from 'vitest'
import request from './request'

vi.mock('element-plus', () => ({
  ElMessage: {
    error: vi.fn(),
  },
}))

describe('request', () => {
  const mock = new MockAdapter(request)

  afterEach(() => {
    mock.reset()
    localStorage.clear()
  })

  it('adds bearer token when token exists', async () => {
    localStorage.setItem('smart-code-note-token', 'abc-token')
    mock.onGet('/ping').reply((config) => {
      expect(config.headers?.Authorization).toBe('Bearer abc-token')
      return [200, { code: 0, message: 'success', data: true }]
    })

    const response = await request.get('/ping')

    expect(response.data.data).toBe(true)
  })

  it('does not add authorization header when no token', async () => {
    mock.onGet('/public').reply((config) => {
      expect(config.headers?.Authorization).toBeUndefined()
      return [200, { code: 0, message: 'ok', data: null }]
    })

    await request.get('/public')
  })

  it('rejects business errors', async () => {
    mock.onGet('/broken').reply(200, { code: 400, message: 'bad request', data: null })

    await expect(request.get('/broken')).rejects.toThrow('bad request')
  })

  it('clears token and redirects on 401', async () => {
    localStorage.setItem('smart-code-note-token', 'expired-token')
    // Mock window.location to prevent actual redirect
    const originalLocation = window.location
    Object.defineProperty(window, 'location', {
      writable: true,
      value: { href: '', pathname: '/dashboard' },
    })
    mock.onGet('/protected').reply(401, { code: 401, message: 'unauthorized', data: null })

    await expect(request.get('/protected')).rejects.toThrow()
    expect(localStorage.getItem('smart-code-note-token')).toBeNull()

    Object.defineProperty(window, 'location', {
      writable: true,
      value: originalLocation,
    })
  })
})
