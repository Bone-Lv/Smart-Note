package com.gdut.domain.vo.chat;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "群成员信息VO")
public class GroupMemberVO {
    
    @Schema(description = "成员ID")
    private Long id;
    
    @Schema(description = "用户ID")
    private Long userId;
    
    @Schema(description = "用户名")
    private String username;
    
    @Schema(description = "用户头像")
    private String avatar;
    
    @Schema(description = "群内角色：0-普通成员，1-管理员，2-群主")
    private Integer role;
    
    @Schema(description = "群昵称")
    private String nickname;
    
    @Schema(description = "加入时间")
    private LocalDateTime joinTime;
}
