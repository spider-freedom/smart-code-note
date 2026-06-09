import request from './request'

export const chatApi = {
  async listSessions() {
    return request({ url: '/chat/sessions' })
  },

  async getSession(id) {
    return request({ url: `/chat/sessions/${id}` })
  },

  async deleteSession(id) {
    return request({ url: `/chat/sessions/${id}`, method: 'DELETE' })
  },

  async getLearningContext() {
    return request({ url: '/chat/context' })
  }
}
