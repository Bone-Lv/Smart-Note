package com.gdut.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gdut.common.exception.BusinessException;
import com.gdut.common.enums.ResultCode;
import com.gdut.domain.dto.chat.ChatHistoryQueryDTO;
import com.gdut.domain.dto.chat.ChatMessageDTO;
import com.gdut.domain.entity.chat.ChatHistory;
import com.gdut.domain.entity.note.AiUsage;
import com.gdut.domain.vo.chat.ChatMessageVO;
import com.gdut.domain.vo.chat.AIConversationVO;
import com.gdut.domain.vo.chat.CursorPageResult;
import com.gdut.mapper.AiUsageMapper;
import com.gdut.mapper.ChatHistoryMapper;
import com.gdut.repository.ChatHistoryRepository;
import com.gdut.service.AIChatService;
import com.gdut.service.ChatHistorySaveService;
import com.gdut.service.VectorStoreService;
import com.gdut.common.util.AliyunOSSOperator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.content.Media;
import org.springframework.ai.document.Document;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.MimeType;
import reactor.core.publisher.Flux;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.gdut.constant.AppConstants.DAILY_AI_USAGE_LIMIT;

@Slf4j
@Service
@RequiredArgsConstructor
public class AIChatServiceImpl extends ServiceImpl<ChatHistoryMapper, ChatHistory> implements AIChatService {

    private final ChatClient chatClient;
    private final ChatClient sortClient;
    private final ChatClient searchClient;
    private final ChatClient multimodalChatClient;
    private final ChatMemory chatMemory;
    private final ChatHistoryRepository chatHistoryRepository;
    private final VectorStoreService vectorStoreService;
    private final AliyunOSSOperator aliyunOSSOperator;
    private final ChatHistorySaveService chatHistorySaveService;

    private final AiUsageMapper aiUsageMapper;


    @Override
    public Flux<String> sendMessage(Long userId, ChatMessageDTO messageDTO) {
        // 1. 先验证参数
        if (StrUtil.isBlank(messageDTO.getContent())) {
            return Flux.error(new BusinessException(ResultCode.BAD_REQUEST, "消息内容不能为空"));
        }

        // 2. 检查并增加每日用量（利用数据库唯一索引保证并发安全）
        incrementDailyUsage(userId);

        // 3. 如果没有传入conversationId，生成新的
        String conversationId = StrUtil.isNotBlank(messageDTO.getConversationId()) 
                ? messageDTO.getConversationId() 
                : UUID.randomUUID().toString();

        // 4. 保存用户消息到数据库（通过独立Service，事务生效）
        chatHistorySaveService.saveUserMessage(userId, conversationId, messageDTO.getContent());

        // 5. 使用 sortClient 进行意图识别
        String intent = recognizeIntent(messageDTO.getContent());
        log.info("用户意图识别结果: {}", intent);

        // 6. 根据意图分发到不同的处理逻辑，使用流式输出
        Flux<String> responseFlux;
        
        try {
            if (intent.contains("闲聊")) {
                // 普通聊天，使用 chatClient 并带上上下文记忆
                responseFlux = handleChatIntentStream(conversationId, messageDTO.getContent());
            } else if (intent.contains("笔记总结")) {
                // 笔记总结逻辑：使用拥有 Tool 能力的 chatClient
                responseFlux = handleChatIntentStream(conversationId, messageDTO.getContent());
            } else if (intent.contains("相关知识搜索")) {
                responseFlux = handleSearchIntentStream(conversationId, messageDTO.getContent(), userId);
            } else {
                // 默认走聊天逻辑
                responseFlux = handleChatIntentStream(conversationId, messageDTO.getContent());
            }
        } catch (Exception e) {
            log.error("AI调用失败，返回友好提示", e);
            return Flux.just("抱歉，AI服务暂时不可用，请稍后重试。");
        }

        // 7. 收集完整响应并保存到数据库
        StringBuilder fullResponse = new StringBuilder();
        return responseFlux
                .doOnNext(fullResponse::append)
                .doOnComplete(() -> 
                    chatHistorySaveService.saveAiMessageAsync(userId, conversationId, fullResponse.toString())
                )
                .onErrorResume(error -> {
                    log.error("AI 流式对话失败, conversationId: {}", conversationId, error);
                    String errorMsg = "抱歉，AI服务出现异常，请稍后重试";
                    chatHistorySaveService.saveAiMessageAsync(userId, conversationId, errorMsg);
                    return Flux.just(errorMsg);
                });
    }

    @Override
    public Flux<String> chatWithDocument(Long userId, ChatMessageDTO dto) {
        // 1. 检查并增加每日用量（防止滥用）
        incrementDailyUsage(userId);

        String conversationId = StrUtil.isNotBlank(dto.getConversationId()) 
                ? dto.getConversationId() 
                : UUID.randomUUID().toString();

        try {
            if (dto.getFiles() == null || dto.getFiles().isEmpty()) {
                return Flux.error(new BusinessException(ResultCode.BAD_REQUEST, "请至少上传一个文件"));
            }

            // 2. 上传文件到 OSS 并构建 Media 列表
            List<Media> medias = new ArrayList<>();
            StringBuilder fileInfoBuilder = new StringBuilder();
            
            for (var file : dto.getFiles()) {
                if (file.getContentType() == null) continue;
                
                // 上传到 OSS
                String fileName = file.getName();
                String fileUrl = aliyunOSSOperator.upload(file, fileName);
                
                // 构建 Media 对象供 AI 识别
                medias.add(new Media(MimeType.valueOf(file.getContentType()), file.getResource()));
                
                // 记录文件信息用于后续清理
                if (!fileInfoBuilder.toString().isEmpty()) fileInfoBuilder.append(",");
                fileInfoBuilder.append(String.format("{\"url\":\"%s\",\"name\":\"%s\"}", fileUrl, fileName));
            }

            // 3. 保存用户消息到数据库（通过独立Service，事务生效）
            chatHistorySaveService.saveUserMessageWithMetadata(userId, conversationId, dto.getContent(), "[" + fileInfoBuilder + "]");

            // 4. 请求多模态模型，使用流式输出
            StringBuilder fullResponse = new StringBuilder();
            return multimodalChatClient.prompt()
                    .user(p -> p.text(dto.getContent()).media(medias.toArray(new Media[0])))
                    .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                    .stream()
                    .content()
                    .doOnNext(fullResponse::append)
                    .doOnComplete(() -> 
                        chatHistorySaveService.saveAiMessageAsync(userId, conversationId, fullResponse.toString())
                    )
                    .onErrorResume(error -> {
                        log.error("文档对话失败, conversationId: {}", conversationId, error);
                        String errorMsg = "文件识别失败，请重试";
                        chatHistorySaveService.saveAiMessageAsync(userId, conversationId, errorMsg);
                        return Flux.just(errorMsg);
                    });
            
        } catch (Exception e) {
            log.error("文档对话处理失败", e);
            return Flux.error(new BusinessException(ResultCode.INTERNAL_SERVER_ERROR, "文件识别失败"));
        }
    }
        
    /**
     * 识别用户意图
     */
    private String recognizeIntent(String content) {
        try {
            String intent = sortClient.prompt()
                    .user(content)
                    .call()
                    .content();
            if (!StrUtil.isBlank(intent)) {
                return intent.trim();
            }else{
                throw new BusinessException(ResultCode.AI_SERVICE_UNAVAILABLE, "意图识别失败");
            }

        } catch (Exception e) {
            log.error("意图识别失败，降级为闲聊模式", e);
            return "闲聊";
        }

    }

    /**
     * 处理聊天意图(流式输出)
     */
    private Flux<String> handleChatIntentStream(String conversationId, String content) {
        return chatClient.prompt()
                .advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, conversationId))
                .user(content)
                .stream()
                .content();
    }

    /**
     * 处理搜索意图 - 基于用户笔记内容的RAG问答(流式输出)
     */
    private Flux<String> handleSearchIntentStream(String conversationId, String content, Long userId) {
        log.debug("开始基于笔记的语义搜索: userId={}, query={}", userId, content);
        
        // 1. 从向量数据库搜索相关笔记
        List<Document> relevantDocs = vectorStoreService.semanticSearch(content, userId, 5);
        
        if (relevantDocs.isEmpty()) {
            log.debug("未找到相关笔记");
            return Flux.just("抱歉，我在你的笔记库中没有找到与这个问题相关的内容。你可以尝试换个方式提问，或者先创建一些相关笔记。");
        }
        
        log.info("找到 {} 条相关笔记", relevantDocs.size());
        
        // 2. 构建上下文信息
        String context = buildContext(relevantDocs);
        
        // 3. 构建 Prompt
        String prompt = buildPrompt(content, context);
        
        // 4. 调用 AI 生成回答(流式输出)
        return searchClient.prompt()
                .user(prompt)
                .advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, conversationId))
                .stream()
                .content();
    }

    

    /**
     * 构建上下文信息
     */
    private String buildContext(List<Document> docs) {
        StringBuilder context = new StringBuilder();
        context.append("以下是从你的笔记库中搜索到的相关内容：\n\n");
        
        for (int i = 0; i < docs.size(); i++) {
            Document doc = docs.get(i);
            String title = (String) doc.getMetadata().getOrDefault("title", "无标题");
            String text = doc.getText();
            
            context.append(String.format("【笔记 %d】%s\n", i + 1, title));
            context.append(text);
            context.append("\n\n");
        }
        
        return context.toString();
    }
    
    /**
     * 构建 Prompt
     */
    private String buildPrompt(String query, String context) {
        return String.format("""
            你是一个智能笔记助手。请根据以下从用户笔记库中检索到的相关内容，回答用户的问题。
            
            ## 检索到的笔记内容：
            %s
            
            ## 用户问题：
            %s
            
            ## 回答要求：
            1. 严格基于上述笔记内容进行回答，不要编造不存在的信息
            2. 如果笔记内容与问题无关或信息不足，请如实告知用户
            3. 回答要简洁明了，重点突出
            4. 可以适当引用笔记中的关键信息
            5. 如果涉及多个笔记，可以综合整理后回答
            
            请开始回答：
            """, context, query);
    }

    @Override
    public CursorPageResult<ChatMessageVO> getChatHistory(Long userId, ChatHistoryQueryDTO queryDTO) {
        int pageSize = queryDTO.getPageSize() != null ? queryDTO.getPageSize() : 20;
        Long cursor = queryDTO.getCursor();
        
        LambdaQueryWrapper<ChatHistory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatHistory::getUserId, userId);
        
        // 如果指定了会话ID，只查询该会话的历史
        if (StrUtil.isNotBlank(queryDTO.getConversationId())) {
            wrapper.eq(ChatHistory::getConversationId, queryDTO.getConversationId());
        }
        
        // 游标分页：基于记录ID进行分页
        if (cursor != null) {
            wrapper.lt(ChatHistory::getId, cursor);
        }
        wrapper.orderByDesc(ChatHistory::getId);
        
        // 多取一条数据用于判断是否有下一页
        wrapper.last("LIMIT " + (pageSize + 1));
        
        List<ChatHistory> histories = list(wrapper);
        
        // 判断是否有更多数据
        boolean hasNext = histories.size() > pageSize;
        Long nextCursor = null;
        
        if (hasNext) {
            // 移除多余的一条数据
            histories = histories.subList(0, pageSize);
            // 设置下一页游标为最后一条记录的ID
            nextCursor = histories.getLast().getId();
        }
        
        // 转换为VO
        List<ChatMessageVO> voList = histories.stream()
                .map(history -> BeanUtil.copyProperties(history, ChatMessageVO.class))
                .collect(Collectors.toList());
        
        // 返回专门的游标分页结果
        return CursorPageResult.<ChatMessageVO>builder()
                .records(voList)
                .nextCursor(nextCursor)
                .hasNext(hasNext)
                .pageSize(pageSize)
                .build();
    }


    @Override
    public List<AIConversationVO> getConversations(Long userId) {
        List<String> conversationIds = chatHistoryRepository.getConversationIds(userId);
        
        return conversationIds.stream()
                .map(conversationId -> {
                    AIConversationVO vo = new AIConversationVO();
                    vo.setConversationId(conversationId);
                    
                    // 获取该会话的最后一条消息
                    List<ChatHistory> messages = chatHistoryRepository.getMessages(conversationId, 1);
                    if (!messages.isEmpty()) {
                        ChatHistory lastMessage = messages.getLast();
                        vo.setLastMessage(lastMessage.getContent());
                        vo.setLastMessageTime(lastMessage.getCreateTime());
                    }
                    
                    // 使用 MyBatis-Plus 原生方法统计数量
                    LambdaQueryWrapper<ChatHistory> countWrapper = new LambdaQueryWrapper<>();
                    countWrapper.eq(ChatHistory::getConversationId, conversationId);
                    vo.setMessageCount(Math.toIntExact(count(countWrapper)));
                    
                    return vo;
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteConversation(Long userId, String conversationId) {
        // 验证权限：确保只能删除自己的会话
        List<ChatHistory> messages = chatHistoryRepository.getMessages(conversationId, 1);
        if (!messages.isEmpty() && !messages.getFirst().getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权删除该会话");
        }
        
        // 先清理会话中上传的OSS文件
        deleteOssFilesFromConversation(conversationId);
        
        // 再删除会话记录
        chatHistoryRepository.deleteByConversationId(conversationId);
        
        // 同时清除内存中的对话历史
        chatMemory.clear(conversationId);
    }

    /**
     * 清理会话中上传的OSS文件
     */
    private void deleteOssFilesFromConversation(String conversationId) {
        try {
            List<ChatHistory> messages = chatHistoryRepository.getMessages(conversationId, 1000);
            
            for (ChatHistory message : messages) {
                String metadata = message.getMetadata();
                if (StrUtil.isBlank(metadata)) continue;

                // 简单解析 metadata 中的 URL (格式: [{"url":"...","name":"..."}])
                // 提取所有包含 aliyuncs.com 的 URL
                Pattern pattern = Pattern.compile("\"url\"\\s*:\\s*\"([^\"]+)\"");
                Matcher matcher = pattern.matcher(metadata);
                
                while (matcher.find()) {
                    String fileUrl = matcher.group(1);
                    if (fileUrl.contains("aliyuncs.com")) {
                        try {
                            String objectKey = aliyunOSSOperator.extractObjectKeyFromUrl(fileUrl);
                            aliyunOSSOperator.deleteFile(objectKey);
                            log.info("已删除会话关联的OSS文件: {}", objectKey);
                        } catch (Exception e) {
                            log.warn("删除OSS文件失败: url={}", fileUrl, e);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("清理会话OSS文件异常: conversationId={}", conversationId, e);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void clearAllConversations(Long userId) {
        List<String> conversationIds = chatHistoryRepository.getConversationIds(userId);
        
        // 先清理所有会话的OSS文件
        for (String conversationId : conversationIds) {
            deleteOssFilesFromConversation(conversationId);
        }
        
        // 再删除所有会话记录
        for (String conversationId : conversationIds) {
            chatHistoryRepository.deleteByConversationId(conversationId);
            chatMemory.clear(conversationId);
        }
    }

    /**
     * 增加每日用量并检查限制（并发安全）
     */
    private void incrementDailyUsage(Long userId) {
        LocalDate today = LocalDate.now();
        int dailyLimit = DAILY_AI_USAGE_LIMIT;

        try {
            // 1. 尝试插入今日记录（利用数据库唯一索引保证并发安全）
            AiUsage newUsage = new AiUsage();
            newUsage.setUserId(userId);
            newUsage.setUsageDate(today);
            newUsage.setUsageCount(1);
            aiUsageMapper.insert(newUsage);
        } catch (DuplicateKeyException e) {
            // 2. 如果记录已存在，执行原子性递增
            // 使用 MP 的 LambdaUpdateWrapper 实现：SET count = count + 1 WHERE count < limit
            LambdaUpdateWrapper<AiUsage> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(AiUsage::getUserId, userId)
                    .eq(AiUsage::getUsageDate, today)
                    .lt(AiUsage::getUsageCount, dailyLimit) // 关键：只有未超限时才更新
                    .setSql("usage_count = usage_count + 1"); // 原子性自增

            int updatedRows = aiUsageMapper.update(null, updateWrapper);
            
            // 如果更新行数为 0，说明已经达到或超过上限
            if (updatedRows == 0) {
                throw new BusinessException(ResultCode.AI_QUOTA_EXCEEDED, "今日 AI 对话次数已达上限（" + dailyLimit + "次），请明天再来~");
            }
        }
    }



}