package com.gdut.domain.dto.chat;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
@Schema(description = "发送群聊消息请求DTO")
public class GroupMessageDTO {
    
    @NotNull(message = "群聊ID不能为空")
    @Schema(description = "群聊ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long groupId;
    
    @Schema(description = "消息类型：1-文本，2-图片，3-文件", example = "1")
    private Integer messageType = 1;
    
    @Schema(description = "文本消息内容")
    private String content;
    
    @Schema(description = "图片/文件")
    private MultipartFile file;
}
