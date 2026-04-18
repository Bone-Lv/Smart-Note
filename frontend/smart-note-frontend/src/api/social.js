import request from '../utils/request.js';

export const socialApi = {
  // 查找用户
  searchUser(account) {
    return request.get('/friend/search', { params: { account } });
  },

  // 发送好友申请
  sendFriendRequest(data) {
    return request.post('/friend/request', data);
  },

  // 获取收到的好友申请
  getReceivedRequests() {
    return request.get('/friend/requests/received');
  },

  // 处理好友申请
  handleFriendRequest(data) {
    return request.put('/friend/request/handle', data);
  },

  // 获取好友列表
  getFriendList(groupId = null) {
    return request.get('/friend/list', { params: { groupId } });
  },

  // 删除好友
  deleteFriend(friendUserId) {
    return request.delete(`/friend/${friendUserId}`);
  },

  // 更新好友备注
  updateFriendRemark(data) {
    return request.put('/friend/remark', data);
  },

  // 移动好友到分组
  moveFriendToGroup(data) {
    return request.put('/friend/move', data);
  },

  // 获取好友分组列表
  getFriendGroups() {
    return request.get('/friend/groups');
  },

  // 创建好友分组
  createFriendGroup(groupName) {
    return request.post('/friend/group', null, { params: { groupName } });
  },

  // 删除好友分组
  deleteFriendGroup(groupId) {
    return request.delete(`/friend/group/${groupId}`);
  },

  // 获取离线消息
  getOfflineMessages() {
    return request.get('/message/private/offline');
  },

  // 获取私聊历史消息（游标分页）
  getPrivateMessageHistory(queryDTO) {
    return request.get('/message/private/history', { params: queryDTO });
  },

  // 发送私聊消息
  sendPrivateMessage(dto) {
    return request.post('/message/private/send', null, { params: { dto } });
  },

  // 标记消息为已读
  markMessageAsRead(messageId) {
    return request.put(`/message/private/read/${messageId}`);
  },

  // 标记所有消息为已读
  markAllMessagesAsRead(friendUserId) {
    return request.put(`/message/private/read-all/${friendUserId}`);
  },

  // 清空聊天记录
  clearPrivateChatHistory(friendUserId) {
    return request.delete(`/message/private/clear/${friendUserId}`);
  },

  // 获取群聊列表
  getMyGroups() {
    return request.get('/message/group/my-groups');
  },

  // 获取群聊详情
  getGroupDetail(groupId) {
    return request.get(`/message/group/detail/${groupId}`);
  },

  // 获取群聊历史消息（游标分页）
  getGroupMessageHistory(queryDTO) {
    return request.get('/message/group/history', { params: queryDTO });
  },

  // 发送群聊消息
  sendGroupMessage(dto) {
    return request.post('/message/group/send', null, { params: { dto } });
  },

  // 标记群聊所有消息为已读
  markAllGroupMessagesAsRead(groupId) {
    return request.put(`/message/group/read-all/${groupId}`);
  },

  // 清空群聊历史记录
  clearGroupChatHistory(groupId) {
    return request.delete(`/message/group/clear/${groupId}`);
  },

  // 创建群聊
  createGroup(dto) {
    return request.post('/message/group/create', null, { params: { dto } });
  },

  // 申请加入群聊
  joinGroup(groupId) {
    return request.post(`/message/group/join/${groupId}`);
  },

  // 退出群聊
  leaveGroup(groupId) {
    return request.post(`/message/group/leave/${groupId}`);
  },

  // 获取待审核的入群申请
  getPendingJoinRequests(groupId) {
    return request.get(`/message/group/pending/${groupId}`);
  },

  // 审批入群申请
  approveGroupJoin(data) {
    return request.post('/message/group/approve', data);
  },

  // 获取会话列表
  getConversationList() {
    return request.get('/message/conversations');
  }
};