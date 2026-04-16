package com.gdut.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gdut.domain.dto.chat.CreateGroupDTO;
import com.gdut.domain.dto.chat.ApproveGroupJoinDTO;
import com.gdut.domain.dto.chat.GroupMessageDTO;
import com.gdut.domain.dto.chat.MessageQueryDTO;
import com.gdut.domain.dto.chat.PrivateMessageDTO;
import com.gdut.domain.vo.chat.*;

import java.util.List;

public interface MessageService {
    /**
     * 发送私聊消息
     */
    PrivateMessageVO sendPrivateMessage(Long senderId, PrivateMessageDTO dto);
    
    /**
     * 获取与某好友的聊天历史（游标分页）
     */
    CursorPageResult<PrivateMessageVO> getPrivateMessageHistory(Long userId, MessageQueryDTO queryDTO);
    
    /**
     * 获取离线消息
     */
    List<PrivateMessageVO> getOfflineMessages(Long userId);
    
    /**
     * 标记消息为已读
     */
    void markMessageAsRead(Long userId, Long messageId);
    
    /**
     * 标记与某好友的所有消息为已读
     */
    void markAllMessagesAsRead(Long userId, Long friendUserId);
    
    /**
     * 清空与某好友的聊天记录
     */
    void clearPrivateChatHistory(Long userId, Long friendUserId);
    
    /**
     * 清空群聊历史记录（仅自己不可见）
     */
    void clearGroupChatHistory(Long userId, Long groupId);


    /**
     * 获取会话列表（私聊+群聊）
     */
    List<ConversationSessionVO> getConversationList(Long userId);
    
    /**
     * 创建群聊
     */
    Long createGroup(Long userId, CreateGroupDTO dto);
    
    /**
     * 发送群聊消息
     */
    GroupMessageVO sendGroupMessage(Long senderId, GroupMessageDTO dto);
    
    /**
     * 获取群聊消息历史（游标分页）
     */
    CursorPageResult<GroupMessageVO> getGroupMessageHistory(Long userId, MessageQueryDTO queryDTO);
    
    /**
     * 获取我的群聊列表
     */
    List<ChatGroupVO> getMyGroups(Long userId);
    
    /**
     * 加入群聊（提交申请）
     */
    void joinGroup(Long userId, Long groupId);
    
    /**
     * 审批入群申请（群主/管理员）
     */
    void approveGroupJoin(Long operatorId, ApproveGroupJoinDTO dto);
    
    /**
     * 获取待审核的入群申请列表（群主/管理员）
     */
    List<PendingGroupJoinVO> getPendingJoinRequests(Long operatorId, Long groupId);
    
    /**
     * 退出群聊
     */
    void leaveGroup(Long userId, Long groupId);
    
    /**
     * 获取群聊详情
     */
    ChatGroupVO getGroupDetail(Long userId, Long groupId);
    
    /**
     * 标记群聊所有消息为已读
     */
    void markAllGroupMessagesAsRead(Long userId, Long groupId);
}
