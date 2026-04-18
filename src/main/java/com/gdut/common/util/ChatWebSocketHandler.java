package com.gdut.common.util;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.gdut.common.enums.NoteType;
import com.gdut.common.enums.NoteVisibility;
import com.gdut.common.enums.WebSocketMessageType;
import static com.gdut.common.enums.WebSocketMessageType.*;
import com.gdut.domain.entity.chat.PrivateMessage;
import com.gdut.domain.entity.chat.GroupMessage;
import com.gdut.domain.entity.chat.ChatGroupMember;
import com.gdut.domain.entity.note.Note;
import com.gdut.domain.entity.note.NoteFriendPermission;
import com.gdut.domain.entity.friend.Friend;
import com.gdut.common.enums.FriendStatus;
import com.gdut.mapper.NoteFriendPermissionMapper;
import com.gdut.mapper.NoteMapper;
import com.gdut.mapper.PrivateMessageMapper;
import com.gdut.mapper.GroupMessageMapper;
import com.gdut.mapper.ChatGroupMemberMapper;
import com.gdut.mapper.FriendMapper;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket消息处理器
 * 负责管理用户在线状态、消息收发
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper;
    private final PrivateMessageMapper privateMessageMapper;
    private final NoteMapper noteMapper;
    private final NoteFriendPermissionMapper noteFriendPermissionMapper;
    private final GroupMessageMapper groupMessageMapper;
    private final ChatGroupMemberMapper chatGroupMemberMapper;
    private final FriendMapper friendMapper;

    // 存储在线用户的WebSocket Session，key为userId
    private static final Map<Long, WebSocketSession> ONLINE_USERS = new ConcurrentHashMap<>();
    
    // 存储笔记编辑者信息，key为noteId，value为EditLockInfo（包含userId和最后活动时间）
    private static final Map<Long, EditLockInfo> NOTE_EDITORS = new ConcurrentHashMap<>();
    
    // 存储笔记的观察者，key为noteId，value为正在查看的用户ID集合
    private static final Map<Long, Set<Long>> NOTE_VIEWERS = new ConcurrentHashMap<>();
    
    // 编辑锁超时时间（毫秒），默认10分钟
    private static final long EDIT_LOCK_TIMEOUT = 10 * 60 * 1000;
    
    /**
     * 编辑锁信息
     */
    private static class EditLockInfo {
        private final Long userId;
        private volatile long lastActivityTime;
        
        public EditLockInfo(Long userId) {
            this.userId = userId;
            this.lastActivityTime = System.currentTimeMillis();
        }
        
        public Long getUserId() {
            return userId;
        }
        
        public long getLastActivityTime() {
            return lastActivityTime;
        }
        
        public void updateActivityTime() {
            this.lastActivityTime = System.currentTimeMillis();
        }
        
        public boolean isExpired() {
            return System.currentTimeMillis() - lastActivityTime > EDIT_LOCK_TIMEOUT;
        }
    }

    /**
     * WebSocket连接建立成功后调用
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session){
        Long userId = (Long) session.getAttributes().get("userId");
        if (userId != null) {
            ONLINE_USERS.put(userId, session);
            log.info("用户上线：userId={}, 当前在线人数={}", userId, ONLINE_USERS.size());
            
            // 发送连接成功消息
            sendMessage(session, Map.of(
                "type", CONNECTED.getValue(),
                "message", "连接成功",
                "userId", userId
            ));
            
            // 新增：上线后返回离线消息总数
            pushOfflineMessageCount(userId, session);
            
            // 新增：通知好友我上线了
            notifyFriendsOnlineStatus(userId, true);
        }
    }

    /**
     * 接收到客户端消息时调用
     */
    @Override
    protected void handleTextMessage(WebSocketSession session, @NonNull TextMessage message) {
        Long userId = (Long) session.getAttributes().get("userId");
        if (userId == null) {
            return;
        }

        String payload = message.getPayload();
        log.info("收到用户{}的消息：{}", userId, payload);

        try {
            // 解析消息
            // 使用 TypeReference 明确指定泛型，消除 unchecked assignment 警告
            Map<String, Object> msgMap = objectMapper.readValue(payload, new TypeReference<>() {});
            String msgTypeStr = (String) msgMap.get("type");
            WebSocketMessageType msgType = WebSocketMessageType.fromValue(msgTypeStr);
            
            if (msgType == null) {
                log.warn("未知的消息类型：{}", msgTypeStr);
                return;
            }
            
            switch (msgType) {
                case PING -> 
                    // 心跳响应
                    sendMessage(session, Map.of("type", PONG.getValue()));
                case MARK_READ -> {
                    Object messageIdObj = msgMap.get("messageId");
                    Object friendUserIdObj = msgMap.get("friendUserId");
                    Object upToMessageIdObj = msgMap.get("upToMessageId");
                    Object groupIdObj = msgMap.get("groupId");

                    boolean success = false;
                    String responseMessage = "";

                    // 群聊标记已读
                    if (groupIdObj != null) {
                        Long groupId = Long.valueOf(groupIdObj.toString());
                        markGroupMessagesAsRead(userId, groupId);
                        success = true;
                        responseMessage = "群聊消息已标记为已读";
                    }
                    // 私聊批量标记已读
                    else if (upToMessageIdObj != null && friendUserIdObj != null) {
                        Long friendUserId = Long.valueOf(friendUserIdObj.toString());
                        Long upToMessageId = Long.valueOf(upToMessageIdObj.toString());
                        markMessagesAsReadBatch(userId, friendUserId, upToMessageId);
                        success = true;
                        responseMessage = "私聊消息已批量标记为已读";
                    }
                    // 私聊单条标记已读
                    else if (messageIdObj != null) {
                        Long messageId = Long.valueOf(messageIdObj.toString());
                        markMessageAsRead(messageId, userId);
                        success = true;
                        responseMessage = "消息已标记为已读";
                    } else {
                        responseMessage = "无效的标记已读请求";
                    }
                    
                    // 发送响应给前端
                    sendMessage(session, Map.of(
                        "type", "mark_read_ack",
                        "success", success,
                        "message", responseMessage
                    ));
                }
                case NOTE_EDIT_REQUEST -> {
                    // 请求获取笔记编辑锁
                    Long noteId = Long.valueOf(msgMap.get("noteId").toString());
                    handleNoteEditRequest(userId, noteId, session);
                }
                case NOTE_EDIT_RELEASE -> {
                    // 释放笔记编辑锁
                    Long noteId = Long.valueOf(msgMap.get("noteId").toString());
                    handleNoteEditRelease(userId, noteId);
                }
                case NOTE_CONTENT_UPDATE -> {
                    // 实时同步笔记内容变更
                    Long noteId = Long.valueOf(msgMap.get("noteId").toString());
                    String content = (String) msgMap.get("content");
                    Integer version = msgMap.get("version") != null ? Integer.valueOf(msgMap.get("version").toString()) : null;
                    handleNoteContentUpdate(userId, noteId, content, version);
                }
                case NOTE_VIEW_START -> {
                    // 用户开始查看笔记
                    Long noteId = Long.valueOf(msgMap.get("noteId").toString());
                    addUserToNoteViewers(userId, noteId);
                }
                case NOTE_VIEW_END -> {
                    // 用户停止查看笔记
                    Long noteId = Long.valueOf(msgMap.get("noteId").toString());
                    removeUserFromNoteViewers(userId, noteId);
                }
                default -> log.warn("未处理的消息类型：{}", msgType);
            }
        } catch (Exception e) {
            log.error("处理WebSocket消息失败", e);
        }
    }

    /**
     * 连接关闭后调用
     */
    @Override
    @SuppressWarnings("resource")
    public void afterConnectionClosed(WebSocketSession session, @NonNull CloseStatus status) {
        Long userId = (Long) session.getAttributes().get("userId");
        if (userId != null) {
            // 先通知好友我下线了（在移除之前）
            notifyFriendsOnlineStatus(userId, false);
            
            ONLINE_USERS.remove(userId);
            log.info("用户下线：userId={}, 当前在线人数={}", userId, ONLINE_USERS.size());
            
            // 查找并释放该用户持有的编辑锁（理论上只有一个）
            NOTE_EDITORS.entrySet().removeIf(entry -> {
                if (entry.getValue().getUserId().equals(userId)) {
                    Long noteId = entry.getKey();
                    log.info("用户{}断开连接，自动释放笔记{}的编辑锁", userId, noteId);
                    
                    // ✅ 同步清除数据库中的编辑锁
                    Note updateNote = new Note();
                    updateNote.setId(noteId);
                    updateNote.setEditingUserId(null);
                    updateNote.setEditingLockTime(null);
                    noteMapper.updateById(updateNote);
                    
                    notifyLockReleasedWithoutRemove(noteId);
                    return true;
                }
                return false;
            });
            
            // 清理该用户的所有观察者身份
            NOTE_VIEWERS.forEach((noteId, viewers) -> {
                viewers.remove(userId);
                if (viewers.isEmpty()) {
                    NOTE_VIEWERS.remove(noteId);
                }
            });
        }
    }

    /**
     * 处理传输错误
     */
    @Override
    public void handleTransportError(WebSocketSession session,@NonNull Throwable exception) throws Exception {
        Long userId = (Long) session.getAttributes().get("userId");
        log.error("WebSocket传输错误：userId={}", userId, exception);
        if (session.isOpen()) {
            session.close();
        }
    }

    /**
     * 向指定用户发送消息
     */
    public void sendMessageToUser(Long userId, Map<String, Object> message) {
        WebSocketSession session = ONLINE_USERS.get(userId);
        if (session != null && session.isOpen()) {
            sendMessage(session, message);
        } else {
            log.info("用户{}不在线，消息已保存为离线消息", userId);
        }
    }

    /**
     * 检查用户是否在线
     */
    public boolean isUserOnline(Long userId) {
        WebSocketSession session = ONLINE_USERS.get(userId);
        return session != null && session.isOpen();
    }

    /**
     * 发送消息到WebSocket会话
     */
    private void sendMessage(WebSocketSession session, Map<String, Object> message) {
        // 检查session是否有效且打开
        if (session == null || !session.isOpen()) {
            log.debug("WebSocket会话已关闭或为空，跳过发送消息");
            return;
        }
        
        try {
            String jsonMessage = objectMapper.writeValueAsString(message);
            synchronized (session) {
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage(jsonMessage));
                }
            }
        } catch (IllegalStateException e) {
            // WebSocket端点状态异常（如正在写入时再次写入），忽略该错误
            log.debug("WebSocket端点状态异常，跳过发送: {}", e.getMessage());
        } catch (IOException e) {
            log.error("发送WebSocket消息失败", e);
        }
    }

    /**
     * 标记消息为已读
     */
    private void markMessageAsRead(Long messageId, Long userId) {
        PrivateMessage message = privateMessageMapper.selectById(messageId);
        if (message != null && message.getReceiverId().equals(userId)) {
            message.setIsRead(1);
            privateMessageMapper.updateById(message);
            log.info("消息已标记为已读：messageId={}, userId={}", messageId, userId);
        }
    }
    
    /**
     * 批量标记消息为已读（标记与某好友的指定消息之前的所有消息）
     */
    private void markMessagesAsReadBatch(Long userId, Long friendUserId, Long upToMessageId) {
        LambdaUpdateWrapper<PrivateMessage> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(PrivateMessage::getReceiverId, userId)
               .eq(PrivateMessage::getSenderId, friendUserId)
               .le(PrivateMessage::getId, upToMessageId)
               .eq(PrivateMessage::getIsRead, 0)
               .set(PrivateMessage::getIsRead, 1);
        
        int updatedCount = privateMessageMapper.update(null, wrapper);
        log.info("用户{}批量标记与好友{}的消息为已读，直到消息ID={}，共更新{}条", 
                 userId, friendUserId, upToMessageId, updatedCount);
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
            log.info("用户{}已标记群聊{}的所有消息为已读，最后已读消息ID={}", userId, groupId, latestMsg.getId());
        } else {
            log.info("用户{}标记群聊{}为已读，但该群暂无消息", userId, groupId);
        }
    }

    /**
     * 推送离线消息总数
     * 
     * @param userId 接收者ID
     * @param session WebSocket会话
     */
    private void pushOfflineMessageCount(Long userId, WebSocketSession session) {
        try {
            // 查询未读且未被删除的离线消息总数
            Long totalCount = privateMessageMapper.selectCount(
                new LambdaQueryWrapper<PrivateMessage>()
                    .eq(PrivateMessage::getReceiverId, userId)
                    .eq(PrivateMessage::getIsRead, 0)
                    .eq(PrivateMessage::getDeletedByReceiver, 0)
            );
            
            // 构建响应数据
            Map<String, Object> response = Map.of(
                "type", OFFLINE_MESSAGE_COUNT.getValue(),
                "totalCount", totalCount
            );
            
            // 发送统计信息
            sendMessage(session, response);
            
            log.info("已向用户{}推送离线消息总数：{}条", userId, totalCount);
            
        } catch (Exception e) {
            log.error("推送离线消息统计失败：userId={}", userId, e);
        }
    }
    /**
     * 处理笔记编辑请求
     */
    private void handleNoteEditRequest(Long userId, Long noteId, WebSocketSession session) {
        log.info("用户{}请求获取笔记{}的编辑锁", userId, noteId);
        
        // 1. 查询笔记信息
        Note note = noteMapper.selectById(noteId);
        if (note == null) {
            log.warn("笔记{}不存在，拒绝编辑请求", noteId);
            sendMessage(session, Map.of(
                "type", EDIT_LOCK_DENIED.getValue(),
                "noteId", noteId,
                "success", false,
                "message", "笔记不存在"
            ));
            return;
        }
        
        // 2. PDF笔记不支持编辑
        if (note.getNoteType() == NoteType.PDF) {
            log.warn("笔记{}是PDF类型，不支持编辑", noteId);
            sendMessage(session, Map.of(
                "type", EDIT_LOCK_DENIED.getValue(),
                "noteId", noteId,
                "success", false,
                "message", "PDF笔记仅支持查看，不支持编辑"
            ));
            return;
        }
        
        // 3. 检查权限：只有作者或有编辑权限的人才能获取编辑锁
        if (!hasEditPermission(userId, note)) {
            log.warn("用户{}没有权限编辑笔记{}", userId, noteId);
            sendMessage(session, Map.of(
                "type", EDIT_LOCK_DENIED.getValue(),
                "noteId", noteId,
                "success", false,
                "message", "没有权限编辑该笔记"
            ));
            return;
        }
        
        // 4. 检查该笔记是否已被其他人锁定
        EditLockInfo lockInfo = NOTE_EDITORS.get(noteId);
        
        // 如果锁已过期，自动释放
        if (lockInfo != null && lockInfo.isExpired()) {
            log.info("笔记{}的编辑锁已过期，自动释放", noteId);
            NOTE_EDITORS.remove(noteId);
            notifyLockReleasedWithoutRemove(noteId);
            lockInfo = null;
        }
        
        log.info("笔记{}的当前编辑者：{}", noteId, lockInfo != null ? lockInfo.getUserId() : "无");
        
        if (lockInfo == null) {
            // 无人编辑，授予编辑锁
            NOTE_EDITORS.put(noteId, new EditLockInfo(userId));
            
            // ✅ 同步更新数据库
            Note updateNote = new Note();
            updateNote.setId(noteId);
            updateNote.setEditingUserId(userId);
            updateNote.setEditingLockTime(java.time.LocalDateTime.now());
            noteMapper.updateById(updateNote);
            
            log.info("✅ 用户{}成功获得笔记{}的编辑锁", userId, noteId);
            sendMessage(session, Map.of(
                "type", EDIT_LOCK_GRANTED.getValue(),
                "noteId", noteId,
                "success", true,
                "message", "已获得编辑权限"
            ));
        } else if (lockInfo.getUserId().equals(userId)) {
            // ✅ 已经是当前用户在编辑，允许重新获取锁（刷新活动时间）
            lockInfo.updateActivityTime();
            
            // ✅ 同步刷新数据库中的锁时间
            Note updateNote = new Note();
            updateNote.setId(noteId);
            updateNote.setEditingLockTime(java.time.LocalDateTime.now());
            noteMapper.updateById(updateNote);
            
            log.info("✅ 用户{}重新获取笔记{}的编辑锁（刷新活动时间）", userId, noteId);
            sendMessage(session, Map.of(
                "type", EDIT_LOCK_GRANTED.getValue(),
                "noteId", noteId,
                "success", true,
                "message", "已持有编辑权限"
            ));
        } else {
            // 被其他人锁定
            log.warn("❌ 用户{}请求编辑笔记{}被拒绝，当前编辑者为{}", userId, noteId, lockInfo.getUserId());
            sendMessage(session, Map.of(
                "type", EDIT_LOCK_DENIED.getValue(),
                "noteId", noteId,
                "success", false,
                "editorId", lockInfo.getUserId(),
                "message", "该笔记正在被其他用户编辑"
            ));
        }
    }
    
    /**
     * 检查用户是否有编辑权限
     */
    private boolean hasEditPermission(Long userId, Note note) {
        // 作者是笔记所有者
        if (note.getUserId().equals(userId)) {
            return true;
        }
        
        // 检查是否有编辑权限（FRIENDS_EDIT）
        if (note.getVisibility() == NoteVisibility.FRIENDS_EDIT) {
            LambdaQueryWrapper<NoteFriendPermission> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(NoteFriendPermission::getNoteId, note.getId())
                   .eq(NoteFriendPermission::getFriendUserId, userId)
                   .eq(NoteFriendPermission::getCanEdit, 1);
            return noteFriendPermissionMapper.selectCount(wrapper) > 0;
        }
        
        return false;
    }
    
    /**
     * 处理笔记编辑锁释放
     */
    private void handleNoteEditRelease(Long userId, Long noteId) {
        EditLockInfo lockInfo = NOTE_EDITORS.get(noteId);
        
        if (lockInfo != null && lockInfo.getUserId().equals(userId)) {
            NOTE_EDITORS.remove(noteId);
            
            // ✅ 同步清除数据库中的编辑锁
            Note updateNote = new Note();
            updateNote.setId(noteId);
            updateNote.setEditingUserId(null);
            updateNote.setEditingLockTime(null);
            noteMapper.updateById(updateNote);
            
            log.info("用户{}释放笔记{}的编辑锁", userId, noteId);
            
            // 通知观察者
            notifyLockReleasedWithoutRemove(noteId);
        }
    }
    
    /**
     * 处理笔记内容更新广播
     */
    private void handleNoteContentUpdate(Long userId, Long noteId, String content, Integer version) {
        // 验证是否是当前编辑者
        EditLockInfo lockInfo = NOTE_EDITORS.get(noteId);
        if (lockInfo == null || !lockInfo.getUserId().equals(userId)) {
            log.warn("用户{}尝试更新笔记{}但未持有编辑锁", userId, noteId);
            return;
        }
        
        // ✅ 刷新编辑锁的活动时间
        lockInfo.updateActivityTime();
        
        // 广播给所有查看该笔记的其他用户（除了编辑者自己）
        broadcastNoteUpdate(noteId, userId, content, version);
    }
    
    /**
     * 广播笔记内容更新给观察者
     */
    public void broadcastNoteUpdate(Long noteId, Long editorId, String content, Integer version) {
        Map<String, Object> updateMessage = Map.of(
            "type", NOTE_CONTENT_UPDATED.getValue(),
            "noteId", noteId,
            "editorId", editorId,
            "content", content != null ? content : "",
            "version", version != null ? version : 0,
            "timestamp", System.currentTimeMillis()
        );
        
        // 只发送给正在查看该笔记的观察者（除了编辑者自己）
        Set<Long> viewers = NOTE_VIEWERS.get(noteId);
        if (viewers != null && !viewers.isEmpty()) {
            viewers.forEach(viewerId -> {
                if (!viewerId.equals(editorId)) {
                    WebSocketSession session = ONLINE_USERS.get(viewerId);
                    if (session != null && session.isOpen()) {
                        sendMessage(session, updateMessage);
                    }
                }
            });
            log.debug("笔记{}的内容更新已广播给{}个观察者", noteId, viewers.size() - 1);
        } else {
            log.debug("笔记{}没有观察者，跳过广播", noteId);
        }
    }
    
    /**
     * 通知观察者编辑锁已被释放（包含移除操作）
     */
    public void notifyLockReleased(Long noteId) {
        NOTE_EDITORS.remove(noteId);
        notifyLockReleasedWithoutRemove(noteId);
    }
    
    /**
     * 通知观察者编辑锁已被释放（不包含移除操作，由调用者负责）
     */
    private void notifyLockReleasedWithoutRemove(Long noteId) {
        Set<Long> viewers = NOTE_VIEWERS.get(noteId);
        if (viewers == null || viewers.isEmpty()) {
            log.debug("笔记{}没有观察者，无需通知", noteId);
            return;
        }
        
        Map<String, Object> releaseMessage = Map.of(
            "type", EDIT_LOCK_RELEASED.getValue(),
            "noteId", noteId,
            "message", "编辑者已离开，现在可以编辑"
        );
        
        viewers.forEach(viewerId -> {
            WebSocketSession session = ONLINE_USERS.get(viewerId);
            if (session != null && session.isOpen()) {
                sendMessage(session, releaseMessage);
            }
        });
        
        log.info("笔记{}的编辑锁已释放，已通知{}个观察者", noteId, viewers.size());
    }
    
    /**
     * 添加用户到笔记观察者列表
     */
    public void addUserToNoteViewers(Long userId, Long noteId) {
        NOTE_VIEWERS.computeIfAbsent(noteId, k -> ConcurrentHashMap.newKeySet())
                    .add(userId);
        log.debug("用户{}开始查看笔记{}，当前观察者数：{}", 
                  userId, noteId, NOTE_VIEWERS.get(noteId).size());
    }
    
    /**
     * 从笔记观察者列表中移除用户
     */
    public void removeUserFromNoteViewers(Long userId, Long noteId) {
        Set<Long> viewers = NOTE_VIEWERS.get(noteId);
        if (viewers != null) {
            viewers.remove(userId);
            if (viewers.isEmpty()) {
                NOTE_VIEWERS.remove(noteId);
            }
            log.debug("用户{}停止查看笔记{}", userId, noteId);
        }
    }

    /**
     * 通知好友在线状态变更
     * @param userId 当前用户ID
     * @param isOnline true-上线，false-下线
     */
    private void notifyFriendsOnlineStatus(Long userId, boolean isOnline) {
        // 查询当前用户的所有好友
        List<Friend> friends = friendMapper.selectList(
                new LambdaQueryWrapper<Friend>()
                        .eq(Friend::getUserId, userId)
                        .eq(Friend::getStatus, FriendStatus.ACCEPTED)
        );
        
        if (friends == null || friends.isEmpty()) {
            return;
        }
        
        // 构建通知消息
        String messageType = isOnline ? FRIEND_ONLINE.getValue() : FRIEND_OFFLINE.getValue();
        Map<String, Object> notification = Map.of(
                "type", messageType,
                "friendUserId", userId
        );
        
        // 向所有在线的好友发送通知
        for (Friend friend : friends) {
            Long friendUserId = friend.getFriendUserId();
            WebSocketSession friendSession = ONLINE_USERS.get(friendUserId);
            
            if (friendSession != null && friendSession.isOpen()) {
                sendMessage(friendSession, notification);
                log.debug("已向好友{}发送{}通知", friendUserId, isOnline ? "上线" : "下线");
            }
        }
        
        log.info("用户{}{}，已通知{}个在线好友", userId, isOnline ? "上线" : "下线", friends.size());
    }

}
