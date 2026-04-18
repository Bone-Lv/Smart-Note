-- AI 对话历史表
CREATE TABLE `chat_history` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `conversation_id` VARCHAR(64) NOT NULL COMMENT '会话ID，用于区分不同对话',
    `role` VARCHAR(20) NOT NULL COMMENT '角色：user-用户，assistant-AI助手',
    `content` TEXT NOT NULL COMMENT '消息内容',
    `metadata` JSON DEFAULT NULL COMMENT '元数据（可选，存储额外信息）',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    KEY `idx_user_conversation` (`user_id`, `conversation_id`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 对话历史记录表';
