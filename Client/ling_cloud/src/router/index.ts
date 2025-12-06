import { createRouter, createWebHistory } from 'vue-router'
import LoginPage from '../views/LoginPage.vue'
import Dashboard from '../views/Dashboard.vue'
import { useAuthStore } from '../stores/auth'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      redirect: '/login'
    },
    {
      path: '/login',
      name: 'login',
      component: LoginPage
    },
    {
      path: '/dashboard',
      name: 'dashboard',
      component: Dashboard,
      meta: { requiresAuth: true }
    }
  ]
})

// 路由守卫 - 检查登录状态
router.beforeEach(async (to, _from) => {
  const authStore = useAuthStore()

  // 检查路由是否需要认证
  const requiresAuth = to.meta.requiresAuth

  if (requiresAuth) {
    // 访问需要认证的页面，通过API检查是否已登录
    const isAuthenticated = await authStore.initAuth()

    if (!isAuthenticated) {
      // 未登录，跳转到登录页面
      return { name: 'login', query: { redirect: to.fullPath } }
    }

    // 已登录，继续访问
    return true
  }

  // 访问登录页面时，如果已经登录则跳转到dashboard
  if (to.path === '/login') {
    const isAuthenticated = await authStore.initAuth()
    if (isAuthenticated) {
      return { name: 'dashboard' }
    }
  }

  // 其他页面正常访问
  return true
})

export default router