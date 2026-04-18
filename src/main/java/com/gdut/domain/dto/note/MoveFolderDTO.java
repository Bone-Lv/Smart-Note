package com.gdut.domain.dto.note;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "移动文件夹请求DTO")
public class MoveFolderDTO {
    
    @Schema(description = "新的父文件夹ID，传null表示移到根目录")
    private Long parentId;
}
