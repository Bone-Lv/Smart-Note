package com.gdut.service;

import java.util.List;
import com.gdut.domain.vo.note.RecycleBinFolderVO;
import com.gdut.domain.vo.note.RecycleBinNoteVO;

/**
 * 回收站服务接口
 */
public interface RecycleBinService {
    
    /**
     * 获取用户的回收站笔记列表
     *
     * @param userId 用户ID
     * @return 回收站笔记列表
     */
    List<RecycleBinNoteVO> getRecycleBinNotes(Long userId);
    
    /**
     * 获取用户的回收站文件夹列表
     *
     * @param userId 用户ID
     * @return 回收站文件夹列表
     */
    List<RecycleBinFolderVO> getRecycleBinFolders(Long userId);
    
    /**
     * 还原笔记（从回收站恢复）
     *
     * @param userId 用户ID
     * @param noteId 笔记ID
     */
    void restoreNote(Long userId, Long noteId);
    
    /**
     * 还原文件夹（从回收站恢复）
     *
     * @param userId 用户ID
     * @param folderId 文件夹ID
     */
    void restoreFolder(Long userId, Long folderId);
    
    /**
     * 彻底删除笔记（从回收站永久删除）
     *
     * @param userId 用户ID
     * @param noteId 笔记ID
     */
    void permanentlyDeleteNote(Long userId, Long noteId);
    
    /**
     * 彻底删除文件夹（从回收站永久删除）
     *
     * @param userId 用户ID
     * @param folderId 文件夹ID
     */
    void permanentlyDeleteFolder(Long userId, Long folderId);
    
    /**
     * 清空用户的回收站
     *
     * @param userId 用户ID
     */
    void emptyRecycleBin(Long userId);
    
    /**
     * 定时任务：清理超过5分钟的回收站数据（滑动窗口算法）
     */
    void cleanupExpiredRecycleBinItems();
}
