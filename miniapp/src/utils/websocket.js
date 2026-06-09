const WS_BASE_URL = 'ws://localhost:8080/ws/chat'

class ChatWebSocket {
  constructor() {
    this.socket = null
    this.handlers = { chunk: [], result: [], error: [], open: [], close: [] }
    this.reconnectTimer = null
    this.connected = false
  }

  connect() {
    const token = uni.getStorageSync('smart-code-note-token')
    if (!token) return

    this.socket = uni.connectSocket({
      url: `${WS_BASE_URL}?token=${token}`,
      complete: () => {}
    })

    this.socket.onOpen(() => {
      this.connected = true
      this.handlers.open.forEach(fn => fn())
    })

    this.socket.onMessage((res) => {
      try {
        const { type, content } = JSON.parse(res.data)
        if (this.handlers[type]) {
          this.handlers[type].forEach(fn => fn(content))
        }
      } catch (e) {
        // ignore parse errors
      }
    })

    this.socket.onClose(() => {
      this.connected = false
      this.handlers.close.forEach(fn => fn())
    })

    this.socket.onError((err) => {
      this.connected = false
      this.handlers.error.forEach(fn => fn(err))
    })
  }

  on(event, fn) {
    if (this.handlers[event]) {
      this.handlers[event].push(fn)
    }
  }

  off(event, fn) {
    if (this.handlers[event]) {
      this.handlers[event] = this.handlers[event].filter(f => f !== fn)
    }
  }

  send(message, sessionId) {
    if (!this.connected) {
      uni.showToast({ title: '连接已断开', icon: 'none' })
      return
    }
    this.socket.send({
      data: JSON.stringify({ message, sessionId: sessionId || null })
    })
  }

  close() {
    this.connected = false
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer)
    }
    if (this.socket) {
      this.socket.close()
    }
  }
}

export default new ChatWebSocket()
