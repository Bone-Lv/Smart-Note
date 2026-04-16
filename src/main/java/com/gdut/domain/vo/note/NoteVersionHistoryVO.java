package com.gdut.domain.vo.note;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "笔记版本历史响应对象")
public class NoteVersionHistoryVO {

    @Schema(description = "版本记录ID")
    private Long id;

    @Schema(description = "笔记ID")
    private Long noteId;

    @Schema(description = "操作用户ID")
    private Long userId;

    @Schema(description = "版本号")
    private Integer version;

    @Schema(description = "笔记标题")
    private String title;

    @Schema(description = "笔记内容（仅展示摘要，完整内容按需加载）")
    private String content;

    @Schema(description = "标签")
    private String tags;

    @Schema(description = "内容长度")
    private Integer contentLength;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
