package com.gdut.domain.vo.note;

import com.gdut.common.enums.NoteType;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 回收站笔记VO
 */
@Data
public class RecycleBinNoteVO {
    
    private Long id;
    
    private Long userId;
    
    private Long folderId;
    
    private String title;
    
    private String content;
    
    private String tags;
    
    private NoteType noteType;
    
    private LocalDateTime createTime;
    
    private LocalDateTime updateTime;
    
    /**
     * 进入回收站的时间
     */
    private LocalDateTime deletedTime;
    
    /**
     * 剩余时间（秒），用于前端倒计时显示
     */
    private Long remainingSeconds;
}
