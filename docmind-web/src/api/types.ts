export interface Document {
  id: string
  name: string
  type: string
  size: number
  isPublic: boolean
  chunkCount: number
  status: string
  uploadTime: string
  userId: string
}

export interface ChatMessage {
  role: 'user' | 'assistant'
  content: string
}

export interface AuthRequest {
  email: string
  password: string
}

export interface AuthResponse {
  token: string
  email: string
}

/** 后端分页响应（经响应拦截器解包后的 Page 对象结构） */
export interface PageResult<T> {
  content: T[]
  totalElements: number
  totalPages: number
  number: number // 当前页（0-based）
  size: number
}
