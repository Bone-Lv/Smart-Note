package com.gdut.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gdut.domain.entity.note.Note;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface NoteMapper extends BaseMapper<Note> {
    
    /**
     * 物理删除笔记（绕过逻辑删除）
     */
    @Delete("DELETE FROM note WHERE id = #{id}")
    int physicallyDeleteById(Long id);
}
