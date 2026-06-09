<script setup lang="ts">
const props = defineProps<{
  level?: number | null
}>()

const labels = ['未学习', '初步了解', '需要复习', '基本掌握', '熟练掌握', '长期掌握']
const types = ['info', 'warning', 'danger', 'primary', 'success', 'success'] as const
</script>

<template>
  <el-tag :type="types[props.level ?? 0] || 'info'">
    {{ labels[props.level ?? 0] || '未知' }}
  </el-tag>
</template>
