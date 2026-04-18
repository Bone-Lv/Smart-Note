package com.gdut.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.extension.toolkit.Db;
import com.gdut.domain.entity.note.Note;
import com.gdut.domain.entity.note.NoteFolder;
import com.gdut.domain.vo.note.FolderVO;
import com.gdut.mapper.NoteFolderMapper;
import com.gdut.mapper.NoteMapper;
import com.gdut.service.NoteFolderService;
import com.gdut.service.VectorStoreService;
import com.gdut.common.exception.BusinessException;
import com.gdut.common.enums.ResultCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NoteFolderServiceImpl extends ServiceImpl<NoteFolderMapper, NoteFolder> implements NoteFolderService {
    
    private final NoteMapper noteMapper;
    private final VectorStoreService vectorStoreService;
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createFolder(Long userId, String name, Long parentId, Integer sortOrder) {
        // 如果指定了父文件夹，验证父文件夹是否存在且属于当前用户
        if (parentId != null) {
            NoteFolder parentFolder = getById(parentId);
            if (parentFolder == null || !parentFolder.getUserId().equals(userId)) {
                throw new BusinessException(ResultCode.FOLDER_NOT_EXIST, "父文件夹不存在或无权限");
            }
        }
        
        // 检查同级文件夹名称是否重复
        LambdaQueryWrapper<NoteFolder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(NoteFolder::getUserId, userId)
               .eq(NoteFolder::getParentId, parentId)
               .eq(NoteFolder::getName, name);
        if (count(wrapper) > 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "同级文件夹下已存在同名文件夹");
        }
        
        NoteFolder folder = new NoteFolder();
        folder.setUserId(userId);
        folder.setName(name);
        folder.setParentId(parentId);
        folder.setSortOrder(sortOrder != null ? sortOrder : 0);
        
        save(folder);
        
        return folder.getId();
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void renameFolder(Long userId, Long folderId, String newName) {
        NoteFolder folder = getById(folderId);
        if (folder == null || !folder.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FOLDER_NOT_EXIST, "文件夹不存在或无权限");
        }
        
        if (StrUtil.isBlank(newName)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "文件夹名称不能为空");
        }
        
        // 检查同级文件夹名称是否重复
        LambdaQueryWrapper<NoteFolder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(NoteFolder::getUserId, userId)
               .eq(NoteFolder::getParentId, folder.getParentId())
               .eq(NoteFolder::getName, newName)
               .ne(NoteFolder::getId, folderId);
        if (count(wrapper) > 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "同级文件夹下已存在同名文件夹");
        }
        
        lambdaUpdate()
                .eq(NoteFolder::getId, folderId)
                .set(NoteFolder::getName, newName)
                .update();
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteFolder(Long userId, Long folderId, Boolean deleteNotes) {
        NoteFolder folder = getById(folderId);
        if (folder == null || !folder.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FOLDER_NOT_EXIST, "文件夹不存在或无权限");
        }
        
        // 检查文件夹是否已在回收站中
        if (folder.getDeleted() == 1) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "文件夹已在回收站中");
        }
        
        // 检查是否有子文件夹
        LambdaQueryWrapper<NoteFolder> childWrapper = new LambdaQueryWrapper<>();
        childWrapper.eq(NoteFolder::getParentId, folderId);
        if (count(childWrapper) > 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "文件夹下还有子文件夹，请先删除子文件夹");
        }
        
        // 根据 deleteNotes 参数决定如何处理笔记
        if (deleteNotes != null && deleteNotes) {
            // 先从向量数据库删除这些笔记
            LambdaQueryWrapper<Note> noteQueryWrapper = new LambdaQueryWrapper<>();
            noteQueryWrapper.eq(Note::getFolderId, folderId);
            List<Note> notes = noteMapper.selectList(noteQueryWrapper);
            for (Note note : notes) {
                vectorStoreService.removeNoteFromVectorStore(note.getId());
            }
            
            // 将文件夹下的所有笔记一起移入回收站
            LambdaUpdateWrapper<Note> noteWrapper = new LambdaUpdateWrapper<>();
            noteWrapper.eq(Note::getFolderId, folderId)
                    .set(Note::getDeleted, 1)
                    .set(Note::getDeletedTime, LocalDateTime.now());
            Db.update(noteWrapper);
        } else {
            // 将文件夹下的笔记移到根目录（folderId设为null）
            LambdaUpdateWrapper<Note> noteWrapper = new LambdaUpdateWrapper<>();
            noteWrapper.eq(Note::getFolderId, folderId)
                    .set(Note::getFolderId, null);
            Db.update(noteWrapper);
        }
        
        // 移入回收站：标记为已删除，记录删除时间
        lambdaUpdate()
                .eq(NoteFolder::getId, folderId)
                .set(NoteFolder::getDeleted, 1)
                .set(NoteFolder::getDeletedTime, LocalDateTime.now())
                .update();
        
        log.info("用户 {} 删除了文件夹 {}，已移入回收站", userId, folderId);
    }
    
    @Override
    public List<FolderVO> getFolderTree(Long userId) {
        // 获取用户的所有文件夹
        LambdaQueryWrapper<NoteFolder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(NoteFolder::getUserId, userId)
               .orderByAsc(NoteFolder::getParentId)
               .orderByAsc(NoteFolder::getSortOrder)
               .orderByAsc(NoteFolder::getCreateTime);
        
        List<NoteFolder> folders = list(wrapper);
        
        return folders.stream()
                .map(folder -> convertToVO(folder, userId))
                .collect(Collectors.toList());
    }
    
    @Override
    public List<FolderVO> getChildFolders(Long userId, Long parentId) {
        LambdaQueryWrapper<NoteFolder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(NoteFolder::getUserId, userId)
                .eq(NoteFolder::getParentId, parentId)
                .orderByAsc(NoteFolder::getSortOrder);
        
        List<NoteFolder> folders = list(wrapper);
        return folders.stream()
                .map(folder -> BeanUtil.copyProperties(folder, FolderVO.class))
                .toList();
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void moveFolder(Long userId, Long folderId, Long newParentId) {
        NoteFolder folder = getById(folderId);
        
        if (folder == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "文件夹不存在");
        }
        
        // 只有文件夹所有者才能移动
        if (!folder.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权操作该文件夹");
        }
        
        // 不能将文件夹移动到自己下面（防止循环引用）
        if (folderId.equals(newParentId)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "不能将文件夹移动到自己下面");
        }
        
        // 如果指定了新的父文件夹，验证父文件夹是否存在且属于该用户
        if (newParentId != null) {
            NoteFolder parentFolder = getById(newParentId);
            if (parentFolder == null) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "父文件夹不存在");
            }
            if (!parentFolder.getUserId().equals(userId)) {
                throw new BusinessException(ResultCode.FORBIDDEN, "无权访问该父文件夹");
            }
            
            // 检查是否会造成循环引用（新父文件夹不能是当前文件夹的子文件夹）
            if (isDescendant(folderId, newParentId, userId)) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "不能将文件夹移动到其子文件夹下");
            }
        }
        
        // 更新文件夹的父文件夹ID
        lambdaUpdate()
                .eq(NoteFolder::getId, folderId)
                .set(NoteFolder::getParentId, newParentId)
                .update();
        
        log.info("用户{}将文件夹{}移动到{}", userId, folderId, newParentId == null ? "根目录" : "文件夹" + newParentId);
    }
    
    /**
     * 转换为VO并统计子文件夹和笔记数量
     */
    private FolderVO convertToVO(NoteFolder folder, Long userId) {
        FolderVO vo = BeanUtil.copyProperties(folder, FolderVO.class);
        
        // 统计直接子文件夹数量
        Long childFolderCount = Db.lambdaQuery(NoteFolder.class)
                .eq(NoteFolder::getUserId, userId)
                .eq(NoteFolder::getParentId, folder.getId())
                .count();
        vo.setChildFolderCount(childFolderCount.intValue());
        
        // 递归统计该文件夹及所有子文件夹中的笔记总数
        Long noteCount = countAllNotesInFolder(userId, folder.getId());
        vo.setNoteCount(noteCount.intValue());
        
        return vo;
    }
    
    /**
     * 递归统计文件夹及其所有子文件夹中的笔记总数
     */
    private Long countAllNotesInFolder(Long userId, Long folderId) {
        // 统计当前文件夹下的笔记
        Long currentFolderNoteCount = Db.lambdaQuery(Note.class)
                .eq(Note::getUserId, userId)
                .eq(Note::getFolderId, folderId)
                .count();
        
        // 查找所有直接子文件夹
        List<NoteFolder> childFolders = Db.lambdaQuery(NoteFolder.class)
                .eq(NoteFolder::getUserId, userId)
                .eq(NoteFolder::getParentId, folderId)
                .list();
        
        // 递归统计每个子文件夹中的笔记
        Long childFoldersNoteCount = childFolders.stream()
                .mapToLong(childFolder -> countAllNotesInFolder(userId, childFolder.getId()))
                .sum();
        
        return currentFolderNoteCount + childFoldersNoteCount;
    }
    
    /**
     * 检查 targetFolderId 是否是 sourceFolderId 的后代文件夹（防止循环引用）
     */
    private boolean isDescendant(Long sourceFolderId, Long targetFolderId, Long userId) {
        Long currentId = targetFolderId;
        while (currentId != null) {
            if (currentId.equals(sourceFolderId)) {
                return true;
            }
            NoteFolder currentFolder = getById(currentId);
            if (currentFolder == null || !currentFolder.getUserId().equals(userId)) {
                break;
            }
            currentId = currentFolder.getParentId();
        }
        return false;
    }
}
