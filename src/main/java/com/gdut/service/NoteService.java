package com.gdut.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.gdut.domain.dto.note.CreateAnnotationDTO;
import com.gdut.domain.dto.note.CreateNoteDTO;
import com.gdut.domain.dto.note.NoteQueryDTO;
import com.gdut.domain.dto.note.UpdateAnnotationDTO;
import com.gdut.domain.dto.note.UpdateNoteDTO;
import com.gdut.domain.dto.note.UpdateVisibilityDTO;
import com.gdut.domain.entity.note.Note;
import com.gdut.domain.vo.note.AnnotationVO;
import com.gdut.domain.vo.note.AiAnalysisVO;
import com.gdut.domain.vo.note.FriendPermissionVO;
import com.gdut.domain.vo.note.ImageUploadVO;
import com.gdut.domain.vo.note.NoteImportVO;
import com.gdut.domain.vo.note.NoteShareVO;
import com.gdut.domain.vo.note.NoteVO;
import com.gdut.domain.vo.note.NoteVersionHistoryVO;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface NoteService extends IService<Note> {
    
    /**
     * 创建笔记
     *
     * @param userId 用户ID
     * @param createNoteDTO 笔记信息
     * @return 新建笔记的ID
     */
    Long createNote(Long userId, CreateNoteDTO createNoteDTO);
    
    IPage<NoteVO> getNoteList(Long userId, NoteQueryDTO queryDTO);
    
    NoteVO getNoteDetail(Long userId, Long noteId);
    
    /**
     * 保存笔记为新版本
     *
     * @param userId 用户ID
     * @param noteId 笔记ID
     * @param updateNoteDTO 更新信息
     * @return 新版本号
     */
    Integer updateNote(Long userId, Long noteId, UpdateNoteDTO updateNoteDTO);
    
    void deleteNote(Long userId, Long noteId);
    
    List<NoteVO> getRecentViewedNotes(Long userId, Integer limit);
    
    /**
     * 获取最常看的3篇笔记（基于LFU算法）
     *
     * @param userId 用户ID
     * @return 最常看的笔记列表
     */
    List<NoteVO> getTop3FrequentNotes(Long userId);
    
    void updateVisibility(Long userId, Long noteId, UpdateVisibilityDTO visibilityDTO);
    
    NoteShareVO generateShareInfo(Long userId, Long noteId);
    
    NoteVO getNoteByShareCode(String shareCode);
    
    /**
     * 查看公开笔记（所有人可见）
     *
     * @param noteId 笔记ID
     * @param currentUserId 当前用户ID（可为null）
     * @return 笔记详情
     */
    NoteVO getPublicNote(Long noteId, Long currentUserId);
    
    /**
     * 查看好友分享的笔记（部分好友可见/可编辑）
     *
     * @param userId 当前用户ID
     * @param noteId 笔记ID
     * @return 笔记详情
     */
    NoteVO getSharedNote(Long userId, Long noteId);
    
    List<FriendPermissionVO> getNotePermissions(Long userId, Long noteId);

    AiAnalysisVO analyzeNote(Long userId, Long noteId, Boolean forceRefresh);
    
    /**
     * 导入Markdown笔记
     *
     * @param userId 用户ID
     * @param file Markdown文件
     * @param folderId 文件夹ID
     * @return 导入的笔记信息
     */
    NoteImportVO importMarkdownNote(Long userId, MultipartFile file, Long folderId);
    
    /**
     * 导入PDF笔记
     *
     * @param userId 用户ID
     * @param file PDF文件
     * @param folderId 文件夹ID
     * @return 导入的笔记信息
     */
    NoteImportVO importPdfNote(Long userId, MultipartFile file, Long folderId);
    
    /**
     * 导出笔记为Markdown文件
     *
     * @param userId 用户ID
     * @param noteId 笔记ID
     * @param response HTTP响应
     */
    void exportAsMarkdown(Long userId, Long noteId, HttpServletResponse response);
    
    /**
     * 导出笔记为PDF文件
     *
     * @param userId 用户ID
     * @param noteId 笔记ID
     * @param response HTTP响应
     */
    void exportAsPdf(Long userId, Long noteId, HttpServletResponse response);
    
    /**
     * 上传笔记图片到OSS
     *
     * @param file 图片文件
     * @return 图片URL和Markdown格式
     */
    ImageUploadVO uploadNoteImage(MultipartFile file);
    
    /**
     * 获取笔记版本历史列表
     *
     * @param userId 用户ID
     * @param noteId 笔记ID
     * @return 版本历史列表
     */
    List<NoteVersionHistoryVO> getVersionHistory(Long userId, Long noteId);
    
    /**
     * 获取指定版本的完整内容
     *
     * @param userId 用户ID
     * @param noteId 笔记ID
     * @param version 版本号
     * @return 指定版本的笔记内容
     */
    NoteVO getVersionDetail(Long userId, Long noteId, Integer version);
    
    /**
     * 版本回退：将指定旧版本的内容复制到新版本
     *
     * @param userId 用户ID
     * @param noteId 笔记ID
     * @param targetVersion 要回退到的目标版本号
     * @return 回退后的新版本号
     */
    Integer rollbackToVersion(Long userId, Long noteId, Integer targetVersion);
    
    /**
     * 获取笔记的批注列表
     *
     * @param userId 用户ID
     * @param noteId 笔记ID
     * @return 批注列表
     */
    List<AnnotationVO> getNoteAnnotations(Long userId, Long noteId);
    
    /**
     * 创建笔记批注
     *
     * @param userId 用户ID
     * @param noteId 笔记ID
     * @param createAnnotationDTO 批注信息
     * @return 新建批注的ID
     */
    Long createAnnotation(Long userId, Long noteId, CreateAnnotationDTO createAnnotationDTO);
    
    /**
     * 更新笔记批注
     *
     * @param userId 用户ID
     * @param annotationId 批注ID
     * @param updateAnnotationDTO 更新信息
     */
    void updateAnnotation(Long userId, Long annotationId, UpdateAnnotationDTO updateAnnotationDTO);
    
    /**
     * 删除笔记批注
     *
     * @param userId 用户ID
     * @param annotationId 批注ID
     */
    void deleteAnnotation(Long userId, Long annotationId);
    
    /**
     * 获取笔记的编辑锁
     *
     * @param userId 用户ID
     * @param noteId 笔记ID
     * @return 是否成功获取编辑锁
     */
    boolean acquireEditLock(Long userId, Long noteId);
    
    /**
     * 释放笔记的编辑锁
     *
     * @param userId 用户ID
     * @param noteId 笔记ID
     */
    void releaseEditLock(Long userId, Long noteId);
    
    /**
     * 实时同步笔记内容（防抖保存，不增加版本号）
     *
     * @param userId 用户ID
     * @param noteId 笔记ID
     * @param content 新内容
     * @param title 新标题（可选）
     * @param tags 新标签（可选）
     */
    void syncNoteContent(Long userId, Long noteId, String content, String title, String tags);
    
    /**
     * 移动笔记到指定文件夹
     *
     * @param userId 用户ID
     * @param noteId 笔记ID
     * @param folderId 目标文件夹ID，传null表示移到根目录
     */
    void moveNote(Long userId, Long noteId, Long folderId);
}
