import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { PUBLIC_ROUTES } from '@/constants'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      redirect: '/dashboard',
    },
    {
      path: '/',
      component: () => import('@/layouts/MainLayout.vue'),
      children: [
        {
          path: 'dashboard',
          name: 'dashboard',
          component: () => import('@/views/dashboard/DashboardView.vue'),
          meta: { title: '工作台' },
        },
        {
          path: 'notes',
          name: 'notes',
          component: () => import('@/views/notes/NoteListView.vue'),
          meta: { title: '我的笔记' },
        },
        {
          path: 'notes/:id',
          name: 'note-detail',
          component: () => import('@/views/notes/NoteDetailView.vue'),
          meta: { title: '笔记详情' },
        },
        {
          path: 'knowledge',
          name: 'knowledge',
          component: () => import('@/views/knowledge/KnowledgeListView.vue'),
          meta: { title: '知识点' },
        },
        {
          path: 'knowledge/:id',
          name: 'knowledge-detail',
          component: () => import('@/views/knowledge/KnowledgeDetailView.vue'),
          meta: { title: '知识点详情' },
        },
        {
          path: 'questions',
          name: 'questions',
          component: () => import('@/views/questions/QuestionListView.vue'),
          meta: { title: '题目管理' },
        },
        {
          path: 'questions/:id',
          name: 'question-detail',
          component: () => import('@/views/questions/QuestionDetailView.vue'),
          meta: { title: '题目详情' },
        },
        {
          path: 'practice',
          name: 'practice',
          component: () => import('@/views/practice/PracticeView.vue'),
          meta: { title: '在线练习' },
        },
        {
          path: 'reviews',
          name: 'reviews',
          component: () => import('@/views/reviews/ReviewPlanView.vue'),
          meta: { title: '复习计划' },
        },
        {
          path: 'profile',
          name: 'profile',
          component: () => import('@/views/profile/ProfileView.vue'),
          meta: { title: '个人中心' },
        },
      ],
    },
    {
      path: '/',
      component: () => import('@/layouts/AuthLayout.vue'),
      children: [
        {
          path: 'login',
          name: 'login',
          component: () => import('@/views/auth/LoginView.vue'),
          meta: { title: '登录' },
        },
        {
          path: 'register',
          name: 'register',
          component: () => import('@/views/auth/RegisterView.vue'),
          meta: { title: '注册' },
        },
      ],
    },
    {
      path: '/:pathMatch(.*)*',
      redirect: '/dashboard',
    },
  ],
})

router.beforeEach(async (to) => {
  const title = to.meta.title

  if (typeof title === 'string') {
    document.title = `${title} — 智能笔记学习平台`
  }

  const authStore = useAuthStore()

  if (PUBLIC_ROUTES.includes(to.path)) {
    if (authStore.isLoggedIn && to.path === '/login') {
      return (to.query.redirect as string) || '/dashboard'
    }

    return true
  }

  if (!authStore.isLoggedIn) {
    return {
      path: '/login',
      query: {
        redirect: to.fullPath,
      },
    }
  }

  if (!authStore.user) {
    try {
      await authStore.fetchCurrentUser()
    } catch {
      return {
        path: '/login',
        query: {
          redirect: to.fullPath,
        },
      }
    }
  }

  return true
})

export default router
