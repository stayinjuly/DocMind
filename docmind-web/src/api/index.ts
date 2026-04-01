import axios from 'axios'
import type { Document } from './types'

const API_BASE = 'http://localhost:8080'

const api = axios.create({
  baseURL: API_BASE,
  timeout: 30000,
})

// Document APIs
export const documentApi = {
  list: () => api.get<Document[]>('/documents'),

  upload: (file: File, userId: string) => {
    const formData = new FormData()
    formData.append('file', file)
    formData.append('userId', userId)
    return api.post('/documents', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  },

  delete: (id: string) => api.delete(`/documents/${id}`),
}

// QA APIs
export const qaApi = {
  ask: (question: string, userId: string) =>
    api.get('/qa', { params: { question, userId } }),

  clearHistory: (userId: string) =>
    api.delete(`/qa/history/del/${userId}`),
}

// SSE Stream for chat
export function createChatStream(question: string, userId: string): EventSource {
  const url = `${API_BASE}/qa/stream?question=${encodeURIComponent(question)}&userId=${encodeURIComponent(userId)}`
  return new EventSource(url)
}

export default api
