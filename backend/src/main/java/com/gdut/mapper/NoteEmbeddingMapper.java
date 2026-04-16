package com.gdut.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gdut.domain.entity.note.NoteEmbedding;
import org.apache.ibatis.annotations.Mapper;

/**
 * 笔记向量存储 Mapper
 */
@Mapper
public interface NoteEmbeddingMapper extends BaseMapper<NoteEmbedding> {
}
