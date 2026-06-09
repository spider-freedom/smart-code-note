import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import ElementPlus from 'element-plus'
import QuestionRenderer from './QuestionRenderer.vue'
import type { PracticeQuestion } from '@/types/question'

const baseQuestion: PracticeQuestion = {
  id: 1,
  noteId: 1,
  knowledgeId: 1,
  questionType: 'single_choice',
  content: 'Which option is correct?',
  difficulty: 'easy',
  options: [
    { id: 1, optionKey: 'A', optionContent: 'Alpha' },
    { id: 2, optionKey: 'B', optionContent: 'Beta' },
  ],
}

describe('QuestionRenderer', () => {
  it('renders single choice options', () => {
    const wrapper = mount(QuestionRenderer, {
      props: {
        question: baseQuestion,
        modelValue: '',
        'onUpdate:modelValue': (value: string) => wrapper.setProps({ modelValue: value }),
      },
      global: {
        plugins: [ElementPlus],
      },
    })

    expect(wrapper.text()).toContain('A. Alpha')
    expect(wrapper.text()).toContain('B. Beta')
  })

  it('renders textarea for subjective questions', () => {
    const wrapper = mount(QuestionRenderer, {
      props: {
        question: { ...baseQuestion, questionType: 'short_answer', options: [] },
        modelValue: '',
      },
      global: {
        plugins: [ElementPlus],
      },
    })

    expect(wrapper.find('textarea').exists()).toBe(true)
  })
})
