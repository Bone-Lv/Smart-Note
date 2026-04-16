package com.gdut.domain.dto.note;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
@Schema(description = "AI文档识别请求DTO（支持PDF和图片）")
public class AiDocumentAnalysisDTO {
    
    @Schema(description = "文件（支持PDF、JPG、PNG、JPEG、WEBP）", requiredMode = Schema.RequiredMode.REQUIRED)
    private MultipartFile file;
    
    @NotBlank(message = "提示词不能为空")
    @Schema(description = "提示词，例如：请识别这个PDF并总结内容", requiredMode = Schema.RequiredMode.REQUIRED, example = "请识别这个PDF并总结内容")
    private String prompt;
    
    @Schema(description = "是否只提取文字（默认false会进行智能总结）", defaultValue = "false")
    private Boolean extractOnly = false;
}
