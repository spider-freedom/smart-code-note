<template>
  <view class="page-container">
    <!-- 欢迎区域 -->
    <view class="hero card">
      <text class="hero-greeting">你好，{{ userInfo?.nickname || '同学' }} 👋</text>
      <text class="hero-subtitle">继续你的学习之旅吧</text>
    </view>

    <!-- 统计卡片 -->
    <view class="stat-grid">
      <view class="stat-card">
        <text class="stat-value">{{ overview?.totalNotes || 0 }}</text>
        <text class="stat-label">笔记</text>
      </view>
      <view class="stat-card">
        <text class="stat-value">{{ overview?.totalKnowledgePoints || 0 }}</text>
        <text class="stat-label">知识点</text>
      </view>
      <view class="stat-card">
        <text class="stat-value">{{ overview?.masteredKnowledgePoints || 0 }}</text>
        <text class="stat-label">已掌握</text>
      </view>
      <view class="stat-card accent">
        <text class="stat-value">{{ overview?.todayCorrectRate || 0 }}%</text>
        <text class="stat-label">正确率</text>
      </view>
    </view>

    <!-- 今日任务 -->
    <view class="card" v-if="todayTasks && todayTasks.length">
      <text class="section-title">今日复习任务</text>
      <view class="task-item" v-for="task in todayTasks.slice(0, 5)" :key="task.id">
        <view class="task-info">
          <text class="task-title">{{ task.knowledgeTitle }}</text>
          <text class="task-meta">掌握度: {{ '★'.repeat(task.masteryLevel || 0) }}{{ '☆'.repeat(5 - (task.masteryLevel || 0)) }}</text>
        </view>
        <text class="task-arrow">→</text>
      </view>
      <view class="section-more" v-if="todayTasks.length > 5" @tap="goToReviews">
        <text>查看全部 {{ todayTasks.length }} 个任务</text>
      </view>
    </view>

    <!-- 薄弱知识点 -->
    <view class="card" v-if="weakKnowledge && weakKnowledge.length">
      <text class="section-title">需要加强的知识点</text>
      <view class="weak-item" v-for="item in weakKnowledge.slice(0, 3)" :key="item.id">
        <view class="weak-bar">
          <view class="weak-bar-fill" :style="{ width: (item.masteryLevel / 5 * 100) + '%' }"></view>
        </view>
        <text class="weak-title">{{ item.title }}</text>
        <text class="weak-level">Lv.{{ item.masteryLevel }}</text>
      </view>
    </view>

    <!-- AI 学习建议 -->
    <view class="card" v-if="suggestions && suggestions.length">
      <text class="section-title">AI 学习建议</text>
      <view class="suggestion-item" v-for="(tip, index) in suggestions" :key="index">
        <text class="suggestion-num">{{ index + 1 }}</text>
        <text class="suggestion-text">{{ tip }}</text>
      </view>
    </view>

    <!-- 快捷操作 -->
    <view class="quick-actions">
      <text class="section-title">快捷操作</text>
      <view class="action-grid">
        <view class="action-item" @tap="goToPage('/pages/notes/list/list')">
          <text class="action-icon">📝</text>
          <text class="action-label">上传笔记</text>
        </view>
        <view class="action-item" @tap="goToPage('/pages/practice/practice')">
          <text class="action-icon">✏️</text>
          <text class="action-label">在线练习</text>
        </view>
        <view class="action-item" @tap="goToPage('/pages/reviews/reviews')">
          <text class="action-icon">📅</text>
          <text class="action-label">复习计划</text>
        </view>
        <view class="action-item" @tap="openChat">
          <text class="action-icon">💬</text>
          <text class="action-label">问小码</text>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { useChatStore } from '@/stores/chat'
import { reportApi } from '@/api/report'
import { reviewApi } from '@/api/review'
import { userApi } from '@/api/user'

const authStore = useAuthStore()
const chatStore = useChatStore()

const userInfo = ref(null)
const overview = ref(null)
const todayTasks = ref([])
const weakKnowledge = ref([])
const suggestions = ref([])

onMounted(async () => {
  try {
    userInfo.value = await userApi.getCurrentUser()
  } catch (e) { /* ignore */ }

  try {
    const [ov, tasks, weak, sug] = await Promise.all([
      reportApi.getOverview().catch(() => null),
      reviewApi.getTodayTasks().catch(() => []),
      reportApi.getWeakKnowledge(5).catch(() => []),
      reportApi.getSuggestions().catch(() => null)
    ])
    overview.value = ov
    todayTasks.value = tasks || []
    weakKnowledge.value = weak || []
    if (sug) {
      suggestions.value = [sug.summary, ...(sug.items || [])].filter(Boolean)
    }
  } catch (e) { /* ignore */ }
})

function goToPage(url) {
  uni.navigateTo({ url })
}

function goToReviews() {
  uni.navigateTo({ url: '/pages/reviews/reviews' })
}

function openChat() {
  chatStore.open()
}
</script>

<style scoped>
.hero {
  background: linear-gradient(135deg, #1d4ed8, #3730a3);
  color: #fff;
}

.hero-greeting {
  font-size: 36rpx;
  font-weight: 700;
  display: block;
  margin-bottom: 8rpx;
}

.hero-subtitle {
  font-size: 26rpx;
  opacity: 0.85;
}

.stat-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16rpx;
  margin-bottom: 20rpx;
}

.stat-card {
  background: #fff;
  border-radius: 16rpx;
  padding: 28rpx 24rpx;
  text-align: center;
  box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.04);
}

.stat-value {
  font-size: 44rpx;
  font-weight: 700;
  color: #1d4ed8;
  display: block;
  margin-bottom: 4rpx;
}

.stat-card.accent .stat-value {
  color: #f59e0b;
}

.stat-label {
  font-size: 24rpx;
  color: #6b7280;
}

.section-title {
  font-size: 30rpx;
  font-weight: 600;
  color: #111827;
  display: block;
  margin-bottom: 20rpx;
}

.task-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16rpx 0;
  border-bottom: 1rpx solid #f3f4f6;
}

.task-info {
  flex: 1;
}

.task-title {
  font-size: 28rpx;
  color: #374151;
  display: block;
  margin-bottom: 4rpx;
}

.task-meta {
  font-size: 22rpx;
  color: #9ca3af;
}

.task-arrow {
  font-size: 28rpx;
  color: #d1d5db;
}

.section-more {
  text-align: center;
  padding-top: 16rpx;
  font-size: 26rpx;
  color: #2563eb;
}

.weak-item {
  display: flex;
  align-items: center;
  gap: 16rpx;
  padding: 12rpx 0;
}

.weak-bar {
  width: 120rpx;
  height: 8rpx;
  background: #f3f4f6;
  border-radius: 4rpx;
  overflow: hidden;
}

.weak-bar-fill {
  height: 100%;
  background: linear-gradient(90deg, #f59e0b, #2563eb);
  border-radius: 4rpx;
}

.weak-title {
  flex: 1;
  font-size: 26rpx;
  color: #374151;
}

.weak-level {
  font-size: 24rpx;
  color: #9ca3af;
}

.suggestion-item {
  display: flex;
  gap: 16rpx;
  padding: 12rpx 0;
}

.suggestion-num {
  width: 40rpx;
  height: 40rpx;
  border-radius: 50%;
  background: #dbeafe;
  color: #1d4ed8;
  font-size: 24rpx;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.suggestion-text {
  font-size: 26rpx;
  color: #374151;
  line-height: 1.6;
}

.quick-actions {
  margin-top: 8rpx;
}

.action-grid {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr 1fr;
  gap: 16rpx;
}

.action-item {
  background: #fff;
  border-radius: 16rpx;
  padding: 24rpx 12rpx;
  text-align: center;
  box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.04);
}

.action-icon {
  font-size: 44rpx;
  display: block;
  margin-bottom: 8rpx;
}

.action-label {
  font-size: 22rpx;
  color: #6b7280;
}
</style>
