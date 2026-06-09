<template>
  <view class="level-tag" :style="tagStyle">
    <text v-for="i in 5" :key="i" class="star" :class="{ filled: i <= level }">★</text>
  </view>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  level: { type: Number, default: 0 }
})

const tagStyle = computed(() => ({
  color: props.level >= 4 ? '#f59e0b' : props.level >= 2 ? '#9ca3af' : '#d1d5db'
}))
</script>

<style scoped>
.level-tag {
  display: inline-flex;
  gap: 2rpx;
}

.star {
  font-size: 24rpx;
}

.star.filled {
  color: inherit;
}

.star:not(.filled) {
  color: #e5e7eb;
}
</style>
