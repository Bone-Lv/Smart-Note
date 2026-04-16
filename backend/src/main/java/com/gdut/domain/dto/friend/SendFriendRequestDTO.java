package com.gdut.domain.dto.friend;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "发送好友申请请求DTO")
public class SendFriendRequestDTO {
    @NotBlank(message = "邮箱或手机号不能为空")
    @Schema(description = "目标用户的邮箱或手机号", requiredMode = Schema.RequiredMode.REQUIRED)
    private String account;
    
    @Schema(description = "分组ID（可选，不传则使用默认分组）")
    private Long groupId;
    
    @Schema(description = "好友备注（可选）")
    private String remark;
    
    @Schema(description = "申请消息")
    private String applyMessage;
}
