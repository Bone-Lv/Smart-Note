package com.gdut.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum MessageType {
    TEXT(1, "文本"),
    IMAGE(2, "图片"),
    FILE(3, "文件");
    
    @EnumValue
    @JsonValue
    private final Integer code;
    
    private final String description;
}
