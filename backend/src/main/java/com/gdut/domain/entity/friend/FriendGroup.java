package com.gdut.domain.entity.friend;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@TableName("friend_group")
@AllArgsConstructor
@NoArgsConstructor
public class FriendGroup {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    
    // 用户ID
    private Long userId;
    
    // 分组名称
    private String groupName;
    
    // 排序顺序
    private Integer sortOrder;
    
    // 创建时间
    private LocalDateTime createTime;
    
    // 更新时间
    private LocalDateTime updateTime;
}
