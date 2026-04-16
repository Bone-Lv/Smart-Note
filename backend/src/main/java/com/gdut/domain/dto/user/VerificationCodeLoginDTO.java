package com.gdut.domain.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "验证码登录请求DTO")
public class VerificationCodeLoginDTO {
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    @Schema(description = "邮箱地址", requiredMode = Schema.RequiredMode.REQUIRED, example = "zhangsan@example.com")
    private String email;

    @NotBlank(message = "验证码不能为空")
    @Schema(description = "6位数字验证码", requiredMode = Schema.RequiredMode.REQUIRED, example = "123456")
    private String verificationCode;
}
