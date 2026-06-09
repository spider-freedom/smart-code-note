<template>
  <view class="stat-card" :style="cardStyle">
    <text class="stat-value" :style="{ color: accent || '#1d4ed8' }">{{ value }}</text>
    <text class="stat-label">{{ label }}</text>
  </view>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  value: { type: [String, Number], default: '' },
  label: { type: String, default: '' },
  accent: { type: String, default: '' }
})

const cardStyle = computed(() => ({}))
</script>

<style scoped>
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
  display: block;
  margin-bottom: 4rpx;
}

.stat-label {
  font-size: 24rpx;
  color: #6b7280;
}
</style>
