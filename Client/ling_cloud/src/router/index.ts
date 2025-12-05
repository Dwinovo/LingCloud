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

// 路由守卫 - 使用Vue Router官方推荐方式
router.beforeEach((to, from) => {
  // 检查是否有有效的access_token
  const hasToken = document.cookie
    .split('; ')
    .some(row => row.startsWith('access_token='))

  // 需要认证的路由但没有token
  if (to.meta.requiresAuth && !hasToken) {
    console.log('需要认证但没有token，重定向到登录页')
    return { name: 'login' }
  }

  // 已登录但访问登录页，重定向到dashboard
  if (to.path === '/login' && hasToken) {
    console.log('已登录用户访问登录页，重定向到dashboard')
    return { name: 'dashboard' }
  }

  // 其他情况正常通过
  console.log('路由守卫检查通过:', { from: from.path, to: to.path, authenticated: hasToken })
  return true
})

export default router