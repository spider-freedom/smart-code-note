<template>
  <view class="page-container">
    <view v-if="question" class="card">
      <view class="q-header">
        <text class="q-type-badge">{{ question.questionType }}</text>
        <text class="q-difficulty" v-if="question.difficulty">难度: {{ question.difficulty }}</text>
      </view>

      <text class="q-content">{{ question.content }}</text>

      <!-- 选择题选项 -->
      <view class="options-section" v-if="question.options && question.options.length">
        <view class="option-item" v-for="opt in question.options" :key="opt.optionKey">
          <text class="option-key">{{ opt.optionKey }}.</text>
          <text class="option-content">{{ opt.optionContent }}</text>
          <text class="option-correct" v-if="opt.correct">✓</text>
        </view>
      </view>

      <view class="answer-section" v-if="question.standardAnswer">
        <text class="section-label">标准答案</text>
        <view class="content-box">
          <text class="content-text">{{ question.standardAnswer }}</text>
        </view>
      </view>

      <view class="analysis-section" v-if="question.analysis">
        <text class="section-label">解析</text>
        <view class="content-box">
          <text class="content-text">{{ question.analysis }}</text>
        </view>
      </view>
    </view>

    <view v-else class="empty-state"><text>加载中...</text></view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { questionApi } from '@/api/question'

const question = ref(null)

const pages = getCurrentPages()
const currentPage = pages[pages.length - 1]
const id = currentPage.options?.id

if (id) {
  questionApi.detail(id).then(r => { question.value = r }).catch(() => {})
}
</script>

<style scoped>
.q-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20rpx;
}

.q-type-badge {
  font-size: 24rpx;
  color: #9ca3af;
  background: #f3f4f6;
  padding: 4rpx 16rpx;
  border-radius: 6rpx;
}

.q-difficulty {
  font-size: 24rpx;
  color: #6b7280;
}

.q-content {
  font-size: 30rpx;
  color: #111827;
  line-height: 1.8;
  display: block;
  margin-bottom: 24rpx;
}

.options-section {
  margin-bottom: 24rpx;
}

.option-item {
  display: flex;
  align-items: flex-start;
  gap: 12rpx;
  padding: 12rpx 0;
  border-bottom: 1rpx solid #f3f4f6;
}

.option-key {
  font-size: 26rpx;
  font-weight: 600;
  color: #374151;
  width: 40rpx;
}

.option-content {
  font-size: 26rpx;
  color: #374151;
  flex: 1;
}

.option-correct {
  font-size: 24rpx;
  color: #10b981;
  font-weight: 700;
}

.section-label {
  font-size: 26rpx;
  color: #6b7280;
  margin-bottom: 12rpx;
  display: block;
}

.content-box {
  background: #f9fafb;
  border-radius: 12rpx;
  padding: 20rpx;
}

.content-text {
  font-size: 26rpx;
  color: #374151;
  line-height: 1.8;
  white-space: pre-wrap;
}

.answer-section, .analysis-section {
  margin-bottom: 24rpx;
}

.empty-state {
  display: flex;
  justify-content: center;
  padding-top: 200rpx;
}
</style>
