<template>
  <view class="page-container">
    <view v-if="knowledge" class="card">
      <text class="detail-title">{{ knowledge.title }}</text>
      <view class="detail-meta">
        <text class="meta-item">{{ knowledge.type }}</text>
        <text class="meta-item" v-if="knowledge.difficulty">难度: {{ knowledge.difficulty }}</text>
        <text class="meta-item">掌握度: {{ '★'.repeat(knowledge.masteryLevel || 0) }}{{ '☆'.repeat(5 - (knowledge.masteryLevel || 0)) }}</text>
      </view>
      <view class="content-section" v-if="knowledge.summary">
        <text class="section-label">摘要</text>
        <view class="content-box">
          <text class="content-text">{{ knowledge.summary }}</text>
        </view>
      </view>
      <button class="primary-btn" @tap="handleGenerateQuestions" :loading="generating">
        生成题目
      </button>
    </view>
    <view v-else class="empty-state"><text>加载中...</text></view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { knowledgeApi } from '@/api/knowledge'
import { questionApi } from '@/api/question'

const knowledge = ref(null)
const generating = ref(false)
let id = null

// uni-app page lifecycle
const pages = getCurrentPages()
const currentPage = pages[pages.length - 1]
id = currentPage.options?.id

if (id) {
  knowledgeApi.detail(id).then(r => { knowledge.value = r }).catch(() => {})
}

async function handleGenerateQuestions() {
  generating.value = true
  try {
    await questionApi.generate({ knowledgeId: knowledge.value.id, count: 3 })
    uni.showToast({ title: '题目生成成功', icon: 'success' })
  } catch (e) { /* ignore */ } finally {
    generating.value = false
  }
}
</script>

<style scoped>
.detail-title {
  font-size: 36rpx;
  font-weight: 700;
  color: #111827;
  display: block;
  margin-bottom: 16rpx;
}

.detail-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 16rpx;
  margin-bottom: 24rpx;
}

.meta-item {
  font-size: 24rpx;
  color: #6b7280;
  background: #f3f4f6;
  padding: 4rpx 16rpx;
  border-radius: 6rpx;
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
}

.primary-btn {
  width: 100%;
  height: 80rpx;
  line-height: 80rpx;
  background: #1d4ed8;
  color: #fff;
  border-radius: 40rpx;
  font-size: 28rpx;
  border: none;
  text-align: center;
  margin-top: 32rpx;
}

.empty-state {
  display: flex;
  justify-content: center;
  padding-top: 200rpx;
}
</style>
