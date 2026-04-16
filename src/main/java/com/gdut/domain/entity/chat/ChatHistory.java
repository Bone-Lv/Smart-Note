package com.gdut.domain.entity.chat;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@TableName("chat_history")
@AllArgsConstructor
@NoArgsConstructor
public class ChatHistory {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    
    // 用户ID
    private Long userId;
    
    // 会话ID，用于区分不同对话
    private String conversationId;
    
    // 角色：user-用户，assistant-AI助手
    private String role;
    
    // 消息内容
    private String content;
    
    // 元数据（可选，存储额外信息）
    private String metadata;
    
    // 创建时间
    private LocalDateTime createTime;
}
