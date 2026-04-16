package com.gdut.common.config.ai;

import com.gdut.domain.entity.chat.ChatHistory;
import com.gdut.repository.ChatHistoryRepository;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.lang.NonNull;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 基于数据库的聊天记忆存储库
 * 将Spring AI的ChatMemoryRepository接口适配到我们的数据库存储
 */
public class DatabaseChatMemoryRepository implements ChatMemoryRepository {

    private final ChatHistoryRepository chatHistoryRepository;

    public DatabaseChatMemoryRepository(ChatHistoryRepository chatHistoryRepository) {
        this.chatHistoryRepository = chatHistoryRepository;
    }

    @Override
    @NonNull
    public List<String> findConversationIds() {
        return List.of();
    }

    @Override
    @NonNull
    public List<Message> findByConversationId(@NonNull String conversationId) {
        // 从数据库获取历史消息
        List<ChatHistory> histories = chatHistoryRepository.getMessages(conversationId, 100);
        
        // 转换为Spring AI的Message对象
        return histories.stream()
                .map(history -> {
                    if ("user".equals(history.getRole())) {
                        return new UserMessage(history.getContent());
                    } else {
                        return new AssistantMessage(history.getContent());
                    }
                })
                .collect(Collectors.toList());
    }

    @Override
    public void saveAll(@NonNull String conversationId, @NonNull List<Message> messages) {
    }

    @Override
    public void deleteByConversationId(@NonNull String conversationId) {
        chatHistoryRepository.deleteByConversationId(conversationId);
    }
}
