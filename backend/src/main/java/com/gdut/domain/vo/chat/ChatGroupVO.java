package com.gdut.domain.vo.chat;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "群聊信息VO")
public class ChatGroupVO {
    
    @Schema(description = "群聊ID")
    private Long id;
    
    @Schema(description = "群聊名称")
    private String groupName;
    
    @Schema(description = "群聊头像URL")
    private String avatar;
    
    @Schema(description = "群主用户ID")
    private Long ownerId;
    
    @Schema(description = "群主用户名")
    private String ownerUsername;
    
    @Schema(description = "群成员数量")
    private Integer memberCount;
    
    @Schema(description = "我在群里的角色：0-普通成员，1-管理员，2-群主")
    private Integer myRole;
    
    @Schema(description = "最后一条消息内容")
    private String lastMessage;
    
    @Schema(description = "最后消息时间")
    private LocalDateTime lastMessageTime;
    
    @Schema(description = "未读消息数量")
    private Integer unreadCount;
    
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
