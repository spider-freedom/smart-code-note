import request from '@/utils/request'
import type { ApiResult } from '@/types/api'
import type {
  ChangePasswordRequest,
  LoginRequest,
  LoginResponse,
  RegisterRequest,
  UpdateUserRequest,
  UserInfo,
} from '@/types/user'

export const userApi = {
  async login(data: LoginRequest) {
    const response = await request.post<ApiResult<LoginResponse>>('/user/login', data)
    return response.data.data
  },

  async register(data: RegisterRequest) {
    const response = await request.post<ApiResult<UserInfo>>('/user/register', data)
    return response.data.data
  },

  async getCurrentUser() {
    const response = await request.get<ApiResult<UserInfo>>('/user/info')
    return response.data.data
  },

  async updateUser(data: UpdateUserRequest) {
    const response = await request.put<ApiResult<UserInfo>>('/user/update', data)
    return response.data.data
  },

  async uploadAvatar(file: File) {
    const formData = new FormData()
    formData.append('file', file)
    const response = await request.post<ApiResult<UserInfo>>('/user/avatar', formData)
    return response.data.data
  },

  async changePassword(data: ChangePasswordRequest) {
    await request.put<ApiResult<null>>('/user/password', data)
  },
}
