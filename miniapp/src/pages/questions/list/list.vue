<template>
  <view class="page-container">
    <!-- Tab 切换 -->
    <view class="tabs">
      <view class="tab" :class="{ active: currentTab === 0 }" @tap="switchTab(0)">全部题目</view>
      <view class="tab" :class="{ active: currentTab === 1 }" @tap="switchTab(1)">错题本</view>
    </view>

    <!-- 全部题目 -->
    <view v-if="currentTab === 0">
      <view v-if="questions.length" class="question-list">
        <view class="question-card card" v-for="q in questions" :key="q.id" @tap="goDetail(q.id)">
          <view class="q-header">
            <text class="q-type">{{ q.questionType }}</text>
            <text class="q-difficulty" v-if="q.difficulty">{{ q.difficulty }}</text>
          </view>
          <text class="q-content text-ellipsis">{{ q.content }}</text>
        </view>
      </view>
      <view v-else class="empty-state">
        <text class="empty-icon">📝</text>
        <text class="empty-text">暂无题目</text>
      </view>
    </view>

    <!-- 错题本 -->
    <view v-if="currentTab === 1">
      <view v-if="wrongQuestions.length" class="question-list">
        <view class="wrong-card card" v-for="wq in wrongQuestions" :key="wq.id">
          <view class="q-header">
            <text class="q-type">{{ wq.questionType }}</text>
            <text class="wrong-count">错 {{ wq.wrongCount }} 次</text>
          </view>
          <text class="q-content text-ellipsis">{{ wq.questionContent }}</text>
          <view class="wrong-actions">
            <text class="action-link" @tap="retryWrong(wq.id)">重练</text>
            <text class="action-link" @tap="markMastered(wq.id)">已掌握</text>
          </view>
        </view>
      </view>
      <view v-else class="empty-state">
        <text class="empty-icon">🎉</text>
        <text class="empty-text">没有错题，继续保持</text>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { questionApi } from '@/api/question'
import { wrongQuestionApi } from '@/api/wrongQuestion'

const currentTab = ref(0)
const questions = ref([])
const wrongQuestions = ref([])
const questionPage = ref(1)
const wrongPage = ref(1)

onMounted(() => {
  fetchQuestions(true)
  fetchWrongQuestions(true)
})

function switchTab(index) {
  currentTab.value = index
}

async function fetchQuestions(reset) {
  if (reset) questionPage.value = 1
  try {
    const result = await questionApi.list({ pageNum: questionPage.value, pageSize: 20 })
    if (reset) {
      questions.value = result.records || []
    } else {
      questions.value.push(...(result.records || []))
    }
  } catch (e) { /* ignore */ }
}

async function fetchWrongQuestions(reset) {
  if (reset) wrongPage.value = 1
  try {
    const result = await wrongQuestionApi.list({ pageNum: wrongPage.value, pageSize: 20 })
    if (reset) {
      wrongQuestions.value = result.records || []
    } else {
      wrongQuestions.value.push(...(result.records || []))
    }
  } catch (e) { /* ignore */ }
}

function goDetail(id) {
  uni.navigateTo({ url: `/pages/questions/detail/detail?id=${id}` })
}

async function retryWrong(id) {
  try {
    await wrongQuestionApi.retry(id)
    uni.showToast({ title: '已加入练习队列', icon: 'success' })
  } catch (e) { /* ignore */ }
}

async function markMastered(id) {
  try {
    await wrongQuestionApi.markMastered(id)
    uni.showToast({ title: '已标记为掌握', icon: 'success' })
    fetchWrongQuestions(true)
  } catch (e) { /* ignore */ }
}
</script>

<style scoped>
.tabs {
  display: flex;
  background: #fff;
  border-radius: 16rpx;
  padding: 8rpx;
  margin-bottom: 20rpx;
  box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.04);
}

.tab {
  flex: 1;
  text-align: center;
  padding: 16rpx 0;
  font-size: 28rpx;
  color: #6b7280;
  border-radius: 12rpx;
}

.tab.active {
  background: #dbeafe;
  color: #1d4ed8;
  font-weight: 600;
}

.question-list {
  display: flex;
  flex-direction: column;
  gap: 16rpx;
}

.q-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8rpx;
}

.q-type {
  font-size: 22rpx;
  color: #9ca3af;
  background: #f3f4f6;
  padding: 2rpx 12rpx;
  border-radius: 6rpx;
}

.q-difficulty {
  font-size: 22rpx;
  color: #6b7280;
}

.q-content {
  font-size: 28rpx;
  color: #374151;
  line-height: 1.6;
}

.wrong-count {
  font-size: 22rpx;
  color: #ef4444;
  font-weight: 600;
}

.wrong-actions {
  display: flex;
  gap: 24rpx;
  margin-top: 16rpx;
  padding-top: 16rpx;
  border-top: 1rpx solid #f3f4f6;
}

.action-link {
  font-size: 26rpx;
  color: #2563eb;
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
