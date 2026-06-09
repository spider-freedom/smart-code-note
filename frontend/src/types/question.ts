export interface QuestionOption {
  id: number
  optionKey: string
  optionContent: string
  correct?: boolean
}

export interface QuestionListItem {
  id: number
  noteId: number
  knowledgeId: number
  questionType: string
  content: string
  difficulty: string
  createTime: string
}

export interface QuestionDetail {
  id: number
  noteId: number
  knowledgeId: number
  questionType: string
  content: string
  standardAnswer: string
  analysis: string
  difficulty: string
  options: QuestionOption[]
  createTime: string
  updateTime: string
}

export interface GenerateQuestionRequest {
  knowledgeId: number
  count: number
}

export interface QuestionQuery {
  pageNum: number
  pageSize: number
  noteId?: number
  knowledgeId?: number
  questionType?: string
  difficulty?: string
  keyword?: string
}

export interface PracticeQuestion {
  id: number
  noteId: number
  knowledgeId: number
  questionType: string
  content: string
  difficulty: string
  options: QuestionOption[]
}

export interface StartPracticeQuery {
  noteId?: number
  knowledgeId?: number
  questionType?: string
  count: number
}

export interface SubmitAnswerRequest {
  questionId: number
  answer: string
}

export interface AnswerResult {
  recordId: number
  questionId: number
  questionType: string
  userAnswer: string
  standardAnswer: string
  score: number
  correct: boolean
  aiComment: string
  createTime: string
}
