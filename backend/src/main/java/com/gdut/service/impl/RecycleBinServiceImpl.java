package com.gdut.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.toolkit.Db;
import com.gdut.domain.entity.note.Note;
import com.gdut.domain.entity.note.NoteFolder;
import com.gdut.domain.vo.note.RecycleBinFolderVO;
import com.gdut.domain.vo.note.RecycleBinNoteVO;
import com.gdut.mapper.NoteFolderMapper;
import com.gdut.mapper.NoteMapper;
import com.gdut.service.RecycleBinService;
import com.gdut.service.VectorStoreService;
import com.gdut.common.exception.BusinessException;
import com.gdut.common.enums.ResultCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecycleBinServiceImpl implements RecycleBinService {
    
    private final NoteMapper noteMapper;
    private final NoteFolderMapper noteFolderMapper;
    private final VectorStoreService vectorStoreService;
    
    /**
     * 回收站保留时间（5分钟）
     */
    private static final Duration RETENTION_DURATION = Duration.ofMinutes(5);
    
    @Override
    public List<RecycleBinNoteVO> getRecycleBinNotes(Long userId) {
        // 查询已删除但还未过期的笔记
        LocalDateTime expireTime = LocalDateTime.now().minus(RETENTION_DURATION);
        
        LambdaQueryWrapper<Note> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Note::getUserId, userId)
               .eq(Note::getDeleted, 1)
               .ge(Note::getDeletedTime, expireTime)
               .orderByDesc(Note::getDeletedTime);
        
        List<Note> notes = noteMapper.selectList(wrapper);
        
        return notes.stream().map(note -> {
            RecycleBinNoteVO vo = BeanUtil.copyProperties(note, RecycleBinNoteVO.class);
            
            // 计算剩余时间
            LocalDateTime expireDateTime = note.getDeletedTime().plus(RETENTION_DURATION);
            long remainingSeconds = Duration.between(LocalDateTime.now(), expireDateTime).getSeconds();
            vo.setRemainingSeconds(Math.max(0, remainingSeconds));
            
            return vo;
        }).collect(Collectors.toList());
    }
    
    @Override
    public List<RecycleBinFolderVO> getRecycleBinFolders(Long userId) {
        // 查询已删除但还未过期的文件夹
        LocalDateTime expireTime = LocalDateTime.now().minus(RETENTION_DURATION);
        
        LambdaQueryWrapper<NoteFolder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(NoteFolder::getUserId, userId)
               .eq(NoteFolder::getDeleted, 1)
               .ge(NoteFolder::getDeletedTime, expireTime)
               .orderByDesc(NoteFolder::getDeletedTime);
        
        List<NoteFolder> folders = noteFolderMapper.selectList(wrapper);
        
        return folders.stream().map(folder -> {
            RecycleBinFolderVO vo = BeanUtil.copyProperties(folder, RecycleBinFolderVO.class);
            
            // 统计文件夹下的笔记数量（包括已删除的）
            LambdaQueryWrapper<Note> noteWrapper = new LambdaQueryWrapper<>();
            noteWrapper.eq(Note::getFolderId, folder.getId())
                       .eq(Note::getDeleted, 1);
            Long noteCount = noteMapper.selectCount(noteWrapper);
            vo.setNoteCount(noteCount.intValue());
            
            // 计算剩余时间
            LocalDateTime expireDateTime = folder.getDeletedTime().plus(RETENTION_DURATION);
            long remainingSeconds = Duration.between(LocalDateTime.now(), expireDateTime).getSeconds();
            vo.setRemainingSeconds(Math.max(0, remainingSeconds));
            
            return vo;
        }).collect(Collectors.toList());
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void restoreNote(Long userId, Long noteId) {
        Note note = noteMapper.selectById(noteId);
        
        if (note == null || !note.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.NOTE_NOT_EXIST, "笔记不存在或无权限");
        }
        
        if (note.getDeleted() == 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "笔记不在回收站中");
        }
        
        // 检查是否已过期
        if (isExpired(note.getDeletedTime())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "笔记已在回收站中超过5分钟，无法还原");
        }
        
        // 还原笔记：恢复deleted标志，清空deletedTime
        note.setDeleted(0);
        note.setDeletedTime(null);
        noteMapper.updateById(note);
        
        // 重新同步到向量数据库
        vectorStoreService.addNoteToVectorStore(note.getId(), note.getTitle(), note.getContent(), note.getTags(), userId);
        
        log.info("用户 {} 还原了笔记 {}", userId, noteId);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void restoreFolder(Long userId, Long folderId) {
        NoteFolder folder = noteFolderMapper.selectById(folderId);
        
        if (folder == null || !folder.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FOLDER_NOT_EXIST, "文件夹不存在或无权限");
        }
        
        if (folder.getDeleted() == 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "文件夹不在回收站中");
        }
        
        // 检查是否已过期
        if (isExpired(folder.getDeletedTime())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "文件夹已在回收站中超过5分钟，无法还原");
        }
        
        // 还原文件夹：恢复deleted标志，清空deletedTime
        folder.setDeleted(0);
        folder.setDeletedTime(null);
        noteFolderMapper.updateById(folder);
        
        // 同时还原文件夹下的所有笔记
        LambdaQueryWrapper<Note> noteWrapper = new LambdaQueryWrapper<>();
        noteWrapper.eq(Note::getFolderId, folderId)
                   .eq(Note::getUserId, userId)
                   .eq(Note::getDeleted, 1);
        
        List<Note> notes = noteMapper.selectList(noteWrapper);
        for (Note note : notes) {
            note.setDeleted(0);
            note.setDeletedTime(null);
            noteMapper.updateById(note);
            
            // 重新同步到向量数据库
            vectorStoreService.addNoteToVectorStore(note.getId(), note.getTitle(), note.getContent(), note.getTags(), userId);
        }
        
        log.info("用户 {} 还原了文件夹 {} 及其下的 {} 个笔记", userId, folderId, notes.size());
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void permanentlyDeleteNote(Long userId, Long noteId) {
        Note note = noteMapper.selectById(noteId);
        
        if (note == null || !note.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.NOTE_NOT_EXIST, "笔记不存在或无权限");
        }
        
        if (note.getDeleted() == 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "笔记不在回收站中");
        }
        
        // 从向量数据库删除
        vectorStoreService.removeNoteFromVectorStore(noteId);
        
        // 物理删除笔记
        noteMapper.physicallyDeleteById(noteId);
        
        log.info("用户 {} 彻底删除了笔记 {}", userId, noteId);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void permanentlyDeleteFolder(Long userId, Long folderId) {
        NoteFolder folder = noteFolderMapper.selectById(folderId);
        
        if (folder == null || !folder.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FOLDER_NOT_EXIST, "文件夹不存在或无权限");
        }
        
        if (folder.getDeleted() == 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "文件夹不在回收站中");
        }
        
        // 先彻底删除文件夹下的所有笔记
        LambdaQueryWrapper<Note> noteWrapper = new LambdaQueryWrapper<>();
        noteWrapper.eq(Note::getFolderId, folderId)
                   .eq(Note::getUserId, userId)
                   .eq(Note::getDeleted, 1);
        
        List<Note> notes = noteMapper.selectList(noteWrapper);
        for (Note note : notes) {
            // 从向量数据库删除
            vectorStoreService.removeNoteFromVectorStore(note.getId());
            // 物理删除
            noteMapper.physicallyDeleteById(note.getId());
        }
        
        // 物理删除文件夹
        noteFolderMapper.physicallyDeleteById(folderId);
        
        log.info("用户 {} 彻底删除了文件夹 {} 及其下的 {} 个笔记", userId, folderId, notes.size());
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void emptyRecycleBin(Long userId) {
        // 获取所有已删除的笔记并彻底删除
        LambdaQueryWrapper<Note> noteWrapper = new LambdaQueryWrapper<>();
        noteWrapper.eq(Note::getUserId, userId)
                   .eq(Note::getDeleted, 1);
        
        List<Note> notes = noteMapper.selectList(noteWrapper);
        for (Note note : notes) {
            vectorStoreService.removeNoteFromVectorStore(note.getId());
            noteMapper.physicallyDeleteById(note.getId());
        }
        
        // 获取所有已删除的文件夹并彻底删除
        LambdaQueryWrapper<NoteFolder> folderWrapper = new LambdaQueryWrapper<>();
        folderWrapper.eq(NoteFolder::getUserId, userId)
                    .eq(NoteFolder::getDeleted, 1);
        
        List<NoteFolder> folders = noteFolderMapper.selectList(folderWrapper);
        for (NoteFolder folder : folders) {
            noteFolderMapper.physicallyDeleteById(folder.getId());
        }
        
        log.info("用户 {} 清空了回收站，删除了 {} 个笔记和 {} 个文件夹", userId, notes.size(), folders.size());
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    @Scheduled(fixedRate = 30000) // 每30秒执行一次
    public void cleanupExpiredRecycleBinItems() {
        log.info("开始执行回收站定时清理任务...");
        
        LocalDateTime expireTime = LocalDateTime.now().minus(RETENTION_DURATION);
        
        // 清理过期的笔记
        LambdaQueryWrapper<Note> noteWrapper = new LambdaQueryWrapper<>();
        noteWrapper.eq(Note::getDeleted, 1)
                   .le(Note::getDeletedTime, expireTime);
        
        List<Note> expiredNotes = noteMapper.selectList(noteWrapper);
        int noteCount = 0;
        for (Note note : expiredNotes) {
            try {
                vectorStoreService.removeNoteFromVectorStore(note.getId());
                noteMapper.physicallyDeleteById(note.getId());
                noteCount++;
            } catch (Exception e) {
                log.error("清理过期笔记失败, noteId: {}", note.getId(), e);
            }
        }
        
        // 清理过期的文件夹
        LambdaQueryWrapper<NoteFolder> folderWrapper = new LambdaQueryWrapper<>();
        folderWrapper.eq(NoteFolder::getDeleted, 1)
                    .le(NoteFolder::getDeletedTime, expireTime);
        
        List<NoteFolder> expiredFolders = noteFolderMapper.selectList(folderWrapper);
        int folderCount = 0;
        for (NoteFolder folder : expiredFolders) {
            try {
                noteFolderMapper.physicallyDeleteById(folder.getId());
                folderCount++;
            } catch (Exception e) {
                log.error("清理过期文件夹失败, folderId: {}", folder.getId(), e);
            }
        }
        
        if (noteCount > 0 || folderCount > 0) {
            log.info("回收站定时清理完成，删除了 {} 个笔记和 {} 个文件夹", noteCount, folderCount);
        }
    }
    
    /**
     * 判断是否已过期
     */
    private boolean isExpired(LocalDateTime deletedTime) {
        if (deletedTime == null) {
            return true;
        }
        LocalDateTime expireTime = deletedTime.plus(RETENTION_DURATION);
        return LocalDateTime.now().isAfter(expireTime);
    }
}
