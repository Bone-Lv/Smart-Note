package com.gdut.domain.dto.note;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "移动笔记请求DTO")
public class MoveNoteDTO {
    
    @Schema(description = "目标文件夹ID，传null表示移到根目录")
    private Long folderId;
}
