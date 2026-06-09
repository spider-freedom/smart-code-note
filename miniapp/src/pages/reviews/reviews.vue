<template>
  <view class="page-container">
    <view v-if="tasks.length" class="review-list">
      <view class="review-card card" v-for="task in tasks" :key="task.id">
        <view class="review-header">
          <text class="review-title">{{ task.knowledgeTitle }}</text>
          <text class="review-mastery">掌握度 {{ task.masteryLevel }}/5</text>
        </view>

        <view class="review-actions" v-if="!task.submitted">
          <view class="result-options">
            <view class="result-option" :class="{ selected: task.selectedResult === 'easy' }" @tap="selectResult(task, 'easy')">
              <text>轻松</text>
            </view>
            <view class="result-option" :class="{ selected: task.selectedResult === 'medium' }" @tap="selectResult(task, 'medium')">
              <text>一般</text>
            </view>
            <view class="result-option" :class="{ selected: task.selectedResult === 'hard' }" @tap="selectResult(task, 'hard')">
              <text>困难</text>
            </view>
          </view>
          <button class="submit-review-btn" @tap="submitReview(task)" :loading="task.submitting">
            提交
          </button>
        </view>

        <view class="review-done" v-else>
          <text class="done-text">✓ 已提交</text>
        </view>
      </view>
    </view>

    <view v-else class="empty-state">
      <text class="empty-icon">📅</text>
      <text class="empty-text">今日没有待复习的任务</text>
    </view>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { reviewApi } from '@/api/review'

const tasks = ref([])

onMounted(async () => {
  try {
    const result = await reviewApi.getTodayTasks()
    tasks.value = (result || []).map(t => ({
      ...t,
      selectedResult: null,
      submitting: false,
      submitted: false
    }))
  } catch (e) { /* ignore */ }
})

function selectResult(task, result) {
  task.selectedResult = result
}

async function submitReview(task) {
  if (!task.selectedResult) {
    uni.showToast({ title: '请选择复习结果', icon: 'none' })
    return
  }
  task.submitting = true
  try {
    await reviewApi.submit({
      knowledgeId: task.knowledgeId,
      reviewResult: task.selectedResult
    })
    task.submitted = true
    uni.showToast({ title: '提交成功', icon: 'success' })
  } catch (e) { /* ignore */ } finally {
    task.submitting = false
  }
}
</script>

<style scoped>
.review-list {
  display: flex;
  flex-direction: column;
  gap: 16rpx;
}

.review-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20rpx;
}

.review-title {
  font-size: 30rpx;
  font-weight: 600;
  color: #111827;
}

.review-mastery {
  font-size: 24rpx;
  color: #6b7280;
}

.result-options {
  display: flex;
  gap: 16rpx;
  margin-bottom: 20rpx;
}

.result-option {
  flex: 1;
  text-align: center;
  padding: 16rpx 0;
  background: #f9fafb;
  border-radius: 12rpx;
  font-size: 26rpx;
  color: #6b7280;
  border: 2rpx solid transparent;
}

.result-option.selected {
  background: #dbeafe;
  color: #1d4ed8;
  border-color: #1d4ed8;
}

.submit-review-btn {
  width: 100%;
  height: 72rpx;
  line-height: 72rpx;
  background: #1d4ed8;
  color: #fff;
  border-radius: 36rpx;
  font-size: 28rpx;
  border: none;
  text-align: center;
}

.review-done {
  text-align: center;
  padding: 16rpx 0;
}

.done-text {
  font-size: 26rpx;
  color: #10b981;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding-top: 160rpx;
}

.empty-icon {
  font-size: 80rpx;
  margin-bottom: 20rpx;
}

.empty-text {
  font-size: 28rpx;
  color: #9ca3af;
}
</style>
