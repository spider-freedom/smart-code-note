const BASE_URL = 'http://localhost:8080/api'

const request = (options) => {
  const token = uni.getStorageSync('smart-code-note-token')
  return new Promise((resolve, reject) => {
    uni.request({
      url: BASE_URL + options.url,
      method: options.method || 'GET',
      data: options.data,
      header: {
        'Content-Type': 'application/json',
        'Authorization': token ? `Bearer ${token}` : '',
        ...options.header
      },
      success: (res) => {
        if (res.statusCode === 401) {
          uni.removeStorageSync('smart-code-note-token')
          uni.reLaunch({ url: '/pages/login/login' })
          reject(new Error('unauthorized'))
          return
        }
        if (res.data.code === 0) {
          resolve(res.data.data)
        } else {
          uni.showToast({ title: res.data.message || '请求失败', icon: 'none' })
          reject(res.data)
        }
      },
      fail: (err) => {
        uni.showToast({ title: '网络错误', icon: 'none' })
        reject(err)
      }
    })
  })
}

export default request
