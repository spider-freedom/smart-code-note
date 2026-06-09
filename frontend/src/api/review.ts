import request from '@/utils/request'
import type { ApiResult } from '@/types/api'
import type { ReviewResult, ReviewTask, SubmitReviewResultRequest } from '@/types/review'

export const reviewApi = {
  async getTodayTasks() {
    const response = await request.get<ApiResult<ReviewTask[]>>('/reviews/today')
    return response.data.data
  },

  async submit(data: SubmitReviewResultRequest) {
    const response = await request.post<ApiResult<ReviewResult>>('/reviews/submit', data)
    return response.data.data
  },
}
