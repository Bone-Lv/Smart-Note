package com.gdut.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum NoteType {
    MARKDOWN(0, "Markdown笔记"),
    PDF(1, "PDF笔记");

    @EnumValue
    private final int code;
    @JsonValue
    private final String description;
}
