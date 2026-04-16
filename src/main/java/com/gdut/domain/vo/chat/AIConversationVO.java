package com.gdut.domain.vo.chat;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "AI对话会话VO")
public class AIConversationVO {
    
    @Schema(description = "会话ID")
    private String conversationId;
    
    @Schema(description = "最后一条消息内容")
    private String lastMessage;
    
    @Schema(description = "最后消息时间")
    private LocalDateTime lastMessageTime;
    
    @Schema(description = "消息总数")
    private Integer messageCount;
}
