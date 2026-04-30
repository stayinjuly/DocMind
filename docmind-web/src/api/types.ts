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

export interface UploadResponse {
  success: boolean
  message: string
  documentId?: string
}

export interface AuthRequest {
  email: string
  password: string
}

export interface AuthResponse {
  token: string
  email: string
}
