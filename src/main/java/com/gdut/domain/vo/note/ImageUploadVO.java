package com.gdut.domain.vo.note;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "图片上传响应对象")
public class ImageUploadVO {
    
    @Schema(description = "图片公网访问URL")
    private String imageUrl;
    
    @Schema(description = "Markdown格式的图片链接")
    private String markdownUrl;
}
