-- 好友分组表
CREATE TABLE IF NOT EXISTS `friend_group` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '分组ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `group_name` VARCHAR(50) NOT NULL COMMENT '分组名称',
    `sort_order` INT DEFAULT 0 COMMENT '排序顺序',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    INDEX `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='好友分组表';

-- 好友关系表
CREATE TABLE IF NOT EXISTS `friend` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `friend_user_id` BIGINT NOT NULL COMMENT '好友用户ID',
    `group_id` BIGINT DEFAULT NULL COMMENT '分组ID',
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0-待处理，1-已通过，2-已拒绝，3-已删除',
    `remark` VARCHAR(100) DEFAULT NULL COMMENT '备注名',
    `apply_message` VARCHAR(200) DEFAULT NULL COMMENT '申请消息',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_friend_user_id` (`friend_user_id`),
    UNIQUE KEY `uk_user_friend` (`user_id`, `friend_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='好友关系表';
