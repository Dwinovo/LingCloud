import { defineStore } from 'pinia'
import { ref } from 'vue'
import { authApi } from '../utils/api'

export interface UserInfo {
  username: string
  nickname: string
}

export const useAuthStore = defineStore('auth', () => {
  const userInfo = ref<UserInfo | null>(null)
  const isLoggedIn = ref(false)

  const setUserInfo = (user: UserInfo) => {
    userInfo.value = user
    isLoggedIn.value = true
  }

  const clearUserInfo = () => {
    userInfo.value = null
    isLoggedIn.value = false
    // 清除cookie，指定正确的域名
    document.cookie = 'access_token=; expires=Thu, 01 Jan 1970 00:00:00 UTC; path=/; domain=localhost'
  }

  // 检查是否有token
  const hasToken = (): boolean => {
    return document.cookie.includes('access_token=')
  }

  // 从后端获取用户信息
  const fetchUserInfo = async (): Promise<boolean> => {
    if (!hasToken()) {
      return false
    }

    try {
      const response = await authApi.getCurrentUser()
      if (response.data.code === 0) {
        const user = response.data.data
        setUserInfo({
          username: user.username,
          nickname: user.nickname
        })
        return true
      } else {
        clearUserInfo()
        return false
      }
    } catch (error) {
      console.error('获取用户信息失败:', error)
      clearUserInfo()
      return false
    }
  }

  // 初始化认证状态
  const initAuth = async (): Promise<boolean> => {
    if (userInfo.value && isLoggedIn.value) {
      return true // 已经有用户信息，无需重新获取
    }

    return await fetchUserInfo()
  }

  return {
    userInfo,
    isLoggedIn,
    setUserInfo,
    clearUserInfo,
    fetchUserInfo,
    initAuth
  }
})