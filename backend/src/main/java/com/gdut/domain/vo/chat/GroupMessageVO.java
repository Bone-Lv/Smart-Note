package com.gdut.domain.vo.chat;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "群聊消息VO")
public class GroupMessageVO {
    
    @Schema(description = "消息ID")
    private Long id;
    
    @Schema(description = "群聊ID")
    private Long groupId;
    
    @Schema(description = "发送者用户ID")
    private Long senderId;
    
    @Schema(description = "发送者用户名")
    private String senderUsername;
    
    @Schema(description = "发送者头像")
    private String senderAvatar;
    
    @Schema(description = "消息类型：1-文本，2-图片，3-文件")
    private Integer messageType;
    
    @Schema(description = "消息内容（文本内容或文件URL）")
    private String content;
    
    @Schema(description = "发送时间")
    private LocalDateTime createTime;
}
