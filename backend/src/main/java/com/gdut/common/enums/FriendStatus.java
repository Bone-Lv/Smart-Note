package com.gdut.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum FriendStatus {
    PENDING(0, "待处理"),
    ACCEPTED(1, "已通过"),
    REJECTED(2, "已拒绝"),
    DELETED(3, "已删除");

    @EnumValue
    @JsonValue
    private final Integer code;
    
    private final String description;
}
