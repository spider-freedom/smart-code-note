export interface UserInfo {
  id: number
  username: string
  nickname: string | null
  email: string | null
  avatar: string | null
  createTime: string
}

export interface LoginRequest {
  account: string
  password: string
}

export interface LoginResponse {
  token: string
  user: UserInfo
}

export interface RegisterRequest {
  username: string
  password: string
  confirmPassword: string
  email?: string
  nickname?: string
}

export interface UpdateUserRequest {
  nickname?: string
  email?: string
  avatar?: string
}

export interface ChangePasswordRequest {
  oldPassword: string
  newPassword: string
}
