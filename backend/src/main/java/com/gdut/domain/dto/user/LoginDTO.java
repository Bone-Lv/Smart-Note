package com.gdut.domain.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "用户登录请求DTO")
public class LoginDTO {
    @NotBlank(message = "邮箱/手机号不能为空")
    @Schema(description = "邮箱或手机号", requiredMode = Schema.RequiredMode.REQUIRED, example = "zhangsan@example.com")
    private String account;

    @NotBlank(message = "密码不能为空")
    @Schema(description = "密码", requiredMode = Schema.RequiredMode.REQUIRED, example = "12345678")
    private String password;
}