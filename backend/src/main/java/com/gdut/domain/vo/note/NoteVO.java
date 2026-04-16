package com.gdut.domain.vo.note;

import com.gdut.common.enums.NoteType;
import com.gdut.common.enums.NoteVisibility;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(description = "笔记响应对象")
public class NoteVO {

    @Schema(description = "笔记ID")
    private Long id;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "所属文件夹ID")
    private Long folderId;

    @Schema(description = "笔记标题")
    private String title;

    @Schema(description = "笔记内容")
    private String content;

    @Schema(description = "标签，多个标签用逗号分隔")
    private String tags;

    @Schema(description = "笔记类型")
    private NoteType noteType;

    @Schema(description = "AI智能分析结果")
    private String aiAnalysis;

    @Schema(description = "最后查看时间")
    private LocalDateTime lastViewTime;

    @Schema(description = "可见性")
    private NoteVisibility visibility;

    @Schema(description = "好友权限列表（仅当 visibility 为 1 或 2 时有值）")
    private List<FriendPermissionVO> friendPermissions;

    @Schema(description = "当前用户是否有编辑权限")
    private Boolean canEdit;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "最后修改时间")
    private LocalDateTime updateTime;
    
    @Schema(description = "当前版本号")
    private Integer version;
}
