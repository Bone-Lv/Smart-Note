package com.gdut.domain.vo.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "登录返回VO")
public class LoginVO {
    @Schema(description = "登录令牌")
    private String token;
    @Schema(description = "用户信息")
    private UserVO user;
}