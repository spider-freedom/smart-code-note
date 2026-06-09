<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Check, Delete, EditPen, Search } from '@element-plus/icons-vue'
import { practiceApi } from '@/api/practice'
import { questionApi } from '@/api/question'
import { wrongQuestionApi } from '@/api/wrongQuestion'
import AnswerFeedback from '@/components/AnswerFeedback.vue'
import DifficultyTag from '@/components/DifficultyTag.vue'
import QuestionRenderer from '@/components/QuestionRenderer.vue'
import type { AnswerResult, PracticeQuestion, QuestionListItem } from '@/types/question'
import type { WrongQuestionListItem } from '@/types/wrong-question'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const deletingId = ref<number | null>(null)
const questions = ref<QuestionListItem[]>([])
const total = ref(0)
const activeTab = ref<'all' | 'wrong'>('all')

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  noteId: undefined as number | undefined,
  knowledgeId: undefined as number | undefined,
  questionType: '',
  difficulty: '',
  keyword: '',
})

const wrongQuery = reactive({
  pageNum: 1,
  pageSize: 10,
  questionId: undefined as number | undefined,
  mastered: undefined as number | undefined,
})

const wrongQuestions = ref<WrongQuestionListItem[]>([])
const wrongTotal = ref(0)
const retryLoadingId = ref<number | null>(null)
const masteringId = ref<number | null>(null)
const submitting = ref(false)
const drawerVisible = ref(false)
const currentWrongQuestion = ref<WrongQuestionListItem | null>(null)
const retryQuestion = ref<PracticeQuestion | null>(null)
const answer = ref('')
const result = ref<AnswerResult | null>(null)

const typeLabel = (type: string) => {
  const map: Record<string, string> = {
    single_choice: '单选题', multiple_choice: '多选题', judgement: '判断题',
    fill_blank: '填空题', short_answer: '简答题', code_reading: '代码理解题', correction: '纠错题',
  }
  return map[type] || type || '-'
}

const loadQuestions = async () => {
  loading.value = true
  try {
    syncQueryToUrl()
    const result = await questionApi.list({
      pageNum: query.pageNum, pageSize: query.pageSize,
      noteId: query.noteId, knowledgeId: query.knowledgeId,
      questionType: query.questionType || undefined,
      difficulty: query.difficulty || undefined,
      keyword: query.keyword || undefined,
    })
    questions.value = result.records
    total.value = result.total
  } finally { loading.value = false }
}

const loadWrongQuestions = async () => {
  loading.value = true
  try {
    syncWrongQueryToUrl()
    const data = await wrongQuestionApi.list({
      pageNum: wrongQuery.pageNum, pageSize: wrongQuery.pageSize,
      questionId: wrongQuery.questionId, mastered: wrongQuery.mastered,
    })
    wrongQuestions.value = data.records
    wrongTotal.value = data.total
  } finally { loading.value = false }
}

const syncQueryToUrl = () => {
  router.replace({
    path: '/questions', query: {
      pageNum: query.pageNum === 1 ? undefined : String(query.pageNum),
      pageSize: query.pageSize === 10 ? undefined : String(query.pageSize),
      noteId: query.noteId ? String(query.noteId) : undefined,
      knowledgeId: query.knowledgeId ? String(query.knowledgeId) : undefined,
      questionType: query.questionType || undefined,
      difficulty: query.difficulty || undefined,
      keyword: query.keyword || undefined,
    },
  })
}

const syncWrongQueryToUrl = () => {
  router.replace({
    path: '/questions', query: {
      tab: 'wrong',
      pageNum: wrongQuery.pageNum === 1 ? undefined : String(wrongQuery.pageNum),
      pageSize: wrongQuery.pageSize === 10 ? undefined : String(wrongQuery.pageSize),
      questionId: wrongQuery.questionId ? String(wrongQuery.questionId) : undefined,
      mastered: wrongQuery.mastered === undefined ? undefined : String(wrongQuery.mastered),
    },
  })
}

const handleTabChange = (tab: 'all' | 'wrong') => {
  activeTab.value = tab
  if (tab === 'all') loadQuestions()
  else loadWrongQuestions()
}

const search = () => { query.pageNum = 1; loadQuestions() }
const reset = () => {
  query.pageNum = 1; query.noteId = undefined; query.knowledgeId = undefined
  query.questionType = ''; query.difficulty = ''; query.keyword = ''; loadQuestions()
}

const wrongSearch = () => { wrongQuery.pageNum = 1; loadWrongQuestions() }
const wrongReset = () => { wrongQuery.pageNum = 1; wrongQuery.questionId = undefined; wrongQuery.mastered = undefined; loadWrongQuestions() }

const handlePageChange = (pageNum: number) => {
  query.pageNum = pageNum; loadQuestions()
}
const handleSizeChange = (pageSize: number) => {
  query.pageSize = pageSize; query.pageNum = 1; loadQuestions()
}

const handleWrongPageChange = (pageNum: number) => {
  wrongQuery.pageNum = pageNum; loadWrongQuestions()
}
const handleWrongSizeChange = (pageSize: number) => {
  wrongQuery.pageSize = pageSize; wrongQuery.pageNum = 1; loadWrongQuestions()
}

const deleteQuestion = async (row: QuestionListItem) => {
  await ElMessageBox.confirm('确定删除这道题目吗？', '删除确认', {
    confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning',
  })
  deletingId.value = row.id
  try {
    await questionApi.delete(row.id); ElMessage.success('题目已删除')
    if (questions.value.length === 1 && query.pageNum > 1) query.pageNum -= 1
    loadQuestions()
  } finally { deletingId.value = null }
}

const retry = async (row: WrongQuestionListItem) => {
  retryLoadingId.value = row.id
  try {
    retryQuestion.value = await wrongQuestionApi.retry(row.id)
    currentWrongQuestion.value = row; answer.value = ''; result.value = null
    drawerVisible.value = true
  } finally { retryLoadingId.value = null }
}

const submitRetry = async () => {
  if (!retryQuestion.value) return
  if (!answer.value.trim()) { ElMessage.warning('请先填写答案'); return }
  submitting.value = true
  try {
    result.value = await practiceApi.submit({
      questionId: retryQuestion.value.id, answer: answer.value,
    })
    loadWrongQuestions()
  } finally { submitting.value = false }
}

const markMastered = async (row: WrongQuestionListItem) => {
  masteringId.value = row.id
  try { await wrongQuestionApi.markMastered(row.id); ElMessage.success('已标记为掌握'); loadWrongQuestions() }
  finally { masteringId.value = null }
}

onMounted(() => {
  activeTab.value = (route.query.tab as string) === 'wrong' ? 'wrong' : 'all'
  if (activeTab.value === 'wrong') {
    wrongQuery.pageNum = Number(route.query.pageNum) || 1
    wrongQuery.pageSize = Number(route.query.pageSize) || 10
    wrongQuery.questionId = Number(route.query.questionId) || undefined
    const mastered = Number(route.query.mastered)
    if (Number.isFinite(mastered)) wrongQuery.mastered = mastered
    loadWrongQuestions()
  } else {
    query.pageNum = Number(route.query.pageNum) || 1
    query.pageSize = Number(route.query.pageSize) || 10
    query.noteId = Number(route.query.noteId) || undefined
    const knowledgeId = Number(route.query.knowledgeId)
    if (Number.isFinite(knowledgeId) && knowledgeId > 0) query.knowledgeId = knowledgeId
    query.questionType = (route.query.questionType as string) || ''
    query.difficulty = (route.query.difficulty as string) || ''
    query.keyword = (route.query.keyword as string) || ''
    loadQuestions()
  }
})
</script>

<template>
  <section class="table-page">
    <div class="table-page__header">
      <div>
        <h2>题目管理</h2>
        <p>查看由知识点生成的练习题和错题记录。</p>
      </div>
      <el-button type="primary" :icon="EditPen" @click="router.push('/practice')">开始练习</el-button>
    </div>

    <el-tabs v-model="activeTab" @tab-change="handleTabChange">
      <el-tab-pane label="全部题目" name="all">
        <el-form class="filter-bar" :model="query" inline @submit.prevent="search">
          <el-form-item label="笔记 ID">
            <el-input-number v-model="query.noteId" :min="1" controls-position="right" />
          </el-form-item>
          <el-form-item label="知识点 ID">
            <el-input-number v-model="query.knowledgeId" :min="1" controls-position="right" />
          </el-form-item>
          <el-form-item label="题型">
            <el-input v-model.trim="query.questionType" clearable placeholder="单选题/简答题/判断题" @keyup.enter="search" />
          </el-form-item>
          <el-form-item label="难度">
            <el-input v-model.trim="query.difficulty" clearable placeholder="简单/中等/困难" @keyup.enter="search" />
          </el-form-item>
          <el-form-item label="关键词">
            <el-input v-model.trim="query.keyword" clearable placeholder="题干关键词" @keyup.enter="search" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :icon="Search" @click="search">查询</el-button>
            <el-button @click="reset">重置</el-button>
          </el-form-item>
        </el-form>

        <el-table v-loading="loading" :data="questions" border>
          <el-table-column prop="content" label="题干" min-width="300" show-overflow-tooltip>
            <template #default="{ row }">
              <el-button link type="primary" @click="router.push(`/questions/${row.id}`)">{{ row.content }}</el-button>
            </template>
          </el-table-column>
          <el-table-column prop="questionType" label="题型" width="130">
            <template #default="{ row }">{{ typeLabel(row.questionType) }}</template>
          </el-table-column>
          <el-table-column prop="difficulty" label="难度" width="120">
            <template #default="{ row }"><DifficultyTag :difficulty="row.difficulty" /></template>
          </el-table-column>
          <el-table-column prop="knowledgeId" label="知识点 ID" width="110" />
          <el-table-column prop="createTime" label="创建时间" min-width="170" />
          <el-table-column label="操作" width="160" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="router.push(`/questions/${row.id}`)">详情</el-button>
              <el-button link type="danger" :icon="Delete" :loading="deletingId === row.id" @click="deleteQuestion(row)">删除</el-button>
            </template>
          </el-table-column>
          <template #empty>
            <el-empty description="暂无题目">
              <el-button type="primary" @click="router.push('/knowledge')">去知识点生成题目</el-button>
            </el-empty>
          </template>
        </el-table>

        <div class="pagination-bar">
          <el-pagination
            background layout="total, sizes, prev, pager, next, jumper"
            :current-page="query.pageNum" :page-size="query.pageSize"
            :page-sizes="[10, 20, 50, 100]" :total="total"
            @current-change="handlePageChange" @size-change="handleSizeChange"
          />
        </div>
      </el-tab-pane>

      <el-tab-pane label="错题本" name="wrong">
        <el-form class="filter-bar" :model="wrongQuery" inline @submit.prevent="wrongSearch">
          <el-form-item label="题目 ID">
            <el-input-number v-model="wrongQuery.questionId" :min="1" controls-position="right" />
          </el-form-item>
          <el-form-item label="掌握状态">
            <el-select v-model="wrongQuery.mastered" clearable placeholder="全部" style="width: 140px">
              <el-option label="未掌握" :value="0" />
              <el-option label="已掌握" :value="1" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :icon="Search" @click="wrongSearch">查询</el-button>
            <el-button @click="wrongReset">重置</el-button>
          </el-form-item>
        </el-form>

        <el-table v-loading="loading" :data="wrongQuestions" border>
          <el-table-column prop="content" label="题干" min-width="300" show-overflow-tooltip>
            <template #default="{ row }">
              <el-button link type="primary" @click="router.push(`/questions/${row.questionId}`)">{{ row.content }}</el-button>
            </template>
          </el-table-column>
          <el-table-column prop="questionType" label="题型" width="130" />
          <el-table-column prop="difficulty" label="难度" width="120">
            <template #default="{ row }"><DifficultyTag :difficulty="row.difficulty" /></template>
          </el-table-column>
          <el-table-column prop="wrongCount" label="错误次数" width="100" />
          <el-table-column prop="mastered" label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="row.mastered ? 'success' : 'danger'">{{ row.mastered ? '已掌握' : '未掌握' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="lastWrongTime" label="最近错误时间" min-width="170" />
          <el-table-column label="操作" width="220" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" :loading="retryLoadingId === row.id" @click="retry(row)">重练</el-button>
              <el-button
                link type="success" :icon="Check"
                :disabled="row.mastered" :loading="masteringId === row.id"
                @click="markMastered(row)"
              >
                标记掌握
              </el-button>
            </template>
          </el-table-column>
          <template #empty>
            <el-empty description="暂无错题">
              <el-button type="primary" @click="router.push('/practice')">去练习</el-button>
            </el-empty>
          </template>
        </el-table>

        <div class="pagination-bar">
          <el-pagination
            background layout="total, sizes, prev, pager, next, jumper"
            :current-page="wrongQuery.pageNum" :page-size="wrongQuery.pageSize"
            :page-sizes="[10, 20, 50, 100]" :total="wrongTotal"
            @current-change="handleWrongPageChange" @size-change="handleWrongSizeChange"
          />
        </div>
      </el-tab-pane>
    </el-tabs>

    <el-drawer v-model="drawerVisible" size="620px" title="错题重练">
      <div v-if="retryQuestion" class="retry-panel">
        <div class="retry-panel__meta">
          <el-tag type="danger">错 {{ currentWrongQuestion?.wrongCount || 0 }} 次</el-tag>
          <DifficultyTag :difficulty="retryQuestion.difficulty" />
        </div>
        <QuestionRenderer v-model="answer" :question="retryQuestion" :disabled="Boolean(result)" />
        <div class="form-actions">
          <el-button @click="drawerVisible = false">关闭</el-button>
          <el-button type="primary" :loading="submitting" :disabled="Boolean(result)" @click="submitRetry">提交答案</el-button>
        </div>
        <AnswerFeedback v-if="result" :result="result" />
      </div>
    </el-drawer>
  </section>
</template>
