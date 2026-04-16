package com.gdut.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gdut.domain.entity.chat.ChatHistory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ChatHistoryMapper extends BaseMapper<ChatHistory> {
    
    /**
     * 根据条件查询聊天记录
     */
    @Select("SELECT * FROM chat_history ${ew.customSqlSegment}")
    List<ChatHistory> selectListWithWrapper(com.baomidou.mybatisplus.core.conditions.Wrapper<ChatHistory> queryWrapper);
}
