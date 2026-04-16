package com.gdut.domain.entity.chat;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@TableName("private_message")
@AllArgsConstructor
@NoArgsConstructor
public class PrivateMessage {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    
    // 发送者用户ID
    private Long senderId;
    
    // 接收者用户ID
    private Long receiverId;
    
    // 消息类型：1-文本，2-图片，3-文件
    private Integer messageType;
    
    // 消息内容（文本内容或文件URL）
    private String content;
    
    // 是否已读：0-未读，1-已读
    private Integer isRead;
    
    // 发送方是否删除：0-否，1-是
    private Integer deletedBySender;
    
    // 接收方是否删除：0-否，1-是
    private Integer deletedByReceiver;
    
    // 发送时间
    private LocalDateTime createTime;
}
