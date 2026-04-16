package com.gdut.domain.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

@Data
@Schema(description = "验证码重置密码请求DTO")
public class ResetPasswordDTO {
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    @Schema(description = "邮箱", requiredMode = Schema.RequiredMode.REQUIRED, example = "zhangsan@example.com")
    private String email;

    @NotBlank(message = "验证码不能为空")
    @Schema(description = "6位数字验证码", requiredMode = Schema.RequiredMode.REQUIRED, example = "123456")
    private String verificationCode;

    @NotBlank(message = "新密码不能为空")
    @Length(min = 8, max = 20, message = "密码长度8-20位")
    @Schema(description = "新密码", requiredMode = Schema.RequiredMode.REQUIRED, example = "12345678")
    private String newPassword;

    @NotBlank(message = "确认密码不能为空")
    @Schema(description = "确认密码", requiredMode = Schema.RequiredMode.REQUIRED, example = "12345678")
    private String confirmPassword;
}
