import request from './request'

export const reportApi = {
  async getOverview() {
    return request({ url: '/reports/overview' })
  },

  async getWeakKnowledge(limit = 5) {
    return request({ url: '/reports/weak-knowledge', data: { limit } })
  },

  async getSuggestions() {
    return request({ url: '/reports/suggestions' })
  }
}
