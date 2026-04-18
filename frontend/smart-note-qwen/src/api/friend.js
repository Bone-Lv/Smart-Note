import request from '../utils/request.js';

// ==================== 好友管理功能 ====================

/**
 * 查找用户（通过邮箱或手机号）
 * @param {string} account - 邮箱或手机号
 * @returns {Promise}
 */
export const searchUserApi = (account) => {
  return request.get('/friend/search', {
    params: { account }
  });
};

/**
 * 发送好友申请
 * @param {object} data - { targetUserId, message }
 * @returns {Promise}
 */
export const sendFriendRequestApi = (data) => {
  return request.post('/friend/request', data);
};

/**
 * 获取收到的好友申请列表
 * @returns {Promise}
 */
export const getReceivedRequestsApi = () => {
  return request.get('/friend/requests/received');
};

/**
 * 处理好友申请
 * @param {object} data - { requestId, approved }
 * @returns {Promise}
 */
export const handleFriendRequestApi = (data) => {
  return request.put('/friend/request/handle', data);
};

/**
 * 获取好友列表
 * @param {number|null} groupId - 分组ID（可选，null表示所有好友）
 * @returns {Promise}
 */
export const getFriendListApi = (groupId = null) => {
  return request.get('/friend/list', {
    params: groupId !== null ? { groupId: String(groupId) } : {}
  });
};

/**
 * 获取好友分组列表
 * @returns {Promise}
 */
export const getFriendGroupsApi = () => {
  return request.get('/friend/groups');
};

/**
 * 创建好友分组
 * @param {string} groupName - 分组名称
 * @returns {Promise}
 */
export const createFriendGroupApi = (groupName) => {
  return request.post('/friend/group', null, {
    params: { groupName }
  });
};

/**
 * 删除好友分组
 * @param {string} groupId - 分组ID
 * @returns {Promise}
 */
export const deleteFriendGroupApi = (groupId) => {
  return request.delete(`/friend/group/${groupId}`);
};

/**
 * 更新好友备注
 * @param {object} data - { friendUserId, remark }
 * @returns {Promise}
 */
export const updateFriendRemarkApi = (data) => {
  return request.put('/friend/remark', data);
};

/**
 * 移动好友到分组
 * @param {object} data - { friendUserId, groupId }
 * @returns {Promise}
 */
export const moveFriendToGroupApi = (data) => {
  return request.put('/friend/move', data);
};

/**
 * 删除好友
 * @param {number} friendUserId - 好友用户ID
 * @returns {Promise}
 */
export const deleteFriendApi = (friendUserId) => {
  return request.delete(`/friend/${friendUserId}`);
};
