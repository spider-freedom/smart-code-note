<template>
  <view class="feedback" v-if="result">
    <view class="feedback-header" :class="{ correct: result.correct, wrong: !result.correct }">
      <text class="fb-status">{{ result.correct ? '✓ 回答正确' : '✗ 回答错误' }}</text>
      <text class="fb-score" v-if="result.score != null">得分: {{ result.score }}</text>
    </view>
    <view class="fb-comment" v-if="result.comment">
      <text class="fb-label">AI 点评</text>
      <text class="fb-text">{{ result.comment }}</text>
    </view>
  </view>
</template>

<script setup>
defineProps({
  result: { type: Object, default: null }
})
</script>

<style scoped>
.feedback {
  margin-top: 24rpx;
}

.feedback-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16rpx 20rpx;
  border-radius: 12rpx;
  margin-bottom: 16rpx;
}

.feedback-header.correct {
  background: #ecfdf5;
}

.feedback-header.correct .fb-status { color: #10b981; }

.feedback-header.wrong {
  background: #fef2f2;
}

.feedback-header.wrong .fb-status { color: #ef4444; }

.fb-status {
  font-size: 28rpx;
  font-weight: 600;
}

.fb-score {
  font-size: 26rpx;
  color: #6b7280;
}

.fb-label {
  font-size: 24rpx;
  color: #6b7280;
  display: block;
  margin-bottom: 8rpx;
}

.fb-text {
  font-size: 26rpx;
  color: #374151;
  line-height: 1.6;
}
</style>
