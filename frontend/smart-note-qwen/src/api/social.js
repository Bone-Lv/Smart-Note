import request from '../utils/request.js';

// 查找用户
export const searchUserApi = (account) => {
  return request.get('/friend/search', { params: { account } });
};

// 发送好友申请
export const sendFriendRequestApi = (requestData) => {
  return request.post('/friend/request', requestData);
};

// 获取收到的好友申请
export const getReceivedRequestsApi = () => {
  return request.get('/friend/requests/received');
};

// 处理好友申请
export const handleFriendRequestApi = (requestData) => {
  return request.put('/friend/request/handle', requestData);
};

// 获取好友列表
export const getFriendListApi = (groupId = null) => {
  return request.get('/friend/list', { params: { groupId } });
};

// 获取好友分组列表
export const getFriendGroupsApi = () => {
  return request.get('/friend/groups');
};

// 创建好友分组
export const createFriendGroupApi = (groupName) => {
  return request.post('/friend/group', null, { params: { groupName } });
};

// 更新好友备注
export const updateFriendRemarkApi = (remarkData) => {
  return request.put('/friend/remark', remarkData);
};

// 移动好友到分组
export const moveFriendToGroupApi = (moveData) => {
  return request.put('/friend/move', moveData);
};

// 删除好友
export const deleteFriendApi = (friendUserId) => {
  return request.delete(`/friend/${friendUserId}`);
};

// 删除好友分组
export const deleteFriendGroupApi = (groupId) => {
  return request.delete(`/friend/group/${groupId}`);
};

// 发送私聊消息
export const sendPrivateMessageApi = (messageData) => {
  // 使用 FormData 发送，支持文本和图片
  const formData = new FormData();
  formData.append('receiverId', messageData.receiverId);
  formData.append('content', messageData.content || '');
  formData.append('messageType', messageData.messageType || 1);
  
  if (messageData.imageFile) {
    formData.append('imageFile', messageData.imageFile);
  }
  
  return request.post('/message/private/send', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  });
};

// 获取私聊历史消息（游标分页）
export const getPrivateMessageHistoryApi = (params) => {
  return request.get('/message/private/history', { 
    params: {
      friendUserId: params.friendUserId,
      cursor: params.cursor || null,
      pageSize: params.pageSize || 20
    }
  });
};

// 获取离线消息
export const getOfflineMessagesApi = () => {
  return request.get('/message/private/offline');
};

// 标记消息为已读
export const markMessageAsReadApi = (messageId) => {
  return request.put(`/message/private/read/${messageId}`);
};

// 标记所有消息为已读（通过WebSocket推荐，这里保留HTTP备用）
export const markAllMessagesAsReadApi = (friendUserId, upToMessageId) => {
  return request.put(`/message/private/read/${friendUserId}`, {
    upToMessageId: upToMessageId
  });
};

// 清空聊天记录
export const clearPrivateChatHistoryApi = (friendUserId) => {
  return request.delete(`/message/private/clear/${friendUserId}`);
};

// 创建群聊
export const createGroupApi = (groupData) => {
  console.log('📤 createGroupApi 发送的原始数据:', groupData);
  console.log('📤 createGroupApi 发送的 JSON:', JSON.stringify(groupData));
  
  // 发送 JSON 数据（后端已支持 @RequestBody）
  return request.post('/message/group/create', groupData);
};

// 获取我的群聊列表
export const getMyGroupsApi = () => {
  return request.get('/message/group/my-groups');
};

// 获取群聊详情
export const getGroupDetailApi = (groupId) => {
  return request.get(`/message/group/detail/${groupId}`);
};

// 获取群聊成员列表
export const getGroupMembersApi = (groupId) => {
  return request.get(`/message/group/members/${groupId}`);
};

// 重命名群聊（仅群主）
export const renameGroupApi = (groupId, groupName) => {
  return request.put(`/message/group/${groupId}/name`, { groupName });
};

// 转让群主（仅群主）
export const transferGroupOwnerApi = (groupId, newOwnerId) => {
  return request.post(`/message/group/${groupId}/transfer`, { newOwnerId });
};

// 设置或取消管理员（仅群主）
export const setGroupAdminApi = (groupId, userId, isAdmin) => {
  return request.put(`/message/group/${groupId}/admin`, { userId, isAdmin });
};

// 移除群成员（群主或管理员）
export const removeGroupMemberApi = (groupId, userId) => {
  return request.delete(`/message/group/${groupId}/member`, {
    data: { userId }
  });
};

// 发送群聊消息
export const sendGroupMessageApi = (messageData) => {
  // 使用 FormData 发送，支持文本和图片
  const formData = new FormData();
  formData.append('groupId', messageData.groupId);
  formData.append('content', messageData.content || '');
  formData.append('messageType', messageData.messageType || 1);
  
  if (messageData.imageFile) {
    formData.append('imageFile', messageData.imageFile);
  }
  
  return request.post('/message/group/send', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  });
};

// 获取群聊历史消息（游标分页）
export const getGroupMessageHistoryApi = (params) => {
  return request.get('/message/group/history', { 
    params: {
      groupId: params.groupId,
      cursor: params.cursor || null,
      pageSize: params.pageSize || 20
    }
  });
};

// 申请加入群聊
export const joinGroupApi = (groupId) => {
  return request.post(`/message/group/join/${groupId}`);
};

// 退出群聊
export const leaveGroupApi = (groupId) => {
  return request.post(`/message/group/leave/${groupId}`);
};

// 审批入群申请
export const approveGroupJoinApi = (approvalData) => {
  return request.post('/message/group/approve', approvalData);
};

// 获取待审核的入群申请
export const getPendingJoinRequestsApi = (groupId) => {
  return request.get(`/message/group/pending/${groupId}`);
};

// 标记群聊所有消息为已读
export const markAllGroupMessagesAsReadApi = (groupId) => {
  return request.put(`/message/group/read-all/${groupId}`);
};

// 清空群聊历史记录
export const clearGroupChatHistoryApi = (groupId) => {
  return request.delete(`/message/group/clear/${groupId}`);
};

// 解散群聊（仅群主）
export const disbandGroupApi = (groupId) => {
  return request.delete(`/message/group/disband/${groupId}`);
};

// 获取会话列表
export const getConversationListApi = () => {
  return request.get('/message/conversations');
};