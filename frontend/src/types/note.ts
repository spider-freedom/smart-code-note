export interface NoteUploadResponse {
  id: number
  taskId: number  // Async AI parse task ID — poll GET /api/note/{id}/parse-status
  title: string
  fileType: string
  parseStatus: number
  originalLength: number
  cleanLength: number
}

export interface ParseStatusResponse {
  status: 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED' | 'NOT_FOUND'
  knowledgeCount: number
  questionCount: number
  errorMessage: string | null
}

export interface NoteListItem {
  id: number
  title: string
  category: string | null
  tags: string | null
  fileType: string
  parseStatus: number
  createTime: string
  updateTime: string
}

export interface NoteDetail extends NoteListItem {
  fileUrl: string
  originalContent: string
  cleanContent: string
}

export interface NoteQuery {
  pageNum: number
  pageSize: number
  keyword?: string
  category?: string
}

export interface NoteUploadPayload {
  file: File
  title: string
  category?: string
  tags?: string
}
