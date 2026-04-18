package com.gdut.domain.dto.chat;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "移除群成员DTO")
public class RemoveGroupMemberDTO {
    
    @Schema(description = "要移除的用户ID", example = "123456789")
    @NotNull(message = "用户ID不能为空")
    private Long userId;
}
