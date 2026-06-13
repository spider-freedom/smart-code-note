import request from '@/utils/request'
import { createSSEStream } from '@/utils/sse'
import type { ApiResult, PageResponse } from '@/types/api'
import type {
  GenerateKnowledgeRequest,
  KnowledgeDetail,
  KnowledgeListItem,
  KnowledgeQuery,
  UpdateKnowledgeRequest,
} from '@/types/knowledge'

export const knowledgeApi = {
  async list(params: KnowledgeQuery) {
    const response = await request.get<ApiResult<PageResponse<KnowledgeListItem>>>('/knowledge/list', { params })
    return response.data.data
  },

  async detail(id: number | string) {
    const response = await request.get<ApiResult<KnowledgeDetail>>(`/knowledge/${id}`)
    return response.data.data
  },

  async update(id: number | string, data: UpdateKnowledgeRequest) {
    const response = await request.put<ApiResult<KnowledgeDetail>>(`/knowledge/${id}`, data)
    return response.data.data
  },

  async delete(id: number | string) {
    await request.delete<ApiResult<null>>(`/knowledge/${id}`)
  },

  async batchDelete(ids: number[]): Promise<number> {
    const response = await request.delete<ApiResult<number>>('/knowledge/batch', { data: ids })
    return response.data.data
  },

  async generate(data: GenerateKnowledgeRequest) {
    const response = await request.post<ApiResult<KnowledgeDetail[]>>('/knowledge/generate', data)
    return response.data.data
  },

  generateStream(
    data: GenerateKnowledgeRequest,
    onChunk: (text: string) => void,
    onResult: (result: KnowledgeDetail[]) => void,
    onError: (message: string) => void,
  ): AbortController {
    return createSSEStream('/api/knowledge/generate-stream', data as Record<string, unknown>, onChunk, onResult, onError)
  },
}
