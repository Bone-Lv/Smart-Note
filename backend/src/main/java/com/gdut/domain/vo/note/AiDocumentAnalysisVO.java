package com.gdut.domain.vo.note;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "AI文档识别结果VO")
public class AiDocumentAnalysisVO {
    
    @Schema(description = "识别结果/总结内容")
    private String result;
    
    @Schema(description = "文件OSS URL")
    private String fileUrl;
    
    @Schema(description = "文件类型（如 application/pdf, image/jpeg）")
    private String fileType;
    
    @Schema(description = "处理时间")
    private LocalDateTime processTime;
    
    @Schema(description = "使用的模型")
    private String model;
}
