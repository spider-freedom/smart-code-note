export interface ChatMessage {
  id: number
  sessionId: number
  role: 'user' | 'assistant'
  content: string
  createTime: string
}

export interface ChatSession {
  id: number
  title: string
  createTime: string
  updateTime: string
  messages?: ChatMessage[]
}

export interface ChatLearningContext {
  totalNotes: number
  totalKnowledgePoints: number
  masteredKnowledgePoints: number
  reviewDueCount: number
  todayPracticeCount: number
  todayCorrectRate: number
  recentNoteTitle: string | null
}
