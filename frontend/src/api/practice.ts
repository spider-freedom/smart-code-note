import request from '@/utils/request'
import type { ApiResult } from '@/types/api'
import type { AnswerResult, PracticeQuestion, StartPracticeQuery, SubmitAnswerRequest } from '@/types/question'

export const practiceApi = {
  async start(params: StartPracticeQuery) {
    const response = await request.get<ApiResult<PracticeQuestion[]>>('/practice/start', { params })
    return response.data.data
  },

  async submit(data: SubmitAnswerRequest) {
    const response = await request.post<ApiResult<AnswerResult>>('/practice/submit', data)
    return response.data.data
  },
}
