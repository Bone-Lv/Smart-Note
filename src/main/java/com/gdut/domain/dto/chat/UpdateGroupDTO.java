package com.gdut.domain.dto.chat;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "更新群聊信息DTO")
public class UpdateGroupDTO {
    
    @Schema(description = "新群名称", example = "新的群聊名称")
    @NotBlank(message = "群名称不能为空")
    @Size(max = 50, message = "群名称不能超过50个字符")
    private String groupName;
}
