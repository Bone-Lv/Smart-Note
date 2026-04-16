package com.gdut.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum NoteVisibility {
    PRIVATE(0, "仅自己可见"),
    FRIENDS_VIEW(1, "部分好友可见"),
    FRIENDS_EDIT(2, "部分好友可编辑"),
    PUBLIC(3, "所有人可见");

    @EnumValue
    @JsonValue
    private final Integer code;
    
    private final String description;
}
