<script setup lang="ts">
import { computed } from 'vue'
import { RouterLink, RouterView, useRoute, useRouter } from 'vue-router'
import {
  Collection,
  EditPen,
  House,
  Notebook,
  Reading,
  Tickets,
  User,
} from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const navItems = [
  { path: '/dashboard', label: '工作台', icon: House },
  { path: '/notes', label: '笔记', icon: Notebook },
  { path: '/knowledge', label: '知识点', icon: Collection },
  { path: '/questions', label: '题目', icon: Tickets },
  { path: '/practice', label: '练习', icon: EditPen },
  { path: '/reviews', label: '复习', icon: Reading },
  { path: '/profile', label: '个人', icon: User },
]

const activeTitle = computed(() => route.meta.title || '智能笔记学习平台')

const logout = () => {
  authStore.logout()
  router.push('/login')
}
</script>

<template>
  <div class="main-layout">
    <aside class="main-layout__sidebar">
      <div class="main-layout__brand">
        <div class="main-layout__logo">
          <svg viewBox="0 0 48 48" fill="none" xmlns="http://www.w3.org/2000/svg">
            <defs>
              <linearGradient id="logo-grad" x1="0" y1="0" x2="48" y2="48" gradientUnits="userSpaceOnUse">
                <stop stop-color="#7c3aed"/>
                <stop offset="1" stop-color="#5b21b6"/>
              </linearGradient>
            </defs>
            <rect width="48" height="48" rx="12" fill="url(#logo-grad)"/>
            <path d="M14 12h10l3 2.5v19l-3-2.5H14z" fill="#fff" fill-opacity=".95"/>
            <path d="M24 12h10v19l-3-2.5" fill="#fff" fill-opacity=".55"/>
            <circle cx="36" cy="14" r="2.5" fill="#ddd6fe"/>
          </svg>
        </div>
        <div class="main-layout__brand-text">
          <span class="main-layout__brand-name">智能笔记</span>
          <span class="main-layout__brand-sub">学习平台</span>
        </div>
      </div>

      <nav class="main-layout__nav" aria-label="主导航">
        <RouterLink v-for="item in navItems" :key="item.path" :to="item.path">
          <el-icon><component :is="item.icon" /></el-icon>
          <span>{{ item.label }}</span>
        </RouterLink>
      </nav>
    </aside>

    <section class="main-layout__content">
      <header class="main-layout__header">
        <h1>{{ activeTitle }}</h1>
        <div class="main-layout__actions">
          <span class="main-layout__user">{{ authStore.displayName }}</span>
          <el-button text @click="router.push('/profile')">设置</el-button>
          <el-button type="primary" plain size="small" @click="logout">退出</el-button>
        </div>
      </header>
      <main class="main-layout__main">
        <RouterView />
      </main>
    </section>
  </div>
</template>

<style scoped>
.main-layout__logo svg {
  width: 32px;
  height: 32px;
  flex-shrink: 0;
}

.main-layout__brand {
  display: flex;
  align-items: center;
  gap: 10px;
  padding-bottom: 20px;
  margin-bottom: 4px;
  border-bottom: 1px solid var(--gray-100);
}

.main-layout__brand-text {
  display: flex;
  flex-direction: column;
  line-height: 1.2;
}

.main-layout__brand-name {
  font-size: 15px;
  font-weight: 700;
  color: var(--gray-900);
  letter-spacing: -0.01em;
}

.main-layout__brand-sub {
  font-size: 11px;
  color: var(--gray-400);
  font-weight: 500;
  letter-spacing: 0.04em;
  text-transform: uppercase;
}
</style>
