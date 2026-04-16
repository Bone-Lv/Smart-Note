package com.gdut.domain.entity.chat;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@TableName("chat_group")
@AllArgsConstructor
@NoArgsConstructor
public class ChatGroup {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    
    // 群聊名称
    private String groupName;
    
    // 群聊头像URL
    private String avatar;
    
    // 群主用户ID
    private Long ownerId;
    
    // 群成员数量
    private Integer memberCount;
    
    // 创建时间
    private LocalDateTime createTime;
    
    // 更新时间
    private LocalDateTime updateTime;
}
