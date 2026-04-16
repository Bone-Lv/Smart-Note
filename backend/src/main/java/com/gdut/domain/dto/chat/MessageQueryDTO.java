package com.gdut.domain.dto.chat;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "查询消息历史请求DTO")
public class MessageQueryDTO {
    
    @Schema(description = "对方用户ID（私聊）")
    private Long friendUserId;
    
    @Schema(description = "群聊ID（群聊）")
    private Long groupId;
    
    @Schema(description = "每页大小", example = "20")
    private Integer pageSize = 20;
    
    @Schema(description = "游标值（首次请求不传，后续请求传入上一次返回的current值）", example = "1234567890")
    private Long cursor;
}
