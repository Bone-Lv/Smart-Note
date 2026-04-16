package com.gdut.domain.dto.friend;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "处理好友申请请求DTO")
public class HandleFriendRequestDTO {
    @NotNull(message = "好友关系ID不能为空")
    @Schema(description = "好友关系ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long friendId;
    
    @NotNull(message = "是否同意不能为空")
    @Schema(description = "是否同意：true-同意，false-拒绝", requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean accept;
    
    @Schema(description = "好友备注（可选，仅同意时生效）")
    private String remark;
    
    @Schema(description = "分组ID（可选，仅同意时生效，不传则使用默认分组）")
    private Long groupId;
}
