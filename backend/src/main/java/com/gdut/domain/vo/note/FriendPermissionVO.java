package com.gdut.domain.vo.note;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;


@Data
@Schema(description = "笔记好友权限信息")
public class FriendPermissionVO {

    @Schema(description = "好友用户ID")
    private Long friendUserId;

    @Schema(description = "好友用户名")
    private String friendUsername;

    @Schema(description = "是否可编辑：false-仅查看，true-可编辑")
    private Boolean canEdit;
}
