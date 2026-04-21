import axios from 'axios'
import type { Document, AuthRequest, AuthResponse } from './types'

const API_BASE = 'http://localhost:8080'

const api = axios.create({
  baseURL: API_BASE,
  timeout: 30000,
})

// JWT 请求拦截器：自动在请求头中附加令牌
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// 401 响应拦截器：令牌过期时跳转到登录页
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      const url = error.config?.url || ''
      // 登录/注册接口的401不跳转，由页面自行处理错误提示
      if (!url.startsWith('/auth/')) {
        localStorage.removeItem('token')
        localStorage.removeItem('email')
        window.location.href = '/login'
      }
    }
    return Promise.reject(error)
  }
)

// 认证 API
export const authApi = {
  register: (data: AuthRequest) =>
    api.post<AuthResponse>('/auth/register', data),

  login: (data: AuthRequest) =>
    api.post<AuthResponse>('/auth/login', data),
}

// 文档 API（不再需要传 userId，由后端从 JWT 中提取）
export const documentApi = {
  list: () => api.get<Document[]>('/documents'),

  upload: (file: File, isPublic: boolean = false) => {
    const formData = new FormData()
    formData.append('file', file)
    formData.append('isPublic', String(isPublic))
    return api.post('/documents', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  },

  delete: (id: string) => api.delete(`/documents/${id}`),
}

// 问答 API
export const qaApi = {
  ask: (question: string) =>
    api.get('/qa', { params: { question } }),

  clearHistory: () =>
    api.delete('/qa/history'),
}

// SSE 流式对话（通过查询参数传递令牌，因为 EventSource 不支持自定义请求头）
export function createChatStream(question: string): EventSource {
  const token = localStorage.getItem('token')
  const url = `${API_BASE}/qa/stream?question=${encodeURIComponent(question)}&token=${encodeURIComponent(token || '')}`
  return new EventSource(url)
}

export default api
