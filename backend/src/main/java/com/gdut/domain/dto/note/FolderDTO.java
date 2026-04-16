package com.gdut.domain.dto.note;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "创建/更新文件夹请求参数")
public class FolderDTO {
    
    @NotBlank(message = "文件夹名称不能为空")
    @Schema(description = "文件夹名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;
    
    @Schema(description = "父文件夹ID，不传或传null表示根文件夹")
    private Long parentId;
    
    @Schema(description = "排序顺序")
    private Integer sortOrder;
}
