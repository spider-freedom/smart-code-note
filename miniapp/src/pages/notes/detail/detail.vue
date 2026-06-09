<template>
  <view class="page-container">
    <view v-if="note" class="card">
      <text class="detail-title">{{ note.title }}</text>
      <view class="detail-meta">
        <text class="meta-item" v-if="note.category">分类: {{ note.category }}</text>
        <text class="meta-item" v-if="note.tags">标签: {{ note.tags }}</text>
        <text class="meta-item">{{ note.fileType }}</text>
      </view>

      <view class="content-section">
        <text class="section-label">原文内容</text>
        <view class="content-box">
          <text class="content-text">{{ note.originalContent }}</text>
        </view>
      </view>

      <view class="content-section" v-if="note.cleanContent">
        <text class="section-label">清洗后内容</text>
        <view class="content-box">
          <text class="content-text">{{ note.cleanContent }}</text>
        </view>
      </view>

      <view class="detail-actions">
        <button class="action-btn primary" @tap="handleGenerateKnowledge" :loading="generating">
          生成知识点
        </button>
        <button class="action-btn" @tap="handleReparse" :loading="reparsing">
          重新解析
        </button>
        <button class="action-btn danger" @tap="handleDelete">
          删除
        </button>
      </view>
    </view>

    <view v-else class="empty-state">
      <text>加载中...</text>
    </view>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { noteApi } from '@/api/note'
import { knowledgeApi } from '@/api/knowledge'

const note = ref(null)
const generating = ref(false)
const reparsing = ref(false)
const id = ref(null)

onMounted((options) => {
  // uni-app passes query via the current page stack
  const pages = getCurrentPages()
  const currentPage = pages[pages.length - 1]
  id.value = currentPage.options?.id
  if (id.value) fetchNote()
})

async function fetchNote() {
  try {
    note.value = await noteApi.detail(id.value)
  } catch (e) { /* ignore */ }
}

async function handleGenerateKnowledge() {
  generating.value = true
  try {
    const result = await knowledgeApi.generate({ noteId: note.value.id })
    uni.showToast({ title: `成功生成 ${result.length} 个知识点`, icon: 'success' })
  } catch (e) { /* ignore */ } finally {
    generating.value = false
  }
}

async function handleReparse() {
  reparsing.value = true
  try {
    note.value = await noteApi.reparse(id.value)
    uni.showToast({ title: '重新解析成功', icon: 'success' })
  } catch (e) { /* ignore */ } finally {
    reparsing.value = false
  }
}

async function handleDelete() {
  const res = await new Promise(resolve => {
    uni.showModal({ title: '确认删除', content: '确定要删除这篇笔记吗？', success: resolve })
  })
  if (!res.confirm) return
  try {
    await noteApi.delete(id.value)
    uni.showToast({ title: '已删除', icon: 'success' })
    setTimeout(() => uni.navigateBack(), 1000)
  } catch (e) { /* ignore */ }
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

.content-section {
  margin-bottom: 24rpx;
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
  max-height: 500rpx;
  overflow-y: auto;
}

.content-text {
  font-size: 26rpx;
  color: #374151;
  line-height: 1.8;
  white-space: pre-wrap;
}

.detail-actions {
  display: flex;
  flex-direction: column;
  gap: 16rpx;
  margin-top: 32rpx;
}

.action-btn {
  width: 100%;
  height: 80rpx;
  line-height: 80rpx;
  border-radius: 40rpx;
  font-size: 28rpx;
  border: none;
  text-align: center;
}

.action-btn.primary {
  background: #1d4ed8;
  color: #fff;
}

.action-btn {
  background: #f3f4f6;
  color: #374151;
}

.action-btn.danger {
  background: #fef2f2;
  color: #ef4444;
}
</style>
