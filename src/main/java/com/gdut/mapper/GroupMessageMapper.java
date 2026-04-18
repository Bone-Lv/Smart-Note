package com.gdut.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gdut.domain.dto.chat.MemberUnreadItem;
import com.gdut.domain.entity.chat.GroupMessage;
import com.gdut.domain.vo.chat.GroupUnreadCountVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface GroupMessageMapper extends BaseMapper<GroupMessage> {
    
    List<GroupUnreadCountVO> batchSelectUnreadCount(@Param("items") List<MemberUnreadItem> items, @Param("userId") Long userId);
}
