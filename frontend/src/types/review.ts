export interface ReviewTask {
  knowledgeId: number
  noteId: number
  title: string
  type: string
  summary: string
  difficulty: string
  masteryLevel: number
  nextReviewTime: string
}

export interface SubmitReviewResultRequest {
  knowledgeId: number
  questionId?: number
  score?: number
  reviewResult?: string
}

export interface ReviewResult {
  recordId: number
  knowledgeId: number
  questionId: number | null
  reviewResult: string
  score: number
  masteryLevel: number
  nextReviewTime: string
  createTime: string
}
