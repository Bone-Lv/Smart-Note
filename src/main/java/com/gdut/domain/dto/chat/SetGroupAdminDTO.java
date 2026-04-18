package com.gdut.domain.dto.chat;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "设置群管理员DTO")
public class SetGroupAdminDTO {
    
    @Schema(description = "目标用户ID", example = "123456789")
    @NotNull(message = "用户ID不能为空")
    private Long userId;
    
    @Schema(description = "是否设为管理员：true-设为管理员，false-取消管理员", example = "true")
    @NotNull(message = "操作类型不能为空")
    private Boolean isAdmin;
}
