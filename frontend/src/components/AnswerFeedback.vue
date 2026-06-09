<script setup lang="ts">
import MarkdownPreview from '@/components/MarkdownPreview.vue'
import type { AnswerResult } from '@/types/question'

defineProps<{
  result: AnswerResult
}>()
</script>

<template>
  <section class="answer-feedback" :class="{ 'answer-feedback--correct': result.correct }">
    <div class="answer-feedback__head">
      <el-tag :type="result.correct ? 'success' : 'danger'">
        {{ result.correct ? '回答正确' : '需要复习' }}
      </el-tag>
      <strong>{{ result.score }} 分</strong>
    </div>

    <el-descriptions :column="1" border>
      <el-descriptions-item label="你的答案">{{ result.userAnswer }}</el-descriptions-item>
      <el-descriptions-item label="标准答案">{{ result.standardAnswer }}</el-descriptions-item>
    </el-descriptions>

    <div class="answer-feedback__comment">
      <h3>反馈</h3>
      <MarkdownPreview :content="result.aiComment" />
    </div>
  </section>
</template>
