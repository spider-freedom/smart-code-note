import request from '@/utils/request'
import type { ApiResult } from '@/types/api'
import type { LearningOverview, LearningSuggestion, WeakKnowledge } from '@/types/report'

export const reportApi = {
  async getOverview() {
    const response = await request.get<ApiResult<LearningOverview>>('/reports/overview')
    return response.data.data
  },

  async getWeakKnowledge(limit = 5) {
    const response = await request.get<ApiResult<WeakKnowledge[]>>('/reports/weak-knowledge', {
      params: { limit },
    })
    return response.data.data
  },

  async getSuggestions() {
    const response = await request.get<ApiResult<LearningSuggestion>>('/reports/suggestions')
    return response.data.data
  },
}
