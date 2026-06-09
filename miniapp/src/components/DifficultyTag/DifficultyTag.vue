<template>
  <text class="difficulty-tag" :class="level" :style="tagStyle">{{ label }}</text>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  level: { type: String, default: 'medium' }
})

const label = computed(() => {
  const map = { easy: '简单', medium: '中等', hard: '困难' }
  return map[props.level] || props.level
})

const tagStyle = computed(() => {
  const colors = {
    easy: { bg: '#ecfdf5', color: '#10b981' },
    medium: { bg: '#fef9c3', color: '#f59e0b' },
    hard: { bg: '#fef2f2', color: '#ef4444' }
  }
  const c = colors[props.level] || colors.medium
  return { background: c.bg, color: c.color }
})
</script>

<style scoped>
.difficulty-tag {
  font-size: 22rpx;
  padding: 4rpx 16rpx;
  border-radius: 6rpx;
  font-weight: 500;
}
</style>
