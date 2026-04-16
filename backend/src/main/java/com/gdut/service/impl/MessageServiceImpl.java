package com.gdut.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gdut.common.enums.FriendStatus;
import com.gdut.common.enums.MessageType;
import com.gdut.common.exception.BusinessException;
import com.gdut.common.enums.ResultCode;
import com.gdut.common.util.AliyunOSSOperator;
import com.gdut.domain.dto.chat.*;
import com.gdut.domain.entity.chat.*;
import com.gdut.domain.entity.friend.Friend;
import com.gdut.domain.entity.user.User;
import com.gdut.domain.vo.chat.*;
import com.gdut.mapper.*;
import com.gdut.service.MessageService;
import com.gdut.common.util.ChatWebSocketHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {

    private final PrivateMessageMapper privateMessageMapper;
    private final ChatGroupMapper chatGroupMapper;
    private final ChatGroupMemberMapper chatGroupMemberMapper;
    private final GroupMessageMapper groupMessageMapper;
    private final UserMapper userMapper;
    private final FriendMapper friendMapper;
    private final AliyunOSSOperator aliyunOSSOperator;
    private final ChatWebSocketHandler webSocketHandler;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PrivateMessageVO sendPrivateMessage(Long senderId, PrivateMessageDTO dto) {
        // 1. 验证是否是好友关系
        Friend friend = friendMapper.selectOne(new LambdaQueryWrapper<Friend>()
                .eq(Friend::getUserId, senderId)
                .eq(Friend::getFriendUserId, dto.getReceiverId())
                .eq(Friend::getStatus, FriendStatus.ACCEPTED));
        
        if (friend == null) {
            throw new BusinessException(ResultCode.NOT_FRIENDS);
        }

        // 2. 处理文件上传并获取消息内容
        MessageContentResult contentResult = uploadAndProcessFile(dto.getFile(), dto.getContent(), dto.getMessageType());

        // 3. 保存消息到数据库
        PrivateMessage message = new PrivateMessage();
        message.setSenderId(senderId);
        message.setReceiverId(dto.getReceiverId());
        message.setMessageType(contentResult.messageType);
        message.setContent(contentResult.content);
        message.setIsRead(0);
        message.setCreateTime(LocalDateTime.now());
        privateMessageMapper.insert(message);

        // 4. 如果接收者在线，通过WebSocket推送消息
        pushPrivateMessage(dto.getReceiverId(), message, senderId);

        // 5. 返回消息VO
        return convertToPrivateMessageVO(message);
    }

    @Override
    public CursorPageResult<PrivateMessageVO> getPrivateMessageHistory(Long userId, MessageQueryDTO queryDTO) {
        if (queryDTO.getFriendUserId() == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "好友ID不能为空");
        }
        
        int pageSize = queryDTO.getPageSize() != null ? queryDTO.getPageSize() : 20;
        Long cursor = queryDTO.getCursor();
        
        // 查询双方之间的消息，且当前用户未删除的
        LambdaQueryWrapper<PrivateMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w -> w
                .eq(PrivateMessage::getSenderId, userId)
                .eq(PrivateMessage::getReceiverId, queryDTO.getFriendUserId())
                .eq(PrivateMessage::getDeletedBySender, 0)
                .or()
                .eq(PrivateMessage::getSenderId, queryDTO.getFriendUserId())
                .eq(PrivateMessage::getReceiverId, userId)
                .eq(PrivateMessage::getDeletedByReceiver, 0)
        );
        
        // 游标分页：基于消息ID进行分页（ID越大越新）
        if (cursor != null) {
            wrapper.lt(PrivateMessage::getId, cursor);
        }
        wrapper.orderByDesc(PrivateMessage::getId);
        
        // 多取一条数据用于判断是否有下一页
        wrapper.last("LIMIT " + (pageSize + 1));
        
        List<PrivateMessage> messages = privateMessageMapper.selectList(wrapper);
        
        // 判断是否有更多数据
        boolean hasNext = messages.size() > pageSize;
        Long nextCursor = null;
        
        if (hasNext) {
            // 移除多余的一条数据
            messages = messages.subList(0, pageSize);
            // 设置下一页游标为最后一条记录的ID
            nextCursor = messages.getLast().getId();
        }
        
        // 转换为VO
        List<PrivateMessageVO> voList = messages.stream()
                .map(this::convertToPrivateMessageVO)
                .collect(Collectors.toList());
        
        // 返回专门的游标分页结果
        return CursorPageResult.<PrivateMessageVO>builder()
                .records(voList)
                .nextCursor(nextCursor)
                .hasNext(hasNext)
                .pageSize(pageSize)
                .build();
    }

    @Override
    public List<PrivateMessageVO> getOfflineMessages(Long userId) {
        // 查询所有未读且未被当前用户删除的消息
        List<PrivateMessage> messages = privateMessageMapper.selectList(
                new LambdaQueryWrapper<PrivateMessage>()
                        .eq(PrivateMessage::getReceiverId, userId)
                        .eq(PrivateMessage::getIsRead, 0)
                        .eq(PrivateMessage::getDeletedByReceiver, 0) // ✅ 接收方（当前用户）未删除
                        .orderByAsc(PrivateMessage::getCreateTime)
        );

        return messages.stream()
                .map(this :: convertToPrivateMessageVO)
                .collect(Collectors.toList());
    }

    @Override
    public void markMessageAsRead(Long userId, Long messageId) {
        PrivateMessage message = privateMessageMapper.selectById(messageId);
        if (message != null && message.getReceiverId().equals(userId)) {
            message.setIsRead(1);
            privateMessageMapper.updateById(message);
        }
    }

    @Override
    public void markAllMessagesAsRead(Long userId, Long friendUserId) {
        privateMessageMapper.update(null, 
                new LambdaUpdateWrapper<PrivateMessage>()
                        .eq(PrivateMessage::getReceiverId, userId)
                        .eq(PrivateMessage::getSenderId, friendUserId)
                        .eq(PrivateMessage::getIsRead, 0)
                        .set(PrivateMessage::getIsRead, 1)
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void clearPrivateChatHistory(Long userId, Long friendUserId) {
        // ✅ 优化：直接通过 SQL 批量更新，避免加载大量数据到内存导致 OOM
        
        // 1. 标记当前用户作为发送方的消息为已删除
        privateMessageMapper.update(null,
                new LambdaUpdateWrapper<PrivateMessage>()
                        .eq(PrivateMessage::getSenderId, userId)
                        .eq(PrivateMessage::getReceiverId, friendUserId)
                        .set(PrivateMessage::getDeletedBySender, 1)
        );

        // 2. 标记当前用户作为接收方的消息为已删除
        privateMessageMapper.update(null,
                new LambdaUpdateWrapper<PrivateMessage>()
                        .eq(PrivateMessage::getSenderId, friendUserId)
                        .eq(PrivateMessage::getReceiverId, userId)
                        .set(PrivateMessage::getDeletedByReceiver, 1)
        );

        // 3. 物理删除双方都已删除的消息
        privateMessageMapper.delete(
                new LambdaQueryWrapper<PrivateMessage>()
                        .and(w -> w
                                .eq(PrivateMessage::getSenderId, userId)
                                .eq(PrivateMessage::getReceiverId, friendUserId)
                                .or()
                                .eq(PrivateMessage::getSenderId, friendUserId)
                                .eq(PrivateMessage::getReceiverId, userId)
                        )
                        .eq(PrivateMessage::getDeletedBySender, 1)
                        .eq(PrivateMessage::getDeletedByReceiver, 1)
        );
        
        log.info("用户{}已清空与好友{}的聊天记录", userId, friendUserId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void clearGroupChatHistory(Long userId, Long groupId) {
        // 1. 验证是否是群成员
        ChatGroupMember member = validateGroupMember(userId, groupId);

        // 2. 获取当前群最新的消息ID
        GroupMessage latestMsg = groupMessageMapper.selectOne(
                new LambdaQueryWrapper<GroupMessage>()
                        .eq(GroupMessage::getGroupId, groupId)
                        .orderByDesc(GroupMessage::getId)
                        .last("LIMIT 1")
        );

        // 3. 更新该成员的“最后清空ID”
        // ✅ 优化：无论有没有消息，都执行一次更新，确保状态同步
        long clearId = (latestMsg != null) ? latestMsg.getId() : 0L;
        
        chatGroupMemberMapper.update(null,
                new LambdaUpdateWrapper<ChatGroupMember>()
                        .eq(ChatGroupMember::getGroupId, groupId)
                        .eq(ChatGroupMember::getUserId, userId)
                        .set(ChatGroupMember::getLastClearedMsgId, clearId)
        );
        
        log.info("用户{}已清空群聊{}的历史记录，截止ID:{}", userId, groupId, clearId);
    }

    @Override
    public List<ConversationSessionVO> getConversationList(Long userId) {
        List<ConversationSessionVO> sessions = new ArrayList<>();

        // 1. 获取私聊会话
        List<PrivateMessage> latestPrivateMessages = getLatestPrivateMessages(userId);
        
        // ✅ 优化：批量统计所有好友的未读消息数（从 N+1 降为 1）
        Set<Long> friendUserIds = latestPrivateMessages.stream()
                .map(msg -> msg.getSenderId().equals(userId) ? msg.getReceiverId() : msg.getSenderId())
                .collect(Collectors.toSet());
        
        Map<Long, Integer> unreadCountMap = new HashMap<>();
        if (!friendUserIds.isEmpty()) {
            // 一次性查询所有好友发来的未读消息
            List<Map<String, Object>> results = privateMessageMapper.selectMaps(
                    new LambdaQueryWrapper<PrivateMessage>()
                            .eq(PrivateMessage::getReceiverId, userId)
                            .in(PrivateMessage::getSenderId, friendUserIds)
                            .eq(PrivateMessage::getIsRead, 0)
                            .eq(PrivateMessage::getDeletedByReceiver, 0)
                            .select(PrivateMessage::getSenderId)
            );
            
            // 统计每个好友的未读数
            for (Map<String, Object> row : results) {
                Long senderId = ((Number) row.get("sender_id")).longValue();
                unreadCountMap.merge(senderId, 1, Integer::sum);
            }
        }
        
        for (PrivateMessage msg : latestPrivateMessages) {
            ConversationSessionVO session = new ConversationSessionVO();
            session.setConversationType("private");
            
            Long friendUserId = msg.getSenderId().equals(userId) ? msg.getReceiverId() : msg.getSenderId();
            session.setFriendUserId(friendUserId);
            
            User friend = userMapper.selectById(friendUserId);
            if (friend != null) {
                session.setFriendUsername(friend.getUsername());
                session.setFriendAvatar(friend.getAvatar());
            }
            
            session.setLastMessage(msg.getContent());
            session.setLastMessageTime(msg.getCreateTime());
            
            // 从批量查询结果中获取未读数
            session.setUnreadCount(unreadCountMap.getOrDefault(friendUserId, 0));
            
            sessions.add(session);
        }

        // 2. 获取群聊会话
        List<ChatGroupVO> myGroups = getMyGroups(userId);
        for (ChatGroupVO group : myGroups) {
            ConversationSessionVO session = new ConversationSessionVO();
            session.setConversationType("group");
            session.setGroupId(group.getId());
            session.setGroupName(group.getGroupName());
            session.setGroupAvatar(group.getAvatar());
            session.setLastMessage(group.getLastMessage());
            session.setLastMessageTime(group.getLastMessageTime());
            session.setUnreadCount(group.getUnreadCount());
            sessions.add(session);
        }

        // 按最后消息时间排序
        sessions.sort((a, b) -> {
            if (a.getLastMessageTime() == null) return 1;
            if (b.getLastMessageTime() == null) return -1;
            return b.getLastMessageTime().compareTo(a.getLastMessageTime());
        });

        return sessions;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createGroup(Long userId, CreateGroupDTO dto) {
        // 1. 创建群聊
        ChatGroup group = new ChatGroup();
        group.setGroupName(dto.getGroupName());
        group.setOwnerId(userId);
        group.setMemberCount(1); // 初始只有群主
        group.setCreateTime(LocalDateTime.now());
        
        // 上传群头像
        if (dto.getAvatar() != null && !dto.getAvatar().isEmpty()) {
            try {
                if(!StrUtil.isBlank(dto.getAvatar().getOriginalFilename())){
                    String avatarUrl = aliyunOSSOperator.upload(dto.getAvatar(), dto.getAvatar().getOriginalFilename());
                    group.setAvatar(avatarUrl);
                }
            } catch (Exception e) {
                log.error("群头像上传失败", e);
            }
        }
        
        chatGroupMapper.insert(group);

        // 2. 添加群主为成员
        ChatGroupMember ownerMember = new ChatGroupMember();
        ownerMember.setGroupId(group.getId());
        ownerMember.setUserId(userId);
        ownerMember.setRole(2); // 群主
        ownerMember.setJoinTime(LocalDateTime.now());
        ownerMember.setIsRemoved(0);
        chatGroupMemberMapper.insert(ownerMember);

        // 3. 添加初始成员
        if (dto.getMemberIds() != null && !dto.getMemberIds().isEmpty()) {
            for (Long memberId : dto.getMemberIds()) {
                if (!memberId.equals(userId)) { // 避免重复添加群主
                    ChatGroupMember member = new ChatGroupMember();
                    member.setGroupId(group.getId());
                    member.setUserId(memberId);
                    member.setRole(0); // 普通成员
                    member.setJoinTime(LocalDateTime.now());
                    member.setIsRemoved(0);
                    chatGroupMemberMapper.insert(member);
                    
                    group.setMemberCount(group.getMemberCount() + 1);
                }
            }
            chatGroupMapper.updateById(group);
        }

        return group.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public GroupMessageVO sendGroupMessage(Long senderId, GroupMessageDTO dto) {
        // 1. 验证是否是群成员
        ChatGroupMember member = chatGroupMemberMapper.selectOne(
                new LambdaQueryWrapper<ChatGroupMember>()
                        .eq(ChatGroupMember::getGroupId, dto.getGroupId())
                        .eq(ChatGroupMember::getUserId, senderId)
                        .eq(ChatGroupMember::getIsRemoved, 0)
        );

        if (member == null) {
            throw new BusinessException(ResultCode.NOT_GROUP_MEMBER);
        }

        // 2. 处理文件上传并获取消息内容
        MessageContentResult contentResult = uploadAndProcessFile(dto.getFile(), dto.getContent(), dto.getMessageType());

        // 3. 保存消息
        GroupMessage message = new GroupMessage();
        message.setGroupId(dto.getGroupId());
        message.setSenderId(senderId);
        message.setMessageType(contentResult.messageType);
        message.setContent(contentResult.content);
        message.setCreateTime(LocalDateTime.now());
        groupMessageMapper.insert(message);

        // 4. 推送给群内其他在线成员
        pushGroupMessage(dto.getGroupId(), message, senderId);

        return convertToGroupMessageVO(message);
    }

    @Override
    public CursorPageResult<GroupMessageVO> getGroupMessageHistory(Long userId, MessageQueryDTO queryDTO) {
        if (queryDTO.getGroupId() == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "群聊ID不能为空");
        }

        // 验证是否是群成员
        ChatGroupMember member = validateGroupMember(userId, queryDTO.getGroupId());
        
        int pageSize = queryDTO.getPageSize() != null ? queryDTO.getPageSize() : 20;
        Long cursor = queryDTO.getCursor();
        
        LambdaQueryWrapper<GroupMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GroupMessage::getGroupId, queryDTO.getGroupId());
        
        // 只查询比“最后清空ID”大的消息
        if (member.getLastClearedMsgId() != null && member.getLastClearedMsgId() > 0) {
            wrapper.gt(GroupMessage::getId, member.getLastClearedMsgId());
        }
        
        // 游标分页：基于消息ID进行分页
        if (cursor != null) {
            wrapper.lt(GroupMessage::getId, cursor);
        }
        wrapper.orderByDesc(GroupMessage::getId);
        
        // 多取一条数据用于判断是否有下一页
        wrapper.last("LIMIT " + (pageSize + 1));
        
        List<GroupMessage> messages = groupMessageMapper.selectList(wrapper);
        
        // 判断是否有更多数据
        boolean hasNext = messages.size() > pageSize;
        Long nextCursor = null;
        
        if (hasNext) {
            // 移除多余的一条数据
            messages = messages.subList(0, pageSize);
            // 设置下一页游标为最后一条记录的ID
            nextCursor = messages.getLast().getId();
        }
        
        // 转换为VO
        List<GroupMessageVO> voList = messages.stream()
                .map(this::convertToGroupMessageVO)
                .collect(Collectors.toList());
        
        // 返回专门的游标分页结果
        return CursorPageResult.<GroupMessageVO>builder()
                .records(voList)
                .nextCursor(nextCursor)
                .hasNext(hasNext)
                .pageSize(pageSize)
                .build();
    }

    @Override
    public List<ChatGroupVO> getMyGroups(Long userId) {
        // 1. 获取用户所在的群成员信息（只查已通过的）
        List<ChatGroupMember> members = chatGroupMemberMapper.selectList(
                new LambdaQueryWrapper<ChatGroupMember>()
                        .eq(ChatGroupMember::getUserId, userId)
                        .eq(ChatGroupMember::getIsRemoved, 0)
                        .eq(ChatGroupMember::getStatus, 1) // ✅ 只查询已通过审核的成员
        );

        if (members.isEmpty()) {
            return List.of();
        }

        List<Long> groupIds = members.stream()
                .map(ChatGroupMember::getGroupId)
                .collect(Collectors.toList());

        // 2. 批量获取群聊基本信息
        List<ChatGroup> groups = chatGroupMapper.selectBatchIds(groupIds);
        Map<Long, ChatGroupMember> memberMap = members.stream()
                .collect(Collectors.toMap(ChatGroupMember::getGroupId, m -> m));

        // 3.  核心优化：一次性批量统计所有群的未读消息数 (从 N+1 降为 1)
        List<MemberUnreadItem> items = members.stream()
                .filter(m -> m.getLastReadMsgId() != null) // 只处理有已读记录的
                .map(m -> new MemberUnreadItem(m.getGroupId(), m.getLastReadMsgId()))
                .collect(Collectors.toList());

        Map<Long, Integer> unreadCountMap = new HashMap<>();
        if (!items.isEmpty()) {
            List<GroupUnreadCountVO> results = groupMessageMapper.batchSelectUnreadCount(items);
            for (GroupUnreadCountVO vo : results) {
                unreadCountMap.put(vo.getGroupId(), vo.getCount());
            }
        }

        // 4. 组装 VO
        return groups.stream()
                .map(group -> convertToChatGroupVO(group, memberMap.get(group.getId()), unreadCountMap.getOrDefault(group.getId(), 0)))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void joinGroup(Long userId, Long groupId) {
        // 检查是否已是成员
        ChatGroupMember existing = chatGroupMemberMapper.selectOne(
                new LambdaQueryWrapper<ChatGroupMember>()
                        .eq(ChatGroupMember::getGroupId, groupId)
                        .eq(ChatGroupMember::getUserId, userId)
        );

        if (existing != null) {
            if (existing.getIsRemoved() == 1) {
                // 重新申请加入，重置为待审核状态
                existing.setIsRemoved(0);
                existing.setStatus(0); // 待审核
                existing.setJoinTime(LocalDateTime.now());
                chatGroupMemberMapper.updateById(existing);
            } else if (existing.getStatus() == 0) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "您的入群申请正在审核中");
            } else if (existing.getStatus() == 1) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "您已经是该群成员");
            } else {
                // 之前被拒绝过，可以重新申请
                existing.setStatus(0);
                existing.setJoinTime(LocalDateTime.now());
                chatGroupMemberMapper.updateById(existing);
            }
        } else {
            ChatGroupMember member = new ChatGroupMember();
            member.setGroupId(groupId);
            member.setUserId(userId);
            member.setRole(0);
            member.setJoinTime(LocalDateTime.now());
            member.setIsRemoved(0);
            member.setStatus(0); //  新申请的成员默认为待审核
            chatGroupMemberMapper.insert(member);
        }
        
        //  通过 WebSocket 通知群主和管理员有新的入群申请
        notifyGroupAdmins(groupId, userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approveGroupJoin(Long operatorId, ApproveGroupJoinDTO dto) {
        // 1. 验证操作人权限（必须是群主或管理员）
        ChatGroupMember operator = chatGroupMemberMapper.selectOne(
                new LambdaQueryWrapper<ChatGroupMember>()
                        .eq(ChatGroupMember::getGroupId, dto.getGroupId())
                        .eq(ChatGroupMember::getUserId, operatorId)
                        .eq(ChatGroupMember::getIsRemoved, 0)
                        .in(ChatGroupMember::getRole, 1, 2) // 1-管理员，2-群主
        );

        if (operator == null) {
            throw new BusinessException(ResultCode.FORBIDDEN, "您没有权限审批该申请");
        }

        // 2. 查找申请人的记录
        ChatGroupMember applicant = chatGroupMemberMapper.selectOne(
                new LambdaQueryWrapper<ChatGroupMember>()
                        .eq(ChatGroupMember::getGroupId, dto.getGroupId())
                        .eq(ChatGroupMember::getUserId, dto.getApplicantId())
        );

        if (applicant == null) {
            throw new BusinessException(ResultCode.GROUP_NOT_EXIST, "未找到该申请人的记录");
        }

        if (applicant.getStatus() != 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "该申请已处理过");
        }

        // 3. 执行审批
        if (dto.getAction() == 1) {
            // 同意
            applicant.setStatus(1);
            chatGroupMemberMapper.updateById(applicant);

            // 更新群成员数
            ChatGroup group = chatGroupMapper.selectById(dto.getGroupId());
            if (group != null) {
                group.setMemberCount(group.getMemberCount() + 1);
                chatGroupMapper.updateById(group);
            }
            
            //  通过 WebSocket 通知申请人
            notifyApplicant(dto.getApplicantId(), dto.getGroupId(), true, group != null ? group.getGroupName() : "未知群聊");
        } else if (dto.getAction() == 2) {
            // 拒绝
            applicant.setStatus(2);
            chatGroupMemberMapper.updateById(applicant);
            
            //  通过 WebSocket 通知申请人
            ChatGroup group = chatGroupMapper.selectById(dto.getGroupId());
            notifyApplicant(dto.getApplicantId(), dto.getGroupId(), false, group != null ? group.getGroupName() : "未知群聊");
        } else {
            throw new BusinessException(ResultCode.BAD_REQUEST, "无效的审批操作");
        }
    }

    @Override
    public List<PendingGroupJoinVO> getPendingJoinRequests(Long operatorId, Long groupId) {
        // 1. 验证操作人权限（必须是群主或管理员）
        ChatGroupMember operator = chatGroupMemberMapper.selectOne(
                new LambdaQueryWrapper<ChatGroupMember>()
                        .eq(ChatGroupMember::getGroupId, groupId)
                        .eq(ChatGroupMember::getUserId, operatorId)
                        .eq(ChatGroupMember::getIsRemoved, 0)
                        .in(ChatGroupMember::getRole, 1, 2)
        );

        if (operator == null) {
            throw new BusinessException(ResultCode.FORBIDDEN, "您没有权限查看该群的待审申请");
        }

        // 2. 查询所有待审核的申请
        List<ChatGroupMember> pendingMembers = chatGroupMemberMapper.selectList(
                new LambdaQueryWrapper<ChatGroupMember>()
                        .eq(ChatGroupMember::getGroupId, groupId)
                        .eq(ChatGroupMember::getStatus, 0)
                        .eq(ChatGroupMember::getIsRemoved, 0)
                        .orderByDesc(ChatGroupMember::getJoinTime)
        );

        // 3. 组装 VO
        return pendingMembers.stream().map(member -> {
            PendingGroupJoinVO vo = new PendingGroupJoinVO();
            vo.setMemberId(member.getId());
            vo.setUserId(member.getUserId());
            vo.setJoinTime(member.getJoinTime());
            
            // 获取申请人信息
            User user = userMapper.selectById(member.getUserId());
            if (user != null) {
                vo.setUsername(user.getUsername());
                vo.setAvatar(user.getAvatar());
            }
            
            return vo;
        }).collect(Collectors.toList());
    }

    /**
     * 通知申请人审批结果
     */
    private void notifyApplicant(Long applicantId, Long groupId, boolean approved, String groupName) {
        if (webSocketHandler.isUserOnline(applicantId)) {
            Map<String, Object> notification = new HashMap<>();
            notification.put("type", "group_join_result");
            notification.put("groupId", groupId);
            notification.put("groupName", groupName);
            notification.put("approved", approved);
            notification.put("message", approved ? "您的入群申请已通过" : "您的入群申请被拒绝");
            webSocketHandler.sendMessageToUser(applicantId, notification);
            log.info("已通知用户{}入群申请{}", applicantId, approved ? "通过" : "被拒绝");
        }
    }

    /**
     * 通知群主和管理员有新的入群申请
     */
    private void notifyGroupAdmins(Long groupId, Long applicantId) {
        // 查询群聊信息
        ChatGroup group = chatGroupMapper.selectById(groupId);
        if (group == null) {
            return;
        }
        
        // 查询申请人信息
        User applicant = userMapper.selectById(applicantId);
        String applicantName = applicant != null ? applicant.getUsername() : "未知用户";
        
        // 查询所有群主和管理员
        List<ChatGroupMember> admins = chatGroupMemberMapper.selectList(
                new LambdaQueryWrapper<ChatGroupMember>()
                        .eq(ChatGroupMember::getGroupId, groupId)
                        .in(ChatGroupMember::getRole, 1, 2) // 1-管理员，2-群主
                        .eq(ChatGroupMember::getIsRemoved, 0)
        );
        
        // 逐个通知在线的管理员
        for (ChatGroupMember admin : admins) {
            if (webSocketHandler.isUserOnline(admin.getUserId())) {
                Map<String, Object> notification = new HashMap<>();
                notification.put("type", "new_group_join_request");
                notification.put("groupId", groupId);
                notification.put("groupName", group.getGroupName());
                notification.put("applicantId", applicantId);
                notification.put("applicantName", applicantName);
                notification.put("message", String.format("用户 %s 申请加入群聊", applicantName));
                webSocketHandler.sendMessageToUser(admin.getUserId(), notification);
                log.info("已通知管理员{}有新的入群申请", admin.getUserId());
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void leaveGroup(Long userId, Long groupId) {
        ChatGroupMember member = chatGroupMemberMapper.selectOne(
                new LambdaQueryWrapper<ChatGroupMember>()
                        .eq(ChatGroupMember::getGroupId, groupId)
                        .eq(ChatGroupMember::getUserId, userId)
                        .eq(ChatGroupMember::getIsRemoved, 0)
        );

        if (member == null) {
            throw new BusinessException(ResultCode.NOT_GROUP_MEMBER);
        }

        ChatGroup group = chatGroupMapper.selectById(groupId);
        if (group != null && group.getOwnerId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "群主不能退出群聊，请先转让群主或解散群聊");
        }

        member.setIsRemoved(1);
        chatGroupMemberMapper.updateById(member);

        // 更新群成员数
        if (group != null) {
            group.setMemberCount(Math.max(0, group.getMemberCount() - 1));
            chatGroupMapper.updateById(group);
        }
    }

    @Override
    public ChatGroupVO getGroupDetail(Long userId, Long groupId) {
        ChatGroup group = chatGroupMapper.selectById(groupId);
        if (group == null) {
            throw new BusinessException(ResultCode.GROUP_NOT_EXIST);
        }

        //  验证群成员身份
        ChatGroupMember member = validateGroupMember(userId, groupId);

        //  核心优化：获取详情时自动标记为已读
        markGroupMessagesAsRead(userId, groupId);

        return convertToChatGroupVO(group, member, 0); // 既然刚点开，未读数即为 0
    }

    /**
     * 验证用户是否为群成员（已通过审核且未被移除）
     */
    private ChatGroupMember validateGroupMember(Long userId, Long groupId) {
        ChatGroupMember member = chatGroupMemberMapper.selectOne(
                new LambdaQueryWrapper<ChatGroupMember>()
                        .eq(ChatGroupMember::getGroupId, groupId)
                        .eq(ChatGroupMember::getUserId, userId)
                        .eq(ChatGroupMember::getIsRemoved, 0)
                        .eq(ChatGroupMember::getStatus, 1)
        );

        if (member == null) {
            throw new BusinessException(ResultCode.NOT_GROUP_MEMBER);
        }

        return member;
    }

    /**
     * 标记群聊消息为已读（更新最后已读ID）
     */
    private void markGroupMessagesAsRead(Long userId, Long groupId) {
        // 获取当前群最新的消息ID
        GroupMessage latestMsg = groupMessageMapper.selectOne(
                new LambdaQueryWrapper<GroupMessage>()
                        .eq(GroupMessage::getGroupId, groupId)
                        .orderByDesc(GroupMessage::getId)
                        .last("LIMIT 1")
        );

        if (latestMsg != null) {
            chatGroupMemberMapper.update(null,
                    new LambdaUpdateWrapper<ChatGroupMember>()
                            .eq(ChatGroupMember::getGroupId, groupId)
                            .eq(ChatGroupMember::getUserId, userId)
                            .set(ChatGroupMember::getLastReadMsgId, latestMsg.getId())
            );
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markAllGroupMessagesAsRead(Long userId, Long groupId) {
        // ✅ 验证群成员身份
        validateGroupMember(userId, groupId);

        // 更新最后已读消息ID为当前最新消息ID
        markGroupMessagesAsRead(userId, groupId);
        
        log.info("用户{}已标记群聊{}的所有消息为已读", userId, groupId);
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 统一处理文件上传和消息类型识别
     */
    private MessageContentResult uploadAndProcessFile(MultipartFile file,
                                                      String originalContent, Integer originalType) {
        String content = originalContent;
        Integer messageType = originalType;
        
        if (file != null && !file.isEmpty()) {
            try {
                String fileName = file.getOriginalFilename();
                if (fileName == null || fileName.isEmpty()) {
                    fileName = "unknown_file";
                }
                content = aliyunOSSOperator.upload(file, fileName);
                
                if (originalType == null || originalType == 1) {
                    String contentType = file.getContentType();
                    if (contentType != null && contentType.startsWith("image/")) {
                        messageType = MessageType.IMAGE.getCode();
                    } else {
                        messageType = MessageType.FILE.getCode();
                    }
                }
            } catch (Exception e) {
                log.error("文件上传失败", e);
                throw new BusinessException(ResultCode.INTERNAL_SERVER_ERROR, "文件上传失败");
            }
        }

        if (content == null || content.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "消息内容不能为空");
        }

        return new MessageContentResult(content, messageType);
    }

    /**
     * 推送私聊消息
     */
    private void pushPrivateMessage(Long receiverId, PrivateMessage message, Long senderId) {
        if (webSocketHandler.isUserOnline(receiverId)) {
            // ✅ 优化：获取发送者基本信息，方便前端直接渲染会话列表
            User sender = userMapper.selectById(senderId);
            String senderName = sender != null ? sender.getUsername() : "未知用户";
            String senderAvatar = sender != null ? sender.getAvatar() : "";

            Map<String, Object> pushMessage = buildPushMessage("new_message", message.getId(), senderId, 
                    message.getMessageType(), message.getContent(), message.getCreateTime(), null, receiverId);
            
            // 补充发送者信息
            pushMessage.put("senderName", senderName);
            pushMessage.put("senderAvatar", senderAvatar);

            webSocketHandler.sendMessageToUser(receiverId, pushMessage);
            log.info("消息已推送给用户{}", receiverId);
        } else {
            log.info("用户{}不在线，消息已保存为离线消息", receiverId);
        }
    }

    /**
     * 推送群聊消息
     */
    private void pushGroupMessage(Long groupId, GroupMessage message, Long senderId) {
        List<ChatGroupMember> members = chatGroupMemberMapper.selectList(
                new LambdaQueryWrapper<ChatGroupMember>()
                        .eq(ChatGroupMember::getGroupId, groupId)
                        .eq(ChatGroupMember::getIsRemoved, 0)
                        .eq(ChatGroupMember::getStatus, 1) //  只推送给已通过的成员
        );

        for (ChatGroupMember m : members) {
            if (!m.getUserId().equals(senderId) && webSocketHandler.isUserOnline(m.getUserId())) {
                Map<String, Object> pushMessage = buildPushMessage("new_group_message", message.getId(), senderId, 
                        message.getMessageType(), message.getContent(), message.getCreateTime(), groupId, m.getUserId());
                webSocketHandler.sendMessageToUser(m.getUserId(), pushMessage);
            }
        }
    }

    /**
     * 构建 WebSocket 推送消息体
     */
    private Map<String, Object> buildPushMessage(String type, Long messageId, Long senderId, 
                                                 Integer messageType, String content, LocalDateTime createTime, Long groupId, Long receiverId) {
        Map<String, Object> pushMessage = new HashMap<>();
        pushMessage.put("type", type);
        pushMessage.put("messageId", messageId);
        pushMessage.put("senderId", senderId);
        
        //  只有私聊才添加 receiverId 字段
        if (receiverId != null && groupId == null) {
            pushMessage.put("receiverId", receiverId);
        }
        
        pushMessage.put("messageType", messageType);
        pushMessage.put("content", content);
        pushMessage.put("createTime", createTime.toString());
        if (groupId != null) {
            pushMessage.put("groupId", groupId);
        }
        return pushMessage;
    }

    /**
     * 消息内容结果封装类
     */
    private static class MessageContentResult {
        String content;
        Integer messageType;

        MessageContentResult(String content, Integer messageType) {
            this.content = content;
            this.messageType = messageType;
        }
    }

    private PrivateMessageVO convertToPrivateMessageVO(PrivateMessage message) {
        PrivateMessageVO vo = BeanUtil.copyProperties(message, PrivateMessageVO.class);
        
        // 获取发送者信息
        User sender = userMapper.selectById(message.getSenderId());
        if (sender != null) {
            vo.setSenderUsername(sender.getUsername());
            vo.setSenderAvatar(sender.getAvatar());
        }
        
        return vo;
    }

    private GroupMessageVO convertToGroupMessageVO(GroupMessage message) {
        GroupMessageVO vo = BeanUtil.copyProperties(message, GroupMessageVO.class);
        
        User sender = userMapper.selectById(message.getSenderId());
        if (sender != null) {
            vo.setSenderUsername(sender.getUsername());
            vo.setSenderAvatar(sender.getAvatar());
        }
        
        return vo;
    }

    private ChatGroupVO convertToChatGroupVO(ChatGroup group, ChatGroupMember member, int unreadCount) {
        ChatGroupVO vo = BeanUtil.copyProperties(group, ChatGroupVO.class);
        
        // 获取群主信息
        User owner = userMapper.selectById(group.getOwnerId());
        if (owner != null) {
            vo.setOwnerUsername(owner.getUsername());
        }
        
        // 设置我的角色和未读数
        if (member != null) {
            vo.setMyRole(member.getRole());
            vo.setUnreadCount(unreadCount);
        } else {
            vo.setUnreadCount(0);
        }
        
        // 获取最后一条消息
        GroupMessage lastMessage = groupMessageMapper.selectOne(
                new LambdaQueryWrapper<GroupMessage>()
                        .eq(GroupMessage::getGroupId, group.getId())
                        .orderByDesc(GroupMessage::getCreateTime)
                        .last("LIMIT 1")
        );
        
        if (lastMessage != null) {
            vo.setLastMessage(lastMessage.getContent());
            vo.setLastMessageTime(lastMessage.getCreateTime());
        }
        
        return vo;
    }

    private List<PrivateMessage> getLatestPrivateMessages(Long userId) {
        // 查询每个好友的最新消息（排除已删除的）
        List<PrivateMessage> messages = privateMessageMapper.selectList(
                new LambdaQueryWrapper<PrivateMessage>()
                        .and(w -> w
                                .eq(PrivateMessage::getSenderId, userId)
                                .eq(PrivateMessage::getDeletedBySender, 0)
                                .or()
                                .eq(PrivateMessage::getReceiverId, userId)
                                .eq(PrivateMessage::getDeletedByReceiver, 0)
                        )
                        .orderByDesc(PrivateMessage::getCreateTime)
        );

        // 按好友去重，只保留最新消息
        Map<Long, PrivateMessage> latestMap = new LinkedHashMap<>();
        for (PrivateMessage msg : messages) {
            Long friendUserId = msg.getSenderId().equals(userId) ? msg.getReceiverId() : msg.getSenderId();
            latestMap.putIfAbsent(friendUserId, msg);
        }

        return new ArrayList<>(latestMap.values());
    }
}
