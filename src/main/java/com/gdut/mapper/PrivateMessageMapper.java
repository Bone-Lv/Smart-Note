package com.gdut.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gdut.domain.entity.chat.PrivateMessage;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PrivateMessageMapper extends BaseMapper<PrivateMessage> {
}
