export interface NoteUploadResponse {
  id: number
  title: string
  fileType: string
  parseStatus: number
  originalLength: number
  cleanLength: number
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
