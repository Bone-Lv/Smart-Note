-- 为note表添加编辑锁相关字段
ALTER TABLE `note` 
ADD COLUMN `editing_user_id` BIGINT DEFAULT NULL COMMENT '当前正在编辑的用户ID（NULL表示无人编辑）',
ADD COLUMN `editing_lock_time` DATETIME DEFAULT NULL COMMENT '编辑锁获取时间';

-- 添加索引以优化查询性能
CREATE INDEX `idx_editing_user_id` ON `note`(`editing_user_id`);
