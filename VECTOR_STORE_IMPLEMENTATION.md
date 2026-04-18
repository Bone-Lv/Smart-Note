# 笔记向量存储与语义搜索功能说明

## 功能概述

本功能实现了将每篇笔记的内容自动存储到基于MySQL的向量数据库中，并支持用户通过自然语言进行语义搜索，系统会自动返回与查询最相关的笔记。

## 技术架构

### 1. 核心组件

- **JdbcVectorStore**: 基于MySQL实现的VectorStore，替代了原有的Redis VectorStore
- **DashScope Embedding**: 使用阿里云通义千问的text-embedding-v3模型生成1024维向量
- **NoteEmbedding表**: 存储笔记的向量表示和相关元数据

### 2. 数据流程

```
创建/更新笔记 
    ↓
调用 DashScope Embedding API 生成向量
    ↓
存储到 MySQL note_embedding 表
    ↓
用户发起语义搜索
    ↓
对查询文本生成向量
    ↓
计算余弦相似度并排序
    ↓
返回最相似的TopK笔记
```

## 数据库设计

### note_embedding 表结构

```sql
CREATE TABLE `note_embedding` (
    `note_id` BIGINT NOT NULL COMMENT '笔记ID（与note表关联）',
    `user_id` BIGINT NOT NULL COMMENT '用户ID（用于权限过滤）',
    `vector_json` JSON NOT NULL COMMENT '向量数据（JSON数组格式，1024维）',
    `title` VARCHAR(255) COMMENT '笔记标题（冗余存储，方便查询）',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`note_id`),
    INDEX `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='笔记向量存储表';
```

**说明：**
- `vector_json` 存储1024维的浮点数数组，例如：`[0.123, -0.456, 0.789, ...]`
- 使用JSON类型存储，MySQL 5.7+ 支持
- 搜索时通过Java代码计算余弦相似度

## 核心实现

### 1. JdbcVectorStore (向量存储实现)

位置: `com.gdut.common.config.ai.JdbcVectorStore`

主要功能：
- **add()**: 将文档向量化并存储到MySQL
- **delete()**: 从MySQL删除指定的向量记录
- **similaritySearch()**: 执行语义搜索，返回相似度最高的文档

关键算法：
```java
// 余弦相似度计算
cosineSimilarity = (A·B) / (||A|| × ||B||)
```

### 2. 自动同步机制

在 `NoteServiceImpl` 中实现了自动同步：

- **创建笔记** (createNote): 保存笔记后自动添加到向量库
- **更新笔记** (updateNote): 修改笔记后重新向量化并更新
- **删除笔记** (deleteNote): 删除笔记时同步删除向量记录

### 3. 语义搜索接口

**接口地址**: `GET /note/semantic-search`

**请求参数**:
- `query`: 搜索关键词（支持自然语言）
- `topK`: 返回结果数量（默认5）

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 1,
      "title": "Java学习笔记",
      "content": "...",
      "tags": "java,编程",
      "canEdit": true,
      ...
    }
  ]
}
```

## 使用示例

### 1. 创建笔记（自动向量化）

```bash
POST /note
Content-Type: application/json
Authorization: Bearer <token>

{
  "title": "Spring Boot学习笔记",
  "content": "Spring Boot是一个快速开发框架...",
  "tags": "spring,java"
}
```

### 2. 语义搜索

```bash
GET /note/semantic-search?query=如何配置数据库连接&topK=5
Authorization: Bearer <token>
```

即使用户输入的是"怎么连数据库"这样的自然语言，也能找到相关的"数据库配置"笔记。

### 3. 更新笔记（自动重新向量化）

```bash
PUT /note/1
Content-Type: application/json
Authorization: Bearer <token>

{
  "title": "Spring Boot完整教程",
  "content": "更新后的内容...",
  "tags": "spring,java,tutorial"
}
```

## 配置说明

### application.yml

```yaml
spring:
  ai:
    dashscope:
      api-key: ${DASHSCOPE_API_KEY}  # 阿里云DashScope API Key
      embedding:
        options:
          model: text-embedding-v3    # 嵌入模型
          dimensions: 1024            # 向量维度
```

### 环境变量

需要在 `.env` 文件中配置：
```
DASHSCOPE_API_KEY=your_api_key_here
DB_HOST=localhost
DB_PORT=3306
DB_NAME=your_database
DB_USERNAME=root
DB_PASSWORD=your_password
```

## 性能优化建议

1. **索引优化**: `note_embedding` 表已为 `user_id` 建立索引，加速用户级别的过滤查询

2. **批量操作**: 当前实现是逐条处理，如需导入大量历史笔记，建议添加批量导入接口

3. **缓存策略**: 对于热门搜索词，可以缓存搜索结果

4. **相似度阈值**: 当前设置为0.5，可根据实际需求调整（范围0-1，越高越严格）

## 注意事项

1. **API调用成本**: 每次创建/更新笔记都会调用DashScope Embedding API，会产生费用

2. **向量一致性**: 删除笔记时会同步删除向量记录，确保数据一致性

3. **权限控制**: 语义搜索只会返回当前用户有权限查看的笔记（通过userId过滤）

4. **并发安全**: 向量更新使用了insertOrUpdate逻辑，保证并发场景下的数据一致性

## 测试验证

### 1. 执行建表脚本

```bash
mysql -u root -p your_database < sql/note_embedding.sql
```

### 2. 启动应用

```bash
mvn spring-boot:run
```

### 3. 测试流程

1. 创建几篇测试笔记
2. 调用语义搜索接口，使用不同的自然语言查询
3. 验证返回结果的相关性
4. 更新某篇笔记，再次搜索验证向量是否更新
5. 删除笔记，验证向量记录是否被清理

## 后续优化方向

1. **增量更新**: 只重新向量化变化的部分，减少API调用
2. **多字段加权**: 标题、内容、标签赋予不同权重
3. **混合搜索**: 结合向量搜索和关键词搜索（BM25）
4. **搜索结果解释**: 返回相似度分数，帮助用户理解排序原因
5. **异步处理**: 将向量化操作改为异步，提升响应速度
