package com.gdut.domain.vo.note;

import com.gdut.common.enums.NoteVisibility;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "笔记分享信息")
public class NoteShareVO {
    
    @Schema(description = "笔记ID")
    private Long noteId;
    
    @Schema(description = "分享链接")
    private String shareUrl;
    
    @Schema(description = "分享码（仅自己可见时使用）")
    private String shareCode;
    
    @Schema(description = "可见性")
    private NoteVisibility visibility;
}
