package com.gdut.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gdut.domain.entity.chat.ChatHistory;
import com.gdut.mapper.ChatHistoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class SqlChatHistory implements ChatHistoryRepository {

    private final ChatHistoryMapper chatHistoryMapper;

    @Override
    public void save(ChatHistory chatHistory) {
        chatHistoryMapper.insert(chatHistory);
    }

    @Override
    public List<ChatHistory> getMessages(String conversationId, int limit) {
        LambdaQueryWrapper<ChatHistory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatHistory::getConversationId, conversationId)
                .orderByAsc(ChatHistory::getCreateTime)
                .last("LIMIT " + limit);
        return chatHistoryMapper.selectList(wrapper);
    }

    @Override
    public void deleteByConversationId(String conversationId) {
        LambdaQueryWrapper<ChatHistory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatHistory::getConversationId, conversationId);
        chatHistoryMapper.delete(wrapper);
    }

    @Override
    public List<String> getConversationIds(Long userId) {
        LambdaQueryWrapper<ChatHistory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatHistory::getUserId, userId)
                .select(ChatHistory::getConversationId)
                .groupBy(ChatHistory::getConversationId)
                .orderByDesc(ChatHistory::getCreateTime);
        
        return chatHistoryMapper.selectListWithWrapper(wrapper).stream()
                .map(ChatHistory::getConversationId)
                .distinct()
                .collect(Collectors.toList());
    }
}
