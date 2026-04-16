package com.gdut.service;

/**
 * 聊天历史保存服务
 * 用于解决 @Transactional 自调用问题
 */
public interface ChatHistorySaveService {
    
    /**
     * 保存用户消息
     */
    void saveUserMessage(Long userId, String conversationId, String content);
    
    /**
     * 保存用户消息（带元数据）
     */
    void saveUserMessageWithMetadata(Long userId, String conversationId, String content, String metadata);
    
    /**
     * 保存AI回复
     */
    void saveAiMessage(Long userId, String conversationId, String content);
    
    /**
     * 异步保存AI回复
     */
    void saveAiMessageAsync(Long userId, String conversationId, String content);
}
