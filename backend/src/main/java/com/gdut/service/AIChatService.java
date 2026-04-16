package com.gdut.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.gdut.domain.dto.chat.ChatHistoryQueryDTO;
import com.gdut.domain.dto.chat.ChatMessageDTO;
import com.gdut.domain.entity.chat.ChatHistory;
import com.gdut.domain.vo.chat.ChatMessageVO;
import com.gdut.domain.vo.chat.AIConversationVO;
import com.gdut.domain.vo.chat.CursorPageResult;
import reactor.core.publisher.Flux;

import java.util.List;

public interface AIChatService extends IService<ChatHistory> {
    
    /**
     * 发送消息并获取AI回复(流式输出)
     */
    Flux<String> sendMessage(Long userId, ChatMessageDTO messageDTO);
        
    /**
     * 与文件对话:上传文件后提问,AI基于文件内容回答(流式输出)
     */
    Flux<String> chatWithDocument(Long userId,  ChatMessageDTO dto);
    
    /**
     * 查询聊天历史（游标分页）
     */
    CursorPageResult<ChatMessageVO> getChatHistory(Long userId, ChatHistoryQueryDTO queryDTO);
    
    /**
     * 获取用户的所有会话列表
     */
    List<AIConversationVO> getConversations(Long userId);
    
    /**
     * 删除指定会话
     */
    void deleteConversation(Long userId, String conversationId);
    
    /**
     * 清空所有会话
     */
    void clearAllConversations(Long userId);
}
