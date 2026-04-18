-- 笔记批注表
CREATE TABLE `note_annotation` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `note_id` BIGINT NOT NULL COMMENT '笔记ID',
    `user_id` BIGINT NOT NULL COMMENT '批注用户ID',
    `content` TEXT NOT NULL COMMENT '批注内容',
    `target_content` TEXT NOT NULL COMMENT '被批注的目标内容',
    `start_position` INT NOT NULL COMMENT '目标内容在全文中的起始位置（字符索引）',
    `end_position` INT NOT NULL COMMENT '目标内容在全文中的结束位置（字符索引）',
    `note_version` INT NOT NULL COMMENT '批注创建时的笔记版本号',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    INDEX `idx_note_id` (`note_id`),
    INDEX `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='笔记批注表';
