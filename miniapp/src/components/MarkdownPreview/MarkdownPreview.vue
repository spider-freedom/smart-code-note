<template>
  <view class="markdown-body" v-html="renderedHtml"></view>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  content: { type: String, default: '' }
})

const renderedHtml = computed(() => {
  // Simple markdown-to-text renderer for mini program
  // For full markdown support, integrate mp-html component
  let text = props.content || ''
  text = text.replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
  text = text.replace(/\*(.+?)\*/g, '<em>$1</em>')
  text = text.replace(/`(.+?)`/g, '<code>$1</code>')
  text = text.replace(/\n\n/g, '<br/><br/>')
  text = text.replace(/\n/g, '<br/>')
  return text
})
</script>

<style scoped>
.markdown-body {
  font-size: 28rpx;
  color: #374151;
  line-height: 1.8;
}

.markdown-body :deep(strong) {
  font-weight: 700;
  color: #111827;
}

.markdown-body :deep(code) {
  background: #f3f4f6;
  padding: 2rpx 8rpx;
  border-radius: 4rpx;
  font-size: 24rpx;
  color: #ef4444;
}

.markdown-body :deep(em) {
  font-style: italic;
  color: #6b7280;
}
</style>
