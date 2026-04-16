package com.gdut.domain.vo.note;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "笔记批注响应对象")
public class AnnotationVO {
    
    @Schema(description = "批注ID")
    private Long id;
    
    @Schema(description = "笔记ID")
    private Long noteId;
    
    @Schema(description = "批注用户ID")
    private Long userId;
    
    @Schema(description = "批注用户名")
    private String username;
    
    @Schema(description = "批注内容")
    private String content;
    
    @Schema(description = "被批注的目标内容")
    private String targetContent;
    
    @Schema(description = "目标内容在全文中的起始位置")
    private Integer startPosition;
    
    @Schema(description = "目标内容在全文中的结束位置")
    private Integer endPosition;
    
    @Schema(description = "批注创建时的笔记版本号")
    private Integer noteVersion;
    
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
    
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
