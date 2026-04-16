package com.gdut.domain.entity.friend;

import com.baomidou.mybatisplus.annotation.*;
import com.gdut.common.enums.FriendStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@TableName("friend")
@AllArgsConstructor
@NoArgsConstructor
public class Friend {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    
    // 用户ID
    private Long userId;
    
    // 好友用户ID
    private Long friendUserId;
    
    // 分组ID
    private Long groupId;
    
    // 状态：0-待处理，1-已通过，2-已拒绝，3-已删除
    private FriendStatus status;
    
    // 备注名
    private String remark;
    
    // 申请消息
    private String applyMessage;
    
    // 创建时间
    private LocalDateTime createTime;
    
    // 更新时间
    private LocalDateTime updateTime;
}
