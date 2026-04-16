package com.gdut.domain.dto.note;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "创建笔记请求参数")
public class CreateNoteDTO {

    @NotBlank(message = "标题不能为空")
    @Schema(description = "笔记标题", requiredMode = Schema.RequiredMode.REQUIRED)
    private String title;

    @NotBlank(message = "内容不能为空")
    @Schema(description = "笔记内容", requiredMode = Schema.RequiredMode.REQUIRED)
    private String content;

    @Schema(description = "标签，多个标签用逗号分隔")
    private String tags;
    
    @Schema(description = "所属文件夹ID，不传或传null表示放到根目录")
    private Long folderId;
}