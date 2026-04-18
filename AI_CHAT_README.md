# AI聊天功能使用说明

## 功能概述

实现了完整的AI对话历史和记忆功能,包括:
- ✅ 基于数据库的对话历史存储
- ✅ 支持多会话管理
- ✅ 上下文记忆功能
- ✅ AI使用量统计
- ✅ 完整的RESTful API接口

## 数据库初始化

在MySQL中执行以下SQL脚本创建对话历史表:

```sql
-- 执行 sql/chat_history.sql
CREATE TABLE `chat_history` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `conversation_id` VARCHAR(64) NOT NULL COMMENT '会话ID,用于区分不同对话',
    `role` VARCHAR(20) NOT NULL COMMENT '角色:user-用户,assistant-AI助手',
    `content` TEXT NOT NULL COMMENT '消息内容',
    `metadata` JSON DEFAULT NULL COMMENT '元数据(可选,存储额外信息)',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    KEY `idx_user_conversation` (`user_id`, `conversation_id`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 对话历史记录表';
```

## API接口说明

### 1. 发送消息
**接口**: `POST /ai/chat/message`

**请求体**:
```json
{
  "content": "你好,请介绍一下自己",
  "conversationId": "可选,不传则创建新会话"
}
```

**响应**:
```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "reply": "你好!我是一个AI助手...",
    "conversationId": "xxx-xxx-xxx"
  }
}
```

### 2. 查询聊天历史
**接口**: `GET /ai/chat/history`

**参数**:
- `conversationId`: 会话ID(可选,不传则查询所有会话)
- `page`: 页码,默认1
- `pageSize`: 每页大小,默认20

**响应**:
```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "records": [
      {
        "id": 1,
        "role": "user",
        "content": "你好",
        "createTime": "2026-04-11T10:00:00"
      },
      {
        "id": 2,
        "role": "assistant",
        "content": "你好!我是AI助手",
        "createTime": "2026-04-11T10:00:01"
      }
    ],
    "total": 2,
    "current": 1,
    "size": 20
  }
}
```

### 3. 获取会话列表
**接口**: `GET /ai/chat/conversations`

**响应**:
```json
{
  "code": 200,
  "msg": "操作成功",
  "data": [
    {
      "conversationId": "xxx-xxx-xxx",
      "lastMessage": "最后一条消息内容",
      "lastMessageTime": "2026-04-11T10:00:00",
      "messageCount": 10
    }
  ]
}
```

### 4. 删除会话
**接口**: `DELETE /ai/chat/conversation/{conversationId}`

### 5. 清空所有会话
**接口**: `DELETE /ai/chat/conversations/all`

## 核心架构

### 1. 数据层
- **ChatHistory**: 对话历史实体类
- **ChatHistoryMapper**: MyBatis-Plus Mapper接口
- **SqlChatHistory**: 基于数据库的Repository实现

### 2. 配置层
- **AIConfig**: Spring AI配置类
  - 配置sortClient使用数据库存储
  - 集成DatabaseChatMemoryRepository
- **DatabaseChatMemoryRepository**: 适配器,将Spring AI的ChatMemoryRepository接口适配到数据库存储

### 3. 业务层
- **AIChatService**: 业务接口
- **AIChatServiceImpl**: 业务实现
  - 发送消息并保存历史
  - 查询聊天历史(支持分页)
  - 管理会话
  - 统计AI使用量

### 4. 控制层
- **AIChatController**: RESTful API控制器
  - 提供完整的聊天相关接口
  - 集成JWT认证(@RequireRole)
  - Swagger文档注解

## 特性说明

### 1. 上下文记忆
- 使用Spring AI的ChatMemory机制
- 自动维护最近20条消息的上下文
- 基于conversationId区分不同会话

### 2. 持久化存储
- 所有对话历史保存到MySQL数据库
- 支持按用户、按会话查询
- 应用重启后历史不丢失

### 3. 用量统计
- 自动记录每日AI使用次数
- 复用现有的ai_usage表
- 按用户、按日期统计

### 4. 安全性
- 所有接口需要JWT认证
- 用户只能访问自己的对话历史
- 删除会话时验证权限

## 使用示例

### 前端调用示例

```javascript
// 1. 发送消息
const response = await fetch('/ai/chat/message', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
    'token': 'your-jwt-token'
  },
  body: JSON.stringify({
    content: '你好',
    conversationId: '' // 第一次不传,后续使用返回的conversationId
  })
});

const data = await response.json();
const conversationId = data.data.conversationId;

// 2. 继续对话(使用同一个conversationId)
await fetch('/ai/chat/message', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
    'token': 'your-jwt-token'
  },
  body: JSON.stringify({
    content: '你能做什么?',
    conversationId: conversationId // 传入之前的会话ID
  })
});

// 3. 查询历史
await fetch(`/ai/chat/history?conversationId=${conversationId}&page=1&pageSize=20`, {
  headers: {
    'token': 'your-jwt-token'
  }
});

// 4. 获取会话列表
await fetch('/ai/chat/conversations', {
  headers: {
    'token': 'your-jwt-token'
  }
});
```

## 注意事项

1. **首次使用**: 需要先执行SQL脚本创建表
2. **会话管理**: 建议前端保存conversationId,用于维持对话上下文
3. **性能优化**: 大量历史消息时建议定期清理旧会话
4. **Token刷新**: 注意JWT Token过期时间,及时刷新

## 扩展建议

1. **流式响应**: 可以改为SSE流式输出,提升用户体验
2. **消息搜索**: 添加全文搜索功能
3. **导出功能**: 支持导出对话历史为PDF/Markdown
4. **多模型支持**: 可配置不同的AI模型用于不同场景
5. **敏感词过滤**: 添加输入内容的安全检查
