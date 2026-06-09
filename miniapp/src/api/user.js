import request from './request'

export const userApi = {
  async wxLogin(code) {
    return request({ url: '/user/wx-login', method: 'POST', data: { code } })
  },

  async register(data) {
    return request({ url: '/user/register', method: 'POST', data })
  },

  async login(data) {
    return request({ url: '/user/login', method: 'POST', data })
  },

  async getCurrentUser() {
    return request({ url: '/user/info' })
  },

  async updateUser(data) {
    return request({ url: '/user/update', method: 'PUT', data })
  },

  async uploadAvatar(filePath) {
    const token = uni.getStorageSync('smart-code-note-token')
    return new Promise((resolve, reject) => {
      uni.uploadFile({
        url: 'http://localhost:8080/api/user/avatar',
        filePath,
        name: 'file',
        header: { 'Authorization': token ? `Bearer ${token}` : '' },
        success: (res) => {
          try {
            const data = JSON.parse(res.data)
            if (data.code === 0) resolve(data.data)
            else reject(data)
          } catch (e) {
            reject(new Error('解析响应失败'))
          }
        },
        fail: reject
      })
    })
  },

  async changePassword(data) {
    return request({ url: '/user/password', method: 'PUT', data })
  }
}
