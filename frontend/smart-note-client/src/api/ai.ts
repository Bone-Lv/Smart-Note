// src/api/ai.ts
import request from '@/utils/request'
import { ApiResponse, AIConversationVO, ChatMessageVO, ChatHistoryQueryDTO, CursorPageResult, ChatMessageDTO } from '@/types/api'

// 获取会话列表
export const getConversations = () => {
  return request.get<ApiResponse<AIConversationVO[]>>('/ai/chat/conversations')
}

// 查询聊天历史
export const getChatHistory = (params: ChatHistoryQueryDTO) => {
  return request.get<ApiResponse<CursorPageResult<ChatMessageVO>>>('/ai/chat/history', { params })
}

// 发送消息
export const sendMessage = (data: ChatMessageDTO) => {
  return request.post<ApiResponse<string[]>>('/ai/chat/message', data)
}

// 与文件对话
export const chatWithDocument = (data: ChatMessageDTO) => {
  return request.post<ApiResponse<string[]>>('/ai/chat/chat-with-document', data)
}

// 删除会话
export const deleteConversation = (conversationId: string) => {
  return request.delete<ApiResponse<any>>(`/ai/chat/conversation/${conversationId}`)
}

// 清空所有会话
export const clearAllConversations = () => {
  return request.delete<ApiResponse<any>>('/ai/chat/conversations/all')
}