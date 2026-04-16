package com.gdut.domain.dto.chat;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Data
@Schema(description = "发送聊天消息请求DTO")
public class ChatMessageDTO {
    
    @NotBlank(message = "消息内容不能为空")
    @Schema(description = "消息内容", requiredMode = Schema.RequiredMode.REQUIRED)
    private String content;
    
    @Schema(description = "会话ID（不传则创建新会话）")
    private String conversationId;

    @Schema(description = "多媒体文件")
    private List<MultipartFile> files;
}
