-- AI 用量统计表（按天统计）
CREATE TABLE `ai_usage` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `usage_date` DATE NOT NULL COMMENT '使用日期',
    `usage_count` INT NOT NULL DEFAULT 0 COMMENT '当日使用次数',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY `uk_user_date` (`user_id`, `usage_date`) COMMENT '用户日期唯一索引',
    KEY `idx_usage_date` (`usage_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 每日用量统计表';
