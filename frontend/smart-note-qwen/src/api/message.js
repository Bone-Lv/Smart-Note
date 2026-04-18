import request from '../utils/request.js';

// ==================== 私聊功能 ====================

/**
 * 发送私聊消息
 * @param {FormData} formData - 包含 receiverId, content, messageType, imageFile(可选)
 * @returns {Promise}
 */
export const sendPrivateMessageApi = (formData) => {
  return request.post('/message/private/send', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  });
};

/**
 * 获取私聊历史记录（游标分页）
 * @param {string} friendUserId - 好友ID
 * @param {object} params - { cursor, pageSize }
 * @returns {Promise}
 */
export const getPrivateHistoryApi = (friendUserId, params = {}) => {
  return request.get('/message/private/history', {
    params: { friendUserId, ...params }
  });
};

/**
 * 获取离线消息
 * @returns {Promise}
 */
export const getOfflineMessagesApi = () => {
  return request.get('/message/private/offline');
};

/**
 * 标记单条消息已读
 * @param {number} messageId - 消息ID
 * @returns {Promise}
 */
export const markMessageReadApi = (messageId) => {
  return request.put(`/message/private/read/${messageId}`);
};

/**
 * 批量标记消息已读（推荐）
 * @param {number} friendUserId - 好友ID
 * @returns {Promise}
 */
export const markAllReadApi = (friendUserId) => {
  return request.put(`/message/private/read-all/${friendUserId}`);
};

/**
 * 清空私聊记录
 * @param {number} friendUserId - 好友ID
 * @returns {Promise}
 */
export const clearPrivateChatApi = (friendUserId) => {
  return request.delete(`/message/private/clear/${friendUserId}`);
};

// ==================== 群聊功能 ====================

/**
 * 创建群聊
 * @param {object} data - { groupName, memberIds }
 * @returns {Promise}
 */
export const createGroupApi = (data) => {
  return request.post('/message/group/create', data);
};

/**
 * 发送群聊消息
 * @param {FormData} formData - 包含 groupId, content, messageType, imageFile(可选)
 * @returns {Promise}
 */
export const sendGroupMessageApi = (formData) => {
  return request.post('/message/group/send', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  });
};

/**
 * 获取群聊历史记录（游标分页）
 * @param {number} groupId - 群ID
 * @param {object} params - { cursor, pageSize }
 * @returns {Promise}
 */
export const getGroupHistoryApi = (groupId, params = {}) => {
  return request.get('/message/group/history', {
    params: { groupId, ...params }
  });
};

/**
 * 获取我的群聊列表
 * @returns {Promise}
 */
export const getMyGroupsApi = () => {
  return request.get('/message/group/my-groups');
};

/**
 * 获取群聊详情
 * @param {number} groupId - 群ID
 * @returns {Promise}
 */
export const getGroupDetailApi = (groupId) => {
  return request.get(`/message/group/detail/${groupId}`);
};

/**
 * 获取待审核的入群申请
 * @param {number} groupId - 群ID
 * @returns {Promise}
 */
export const getPendingApplicationsApi = (groupId) => {
  return request.get(`/message/group/pending/${groupId}`);
};

/**
 * 申请加入群聊
 * @param {number} groupId - 群ID
 * @returns {Promise}
 */
export const joinGroupApi = (groupId) => {
  return request.post(`/message/group/join/${groupId}`);
};

/**
 * 审批入群申请
 * @param {object} data - { groupId, applicantId, approved }
 * @returns {Promise}
 */
export const approveGroupApplicationApi = (data) => {
  return request.post('/message/group/approve', data);
};

/**
 * 标记群聊所有消息为已读
 * @param {number} groupId - 群ID
 * @returns {Promise}
 */
export const markGroupAllReadApi = (groupId) => {
  return request.put(`/message/group/read-all/${groupId}`);
};

/**
 * 退出群聊
 * @param {number} groupId - 群ID
 * @returns {Promise}
 */
export const leaveGroupApi = (groupId) => {
  return request.post(`/message/group/leave/${groupId}`);
};

// ==================== 会话列表 ====================

/**
 * 获取会话列表（私聊+群聊混合）
 * @returns {Promise}
 */
export const getConversationsApi = () => {
  return request.get('/message/conversations');
};
