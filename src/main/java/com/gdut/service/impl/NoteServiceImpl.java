package com.gdut.service.impl;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.extension.toolkit.Db;
import com.gdut.common.util.AliyunOSSOperator;
import com.gdut.common.util.PdfUtil;
import com.gdut.constant.AppConstants;
import com.gdut.domain.dto.note.CreateAnnotationDTO;
import com.gdut.domain.dto.note.CreateNoteDTO;
import com.gdut.domain.dto.note.NoteQueryDTO;
import com.gdut.domain.dto.note.UpdateAnnotationDTO;
import com.gdut.domain.dto.note.UpdateNoteDTO;
import com.gdut.domain.dto.note.UpdateVisibilityDTO;
import com.gdut.domain.entity.chat.PrivateMessage;
import com.gdut.domain.entity.note.*;
import com.gdut.domain.entity.user.User;
import com.gdut.domain.vo.note.AnnotationVO;
import com.gdut.domain.vo.note.AiAnalysisVO;
import com.gdut.domain.vo.note.FriendPermissionVO;
import com.gdut.domain.vo.note.ImageUploadVO;
import com.gdut.domain.vo.note.NoteImportVO;
import com.gdut.domain.vo.note.NoteShareVO;
import com.gdut.domain.vo.note.NoteVO;
import com.gdut.domain.vo.note.NoteVersionHistoryVO;
import com.gdut.common.enums.NoteType;
import com.gdut.common.enums.NoteVisibility;
import com.gdut.mapper.*;
import com.gdut.service.NoteService;
import com.gdut.service.VectorStoreService;
import com.gdut.common.util.ChatWebSocketHandler;
import com.gdut.common.exception.BusinessException;
import com.gdut.common.enums.ResultCode;
import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import static java.nio.charset.StandardCharsets.UTF_8;

@Slf4j
@Service
@RequiredArgsConstructor
public class NoteServiceImpl extends ServiceImpl<NoteMapper, Note> implements NoteService {

    private final NoteFriendPermissionMapper noteFriendPermissionMapper;
    
    private final NoteVersionHistoryMapper noteVersionHistoryMapper;
    
    private final NoteAnnotationMapper noteAnnotationMapper;
    
    private final ChatClient analysisClient;
    
    private final AiUsageMapper aiUsageMapper;
    
    private final VectorStoreService vectorStoreService;
    
    private final UserMapper userMapper;
    
    private final PrivateMessageMapper privateMessageMapper;
    
    private final ChatWebSocketHandler webSocketHandler;

    private final AliyunOSSOperator aliyunOSSOperator;
    
    private final CacheManager cacheManager;

    private final NoteFolderMapper noteFolderMapper;
    
    @Value("${server.servlet.context-path:}")
    private String contextPath;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createNote(Long userId, CreateNoteDTO createNoteDTO) {
        // 转成 PO
        Note note = BeanUtil.copyProperties(createNoteDTO, Note.class);

        // 保存笔记
        note.setUserId(userId);
        note.setVisibility(NoteVisibility.PRIVATE);
        note.setVersion(1); // 初始版本为1
        save(note);
        
        // 同步到向量数据库
        vectorStoreService.addNoteToVectorStore(note.getId(), note.getTitle(), note.getContent(), note.getTags(), userId);
        
        return note.getId();
    }

    @Override
    public IPage<NoteVO> getNoteList(Long userId, NoteQueryDTO queryDTO) {
        // 判断是否使用游标分页
        if (queryDTO.getCursor() != null) {
            return getNoteListByCursor(userId, queryDTO);
        }
        
        // 传统分页（兼容旧代码）
        Page<Note> page = new Page<>(queryDTO.getPage(), queryDTO.getPageSize());

        // 设置查询条件
        LambdaQueryWrapper<Note> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Note::getUserId, userId);
        
        // 按文件夹筛选（支持三种模式：不传查所有、传ID查指定文件夹）
        if (queryDTO.getFolderId() != null) {
            wrapper.eq(Note::getFolderId, queryDTO.getFolderId());
        }
        
        // 如果传入了可见性条件，则进行筛选
        if (queryDTO.getVisibility() != null) {
            wrapper.eq(Note::getVisibility, queryDTO.getVisibility());
        }
        
        if (StrUtil.isNotBlank(queryDTO.getKeyword())) {
            wrapper.and(w -> w.like(Note::getTitle, queryDTO.getKeyword())
                    .or()
                    .like(Note::getTags, queryDTO.getKeyword()));
        }
        
        if ("title".equals(queryDTO.getSortOrder())) {
            wrapper.orderByAsc(Note::getTitle);
        } else {
            wrapper.orderByDesc(Note::getUpdateTime);
        }

        // 获得查询结果
        IPage<Note> notePage = page(page, wrapper);
        
        IPage<NoteVO> voPage = new Page<>(notePage.getCurrent(), notePage.getSize(), notePage.getTotal());
        // 用stream流转换成NoteVO展示给前端
        List<NoteVO> voList = notePage.getRecords().stream()
                .map(note -> {
                    NoteVO vo = BeanUtil.copyProperties(note, NoteVO.class);
                    // PDF笔记不可编辑，其他类型根据权限判断
                    vo.setCanEdit(note.getNoteType() != NoteType.PDF);
                    return vo;
                })
                .toList();
        voPage.setRecords(voList);
        
        return voPage;
    }
    
    /**
     * 使用游标分页查询笔记列表（解决深分页性能问题）
     */
    private IPage<NoteVO> getNoteListByCursor(Long userId, NoteQueryDTO queryDTO) {
        int pageSize = queryDTO.getPageSize() != null ? queryDTO.getPageSize() : 20;
        Long cursor = queryDTO.getCursor();
        
        // 设置查询条件
        LambdaQueryWrapper<Note> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Note::getUserId, userId);
        
        // 按文件夹筛选
        if (queryDTO.getFolderId() != null) {
            wrapper.eq(Note::getFolderId, queryDTO.getFolderId());
        }
        
        // 可见性筛选
        if (queryDTO.getVisibility() != null) {
            wrapper.eq(Note::getVisibility, queryDTO.getVisibility());
        }
        
        // 关键词搜索
        if (StrUtil.isNotBlank(queryDTO.getKeyword())) {
            wrapper.and(w -> w.like(Note::getTitle, queryDTO.getKeyword())
                    .or()
                    .like(Note::getTags, queryDTO.getKeyword()));
        }
        
        // 游标分页：根据排序方式添加游标条件
        if ("title".equals(queryDTO.getSortOrder())) {
            // 按标题排序时，暂不支持游标分页，降级为传统分页
            Page<Note> page = new Page<>(queryDTO.getPage(), pageSize);
            wrapper.orderByAsc(Note::getTitle);
            IPage<Note> notePage = page(page, wrapper);
            IPage<NoteVO> voPage = new Page<>(notePage.getCurrent(), notePage.getSize(), notePage.getTotal());
            List<NoteVO> voList = notePage.getRecords().stream()
                    .map(note -> {
                        NoteVO vo = BeanUtil.copyProperties(note, NoteVO.class);
                        vo.setCanEdit(note.getNoteType() != NoteType.PDF);
                        return vo;
                    })
                    .toList();
            voPage.setRecords(voList);
            return voPage;
        } else {
            // 按更新时间排序（默认）- 使用游标分页
            if (cursor != null) {
                // 获取游标记录的时间和ID
                Note cursorNote = getById(cursor);
                if (cursorNote != null) {
                    // 使用复合游标：先比较更新时间，再比较ID
                    wrapper.and(w -> w
                        .lt(Note::getUpdateTime, cursorNote.getUpdateTime())
                        .or()
                        .eq(Note::getUpdateTime, cursorNote.getUpdateTime())
                        .lt(Note::getId, cursor)
                    );
                }
            }
            wrapper.orderByDesc(Note::getUpdateTime);
            wrapper.orderByDesc(Note::getId); // 添加次要排序，确保稳定性
            
            // 多取一条数据用于判断是否有下一页
            wrapper.last("LIMIT " + (pageSize + 1));
            
            List<Note> notes = list(wrapper);
            
            // 判断是否有更多数据
            boolean hasNext = notes.size() > pageSize;
            Long nextCursor = null;
            
            if (hasNext) {
                // 移除多余的一条数据
                notes = notes.subList(0, pageSize);
                // 设置下一页游标为最后一条记录的ID
                nextCursor = notes.getLast().getId();
            }
            
            // 转换为VO
            List<NoteVO> voList = notes.stream()
                    .map(note -> {
                        NoteVO vo = BeanUtil.copyProperties(note, NoteVO.class);
                        vo.setCanEdit(note.getNoteType() != NoteType.PDF);
                        return vo;
                    })
                    .toList();
            
            // 返回IPage对象以保持接口兼容性，通过current字段传递游标信息
            Page<NoteVO> voPage = new Page<>();
            voPage.setRecords(voList);
            voPage.setSize(pageSize);
            voPage.setCurrent(nextCursor != null ? nextCursor : 0); // 用current字段存储nextCursor
            
            return voPage;
        }
    }

    @Override
    public NoteVO getNoteDetail(Long userId, Long noteId) {
        Cache cache = cacheManager.getCache("noteDetail");
        String cacheKey = userId + "_" + noteId;
        
        // 1. 先尝试从缓存获取
        if (cache != null) {
            Cache.ValueWrapper wrapper = cache.get(cacheKey);
            if (wrapper != null) {
                NoteVO cachedVo = (NoteVO) wrapper.get();
                // 即使命中缓存，也要执行副作用逻辑
                recordNoteAccess(userId, noteId);
                updateLastViewTimeSync(noteId);
                return cachedVo;
            }
        }

        // 2. 缓存未命中，执行数据库查询
        Note note = getById(noteId);
            
        if (note == null) {
            throw new BusinessException(ResultCode.NOTE_NOT_EXIST);
        }
            
        // 3. 权限校验
        if (!note.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.NOTE_NO_PERMISSION, "无权查看该笔记");
        }
            
        // 4. 更新最后查看时间
        lambdaUpdate()
                .eq(Note::getId, noteId)
                .set(Note::getLastViewTime, LocalDateTime.now())
                .update();
            
        note.setLastViewTime(LocalDateTime.now());
            
        NoteVO vo = BeanUtil.copyProperties(note, NoteVO.class);
        // PDF笔记不可编辑，其他类型默认可编辑（作者本人）
        vo.setCanEdit(note.getNoteType() != NoteType.PDF);
            
        // 5. 记录访问频率（LFU统计）
        recordNoteAccess(userId, noteId);

        // 6. 存入缓存
        if (cache != null) {
            cache.put(cacheKey, vo);
        }
            
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer updateNote(Long userId, Long noteId, UpdateNoteDTO updateNoteDTO) {
        Note note = getById(noteId);
        
        if (note == null) {
            throw new BusinessException(ResultCode.NOTE_NOT_EXIST);
        }
        
        // PDF笔记不允许编辑
        if (note.getNoteType() == NoteType.PDF) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "PDF笔记仅支持查看，不支持编辑");
        }
        
        // 判断当前用户是否有编辑权限
        if (!hasEditPermission(userId, note)) {
            throw new BusinessException(ResultCode.NOTE_NO_PERMISSION, "无权修改该笔记");
        }

        // === 创建新版本：先保存当前版本到历史表 ===
        NoteVersionHistory history = new NoteVersionHistory();
        history.setNoteId(noteId);
        history.setUserId(userId);
        history.setVersion(note.getVersion());
        history.setTitle(note.getTitle());
        history.setContent(note.getContent());
        history.setTags(note.getTags());
        history.setCreateTime(LocalDateTime.now());
        noteVersionHistoryMapper.insert(history);
        
        // 更新笔记内容（MP会自动递增@Version字段）
        LambdaUpdateWrapper<Note> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Note::getId, noteId);
        
        if (StrUtil.isNotBlank(updateNoteDTO.getTitle())) {
            updateWrapper.set(Note::getTitle, updateNoteDTO.getTitle());
        }
        if (StrUtil.isNotBlank(updateNoteDTO.getContent())) {
            updateWrapper.set(Note::getContent, updateNoteDTO.getContent());
        }
        if (updateNoteDTO.getTags() != null) {
            updateWrapper.set(Note::getTags, updateNoteDTO.getTags());
        }
        
        boolean success = update(updateWrapper);
        
        if (!success) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "笔记已被其他用户修改，请刷新后重试");
        }
        
        // 同步更新到向量数据库
        Note updatedNote = getById(noteId);
        vectorStoreService.addNoteToVectorStore(updatedNote.getId(), updatedNote.getTitle(), updatedNote.getContent(), updatedNote.getTags(), updatedNote.getUserId());
        
        log.info("用户{}将笔记{}保存为新版本，版本号: {}", userId, noteId, updatedNote.getVersion());
        
        return updatedNote.getVersion();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteNote(Long userId, Long noteId) {
        Note note = getById(noteId);
        
        if (note == null) {
            throw new BusinessException(ResultCode.NOTE_NOT_EXIST);
        }
        
        // 只有笔记所有者才能删除
        if (!note.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.NOTE_NO_PERMISSION, "无权删除该笔记");
        }
        
        // 检查笔记是否已在回收站中
        if (note.getDeleted() == 1) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "笔记已在回收站中");
        }
        
        // 从向量数据库删除（防止已删除的笔记被AI搜索到）
        vectorStoreService.removeNoteFromVectorStore(noteId);
        
        // 移入回收站：标记为已删除，记录删除时间（MP会自动递增@Version字段）
        LambdaUpdateWrapper<Note> deleteWrapper = new LambdaUpdateWrapper<>();
        deleteWrapper.eq(Note::getId, noteId)
                .set(Note::getDeleted, 1)
                .set(Note::getDeletedTime, LocalDateTime.now());
        
        boolean success = update(deleteWrapper);
        if (!success) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "笔记已被其他用户修改，请刷新后重试");
        }
        
        // 清除缓存
        clearNoteDetailCache(noteId);
        
        log.info("用户 {} 删除了笔记 {}，已移入回收站", userId, noteId);
    }

    @Override
    public List<NoteVO> getRecentViewedNotes(Long userId, Integer limit) {
        LambdaQueryWrapper<Note> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Note::getUserId, userId)
                .isNotNull(Note::getLastViewTime)
                .orderByDesc(Note::getLastViewTime)
                .last("LIMIT " + limit);
        
        List<Note> notes = list(wrapper);
        return notes.stream()
                .map(note -> {
                    NoteVO vo = BeanUtil.copyProperties(note, NoteVO.class);
                    // PDF笔记不可编辑
                    vo.setCanEdit(note.getNoteType() != NoteType.PDF);
                    return vo;
                })
                .toList();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<NoteVO> getTop3FrequentNotes(Long userId) {
        Cache frequencyCache = cacheManager.getCache("noteFrequency");
        if (frequencyCache == null) {
            return List.of();
        }
        
        Cache detailCache = cacheManager.getCache("noteDetail");
        
        // 获取Caffeine原生缓存对象
        var nativeCache =
            (com.github.benmanes.caffeine.cache.Cache<String, Long>) frequencyCache.getNativeCache();
        
        return nativeCache.asMap().entrySet().stream()
                .filter(entry -> entry.getKey().startsWith(userId + "_"))
                .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                .limit(3)
                .map(entry -> {
                    Long noteId = Long.parseLong(entry.getKey().split("_")[1]);
                    if (detailCache != null) {
                        Cache.ValueWrapper wrapper = detailCache.get(noteId);
                        return wrapper != null ? (NoteVO) wrapper.get() : null;
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .toList();
    }
    
    /**
     * 记录笔记访问频率（LFU算法核心）
     */
    private void recordNoteAccess(Long userId, Long noteId) {
        Cache frequencyCache = cacheManager.getCache("noteFrequency");
        if (frequencyCache != null) {
            String key = userId + "_" + noteId;
            Cache.ValueWrapper wrapper = frequencyCache.get(key);
            long count = wrapper != null ? ((Number) wrapper.get()).longValue() : 0L;
            frequencyCache.put(key, count + 1);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateVisibility(Long userId, Long noteId, UpdateVisibilityDTO visibilityDTO) {
        // 获取当前笔记对象
        Note note = getById(noteId);
        
        if (note == null) {
            throw new BusinessException(ResultCode.NOTE_NOT_EXIST);
        }
        
        // 只有笔记所有者才能修改权限
        if (!note.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.NOTE_NO_PERMISSION, "无权操作该笔记");
        }
        
        NoteVisibility visibility = visibilityDTO.getVisibility();

        // 如果是公开或者私有笔记，则删除所有好友权限
        if (visibility == NoteVisibility.PRIVATE || visibility == NoteVisibility.PUBLIC) {
            LambdaQueryWrapper<NoteFriendPermission> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(NoteFriendPermission::getNoteId, noteId);
            noteFriendPermissionMapper.delete(wrapper);
        }

        // 如果是部分好友可见或者可编辑，则删除原有好友权限，再进行添加
        if (visibility == NoteVisibility.FRIENDS_VIEW || visibility == NoteVisibility.FRIENDS_EDIT) {
            if (visibilityDTO.getFriendUserIds() == null || visibilityDTO.getFriendUserIds().isEmpty()) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "请指定好友用户");
            }

            // 删除原来的好友权限
            LambdaQueryWrapper<NoteFriendPermission> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(NoteFriendPermission::getNoteId, noteId);
            noteFriendPermissionMapper.delete(wrapper);
            
            int canEdit = (visibility == NoteVisibility.FRIENDS_EDIT) ? 1 : 0;
            List<NoteFriendPermission> permissions = visibilityDTO.getFriendUserIds().stream()
                    .map(friendUserId -> {
                        // 创建权限对象
                        NoteFriendPermission permission = new NoteFriendPermission();
                        permission.setNoteId(noteId);
                        permission.setFriendUserId(friendUserId);
                        permission.setCanEdit(canEdit);
                        return permission;
                    })
                    .toList();
            
            // 使用批量插入替代循环单条插入，大幅提升性能
            Db.saveBatch(permissions);
            
            // 发送通知给被授权的好友
            sendNotePermissionNotification(userId, noteId, note.getTitle(), note.getTags(), visibilityDTO.getFriendUserIds(), canEdit);
        }
        
        // 更新可见性（MP会自动递增@Version字段）
        LambdaUpdateWrapper<Note> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Note::getId, noteId)
                .set(Note::getVisibility, visibility);
        
        boolean success = update(updateWrapper);
        if (!success) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "笔记已被其他用户修改，请刷新后重试");
        }
    }

    @Override
    public NoteShareVO generateShareInfo(Long userId, Long noteId) {
        Note note = getById(noteId);
        
        if (note == null) {
            throw new BusinessException(ResultCode.NOTE_NOT_EXIST);
        }
        
        // 只有笔记所有者才能生成分享信息
        if (!note.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.NOTE_NO_PERMISSION, "无权操作该笔记");
        }

        // 生成分享信息
        NoteShareVO shareVO = new NoteShareVO();
        shareVO.setNoteId(noteId);
        shareVO.setVisibility(note.getVisibility());

        // 如果是私有笔记，则生成分享码
        if (note.getVisibility() == NoteVisibility.PRIVATE) {
            if (StrUtil.isBlank(note.getShareCode())) {
                String shareCode = RandomUtil.randomString(8);
                lambdaUpdate()
                        .eq(Note::getId, noteId)
                        .set(Note::getShareCode, shareCode)
                        .update();
                note.setShareCode(shareCode);
            }
            shareVO.setShareCode(note.getShareCode());
            // ✅ 优化：分享码作为路径参数，不再暴露在查询字符串中
            shareVO.setShareUrl(contextPath + "/note/share/" + note.getShareCode());
        } else if (note.getVisibility() == NoteVisibility.PUBLIC) {
            shareVO.setShareUrl(contextPath + "/note/public/" + noteId);
        } else {
            shareVO.setShareUrl(contextPath + "/note/shared/" + noteId);
        }
        
        return shareVO;
    }

    @Override
    public NoteVO getNoteByShareCode(String shareCode) {
        // 根据分享码查询笔记
        LambdaQueryWrapper<Note> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Note::getShareCode, shareCode);
        Note note = getOne(wrapper);
        
        if (note == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "分享链接无效或已过期");
        }
        
        
        NoteVO vo = BeanUtil.copyProperties(note, NoteVO.class);
        // 通过分享码访问的笔记不可编辑
        vo.setCanEdit(false);
                    
        return vo;
    }

    @Override
    public NoteVO getPublicNote(Long noteId, Long currentUserId) {
        Note note = getById(noteId);
        
        if (note == null) {
            throw new BusinessException(ResultCode.NOTE_NOT_EXIST);
        }
        
        NoteVisibility visibility = note.getVisibility();
        
        // 仅自己可见的笔记，不能通过公开接口访问
        if (visibility == NoteVisibility.PRIVATE) {
            throw new BusinessException(ResultCode.FORBIDDEN, "该笔记未公开");
        }
        
        // 所有人可见的公开笔记
        if (visibility == NoteVisibility.PUBLIC) {
            // 仅当作者是当前登录用户时，才记录查看时间
            if (note.getUserId().equals(currentUserId)) {
                lambdaUpdate()
                        .eq(Note::getId, noteId)
                        .set(Note::getLastViewTime, LocalDateTime.now())
                        .update();
                note.setLastViewTime(LocalDateTime.now());
            }
            
            NoteVO vo = BeanUtil.copyProperties(note, NoteVO.class);
            vo.setCanEdit(note.getUserId().equals(currentUserId));
            return vo;
        }
        
        // 部分好友可见/可编辑的笔记，不属于公开笔记范畴
        throw new BusinessException(ResultCode.FORBIDDEN, "该笔记需要对好友可见，请使用共享笔记接口访问");
    }
    
    @Override
    public NoteVO getSharedNote(Long userId, Long noteId) {
        Note note = getById(noteId);
        
        if (note == null) {
            throw new BusinessException(ResultCode.NOTE_NOT_EXIST);
        }
        
        NoteVisibility visibility = note.getVisibility();
        
        // 仅自己可见或所有人可见的笔记，不走共享逻辑
        if (visibility == NoteVisibility.PRIVATE || visibility == NoteVisibility.PUBLIC) {
            throw new BusinessException(ResultCode.FORBIDDEN, "该笔记不是共享笔记");
        }
        
        // 如果是作者本人查看
        if (note.getUserId().equals(userId)) {
            lambdaUpdate()
                    .eq(Note::getId, noteId)
                    .set(Note::getLastViewTime, LocalDateTime.now())
                    .update();
            note.setLastViewTime(LocalDateTime.now());
            
            NoteVO vo = BeanUtil.copyProperties(note, NoteVO.class);
            vo.setCanEdit(true);
            return vo;
        }
        
        // 判断是否是有权限查看该笔记的好友
        LambdaQueryWrapper<NoteFriendPermission> permissionWrapper = new LambdaQueryWrapper<>();
        permissionWrapper.eq(NoteFriendPermission::getNoteId, noteId)
                .eq(NoteFriendPermission::getFriendUserId, userId);
        NoteFriendPermission permission = noteFriendPermissionMapper.selectOne(permissionWrapper);
        
        if (permission == null) {
            throw new BusinessException(ResultCode.NOTE_NO_PERMISSION, "您无权查看该笔记");
        }
        
        NoteVO vo = BeanUtil.copyProperties(note, NoteVO.class);
        vo.setCanEdit(permission.getCanEdit() == 1);
        
        return vo;
    }

    @Override
    public List<FriendPermissionVO> getNotePermissions(Long userId, Long noteId) {
        Note note = getById(noteId);
        
        if (note == null) {
            throw new BusinessException(ResultCode.NOTE_NOT_EXIST);
        }
        
        // 仅限作者查看权限列表
        if (!note.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.NOTE_NO_PERMISSION, "仅作者可以查看权限列表");
        }
        
        // 查询该笔记的所有好友权限
        List<NoteFriendPermission> permissions = Db.lambdaQuery(NoteFriendPermission.class)
                .eq(NoteFriendPermission::getNoteId, noteId)
                .list();
        
        if (permissions.isEmpty()) {
            return List.of();
        }
        
        // 批量获取用户名
        Set<Long> friendIds = permissions.stream()
                .map(NoteFriendPermission::getFriendUserId)
                .collect(Collectors.toSet());
        
        Map<Long, String> usernameMap = Db.lambdaQuery(User.class)
                .in(User::getId, friendIds)
                .list()
                .stream()
                .collect(Collectors.toMap(User::getId, User::getUsername));
        
        // 转换为 VO
        return permissions.stream().map(p -> {
            FriendPermissionVO vo = new FriendPermissionVO();
            vo.setFriendUserId(p.getFriendUserId());
            vo.setFriendUsername(usernameMap.getOrDefault(p.getFriendUserId(), "未知用户"));
            vo.setCanEdit(p.getCanEdit() == 1);
            return vo;
        }).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AiAnalysisVO analyzeNote(Long userId, Long noteId, Boolean forceRefresh) {

        Note note = getById(noteId);
        
        if (note == null) {
            throw new BusinessException(ResultCode.NOTE_NOT_EXIST);
        }
        
        // 只有笔记所有者才能进行AI分析
        if (!note.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.NOTE_NO_PERMISSION, "无权操作该笔记");
        }

        if(note.getContent() == null){
            throw new BusinessException(ResultCode.BAD_REQUEST, "笔记内容为空，无法进行分析");
        }

        // 判断是否已有分析结果
        if (StrUtil.isNotBlank(note.getAiAnalysis()) && (forceRefresh == null || !forceRefresh)) {
            // 已有分析结果且不强制刷新，直接返回缓存结果
            return JSONUtil.toBean(note.getAiAnalysis(), AiAnalysisVO.class);
        }


        try {
            // 调用千问大模型进行分析
            String aiResponse = analysisClient.prompt()
                    .user(note.getContent())
                    .call()
                    .content();

            // 校验返回结果是否包含安全拒绝信息
            if (aiResponse != null && aiResponse.contains("无法提供涉及系统安全或数据隐私的操作建议")) {
                throw new BusinessException(ResultCode.FORBIDDEN, "笔记内容包含安全风险，AI 拒绝进行分析");
            }
            
            // 解析 AI 返回的 JSON 结果
            AiAnalysisVO analysisVO;
            try {
                analysisVO = JSONUtil.toBean(aiResponse, AiAnalysisVO.class);
            } catch (Exception e) {
                log.error("AI 返回的 JSON 解析失败, response: {}", aiResponse, e);
                throw new BusinessException(ResultCode.INTERNAL_SERVER_ERROR, "AI 分析结果格式异常，请重试");
            }
            
            // 将分析结果持久化到数据库
            String aiAnalysisJson = JSONUtil.toJsonStr(analysisVO);
            lambdaUpdate()
                    .eq(Note::getId, noteId)
                    .set(Note::getAiAnalysis, aiAnalysisJson)
                    .update();
            
            // 4. 记录用量
            recordUsage(userId);
            
            return analysisVO;
        } catch (RuntimeException e) {
            // 业务异常直接抛出
            throw e;
        } catch (Exception e) {
            log.error("AI 智能分析失败, noteId: {}", noteId, e);
            throw new BusinessException(ResultCode.INTERNAL_SERVER_ERROR, "AI 智能分析失败: " + e.getMessage());
        }
    }
    
    /**
     * 记录用量：检查日期，跨天则重置，同天则 +1（并发安全）
     */
    private void recordUsage(Long userId) {
        LocalDate today = LocalDate.now();
        int dailyLimit = AppConstants.DAILY_AI_USAGE_LIMIT;

        try {
            // 1. 尝试插入今日记录（利用数据库唯一索引保证并发安全）
            AiUsage newUsage = new AiUsage();
            newUsage.setUserId(userId);
            newUsage.setUsageDate(today);
            newUsage.setUsageCount(1);
            newUsage.setUpdateTime(LocalDateTime.now());
            Db.save(newUsage);
        } catch (org.springframework.dao.DuplicateKeyException e) {
            // 2. 如果记录已存在，执行原子性递增或重置
            // 先查询当前记录
            AiUsage existingUsage = Db.lambdaQuery(AiUsage.class)
                    .eq(AiUsage::getUserId, userId)
                    .one();

            if (existingUsage != null) {
                if (!existingUsage.getUsageDate().isEqual(today)) {
                    // 跨天了，更新日期并重置计数为 1
                    LambdaUpdateWrapper<AiUsage> resetWrapper = new LambdaUpdateWrapper<>();
                    resetWrapper.eq(AiUsage::getId, existingUsage.getId())
                            .set(AiUsage::getUsageDate, today)
                            .set(AiUsage::getUsageCount, 1)
                            .set(AiUsage::getUpdateTime, LocalDateTime.now());
                    Db.update(null, resetWrapper);
                } else {
                    // 同一天，原子性递增并检查上限
                    LambdaUpdateWrapper<AiUsage> incrementWrapper = new LambdaUpdateWrapper<>();
                    incrementWrapper.eq(AiUsage::getId, existingUsage.getId())
                            .lt(AiUsage::getUsageCount, dailyLimit) // 只有未超限时才更新
                            .setSql("usage_count = usage_count + 1")
                            .set(AiUsage::getUpdateTime, LocalDateTime.now());

                    int updatedRows = aiUsageMapper.update(null, incrementWrapper);

                    // 如果更新行数为 0，说明已经达到或超过上限
                    if (updatedRows == 0) {
                        throw new BusinessException(ResultCode.AI_QUOTA_EXCEEDED, "今日 AI 分析次数已达上限（" + dailyLimit + "次），请明天再试");
                    }
                }
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public NoteImportVO importMarkdownNote(Long userId, MultipartFile file, Long folderId) {
        // 反向校验：验证文件
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "文件不能为空");
        }
        
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.toLowerCase().endsWith(".md")) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "仅支持.md格式的Markdown文件");
        }
        
        try {
            // 读取Markdown文件内容
            String content = new String(file.getBytes(), UTF_8);
            
            // 提取标题（使用第一个标题或文件名）
            String title = extractMarkdownTitle(content, originalFilename);
            
            // 创建并保存笔记
            Note note = createAndSaveNote(userId, folderId, title, content, NoteType.MARKDOWN);
            
            // 构建返回结果
            NoteImportVO vo = new NoteImportVO();
            vo.setNoteId(note.getId());
            vo.setTitle(note.getTitle());
            vo.setNoteType(NoteType.MARKDOWN);
            vo.setCanEdit(true);
            
            return vo;
        } catch (Exception e) {
            log.error("Markdown导入失败", e);
            throw new BusinessException(ResultCode.INTERNAL_SERVER_ERROR, "Markdown导入失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public NoteImportVO importPdfNote(Long userId, MultipartFile file, Long folderId) {
        // 反向校验：验证文件
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "文件不能为空");
        }
        
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.toLowerCase().endsWith(".pdf")) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "仅支持.pdf格式的PDF文件");
        }
        
        // 验证文件大小（限制20MB）
        if (file.getSize() > 20 * 1024 * 1024) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "PDF文件大小不能超过20MB");
        }
        
        try {
            // 从PDF提取文本
            String content = PdfUtil.extractText(file.getInputStream());
            
            // 反向校验：检查提取的文本是否为空
            if (content == null || content.trim().isEmpty()) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "PDF文件中未提取到文本内容");
            }
            
            // 提取标题（使用文件名）
            String title = originalFilename.replaceAll("\\.pdf$", "");
            
            // 创建并保存笔记
            Note note = createAndSaveNote(userId, folderId, title, content, NoteType.PDF);
            
            // 构建返回结果
            NoteImportVO vo = new NoteImportVO();
            vo.setNoteId(note.getId());
            vo.setTitle(note.getTitle());
            vo.setNoteType(NoteType.PDF);
            vo.setCanEdit(false); // PDF笔记不可编辑
            
            return vo;
        } catch (Exception e) {
            log.error("PDF导入失败", e);
            throw new BusinessException(ResultCode.INTERNAL_SERVER_ERROR, "PDF导入失败: " + e.getMessage());
        }
    }

    @Override
    public void exportAsMarkdown(Long userId, Long noteId, HttpServletResponse response) {
        Note note = getById(noteId);
        
        // 反向校验：验证笔记是否存在
        if (note == null) {
            throw new BusinessException(ResultCode.NOTE_NOT_EXIST);
        }
        
        // 验证权限
        if (!note.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.NOTE_NO_PERMISSION, "无权导出该笔记");
        }
        
        try {
            // 构建Markdown内容
            StringBuilder mdContent = new StringBuilder();
            mdContent.append("# ").append(note.getTitle()).append("\n\n");
            
            if (StrUtil.isNotBlank(note.getTags())) {
                mdContent.append("**标签**: ").append(note.getTags()).append("\n\n");
            }
            
            mdContent.append(note.getContent()).append("\n\n");
            
            if (StrUtil.isNotBlank(note.getAiAnalysis())) {
                mdContent.append("---\n\n");
                mdContent.append("## AI分析\n\n");
                mdContent.append(note.getAiAnalysis()).append("\n\n");
            }
            
            // 设置HTTP响应头
            response.setContentType("text/markdown;charset=UTF-8");
            response.setHeader("Content-Disposition", 
                "attachment;filename=" + URLEncoder.encode(note.getTitle() + ".md", UTF_8));
            
            // 写入响应流
            response.getWriter().write(mdContent.toString());
            response.getWriter().flush();
        } catch (Exception e) {
            log.error("Markdown导出失败", e);
            throw new BusinessException(ResultCode.INTERNAL_SERVER_ERROR, "Markdown导出失败: " + e.getMessage());
        }
    }

    @Override
    public void exportAsPdf(Long userId, Long noteId, HttpServletResponse response) {
        Note note = getById(noteId);
        
        // 反向校验：验证笔记是否存在
        if (note == null) {
            throw new BusinessException(ResultCode.NOTE_NOT_EXIST);
        }
        
        // 验证权限
        if (!note.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.NOTE_NO_PERMISSION, "无权导出该笔记");
        }
        
        try {
            // 使用iText 7生成PDF
            PdfWriter writer = new PdfWriter(response.getOutputStream());
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc);
            
            // 设置默认字体（iText 7默认支持Unicode）
            PdfFont font = PdfFontFactory.createFont(
                StandardFonts.HELVETICA);
            
            // 添加标题
            Paragraph title = new Paragraph(note.getTitle())
                .setFont(font)
                .setFontSize(24)
                .setBold();
            document.add(title);
            
            // 添加标签
            if (StrUtil.isNotBlank(note.getTags())) {
                Paragraph tags = new Paragraph("标签: " + note.getTags())
                    .setFont(font)
                    .setFontSize(12);
                document.add(tags);
            }
            
            // 添加内容
            Paragraph content = new Paragraph(note.getContent())
                .setFont(font)
                .setFontSize(12);
            document.add(content);
            
            // 添加AI分析（如果有）
            if (StrUtil.isNotBlank(note.getAiAnalysis())) {
                document.add(new Paragraph("\n").setFontSize(12));
                Paragraph analysis = new Paragraph("AI分析: " + note.getAiAnalysis())
                    .setFont(font)
                    .setFontSize(10);
                document.add(analysis);
            }
            // 关闭文档
            document.close();
            
            // 设置HTTP响应头
            response.setContentType("application/pdf");
            response.setHeader("Content-Disposition", 
                "attachment;filename=" + URLEncoder.encode(note.getTitle() + ".pdf", UTF_8));
        } catch (Exception e) {
            log.error("PDF导出失败", e);
            throw new BusinessException(ResultCode.INTERNAL_SERVER_ERROR, "PDF导出失败: " + e.getMessage());
        }
    }
    
    /**
     * 从Markdown内容中提取标题
     *
     * @param content Markdown内容
     * @param defaultFilename 默认文件名
     * @return 标题
     */
    private String extractMarkdownTitle(String content, String defaultFilename) {
        // 尝试从Markdown的第一个标题提取
        if (content != null && content.contains("# ")) {
            String[] lines = content.split("\n");
            for (String line : lines) {
                if (line.startsWith("# ")) {
                    return line.replaceFirst("^#+\\s*", "").trim();
                }
            }
        }
        
        // 如果没有找到标题，使用文件名（去掉.md后缀）
        return defaultFilename.replaceAll("\\.md$", "");
    }

    /**
     * 创建并保存笔记（通用方法）
     *
     * @param userId 用户ID
     * @param folderId 文件夹ID
     * @param title 标题
     * @param content 内容
     * @param noteType 笔记类型
     * @return 保存后的笔记对象
     */
    private Note createAndSaveNote(Long userId, Long folderId, String title, String content, NoteType noteType) {
        Note note = new Note();
        note.setUserId(userId);
        note.setFolderId(folderId);
        note.setTitle(title);
        note.setContent(content);
        note.setNoteType(noteType);
        note.setVisibility(NoteVisibility.PRIVATE);
        save(note);
        
        // 同步到向量数据库
        vectorStoreService.addNoteToVectorStore(note.getId(), note.getTitle(), note.getContent(), note.getTags(), userId);
        
        return note;
    }

    /**
     * 发送笔记权限通知给好友（通过私聊消息）
     *
     * @param ownerId 笔记所有者ID
     * @param noteId 笔记ID
     * @param noteTitle 笔记标题
     * @param noteTags 笔记标签
     * @param friendUserIds 好友ID列表
     * @param canEdit 是否可编辑
     */
    private void sendNotePermissionNotification(Long ownerId, Long noteId, String noteTitle, 
                                                String noteTags, List<Long> friendUserIds, int canEdit) {
        // 获取笔记所有者用户名
        User owner = userMapper.selectById(ownerId);
        String ownerName = owner != null ? owner.getUsername() : "未知用户";
        
        String permType = canEdit == 1 ? "编辑" : "查看";
        String noteUrl = contextPath + "/note/shared/" + noteId;
        
        // 构建消息内容：标题 + 标签 + URL
        StringBuilder contentBuilder = new StringBuilder();
        contentBuilder.append("📝 用户 ").append(ownerName).append(" 授权你");
        contentBuilder.append(permType).append("笔记\n\n");
        contentBuilder.append("📌 《").append(noteTitle).append("》\n");
        
        if (StrUtil.isNotBlank(noteTags)) {
            contentBuilder.append("🏷️ 标签：").append(noteTags).append("\n");
        }
        
        contentBuilder.append("\n🔗 点击查看：").append(noteUrl);
        
        String messageContent = contentBuilder.toString();
        
        for (Long friendUserId : friendUserIds) {
            try {
                //  先保存消息到数据库（保证聊天记录完整性）
                PrivateMessage message = new PrivateMessage();
                message.setSenderId(ownerId);
                message.setReceiverId(friendUserId);
                message.setMessageType(1); // 1-文本消息
                message.setContent(messageContent);
                message.setIsRead(0); // 未读
                message.setDeletedBySender(0);
                message.setDeletedByReceiver(0);
                message.setCreateTime(LocalDateTime.now());
                
                privateMessageMapper.insert(message);
                
                //  再判断用户是否在线，在线则实时推送
                if (webSocketHandler.isUserOnline(friendUserId)) {
                    Map<String, Object> notification = new HashMap<>();
                    notification.put("type", "note_permission");
                    notification.put("noteId", noteId);
                    notification.put("noteTitle", noteTitle);
                    notification.put("ownerName", ownerName);
                    notification.put("permission", permType);
                    notification.put("noteUrl", noteUrl);
                    notification.put("createTime", LocalDateTime.now().toString());
                    
                    webSocketHandler.sendMessageToUser(friendUserId, notification);
                    log.info("笔记权限通知已保存并实时推送给用户{}：笔记《{}》已开放{}权限", friendUserId, noteTitle, permType);
                } else {
                    log.info("笔记权限通知已保存为离线消息给用户{}：笔记《{}》", friendUserId, noteTitle);
                }
            } catch (Exception e) {
                log.error("发送笔记权限通知失败，friendUserId: {}, noteId: {}", friendUserId, noteId, e);
            }
        }
    }

    @Override
    public ImageUploadVO uploadNoteImage(MultipartFile file) {
        // 1. 验证文件是否为空
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "请选择要上传的图片");
        }
        
        // 2. 验证文件类型
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.matches(".*\\.(jpg|jpeg|png|gif|webp|bmp|svg)$")) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "仅支持jpg、jpeg、png、gif、webp、bmp、svg格式的图片");
        }
        
        // 3. 验证文件大小（限制10MB）
        if (file.getSize() > 10 * 1024 * 1024) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "图片大小不能超过10MB");
        }
        
        try {
            // 4. 上传到OSS
            String imageUrl = aliyunOSSOperator.upload(file, originalFilename);
            
            // 5. 构造返回结果
            ImageUploadVO vo = new ImageUploadVO();
            vo.setImageUrl(imageUrl);
            vo.setMarkdownUrl("![" + originalFilename + "](" + imageUrl + ")");
            
            log.info("图片上传成功：{}", originalFilename);
            return vo;
        } catch (Exception e) {
            log.error("图片上传失败", e);
            throw new BusinessException(ResultCode.INTERNAL_SERVER_ERROR, "图片上传失败：" + e.getMessage());
        }
    }

    @Override
    public List<NoteVersionHistoryVO> getVersionHistory(Long userId, Long noteId) {
        Note note = getById(noteId);
        
        if (note == null) {
            throw new BusinessException(ResultCode.NOTE_NOT_EXIST);
        }
        
        // 权限校验：仅作者或有编辑权限的人可以查看版本历史
        if (!note.getUserId().equals(userId)) {
            boolean hasPermission = Db.lambdaQuery(NoteFriendPermission.class)
                    .eq(NoteFriendPermission::getNoteId, noteId)
                    .eq(NoteFriendPermission::getFriendUserId, userId)
                    .eq(NoteFriendPermission::getCanEdit, 1)
                    .exists();
            if (!hasPermission) {
                throw new BusinessException(ResultCode.NOTE_NO_PERMISSION, "无权查看该笔记的版本历史");
            }
        }
        
        // 查询版本历史，按版本号倒序
        List<NoteVersionHistory> histories = noteVersionHistoryMapper.selectList(
                new LambdaQueryWrapper<NoteVersionHistory>()
                        .eq(NoteVersionHistory::getNoteId, noteId)
                        .orderByDesc(NoteVersionHistory::getVersion)
        );
        
        return histories.stream().map(history -> {
            NoteVersionHistoryVO vo = BeanUtil.copyProperties(history, NoteVersionHistoryVO.class);
            // 设置内容长度
            vo.setContentLength(history.getContent() != null ? history.getContent().length() : 0);
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public NoteVO getVersionDetail(Long userId, Long noteId, Integer version) {
        Note note = getById(noteId);
        
        if (note == null) {
            throw new BusinessException(ResultCode.NOTE_NOT_EXIST);
        }
        
        // 权限校验
        if (!note.getUserId().equals(userId)) {
            boolean hasPermission = Db.lambdaQuery(NoteFriendPermission.class)
                    .eq(NoteFriendPermission::getNoteId, noteId)
                    .eq(NoteFriendPermission::getFriendUserId, userId)
                    .eq(NoteFriendPermission::getCanEdit, 1)
                    .exists();
            if (!hasPermission) {
                throw new BusinessException(ResultCode.NOTE_NO_PERMISSION, "无权查看该笔记的历史版本");
            }
        }
        
        // 如果查询的是当前版本，直接返回
        if (note.getVersion().equals(version)) {
            NoteVO vo = BeanUtil.copyProperties(note, NoteVO.class);
            vo.setCanEdit(note.getNoteType() != NoteType.PDF);
            return vo;
        }
        
        // 查询历史版本
        NoteVersionHistory history = noteVersionHistoryMapper.selectOne(
                new LambdaQueryWrapper<NoteVersionHistory>()
                        .eq(NoteVersionHistory::getNoteId, noteId)
                        .eq(NoteVersionHistory::getVersion, version)
        );
        
        if (history == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "指定版本不存在");
        }
        
        NoteVO vo = new NoteVO();
        vo.setId(noteId);
        vo.setUserId(note.getUserId());
        vo.setFolderId(note.getFolderId());
        vo.setTitle(history.getTitle());
        vo.setContent(history.getContent());
        vo.setTags(history.getTags());
        vo.setNoteType(note.getNoteType());
        vo.setVersion(history.getVersion());
        vo.setCreateTime(history.getCreateTime());
        vo.setCanEdit(false); // 历史版本不可编辑
        
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer rollbackToVersion(Long userId, Long noteId, Integer targetVersion) {
        Note note = getById(noteId);
        
        if (note == null) {
            throw new BusinessException(ResultCode.NOTE_NOT_EXIST);
        }
        
        // PDF笔记不允许编辑
        if (note.getNoteType() == NoteType.PDF) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "PDF笔记仅支持查看，不支持回退");
        }
        
        // 权限校验：仅作者或编辑权限者可回退
        if (!note.getUserId().equals(userId)) {
            boolean hasPermission = Db.lambdaQuery(NoteFriendPermission.class)
                    .eq(NoteFriendPermission::getNoteId, noteId)
                    .eq(NoteFriendPermission::getFriendUserId, userId)
                    .eq(NoteFriendPermission::getCanEdit, 1)
                    .exists();
            if (!hasPermission) {
                throw new BusinessException(ResultCode.NOTE_NO_PERMISSION, "无权回退该笔记的版本");
            }
        }
        
        // 目标版本号不能大于当前版本
        if (targetVersion >= note.getVersion()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "目标版本号不能大于或等于当前版本");
        }
        
        if (targetVersion < 1) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "目标版本号不能小于1");
        }
        
        // 查找目标版本的内容
        NoteVersionHistory targetHistory = noteVersionHistoryMapper.selectOne(
                new LambdaQueryWrapper<NoteVersionHistory>()
                        .eq(NoteVersionHistory::getNoteId, noteId)
                        .eq(NoteVersionHistory::getVersion, targetVersion)
        );
        
        if (targetHistory == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "指定版本不存在");
        }
        
        // 1. 先将当前版本保存到历史表中（归档当前版本）
        NoteVersionHistory currentHistory = new NoteVersionHistory();
        currentHistory.setNoteId(noteId);
        currentHistory.setUserId(userId);
        currentHistory.setVersion(note.getVersion());
        currentHistory.setTitle(note.getTitle());
        currentHistory.setContent(note.getContent());
        currentHistory.setTags(note.getTags());
        currentHistory.setCreateTime(LocalDateTime.now());
        noteVersionHistoryMapper.insert(currentHistory);
        
        // 2. 用目标版本的内容更新笔记，并递增版本号
        Integer newVersion = note.getVersion() + 1;
        lambdaUpdate()
                .eq(Note::getId, noteId)
                .set(Note::getTitle, targetHistory.getTitle())
                .set(Note::getContent, targetHistory.getContent())
                .set(Note::getTags, targetHistory.getTags())
                .set(Note::getVersion, newVersion)
                .update();
        
        // 3. 同步更新到向量数据库
        vectorStoreService.addNoteToVectorStore(noteId, targetHistory.getTitle(), targetHistory.getContent(), targetHistory.getTags(), userId);
        
        log.info("用户{}将笔记{}回退到版本{}，新版本号: {}", userId, noteId, targetVersion, newVersion);
        
        return newVersion;
    }

    @Override
    public List<AnnotationVO> getNoteAnnotations(Long userId, Long noteId) {
        Note note = getById(noteId);
        if (note == null) {
            throw new BusinessException(ResultCode.NOTE_NOT_EXIST);
        }
        
        // 检查访问权限
        checkNotePermission(userId, note);
        
        // 查询批注列表，按时间倒序
        LambdaQueryWrapper<NoteAnnotation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(NoteAnnotation::getNoteId, noteId)
               .orderByDesc(NoteAnnotation::getCreateTime);
        
        List<NoteAnnotation> annotations = noteAnnotationMapper.selectList(wrapper);
        
        return annotations.stream()
                .map(annotation -> {
                    AnnotationVO vo = BeanUtil.copyProperties(annotation, AnnotationVO.class);
                    User user = userMapper.selectById(annotation.getUserId());
                    vo.setUsername(user != null ? user.getUsername() : "未知用户");
                    return vo;
                })
                .toList();
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createAnnotation(Long userId, Long noteId, CreateAnnotationDTO createAnnotationDTO) {
        Note note = getById(noteId);
        if (note == null) {
            throw new BusinessException(ResultCode.NOTE_NOT_EXIST);
        }
        
        // 检查访问权限
        checkNotePermission(userId, note);
        
        // 检查编辑权限（PDF不可编辑）
        if (note.getNoteType() == NoteType.PDF) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "PDF笔记不支持批注");
        }
        
        boolean hasEditPermission = hasEditPermission(userId, note);
        if (!hasEditPermission) {
            throw new BusinessException(ResultCode.NOTE_NO_PERMISSION, "没有权限对该笔记进行批注");
        }
        
        // 验证位置参数
        if (createAnnotationDTO.getStartPosition() == null || createAnnotationDTO.getEndPosition() == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "批注位置信息不能为空");
        }
        
        // 验证目标内容位置是否匹配
        String actualContent = note.getContent();
        if (actualContent == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "笔记内容为空，无法创建批注");
        }
        
        if (createAnnotationDTO.getStartPosition() < 0 || createAnnotationDTO.getEndPosition() > actualContent.length()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "批注位置超出笔记内容范围");
        }
        
        String contentAtPosition = actualContent.substring(
            createAnnotationDTO.getStartPosition(), 
            createAnnotationDTO.getEndPosition()
        );
        
        if (!contentAtPosition.equals(createAnnotationDTO.getTargetContent())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "目标内容与指定位置的内容不匹配");
        }
        
        NoteAnnotation annotation = new NoteAnnotation();
        annotation.setNoteId(noteId);
        annotation.setUserId(userId);
        annotation.setContent(createAnnotationDTO.getContent());
        annotation.setTargetContent(createAnnotationDTO.getTargetContent());
        annotation.setStartPosition(createAnnotationDTO.getStartPosition());
        annotation.setEndPosition(createAnnotationDTO.getEndPosition());
        annotation.setNoteVersion(note.getVersion());
        annotation.setCreateTime(LocalDateTime.now());
        annotation.setUpdateTime(LocalDateTime.now());
        
        noteAnnotationMapper.insert(annotation);
        
        return annotation.getId();
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateAnnotation(Long userId, Long annotationId, UpdateAnnotationDTO updateAnnotationDTO) {
        NoteAnnotation annotation = noteAnnotationMapper.selectById(annotationId);
        if (annotation == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "批注不存在");
        }
        
        // 只能修改自己的批注
        if (!annotation.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "只能修改自己的批注");
        }
        
        Note note = getById(annotation.getNoteId());
        if (note == null) {
            throw new BusinessException(ResultCode.NOTE_NOT_EXIST);
        }
        
        // PDF笔记不支持批注
        if (note.getNoteType() == NoteType.PDF) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "PDF笔记不支持批注");
        }
        
        // 检查是否仍有编辑权限
        boolean hasEditPermission = hasEditPermission(userId, note);
        if (!hasEditPermission) {
            throw new BusinessException(ResultCode.NOTE_NO_PERMISSION, "没有权限修改该笔记的批注");
        }
        
        LambdaUpdateWrapper<NoteAnnotation> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(NoteAnnotation::getId, annotationId)
               .set(StrUtil.isNotBlank(updateAnnotationDTO.getContent()), 
                    NoteAnnotation::getContent, updateAnnotationDTO.getContent())
               .set(updateAnnotationDTO.getTargetContent() != null, 
                    NoteAnnotation::getTargetContent, updateAnnotationDTO.getTargetContent())
               .set(NoteAnnotation::getUpdateTime, LocalDateTime.now());
        
        noteAnnotationMapper.update(null, wrapper);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAnnotation(Long userId, Long annotationId) {
        NoteAnnotation annotation = noteAnnotationMapper.selectById(annotationId);
        if (annotation == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "批注不存在");
        }
        
        Note note = getById(annotation.getNoteId());
        if (note == null) {
            throw new BusinessException(ResultCode.NOTE_NOT_EXIST);
        }
        
        // 权限判断：笔记所有者或批注作者可以删除
        boolean isOwner = note.getUserId().equals(userId);
        boolean isAuthor = annotation.getUserId().equals(userId);
        
        if (!isOwner && !isAuthor) {
            throw new BusinessException(ResultCode.NOTE_NO_PERMISSION, "没有权限删除该批注");
        }
        
        noteAnnotationMapper.deleteById(annotationId);
    }
    
    /**
     * 检查用户是否有权限访问笔记
     */
    private void checkNotePermission(Long userId, Note note) {
        if (note.getUserId().equals(userId)) {
            return;
        }
        
        switch (note.getVisibility()) {
            case PRIVATE:
                throw new BusinessException(ResultCode.FORBIDDEN, "无权限访问该笔记");
            case FRIENDS_VIEW:
            case FRIENDS_EDIT:
                LambdaQueryWrapper<NoteFriendPermission> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(NoteFriendPermission::getNoteId, note.getId())
                       .eq(NoteFriendPermission::getFriendUserId, userId);
                if (noteFriendPermissionMapper.selectCount(wrapper) == 0) {
                    throw new BusinessException(ResultCode.FORBIDDEN, "无权限访问该笔记");
                }
                break;
            case PUBLIC:
                break;
        }
    }
    
    /**
     * 检查用户是否有编辑权限
     */
    private boolean hasEditPermission(Long userId, Note note) {
        if (note.getUserId().equals(userId)) {
            return true;
        }
        
        if (note.getVisibility() == NoteVisibility.FRIENDS_EDIT) {
            LambdaQueryWrapper<NoteFriendPermission> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(NoteFriendPermission::getNoteId, note.getId())
                   .eq(NoteFriendPermission::getFriendUserId, userId)
                   .eq(NoteFriendPermission::getCanEdit, 1);
            return noteFriendPermissionMapper.selectCount(wrapper) > 0;
        }
        
        return false;
    }
    
    /**
     * 清除笔记详情缓存（所有用户的该笔记缓存）
     */
    private void clearNoteDetailCache(Long noteId) {
        Cache cache = cacheManager.getCache("noteDetail");
        if (cache != null) {
            // Caffeine 不支持直接按模式删除，需要遍历清理
            // 这里简单处理：清空整个缓存（生产环境建议优化）
            cache.clear();
            log.debug("已清除笔记 {} 的详情缓存", noteId);
        }
    }
    
    /**
     * 同步更新最后查看时间
     */
    private void updateLastViewTimeSync(Long noteId) {
        lambdaUpdate()
                .eq(Note::getId, noteId)
                .set(Note::getLastViewTime, LocalDateTime.now())
                .update();
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean acquireEditLock(Long userId, Long noteId) {
        Note note = getById(noteId);
        
        if (note == null) {
            throw new BusinessException(ResultCode.NOTE_NOT_EXIST);
        }
        
        // PDF笔记不支持编辑
        if (note.getNoteType() == NoteType.PDF) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "PDF笔记仅支持查看，不支持编辑");
        }
        
        // 检查权限：只有作者或有编辑权限的人才能获取编辑锁
        if (!hasEditPermission(userId, note)) {
            throw new BusinessException(ResultCode.NOTE_NO_PERMISSION, "没有权限编辑该笔记");
        }
        
        // 尝试获取编辑锁（使用数据库乐观锁）
        boolean updatedRows = lambdaUpdate()
                .eq(Note::getId, noteId)
                .isNull(Note::getEditingUserId) // 只有当无人编辑时才能获取锁
                .set(Note::getEditingUserId, userId)
                .set(Note::getEditingLockTime, LocalDateTime.now())
                .update();
        
        if (updatedRows) {
            log.info("用户{}成功获取笔记{}的编辑锁", userId, noteId);
            return true;
        } else {
            // 已经被其他人锁定
            Note currentNote = getById(noteId);
            if (currentNote != null && currentNote.getEditingUserId() != null) {
                log.info("用户{}请求编辑笔记{}被拒绝，当前编辑者为{}", userId, noteId, currentNote.getEditingUserId());
            }
            return false;
        }
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void releaseEditLock(Long userId, Long noteId) {
        Note note = getById(noteId);
        
        if (note == null) {
            throw new BusinessException(ResultCode.NOTE_NOT_EXIST);
        }
        
        // 只有持有编辑锁的用户才能释放
        if (note.getEditingUserId() != null && !note.getEditingUserId().equals(userId)) {
            log.warn("用户{}尝试释放笔记{}的编辑锁，但当前编辑者为{}", userId, noteId, note.getEditingUserId());
            return;
        }
        
        // 释放编辑锁
        lambdaUpdate()
                .eq(Note::getId, noteId)
                .set(Note::getEditingUserId, null)
                .set(Note::getEditingLockTime, null)
                .update();
        
        log.info("用户{}释放笔记{}的编辑锁", userId, noteId);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void syncNoteContent(Long userId, Long noteId, String content, String title, String tags) {
        Note note = getById(noteId);
        
        if (note == null) {
            throw new BusinessException(ResultCode.NOTE_NOT_EXIST);
        }
        
        // 验证编辑锁
        if (note.getEditingUserId() == null || !note.getEditingUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "未持有编辑锁，无法同步内容");
        }
        
        // PDF笔记不支持编辑
        if (note.getNoteType() == NoteType.PDF) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "PDF笔记仅支持查看，不支持编辑");
        }
        
        // 防抖保存：只更新内容，不递增版本号，不归档历史
        lambdaUpdate()
                .eq(Note::getId, noteId)
                .set(StrUtil.isNotBlank(content), Note::getContent, content)
                .set(StrUtil.isNotBlank(title), Note::getTitle, title)
                .set(tags != null, Note::getTags, tags)
                .set(Note::getUpdateTime, LocalDateTime.now())
                .update();
        
        // 同步到向量数据库（重新向量化）
        Note updatedNote = getById(noteId);
        vectorStoreService.addNoteToVectorStore(updatedNote.getId(), updatedNote.getTitle(), updatedNote.getContent(), updatedNote.getTags(), updatedNote.getUserId());
        
        // 清除缓存
        clearNoteDetailCache(noteId);
        
        log.debug("用户{}实时同步笔记{}的内容", userId, noteId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void moveNote(Long userId, Long noteId, Long folderId) {
        Note note = getById(noteId);

        if (note == null) {
            throw new BusinessException(ResultCode.NOTE_NOT_EXIST);
        }

        // 只有笔记所有者才能移动
        if (!note.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.NOTE_NO_PERMISSION, "无权移动该笔记");
        }

        // 如果指定了文件夹ID，验证文件夹是否存在且属于该用户
        if (folderId != null) {
            NoteFolder folder = noteFolderMapper.selectById(folderId);
            if (folder == null) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "文件夹不存在");
            }
            if (!folder.getUserId().equals(userId)) {
                throw new BusinessException(ResultCode.NOTE_NO_PERMISSION, "无权访问该文件夹");
            }
        }

        // 更新笔记的文件夹ID
        lambdaUpdate()
                .eq(Note::getId, noteId)
                .set(Note::getFolderId, folderId)
                .update();

        log.info("用户{}将笔记{}移动到文件夹{}", userId, noteId, folderId == null ? "根目录" : folderId);
    }
}
