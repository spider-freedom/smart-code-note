export interface LearningOverview {
  noteCount: number
  knowledgeCount: number
  questionCount: number
  answerCount: number
  correctAnswerCount: number
  correctRate: number
  wrongQuestionCount: number
  masteredWrongQuestionCount: number
  dueReviewCount: number
  averageMasteryLevel: number
}

export interface WeakKnowledge {
  knowledgeId: number
  noteId: number
  title: string
  type: string
  difficulty: string
  masteryLevel: number
  answerCount: number
  wrongCount: number
  correctRate: number
  weaknessScore: number
}

export interface LearningSuggestion {
  summary: string
  suggestions: string[]
  weakKnowledgePoints: WeakKnowledge[]
}
