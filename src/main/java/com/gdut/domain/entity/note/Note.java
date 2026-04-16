package com.gdut.domain.entity.note;

import com.baomidou.mybatisplus.annotation.*;
import com.gdut.common.enums.NoteType;
import com.gdut.common.enums.NoteVisibility;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@TableName("note")
@AllArgsConstructor
@NoArgsConstructor
public class Note {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    
    private Long userId;
    
    private Long folderId;
    
    private String title;
    
    private String content;
    
    private String tags;
    
    private NoteType noteType;
    
    private String aiAnalysis;
    
    private LocalDateTime lastViewTime;
    
    private NoteVisibility visibility;
    
    private String shareCode;
    
    private LocalDateTime createTime;
    
    private LocalDateTime updateTime;
    
    @TableLogic
    private Integer deleted;
    
    /**
     * 进入回收站的时间
     */
    private LocalDateTime deletedTime;

    @Version
    private Integer version;
    
    /**
     * 当前正在编辑的用户ID（NULL表示无人编辑）
     */
    private Long editingUserId;
    
    /**
     * 编辑锁获取时间
     */
    private LocalDateTime editingLockTime;
}
