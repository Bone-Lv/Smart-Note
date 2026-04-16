package com.gdut.domain.entity.note;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@TableName("note_version_history")
@AllArgsConstructor
@NoArgsConstructor
public class NoteVersionHistory {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    
    private Long noteId;
    
    private Long userId;
    
    private Integer version;
    
    private String title;
    
    private String content;
    
    private String tags;
    
    private LocalDateTime createTime;
}
