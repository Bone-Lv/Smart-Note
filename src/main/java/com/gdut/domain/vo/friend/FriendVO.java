package com.gdut.domain.vo.friend;

import com.gdut.common.enums.FriendStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "好友VO")
public class FriendVO {
    @Schema(description = "好友关系ID")
    private Long id;
    
    @Schema(description = "好友用户ID")
    private Long friendUserId;
    
    @Schema(description = "好友用户名")
    private String friendUsername;
    
    @Schema(description = "好友头像URL")
    private String friendAvatar;
    
    @Schema(description = "好友座右铭")
    private String friendMotto;
    
    @Schema(description = "在线状态：true-在线，false-离线")
    private Boolean isOnline;
    
    @Schema(description = "备注名")
    private String remark;
    
    @Schema(description = "分组ID")
    private Long groupId;
    
    @Schema(description = "分组名称")
    private String groupName;
    
    @Schema(description = "好友状态")
    private FriendStatus status;
    
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
