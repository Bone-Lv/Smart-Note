package com.gdut.domain.dto.chat;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Data
@Schema(description = "创建群聊请求DTO")
public class CreateGroupDTO {
    
    @NotBlank(message = "群聊名称不能为空")
    @Size(max = 10, message = "群聊名称不能超过10个字符")
    @Schema(description = "群聊名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String groupName;
    
    @Schema(description = "群聊头像")
    private MultipartFile avatar;
    
    @Schema(description = "初始成员用户ID列表")
    private List<Long> memberIds;
}
