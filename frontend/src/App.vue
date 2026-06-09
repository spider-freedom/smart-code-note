<script setup lang="ts">
import { useRoute } from 'vue-router'
import { computed } from 'vue'
import ChatBubble from '@/components/ChatBubble.vue'
import { PUBLIC_ROUTES } from '@/constants'

const route = useRoute()
const isLoggedIn = computed(() => !PUBLIC_ROUTES.includes(route.path))
</script>

<template>
  <RouterView />
  <ChatBubble v-if="isLoggedIn" />
</template>
