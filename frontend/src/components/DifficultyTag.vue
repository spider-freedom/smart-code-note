<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  difficulty?: string | null
}>()

const labelMap: Record<string, string> = {
  easy: '简单',
  simple: '简单',
  medium: '中等',
  hard: '困难',
  difficult: '困难',
}

const typeMap: Record<string, 'success' | 'warning' | 'danger' | 'info'> = {
  easy: 'success',
  simple: 'success',
  medium: 'warning',
  hard: 'danger',
  difficult: 'danger',
}

const displayLabel = computed(() => {
  if (!props.difficulty) return '未设置'
  return labelMap[props.difficulty.toLowerCase()] || props.difficulty
})
</script>

<template>
  <el-tag :type="typeMap[(props.difficulty || '').toLowerCase()] || 'info'">
    {{ displayLabel }}
  </el-tag>
</template>
