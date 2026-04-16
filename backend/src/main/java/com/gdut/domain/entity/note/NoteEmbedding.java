package com.gdut.domain.entity.note;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 笔记向量存储实体类
 * 用于存储笔记的向量表示，实现语义搜索
 */
@Data
@TableName("note_embedding")
@AllArgsConstructor
@NoArgsConstructor
public class NoteEmbedding {
    
    /**
     * 笔记ID（主键，与note表关联）
     */
    private Long noteId;
    
    /**
     * 用户ID（用于权限过滤）
     */
    private Long userId;
    
    /**
     * 向量数据（JSON数组格式，1024维）
     */
    @TableField("vector_json")
    private String vectorJson;
    
    /**
     * 笔记标题（冗余存储，方便查询）
     */
    private String title;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
