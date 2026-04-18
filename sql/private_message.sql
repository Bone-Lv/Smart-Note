-- 好友私聊消息表
CREATE TABLE `private_message` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `sender_id` BIGINT NOT NULL COMMENT '发送者用户ID',
    `receiver_id` BIGINT NOT NULL COMMENT '接收者用户ID',
    `message_type` TINYINT NOT NULL DEFAULT 1 COMMENT '消息类型：1-文本，2-图片，3-文件',
    `content` TEXT NOT NULL COMMENT '消息内容（文本内容或文件URL）',
    `is_read` TINYINT NOT NULL DEFAULT 0 COMMENT '是否已读：0-未读，1-已读',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除：0-未删除，1-已删除',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
    PRIMARY KEY (`id`),
    INDEX `idx_sender_id` (`sender_id`),
    INDEX `idx_receiver_id` (`receiver_id`),
    INDEX `idx_receiver_unread` (`receiver_id`, `is_read`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='好友私聊消息表';

-- 群聊表
CREATE TABLE `chat_group` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `group_name` VARCHAR(100) NOT NULL COMMENT '群聊名称',
    `avatar` VARCHAR(500) DEFAULT NULL COMMENT '群聊头像URL',
    `owner_id` BIGINT NOT NULL COMMENT '群主用户ID',
    `member_count` INT NOT NULL DEFAULT 0 COMMENT '群成员数量',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    INDEX `idx_owner_id` (`owner_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='群聊表';

-- 群聊成员表
CREATE TABLE `chat_group_member` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `group_id` BIGINT NOT NULL COMMENT '群聊ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `role` TINYINT NOT NULL DEFAULT 0 COMMENT '角色：0-普通成员，1-管理员，2-群主',
    `nickname` VARCHAR(100) DEFAULT NULL COMMENT '群昵称',
    `join_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
    `is_removed` TINYINT NOT NULL DEFAULT 0 COMMENT '是否被移除：0-否，1-是',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_group_user` (`group_id`, `user_id`),
    INDEX `idx_group_id` (`group_id`),
    INDEX `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='群聊成员表';

-- 群聊消息表
CREATE TABLE `group_message` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `group_id` BIGINT NOT NULL COMMENT '群聊ID',
    `sender_id` BIGINT NOT NULL COMMENT '发送者用户ID',
    `message_type` TINYINT NOT NULL DEFAULT 1 COMMENT '消息类型：1-文本，2-图片，3-文件',
    `content` TEXT NOT NULL COMMENT '消息内容（文本内容或文件URL）',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
    PRIMARY KEY (`id`),
    INDEX `idx_group_id` (`group_id`),
    INDEX `idx_sender_id` (`sender_id`),
    INDEX `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='群聊消息表';
