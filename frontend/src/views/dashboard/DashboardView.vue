<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { use } from 'echarts/core'
import { BarChart, PieChart } from 'echarts/charts'
import { GridComponent, LegendComponent, TooltipComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import VChart from 'vue-echarts'

use([BarChart, PieChart, GridComponent, LegendComponent, TooltipComponent, CanvasRenderer])
import {
  Collection,
  DataAnalysis,
  Document,
  EditPen,
  Notebook,
  Refresh,
  Tickets,
  Warning,
} from '@element-plus/icons-vue'
import { reportApi } from '@/api/report'
import { reviewApi } from '@/api/review'
import StatCard from '@/components/StatCard.vue'
import type { LearningOverview, LearningSuggestion, WeakKnowledge } from '@/types/report'
import type { ReviewTask } from '@/types/review'

const router = useRouter()
const loading = ref(false)
const error = ref('')
const overview = ref<LearningOverview | null>(null)
const todayTasks = ref<ReviewTask[]>([])
const weakKnowledge = ref<WeakKnowledge[]>([])
const suggestion = ref<LearningSuggestion | null>(null)

const percent = (value?: number | null) => {
  if (value === null || value === undefined || Number.isNaN(value)) return '0%'
  return `${Math.round(value * 100)}%`
}

const numberText = (value?: number | null) => value ?? 0

const masteryText = computed(() => {
  const value = overview.value?.averageMasteryLevel
  if (value === null || value === undefined) return '0.0'
  return value.toFixed(1)
})

const stats = computed(() => [
  {
    title: '笔记',
    value: numberText(overview.value?.noteCount),
    caption: '已上传开发笔记',
    icon: Notebook,
  },
  {
    title: '知识点',
    value: numberText(overview.value?.knowledgeCount),
    caption: '可复习知识单元',
    icon: Collection,
  },
  {
    title: '题目',
    value: numberText(overview.value?.questionCount),
    caption: '已生成练习题',
    icon: Tickets,
  },
  {
    title: '正确率',
    value: percent(overview.value?.correctRate),
    caption: `${numberText(overview.value?.answerCount)} 次答题`,
    icon: DataAnalysis,
  },
  {
    title: '错题',
    value: numberText(overview.value?.wrongQuestionCount),
    caption: `${numberText(overview.value?.masteredWrongQuestionCount)} 题已掌握`,
    icon: Warning,
  },
  {
    title: '待复习',
    value: numberText(overview.value?.dueReviewCount),
    caption: `平均掌握度 ${masteryText.value}`,
    icon: Refresh,
  },
])

const answerChartOption = computed(() => ({
  tooltip: { trigger: 'item' },
  legend: { bottom: 0 },
  series: [
    {
      name: '答题结果',
      type: 'pie',
      radius: ['48%', '70%'],
      data: [
        { value: numberText(overview.value?.correctAnswerCount), name: '正确' },
        {
          value: Math.max(0, numberText(overview.value?.answerCount) - numberText(overview.value?.correctAnswerCount)),
          name: '错误',
        },
      ],
    },
  ],
}))

const masteryChartOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  grid: { top: 24, right: 16, bottom: 36, left: 36 },
  xAxis: {
    type: 'category',
    data: ['平均掌握度', '待复习', '错题', '已掌握错题'],
  },
  yAxis: { type: 'value' },
  series: [
    {
      name: '学习状态',
      type: 'bar',
      data: [
        Number((overview.value?.averageMasteryLevel || 0).toFixed(1)),
        numberText(overview.value?.dueReviewCount),
        numberText(overview.value?.wrongQuestionCount),
        numberText(overview.value?.masteredWrongQuestionCount),
      ],
      itemStyle: { color: '#2563eb' },
    },
  ],
}))

const weakChartOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  grid: { top: 24, right: 24, bottom: 56, left: 42 },
  xAxis: {
    type: 'category',
    axisLabel: { rotate: 20 },
    data: weakKnowledge.value.map((item) => item.title),
  },
  yAxis: { type: 'value', max: 100 },
  series: [
    {
      name: '薄弱指数',
      type: 'bar',
      data: weakKnowledge.value.map((item) => Math.round((item.weaknessScore || 0) * 100)),
      itemStyle: { color: '#dc2626' },
    },
  ],
}))

const quickActions = [
  { title: '上传笔记', description: '导入 .txt 或 .md 开发笔记', path: '/notes', icon: Document },
  { title: '开始练习', description: '按知识点选择题目练习', path: '/practice', icon: EditPen },
  { title: '复习计划', description: '处理今日待复习任务', path: '/reviews', icon: Refresh },
  { title: '题库管理', description: '浏览和筛选题目', path: '/questions', icon: Tickets },
]

const loadDashboard = async () => {
  loading.value = true
  error.value = ''

  try {
    const [overviewData, todayTaskData, weakKnowledgeData, suggestionData] = await Promise.all([
      reportApi.getOverview(),
      reviewApi.getTodayTasks(),
      reportApi.getWeakKnowledge(5),
      reportApi.getSuggestions(),
    ])

    overview.value = overviewData
    todayTasks.value = todayTaskData
    weakKnowledge.value = weakKnowledgeData
    suggestion.value = suggestionData
  } catch (err) {
    error.value = err instanceof Error ? err.message : '首页数据加载失败'
  } finally {
    loading.value = false
  }
}

onMounted(loadDashboard)
</script>

<template>
  <div class="dashboard-page" v-loading="loading">
    <el-alert v-if="error" :title="error" type="error" show-icon :closable="false">
      <template #default>
        <el-button size="small" :icon="Refresh" @click="loadDashboard">重新加载</el-button>
      </template>
    </el-alert>

    <section class="dashboard-hero">
      <div>
        <p class="dashboard-hero__eyebrow">学习工作台</p>
        <h2>把今天该复习的内容处理掉。</h2>
        <p>从笔记、知识点、练习、错题到报告，首页负责把下一步行动摆在你面前。</p>
      </div>
      <el-button type="primary" size="large" @click="router.push('/notes')">上传笔记</el-button>
    </section>

    <section class="stat-grid" aria-label="学习统计">
      <StatCard
        v-for="item in stats"
        :key="item.title"
        :title="item.title"
        :value="item.value"
        :caption="item.caption"
        :icon="item.icon"
      />
    </section>

    <section class="report-chart-grid">
      <article class="dashboard-card">
        <div class="dashboard-card__header">
          <h3>答题正确率</h3>
          <p>正确与错误答题次数占比</p>
        </div>
        <VChart class="report-chart" :option="answerChartOption" autoresize />
      </article>

      <article class="dashboard-card">
        <div class="dashboard-card__header">
          <h3>掌握与复习状态</h3>
          <p>掌握度、待复习和错题情况</p>
        </div>
        <VChart class="report-chart" :option="masteryChartOption" autoresize />
      </article>
    </section>

    <section class="dashboard-grid">
      <article class="dashboard-card dashboard-card--wide">
        <div class="dashboard-card__header">
          <div>
            <h3>今日复习任务</h3>
            <p>{{ todayTasks.length ? `还有 ${todayTasks.length} 个知识点需要复习` : '今天暂无待复习任务' }}</p>
          </div>
          <el-button text @click="router.push('/reviews')">查看全部</el-button>
        </div>

        <el-empty v-if="!todayTasks.length" description="暂无复习任务">
          <el-button type="primary" plain @click="router.push('/notes')">上传第一篇笔记</el-button>
        </el-empty>

        <div v-else class="task-list">
          <button
            v-for="task in todayTasks.slice(0, 5)"
            :key="task.knowledgeId"
            class="task-item"
            type="button"
            @click="router.push(`/knowledge/${task.knowledgeId}`)"
          >
            <span>
              <strong>{{ task.title }}</strong>
              <small>{{ task.type || '未分类' }} · {{ task.difficulty || '未设置难度' }}</small>
            </span>
            <el-tag size="small" type="info">掌握度 {{ task.masteryLevel ?? 0 }}</el-tag>
          </button>
        </div>
      </article>

      <article class="dashboard-card">
        <div class="dashboard-card__header">
          <div>
            <h3>薄弱知识点</h3>
            <p>优先处理正确率低、错题多的内容</p>
          </div>
        </div>

        <el-empty v-if="!weakKnowledge.length" description="暂无薄弱点数据" />

        <div v-else class="weak-list">
          <button
            v-for="item in weakKnowledge"
            :key="item.knowledgeId"
            class="weak-item"
            type="button"
            @click="router.push(`/knowledge/${item.knowledgeId}`)"
          >
            <span>
              <strong>{{ item.title }}</strong>
              <small>正确率 {{ percent(item.correctRate) }} · 错 {{ item.wrongCount ?? 0 }} 次</small>
            </span>
            <el-progress :percentage="Math.round((item.weaknessScore || 0) * 100)" :show-text="false" />
          </button>
        </div>
      </article>
    </section>

    <section class="dashboard-card">
      <div class="dashboard-card__header">
        <div>
          <h3>薄弱指数排行</h3>
          <p>按知识点薄弱分排序</p>
        </div>
      </div>
      <el-empty v-if="!weakKnowledge.length" description="暂无图表数据" />
      <VChart v-else class="report-chart" :option="weakChartOption" autoresize />
    </section>

    <section class="dashboard-card">
      <div class="dashboard-card__header">
        <h3>AI 学习建议</h3>
        <p>{{ suggestion?.summary || '暂无学习建议，请先上传笔记并完成练习。' }}</p>
      </div>

      <el-empty v-if="!suggestion?.suggestions?.length" description="暂无建议" />

      <ol v-else class="suggestion-list">
        <li v-for="item in suggestion.suggestions" :key="item">{{ item }}</li>
      </ol>
    </section>

    <section class="quick-grid" aria-label="快捷入口">
      <button v-for="action in quickActions" :key="action.path" class="quick-action" type="button" @click="router.push(action.path)">
        <el-icon><component :is="action.icon" /></el-icon>
        <span>
          <strong>{{ action.title }}</strong>
          <small>{{ action.description }}</small>
        </span>
      </button>
    </section>
  </div>
</template>
