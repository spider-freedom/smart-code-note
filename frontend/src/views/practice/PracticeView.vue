<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { ArrowRight, Refresh, VideoPlay } from '@element-plus/icons-vue'
import { practiceApi } from '@/api/practice'
import AnswerFeedback from '@/components/AnswerFeedback.vue'
import DifficultyTag from '@/components/DifficultyTag.vue'
import QuestionRenderer from '@/components/QuestionRenderer.vue'
import type { AnswerResult, PracticeQuestion } from '@/types/question'

const route = useRoute()
const router = useRouter()
const formRef = ref<FormInstance>()
const loading = ref(false)
const submitting = ref(false)
const questions = ref<PracticeQuestion[]>([])
const currentIndex = ref(0)
const answer = ref('')
const result = ref<AnswerResult | null>(null)

const form = reactive({
  noteId: undefined as number | undefined,
  knowledgeId: undefined as number | undefined,
  questionType: '',
  count: 10,
})

const rules: FormRules<typeof form> = {
  count: [{ required: true, message: '请输入题目数量', trigger: 'blur' }],
}

const currentQuestion = computed(() => questions.value[currentIndex.value])
const progressText = computed(() => {
  if (!questions.value.length) return '未开始'
  return `${currentIndex.value + 1} / ${questions.value.length}`
})

const startPractice = async () => {
  if (!formRef.value) return

  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    questions.value = await practiceApi.start({
      noteId: form.noteId,
      knowledgeId: form.knowledgeId,
      questionType: form.questionType || undefined,
      count: form.count,
    })
    currentIndex.value = 0
    answer.value = ''
    result.value = null

    if (!questions.value.length) {
      ElMessage.info('没有匹配的练习题，请先生成题目')
    }
  } finally {
    loading.value = false
  }
}

const submitAnswer = async () => {
  if (!currentQuestion.value) return

  if (!answer.value.trim()) {
    ElMessage.warning('请先填写答案')
    return
  }

  submitting.value = true
  try {
    result.value = await practiceApi.submit({
      questionId: currentQuestion.value.id,
      answer: answer.value,
    })
  } finally {
    submitting.value = false
  }
}

const nextQuestion = () => {
  if (currentIndex.value < questions.value.length - 1) {
    currentIndex.value += 1
    answer.value = ''
    result.value = null
    return
  }

  ElMessage.success('本轮练习已完成')
}

const restart = () => {
  questions.value = []
  currentIndex.value = 0
  answer.value = ''
  result.value = null
}

onMounted(() => {
  const knowledgeId = Number(route.query.knowledgeId)
  if (Number.isFinite(knowledgeId) && knowledgeId > 0) {
    form.knowledgeId = knowledgeId
  }
})
</script>

<template>
  <section class="practice-page">
    <aside class="practice-config">
      <div class="form-panel__header">
        <h2>在线练习</h2>
        <p>选择笔记、知识点、题型和数量，开始一轮练习。</p>
      </div>

      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item label="笔记 ID">
          <el-input-number v-model="form.noteId" :min="1" controls-position="right" />
        </el-form-item>
        <el-form-item label="知识点 ID">
          <el-input-number v-model="form.knowledgeId" :min="1" controls-position="right" />
        </el-form-item>
        <el-form-item label="题型">
          <el-input v-model.trim="form.questionType" clearable placeholder="可选，如 单选题" />
        </el-form-item>
        <el-form-item label="数量" prop="count">
          <el-input-number v-model="form.count" :min="1" :max="50" controls-position="right" />
        </el-form-item>
        <el-button type="primary" :icon="VideoPlay" :loading="loading" @click="startPractice">开始练习</el-button>
      </el-form>
    </aside>

    <main class="practice-main">
      <el-empty v-if="!currentQuestion" description="请选择条件并开始练习">
        <el-button type="primary" plain @click="router.push('/knowledge')">去生成题目</el-button>
      </el-empty>

      <template v-else>
        <article class="practice-question-card">
          <div class="practice-question-card__header">
            <div>
              <p>进度 {{ progressText }}</p>
              <h2>题目 #{{ currentQuestion.id }}</h2>
            </div>
            <div class="tag-row">
              <el-tag>{{ currentQuestion.questionType }}</el-tag>
              <DifficultyTag :difficulty="currentQuestion.difficulty" />
            </div>
          </div>

          <QuestionRenderer v-model="answer" :question="currentQuestion" :disabled="Boolean(result)" />

          <div class="form-actions">
            <el-button :icon="Refresh" @click="restart">重新选择</el-button>
            <div class="detail-page__actions">
              <el-button
                type="primary"
                :loading="submitting"
                :disabled="Boolean(result)"
                @click="submitAnswer"
              >
                提交答案
              </el-button>
              <el-button :icon="ArrowRight" :disabled="!result" @click="nextQuestion">下一题</el-button>
            </div>
          </div>
        </article>

        <AnswerFeedback v-if="result" :result="result" />
      </template>
    </main>
  </section>
</template>
