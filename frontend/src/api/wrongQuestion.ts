import request from '@/utils/request'
import type { ApiResult, PageResponse } from '@/types/api'
import type { PracticeQuestion } from '@/types/question'
import type { WrongQuestionDetail, WrongQuestionListItem, WrongQuestionQuery } from '@/types/wrong-question'

export const wrongQuestionApi = {
  async list(params: WrongQuestionQuery) {
    const response = await request.get<ApiResult<PageResponse<WrongQuestionListItem>>>('/wrong-questions/list', {
      params,
    })
    return response.data.data
  },

  async retry(id: number | string) {
    const response = await request.post<ApiResult<PracticeQuestion>>(`/wrong-questions/${id}/retry`)
    return response.data.data
  },

  async markMastered(id: number | string) {
    const response = await request.put<ApiResult<WrongQuestionDetail>>(`/wrong-questions/${id}/mastered`)
    return response.data.data
  },
}
