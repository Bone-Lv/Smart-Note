package com.gdut.domain.dto.note;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "创建批注请求参数")
public class CreateAnnotationDTO {
    
    @NotBlank(message = "批注内容不能为空")
    @Schema(description = "批注内容", requiredMode = Schema.RequiredMode.REQUIRED)
    private String content;
    
    @NotBlank(message = "被批注的目标内容不能为空")
    @Schema(description = "被批注的目标内容", requiredMode = Schema.RequiredMode.REQUIRED)
    private String targetContent;
    
    @NotNull(message = "起始位置不能为空")
    @Schema(description = "目标内容在全文中的起始位置（字符索引）", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer startPosition;
    
    @NotNull(message = "结束位置不能为空")
    @Schema(description = "目标内容在全文中的结束位置（字符索引）", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer endPosition;
}
