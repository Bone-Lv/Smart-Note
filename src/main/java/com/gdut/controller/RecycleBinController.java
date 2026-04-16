package com.gdut.controller;

import com.gdut.annotation.RequireRole;
import com.gdut.domain.entity.common.Result;
import com.gdut.domain.vo.note.RecycleBinFolderVO;
import com.gdut.domain.vo.note.RecycleBinNoteVO;
import com.gdut.service.RecycleBinService;
import com.gdut.common.util.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/recycle-bin")
@RequiredArgsConstructor
@Tag(name = "回收站接口", description = "回收站的查询、还原、彻底删除等功能")
public class RecycleBinController {
    
    private final RecycleBinService recycleBinService;
    
    @GetMapping("/notes")
    @RequireRole
    @Operation(summary = "获取回收站笔记列表", description = "查看已进入回收站但还未过期的笔记")
    public Result<List<RecycleBinNoteVO>> getRecycleBinNotes() {
        return Result.success(recycleBinService.getRecycleBinNotes(UserContext.getUserId()));
    }
    
    @GetMapping("/folders")
    @RequireRole
    @Operation(summary = "获取回收站文件夹列表", description = "查看已进入回收站但还未过期的文件夹")
    public Result<List<RecycleBinFolderVO>> getRecycleBinFolders() {
        return Result.success(recycleBinService.getRecycleBinFolders(UserContext.getUserId()));
    }
    
    @PostMapping("/notes/{noteId}/restore")
    @RequireRole
    @Operation(summary = "还原笔记", description = "将笔记从回收站还原")
    public Result<Void> restoreNote(@PathVariable Long noteId) {
        recycleBinService.restoreNote(UserContext.getUserId(), noteId);
        return Result.success(null);
    }
    
    @PostMapping("/folders/{folderId}/restore")
    @RequireRole
    @Operation(summary = "还原文件夹", description = "将文件夹从回收站还原（包括文件夹下的笔记）")
    public Result<Void> restoreFolder(@PathVariable Long folderId) {
        recycleBinService.restoreFolder(UserContext.getUserId(), folderId);
        return Result.success(null);
    }
    
    @DeleteMapping("/notes/{noteId}")
    @RequireRole
    @Operation(summary = "彻底删除笔记", description = "从回收站永久删除笔记（不可恢复）")
    public Result<Void> permanentlyDeleteNote(@PathVariable Long noteId) {
        recycleBinService.permanentlyDeleteNote(UserContext.getUserId(), noteId);
        return Result.success(null);
    }
    
    @DeleteMapping("/folders/{folderId}")
    @RequireRole
    @Operation(summary = "彻底删除文件夹", description = "从回收站永久删除文件夹及其下的笔记（不可恢复）")
    public Result<Void> permanentlyDeleteFolder(@PathVariable Long folderId) {
        recycleBinService.permanentlyDeleteFolder(UserContext.getUserId(), folderId);
        return Result.success(null);
    }
    
    @DeleteMapping("/empty")
    @RequireRole
    @Operation(summary = "清空回收站", description = "清空当前用户的回收站（彻底删除所有内容）")
    public Result<Void> emptyRecycleBin() {
        recycleBinService.emptyRecycleBin(UserContext.getUserId());
        return Result.success(null);
    }
}
