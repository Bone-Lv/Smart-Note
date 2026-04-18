# 即时通讯模块实现指南

本文档详细说明了智能笔记前端项目中即时通讯模块的实现细节和使用方法。

## 📋 目录

- [一、API接口规范](#一api接口规范)
- [二、WebSocket通信](#二websocket通信)
- [三、页面组件](#三页面组件)
- [四、使用示例](#四使用示例)
- [五、注意事项](#五注意事项)

---

## 一、API接口规范

### 1.1 私聊功能

#### 发送私聊消息
```javascript
// API: POST /message/private/send
// Content-Type: multipart/form-data

import { sendPrivateMessageApi } from '@/api/social.js';

// 发送文本消息
await sendPrivateMessageApi({
  receiverId: 789,
  content: '你好',
  messageType: 1
});

// 发送图片消息
const fileInput = document.querySelector('#imageInput');
const imageFile = fileInput.files[0];

await sendPrivateMessageApi({
  receiverId: 789,
  content: '',
  messageType: 2,
  imageFile: imageFile
});
```

#### 获取私聊历史（游标分页）
```javascript
// API: GET /message/private/history

import { getPrivateMessageHistoryApi } from '@/api/social.js';

// 首次加载
const response = await getPrivateMessageHistoryApi({
  friendUserId: 789,
  pageSize: 20
});

// 返回数据格式
{
  "records": [...],      // 消息列表
  "nextCursor": "xxx",   // 下一页游标
  "hasMore": true        // 是否还有更多
}

// 加载更多
const nextResponse = await getPrivateMessageHistoryApi({
  friendUserId: 789,
  cursor: response.data.data.nextCursor,
  pageSize: 20
});
```

#### 标记消息已读
```javascript
// 方式1: WebSocket（推荐）
import { useWebSocket } from '@/hooks/useWebSocket.js';

const { markRead } = useWebSocket();

// 批量标记已读
markRead({
  friendUserId: 789,
  upToMessageId: 12345  // 标记此ID之前的所有消息
});

// 方式2: HTTP（备用）
import { markAllMessagesAsReadApi } from '@/api/social.js';

await markAllMessagesAsReadApi(789, 12345);
```

#### 清空聊天记录
```javascript
import { clearPrivateChatHistoryApi } from '@/api/social.js';

await clearPrivateChatHistoryApi(789);
// 仅自己不可见，不影响对方
```

### 1.2 群聊功能

#### 创建群聊
```javascript
import { createGroupApi } from '@/api/social.js';

await createGroupApi({
  groupName: '学习小组',
  memberIds: [123, 456, 789]
});
```

#### 发送群聊消息
```javascript
import { sendGroupMessageApi } from '@/api/social.js';

// 发送文本
await sendGroupMessageApi({
  groupId: 100,
  content: '大家好',
  messageType: 1
});

// 发送图片
await sendGroupMessageApi({
  groupId: 100,
  content: '',
  messageType: 2,
  imageFile: imageFile
});
```

#### 获取群聊历史
```javascript
import { getGroupMessageHistoryApi } from '@/api/social.js';

const response = await getGroupMessageHistoryApi({
  groupId: 100,
  cursor: null,  // 首次不传
  pageSize: 20
});
```

#### 加入/退出群聊
```javascript
import { joinGroupApi, leaveGroupApi } from '@/api/social.js';

// 申请加入
await joinGroupApi(groupId);

// 退出群聊
await leaveGroupApi(groupId);
```

#### 审批入群申请
```javascript
import { approveGroupJoinApi } from '@/api/social.js';

// 同意申请
await approveGroupJoinApi({
  groupId: 100,
  applicantId: 456,
  approved: true
});

// 拒绝申请
await approveGroupJoinApi({
  groupId: 100,
  applicantId: 456,
  approved: false
});
```

### 1.3 会话列表

#### 获取会话列表
```javascript
import { getConversationListApi } from '@/api/social.js';

const response = await getConversationListApi();
// 返回私聊和群聊的混合列表，按最后消息时间排序
```

---

## 二、WebSocket通信

### 2.1 连接管理

```javascript
import { useWebSocket, WS_MESSAGE_TYPES } from '@/hooks/useWebSocket.js';

const { connect, disconnect, isConnected, on, off } = useWebSocket();

// 连接WebSocket（自动携带Cookie认证）
connect();

// 断开连接
disconnect();

// 检查连接状态
console.log(isConnected.value); // true/false
```

### 2.2 消息监听

```javascript
// 监听私聊消息
on(WS_MESSAGE_TYPES.PRIVATE_MESSAGE, (message) => {
  console.log('收到私聊消息:', message);
  
  // 添加到聊天窗口
  messages.value.push(message);
  
  // 如果不在聊天窗口，显示通知
  if (!isInChatWith(message.senderId)) {
    ElNotification({
      title: '新消息',
      message: message.content,
      type: 'info'
    });
  }
});

// 监听群聊消息
on(WS_MESSAGE_TYPES.GROUP_MESSAGE, (message) => {
  console.log('收到群聊消息:', message);
  messages.value.push(message);
});

// 监听笔记权限通知
on(WS_MESSAGE_TYPES.NOTE_PERMISSION, (message) => {
  ElNotification({
    title: '笔记共享通知',
    message: `用户${message.ownerName}与你分享了笔记《${message.noteTitle}》`,
    type: 'success'
  });
});
```

### 2.3 心跳机制

WebSocket内置心跳机制，每30秒自动发送一次ping，保持连接活跃。

```javascript
// 服务端响应pong，无需手动处理
on(WS_MESSAGE_TYPES.PONG, () => {
  // 连接正常
});
```

### 2.4 离线消息

连接成功后，会通过`offline_message_count`推送离线消息数：

```javascript
on(WS_MESSAGE_TYPES.OFFLINE_MESSAGE_COUNT, (data) => {
  console.log('离线消息数:', data.count);
});
```

---

## 三、页面组件

### 3.1 会话列表页面 (`/app/conversations`)

**文件**: `src/views/Social/ConversationList.vue`

**功能**:
- 显示所有私聊和群聊会话
- 按最后消息时间排序
- 显示未读数徽章
- 支持搜索会话
- 实时更新（WebSocket）

**使用**:
```vue
<router-link to="/app/conversations">
  <i class="fas fa-comment-dots"></i>
  <span>消息</span>
</router-link>
```

### 3.2 私聊页面 (`/app/chat/private/:friendUserId`)

**文件**: `src/views/Social/PrivateChat.vue`

**功能**:
- 文本消息发送/接收
- 图片消息发送/接收
- 游标分页加载历史消息
- 无限滚动（滚动到顶部自动加载）
- 标记已读
- 清空聊天记录
- 删除好友
- 实时消息推送

**特性**:
- 支持Enter发送、Shift+Enter换行
- 图片预览
- 文件大小限制（5MB）
- 在线状态显示
- 消息时间智能格式化

**路由参数**:
- `friendUserId`: 好友ID

**示例**:
```javascript
// 从好友列表跳转
router.push(`/app/chat/private/${friend.friendUserId}`);
```

### 3.3 群聊页面 (`/app/groups`)

**文件**: `src/views/Social/GroupChat.vue`

**功能**:
- 群聊列表展示
- 创建群聊
- 发送文本/图片消息
- 游标分页加载历史
- 成员管理
- 退出群聊
- 清空聊天记录
- 实时消息推送

**新增功能**:
- ✅ 支持游标分页
- ✅ 无限滚动加载
- ✅ WebSocket实时接收
- ✅ 自动标记已读
- ✅ 未读消息通知

---

## 四、使用示例

### 4.1 完整私聊流程

```vue
<template>
  <div class="chat-page">
    <!-- 消息列表 -->
    <div class="messages" ref="messagesContainer" @scroll="handleScroll">
      <div v-if="loadingMore" class="loading">加载中...</div>
      
      <div 
        v-for="msg in messages" 
        :key="msg.id"
        class="message"
      >
        {{ msg.content }}
      </div>
    </div>
    
    <!-- 输入框 -->
    <div class="input-area">
      <textarea 
        v-model="inputText"
        @keydown.enter.exact.prevent="sendMessage"
      ></textarea>
      <button @click="sendMessage">发送</button>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, nextTick } from 'vue';
import { useRoute } from 'vue-router';
import { useWebSocket, WS_MESSAGE_TYPES } from '@/hooks/useWebSocket.js';
import { 
  getPrivateMessageHistoryApi, 
  sendPrivateMessageApi 
} from '@/api/social.js';

const route = useRoute();
const { connect, disconnect, on, off, markRead, isConnected } = useWebSocket();

const messages = ref([]);
const inputText = ref('');
const loadingMore = ref(false);
const hasMore = ref(true);
const currentCursor = ref(null);
const messagesContainer = ref(null);

const friendUserId = route.params.friendUserId;

// 加载历史消息
const loadMessages = async (isLoadMore = false) => {
  if (isLoadMore && (!hasMore.value || loadingMore.value)) return;
  
  loadingMore.value = true;
  
  try {
    const res = await getPrivateMessageHistoryApi({
      friendUserId,
      cursor: isLoadMore ? currentCursor.value : null,
      pageSize: 20
    });
    
    const data = res.data.data;
    const newMsgs = data.records || [];
    
    if (isLoadMore) {
      messages.value = [...newMsgs, ...messages.value];
    } else {
      messages.value = newMsgs;
    }
    
    currentCursor.value = data.nextCursor;
    hasMore.value = data.hasMore;
    
    await nextTick();
    if (!isLoadMore) {
      scrollToBottom();
    }
  } finally {
    loadingMore.value = false;
  }
};

// 发送消息
const sendMessage = async () => {
  if (!inputText.value.trim()) return;
  
  try {
    await sendPrivateMessageApi({
      receiverId: friendUserId,
      content: inputText.value,
      messageType: 1
    });
    
    inputText.value = '';
    // 消息会通过WebSocket推送，无需手动添加
  } catch (error) {
    console.error('发送失败:', error);
  }
};

// 滚动处理
const handleScroll = async (e) => {
  const container = e.target;
  if (container.scrollTop < 50 && hasMore.value) {
    await loadMessages(true);
  }
};

// 滚动到底部
const scrollToBottom = () => {
  if (messagesContainer.value) {
    messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight;
  }
};

onMounted(async () => {
  // 连接WebSocket
  connect();
  
  // 加载历史消息
  await loadMessages();
  
  // 标记已读
  if (messages.value.length > 0) {
    const lastMsg = messages.value[messages.value.length - 1];
    markRead({
      friendUserId,
      upToMessageId: lastMsg.id
    });
  }
  
  // 监听新消息
  on(WS_MESSAGE_TYPES.PRIVATE_MESSAGE, (msg) => {
    if (msg.senderId == friendUserId || msg.receiverId == friendUserId) {
      messages.value.push(msg);
      scrollToBottom();
      
      // 标记已读
      markRead({
        friendUserId,
        upToMessageId: msg.id
      });
    }
  });
});

onUnmounted(() => {
  disconnect();
});
</script>
```

### 4.2 从好友列表跳转聊天

```javascript
// FriendList.vue
import { useRouter } from 'vue-router';

const router = useRouter();

const startChat = (friend) => {
  router.push(`/app/chat/private/${friend.friendUserId}`);
};
```

### 4.3 图片上传示例

```vue
<template>
  <div>
    <input 
      type="file" 
      accept="image/*" 
      @change="handleImageSelect"
      style="display: none"
      ref="imageInput"
    >
    <button @click="$refs.imageInput.click()">
      选择图片
    </button>
    
    <div v-if="previewUrl" class="preview">
      <img :src="previewUrl" alt="preview">
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import { sendPrivateMessageApi } from '@/api/social.js';

const imageInput = ref(null);
const previewUrl = ref('');
const selectedFile = ref(null);

const handleImageSelect = async (e) => {
  const file = e.target.files[0];
  if (!file) return;
  
  // 验证
  if (!file.type.startsWith('image/')) {
    alert('请选择图片文件');
    return;
  }
  
  if (file.size > 5 * 1024 * 1024) {
    alert('图片不能超过5MB');
    return;
  }
  
  selectedFile.value = file;
  
  // 生成预览
  const reader = new FileReader();
  reader.onload = (e) => {
    previewUrl.value = e.target.result;
  };
  reader.readAsDataURL(file);
  
  // 发送
  await sendPrivateMessageApi({
    receiverId: friendUserId,
    content: '',
    messageType: 2,
    imageFile: file
  });
};
</script>
```

---

## 五、注意事项

### 5.1 认证与安全

✅ **正确做法**:
- WebSocket连接时浏览器自动携带HttpOnly Cookie
- 无需在URL中传递token参数
- Axios配置`withCredentials: true`

❌ **错误做法**:
```javascript
// ❌ 不要在URL中传递token
const ws = new WebSocket(`ws://localhost:8080/ws?token=${token}`);

// ✅ 正确：直接连接，自动携带Cookie
const ws = new WebSocket('ws://localhost:8080/ws');
```

### 5.2 消息去重

由于发送消息后可能同时收到WebSocket推送，需要避免重复显示：

```javascript
// 方案1: 发送后不手动添加，等待WebSocket推送
await sendPrivateMessageApi(data);
// 不执行 messages.value.push()

// 方案2: 使用临时ID，收到WebSocket消息时替换
const tempId = Date.now();
messages.value.push({ id: tempId, ... });

on(WS_MESSAGE_TYPES.PRIVATE_MESSAGE, (msg) => {
  const index = messages.value.findIndex(m => m.id === tempId);
  if (index !== -1) {
    messages.value.splice(index, 1, msg); // 替换
  } else {
    messages.value.push(msg);
  }
});
```

### 5.3 内存泄漏防护

组件卸载时必须移除WebSocket监听器：

```javascript
onMounted(() => {
  const handler = (msg) => { /* ... */ };
  on(WS_MESSAGE_TYPES.PRIVATE_MESSAGE, handler);
  
  // 保存引用以便卸载时移除
  window._handler = handler;
});

onUnmounted(() => {
  if (window._handler) {
    off(WS_MESSAGE_TYPES.PRIVATE_MESSAGE, window._handler);
    delete window._handler;
  }
  disconnect();
});
```

### 5.4 性能优化

1. **虚拟滚动**: 大量消息时使用虚拟列表
2. **图片懒加载**: 使用`v-lazy`指令
3. **防抖节流**: 搜索、滚动加载等场景
4. **消息分页**: 每次加载20条，避免一次性加载过多

### 5.5 错误处理

```javascript
try {
  await sendPrivateMessageApi(data);
} catch (error) {
  if (error.response?.status === 401) {
    // 未授权，跳转登录
    router.push('/login');
  } else if (error.response?.status === 403) {
    ElMessage.error('没有权限发送消息');
  } else {
    ElMessage.error('发送失败，请重试');
  }
}
```

### 5.6 路由路径规范

✅ **正确路径**:
```javascript
'/app/chat/private/123'  // 带/app前缀
'/app/conversations'
'/app/groups'
```

❌ **错误路径**:
```javascript
'/chat/private/123'  // 缺少/app前缀
```

---

## 六、常见问题

### Q1: WebSocket连接失败？

**检查项**:
1. 后端服务是否启动
2. 用户是否已登录（Cookie是否存在）
3. 浏览器控制台是否有CORS错误
4. 网络防火墙是否阻止WebSocket

### Q2: 消息发送成功但未显示？

**原因**: 可能只调用了HTTP接口，未等待WebSocket推送

**解决**: 
- 方案1: 发送后等待WebSocket推送（推荐）
- 方案2: 发送后手动添加到列表（需处理去重）

### Q3: 历史消息加载不全？

**检查**:
1. 是否正确传递cursor参数
2. 是否检查hasMore标志
3. 滚动事件是否正确触发

### Q4: 未读数不准确？

**原因**: 可能在多个地方更新未读数导致冲突

**解决**: 统一在WebSocket消息处理器中更新

---

## 七、后续优化建议

1. **消息加密**: 敏感消息端到端加密
2. **消息撤回**: 支持撤回2分钟内的消息
3. **@功能**: 群聊中@特定成员
4. **表情面板**: 丰富的表情符号选择
5. **语音消息**: 支持录音发送
6. **文件传输**: 支持各种文件类型
7. **消息搜索**: 全文检索历史消息
8. **消息置顶**: 重要消息置顶显示
9. **草稿保存**: 自动保存未发送的消息
10. **多端同步**: 确保多设备消息同步

---

## 八、相关文件清单

### API层
- `src/api/social.js` - 社交和消息相关API

### Hooks
- `src/hooks/useWebSocket.js` - WebSocket封装

### 页面组件
- `src/views/Social/ConversationList.vue` - 会话列表
- `src/views/Social/PrivateChat.vue` - 私聊页面
- `src/views/Social/GroupChat.vue` - 群聊页面
- `src/views/Social/FriendList.vue` - 好友列表

### 路由
- `src/router/index.js` - 路由配置

### 布局
- `src/layouts/DefaultLayout.vue` - 默认布局（含导航）

---

**文档版本**: v1.0  
**更新日期**: 2026-04-17  
**维护者**: Smart-Note Team
