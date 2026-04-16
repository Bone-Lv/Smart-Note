package com.gdut.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gdut.domain.entity.note.NoteFolder;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface NoteFolderMapper extends BaseMapper<NoteFolder> {
    
    /**
     * 物理删除文件夹（绕过逻辑删除）
     */
    @Delete("DELETE FROM note_folder WHERE id = #{id}")
    int physicallyDeleteById(Long id);
}
