package com.gdut.common.config.ai;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.toolkit.Db;
import com.gdut.domain.entity.note.Note;
import com.gdut.domain.entity.note.NoteEmbedding;
import com.gdut.mapper.NoteEmbeddingMapper;
import com.gdut.mapper.NoteMapper;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 基于 MySQL 的 VectorStore 实现
 * 使用 DashScope Embedding 模型生成向量，存储在 MySQL 中
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JdbcVectorStore implements VectorStore {

    private final EmbeddingModel embeddingModel;
    private final NoteEmbeddingMapper noteEmbeddingMapper;
    private final NoteMapper noteMapper;

    @Override
    public void add(@NonNull List<Document> documents) {
        if (documents.isEmpty()) {
            return;
        }

        for (Document doc : documents) {
            try {
                // 1. 生成向量
                String text = doc.getText();
                if (StrUtil.isBlank(text)){
                    continue;
                }
                float[] embeddings = embeddingModel.embed(text);
                
                // 2. 转换为 JSON 字符串
                String vectorJson = JSONUtil.toJsonStr(embeddings);
                
                // 3. 提取元数据
                Long noteId = Long.parseLong(doc.getId());
                Long userId = Long.valueOf(doc.getMetadata().get("userId").toString());
                String title = doc.getMetadata().getOrDefault("title", "").toString();

                // 4. 保存到数据库（使用 Db.saveOrUpdate 避免并发问题）
                NoteEmbedding embedding = new NoteEmbedding();
                embedding.setNoteId(noteId);
                embedding.setUserId(userId);
                embedding.setVectorJson(vectorJson);
                embedding.setTitle(title);
                embedding.setCreateTime(LocalDateTime.now());
                embedding.setUpdateTime(LocalDateTime.now());
                
                // 使用 MyBatis-Plus 的 Db 工具类，内部已处理并发
                // 先尝试插入，如果主键冲突则自动转为更新
                Db.saveOrUpdate(embedding);
                log.debug("保存笔记向量（插入或更新）: noteId={}", noteId);
            } catch (Exception e) {
                log.error("添加文档向量失败: documentId={}", doc.getId(), e);
            }
        }
    }

    @Override
    public void delete(@NonNull List<String> idList) {
        if (idList.isEmpty()) {
            return;
        }

        try {
            for (String id : idList) {
                noteEmbeddingMapper.deleteById(Long.parseLong(id));
                log.debug("删除笔记向量: noteId={}", id);
            }
        } catch (Exception e) {
            log.error("删除文档向量失败", e);
        }
    }

    @Override
    public void delete(@NonNull Filter.Expression filterExpression) {
        // 基于过滤表达式删除（当前实现暂不支持复杂过滤）
        log.warn("基于过滤表达式的删除暂未实现: {}", filterExpression);
    }

    @Override
    public @NonNull List<Document> similaritySearch(@NonNull SearchRequest searchRequest) {
        try {
            // 1. 对查询文本进行向量化
            String query = searchRequest.getQuery();
            float[] queryEmbedding = embeddingModel.embed(query);
            
            // 2. 从数据库查询所有符合条件的记录（根据用户ID过滤）
            LambdaQueryWrapper<NoteEmbedding> wrapper = new LambdaQueryWrapper<>();
            
            // 解析过滤条件
            Filter.Expression filterExpression = searchRequest.getFilterExpression();
            if (filterExpression != null) {
                // 简化处理：从元数据中提取userId进行过滤
                // 实际使用中可以根据需要扩展更复杂的过滤逻辑
                String filterStr = filterExpression.toString();
                if (filterStr.contains("userId")) {
                    // 提取 userId 值，格式如: userId == '123'
                    String userIdStr = filterStr.replaceAll(".*userId\\s*==\\s*'([^']+)'.*", "$1");
                    try {
                        Long userId = Long.parseLong(userIdStr);
                        wrapper.eq(NoteEmbedding::getUserId, userId);
                    } catch (NumberFormatException e) {
                        log.warn("无法解析 userId: {}", userIdStr);
                    }
                }
            }
            wrapper.last("LIMIT 100");
            
            List<NoteEmbedding> embeddings = noteEmbeddingMapper.selectList(wrapper);
            
            if (embeddings.isEmpty()) {
                return List.of();
            }
            
            // 3. 计算余弦相似度并排序
            List<DocumentScore> scoredDocs = embeddings.stream()
                    .map(embedding -> {
            
                        float[] storedEmbedding = JSONUtil.toBean(embedding.getVectorJson(), float[].class);
                        double similarity = cosineSimilarity(queryEmbedding, storedEmbedding);
                                    
                        // 构建 Document（只存储必要的元数据）
                        Map<String, Object> metadata = new HashMap<>();
                        metadata.put("userId", embedding.getUserId()); // 保留userId用于权限校验
                                    
                        Document doc = new Document(
                                embedding.getNoteId().toString(),
                                metadata
                        );
                                    
                        return new DocumentScore(doc, similarity);
                    })
                    .filter(ds -> ds.score >= searchRequest.getSimilarityThreshold())
                    .sorted((a, b) -> Double.compare(b.score, a.score)) // 降序排列
                    .limit(searchRequest.getTopK())
                    .toList();
                        
            // 4. 批量查询笔记完整内容
            if (!scoredDocs.isEmpty()) {
                List<Long> noteIds = scoredDocs.stream()
                        .map(ds -> Long.parseLong(ds.getDocument().getId()))
                        .toList();
                            
                // 批量查询笔记
                List<Note> notes = noteMapper.selectBatchIds(noteIds);
                Map<Long, Note> noteMap = notes.stream()
                        .collect(Collectors.toMap(Note::getId, note -> note));
                            
                // 填充完整内容到 Document
                scoredDocs = scoredDocs.stream()
                        .map(ds -> {
                            Long noteId = Long.parseLong(ds.getDocument().getId());
                            Note note = noteMap.get(noteId);
                                        
                            if (note != null) {
                                // 使用标题 + 内容作为 Document 的 text
                                String fullText = note.getTitle() + "\n" + note.getContent();
                                Document updatedDoc = new Document(
                                        ds.getDocument().getId(),
                                        fullText,
                                        ds.getDocument().getMetadata()
                                );
                                return new DocumentScore(updatedDoc, ds.getScore());
                            }
                            return ds;
                        })
                        .toList();
            }
                        
            // 5. 返回排序后的文档列表
            return scoredDocs.stream()
                    .map(DocumentScore::getDocument)
                    .collect(Collectors.toList());
                    
        } catch (Exception e) {
            log.error("语义搜索失败: query={}", searchRequest.getQuery(), e);
            return List.of();
        }
    }

    /**
     * 计算余弦相似度
     */
    private double cosineSimilarity(float[] vec1, float[] vec2) {
        if (vec1.length != vec2.length) {
            return 0.0;
        }
        
        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;
        
        for (int i = 0; i < vec1.length; i++) {
            dotProduct += vec1[i] * vec2[i];
            norm1 += vec1[i] * vec1[i];
            norm2 += vec2[i] * vec2[i];
        }
        
        if (norm1 == 0.0 || norm2 == 0.0) {
            return 0.0;
        }
        
        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }

    /**
     * 内部类：文档和相似度分数
     */
    @Getter
    private static class DocumentScore {
        private final Document document;
        private final double score;
        
        public DocumentScore(Document document, double score) {
            this.document = document;
            this.score = score;
        }

    }
}
