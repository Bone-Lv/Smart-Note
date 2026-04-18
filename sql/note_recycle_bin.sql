-- 回收站功能：为note表和note_folder表添加deleted_time字段
-- 用于记录进入回收站的时间，支持滑动窗口自动清理

-- 为note表添加deleted_time字段
ALTER TABLE `note` ADD COLUMN `deleted_time` DATETIME DEFAULT NULL COMMENT '进入回收站的时间（用于5分钟后自动清理）' AFTER `deleted`;
CREATE INDEX `idx_deleted_time` ON `note`(`deleted_time`);

-- 为note_folder表添加deleted_time字段
ALTER TABLE `note_folder` ADD COLUMN `deleted_time` DATETIME DEFAULT NULL COMMENT '进入回收站的时间（用于5分钟后自动清理）' AFTER `deleted`;
CREATE INDEX `idx_folder_deleted_time` ON `note_folder`(`deleted_time`);
