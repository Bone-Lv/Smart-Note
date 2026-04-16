package com.gdut.domain.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

@Data
@Schema(description = "用户注册请求DTO")
public class RegisterDTO {
    @NotBlank(message = "用户名不能为空")
    @Length(min = 3, max = 20, message = "用户名长度3-20位")
    @Schema(description = "用户名", requiredMode = Schema.RequiredMode.REQUIRED, example = "zhangsan")
    private String username;

    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    @Schema(description = "邮箱", requiredMode = Schema.RequiredMode.REQUIRED, example = "zhangsan@example.com")
    private String email;

    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    @Schema(description = "手机号", requiredMode = Schema.RequiredMode.REQUIRED, example = "13800138000")
    private String phone;

    @NotBlank(message = "密码不能为空")
    @Length(min = 8, max = 20, message = "密码长度8-20位")
    @Schema(description = "密码", requiredMode = Schema.RequiredMode.REQUIRED, example = "12345678")
    private String password;

    @NotBlank(message = "确认密码不能为空")
    @Schema(description = "确认密码", requiredMode = Schema.RequiredMode.REQUIRED, example = "12345678")
    private String confirmPassword;
}