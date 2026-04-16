package com.gdut.domain.vo.note;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 回收站文件夹VO
 */
@Data
public class RecycleBinFolderVO {
    
    private Long id;
    
    private Long userId;
    
    private String name;
    
    private Long parentId;
    
    private LocalDateTime createTime;
    
    private LocalDateTime updateTime;
    
    /**
     * 进入回收站的时间
     */
    private LocalDateTime deletedTime;
    
    /**
     * 文件夹下的笔记数量
     */
    private Integer noteCount;
    
    /**
     * 剩余时间（秒），用于前端倒计时显示
     */
    private Long remainingSeconds;
}
