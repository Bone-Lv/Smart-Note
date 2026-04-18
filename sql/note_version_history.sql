-- 笔记版本管理功能 - 数据库迁移脚本
-- 执行时间: 2026-04-14

-- 1. 给 note 表添加 version 字段
ALTER TABLE note ADD COLUMN version INT DEFAULT 1 COMMENT '当前版本号';
ALTER TABLE note MODIFY COLUMN version INT NOT NULL DEFAULT 1 COMMENT '当前版本号';

-- 2. 为已存在的笔记初始化版本号为 1
UPDATE note SET version = 1 WHERE version IS NULL OR version = 0;

-- 3. 创建笔记版本历史表
CREATE TABLE IF NOT EXISTS note_version_history (
    id BIGINT PRIMARY KEY COMMENT '版本记录ID',
    note_id BIGINT NOT NULL COMMENT '笔记ID',
    user_id BIGINT NOT NULL COMMENT '操作用户ID',
    version INT NOT NULL COMMENT '版本号',
    title VARCHAR(255) DEFAULT NULL COMMENT '笔记标题',
    content TEXT COMMENT '笔记内容',
    tags VARCHAR(500) DEFAULT NULL COMMENT '标签',
    create_time DATETIME NOT NULL COMMENT '创建时间',
    INDEX idx_note_id (note_id),
    INDEX idx_note_version (note_id, version),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='笔记版本历史表';
