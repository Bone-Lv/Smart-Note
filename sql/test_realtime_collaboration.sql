-- ============================================
-- 笔记实时协作编辑功能 - 测试数据准备
-- ============================================

-- 1. 检查编辑锁字段是否已添加
SELECT 
    COLUMN_NAME, 
    COLUMN_TYPE, 
    COLUMN_COMMENT 
FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_SCHEMA = DATABASE() 
  AND TABLE_NAME = 'note' 
  AND COLUMN_NAME IN ('editing_user_id', 'editing_lock_time');

-- 2. 创建测试用户（如果不存在）
INSERT INTO `user` (`id`, `username`, `password`, `email`, `role`, `create_time`) 
VALUES 
(1001, 'test_user_a', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 'userA@test.com', 'user', NOW()),
(1002, 'test_user_b', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 'userB@test.com', 'user', NOW())
ON DUPLICATE KEY UPDATE username=username;

-- 3. 创建测试笔记
INSERT INTO `note` (`id`, `user_id`, `title`, `content`, `tags`, `note_type`, `visibility`, `version`, `create_time`, `update_time`)
VALUES 
(9001, 1001, '测试协作编辑笔记', '这是初始内容...', '测试,协作', 1, 1, 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE title=title;

-- 4. 查看当前编辑锁状态
SELECT 
    id AS note_id,
    title,
    editing_user_id,
    editing_lock_time,
    version,
    CASE 
        WHEN editing_user_id IS NULL THEN '无人编辑'
        ELSE CONCAT('用户 ', editing_user_id, ' 正在编辑')
    END AS edit_status
FROM note 
WHERE id = 9001;

-- 5. 模拟获取编辑锁（手动测试SQL）
-- 注意：实际使用中应通过 API 调用
UPDATE note 
SET editing_user_id = 1001, 
    editing_lock_time = NOW() 
WHERE id = 9001 
  AND editing_user_id IS NULL;

-- 6. 模拟释放编辑锁
UPDATE note 
SET editing_user_id = NULL, 
    editing_lock_time = NULL 
WHERE id = 9001 
  AND editing_user_id = 1001;

-- 7. 查看版本历史（用于验证防抖保存不增加版本）
SELECT 
    note_id,
    version,
    title,
    LENGTH(content) AS content_length,
    create_time
FROM note_version_history 
WHERE note_id = 9001 
ORDER BY version DESC;

-- 8. 清理测试数据（测试完成后执行）
-- DELETE FROM note WHERE id = 9001;
-- DELETE FROM user WHERE id IN (1001, 1002);
