package com.gdut.domain.vo.note;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "AI智能分析结果VO")
public class AiAnalysisVO {
    @Schema(description = "摘要")
    private String summary;
    
    @Schema(description = "要点列表")
    private List<String> keyPoints;
    
    @Schema(description = "标签建议列表")
    private List<String> tags;
}
