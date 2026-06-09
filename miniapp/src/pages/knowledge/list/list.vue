<template>
  <view class="page-container">
    <view class="search-bar">
      <input class="search-input" v-model="keyword" placeholder="搜索知识点..." @confirm="search" />
    </view>

    <view v-if="list.length" class="knowledge-list">
      <view class="knowledge-card card" v-for="item in list" :key="item.id" @tap="goDetail(item.id)">
        <view class="k-header">
          <text class="k-title text-ellipsis">{{ item.title }}</text>
          <text class="k-type">{{ item.type }}</text>
        </view>
        <text class="k-summary" v-if="item.summary">{{ item.summary }}</text>
        <view class="k-footer">
          <view class="mastery-stars">
            <text v-for="i in 5" :key="i" :style="{ color: i <= (item.masteryLevel || 0) ? '#f59e0b' : '#e5e7eb' }">★</text>
          </view>
          <text class="k-difficulty" v-if="item.difficulty">{{ item.difficulty }}</text>
        </view>
      </view>
    </view>

    <view v-else class="empty-state">
      <text class="empty-icon">📚</text>
      <text class="empty-text">暂无知识点</text>
    </view>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { knowledgeApi } from '@/api/knowledge'

const list = ref([])
const keyword = ref('')
const pageNum = ref(1)

onMounted(() => fetchList(true))

async function fetchList(reset) {
  if (reset) {
    pageNum.value = 1
    list.value = []
  }
  try {
    const result = await knowledgeApi.list({
      pageNum: pageNum.value,
      pageSize: 20,
      keyword: keyword.value || undefined
    })
    if (reset) {
      list.value = result.records || []
    } else {
      list.value.push(...(result.records || []))
    }
  } catch (e) { /* ignore */ }
}

function search() {
  fetchList(true)
}

function goDetail(id) {
  uni.navigateTo({ url: `/pages/knowledge/detail/detail?id=${id}` })
}
</script>

<style scoped>
.search-bar {
  margin-bottom: 20rpx;
}

.search-input {
  height: 72rpx;
  background: #fff;
  border-radius: 36rpx;
  padding: 0 28rpx;
  font-size: 28rpx;
  box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.04);
}

.knowledge-list {
  display: flex;
  flex-direction: column;
  gap: 16rpx;
}

.k-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8rpx;
}

.k-title {
  font-size: 30rpx;
  font-weight: 600;
  color: #111827;
  flex: 1;
}

.k-type {
  font-size: 22rpx;
  color: #9ca3af;
  background: #f3f4f6;
  padding: 2rpx 12rpx;
  border-radius: 6rpx;
}

.k-summary {
  font-size: 24rpx;
  color: #6b7280;
  line-height: 1.6;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  margin-bottom: 12rpx;
}

.k-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.mastery-stars {
  font-size: 28rpx;
}

.k-difficulty {
  font-size: 22rpx;
  color: #6b7280;
  background: #f3f4f6;
  padding: 4rpx 12rpx;
  border-radius: 6rpx;
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
