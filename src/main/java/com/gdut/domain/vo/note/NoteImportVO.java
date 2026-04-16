package com.gdut.domain.vo.note;

import com.gdut.common.enums.NoteType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "笔记导入响应对象")
public class NoteImportVO {

    @Schema(description = "导入的笔记ID")
    private Long noteId;

    @Schema(description = "笔记标题")
    private String title;

    @Schema(description = "笔记类型")
    private NoteType noteType;

    @Schema(description = "是否可编辑")
    private Boolean canEdit;
}
