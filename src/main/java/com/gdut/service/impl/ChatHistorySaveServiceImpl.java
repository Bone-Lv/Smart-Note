package com.gdut.service.impl;

import com.gdut.domain.entity.chat.ChatHistory;
import com.gdut.repository.ChatHistoryRepository;
import com.gdut.service.ChatHistorySaveService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatHistorySaveServiceImpl implements ChatHistorySaveService {

    private final ChatHistoryRepository chatHistoryRepository;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveUserMessage(Long userId, String conversationId, String content) {
        ChatHistory userMessage = new ChatHistory();
        userMessage.setUserId(userId);
        userMessage.setConversationId(conversationId);
        userMessage.setRole("user");
        userMessage.setContent(content);
        userMessage.setCreateTime(LocalDateTime.now());
        chatHistoryRepository.save(userMessage);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveUserMessageWithMetadata(Long userId, String conversationId, String content, String metadata) {
        ChatHistory userMessage = new ChatHistory();
        userMessage.setUserId(userId);
        userMessage.setConversationId(conversationId);
        userMessage.setRole("user");
        userMessage.setContent(content);
        userMessage.setMetadata(metadata);
        userMessage.setCreateTime(LocalDateTime.now());
        chatHistoryRepository.save(userMessage);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveAiMessage(Long userId, String conversationId, String content) {
        ChatHistory aiMessage = new ChatHistory();
        aiMessage.setUserId(userId);
        aiMessage.setConversationId(conversationId);
        aiMessage.setRole("assistant");
        aiMessage.setContent(content);
        aiMessage.setCreateTime(LocalDateTime.now());
        chatHistoryRepository.save(aiMessage);
        log.info("AI对话完成并保存, conversationId: {}", conversationId);
    }

    @Override
    @Async("aiMessageSaveExecutor")
    @Transactional(rollbackFor = Exception.class)
    public void saveAiMessageAsync(Long userId, String conversationId, String content) {
        saveAiMessage(userId, conversationId, content);
    }
}
