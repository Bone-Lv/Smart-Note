package com.gdut.domain.dto.note;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "AI智能分析请求DTO")
public class AiAnalysisDTO {
    @NotNull(message = "笔记ID不能为空")
    @Schema(description = "笔记ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long noteId;
    
    @Schema(description = "是否强制重新分析（覆盖旧结果）", example = "false")
    private Boolean forceRefresh;
}
