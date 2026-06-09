import request from '@/utils/request'
import { createSSEStream } from '@/utils/sse'
import type { ApiResult } from '@/types/api'
import type { ChatLearningContext, ChatMessage, ChatSession } from '@/types/chat'

export const chatApi = {
  async listSessions() {
    const response = await request.get<ApiResult<ChatSession[]>>('/chat/sessions')
    return response.data.data
  },

  async getSession(id: number) {
    const response = await request.get<ApiResult<ChatSession>>(`/chat/sessions/${id}`)
    return response.data.data
  },

  async deleteSession(id: number) {
    await request.delete<ApiResult<null>>(`/chat/sessions/${id}`)
  },

  async getLearningContext() {
    const response = await request.get<ApiResult<ChatLearningContext>>('/chat/context')
    return response.data.data
  },

  sendMessage(
    message: string,
    sessionId: number | null,
    onChunk: (text: string) => void,
    onResult: (msg: ChatMessage) => void,
    onError: (msg: string) => void,
  ): AbortController {
    const params: Record<string, unknown> = { message }
    if (sessionId) params.sessionId = sessionId
    return createSSEStream('/api/chat/send', params, onChunk, onResult, onError)
  },
}
