package com.gdut.domain.dto.friend;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "移动好友到分组请求DTO")
public class MoveFriendToGroupDTO {
    @NotNull(message = "好友用户ID不能为空")
    @Schema(description = "好友用户ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long friendUserId;
    
    @Schema(description = "分组ID（null表示移除分组）")
    private Long groupId;
}
