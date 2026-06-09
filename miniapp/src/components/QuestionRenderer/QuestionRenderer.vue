<template>
  <view class="question-renderer">
    <view class="q-header">
      <text class="q-type-tag">{{ question.questionType }}</text>
      <DifficultyTag :level="question.difficulty || 'medium'" />
    </view>

    <text class="q-content">{{ question.content }}</text>

    <view class="options" v-if="question.options && question.options.length">
      <view class="option-row" v-for="opt in question.options" :key="opt.optionKey">
        <text class="opt-key">{{ opt.optionKey }}</text>
        <text class="opt-text">{{ opt.optionContent }}</text>
        <text class="opt-correct" v-if="showAnswer && opt.correct">✓</text>
      </view>
    </view>

    <view class="answer-reveal" v-if="showAnswer && question.standardAnswer">
      <text class="reveal-label">正确答案</text>
      <text class="reveal-text">{{ question.standardAnswer }}</text>
    </view>
  </view>
</template>

<script setup>
import DifficultyTag from '@/components/DifficultyTag/DifficultyTag.vue'

defineProps({
  question: { type: Object, required: true },
  showAnswer: { type: Boolean, default: false }
})
</script>

<style scoped>
.q-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16rpx;
}

.q-type-tag {
  font-size: 22rpx;
  color: #9ca3af;
  background: #f3f4f6;
  padding: 2rpx 12rpx;
  border-radius: 6rpx;
}

.q-content {
  font-size: 28rpx;
  color: #111827;
  line-height: 1.8;
  display: block;
  margin-bottom: 20rpx;
}

.options {
  display: flex;
  flex-direction: column;
  gap: 12rpx;
}

.option-row {
  display: flex;
  align-items: flex-start;
  gap: 12rpx;
  padding: 12rpx 0;
}

.opt-key {
  font-size: 26rpx;
  font-weight: 700;
  color: #374151;
  width: 40rpx;
}

.opt-text {
  font-size: 26rpx;
  color: #374151;
  flex: 1;
}

.opt-correct {
  color: #10b981;
  font-size: 24rpx;
  font-weight: 700;
}

.answer-reveal {
  margin-top: 20rpx;
  padding: 16rpx;
  background: #ecfdf5;
  border-radius: 12rpx;
}

.reveal-label {
  font-size: 24rpx;
  color: #6b7280;
  display: block;
  margin-bottom: 4rpx;
}

.reveal-text {
  font-size: 26rpx;
  color: #374151;
}
</style>
