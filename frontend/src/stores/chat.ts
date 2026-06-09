import { defineStore } from 'pinia'
import { ref } from 'vue'
import { chatApi } from '@/api/chat'
import type { ChatMessage, ChatSession } from '@/types/chat'

export const useChatStore = defineStore('chat', () => {
  const sessions = ref<ChatSession[]>([])
  const currentSessionId = ref<number | null>(null)
  const messages = ref<ChatMessage[]>([])
  const isOpen = ref(false)
  const isStreaming = ref(false)
  let abortController: AbortController | null = null

  async function loadSessions() {
    try {
      sessions.value = await chatApi.listSessions()
    } catch {
      // Silently fail - sessions are not critical
    }
  }

  async function loadSession(id: number) {
    try {
      const session = await chatApi.getSession(id)
      currentSessionId.value = session.id
      messages.value = session.messages || []
    } catch {
      // Failed to load session
    }
  }

  function newSession() {
    currentSessionId.value = null
    messages.value = []
  }

  async function sendMessage(content: string) {
    if (!content.trim() || isStreaming.value) return

    const userMsg: ChatMessage = {
      id: Date.now(),
      sessionId: currentSessionId.value || 0,
      role: 'user',
      content: content.trim(),
      createTime: new Date().toISOString(),
    }
    messages.value.push(userMsg)

    // Add a placeholder for the assistant reply
    const assistantMsg: ChatMessage = {
      id: Date.now() + 1,
      sessionId: currentSessionId.value || 0,
      role: 'assistant',
      content: '',
      createTime: new Date().toISOString(),
    }
    messages.value.push(assistantMsg)

    isStreaming.value = true

    abortController = chatApi.sendMessage(
      content.trim(),
      currentSessionId.value,
      (chunk) => {
        assistantMsg.content += chunk
      },
      (result) => {
        // Replace placeholder with real message
        assistantMsg.id = result.id
        assistantMsg.sessionId = result.sessionId
        currentSessionId.value = result.sessionId
        isStreaming.value = false
        loadSessions()
      },
      (error) => {
        assistantMsg.content = '抱歉，消息发送失败了：' + error
        isStreaming.value = false
      },
    )
  }

  function cancelStreaming() {
    abortController?.abort()
    isStreaming.value = false
  }

  function toggleOpen() {
    isOpen.value = !isOpen.value
  }

  async function deleteSessionAndRefresh(id: number) {
    await chatApi.deleteSession(id)
    if (currentSessionId.value === id) {
      newSession()
    }
    await loadSessions()
  }

  return {
    sessions,
    currentSessionId,
    messages,
    isOpen,
    isStreaming,
    loadSessions,
    loadSession,
    newSession,
    sendMessage,
    cancelStreaming,
    toggleOpen,
    deleteSessionAndRefresh,
  }
})
