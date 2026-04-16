package com.gdut.domain.dto.note;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "更新批注请求参数")
public class UpdateAnnotationDTO {
    
    @Schema(description = "批注内容")
    private String content;
    
    @Schema(description = "被批注的目标内容")
    private String targetContent;
}
