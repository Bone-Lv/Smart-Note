# 即时通讯模块 - 快速开始

## 🚀 5分钟快速上手

### 1. 查看会话列表

访问 `/app/conversations` 或点击导航栏的"消息"图标。

```vue
<router-link to="/app/conversations">
  <i class="fas fa-comment-dots"></i>
  消息
</router-link>
```

### 2. 开始私聊

从好友列表点击好友卡片，或直接访问：

```javascript
// 方式1: 从好友列表
router.push(`/app/chat/private/${friendUserId}`);

// 方式2: 直接访问
// http://localhost:5173/app/chat/private/123
```

### 3. 发送消息

**文本消息**:
```javascript
import { sendPrivateMessageApi } from '@/api/social.js';

await sendPrivateMessageApi({
  receiverId: 789,
  content: '你好！',
  messageType: 1
});
```

**图片消息**:
```javascript
const file = document.querySelector('#imageInput').files[0];

await sendPrivateMessageApi({
  receiverId: 789,
  content: '',
  messageType: 2,
  imageFile: file
});
```

### 4. 接收实时消息

```javascript
import { useWebSocket, WS_MESSAGE_TYPES } from '@/hooks/useWebSocket.js';

const { on } = useWebSocket();

on(WS_MESSAGE_TYPES.PRIVATE_MESSAGE, (message) => {
  console.log('收到消息:', message.content);
  // 添加到消息列表
  messages.value.push(message);
});
```

### 5. 标记已读

```javascript
const { markRead } = useWebSocket();

// 批量标记
markRead({
  friendUserId: 789,
  upToMessageId: lastMessageId
});
```

---

## 📱 核心功能速览

| 功能 | API/方法 | 说明 |
|------|---------|------|
| 发送私聊 | `sendPrivateMessageApi()` | 支持文本和图片 |
| 发送群聊 | `sendGroupMessageApi()` | 支持文本和图片 |
| 加载历史 | `getPrivateMessageHistoryApi()` | 游标分页 |
| 标记已读 | `markRead()` | WebSocket方式 |
| 清空记录 | `clearPrivateChatHistoryApi()` | 仅自己不可见 |
| 创建群聊 | `createGroupApi()` | 指定成员列表 |
| 加入群聊 | `joinGroupApi()` | 提交申请 |
| 退出群聊 | `leaveGroupApi()` | 立即生效 |

---

## ⚡ 常用代码片段

### 私聊页面模板

```vue
<template>
  <div class="chat">
    <div class="messages" @scroll="handleScroll">
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
import { ref, onMounted } from 'vue';
import { useRoute } from 'vue-router';
import { useWebSocket, WS_MESSAGE_TYPES } from '@/hooks/useWebSocket.js';
import { getPrivateMessageHistoryApi, sendPrivateMessageApi } from '@/api/social.js';

const route = useRoute();
const { connect, on, markRead } = useWebSocket();

const messages = ref([]);
const input = ref('');
const friendUserId = route.params.friendUserId;

const loadMessages = async () => {
  const res = await getPrivateMessageHistoryApi({
    friendUserId,
    pageSize: 20
  });
  messages.value = res.data.data.records;
};

const send = async () => {
  await sendPrivateMessageApi({
    receiverId: friendUserId,
    content: input.value,
    messageType: 1
  });
  input.value = '';
};

const handleScroll = (e) => {
  if (e.target.scrollTop < 50) {
    // 加载更多...
  }
};

onMounted(() => {
  connect();
  loadMessages();
  
  on(WS_MESSAGE_TYPES.PRIVATE_MESSAGE, (msg) => {
    messages.value.push(msg);
  });
});
</script>
```

---

## 🔧 调试技巧

### 1. 检查WebSocket连接状态

```javascript
const { isConnected } = useWebSocket();
console.log('连接状态:', isConnected.value);
```

### 2. 查看所有WebSocket消息

在浏览器控制台：

```javascript
// 打开useWebSocket.js，在onmessage中添加日志
ws.value.onmessage = (event) => {
  console.log('📨 收到消息:', JSON.parse(event.data));
};
```

### 3. 模拟发送消息

```javascript
// 在浏览器控制台
const { send } = useWebSocket();
send('ping'); // 测试心跳
```

### 4. 检查API响应

```javascript
const res = await getPrivateMessageHistoryApi({ friendUserId: 123 });
console.log('返回数据:', res.data);
console.log('消息数:', res.data.data.records.length);
console.log('是否有更多:', res.data.data.hasMore);
console.log('下一页游标:', res.data.data.nextCursor);
```

---

## ❗ 常见错误

### 错误1: 路由跳转404

**原因**: 路径缺少 `/app` 前缀

**解决**:
```javascript
// ❌ 错误
router.push('/chat/private/123');

// ✅ 正确
router.push('/app/chat/private/123');
```

### 错误2: WebSocket未连接就发送消息

**解决**:
```javascript
const { isConnected, send } = useWebSocket();

if (isConnected.value) {
  send('mark_read', { friendUserId: 123 });
} else {
  console.warn('WebSocket未连接');
}
```

### 错误3: 消息重复显示

**原因**: 发送后手动添加 + WebSocket推送

**解决**: 只等待WebSocket推送，不手动添加

### 错误4: 图片上传失败

**检查**:
1. 文件大小是否超过5MB
2. 文件类型是否正确
3. FormData是否正确构造

---

## 📚 更多信息

详细文档请查看: [IM_IMPLEMENTATION_GUIDE.md](./IM_IMPLEMENTATION_GUIDE.md)

---

**祝你使用愉快！** 🎉
