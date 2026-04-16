package com.gdut.domain.entity.note;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@TableName("note_friend_permission")
@AllArgsConstructor
@NoArgsConstructor
public class NoteFriendPermission {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    
    private Long noteId;
    
    private Long friendUserId;
    
    private Integer canEdit;
    
    private LocalDateTime createTime;
}
