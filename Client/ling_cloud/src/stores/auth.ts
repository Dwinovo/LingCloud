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
    // 清除cookie，使用SameSite=None
    document.cookie = 'access_token=; expires=Thu, 01 Jan 1970 00:00:00 UTC; path=/; SameSite=None'
  }

  // 从后端获取用户信息 - HttpOnly Cookie认证的标准方式
  const fetchUserInfo = async (): Promise<boolean> => {
    try {
      const response = await authApi.getCurrentUser()
      if (response.data.code === 0) {
        const user = response.data.data
        setUserInfo({
          username: user.username,
          nickname: user.nickname
        })
        return true
      } else if (response.data.code === 2000) {
        // 需要重新登录
        clearUserInfo()
        return false
      } else {
        // 其他错误，清除状态
        clearUserInfo()
        return false
      }
    } catch (error: any) {
      console.error('获取用户信息失败:', error)
      // 401表示未认证，清除状态
      if (error.response?.status === 401) {
        clearUserInfo()
        return false
      }
      // 其他网络错误，保持当前状态
      return isLoggedIn.value
    }
  }

  // 初始化认证状态 - 通过API调用而不是检查cookie
  const initAuth = async (): Promise<boolean> => {
    // 如果已经有用户信息，先返回true（快速路径）
    if (userInfo.value && isLoggedIn.value) {
      return true
    }

    // 通过/me接口检查登录状态
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