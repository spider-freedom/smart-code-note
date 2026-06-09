import type { PracticeQuestion } from '@/types/question'

export interface WrongQuestionListItem {
  id: number
  questionId: number
  noteId: number
  knowledgeId: number
  questionType: string
  content: string
  difficulty: string
  wrongCount: number
  mastered: boolean
  lastWrongTime: string
  createTime: string
}

export interface WrongQuestionDetail {
  id: number
  questionId: number
  wrongCount: number
  mastered: boolean
  lastWrongTime: string
  question: PracticeQuestion
}

export interface WrongQuestionQuery {
  pageNum: number
  pageSize: number
  questionId?: number
  mastered?: number
}
