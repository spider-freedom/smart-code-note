export interface KnowledgeListItem {
  id: number
  noteId: number
  title: string
  type: string | null
  difficulty: string | null
  masteryLevel: number
  nextReviewTime: string | null
  createTime: string
}

export interface KnowledgeDetail extends KnowledgeListItem {
  summary: string | null
  updateTime: string
}

export interface KnowledgeQuery {
  pageNum: number
  pageSize: number
  noteId?: number
  keyword?: string
  type?: string
  difficulty?: string
  masteryLevel?: number
}

export interface UpdateKnowledgeRequest {
  title: string
  type?: string
  summary?: string
  difficulty?: string
  masteryLevel?: number
  nextReviewTime?: string
}

export interface GenerateKnowledgeRequest {
  noteId: number
}
