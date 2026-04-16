package com.gdut.service;

import org.springframework.ai.document.Document;

import java.util.List;

/**
 * VectorStore 服务：处理笔记的向量化存储和语义搜索
 */
public interface VectorStoreService {

    /**
     * 将笔记添加到向量数据库
     * @param noteId 笔记ID
     * @param title 笔记标题
     * @param content 笔记内容
     * @param tags 笔记标签
     * @param userId 用户ID
     */
    void addNoteToVectorStore(Long noteId, String title, String content, String tags, Long userId);

    /**
     * 从向量数据库删除笔记
     * @param noteId 笔记ID
     */
    void removeNoteFromVectorStore(Long noteId);

    /**
     * 语义搜索：根据查询词找到最相似的笔记
     * @param query 搜索关键词
     * @param userId 当前用户ID（用于过滤）
     * @param topK 返回结果数量
     * @return 相似的 Document 列表
     */
    List<Document> semanticSearch(String query, Long userId, int topK);
}
