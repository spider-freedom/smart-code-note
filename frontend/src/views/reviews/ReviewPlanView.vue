<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Check, Close, EditPen, Refresh, Star } from '@element-plus/icons-vue'
import { reviewApi } from '@/api/review'
import DifficultyTag from '@/components/DifficultyTag.vue'
import KnowledgeLevelTag from '@/components/KnowledgeLevelTag.vue'
import MarkdownPreview from '@/components/MarkdownPreview.vue'
import type { ReviewResult, ReviewTask } from '@/types/review'

const router = useRouter()
const loading = ref(false)
const submittingKey = ref('')
const tasks = ref<ReviewTask[]>([])
const latestResult = ref<ReviewResult | null>(null)

const resultActions = [
  { label: '记住了', value: 'remembered', score: 100, type: 'success' as const, icon: Check },
  { label: '忘记了', value: 'forgotten', score: 0, type: 'danger' as const, icon: Close },
  { label: '已掌握', value: 'mastered', score: 100, type: 'primary' as const, icon: Star },
]

const loadTasks = async () => {
  loading.value = true
  try {
    tasks.value = await reviewApi.getTodayTasks()
  } finally {
    loading.value = false
  }
}

const submitResult = async (task: ReviewTask, action: (typeof resultActions)[number]) => {
  submittingKey.value = `${task.knowledgeId}-${action.value}`
  try {
    latestResult.value = await reviewApi.submit({
      knowledgeId: task.knowledgeId,
      reviewResult: action.value,
      score: action.score,
    })
    ElMessage.success(`已提交：${action.label}`)
    await loadTasks()
  } finally {
    submittingKey.value = ''
  }
}

onMounted(loadTasks)
</script>

<template>
  <section class="review-page" v-loading="loading">
    <div class="dashboard-hero">
      <div>
        <p class="dashboard-hero__eyebrow">复习计划</p>
        <h2>今日复习任务</h2>
        <p>{{ tasks.length ? `今天还有 ${tasks.length} 个知识点需要处理。` : '今日复习任务已清空，可以去练习或上传新笔记。' }}</p>
      </div>
      <div class="detail-page__actions">
        <el-button :icon="Refresh" @click="loadTasks">刷新</el-button>
        <el-button type="primary" :icon="EditPen" @click="router.push('/practice')">开始练习</el-button>
      </div>
    </div>

    <el-alert
      v-if="latestResult"
      type="success"
      show-icon
      :closable="false"
      :title="`最近提交：${latestResult.reviewResult}，掌握度更新为 ${latestResult.masteryLevel}`"
    >
      <template #default>
        下次复习时间：{{ latestResult.nextReviewTime }}
      </template>
    </el-alert>

    <el-empty v-if="!tasks.length && !loading" description="今日暂无复习任务">
      <el-button type="primary" @click="router.push('/knowledge')">查看知识点</el-button>
    </el-empty>

    <div v-else class="review-task-grid">
      <article v-for="task in tasks" :key="task.knowledgeId" class="review-task-card">
        <div class="review-task-card__header">
          <div>
            <h3>{{ task.title }}</h3>
            <p>笔记 #{{ task.noteId }} · 知识点 #{{ task.knowledgeId }}</p>
          </div>
          <div class="tag-row">
            <DifficultyTag :difficulty="task.difficulty" />
            <KnowledgeLevelTag :level="task.masteryLevel" />
          </div>
        </div>

        <div class="review-task-card__meta">
          <el-tag type="info">{{ task.type || '未分类' }}</el-tag>
          <span>计划复习：{{ task.nextReviewTime || '立即复习' }}</span>
        </div>

        <MarkdownPreview :content="task.summary" />

        <div class="review-task-card__actions">
          <el-button text @click="router.push(`/knowledge/${task.knowledgeId}`)">查看详情</el-button>
          <div>
            <el-button
              v-for="action in resultActions"
              :key="action.value"
              :type="action.type"
              :icon="action.icon"
              :loading="submittingKey === `${task.knowledgeId}-${action.value}`"
              @click="submitResult(task, action)"
            >
              {{ action.label }}
            </el-button>
          </div>
        </div>
      </article>
    </div>
  </section>
</template>
