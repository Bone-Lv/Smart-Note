# 实时协作编辑功能 - 快速开始

## 📋 前置准备

### 1. 执行数据库迁移
```bash
# 在MySQL中执行
mysql -u root -p your_database < sql/note_editing_lock.sql
```

或者直接在MySQL客户端运行 `sql/note_editing_lock.sql` 的内容。

### 2. 重启应用
确保Spring Boot应用重新启动以加载新的实体字段。

## 🚀 使用步骤

### 后端API测试（使用Postman或curl）

#### 步骤1: 获取Token
```bash
POST http://localhost:8080/user/login
Content-Type: application/json

{
  "username": "your_username",
  "password": "your_password"
}
```

从响应中获取token。

#### 步骤2: 获取编辑锁
```bash
POST http://localhost:8080/note/123/lock
Authorization: Bearer {your_token}
```

响应：
```json
{
  "code": 200,
  "data": true,
  "message": "success"
}
```

#### 步骤3: 实时同步内容
```bash
PUT http://localhost:8080/note/123/sync
Authorization: Bearer {your_token}
Content-Type: application/json

{
  "content": "实时更新的内容...",
  "title": "可选的新标题",
  "tags": "可选的标签"
}
```

#### 步骤4: 保存为新版本
```bash
PUT http://localhost:8080/note/123
Authorization: Bearer {your_token}
Content-Type: application/json

{
  "content": "最终内容",
  "version": 5,
  "saveAsNewVersion": true
}
```

#### 步骤5: 释放编辑锁
```bash
DELETE http://localhost:8080/note/123/lock
Authorization: Bearer {your_token}
```

### WebSocket测试（使用wscat或浏览器控制台）

#### 安装wscat
```bash
npm install -g wscat
```

#### 连接WebSocket
```bash
wscat -c "ws://localhost:8080/ws/chat?token=YOUR_TOKEN"
```

#### 发送消息示例

**请求编辑锁：**
```json
{"type": "note_edit_request", "noteId": 123}
```

**释放编辑锁：**
```json
{"type": "note_edit_release", "noteId": 123}
```

**同步内容：**
```json
{
  "type": "note_content_update",
  "noteId": 123,
  "content": "新内容...",
  "version": 5
}
```

## 🔍 验证功能

### 场景1: 单人编辑
1. 用户A获取编辑锁 → ✅ 成功
2. 用户A修改内容 → ✅ 实时保存
3. 用户A释放编辑锁 → ✅ 成功

### 场景2: 并发编辑
1. 用户A获取编辑锁 → ✅ 成功
2. 用户B尝试获取同一笔记的编辑锁 → ❌ 失败（提示"该笔记正在被其他用户编辑"）
3. 用户A释放编辑锁
4. 用户B再次尝试 → ✅ 成功

### 场景3: 实时广播
1. 用户A和用户B都打开同一笔记
2. 用户A获取编辑锁并修改内容
3. 用户B通过WebSocket收到 `note_content_updated` 消息
4. 用户B的界面实时更新显示新内容

### 场景4: 异常断开
1. 用户A获取编辑锁后直接关闭浏览器
2. WebSocket连接断开
3. 后端自动释放编辑锁
4. 其他在线用户收到 `edit_lock_released` 通知

## 🐛 常见问题

### Q1: 获取编辑锁失败
**原因**：
- 笔记不存在
- 没有编辑权限
- 已被其他人锁定

**解决**：检查响应中的错误信息

### Q2: WebSocket连接失败
**原因**：
- Token无效或过期
- WebSocket路径配置错误

**解决**：
- 确认Token有效
- 检查WebSocketConfig配置

### Q3: 内容不同步
**原因**：
- 前端未正确监听WebSocket消息
- 防抖时间设置过长

**解决**：
- 检查前端WebSocket消息处理逻辑
- 调整防抖时间（建议300-500ms）

## 📊 监控和调试

### 查看当前编辑锁状态
```sql
SELECT 
    id,
    title,
    editing_user_id,
    editing_lock_time,
    CASE 
        WHEN editing_user_id IS NULL THEN '无人编辑'
        ELSE CONCAT('用户 ', editing_user_id, ' 正在编辑')
    END AS status
FROM note 
WHERE editing_user_id IS NOT NULL;
```

### 查看日志
```bash
# 查看实时日志
tail -f logs/dormsys.log | grep "编辑锁"
```

关键日志：
- `用户X成功获取笔记Y的编辑锁`
- `用户X释放笔记Y的编辑锁`
- `用户X请求编辑笔记Y被拒绝，当前编辑者为Z`
- `用户X断开连接，自动释放笔记Y的编辑锁`

## 🎯 下一步

1. **前端集成**：参考 `REALTIME_COLLABORATION.md` 中的Vue示例
2. **性能优化**：根据实际需求调整防抖时间
3. **超时机制**：考虑添加编辑锁超时自动释放（可选）
4. **冲突解决**：如果多人同时手动保存，版本号冲突时提示用户

## 📚 相关文档

- [完整使用说明](REALTIME_COLLABORATION.md)
- [测试SQL脚本](sql/test_realtime_collaboration.sql)
- [数据库迁移脚本](sql/note_editing_lock.sql)
