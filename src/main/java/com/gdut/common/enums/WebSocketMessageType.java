package com.gdut.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * WebSocket 消息类型枚举
 */
@Getter
@AllArgsConstructor
public enum WebSocketMessageType {
    
    // ==================== 客户端 → 服务端 ====================
    
    /**
     * 心跳请求
     */
    PING("ping"),
    
    /**
     * 标记消息已读
     */
    MARK_READ("mark_read"),
    
    /**
     * 请求笔记编辑锁
     */
    NOTE_EDIT_REQUEST("note_edit_request"),
    
    /**
     * 释放笔记编辑锁
     */
    NOTE_EDIT_RELEASE("note_edit_release"),
    
    /**
     * 笔记内容更新
     */
    NOTE_CONTENT_UPDATE("note_content_update"),
    
    /**
     * 开始查看笔记
     */
    NOTE_VIEW_START("note_view_start"),
    
    /**
     * 停止查看笔记
     */
    NOTE_VIEW_END("note_view_end"),
    
    // ==================== 服务端 → 客户端 ====================
    
    /**
     * 连接成功响应
     */
    CONNECTED("connected"),
    
    /**
     * 心跳响应
     */
    PONG("pong"),
    
    /**
     * 离线消息总数
     */
    OFFLINE_MESSAGE_COUNT("offline_message_count"),
    
    /**
     * 编辑锁授予
     */
    EDIT_LOCK_GRANTED("edit_lock_granted"),
    
    /**
     * 编辑锁拒绝
     */
    EDIT_LOCK_DENIED("edit_lock_denied"),
    
    /**
     * 编辑锁释放通知
     */
    EDIT_LOCK_RELEASED("edit_lock_released"),
    
    /**
     * 笔记内容更新广播
     */
    NOTE_CONTENT_UPDATED("note_content_updated"),
    
    /**
     * 好友上线通知
     */
    FRIEND_ONLINE("friend_online"),
    
    /**
     * 好友下线通知
     */
    FRIEND_OFFLINE("friend_offline");
    
    /**
     * 消息类型的字符串值（用于 JSON 传输）
     */
    private final String value;
    
    /**
     * 根据字符串值查找枚举
     */
    public static WebSocketMessageType fromValue(String value) {
        for (WebSocketMessageType type : values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        return null;
    }
}
