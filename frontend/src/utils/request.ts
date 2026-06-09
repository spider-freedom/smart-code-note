import axios, { AxiosError } from 'axios'
import { ElMessage } from 'element-plus'
import { TOKEN_KEY } from '@/constants'
import type { ApiResult } from '@/types/api'

const request = axios.create({
  baseURL: '/api',
  timeout: 15000,
})

request.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEY)

  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }

  return config
})

request.interceptors.response.use(
  (response) => {
    const result = response.data as ApiResult<unknown>

    if (typeof result?.code === 'number' && result.code !== 0) {
      const message = result.message || '请求失败'
      ElMessage.error(message)
      return Promise.reject(new Error(message))
    }

    return response
  },
  (error: AxiosError<ApiResult<unknown>>) => {
    const status = error.response?.status
    const message = error.response?.data?.message || error.message || '网络请求失败'

    if (status === 401) {
      localStorage.removeItem(TOKEN_KEY)
    }

    ElMessage.error(message)
    return Promise.reject(error)
  },
)

export default request
