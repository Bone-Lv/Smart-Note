package com.gdut.controller;

import com.gdut.annotation.RequireRole;
import com.gdut.domain.dto.note.DeleteFolderDTO;
import com.gdut.domain.dto.note.FolderDTO;
import com.gdut.domain.entity.common.Result;
import com.gdut.domain.vo.note.FolderVO;
import com.gdut.service.NoteFolderService;
import com.gdut.common.util.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/note/folder")
@RequiredArgsConstructor
@Tag(name = "笔记文件夹管理接口", description = "笔记文件夹的创建、重命名、删除和查询等功能")
public class FolderController {
    
    private final NoteFolderService noteFolderService;
    
    @PostMapping
    @RequireRole
    @Operation(summary = "创建文件夹", description = "创建一个新的笔记文件夹，可选择指定父文件夹")
    public Result<Void> createFolder(@Valid @RequestBody FolderDTO folderDTO) {
        noteFolderService.createFolder(
                UserContext.getUserId(),
                folderDTO.getName(),
                folderDTO.getParentId(),
                folderDTO.getSortOrder()
        );
        return Result.success(null);
    }
    
    @PutMapping("/{folderId}")
    @RequireRole
    @Operation(summary = "重命名文件夹", description = "修改文件夹名称")
    public Result<Void> renameFolder(@PathVariable Long folderId, @Valid @RequestBody FolderDTO folderDTO) {
        noteFolderService.renameFolder(UserContext.getUserId(), folderId, folderDTO.getName());
        return Result.success(null);
    }
    
    @DeleteMapping("/{folderId}")
    @RequireRole
    @Operation(summary = "删除文件夹", description = "删除文件夹，可选择是否同时删除该文件夹内所有笔记。默认false：笔记移到根目录；true：同时删除笔记。删除后进入回收站，5分钟后自动彻底删除")
    public Result<Void> deleteFolder(@PathVariable Long folderId, @RequestBody(required = false) DeleteFolderDTO deleteFolderDTO) {
        Boolean deleteNotes = deleteFolderDTO != null && deleteFolderDTO.getDeleteNotes() != null 
                ? deleteFolderDTO.getDeleteNotes() : false;
        noteFolderService.deleteFolder(UserContext.getUserId(), folderId, deleteNotes);
        return Result.success(null);
    }
    
    @GetMapping("/tree")
    @RequireRole
    @Operation(summary = "获取文件夹树", description = "获取当前用户的所有文件夹")
    public Result<List<FolderVO>> getFolderTree() {
        return Result.success(noteFolderService.getFolderTree(UserContext.getUserId()));
    }
    
    @GetMapping("/children")
    @RequireRole
    @Operation(summary = "获取子文件夹列表", description = "获取指定父文件夹下的子文件夹列表，不传parentId表示获取根文件夹")
    public Result<List<FolderVO>> getChildFolders(@RequestParam(required = false) Long parentId) {
        return Result.success(noteFolderService.getChildFolders(UserContext.getUserId(), parentId));
    }
}
