package com.gdut.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.gdut.annotation.RequireRole;
import com.gdut.common.exception.BusinessException;
import com.gdut.domain.dto.chat.ChatHistoryQueryDTO;
import com.gdut.domain.dto.chat.ChatMessageDTO;
import com.gdut.domain.entity.common.Result;
import com.gdut.domain.vo.chat.ChatMessageVO;
import com.gdut.domain.vo.chat.AIConversationVO;
import com.gdut.domain.vo.chat.CursorPageResult;
import com.gdut.service.AIChatService;
import com.gdut.common.util.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/ai/chat")
@RequiredArgsConstructor
@Tag(name = "AI聊天接口", description = "AI对话、历史记录管理等功能")
public class AIChatController {

    private final AIChatService aiChatService;


    @PostMapping("/message")
    @RequireRole
    @Operation(summary = "发送消息", description = "向AI发送消息并获取回复,支持上下文对话,流式输出")
    public Flux<String> sendMessage(@Valid @RequestBody ChatMessageDTO messageDTO) {
        Long userId = UserContext.getUserId();
        try {
            if(messageDTO.getFiles() != null && !messageDTO.getFiles().isEmpty()){
                return aiChatService.chatWithDocument(userId, messageDTO);
            }else{
                return aiChatService.sendMessage(userId, messageDTO);
            }
        } catch (BusinessException e) {
            log.error("业务异常: {}", e.getMessage());
            return Flux.just("错误：" + e.getMessage());
        } catch (Exception e) {
            log.error("系统异常", e);
            return Flux.just("抱歉，服务暂时不可用，请稍后重试");
        }
    }

    @GetMapping("/history")
    @RequireRole
    @Operation(summary = "查询聊天历史", description = "查询指定会话或所有会话的聊天历史，使用游标分页（传入cursor参数）")
    public Result<CursorPageResult<ChatMessageVO>> getChatHistory(ChatHistoryQueryDTO queryDTO) {
        Long userId = UserContext.getUserId();
        CursorPageResult<ChatMessageVO> history = aiChatService.getChatHistory(userId, queryDTO);
        return Result.success(history);
    }

    @GetMapping("/conversations")
    @RequireRole
    @Operation(summary = "获取会话列表", description = "获取当前用户的所有会话列表")
    public Result<List<AIConversationVO>> getConversations() {
        Long userId = UserContext.getUserId();
        List<AIConversationVO> conversations = aiChatService.getConversations(userId);
        return Result.success(conversations);
    }

    @DeleteMapping("/conversation/{conversationId}")
    @RequireRole
    @Operation(summary = "删除会话", description = "删除指定的会话及其所有历史记录")
    public Result<Void> deleteConversation(@PathVariable String conversationId) {
        Long userId = UserContext.getUserId();
        aiChatService.deleteConversation(userId, conversationId);
        return Result.success(null);
    }

    @DeleteMapping("/conversations/all")
    @RequireRole
    @Operation(summary = "清空所有会话", description = "清空当前用户的所有会话和聊天记录")
    public Result<Void> clearAllConversations() {
        Long userId = UserContext.getUserId();
        aiChatService.clearAllConversations(userId);
        return Result.success(null);
    }

    @PostMapping("/chat-with-document")
    @RequireRole
    @Operation(summary = "与文件对话", description = "上传PDF或图片,针对文件内容提问,AI会基于文件内容回答。文件会存储到OSS,删除会话时自动清理,流式输出")
    public Flux<String> chatWithDocument(@ModelAttribute ChatMessageDTO dto) {
        Long userId = UserContext.getUserId();
        try {
            return aiChatService.chatWithDocument(userId, dto);
        } catch (BusinessException e) {
            log.error("业务异常: {}", e.getMessage());
            return Flux.just("错误：" + e.getMessage());
        } catch (Exception e) {
            log.error("系统异常", e);
            return Flux.just("抱歉，服务暂时不可用，请稍后重试");
        }
    }
}
