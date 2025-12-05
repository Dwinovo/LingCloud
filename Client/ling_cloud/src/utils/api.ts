import axios from 'axios'

// 使用相对路径，通过 Vite 代理转发到后端
const API_BASE_URL = '/api'

const api = axios.create({
  baseURL: API_BASE_URL,
  withCredentials: true, // 重要：支持cookie
  headers: {
    'Content-Type': 'application/json'
  }
})

// 响应拦截器
api.interceptors.response.use(
  (response) => {
    return response
  },
  (error) => {
    if (error.response?.status === 401) {
      // 清除本地认证状态
      document.cookie = 'access_token=; expires=Thu, 01 Jan 1970 00:00:00 UTC; path=/;'
      window.location.href = '/login'
    }
    return Promise.reject(error)
  }
)

export interface ApiResponse<T = any> {
  success: boolean
  data: T
  message: string
  code: number
}

export const authApi = {
  // 用户注册
  register: (username: string, password: string, nickname: string) =>
    api.post<ApiResponse<any>>('/auth/register', { username, password, nickname }),

  // 用户登录
  login: (username: string, password: string) =>
    api.post<ApiResponse<any>>('/auth/login', { username, password }),

  // 获取当前用户信息
  getCurrentUser: () =>
    api.get<ApiResponse<any>>('/user/me')
}

export const fileApi = {
  // 文件上传
  upload: (formData: FormData) =>
    api.post<ApiResponse<void>>('/file/upload', formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    }),

  // 获取文件列表
  listFiles: () =>
    api.get<ApiResponse<any>>('/file/list'),

  // 下载文件
  download: (fileId: string) =>
    api.get(`/file/download/${fileId}`, {
      responseType: 'blob'
    })
}

export default api