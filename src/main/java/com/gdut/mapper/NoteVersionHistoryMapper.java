package com.gdut.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gdut.domain.entity.note.NoteVersionHistory;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface NoteVersionHistoryMapper extends BaseMapper<NoteVersionHistory> {
}
