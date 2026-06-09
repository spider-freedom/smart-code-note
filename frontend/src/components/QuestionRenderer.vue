<script setup lang="ts">
import { computed } from 'vue'
import MarkdownPreview from '@/components/MarkdownPreview.vue'
import type { PracticeQuestion, QuestionDetail } from '@/types/question'

const props = defineProps<{
  question: PracticeQuestion | QuestionDetail
  disabled?: boolean
}>()

const model = defineModel<string>({ required: true })

const normalizedType = computed(() => (props.question.questionType || '').toLowerCase())
const isSingleChoice = computed(() => normalizedType.value === 'single_choice' || normalizedType.value === 'judgement')
const isMultipleChoice = computed(() => normalizedType.value === 'multiple_choice')
const selectedMultiple = computed({
  get: () => (model.value ? model.value.split(',').filter(Boolean) : []),
  set: (value: string[]) => {
    model.value = value.slice().sort().join(',')
  },
})
</script>

<template>
  <article class="question-renderer">
    <MarkdownPreview :content="question.content" />

    <el-radio-group v-if="isSingleChoice" v-model="model" :disabled="disabled" class="option-list">
      <el-radio v-for="option in question.options" :key="option.id" :value="option.optionKey" border>
        {{ option.optionKey }}. {{ option.optionContent }}
      </el-radio>
    </el-radio-group>

    <el-checkbox-group v-else-if="isMultipleChoice" v-model="selectedMultiple" :disabled="disabled" class="option-list">
      <el-checkbox v-for="option in question.options" :key="option.id" :value="option.optionKey" border>
        {{ option.optionKey }}. {{ option.optionContent }}
      </el-checkbox>
    </el-checkbox-group>

    <el-input
      v-else
      v-model="model"
      :disabled="disabled"
      type="textarea"
      :rows="6"
      placeholder="请输入你的答案"
    />
  </article>
</template>
