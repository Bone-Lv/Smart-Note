package com.gdut.domain.dto.note;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "删除文件夹请求参数")
public class DeleteFolderDTO {
    
    @Schema(description = "是否同时删除文件夹内的所有笔记，默认false（移到根目录）", example = "false")
    private Boolean deleteNotes = false;
}
