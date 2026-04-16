package com.gdut.service.impl;

import com.gdut.service.VectorStoreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class VectorStoreServiceImpl implements VectorStoreService {

    private final VectorStore vectorStore;

    @Override
    public void addNoteToVectorStore(Long noteId, String title, String content, String tags, Long userId) {
        try {
            // 构建 Document：text 用于向量化，metadata 用于存储结构化信息
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("noteId", noteId);
            metadata.put("userId", userId);
            metadata.put("title", title);
            metadata.put("tags", tags != null ? tags : "");

            Document document = new Document(
                    noteId.toString(), // ID
                    title + "\n" + content, // 文本内容（标题+内容一起向量化）
                    metadata
            );

            // 添加到向量数据库
            vectorStore.add(List.of(document));
            log.info("笔记已添加到向量数据库: noteId={}, title={}", noteId, title);
        } catch (Exception e) {
            log.error("添加笔记到向量数据库失败: noteId={}", noteId, e);
            // 不抛出异常，避免影响主流程
        }
    }

    @Override
    public void removeNoteFromVectorStore(Long noteId) {
        try {
            // 根据 noteId 删除
            vectorStore.delete(List.of(noteId.toString()));
            log.info("笔记已从向量数据库删除: noteId={}", noteId);
        } catch (Exception e) {
            log.error("从向量数据库删除笔记失败: noteId={}", noteId, e);
        }
    }

    @Override
    public List<Document> semanticSearch(String query, Long userId, int topK) {
        try {
            // 构建搜索请求：设置相似度阈值和过滤条件
            SearchRequest searchRequest = SearchRequest.builder()
                    .query(query)       //用户问题
                    .topK(topK)         // 返回的笔记数量
                    .similarityThreshold(0.5) // 相似度阈值（0~1），低于此值的结果会被过滤
                    .filterExpression("userId == '" + userId + "'") // 只搜索当前用户的笔记
                    .build();

            // 执行语义搜索
            List<Document> results = vectorStore.similaritySearch(searchRequest);
            log.info("语义搜索完成: query={}, 结果数量={}", query, results.size());
            return results;
        } catch (Exception e) {
            log.error("语义搜索失败: query={}", query, e);
            return List.of();
        }
    }
}

