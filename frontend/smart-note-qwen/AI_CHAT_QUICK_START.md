# AI聊天功能 - 快速开始

## 🚀 5分钟快速上手

### 1. 打开AI聊天窗口

点击右下角的"AI助手"悬浮按钮，或导航栏中的"AI助手"入口。

### 2. 发送文本消息

在输入框中输入问题，按 **Enter** 发送，**Shift+Enter** 换行。

```javascript
// 代码方式发送
import { useAiStore } from '@/stores/aiStore.js';

const aiStore = useAiStore();
await aiStore.sendMessage('你好，请介绍一下自己');
```

### 3. 上传文件进行问答

点击输入框左侧的📎图标，选择PDF或图片文件。

```javascript
// 代码方式发送带文件的消息
const files = document.querySelector('#fileInput').files;

await aiStore.sendMessageWithFiles('这个PDF讲了什么？', Array.from(files));
```

**支持的文件类型**:
- PDF文档
- 图片（JPG, PNG, GIF）

**限制**: 单个文件最大10MB

### 4. 查看历史对话

点击聊天窗口顶部的"←"返回会话列表，查看所有历史对话。

### 5. 创建新对话

在会话列表页面，点击"➕ 新对话"按钮。

```javascript
// 代码方式
aiStore.startNewConversation();
```

---

## 📱 核心功能速览

| 功能 | 操作 | 说明 |
|------|------|------|
| 发送消息 | Enter键 | 流式显示AI回复 |
| 换行 | Shift+Enter | 输入多行文本 |
| 上传文件 | 点击📎图标 | 支持PDF和图片 |
| 查看历史 | 点击←按钮 | 返回会话列表 |
| 删除会话 | 点击🗑️图标 | 同时删除历史和文件 |
| 加载更多 | 滚动到顶部 | 自动加载历史消息 |
| Markdown | 自动渲染 | AI回复支持Markdown |

---

## ⚡ 常用代码片段

### 基本对话

```vue
<script setup>
import { useAiStore } from '@/stores/aiStore.js';

const aiStore = useAiStore();

// 发送消息
const send = async () => {
  const result = await aiStore.sendMessage('你好');
  
  if (result.success) {
    console.log('发送成功');
  }
};
</script>
```

### 文件问答

```vue
<script setup>
import { ref } from 'vue';
import { useAiStore } from '@/stores/aiStore.js';

const aiStore = useAiStore();
const selectedFiles = ref([]);

const handleFileSelect = (event) => {
  selectedFiles.value = Array.from(event.target.files);
};

const sendWithFiles = async () => {
  const result = await aiStore.sendMessageWithFiles(
    '分析这个文档',
    selectedFiles.value
  );
  
  if (result.success) {
    selectedFiles.value = []; // 清空文件
  }
};
</script>

<template>
  <input 
    type="file" 
    accept=".pdf,image/*" 
    multiple
    @change="handleFileSelect"
  >
  <button @click="sendWithFiles">发送</button>
</template>
```

### 会话管理

```javascript
import { useAiStore } from '@/stores/aiStore.js';

const aiStore = useAiStore();

// 加载会话列表
await aiStore.loadConversations();

// 选择会话
const conversation = aiStore.conversations[0];
aiStore.setCurrentConversation(conversation);

// 加载该会话的历史
await aiStore.loadChatHistory(conversation.conversationId);

// 删除会话
await aiStore.deleteConversation(conversation.conversationId);

// 清空所有会话
await aiStore.clearAllConversations();
```

### 监听消息变化

```javascript
import { watch } from 'vue';
import { useAiStore } from '@/stores/aiStore.js';

const aiStore = useAiStore();

watch(() => aiStore.messages, (newMessages) => {
  console.log('消息更新:', newMessages);
  
  // 自动滚动到底部
  scrollToBottom();
}, { deep: true });
```

---

## 🔧 调试技巧

### 1. 检查SSE流

在浏览器控制台查看SSE数据：

```javascript
// 在ai.js中添加日志
const response = await fetch(url, options);
const reader = response.body.getReader();

while (true) {
  const { done, value } = await reader.read();
  if (done) break;
  
  const chunk = decoder.decode(value);
  console.log('📨 SSE chunk:', chunk); // 添加这行
}
```

### 2. 查看Store状态

```javascript
// 在浏览器控制台
import { useAiStore } from '@/stores/aiStore.js';
const aiStore = useAiStore();

console.log('当前会话:', aiStore.currentConversation);
console.log('消息列表:', aiStore.messages);
console.log('是否加载中:', aiStore.isLoading);
console.log('是否有更多:', aiStore.hasMore);
```

### 3. 模拟发送消息

```javascript
// 在浏览器控制台
const aiStore = useAiStore();
await aiStore.sendMessage('测试消息');
```

### 4. 检查API响应

```javascript
import { getChatHistoryApi } from '@/api/ai.js';

const res = await getChatHistoryApi({ conversationId: 'xxx' });
console.log('历史消息数:', res.data.data.records.length);
console.log('是否有更多:', res.data.data.hasMore);
console.log('下一页游标:', res.data.data.nextCursor);
```

---

## ❗ 常见错误

### 错误1: SSE流接收失败

**原因**: 未设置 `credentials: 'include'`

**解决**:
```javascript
// ✅ 正确
fetch(url, {
  credentials: 'include', // 携带Cookie
  // ...
});
```

### 错误2: 文件上传失败

**检查**:
1. 文件类型是否正确（PDF或图片）
2. 文件大小是否超过10MB
3. FormData是否正确构造
4. 不要手动设置Content-Type

### 错误3: 历史消息重复加载

**原因**: 游标未正确更新

**解决**:
```javascript
// 确保在loadChatHistory中更新
currentCursor.value = data.nextCursor;
hasMore.value = data.hasMore;
```

### 错误4: Markdown渲染异常

**检查**:
1. 是否安装了markdown-it
2. 是否使用v-html渲染
3. CSS样式是否正确

---

## 📊 性能优化建议

### 1. 虚拟滚动

当消息数量超过100条时，考虑使用虚拟列表：

```bash
npm install vue-virtual-scroller
```

### 2. 防抖处理

滚动事件添加防抖：

```javascript
import { debounce } from 'lodash-es';

const handleScroll = debounce(async (event) => {
  // ... 加载逻辑
}, 300);
```

### 3. 图片懒加载

```vue
<img v-lazy="imageUrl" alt="image">
```

### 4. 消息缓存

已加载的消息缓存在Store中，避免重复请求。

---

## 📚 更多信息

详细文档请查看: [AI_CHAT_IMPLEMENTATION_GUIDE.md](./AI_CHAT_IMPLEMENTATION_GUIDE.md)

---

**祝你使用愉快！** 🎉
