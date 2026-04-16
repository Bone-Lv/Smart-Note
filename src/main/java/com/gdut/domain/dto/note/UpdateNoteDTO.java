package com.gdut.domain.dto.note;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "更新笔记请求参数")
public class UpdateNoteDTO {
    
    @Schema(description = "笔记标题")
    private String title;
    
    @Schema(description = "笔记内容")
    private String content;
    
    @Schema(description = "标签，多个标签用逗号分隔")
    private String tags;
    
    @Schema(description = "当前版本号，用于乐观锁控制。前端传入期望的版本号，后端校验是否匹配")
    private Integer version;
}
