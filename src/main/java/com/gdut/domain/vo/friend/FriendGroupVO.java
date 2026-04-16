package com.gdut.domain.vo.friend;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "好友分组VO")
public class FriendGroupVO {
    @Schema(description = "分组ID")
    private Long id;
    
    @Schema(description = "分组名称")
    private String groupName;
    
    @Schema(description = "排序顺序")
    private Integer sortOrder;
    
    @Schema(description = "好友数量")
    private Integer friendCount;
    
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
