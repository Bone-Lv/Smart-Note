package com.gdut.domain.dto.friend;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "更新好友备注请求DTO")
public class UpdateFriendRemarkDTO {
    @NotNull(message = "好友用户ID不能为空")
    @Schema(description = "好友用户ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long friendUserId;
    
    @Size(max = 100, message = "备注名不能超过100个字符")
    @Schema(description = "备注名")
    private String remark;
}
