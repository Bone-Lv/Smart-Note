package com.gdut.domain.dto.note;

import com.gdut.common.enums.NoteVisibility;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "笔记查询参数")
public class NoteQueryDTO {
    
    @Schema(description = "页码（传统分页，与cursor二选一）", example = "1", deprecated = true)
    private Integer page = 1;
    
    @Schema(description = "每页大小", example = "20")
    private Integer pageSize = 20;
    
    @Schema(description = "游标值（游标分页，与page二选一，优先级更高）", example = "1234567890")
    private Long cursor;
    
    @Schema(description = "搜索关键词（标题或标签）")
    private String keyword;
    
    @Schema(description = "排序方式：time-按时间倒序，title-按标题排序", example = "time")
    private String sortOrder = "time";
    
    @Schema(description = "可见性筛选：0-仅自己可见，1-部分好友可见，2-部分好友可编辑，3-所有人可见")
    private NoteVisibility visibility;
    
    @Schema(description = "文件夹ID筛选，不传表示查询所有笔记，传null表示查询根目录（未归类）笔记")
    private Long folderId;
}
