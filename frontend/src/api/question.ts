import request from '@/utils/request'
import { createSSEStream } from '@/utils/sse'
import type { ApiResult, PageResponse } from '@/types/api'
import type { GenerateQuestionRequest, QuestionDetail, QuestionListItem, QuestionQuery } from '@/types/question'

export const questionApi = {
  async generate(data: GenerateQuestionRequest) {
    const response = await request.post<ApiResult<QuestionDetail[]>>('/questions/generate', data)
    return response.data.data
  },

  async list(params: QuestionQuery) {
    const response = await request.get<ApiResult<PageResponse<QuestionListItem>>>('/questions/list', { params })
    return response.data.data
  },

  async detail(id: number | string) {
    const response = await request.get<ApiResult<QuestionDetail>>(`/questions/${id}`)
    return response.data.data
  },

  async delete(id: number | string) {
    await request.delete<ApiResult<null>>(`/questions/${id}`)
  },

  generateStream(
    data: GenerateQuestionRequest,
    onChunk: (text: string) => void,
    onResult: (result: QuestionDetail[]) => void,
    onError: (message: string) => void,
  ): AbortController {
    return createSSEStream('/api/questions/generate-stream', data as Record<string, unknown>, onChunk, onResult, onError)
  },
}
