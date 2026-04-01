export interface Document {
  id: string
  name: string
  type: string
  size: number
  filePath: string
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
