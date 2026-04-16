package com.gdut.domain.dto.note;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
@Schema(description = "图片上传请求参数")
public class ImageUploadDTO {
    
    @Schema(description = "图片文件", requiredMode = Schema.RequiredMode.REQUIRED)
    private MultipartFile file;
}
