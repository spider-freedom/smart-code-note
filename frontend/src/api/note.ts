import request from '@/utils/request'
import type { ApiResult, PageResponse } from '@/types/api'
import type { NoteDetail, NoteListItem, NoteQuery, NoteUploadPayload, NoteUploadResponse } from '@/types/note'

export const noteApi = {
  async upload(payload: NoteUploadPayload) {
    const formData = new FormData()
    formData.append('file', payload.file)
    formData.append('title', payload.title)

    if (payload.category) {
      formData.append('category', payload.category)
    }

    if (payload.tags) {
      formData.append('tags', payload.tags)
    }

    const response = await request.post<ApiResult<NoteUploadResponse>>('/note/upload', formData)
    return response.data.data
  },

  async list(params: NoteQuery) {
    const response = await request.get<ApiResult<PageResponse<NoteListItem>>>('/note/list', { params })
    return response.data.data
  },

  async detail(id: number | string) {
    const response = await request.get<ApiResult<NoteDetail>>(`/note/${id}`)
    return response.data.data
  },

  async delete(id: number | string) {
    await request.delete<ApiResult<null>>(`/note/${id}`)
  },

  async reparse(id: number | string) {
    const response = await request.post<ApiResult<NoteDetail>>(`/note/${id}/parse`)
    return response.data.data
  },
}
