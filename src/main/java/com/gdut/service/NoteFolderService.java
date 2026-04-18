package com.gdut.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.gdut.domain.entity.note.NoteFolder;
import com.gdut.domain.vo.note.FolderVO;

import java.util.List;

public interface NoteFolderService extends IService<NoteFolder> {
    
    /**
     * 创建文件夹
     * @return 新建文件夹的ID
     */
    Long createFolder(Long userId, String name, Long parentId, Integer sortOrder);
    
    /**
     * 重命名文件夹
     */
    void renameFolder(Long userId, Long folderId, String newName);
    
    /**
     * 删除文件夹
     * @param deleteNotes 是否同时删除文件夹内的所有笔记，false表示移到根目录
     */
    void deleteFolder(Long userId, Long folderId, Boolean deleteNotes);
    
    /**
     * 获取用户的文件夹树
     */
    List<FolderVO> getFolderTree(Long userId);
    
    /**
     * 获取指定父文件夹下的子文件夹列表
     */
    List<FolderVO> getChildFolders(Long userId, Long parentId);
    
    /**
     * 移动文件夹到指定父文件夹
     *
     * @param userId 用户ID
     * @param folderId 要移动的文件夹ID
     * @param newParentId 新的父文件夹ID，传null表示移到根目录
     */
    void moveFolder(Long userId, Long folderId, Long newParentId);
}
