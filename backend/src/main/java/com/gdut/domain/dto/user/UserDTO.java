package com.gdut.domain.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

@Data
@Schema(description = "更新用户信息DTO")
public class UserDTO {
    
    @Length(min = 3, max = 20, message = "用户名长度3-20位")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "用户名只能包含字母、数字和下划线")
    @Schema(description = "用户名", example = "zhangsan",requiredMode = Schema.RequiredMode.REQUIRED)
    private String username;
    
    @Schema(description = "头像URL", example = "https://example.com/avatar.jpg",requiredMode = Schema.RequiredMode.REQUIRED)
    private String avatar;
    
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    @Schema(description = "手机号", example = "13800138000",requiredMode = Schema.RequiredMode.REQUIRED)
    private String phone;
    
    @Length(max = 200, message = "座右铭长度不能超过200位")
    @Schema(description = "座右铭", example = "天道酬勤")
    private String motto;
}
