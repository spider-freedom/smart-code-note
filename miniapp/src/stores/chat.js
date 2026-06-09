import { defineStore } from 'pinia'
import { ref, reactive } from 'vue'
import websocket from '@/utils/websocket'

export const useChatStore = defineStore('chat', () => {
  const visible = ref(false)
  const messages = ref([])
  const sessions = ref([])
  const currentSessionId = ref(null)
  const streaming = ref(false)
  const streamingContent = ref('')

  function toggle() {
    visible.value = !visible.value
  }

  function open() {
    visible.value = true
  }

  function close() {
    visible.value = false
  }

  function addUserMessage(text) {
    messages.value.push({ role: 'user', content: text, createTime: new Date().toISOString() })
  }

  function startStream() {
    streaming.value = true
    streamingContent.value = ''
  }

  function appendChunk(chunk) {
    streamingContent.value += chunk
  }

  function finishStream(result) {
    messages.value.push({
      id: result.id,
      sessionId: result.sessionId,
      role: 'assistant',
      content: result.content,
      createTime: result.createTime
    })
    if (result.sessionId && !currentSessionId.value) {
      currentSessionId.value = result.sessionId
      loadSessions()
    }
    streaming.value = false
    streamingContent.value = ''
  }

  function handleError(err) {
    streaming.value = false
    streamingContent.value = ''
    uni.showToast({ title: typeof err === 'string' ? err : '对话失败', icon: 'none' })
  }

  function sendMessage(text) {
    if (!text.trim()) return
    addUserMessage(text)
    startStream()
    websocket.send(text, currentSessionId.value)
  }

  async function loadSessions() {
    try {
      const { default: request } = await import('@/api/request')
      sessions.value = await request({ url: '/chat/sessions' })
    } catch (e) {
      // ignore
    }
  }

  async function loadSession(sessionId) {
    try {
      const { default: request } = await import('@/api/request')
      const data = await request({ url: `/chat/sessions/${sessionId}` })
      currentSessionId.value = sessionId
      messages.value = data.messages || []
    } catch (e) {
      // ignore
    }
  }

  async function deleteSession(sessionId) {
    try {
      const { default: request } = await import('@/api/request')
      await request({ url: `/chat/sessions/${sessionId}`, method: 'DELETE' })
      sessions.value = sessions.value.filter(s => s.id !== sessionId)
      if (currentSessionId.value === sessionId) {
        currentSessionId.value = null
        messages.value = []
      }
    } catch (e) {
      // ignore
    }
  }

  function clearMessages() {
    messages.value = []
    currentSessionId.value = null
    streaming.value = false
    streamingContent.value = ''
  }

  return {
    visible, messages, sessions, currentSessionId, streaming, streamingContent,
    toggle, open, close, sendMessage, loadSessions, loadSession, deleteSession, clearMessages,
    appendChunk, finishStream, handleError, addUserMessage, startStream
  }
})
