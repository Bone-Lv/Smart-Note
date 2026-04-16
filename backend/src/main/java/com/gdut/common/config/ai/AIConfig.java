package com.gdut.common.config.ai;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.gdut.common.util.AITools;
import com.gdut.constant.AppConstants;
import com.gdut.repository.ChatHistoryRepository;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.gdut.constant.AppConstants.*;

/**
 * AI 相关配置类
 * VectorStore 使用基于 MySQL 的 JdbcVectorStore 实现（自动扫描注入）
 */
@Configuration
public class AIConfig {

    @Bean
    public ChatMemory chatMemory(ChatHistoryRepository chatHistoryRepository) {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(new DatabaseChatMemoryRepository(chatHistoryRepository))
                .maxMessages(20)
                .build();
    }

    @Bean
    public ChatClient analysisClient(DashScopeChatModel model) {
        return ChatClient
                .builder(model)
                .defaultSystem(AppConstants.AI_NOTE_ANALYSIS_PROMPT)
                .defaultAdvisors(new SimpleLoggerAdvisor())
                .build();
    }

    @Bean
    public ChatClient sortClient(DashScopeChatModel model, ChatMemory chatMemory) {
        return ChatClient
                .builder(model)
                .defaultSystem(AppConstants.AI_SORT_PROMPT)
                .defaultAdvisors(
                        new SimpleLoggerAdvisor(),
                        MessageChatMemoryAdvisor.builder(chatMemory).build()
                )
                .build();
    }

    @Bean
    public ChatClient summaryClient(DashScopeChatModel model, ChatMemory chatMemory, AITools aiTools) {
        return ChatClient
                .builder(model)
                .defaultSystem("""
                        你是一个智能笔记助手。你可以使用以下工具：
                        1. `readNote`: 用于搜索和读取笔记内容。
                        2. `writeNote`: 用于将内容追加到笔记末尾。
                        
                        **工作流规则（必须严格遵守）：**
                        - 当用户要求总结笔记时：
                          1. 先调用 `readNote` 获取笔记内容。
                          2. 生成精炼的总结。
                          3. **暂停**，向用户展示总结，并询问："是否将此总结保存到笔记末尾？"
                          4. **严禁**在用户明确回复"是/保存/确认"之前调用 `writeNote`。
                          5. 只有在收到用户的保存指令后，才调用 `writeNote` 将总结追加到原文末尾。
                        """)
                .defaultAdvisors(
                        new SimpleLoggerAdvisor(),
                        MessageChatMemoryAdvisor.builder(chatMemory).build()
                )
                .defaultTools(aiTools)
                .build();
    }

    @Bean
    public ChatClient searchClient(DashScopeChatModel model, ChatMemory chatMemory,AITools aitools) {
        return ChatClient
                .builder(model)
                .defaultSystem(AI_SEARCH_PROMPT)
                .defaultAdvisors(
                        new SimpleLoggerAdvisor(),
                        MessageChatMemoryAdvisor.builder(chatMemory).build()
                )
                .build();
    }

    @Bean
    public ChatClient chatClient(DashScopeChatModel model, ChatMemory chatMemory) {
        return ChatClient
                .builder(model)
                .defaultSystem("你是一个人闲聊助手，专门负责用来接收并理解用户发过来的信息并给予答复，要求礼貌且脾气温和")
                .defaultAdvisors(
                        new SimpleLoggerAdvisor(),
                        MessageChatMemoryAdvisor.builder(chatMemory).build()
                )
                .build();
    }

    /**
     * 多模态 ChatClient（用于PDF和图片识别）
     */
    @Bean
    public ChatClient multimodalChatClient(DashScopeChatModel dashScopeChatModel, ChatMemory chatMemory) {
        return ChatClient
                .builder(dashScopeChatModel)
                .defaultOptions(ChatOptions.builder().model("qwen3.5-omni-flash").build())
                .defaultSystem(AI_DOCUMENT_PROMPT)
                .defaultAdvisors(
                        new SimpleLoggerAdvisor(),
                        MessageChatMemoryAdvisor.builder(chatMemory).build()
                )
                .build();
    }
}
