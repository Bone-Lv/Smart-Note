# 游标分页测试指南

## 测试准备

### 1. 启动应用
确保应用已成功启动，并连接到数据库。

### 2. 准备测试数据
在数据库中插入足够的测试数据（建议至少100条以上）以验证分页效果。

```sql
-- 示例：插入测试笔记
INSERT INTO note (id, user_id, title, content, tags, visibility, create_time, update_time)
SELECT 
    FLOOR(1000000000000 + seq * 1000),
    1,
    CONCAT('测试笔记_', seq),
    CONCAT('这是第', seq, '条测试笔记的内容'),
    '测试',
    0,
    NOW() - INTERVAL seq DAY,
    NOW() - INTERVAL seq DAY
FROM (
    SELECT @row := @row + 1 AS seq
    FROM information_schema.columns, (SELECT @row := 0) r
    LIMIT 100
) numbers;
```

## API 测试

### 1. 笔记列表 - 传统分页

**请求：**
```http
GET /api/notes/list?page=1&pageSize=10
Authorization: Bearer YOUR_TOKEN
```

**预期响应：**
```json
{
    "code": 200,
    "message": "success",
    "data": {
        "records": [...],
        "total": 100,
        "size": 10,
        "current": 1,
        "pages": 10
    }
}
```

### 2. 笔记列表 - 游标分页（首次加载）

**请求：**
```http
GET /api/notes/list?pageSize=10
Authorization: Bearer YOUR_TOKEN
```

**预期响应：**
```json
{
    "code": 200,
    "message": "success",
    "data": {
        "records": [
            {
                "id": 1000000100000,
                "title": "测试笔记_1",
                "content": "这是第1条测试笔记的内容",
                "updateTime": "2024-01-01T12:00:00"
            }
            // ... 共10条记录
        ],
        "size": 10,
        "current": 1000000090000  // 这里存储的是nextCursor
    }
}
```

### 3. 笔记列表 - 游标分页（加载更多）

从上次响应的 `data.current` 字段获取游标值，然后发起第二次请求：

**请求：**
```http
GET /api/notes/list?pageSize=10&cursor=1000000090000
Authorization: Bearer YOUR_TOKEN
```

**预期响应：**
```json
{
    "code": 200,
    "message": "success",
    "data": {
        "records": [
            // 下一页的10条记录
        ],
        "size": 10,
        "current": 1000000080000  // 新的游标值
    }
}
```

### 4. 私聊消息历史 - 游标分页

**请求（首次）：**
```http
GET /api/messages/private/history?friendUserId=2&pageSize=20
Authorization: Bearer YOUR_TOKEN
```

**请求（加载更多）：**
```http
GET /api/messages/private/history?friendUserId=2&pageSize=20&cursor=LAST_MESSAGE_ID
Authorization: Bearer YOUR_TOKEN
```

### 5. 群聊消息历史 - 游标分页

**请求（首次）：**
```http
GET /api/messages/group/history?groupId=1&pageSize=20
Authorization: Bearer YOUR_TOKEN
```

**请求（加载更多）：**
```http
GET /api/messages/group/history?groupId=1&pageSize=20&cursor=LAST_MESSAGE_ID
Authorization: Bearer YOUR_TOKEN
```

### 6. AI聊天历史 - 游标分页

**请求（首次）：**
```http
GET /api/ai/chat/history?pageSize=20
Authorization: Bearer YOUR_TOKEN
```

**请求（加载更多）：**
```http
GET /api/ai/chat/history?pageSize=20&cursor=LAST_RECORD_ID
Authorization: Bearer YOUR_TOKEN
```

## 性能测试

### 使用 JMeter 或 Postman 进行压力测试

#### 测试场景1：深分页性能对比

**传统分页（第5000页）：**
```http
GET /api/notes/list?page=5000&pageSize=10
```

**游标分页（同等位置）：**
```http
GET /api/notes/list?pageSize=10&cursor=SOME_CURSOR_VALUE
```

**预期结果：**
- 传统分页：响应时间随页码增加而显著增长（可能达到秒级）
- 游标分页：响应时间保持稳定（毫秒级）

#### 测试场景2：并发查询

使用工具模拟100个并发用户同时查询：

**传统分页：**
- 平均响应时间：较高
- TPS：较低
- 数据库CPU：较高

**游标分页：**
- 平均响应时间：低且稳定
- TPS：高
- 数据库CPU：较低

## 前端集成测试

### Vue 3 示例

```vue
<template>
    <div class="note-list">
        <div v-for="note in notes" :key="note.id" class="note-item">
            <h3>{{ note.title }}</h3>
            <p>{{ note.content }}</p>
        </div>
        
        <button 
            v-if="hasNext" 
            @click="loadMore" 
            :disabled="loading"
        >
            {{ loading ? '加载中...' : '加载更多' }}
        </button>
        
        <p v-else>没有更多数据了</p>
    </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import axios from 'axios';

const notes = ref([]);
const nextCursor = ref(null);
const hasNext = ref(true);
const loading = ref(false);

// 加载笔记列表
async function loadNotes() {
    loading.value = true;
    try {
        const params = {
            pageSize: 10,
            ...(nextCursor.value && { cursor: nextCursor.value })
        };
        
        const res = await axios.get('/api/notes/list', { params });
        
        if (res.data.code === 200) {
            const data = res.data.data;
            notes.value.push(...data.records);
            
            // 从 current 字段获取 nextCursor
            nextCursor.value = data.current > 0 ? data.current : null;
            hasNext.value = nextCursor.value !== null;
        }
    } catch (error) {
        console.error('加载笔记失败:', error);
    } finally {
        loading.value = false;
    }
}

// 加载更多
function loadMore() {
    if (!loading.value && hasNext.value) {
        loadNotes();
    }
}

onMounted(() => {
    loadNotes();
});
</script>
```

## 验证要点

### 1. 功能验证
- ✅ 首次加载能正确返回第一页数据
- ✅ 使用游标能正确返回下一页数据
- ✅ 最后一页时 `hasNext` 为 `false`
- ✅ 数据不重复、不遗漏

### 2. 性能验证
- ✅ 深分页场景下响应时间稳定
- ✅ 数据库查询效率高（通过慢查询日志验证）
- ✅ 并发性能好

### 3. 兼容性验证
- ✅ 不使用 `cursor` 参数时，仍使用传统分页
- ✅ 现有功能不受影响
- ✅ API 响应格式保持一致

## 调试技巧

### 1. 查看生成的SQL

在 `application.yml` 中启用 MyBatis-Plus 的 SQL 日志：

```yaml
mybatis-plus:
  configuration:
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
```

**传统分页SQL：**
```sql
SELECT * FROM note WHERE user_id = ? ORDER BY update_time DESC LIMIT ?, ?
```

**游标分页SQL：**
```sql
SELECT * FROM note 
WHERE user_id = ? AND update_time < ? 
ORDER BY update_time DESC 
LIMIT 21
```

### 2. 使用 EXPLAIN 分析查询

```sql
EXPLAIN SELECT * FROM note 
WHERE user_id = 1 AND update_time < '2024-01-01 00:00:00' 
ORDER BY update_time DESC 
LIMIT 21;
```

检查：
- `type`: 应该是 `range` 或 `ref`，而不是 `ALL`
- `rows`: 扫描行数应该接近 `LIMIT` 值
- `Extra`: 不应该出现 `Using filesort`

### 3. 监控数据库性能

```sql
-- 查看慢查询
SHOW VARIABLES LIKE 'slow_query_log';
SHOW VARIABLES LIKE 'long_query_time';

-- 查看当前正在执行的查询
SHOW PROCESSLIST;
```

## 常见问题排查

### Q1: 游标分页返回空数据

**可能原因：**
- 游标值不正确
- 数据已被删除

**解决方法：**
- 检查游标值是否来自上一次响应
- 清除游标，重新从头加载

### Q2: 数据重复

**可能原因：**
- 排序字段不唯一
- 数据在查询过程中被修改

**解决方法：**
- 使用唯一字段作为游标（如主键ID）
- 接受少量重复（实时场景可接受）

### Q3: 性能没有明显提升

**可能原因：**
- 缺少合适的索引
- 数据量不够大

**解决方法：**
- 添加复合索引：`INDEX idx_user_update (user_id, update_time)`
- 增加测试数据量到10万级以上

## 总结

游标分页在以下场景表现优异：
- ✅ 大数据量（10万+记录）
- ✅ 深分页（页码 > 1000）
- ✅ 无限滚动加载
- ✅ 实时消息流

传统分页更适合：
- ✅ 小数据量
- ✅ 需要跳转到指定页
- ✅ 需要显示总页数

根据实际业务场景选择合适的分页方式。
