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

export interface InitUploadResponse {
  status: number
  message: string
  iv?: string | null
}

export interface UserFileInfo {
  id: string
  fileHash: string
  name: string | null
  createdAt: string
  updatedAt: string
}

export interface DownloadFileInfo {
  iv: string
  tag: string
  url: string
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
  // 上传前初始化，返回是否需要PoW
  init: (hash: string) =>
    api.post<ApiResponse<InitUploadResponse>>('/file/init', { h: hash }),

  // 提交PoW请求（二进制）
  submitPow: (formData: FormData) =>
    api.post<ApiResponse<string>>('/file/pow', formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    }),

  // 文件上传
  upload: (formData: FormData) =>
    api.post<ApiResponse<void>>('/file/upload', formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    }),

  // 获取文件列表
  listFiles: () =>
    api.get<ApiResponse<UserFileInfo[]>>('/file/list'),

  // 下载文件
  download: (fileId: string) =>
    api.get<ApiResponse<DownloadFileInfo>>(`/file/${encodeURIComponent(fileId)}`),

  // 删除文件
  delete: (fileId: string) =>
    api.delete<ApiResponse<string>>(`/file/${encodeURIComponent(fileId)}`)
}

export default api
