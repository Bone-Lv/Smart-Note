package com.gdut.domain.entity.chat;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@TableName("group_message")
@AllArgsConstructor
@NoArgsConstructor
public class GroupMessage {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    
    // 群聊ID
    private Long groupId;
    
    // 发送者用户ID
    private Long senderId;
    
    // 消息类型：1-文本，2-图片，3-文件
    private Integer messageType;
    
    // 消息内容（文本内容或文件URL）
    private String content;
    
    // 发送时间
    private LocalDateTime createTime;
}
