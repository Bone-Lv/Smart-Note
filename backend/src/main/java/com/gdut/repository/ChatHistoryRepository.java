package com.gdut.repository;

import com.gdut.domain.entity.chat.ChatHistory;

import java.util.List;

public interface ChatHistoryRepository {

    /**
     * 保存聊天记录
     */
    void save(ChatHistory chatHistory);

    /**
     * 获取指定会话的聊天记录
     */
    List<ChatHistory> getMessages(String conversationId, int limit);

    /**
     * 删除指定会话的所有记录
     */
    void deleteByConversationId(String conversationId);

    /**
     * 获取用户的所有会话ID列表
     */
    List<String> getConversationIds(Long userId);
}
