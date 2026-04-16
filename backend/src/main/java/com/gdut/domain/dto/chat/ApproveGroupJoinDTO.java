package com.gdut.domain.dto.chat;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ApproveGroupJoinDTO {
    @NotNull(message = "群聊ID不能为空")
    private Long groupId;
    
    @NotNull(message = "申请人ID不能为空")
    private Long applicantId;
    
    // 1-同意，2-拒绝
    @NotNull(message = "审批结果不能为空")
    private Integer action;
}
