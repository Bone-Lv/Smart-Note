package com.gdut.domain.dto.chat;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "查询聊天历史请求DTO")
public class ChatHistoryQueryDTO {
    
    @Schema(description = "会话ID（不传则查询所有会话列表）")
    private String conversationId;
    
    @Schema(description = "每页大小", example = "20")
    private Integer pageSize = 20;
    
    @Schema(description = "游标值（首次请求不传，后续请求传入上一次返回的nextCursor值）", example = "1234567890")
    private Long cursor;
}
