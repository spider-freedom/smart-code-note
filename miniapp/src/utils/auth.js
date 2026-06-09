export function getToken() {
  return uni.getStorageSync('smart-code-note-token')
}

export function setToken(token) {
  uni.setStorageSync('smart-code-note-token', token)
}

export function removeToken() {
  uni.removeStorageSync('smart-code-note-token')
}

export function isLoggedIn() {
  return !!getToken()
}
