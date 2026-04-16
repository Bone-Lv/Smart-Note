package com.gdut.domain.vo.friend;

import com.gdut.common.enums.FriendStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "好友申请VO")
public class FriendRequestVO {
    @Schema(description = "好友关系ID")
    private Long id;
    
    @Schema(description = "申请人用户ID")
    private Long userId;
    
    @Schema(description = "申请人用户名")
    private String applicantUsername;
    
    @Schema(description = "申请人头像URL")
    private String applicantAvatar;
    
    @Schema(description = "被申请人用户ID")
    private Long friendUserId;
    
    @Schema(description = "申请消息")
    private String applyMessage;
    
    @Schema(description = "申请状态：0-待处理，1-已通过，2-已拒绝")
    private FriendStatus status;
    
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
