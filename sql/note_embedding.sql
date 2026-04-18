-- 笔记向量存储表
-- 用于存储笔记的向量表示，实现语义搜索
CREATE TABLE `note_embedding` (
    `note_id` BIGINT NOT NULL COMMENT '笔记ID（与note表关联）',
    `user_id` BIGINT NOT NULL COMMENT '用户ID（用于权限过滤）',
    `vector_json` JSON NOT NULL COMMENT '向量数据（JSON数组格式，1024维）',
    `title` VARCHAR(255) COMMENT '笔记标题（冗余存储，方便查询）',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`note_id`),
    INDEX `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='笔记向量存储表';

-- 说明：
-- 1. vector_json 存储示例：[0.123, -0.456, 0.789, ...] 共 1024 个浮点数
-- 2. 使用 JSON 类型存储向量，MySQL 5.7+ 支持
-- 3. 搜索时使用 MySQL 数学函数计算余弦相似度
