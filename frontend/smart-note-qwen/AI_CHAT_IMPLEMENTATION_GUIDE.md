# AI对话模块实现指南

本文档详细说明了智能笔记前端项目中AI对话模块的实现细节和使用方法。

## 📋 目录

- [一、API接口规范](#一api接口规范)
- [二、SSE流式输出](#二sse流式输出)
- [三、功能特性](#三功能特性)
- [四、使用示例](#四使用示例)
- [五、注意事项](#五注意事项)

---

## 一、API接口规范

### 1.1 发送消息（SSE流式）

**接口**: `POST /ai/chat/message`

**请求体**:
```json
{
  "content": "你好",
  "conversationId": "", // 可选，不传则创建新会话
  "files": [] // 可选，已上传文件的URL列表
}
```

**响应**: Server-Sent Events (SSE) 流式输出

**前端调用**:
```javascript
import { sendChatMessageApi } from '@/api/ai.js';

const response = await sendChatMessageApi({
  content: '你好',
  conversationId: 'conv_123'
});

// 处理SSE流
const reader = response.body.getReader();
const decoder = new TextDecoder();

let aiResponse = '';
while (true) {
  const { done, value } = await reader.read();
  if (done) break;
  
  const chunk = decoder.decode(value, { stream: true });
  const lines = chunk.split('\n');
  
  for (const line of lines) {
    if (line.startsWith('data: ')) {
      const data = line.slice(6);
      if (data !== '[DONE]') {
        aiResponse += data;
        // 实时更新UI
        updateUI(aiResponse);
      }
    }
  }
}
```

### 1.2 与文件对话

**接口**: `POST /ai/chat/chat-with-document`

**Content-Type**: `multipart/form-data`

**参数**:
- `content`: 问题内容
- `files`: PDF或图片文件（可多个）
- `conversationId`: 会话ID（可选）

**前端调用**:
```javascript
import { chatWithDocumentApi } from '@/api/ai.js';

const formData = new FormData();
formData.append('content', '这个PDF讲了什么？');
formData.append('conversationId', 'conv_123');

// 添加文件
files.forEach(file => {
  formData.append('files', file);
});

const response = await chatWithDocumentApi(formData);

// 同样使用SSE流式处理
const reader = response.body.getReader();
// ... 同上
```

**说明**: 
- 文件会自动上传到OSS
- AI基于文件内容回答
- 删除会话时自动清理关联文件

### 1.3 查询聊天历史（游标分页）

**接口**: `GET /ai/chat/history`

**查询参数**:
- `conversationId`: 会话ID（可选，不传则查询所有会话）
- `cursor`: 游标（可选）
- `pageSize`: 每页数量（默认20）

**返回**:
```json
{
  "records": [...],
  "nextCursor": "xxx",
  "hasMore": true
}
```

**前端调用**:
```javascript
import { getChatHistoryApi } from '@/api/ai.js';

// 首次加载
const response = await getChatHistoryApi({
  conversationId: 'conv_123',
  pageSize: 20
});

// 加载更多
const nextResponse = await getChatHistoryApi({
  conversationId: 'conv_123',
  cursor: response.data.data.nextCursor,
  pageSize: 20
});
```

### 1.4 会话管理

#### 获取会话列表
```javascript
import { getConversationsApi } from '@/api/ai.js';

const response = await getConversationsApi();
// 返回会话列表（会话ID、最后一条消息、消息数等）
```

#### 删除会话
```javascript
import { deleteConversationApi } from '@/api/ai.js';

await deleteConversationApi(conversationId);
// 同时删除该会话的所有历史记录和关联文件
```

#### 清空所有会话
```javascript
import { clearAllConversationsApi } from '@/api/ai.js';

await clearAllConversationsApi();
```

---

## 二、SSE流式输出

### 2.1 什么是SSE？

Server-Sent Events (SSE) 是一种服务器推送技术，允许服务器向客户端实时推送数据。在AI对话场景中，用于逐字显示AI的回复，提供更好的用户体验。

### 2.2 SSE数据格式

服务器返回的数据格式：
```
data: 你
data: 好
data: ！
data: 我
data: 是
data: A
data: I
data: 助
data: 手
data: 。
data: [DONE]
```

### 2.3 前端处理流程

```javascript
// 1. 发起请求
const response = await fetch('/ai/chat/message', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  credentials: 'include', // 携带Cookie
  body: JSON.stringify({ content: '你好' })
});

// 2. 获取读取器
const reader = response.body.getReader();
const decoder = new TextDecoder();

// 3. 循环读取
let aiResponse = '';
while (true) {
  const { done, value } = await reader.read();
  if (done) break;
  
  // 4. 解码数据块
  const chunk = decoder.decode(value, { stream: true });
  const lines = chunk.split('\n');
  
  // 5. 解析每一行
  for (const line of lines) {
    if (line.startsWith('data: ')) {
      const data = line.slice(6);
      if (data !== '[DONE]') {
        aiResponse += data;
        // 6. 实时更新UI
        updateLastMessage(aiResponse);
      }
    }
  }
}
```

### 2.4 优势

✅ **实时反馈**: 用户可以看到AI正在生成回复  
✅ **减少等待**: 不需要等待完整回复才显示  
✅ **流畅体验**: 类似打字机的效果  
✅ **节省带宽**: 无需轮询  

---

## 三、功能特性

### 3.1 核心功能

| 功能 | 说明 | 状态 |
|------|------|------|
| 文本对话 | 发送文本消息，AI流式回复 | ✅ |
| 文件问答 | 上传PDF/图片，AI基于文件回答 | ✅ |
| 会话管理 | 创建、删除、清空会话 | ✅ |
| 历史记录 | 游标分页加载历史消息 | ✅ |
| 无限滚动 | 滚动到顶部自动加载更多 | ✅ |
| Markdown渲染 | AI回复支持Markdown格式 | ✅ |
| 多文件上传 | 一次可上传多个文件 | ✅ |

### 3.2 文件支持

**支持的类型**:
- PDF文档（`.pdf`）
- 图片（`.jpg`, `.jpeg`, `.png`, `.gif`）

**限制**:
- 单个文件最大10MB
- 可一次上传多个文件

**使用场景**:
- PDF文档分析和总结
- 图片内容识别和描述
- 基于文档的问答

### 3.3 会话管理

**会话列表显示**:
- 会话ID
- 最后一条消息预览
- 最后消息时间
- 消息总数

**操作**:
- 点击会话进入对话
- 删除单个会话（同时删除历史和文件）
- 清空所有会话
- 创建新会话

---

## 四、使用示例

### 4.1 基本文本对话

```vue
<template>
  <div class="chat">
    <div class="messages">
      <div v-for="msg in messages" :key="msg.id">
        {{ msg.content }}
      </div>
    </div>
    
    <textarea 
      v-model="input"
      @keydown.enter.exact.prevent="send"
    ></textarea>
    <button @click="send">发送</button>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import { useAiStore } from '@/stores/aiStore.js';

const aiStore = useAiStore();
const input = ref('');

const send = async () => {
  if (!input.value.trim()) return;
  
  const result = await aiStore.sendMessage(input.value);
  
  if (result.success) {
    input.value = '';
  } else {
    console.error('发送失败:', result.error);
  }
};
</script>
```

### 4.2 文件问答

```vue
<template>
  <div class="chat">
    <!-- 文件选择 -->
    <input 
      type="file" 
      accept=".pdf,image/*" 
      multiple
      @change="handleFileSelect"
    >
    
    <!-- 文件预览 -->
    <div v-if="selectedFiles.length > 0">
      <div v-for="(file, index) in selectedFiles" :key="index">
        {{ file.name }}
        <button @click="removeFile(index)">×</button>
      </div>
    </div>
    
    <!-- 输入框 -->
    <textarea v-model="input"></textarea>
    <button @click="sendWithFiles">发送</button>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import { useAiStore } from '@/stores/aiStore.js';

const aiStore = useAiStore();
const input = ref('');
const selectedFiles = ref([]);

const handleFileSelect = (event) => {
  selectedFiles.value = Array.from(event.target.files);
};

const removeFile = (index) => {
  selectedFiles.value.splice(index, 1);
};

const sendWithFiles = async () => {
  if (!input.value.trim() && selectedFiles.value.length === 0) return;
  
  const result = await aiStore.sendMessageWithFiles(
    input.value, 
    selectedFiles.value
  );
  
  if (result.success) {
    input.value = '';
    selectedFiles.value = [];
  }
};
</script>
```

### 4.3 加载历史消息

```vue
<template>
  <div class="messages-container" @scroll="handleScroll">
    <div v-if="loadingMore">加载中...</div>
    
    <div v-for="msg in messages" :key="msg.id">
      {{ msg.content }}
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { useAiStore } from '@/stores/aiStore.js';

const aiStore = useAiStore();
const loadingMore = ref(false);

const loadHistory = async () => {
  await aiStore.loadChatHistory(conversationId);
};

const handleScroll = async (event) => {
  const container = event.target;
  
  // 滚动到顶部时加载更多
  if (container.scrollTop < 50 && aiStore.hasMore) {
    loadingMore.value = true;
    await aiStore.loadMoreHistory();
    loadingMore.value = false;
  }
};

onMounted(() => {
  loadHistory();
});
</script>
```

### 4.4 会话管理

```javascript
import { useAiStore } from '@/stores/aiStore.js';

const aiStore = useAiStore();

// 加载会话列表
await aiStore.loadConversations();

// 选择会话
aiStore.setCurrentConversation(conversation);
await aiStore.loadChatHistory(conversation.conversationId);

// 删除会话
const result = await aiStore.deleteConversation(conversationId);
if (result.success) {
  console.log('删除成功');
}

// 清空所有会话
await aiStore.clearAllConversations();

// 创建新会话
aiStore.startNewConversation();
```

---

## 五、注意事项

### 5.1 认证与安全

✅ **正确做法**:
```javascript
// 使用credentials: 'include'自动携带Cookie
const response = await fetch(url, {
  method: 'POST',
  credentials: 'include',
  // ...
});
```

❌ **错误做法**:
```javascript
// ❌ 不要在Header中手动添加Token
headers: {
  'Authorization': `Bearer ${localStorage.getItem('token')}`
}
```

### 5.2 SSE流处理

**关键点**:
1. 使用 `decoder.decode(value, { stream: true })` 处理跨块字符
2. 检查 `data !== '[DONE]'` 判断是否结束
3. 实时更新UI，提供流畅体验
4. 错误处理要完善

```javascript
try {
  const response = await sendChatMessageApi(data);
  const reader = response.body.getReader();
  
  while (true) {
    const { done, value } = await reader.read();
    if (done) break;
    
    const chunk = decoder.decode(value, { stream: true });
    // ... 处理数据
  }
} catch (error) {
  console.error('SSE error:', error);
  ElMessage.error('AI回复失败');
}
```

### 5.3 文件上传

**验证要点**:
```javascript
// 1. 检查文件类型
if (!file.type.startsWith('image/') && file.type !== 'application/pdf') {
  ElMessage.warning('不支持的文件类型');
  return;
}

// 2. 检查文件大小（10MB）
if (file.size > 10 * 1024 * 1024) {
  ElMessage.warning('文件过大');
  return;
}

// 3. 使用FormData上传
const formData = new FormData();
formData.append('files', file);
```

### 5.4 游标分页

**状态管理**:
```javascript
// Store中维护
const currentCursor = ref(null);
const hasMore = ref(true);

// 加载时更新
currentCursor.value = data.nextCursor;
hasMore.value = data.hasMore;

// 加载更多时使用
await loadChatHistory(conversationId, currentCursor.value, true);
```

### 5.5 Markdown渲染

**安全渲染**:
```javascript
import MarkdownIt from 'markdown-it';

const md = new MarkdownIt({
  html: true,      // 允许HTML标签
  linkify: true,   // 自动链接
  typographer: true // 智能引号等
});

// 在模板中使用
<div v-html="md.render(content)"></div>
```

**注意**: 确保后端已对AI回复进行安全过滤，防止XSS攻击。

### 5.6 性能优化

1. **虚拟滚动**: 大量消息时使用虚拟列表
2. **防抖节流**: 滚动事件使用防抖
3. **懒加载**: 图片和文件懒加载
4. **消息缓存**: 已加载的消息缓存在Store中

### 5.7 错误处理

```javascript
try {
  const result = await aiStore.sendMessage(content);
  
  if (!result.success) {
    ElMessage.error('发送失败：' + result.error);
  }
} catch (error) {
  console.error('Unexpected error:', error);
  ElMessage.error('网络错误，请重试');
}
```

---

## 六、常见问题

### Q1: SSE流接收不完整？

**检查项**:
1. 网络连接是否稳定
2. 是否正确设置 `credentials: 'include'`
3. 后端是否正确设置CORS头
4. 浏览器是否支持SSE

### Q2: 文件上传失败？

**检查**:
1. 文件类型是否在白名单内
2. 文件大小是否超过限制
3. FormData是否正确构造
4. 不要手动设置Content-Type

### Q3: 历史消息加载重复？

**原因**: 游标未正确更新

**解决**:
```javascript
// 确保每次加载后更新游标
currentCursor.value = data.nextCursor;
hasMore.value = data.hasMore;
```

### Q4: Markdown渲染异常？

**检查**:
1. MarkdownIt是否正确初始化
2. 是否使用 `v-html` 渲染
3. CSS样式是否正确加载

---

## 七、相关文件清单

### API层
- `src/api/ai.js` - AI相关API接口

### Store
- `src/stores/aiStore.js` - AI状态管理

### 页面组件
- `src/views/AiChat/GlobalChat.vue` - 全局AI聊天组件

### 布局
- `src/layouts/DefaultLayout.vue` - 包含AI悬浮按钮

---

## 八、后续优化建议

1. **代码高亮**: 为代码块添加语法高亮（highlight.js）
2. **公式渲染**: 支持LaTeX数学公式（KaTeX）
3. **图表生成**: AI生成图表并渲染
4. **语音输入**: 支持语音转文字
5. **语音输出**: TTS朗读AI回复
6. **对话导出**: 导出对话为PDF或Markdown
7. **收藏功能**: 收藏重要的AI回复
8. **提示词模板**: 预设常用提示词
9. **上下文管理**: 自定义上下文长度
10. **模型切换**: 支持切换不同的AI模型

---

**文档版本**: v1.0  
**更新日期**: 2026-04-17  
**维护者**: Smart-Note Team
