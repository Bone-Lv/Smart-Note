package com.gdut.domain.dto.note;

import com.gdut.common.enums.NoteVisibility;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "更新笔记可见性请求参数")
public class UpdateVisibilityDTO {
    @NotNull(message = "可见性不能为空")
    @Schema(description = "可见性", requiredMode = Schema.RequiredMode.REQUIRED)
    private NoteVisibility visibility;
    
    @Schema(description = "好友用户 ID 列表")
    private List<Long> friendUserIds;
}
