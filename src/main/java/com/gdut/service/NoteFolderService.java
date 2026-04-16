package com.gdut.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.gdut.domain.entity.note.NoteFolder;
import com.gdut.domain.vo.note.FolderVO;

import java.util.List;

public interface NoteFolderService extends IService<NoteFolder> {
    
    /**
     * 创建文件夹
     */
    void createFolder(Long userId, String name, Long parentId, Integer sortOrder);
    
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
}
