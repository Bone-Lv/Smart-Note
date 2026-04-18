package com.gdut.domain.dto.chat;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "转让群主DTO")
public class TransferGroupOwnerDTO {
    
    @Schema(description = "新群主用户ID", example = "123456789")
    @NotNull(message = "新群主ID不能为空")
    private Long newOwnerId;
}
