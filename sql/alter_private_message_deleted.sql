-- 修改 private_message 表的逻辑删除字段名称
-- 将 is_deleted 改为 deleted，与 MyBatis-Plus 全局配置保持一致

ALTER TABLE `private_message` 
CHANGE COLUMN `is_deleted` `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除：0-未删除，1-已删除';

-- 验证修改结果
DESC `private_message`;
