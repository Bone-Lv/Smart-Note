// src/api/social.ts
import request from '@/utils/request'
import { 
  ApiResponse, 
  FriendVO, 
  FriendGroupVO, 
  FriendRequestVO, 
  HandleFriendRequestDTO, 
  SendFriendRequestDTO, 
  UpdateFriendRemarkDTO, 
  MoveFriendToGroupDTO,
  ChatGroupVO,
  PrivateMessageVO,
  GroupMessageVO,
  MessageQueryDTO,
  CursorPageResult,
  ConversationSessionVO
} from '@/types/api'

// 查找用户
export const searchUser = (account: string) => {
  return request.get<ApiResponse<any>>('/friend/search', { params: { account } })
}

// 获取好友列表
export const getFriendList = (groupId?: number) => {
  return request.get<ApiResponse<FriendVO[]>>('/friend/list', { params: { groupId } })
}

// 获取好友分组列表
export const getFriendGroups = () => {
  return request.get<ApiResponse<FriendGroupVO[]>>('/friend/groups')
}

// 创建好友分组
export const createFriendGroup = (groupName: string) => {
  return request.post<ApiResponse<any>>('/friend/group', null, { params: { groupName } })
}

// 删除好友分组
export const deleteFriendGroup = (groupId: number) => {
  return request.delete<ApiResponse<any>>(`/friend/group/${groupId}`)
}

// 发送好友申请
export const sendFriendRequest = (data: SendFriendRequestDTO) => {
  return request.post<ApiResponse<any>>('/friend/request', data)
}

// 获取收到的好友申请
export const getReceivedRequests = () => {
  return request.get<ApiResponse<FriendRequestVO[]>>('/friend/requests/received')
}

// 处理好友申请
export const handleFriendRequest = (data: HandleFriendRequestDTO) => {
  return request.put<ApiResponse<any>>('/friend/request/handle', data)
}

// 更新好友备注
export const updateFriendRemark = (data: UpdateFriendRemarkDTO) => {
  return request.put<ApiResponse<any>>('/friend/remark', data)
}

// 移动好友到分组
export const moveFriendToGroup = (data: MoveFriendToGroupDTO) => {
  return request.put<ApiResponse<any>>('/friend/move', data)
}

// 删除好友
export const deleteFriend = (friendUserId: number) => {
  return request.delete<ApiResponse<any>>(`/friend/${friendUserId}`)
}

// 获取会话列表
export const getConversationList = () => {
  return request.get<ApiResponse<ConversationSessionVO[]>>('/message/conversations')
}

// 获取私聊历史消息
export const getPrivateMessageHistory = (params: MessageQueryDTO) => {
  return request.get<ApiResponse<CursorPageResult<PrivateMessageVO>>>('/message/private/history', { params })
}

// 获取群聊历史消息
export const getGroupMessageHistory = (params: MessageQueryDTO) => {
  return request.get<ApiResponse<CursorPageResult<GroupMessageVO>>>('/message/group/history', { params })
}

// 获取离线消息
export const getOfflineMessages = () => {
  return request.get<ApiResponse<PrivateMessageVO[]>>('/message/private/offline')
}

// 标记消息为已读
export const markMessageAsRead = (messageId: number) => {
  return request.put<ApiResponse<any>>(`/message/private/read/${messageId}`)
}

// 标记所有消息为已读
export const markAllMessagesAsRead = (friendUserId: number) => {
  return request.put<ApiResponse<any>>(`/message/private/read-all/${friendUserId}`)
}

// 清空聊天记录
export const clearPrivateChatHistory = (friendUserId: number) => {
  return request.delete<ApiResponse<any>>(`/message/private/clear/${friendUserId}`)
}

// 创建群聊
export const createGroup = (data: any) => {
  return request.post<ApiResponse<number>>('/message/group/create', null, { params: { dto: data } })
}

// 获取我的群聊列表
export const getMyGroups = () => {
  return request.get<ApiResponse<ChatGroupVO[]>>('/message/group/my-groups')
}

// 获取群聊详情
export const getGroupDetail = (groupId: number) => {
  return request.get<ApiResponse<ChatGroupVO>>(`/message/group/detail/${groupId}`)
}

// 申请加入群聊
export const joinGroup = (groupId: number) => {
  return request.post<ApiResponse<any>>(`/message/group/join/${groupId}`)
}

// 退出群聊
export const leaveGroup = (groupId: number) => {
  return request.post<ApiResponse<any>>(`/message/group/leave/${groupId}`)
}

// 清空群聊历史记录
export const clearGroupChatHistory = (groupId: number) => {
  return request.delete<ApiResponse<any>>(`/message/group/clear/${groupId}`)
}

// 获取待审核的入群申请
export const getPendingJoinRequests = (groupId: number) => {
  return request.get<ApiResponse<any>>(`/message/group/pending/${groupId}`)
}

// 审批入群申请
export const approveGroupJoin = (data: any) => {
  return request.post<ApiResponse<any>>('/message/group/approve', data)
}