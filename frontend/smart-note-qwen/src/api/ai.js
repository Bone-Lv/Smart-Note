import request from '../utils/request.js';

// ==================== AI 对话功能 ====================

/**
 * 发送 AI 对话消息（SSE 流式输出）
 * @param {object} data - { content, conversationId, files }
 * @returns {Promise<Response>} SSE 流 Response 对象
 */
export const sendAIMessageApi = (data) => {
  return fetch(`${request.defaults.baseURL}/ai/chat/message`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json'
    },
    credentials: 'include', // 携带 Cookie
    body: JSON.stringify(data)
  });
};

/**
 * 与文件对话（PDF/图片）
 * @param {FormData} formData - { content, files, conversationId }
 * @returns {Promise<Response>} SSE 流 Response 对象
 */
export const chatWithDocumentApi = (formData) => {
  return fetch(`${request.defaults.baseURL}/ai/chat/chat-with-document`, {
    method: 'POST',
    credentials: 'include', // 携带 Cookie
    body: formData
  });
};

/**
 * 获取聊天历史记录（游标分页）
 * @param {object} params - { conversationId, cursor, pageSize }
 * @returns {Promise}
 */
export const getChatHistoryApi = (params = {}) => {
  return request.get('/ai/chat/history', { params });
};

/**
 * 获取会话列表
 * @returns {Promise}
 */
export const getConversationsApi = () => {
  return request.get('/ai/chat/conversations');
};

/**
 * 删除会话
 * @param {string} conversationId - 会话ID
 * @returns {Promise}
 */
export const deleteConversationApi = (conversationId) => {
  return request.delete(`/ai/chat/conversation/${conversationId}`);
};

/**
 * 清空所有会话
 * @returns {Promise}
 */
export const clearAllConversationsApi = () => {
  return request.delete('/ai/chat/conversations/all');
};
