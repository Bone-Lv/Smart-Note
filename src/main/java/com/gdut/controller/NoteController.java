package com.gdut.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.gdut.annotation.RequireRole;
import com.gdut.domain.dto.note.*;
import com.gdut.domain.entity.common.Result;
import com.gdut.domain.vo.note.AnnotationVO;
import com.gdut.domain.vo.note.AiAnalysisVO;
import com.gdut.domain.vo.note.FriendPermissionVO;
import com.gdut.domain.vo.note.ImageUploadVO;
import com.gdut.domain.vo.note.NoteImportVO;
import com.gdut.domain.vo.note.NoteShareVO;
import com.gdut.domain.vo.note.NoteVO;
import com.gdut.domain.vo.note.NoteVersionHistoryVO;
import com.gdut.service.NoteService;
import com.gdut.common.util.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/note")
@RequiredArgsConstructor
@Validated
@Tag(name = "笔记管理接口", description = "笔记的增删改查、分享、权限管理等功能")
public class NoteController {

    private final NoteService noteService;

    @PostMapping
    @RequireRole
    @Operation(summary = "新增笔记", description = "用户可新增一条笔记，包含标题、笔记内容、标签，默认仅自己可见，返回新建笔记的ID")
    public Result<Long> createNote(@Valid @RequestBody CreateNoteDTO createNoteDTO) {
        Long noteId = noteService.createNote(UserContext.getUserId(), createNoteDTO);
        return Result.success(noteId);
    }

    @GetMapping("/list")
    @RequireRole
    @Operation(summary = "我的笔记列表", description = "分页展示当前用户的所有笔记，支持按时间倒序、按标题或标签查询")
    public Result<IPage<NoteVO>> getNoteList(@Valid NoteQueryDTO queryDTO) {
        return Result.success(noteService.getNoteList(UserContext.getUserId(), queryDTO));
    }

    @GetMapping("/{noteId}")
    @RequireRole
    @Operation(summary = "笔记详情", description = "查看单条笔记的完整内容及AI智能分析结果（若有），同时记录查看时间")
    public Result<NoteVO> getNoteDetail(@NotNull @PathVariable Long noteId) {
        return Result.success(noteService.getNoteDetail(UserContext.getUserId(), noteId));
    }

    @PutMapping("/{noteId}")
    @RequireRole
    @Operation(summary = "保存笔记为新版本", description = "用户主动保存，创建新版本并归档历史，返回新版本号")
    public Result<Integer> updateNote(@NotNull @PathVariable Long noteId, @Valid @RequestBody UpdateNoteDTO updateNoteDTO) {
        Integer newVersion = noteService.updateNote(UserContext.getUserId(), noteId, updateNoteDTO);
        return Result.success(newVersion);
    }

    @DeleteMapping("/{noteId}")
    @RequireRole
    @Operation(summary = "删除笔记", description = "采用逻辑删除，笔记将移入回收站，5分钟后自动彻底删除")
    public Result<Void> deleteNote(@NotNull @PathVariable Long noteId) {
        noteService.deleteNote(UserContext.getUserId(), noteId);
        return Result.success(null);
    }

    @GetMapping("/recent")
    @RequireRole
    @Operation(summary = "最近查看的笔记", description = "查看自己最近看过的笔记，默认返回10条")
    public Result<List<NoteVO>> getRecentViewedNotes(@NotNull @RequestParam(defaultValue = "10") Integer limit) {
        return Result.success(noteService.getRecentViewedNotes(UserContext.getUserId(), limit));
    }

    @GetMapping("/top3-frequent")
    @RequireRole
    @Operation(summary = "最常看的3篇笔记", description = "基于LFU算法统计用户最常查看的3篇笔记")
    public Result<List<NoteVO>> getTop3FrequentNotes() {
        return Result.success(noteService.getTop3FrequentNotes(UserContext.getUserId()));
    }

    @PutMapping("/{noteId}/visibility")
    @RequireRole
    @Operation(summary = "设置笔记可见性", description = "设置笔记的分享权限：仅自己可见、部分好友可见、部分好友可编辑、所有人可见")
    public Result<Void> updateVisibility(@NotNull @PathVariable Long noteId, @Valid @RequestBody UpdateVisibilityDTO visibilityDTO) {
        noteService.updateVisibility(UserContext.getUserId(), noteId, visibilityDTO);
        return Result.success(null);
    }

    @PostMapping("/{noteId}/share")
    @RequireRole
    @Operation(summary = "生成分享信息", description = "生成笔记的分享链接或分享码")
    public Result<NoteShareVO> generateShareInfo(@NotNull @PathVariable Long noteId) {
        return Result.success(noteService.generateShareInfo(UserContext.getUserId(), noteId));
    }

    @GetMapping("/share/{shareCode}")
    @Operation(summary = "通过分享码查看笔记", description = "仅自己可见的笔记通过分享码访问，分享码作为路径参数更安全")
    public Result<NoteVO> getNoteByShareCode(@NotNull @PathVariable String shareCode) {
        return Result.success(noteService.getNoteByShareCode(shareCode));
    }

    @GetMapping("/public/{noteId}")
    @Operation(summary = "查看公开笔记", description = "查看所有人可见的公开笔记")
    public Result<NoteVO> getPublicNote(@NotNull @PathVariable Long noteId) {
        return Result.success(noteService.getPublicNote(noteId, UserContext.getUserId()));
    }

    @GetMapping("/shared/{noteId}")
    @RequireRole
    @Operation(summary = "查看好友分享的笔记", description = "查看好友可见/可编辑的笔记")
    public Result<NoteVO> getSharedNote(@NotNull @PathVariable Long noteId) {
        return Result.success(noteService.getSharedNote(UserContext.getUserId(), noteId));
    }

    @GetMapping("/{noteId}/permissions")
    @RequireRole
    @Operation(summary = "查看笔记权限列表", description = "仅限作者查看当前笔记的好友权限分配情况")
    public Result<List<FriendPermissionVO>> getNotePermissions(@NotNull @PathVariable Long noteId) {
        return Result.success(noteService.getNotePermissions(UserContext.getUserId(), noteId));
    }

    @PostMapping("/{noteId}/ai-analyze")
    @RequireRole
    @Operation(summary = "AI智能分析笔记", description = "对笔记发起智能分析请求，生成摘要、要点和标签建议。若已有分析结果可直接返回，支持重新分析")
    public Result<AiAnalysisVO> analyzeNote(@NotNull @PathVariable Long noteId, @RequestBody(required = false) AiAnalysisDTO aiAnalysisDTO) {
        Boolean forceRefresh = aiAnalysisDTO != null ? aiAnalysisDTO.getForceRefresh() : false;
        return Result.success(noteService.analyzeNote(UserContext.getUserId(), noteId, forceRefresh));
    }
    
    @PostMapping("/upload-image")
    @RequireRole
    @Operation(summary = "上传笔记图片", description = "上传笔记中的图片到OSS，返回公网可访问的图片URL")
    public Result<ImageUploadVO> uploadImage(@NotNull @RequestParam("file") MultipartFile file) {
        return Result.success(noteService.uploadNoteImage(file));
    }
    
    @PostMapping("/import/md")
    @RequireRole
    @Operation(summary = "导入Markdown笔记", description = "上传.md文件，解析内容并创建可编辑的笔记")
    public Result<NoteImportVO> importMarkdownNote(
            @NotNull @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) Long folderId) {
        try {
            NoteImportVO vo = noteService.importMarkdownNote(UserContext.getUserId(), file, folderId);
            return Result.success(vo);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    @PostMapping("/import/pdf")
    @RequireRole
    @Operation(summary = "导入PDF笔记", description = "上传.pdf文件，解析内容并创建只读笔记")
    public Result<NoteImportVO> importPdfNote(
            @NotNull @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) Long folderId) {
        try {
            NoteImportVO vo = noteService.importPdfNote(UserContext.getUserId(), file, folderId);
            return Result.success(vo);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    @GetMapping("/{noteId}/export/md")
    @RequireRole
    @Operation(summary = "导出为Markdown", description = "将笔记内容导出为.md文件下载")
    public void exportAsMarkdown(@NotNull @PathVariable Long noteId, HttpServletResponse response) {
        noteService.exportAsMarkdown(UserContext.getUserId(), noteId, response);
    }
    
    @GetMapping("/{noteId}/export/pdf")
    @RequireRole
    @Operation(summary = "导出为PDF", description = "将笔记内容导出为.pdf文件下载")
    public void exportAsPdf(@NotNull @PathVariable Long noteId, HttpServletResponse response) {
        noteService.exportAsPdf(UserContext.getUserId(), noteId, response);
    }
    
    @GetMapping("/{noteId}/versions")
    @RequireRole
    @Operation(summary = "查看版本历史", description = "查看笔记的所有版本历史列表")
    public Result<List<NoteVersionHistoryVO>> getVersionHistory(@NotNull @PathVariable Long noteId) {
        return Result.success(noteService.getVersionHistory(UserContext.getUserId(), noteId));
    }
    
    @GetMapping("/{noteId}/versions/{version}")
    @RequireRole
    @Operation(summary = "查看指定版本详情", description = "查看笔记指定版本的完整内容")
    public Result<NoteVO> getVersionDetail(@NotNull @PathVariable Long noteId, @NotNull @PathVariable Integer version) {
        return Result.success(noteService.getVersionDetail(UserContext.getUserId(), noteId, version));
    }
    
    @PostMapping("/{noteId}/versions/{version}/rollback")
    @RequireRole
    @Operation(summary = "版本回退", description = "将笔记内容回退到指定版本，会创建一个新的版本记录")
    public Result<Integer> rollbackToVersion(@NotNull @PathVariable Long noteId, @NotNull @PathVariable Integer version) {
        return Result.success(noteService.rollbackToVersion(UserContext.getUserId(), noteId, version));
    }
    
    @GetMapping("/{noteId}/annotations")
    @RequireRole
    @Operation(summary = "查看笔记批注列表", description = "查看笔记的所有批注，按时间倒序排列")
    public Result<List<AnnotationVO>> getNoteAnnotations(@NotNull @PathVariable Long noteId) {
        return Result.success(noteService.getNoteAnnotations(UserContext.getUserId(), noteId));
    }
    
    @PostMapping("/{noteId}/annotations")
    @RequireRole
    @Operation(summary = "创建批注", description = "对笔记的某行或某段内容进行批注解释说明，返回新建批注的ID")
    public Result<Long> createAnnotation(@NotNull @PathVariable Long noteId, @Valid @RequestBody CreateAnnotationDTO createAnnotationDTO) {
        Long annotationId = noteService.createAnnotation(UserContext.getUserId(), noteId, createAnnotationDTO);
        return Result.success(annotationId);
    }
    
    @PutMapping("/annotations/{annotationId}")
    @RequireRole
    @Operation(summary = "更新批注", description = "修改批注内容")
    public Result<Void> updateAnnotation(@NotNull @PathVariable Long annotationId, @Valid @RequestBody UpdateAnnotationDTO updateAnnotationDTO) {
        noteService.updateAnnotation(UserContext.getUserId(), annotationId, updateAnnotationDTO);
        return Result.success(null);
    }
    
    @DeleteMapping("/annotations/{annotationId}")
    @RequireRole
    @Operation(summary = "删除批注", description = "删除批注（笔记所有者、批注作者可删除）")
    public Result<Void> deleteAnnotation(@NotNull @PathVariable Long annotationId) {
        noteService.deleteAnnotation(UserContext.getUserId(), annotationId);
        return Result.success(null);
    }
    
    @PostMapping("/{noteId}/lock")
    @RequireRole
    @Operation(summary = "获取笔记编辑锁", description = "请求获取笔记的独占编辑权限，同一时间只能有一人编辑")
    public Result<Boolean> acquireEditLock(@NotNull @PathVariable Long noteId) {
        boolean success = noteService.acquireEditLock(UserContext.getUserId(), noteId);
        return Result.success(success);
    }
    
    @DeleteMapping("/{noteId}/lock")
    @RequireRole
    @Operation(summary = "释放笔记编辑锁", description = "释放笔记的编辑权限，允许其他人编辑")
    public Result<Void> releaseEditLock(@NotNull @PathVariable Long noteId) {
        noteService.releaseEditLock(UserContext.getUserId(), noteId);
        return Result.success(null);
    }
    
    @PutMapping("/{noteId}/sync")
    @RequireRole
    @Operation(summary = "实时同步笔记内容", description = "自动保存，不创建新版本，需持有编辑锁")
    public Result<Void> syncNoteContent(
            @NotNull @PathVariable Long noteId,
            @Valid @RequestBody UpdateNoteDTO updateNoteDTO) {
        noteService.syncNoteContent(
            UserContext.getUserId(),
            noteId,
            updateNoteDTO.getContent(),
            updateNoteDTO.getTitle(),
            updateNoteDTO.getTags()
        );
        return Result.success(null);
    }
    
    @PutMapping("/{noteId}/move")
    @RequireRole
    @Operation(summary = "移动笔记", description = "将笔记移动到指定文件夹，folderId传null表示移到根目录")
    public Result<Void> moveNote(@NotNull @PathVariable Long noteId, @RequestBody MoveNoteDTO moveNoteDTO) {
        noteService.moveNote(UserContext.getUserId(), noteId, moveNoteDTO.getFolderId());
        return Result.success(null);
    }
}
