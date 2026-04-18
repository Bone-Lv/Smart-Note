package com.gdut.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum NoteType {
    MARKDOWN("MARKDOWN", "Markdown笔记"),
    PDF("PDF", "PDF笔记");

    @EnumValue
    @JsonValue
    private final String value;
    
    private final String description;
}
