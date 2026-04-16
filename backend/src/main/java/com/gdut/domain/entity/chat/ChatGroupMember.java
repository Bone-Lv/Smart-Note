package com.gdut.domain.entity.chat;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@TableName("chat_group_member")
@AllArgsConstructor
@NoArgsConstructor
public class ChatGroupMember {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    
    // 群聊ID
    private Long groupId;
    
    // 用户ID
    private Long userId;
    
    // 角色：0-普通成员，1-管理员，2-群主
    private Integer role;
    
    // 群昵称
    private String nickname;
    
    // 加入时间
    private LocalDateTime joinTime;
    
    // 是否被移除：0-否，1-是
    private Integer isRemoved;
    
    // 最后已读的群消息ID
    private Long lastReadMsgId;
    
    // 最后清空的群消息ID（用于隐藏历史记录）
    private Long lastClearedMsgId;
    
    // 成员状态：0-待审核，1-已通过，2-已拒绝
    private Integer status;
}
