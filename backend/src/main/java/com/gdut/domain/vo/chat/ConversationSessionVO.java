package com.gdut.domain.vo.chat;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "会话信息VO（用于消息列表）")
public class ConversationSessionVO {
    
    @Schema(description = "会话类型：private-私聊，group-群聊")
    private String conversationType;
    
    @Schema(description = "对方用户ID（私聊）")
    private Long friendUserId;
    
    @Schema(description = "对方用户名（私聊）")
    private String friendUsername;
    
    @Schema(description = "对方头像（私聊）")
    private String friendAvatar;
    
    @Schema(description = "群聊ID（群聊）")
    private Long groupId;
    
    @Schema(description = "群聊名称（群聊）")
    private String groupName;
    
    @Schema(description = "群聊头像（群聊）")
    private String groupAvatar;
    
    @Schema(description = "最后一条消息内容")
    private String lastMessage;
    
    @Schema(description = "最后消息时间")
    private java.time.LocalDateTime lastMessageTime;
    
    @Schema(description = "未读消息数量")
    private Integer unreadCount;
}
