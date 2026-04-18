import request from '../utils/request.js';

export const aiApi = {
  // 发送消息
  sendMessage(data) {
    return request.post('/ai/chat/message', data);
  },

  // 与文件对话
  chatWithDocument(dto, files) {
    const formData = new FormData();
    formData.append('dto', JSON.stringify(dto));
    if (files) {
      files.forEach(file => {
        formData.append('files', file);
      });
    }
    return request.post('/ai/chat/chat-with-document', formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    });
  },

  // 获取会话列表
  getConversations() {
    return request.get('/ai/chat/conversations');
  },

  // 查询聊天历史（游标分页）
  getChatHistory(queryDTO) {
    return request.get('/ai/chat/history', { params: queryDTO });
  },

  // 删除会话
  deleteConversation(conversationId) {
    return request.delete(`/ai/chat/conversation/${conversationId}`);
  },

  // 清空所有会话
  clearAllConversations() {
    return request.delete('/ai/chat/conversations/all');
  }
};

/**
 * 2. 修复之前报错所需的导出：aiStreamRequest
 * 用途：用于流式输出（打字机效果）
 * 注意：这里使用 responseType: 'stream' 配合 axios
 */
export function aiStreamRequest(data) {
  return request({
    url: '/ai/chat/stream', // 流式聊天接口
    method: 'post',
    data: data,
    // 告诉 axios 这是一个流，防止它等待完整响应
    responseType: 'stream' 
  })
}

/**
 * 3. 辅助函数：获取模型列表 (可选，视业务需求而定)
 */
export function getModels() {
  return request({
    url: '/ai/models',
    method: 'get'
  })
}