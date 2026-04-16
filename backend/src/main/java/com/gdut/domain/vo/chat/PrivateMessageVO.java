package com.gdut.domain.vo.chat;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "私聊消息VO")
public class PrivateMessageVO {
    
    @Schema(description = "消息ID")
    private Long id;
    
    @Schema(description = "发送者用户ID")
    private Long senderId;
    
    @Schema(description = "发送者用户名")
    private String senderUsername;
    
    @Schema(description = "发送者头像")
    private String senderAvatar;
    
    @Schema(description = "接收者用户ID")
    private Long receiverId;
    
    @Schema(description = "消息类型：1-文本，2-图片，3-文件")
    private Integer messageType;
    
    @Schema(description = "消息内容（文本内容或文件URL）")
    private String content;
    
    @Schema(description = "是否已读：0-未读，1-已读")
    private Integer isRead;
    
    @Schema(description = "发送时间")
    private LocalDateTime createTime;
}
