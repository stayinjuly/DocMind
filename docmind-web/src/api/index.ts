import axios from 'axios'
import type { Document, AuthResponse, PageResult } from './types'
import { useUserStore } from '../stores/user'
import router from '../router'

const API_BASE = import.meta.env.VITE_API_BASE || ''

const api = axios.create({
  baseURL: API_BASE,
  timeout: 30000,
})

// JWT 请求拦截器：自动在请求头中附加令牌
api.interceptors.request.use((config) => {
  const userStore = useUserStore()
  if (userStore.token) {
    config.headers.Authorization = `Bearer ${userStore.token}`
  }
  return config
})

// 响应拦截器：统一解包 ApiResponse 包装层
api.interceptors.response.use((response) => {
  if (response.data && typeof response.data === 'object' && 'data' in response.data) {
    response.data = response.data.data
  }
  return response
})

// 401 响应拦截器：令牌过期时跳转到登录页
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      const url = error.config?.url || ''
      // 登录/注册接口的401不跳转，由页面自行处理错误提示
      if (!url.startsWith('/auth/')) {
        const userStore = useUserStore()
        userStore.logout()
        router.push('/login')
      }
    }
    return Promise.reject(error)
  }
)

// 认证 API
export const authApi = {
  register: (data: { email: string; password: string }) =>
    api.post<AuthResponse>('/auth/register', data),

  login: (data: { email: string; password: string }) =>
    api.post<AuthResponse>('/auth/login', data),
}

// 文档 API（不再需要传 userId，由后端从 JWT 中提取）
export const documentApi = {
  list: (page = 0, size = 20) =>
    api.get<PageResult<Document>>('/documents', { params: { page, size } }),

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
  // 同步问答超时 35s，略大于后端 chat 超时(默认 30s)，确保后端的超时错误能传达给前端
  ask: (question: string) =>
    api.post('/qa', { question }, { timeout: 35000 }),

  clearHistory: () =>
    api.delete('/qa/history'),
}

// SSE 流式对话：fetch + ReadableStream，token 走 Authorization 头（不进 URL）
export interface StreamHandlers {
  onMessage: (data: string) => void
  onError: (err: unknown) => void
  signal?: AbortSignal
}

export async function streamChat(question: string, h: StreamHandlers) {
  const userStore = useUserStore()
  let resp: Response
  try {
    resp = await fetch(`${API_BASE}/qa/stream`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${userStore.token}`,
      },
      body: JSON.stringify({ question }),
      signal: h.signal,
    })
  } catch (e) {
    if ((e as Error)?.name !== 'AbortError') h.onError(e)
    return
  }

  // fetch 不走 axios 拦截器，401 过期需自行处理
  if (resp.status === 401) {
    userStore.logout()
    router.push('/login')
    return
  }
  if (!resp.ok || !resp.body) {
    h.onError(new Error(`SSE 请求失败: ${resp.status}`))
    return
  }

  const reader = resp.body.getReader()
  const decoder = new TextDecoder()
  let buffer = ''
  try {
    while (true) {
      const { done, value } = await reader.read()
      if (done) break
      // 规范换行后按 SSE 事件分隔符 \n\n 切分
      buffer += decoder.decode(value, { stream: true }).replace(/\r\n/g, '\n').replace(/\r/g, '\n')
      let idx
      while ((idx = buffer.indexOf('\n\n')) !== -1) {
        const data = parseSseData(buffer.slice(0, idx))
        buffer = buffer.slice(idx + 2)
        if (data !== null) h.onMessage(data)
      }
    }
    // 处理流末尾可能未带尾部分隔符的最后一个事件
    if (buffer.trim()) {
      const data = parseSseData(buffer)
      if (data !== null) h.onMessage(data)
    }
  } catch (e) {
    if ((e as Error)?.name !== 'AbortError') h.onError(e)
  }
}

// 还原 EventSource 的 data 语义：拼接事件块内所有 data: 行（去掉前缀与单个前导空格）
function parseSseData(block: string): string | null {
  const lines = block.split('\n').filter((l) => l.startsWith('data:'))
  if (!lines.length) return null
  return lines.map((l) => l.slice(5).replace(/^ /, '')).join('\n')
}

export default api
