package com.gdut.domain.vo.note;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "文件夹响应对象")
public class FolderVO {
    
    @Schema(description = "文件夹ID")
    private Long id;
    
    @Schema(description = "用户ID")
    private Long userId;
    
    @Schema(description = "文件夹名称")
    private String name;
    
    @Schema(description = "父文件夹ID")
    private Long parentId;
    
    @Schema(description = "排序顺序")
    private Integer sortOrder;
    
    @Schema(description = "子文件夹数量")
    private Integer childFolderCount;
    
    @Schema(description = "笔记数量")
    private Integer noteCount;
    
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
    
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
