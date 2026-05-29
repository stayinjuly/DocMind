import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '../stores/user'

import MainLayout from '../layouts/MainLayout.vue'
import AuthLayout from '../layouts/AuthLayout.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      component: MainLayout,
      meta: { requiresAuth: true, title: 'DocMind - 智能文档问答' },
      children: [
        {
          path: '',
          name: 'chat',
          component: () => import('../views/ChatView.vue'),
          meta: { title: '对话问答 - DocMind' },
        },
        {
          path: 'documents',
          name: 'documents',
          component: () => import('../views/DocumentsView.vue'),
          meta: { title: '文档管理 - DocMind' },
        },
      ],
    },
    {
      path: '/login',
      component: AuthLayout,
      children: [
        {
          path: '',
          name: 'login',
          component: () => import('../views/LoginView.vue'),
          meta: { title: '登录 - DocMind' },
        },
      ],
    },
    {
      path: '/:pathMatch(.*)*',
      name: 'not-found',
      component: () => import('../views/NotFoundView.vue'),
      meta: { title: '页面不存在 - DocMind' },
    },
  ],
})

router.beforeEach((to) => {
  document.title = (to.meta.title as string) || 'DocMind'

  const userStore = useUserStore()
  const requiresAuth = to.matched.some((r) => r.meta.requiresAuth)

  if (requiresAuth && !userStore.isLoggedIn) {
    return { path: '/login' }
  }

  if (to.path === '/login' && userStore.isLoggedIn) {
    return { path: '/' }
  }
})

export default router
