<template>
  <view class="page-container">
    <!-- 配置面板 -->
    <view v-if="!started" class="card config-panel">
      <text class="config-title">开始练习</text>

      <view class="form-group">
        <text class="form-label">知识点</text>
        <picker mode="selector" :range="knowledgeOptions" range-key="title" @change="onKnowledgeChange">
          <view class="picker-value">{{ selectedKnowledge?.title || '不限知识点' }}</view>
        </picker>
      </view>

      <view class="form-group">
        <text class="form-label">题目数量</text>
        <view class="count-selector">
          <view class="count-item" :class="{ active: config.count === 5 }" @tap="config.count = 5">5</view>
          <view class="count-item" :class="{ active: config.count === 10 }" @tap="config.count = 10">10</view>
          <view class="count-item" :class="{ active: config.count === 20 }" @tap="config.count = 20">20</view>
        </view>
      </view>

      <view class="form-group">
        <text class="form-label">难度</text>
        <view class="count-selector">
          <view class="count-item" :class="{ active: !config.difficulty }" @tap="config.difficulty = null">不限</view>
          <view class="count-item" :class="{ active: config.difficulty === 'easy' }" @tap="config.difficulty = 'easy'">简单</view>
          <view class="count-item" :class="{ active: config.difficulty === 'medium' }" @tap="config.difficulty = 'medium'">中等</view>
          <view class="count-item" :class="{ active: config.difficulty === 'hard' }" @tap="config.difficulty = 'hard'">困难</view>
        </view>
      </view>

      <button class="start-btn" @tap="startPractice">开始练习</button>
    </view>

    <!-- 答题面板 -->
    <view v-if="started && currentQuestion" class="card">
      <view class="progress-bar">
        <view class="progress-fill" :style="{ width: (currentIndex / questions.length * 100) + '%' }"></view>
      </view>
      <text class="progress-text">{{ currentIndex + 1 }} / {{ questions.length }}</text>

      <text class="question-content">{{ currentQuestion.content }}</text>

      <!-- 选择题选项 -->
      <view class="options" v-if="currentQuestion.options && currentQuestion.options.length">
        <view class="option" v-for="opt in currentQuestion.options" :key="opt.optionKey"
              :class="{ selected: selectedAnswer === opt.optionKey }"
              @tap="selectOption(opt.optionKey)">
          <text class="opt-key">{{ opt.optionKey }}</text>
          <text class="opt-content">{{ opt.optionContent }}</text>
        </view>
      </view>

      <!-- 主观题输入 -->
      <view v-else class="subjective-area">
        <textarea class="answer-input" v-model="textAnswer" placeholder="请输入你的答案..." />
      </view>

      <!-- 提交按钮 -->
      <button class="submit-btn" @tap="handleSubmit" :loading="submitting" v-if="!answered">
        {{ currentIndex < questions.length - 1 ? '下一题' : '提交' }}
      </button>

      <!-- 答题反馈 -->
      <view v-if="answered && feedback" class="feedback-section">
        <view class="feedback-score" :class="{ correct: feedback.correct, wrong: !feedback.correct }">
          <text>{{ feedback.correct ? '✓ 正确' : '✗ 错误' }}</text>
          <text v-if="feedback.score">得分: {{ feedback.score }}</text>
        </view>
        <view class="feedback-comment" v-if="feedback.comment">
          <text class="section-label">AI 点评</text>
          <text class="comment-text">{{ feedback.comment }}</text>
        </view>
        <button class="next-btn" @tap="nextQuestion" v-if="currentIndex < questions.length - 1">
          继续下一题
        </button>
        <button class="finish-btn" @tap="finishPractice" v-else>
          完成练习
        </button>
      </view>
    </view>

    <!-- 练习完成 -->
    <view v-if="started && finished" class="card result-panel">
      <text class="result-emoji">{{ (correctCount / questions.length >= 0.6) ? '🎉' : '💪' }}</text>
      <text class="result-title">练习完成</text>
      <text class="result-score">正确 {{ correctCount }} / {{ questions.length }}</text>
      <button class="back-btn" @tap="resetPractice">返回</button>
    </view>
  </view>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { practiceApi } from '@/api/practice'
import { knowledgeApi } from '@/api/knowledge'

const started = ref(false)
const finished = ref(false)
const questions = ref([])
const currentIndex = ref(0)
const selectedAnswer = ref(null)
const textAnswer = ref('')
const submitting = ref(false)
const answered = ref(false)
const feedback = ref(null)
const correctCount = ref(0)
const answerResults = ref([])

const knowledgeOptions = ref([])
const selectedKnowledge = ref(null)

const config = ref({
  knowledgeId: null,
  count: 10,
  difficulty: null
})

const currentQuestion = computed(() => questions.value[currentIndex.value] || null)

onMounted(async () => {
  try {
    const result = await knowledgeApi.list({ pageNum: 1, pageSize: 100 })
    knowledgeOptions.value = result.records || []
  } catch (e) { /* ignore */ }
})

function onKnowledgeChange(e) {
  const idx = e.detail.value
  selectedKnowledge.value = knowledgeOptions.value[idx]
  config.value.knowledgeId = selectedKnowledge.value?.id || null
}

async function startPractice() {
  try {
    const result = await practiceApi.start({
      count: config.value.count,
      knowledgeId: config.value.knowledgeId || undefined,
      difficulty: config.value.difficulty || undefined
    })
    questions.value = result || []
    if (!questions.value.length) {
      uni.showToast({ title: '没有可用题目', icon: 'none' })
      return
    }
    started.value = true
    currentIndex.value = 0
    correctCount.value = 0
    answerResults.value = []
    answered.value = false
    feedback.value = null
    selectedAnswer.value = null
    textAnswer.value = ''
  } catch (e) { /* ignore */ }
}

function selectOption(key) {
  if (answered.value) return
  selectedAnswer.value = key
}

async function handleSubmit() {
  if (!selectedAnswer.value && !textAnswer.value) {
    uni.showToast({ title: '请先作答', icon: 'none' })
    return
  }
  submitting.value = true
  try {
    const result = await practiceApi.submit({
      questionId: currentQuestion.value.id,
      answer: selectedAnswer.value || textAnswer.value
    })
    feedback.value = result
    answered.value = true
    if (result.correct) correctCount.value++
    answerResults.value.push(result)
  } catch (e) { /* ignore */ } finally {
    submitting.value = false
  }
}

function nextQuestion() {
  currentIndex.value++
  selectedAnswer.value = null
  textAnswer.value = ''
  answered.value = false
  feedback.value = null
}

function finishPractice() {
  finished.value = true
}

function resetPractice() {
  started.value = false
  finished.value = false
  questions.value = []
  currentIndex.value = 0
  selectedAnswer.value = null
  textAnswer.value = ''
  answered.value = false
  feedback.value = null
  correctCount.value = 0
}
</script>

<style scoped>
.config-title {
  font-size: 32rpx;
  font-weight: 700;
  color: #111827;
  display: block;
  margin-bottom: 24rpx;
}

.form-group {
  margin-bottom: 24rpx;
}

.form-label {
  font-size: 26rpx;
  color: #6b7280;
  margin-bottom: 12rpx;
  display: block;
}

.picker-value {
  height: 72rpx;
  line-height: 72rpx;
  background: #f9fafb;
  border-radius: 12rpx;
  padding: 0 20rpx;
  font-size: 28rpx;
  color: #374151;
  border: 1rpx solid #e5e7eb;
}

.count-selector {
  display: flex;
  gap: 16rpx;
}

.count-item {
  flex: 1;
  text-align: center;
  padding: 14rpx 0;
  background: #f9fafb;
  border-radius: 12rpx;
  font-size: 26rpx;
  color: #6b7280;
  border: 1rpx solid #e5e7eb;
}

.count-item.active {
  background: #dbeafe;
  color: #1d4ed8;
  border-color: #1d4ed8;
}

.start-btn {
  width: 100%;
  height: 80rpx;
  line-height: 80rpx;
  background: #1d4ed8;
  color: #fff;
  border-radius: 40rpx;
  font-size: 28rpx;
  border: none;
  text-align: center;
  margin-top: 16rpx;
}

.progress-bar {
  height: 8rpx;
  background: #f3f4f6;
  border-radius: 4rpx;
  margin-bottom: 8rpx;
}

.progress-fill {
  height: 100%;
  background: #1d4ed8;
  border-radius: 4rpx;
  transition: width 0.3s;
}

.progress-text {
  font-size: 24rpx;
  color: #9ca3af;
  display: block;
  margin-bottom: 24rpx;
}

.question-content {
  font-size: 30rpx;
  color: #111827;
  line-height: 1.8;
  display: block;
  margin-bottom: 32rpx;
}

.options {
  display: flex;
  flex-direction: column;
  gap: 16rpx;
  margin-bottom: 32rpx;
}

.option {
  display: flex;
  align-items: flex-start;
  gap: 16rpx;
  padding: 20rpx;
  background: #f9fafb;
  border-radius: 12rpx;
  border: 2rpx solid transparent;
}

.option.selected {
  background: #dbeafe;
  border-color: #1d4ed8;
}

.opt-key {
  font-size: 28rpx;
  font-weight: 700;
  color: #374151;
  width: 48rpx;
}

.opt-content {
  font-size: 28rpx;
  color: #374151;
  flex: 1;
}

.subjective-area {
  margin-bottom: 32rpx;
}

.answer-input {
  min-height: 200rpx;
  background: #f9fafb;
  border-radius: 12rpx;
  padding: 20rpx;
  font-size: 28rpx;
  border: 1rpx solid #e5e7eb;
}

.submit-btn, .next-btn, .finish-btn, .back-btn {
  width: 100%;
  height: 80rpx;
  line-height: 80rpx;
  border-radius: 40rpx;
  font-size: 28rpx;
  border: none;
  text-align: center;
}

.submit-btn, .next-btn {
  background: #1d4ed8;
  color: #fff;
}

.finish-btn {
  background: #10b981;
  color: #fff;
}

.feedback-section {
  margin-top: 32rpx;
  padding-top: 32rpx;
  border-top: 1rpx solid #e5e7eb;
}

.feedback-score {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16rpx 20rpx;
  border-radius: 12rpx;
  margin-bottom: 20rpx;
  font-size: 28rpx;
  font-weight: 600;
}

.feedback-score.correct {
  background: #ecfdf5;
  color: #10b981;
}

.feedback-score.wrong {
  background: #fef2f2;
  color: #ef4444;
}

.section-label {
  font-size: 24rpx;
  color: #6b7280;
  margin-bottom: 8rpx;
  display: block;
}

.comment-text {
  font-size: 26rpx;
  color: #374151;
  line-height: 1.6;
}

.back-btn {
  background: #f3f4f6;
  color: #374151;
  margin-top: 20rpx;
}

.result-panel {
  text-align: center;
  padding: 48rpx;
}

.result-emoji {
  font-size: 96rpx;
  display: block;
  margin-bottom: 20rpx;
}

.result-title {
  font-size: 36rpx;
  font-weight: 700;
  color: #111827;
  display: block;
  margin-bottom: 12rpx;
}

.result-score {
  font-size: 28rpx;
  color: #6b7280;
  display: block;
  margin-bottom: 32rpx;
}
</style>
