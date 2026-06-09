<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, Delete, EditPen } from '@element-plus/icons-vue'
import { questionApi } from '@/api/question'
import DifficultyTag from '@/components/DifficultyTag.vue'
import MarkdownPreview from '@/components/MarkdownPreview.vue'
import type { QuestionDetail } from '@/types/question'

const typeLabel = (type: string) => {
  const map: Record<string, string> = {
    single_choice: '单选题',
    multiple_choice: '多选题',
    judgement: '判断题',
    fill_blank: '填空题',
    short_answer: '简答题',
    code_reading: '代码理解题',
    correction: '纠错题',
  }
  return map[type] || type || '-'
}

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const deleting = ref(false)
const question = ref<QuestionDetail | null>(null)
const questionId = computed(() => route.params.id as string)

const loadDetail = async () => {
  loading.value = true
  try {
    question.value = await questionApi.detail(questionId.value)
  } finally {
    loading.value = false
  }
}

const deleteQuestion = async () => {
  await ElMessageBox.confirm('确定删除这道题目吗？', '删除确认', {
    confirmButtonText: '删除',
    cancelButtonText: '取消',
    type: 'warning',
  })

  deleting.value = true
  try {
    await questionApi.delete(questionId.value)
    ElMessage.success('题目已删除')
    router.push('/questions')
  } finally {
    deleting.value = false
  }
}

onMounted(loadDetail)
</script>

<template>
  <section class="detail-page" v-loading="loading">
    <div class="detail-page__header">
      <el-button :icon="ArrowLeft" @click="router.push('/questions')">返回列表</el-button>
      <div class="detail-page__actions">
        <el-button :icon="EditPen" @click="router.push(`/practice?knowledgeId=${question?.knowledgeId || ''}`)">练习此知识点</el-button>
        <el-button type="danger" :icon="Delete" :loading="deleting" @click="deleteQuestion">删除</el-button>
      </div>
    </div>

    <el-empty v-if="!question && !loading" description="题目不存在" />

    <template v-if="question">
      <article class="detail-title-card">
        <div>
          <h2>题目 #{{ question.id }}</h2>
          <p>知识点 #{{ question.knowledgeId }} · 笔记 #{{ question.noteId }} · {{ question.createTime }}</p>
        </div>
        <div class="tag-row">
          <el-tag>{{ typeLabel(question.questionType) }}</el-tag>
          <DifficultyTag :difficulty="question.difficulty" />
        </div>
      </article>

      <el-card shadow="never">
        <template #header>题干</template>
        <MarkdownPreview :content="question.content" />
      </el-card>

      <el-card v-if="question.options?.length" shadow="never">
        <template #header>选项</template>
        <div class="option-detail-list">
          <div v-for="option in question.options" :key="option.id" class="option-detail-item">
            <strong>{{ option.optionKey }}.</strong>
            <span>{{ option.optionContent }}</span>
            <el-tag v-if="option.correct" type="success" size="small">正确</el-tag>
          </div>
        </div>
      </el-card>

      <el-card shadow="never">
        <template #header>标准答案</template>
        <MarkdownPreview :content="question.standardAnswer" />
      </el-card>

      <el-card shadow="never">
        <template #header>答案解析</template>
        <MarkdownPreview :content="question.analysis" />
      </el-card>
    </template>
  </section>
</template>
