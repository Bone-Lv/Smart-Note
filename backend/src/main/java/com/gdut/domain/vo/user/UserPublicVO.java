package com.gdut.domain.vo.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "用户公开信息VO（对外展示，不包含隐私信息）")
public class UserPublicVO {

    @Schema(description = "用户ID")
    private Long id;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "头像URL")
    private String avatar;

    @Schema(description = "座右铭")
    private String motto;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
