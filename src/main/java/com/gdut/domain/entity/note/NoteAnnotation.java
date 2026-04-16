package com.gdut.domain.entity.note;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@TableName("note_annotation")
@AllArgsConstructor
@NoArgsConstructor
public class NoteAnnotation {
    
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    
    private Long noteId;
    
    private Long userId;
    
    private String content;
    
    private String targetContent;
    
    private Integer startPosition;
    
    private Integer endPosition;
    
    private Integer noteVersion;
    
    private LocalDateTime createTime;
    
    private LocalDateTime updateTime;
}
