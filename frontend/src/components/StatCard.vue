<script setup lang="ts">
import type { Component } from 'vue'

defineProps<{
  title: string
  value: string | number
  caption: string
  icon: Component
}>()
</script>

<template>
  <article class="stat-card">
    <div class="stat-card__icon">
      <el-icon><component :is="icon" /></el-icon>
    </div>
    <div>
      <p>{{ title }}</p>
      <strong>{{ value }}</strong>
      <span>{{ caption }}</span>
    </div>
  </article>
</template>
