import { createRouter, createWebHistory } from 'vue-router'
import LoginPage from '../views/LoginPage.vue'
import Dashboard from '../views/Dashboard.vue'

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

// 路由守卫 - 检查登录页面的token
router.beforeEach(async (to) => {
  // 访问登录页面时，如果已经有token则跳转到dashboard
  if (to.path === '/login') {
    const hasToken = document.cookie.includes('access_token=')
    if (hasToken) {
      return { name: 'dashboard' }
    }
  }

  return true
})

export default router