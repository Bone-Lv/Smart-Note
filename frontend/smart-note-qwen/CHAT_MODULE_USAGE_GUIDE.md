# 即时通讯模块使用指南

## 📋 概述

即时通讯模块支持私聊和群聊功能，包括：
- 私聊：一对一实时消息
- 群聊：多人聊天室
- 会话列表：私聊和群聊混合展示
- 消息同步：WebSocket 实时推送
- 游标分页：高效加载历史消息
- 已读状态：消息已读未读管理

---

## 🔌 API 接口汇总

### 私聊功能

| 功能 | 接口 | 方法 | 说明 |
|------|------|------|------|
| 发送消息 | `/message/private/send` | POST | multipart/form-data |
| 历史记录 | `/message/private/history` | GET | 游标分页 |
| 离线消息 | `/message/private/offline` | GET | 获取离线消息 |
| 标记已读（单条） | `/message/private/read/{messageId}` | PUT | - |
| 标记已读（批量） | `/message/private/read-all/{friendUserId}` | PUT | 推荐 |
| 清空记录 | `/message/private/clear/{friendUserId}` | DELETE | - |

### 群聊功能

| 功能 | 接口 | 方法 | 说明 |
|------|------|------|------|
| 创建群聊 | `/message/group/create` | POST | groupName, memberIds |
| 发送消息 | `/message/group/send` | POST | multipart/form-data |
| 历史记录 | `/message/group/history` | GET | 游标分页 |
| 我的群聊 | `/message/group/my-groups` | GET | 群列表 |
| 群详情 | `/message/group/detail/{groupId}` | GET | - |
| 待审核申请 | `/message/group/pending/{groupId}` | GET | 群主可见 |
| 加入群聊 | `/message/group/join/{groupId}` | POST | 提交申请 |
| 审批申请 | `/message/group/approve` | POST | groupId, applicantId, approved |
| 标记已读 | `/message/group/read-all/{groupId}` | PUT | - |
| 退出群聊 | `/message/group/leave/{groupId}` | POST | - |

### 会话列表

| 功能 | 接口 | 方法 | 说明 |
|------|------|------|------|
| 获取会话列表 | `/message/conversations` | GET | 私聊+群聊混合 |

---

## 📖 使用示例

### 1. 初始化聊天模块

```vue
<script setup>
import { onMounted, onUnmounted } from 'vue';
import { useChatStore } from '@/stores/chatStore.js';

const chatStore = useChatStore();

onMounted(() => {
  // 初始化聊天 store，自动注册 WebSocket 监听器
  chatStore.init();
});

onUnmounted(() => {
  // 清理资源，移除监听器
  chatStore.destroy();
});
</script>
```

### 2. 获取会话列表

```vue
<script setup>
import { computed } from 'vue';
import { useChatStore } from '@/stores/chatStore.js';

const chatStore = useChatStore();

// 所有会话
const conversations = computed(() => chatStore.conversations);

// 私聊会话
const privateConversations = computed(() => chatStore.privateConversations);

// 群聊会话
const groupConversations = computed(() => chatStore.groupConversations);

// 总未读数
const unreadCount = computed(() => chatStore.unreadCount);

// 手动刷新会话列表
const refreshConversations = () => {
  chatStore.fetchConversations();
};
</script>

<template>
  <div class="conversation-list">
    <div class="unread-badge">未读消息: {{ unreadCount }}</div>
    
    <div v-for="conv in conversations" :key="conv.id" class="conversation-item">
      <div class="avatar">
        <img :src="conv.avatar" :alt="conv.name" />
        <span v-if="conv.unreadCount > 0" class="badge">
          {{ conv.unreadCount }}
        </span>
      </div>
      
      <div class="info">
        <div class="name">{{ conv.name }}</div>
        <div class="last-message">{{ conv.lastMessage }}</div>
      </div>
      
      <div class="time">{{ formatTime(conv.lastMessageTime) }}</div>
    </div>
  </div>
</template>
```

### 3. 打开聊天窗口

```vue
<script setup>
import { ref, watch, nextTick } from 'vue';
import { useChatStore } from '@/stores/chatStore.js';

const chatStore = useChatStore();
const messagesContainer = ref(null);

// 打开私聊
const openPrivateChat = async (friendUserId) => {
  const conversation = {
    type: 'private',
    id: friendUserId,
    targetId: friendUserId
  };
  
  chatStore.setCurrentConversation(conversation);
  await chatStore.loadMessages(conversation);
  
  // 标记为已读
  await chatStore.markAsRead(conversation);
  
  // 滚动到底部
  await nextTick();
  scrollToBottom();
};

// 打开群聊
const openGroupChat = async (groupId) => {
  const conversation = {
    type: 'group',
    id: groupId,
    groupId: groupId
  };
  
  chatStore.setCurrentConversation(conversation);
  await chatStore.loadMessages(conversation);
  
  await chatStore.markAsRead(conversation);
  await nextTick();
  scrollToBottom();
};

// 滚动到底部
const scrollToBottom = () => {
  if (messagesContainer.value) {
    messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight;
  }
};

// 监听消息列表变化，自动滚动
watch(() => chatStore.currentMessages, () => {
  nextTick(() => {
    scrollToBottom();
  });
});
</script>

<template>
  <div class="chat-window">
    <div ref="messagesContainer" class="messages-container">
      <div 
        v-for="msg in chatStore.currentMessages" 
        :key="msg.id"
        class="message-item"
        :class="{ 'my-message': msg.senderId === myUserId }"
      >
        <div class="avatar">
          <img :src="msg.senderAvatar" />
        </div>
        
        <div class="message-content">
          <div class="sender-name">{{ msg.senderName }}</div>
          <div class="message-bubble">
            {{ msg.content }}
          </div>
          <div class="message-time">{{ formatTime(msg.createTime) }}</div>
        </div>
      </div>
    </div>
  </div>
</template>
```

### 4. 加载更多历史消息（游标分页）

```vue
<script setup>
import { ref, onMounted } from 'vue';
import { useChatStore } from '@/stores/chatStore.js';

const chatStore = useChatStore();
const loadingMore = ref(false);

// 监听滚动事件
const handleScroll = async (event) => {
  const { scrollTop } = event.target;
  
  // 滚动到顶部时加载更多
  if (scrollTop < 50 && !loadingMore.value && chatStore.hasMore) {
    await loadMoreMessages();
  }
};

// 加载更多消息
const loadMoreMessages = async () => {
  try {
    loadingMore.value = true;
    
    // 记录加载前的滚动高度
    const container = event.target;
    const oldScrollHeight = container.scrollHeight;
    
    await chatStore.loadMoreMessages();
    
    // 保持滚动位置
    await nextTick();
    const newScrollHeight = container.scrollHeight;
    container.scrollTop = newScrollHeight - oldScrollHeight;
  } finally {
    loadingMore.value = false;
  }
};
</script>

<template>
  <div class="messages-container" @scroll="handleScroll">
    <!-- 加载更多提示 -->
    <div v-if="loadingMore" class="loading-more">
      <el-icon class="is-loading"><Loading /></el-icon>
      <span>加载中...</span>
    </div>
    
    <!-- 没有更多消息 -->
    <div v-if="!chatStore.hasMore && chatStore.currentMessages.length > 0" class="no-more">
      没有更多消息了
    </div>
    
    <!-- 消息列表 -->
    <div v-for="msg in chatStore.currentMessages" :key="msg.id">
      <!-- 消息内容 -->
    </div>
  </div>
</template>
```

### 5. 发送消息

```vue
<script setup>
import { ref } from 'vue';
import { useChatStore } from '@/stores/chatStore.js';
import { ElMessage } from 'element-plus';

const chatStore = useChatStore();
const messageInput = ref('');
const sending = ref(false);

// 发送文本消息
const sendTextMessage = async () => {
  if (!messageInput.value.trim()) return;
  
  try {
    sending.value = true;
    
    const conversation = chatStore.currentConversation;
    let result;
    
    if (conversation.type === 'private') {
      result = await chatStore.sendPrivateMessage(
        conversation.targetId,
        messageInput.value.trim()
      );
    } else {
      result = await chatStore.sendGroupMessage(
        conversation.groupId,
        messageInput.value.trim()
      );
    }
    
    if (result.success) {
      messageInput.value = '';
    } else {
      ElMessage.error('发送失败，请重试');
    }
  } finally {
    sending.value = false;
  }
};

// 发送图片消息
const sendImageMessage = async (file) => {
  try {
    sending.value = true;
    
    const conversation = chatStore.currentConversation;
    const formData = new FormData();
    
    if (conversation.type === 'private') {
      formData.append('receiverId', conversation.targetId);
    } else {
      formData.append('groupId', conversation.groupId);
    }
    
    formData.append('content', '');
    formData.append('messageType', 2); // 2-图片
    formData.append('imageFile', file);
    
    // 直接调用 API
    const result = await sendPrivateMessageApi(formData);
    chatStore.addMessage(result.data);
  } finally {
    sending.value = false;
  }
};

// 按 Enter 发送
const handleKeyPress = (event) => {
  if (event.key === 'Enter' && !event.shiftKey) {
    event.preventDefault();
    sendTextMessage();
  }
};
</script>

<template>
  <div class="message-input">
    <el-input
      v-model="messageInput"
      type="textarea"
      :rows="3"
      placeholder="输入消息，按 Enter 发送..."
      @keydown="handleKeyPress"
      :disabled="sending"
    />
    
    <div class="input-actions">
      <el-button :loading="sending" @click="sendTextMessage" type="primary">
        发送
      </el-button>
    </div>
  </div>
</template>
```

### 6. 群聊管理

```vue
<script setup>
import { ref } from 'vue';
import { 
  createGroupApi, 
  joinGroupApi, 
  approveGroupApplicationApi,
  leaveGroupApi,
  getPendingApplicationsApi
} from '@/api/message.js';
import { ElMessage, ElMessageBox } from 'element-plus';

// 创建群聊
const createGroup = async () => {
  try {
    const { groupName, memberIds } = await ElMessageBox.prompt('请输入群名称', '创建群聊', {
      confirmButtonText: '确定',
      cancelButtonText: '取消'
    });
    
    await createGroupApi({ groupName, memberIds });
    ElMessage.success('群聊创建成功');
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('创建失败');
    }
  }
};

// 加入群聊
const joinGroup = async (groupId) => {
  try {
    await joinGroupApi(groupId);
    ElMessage.success('已提交入群申请');
  } catch (error) {
    ElMessage.error('申请失败');
  }
};

// 审批入群申请
const approveApplication = async (groupId, applicantId, approved) => {
  try {
    await approveGroupApplicationApi({ groupId, applicantId, approved });
    ElMessage.success(approved ? '已同意' : '已拒绝');
  } catch (error) {
    ElMessage.error('操作失败');
  }
};

// 退出群聊
const leaveGroup = async (groupId) => {
  try {
    await ElMessageBox.confirm('确定要退出这个群聊吗？', '退出群聊', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    });
    
    await leaveGroupApi(groupId);
    ElMessage.success('已退出群聊');
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('退出失败');
    }
  }
};

// 查看待审核申请
const viewPendingApplications = async (groupId) => {
  try {
    const result = await getPendingApplicationsApi(groupId);
    pendingApplications.value = result.data;
  } catch (error) {
    ElMessage.error('获取申请列表失败');
  }
};
</script>
```

---

##  关键实现要点

### 1. 消息加载策略

```javascript
// ✅ 首次加载最近 20 条消息
await chatStore.loadMessages(conversation);

// ✅ 向上滚动加载更多
await chatStore.loadMoreMessages();

// ✅ 新消息自动追加到底部
chatStore.addMessage(newMessage);
```

### 2. 发送消息流程

```javascript
// 1. 用户输入内容
// 2. 调用 API 发送
const result = await chatStore.sendPrivateMessage(receiverId, content);

// 3. 乐观更新：立即显示在界面
if (result.success) {
  // 消息已通过 addMessage 添加到列表
}

// 4. WebSocket 推送给接收方
// 5. 如果失败，显示错误提示
```

### 3. 接收消息处理

```javascript
// WebSocket 自动处理
wsService.on('private_message', (message) => {
  chatStore.handlePrivateMessage(message);
});

wsService.on('group_message', (message) => {
  chatStore.handleGroupMessage(message);
});
```

### 4. 标记已读逻辑

```javascript
// 打开聊天窗口时自动标记已读
const openChat = async (conversation) => {
  chatStore.setCurrentConversation(conversation);
  await chatStore.loadMessages(conversation);
  
  // 批量标记已读
  await chatStore.markAsRead(conversation);
};
```

### 5. 离线消息处理

```javascript
// WebSocket 连接成功后收到离线消息数
wsService.on('offline_message_count', (message) => {
  chatStore.handleOfflineMessageCount(message);
});

// 拉取离线消息
const fetchOfflineMessages = async () => {
  const result = await getOfflineMessagesApi();
  // 合并到对应会话
};
```

---

## ⚠️ 注意事项

### 1. 内存管理

```javascript
// ✅ 组件卸载时清理
onUnmounted(() => {
  chatStore.destroy();
});
```

### 2. 滚动位置管理

```javascript
// 加载更多消息时保持滚动位置
const oldScrollHeight = container.scrollHeight;
await chatStore.loadMoreMessages();
await nextTick();
container.scrollTop = newScrollHeight - oldScrollHeight;
```

### 3. 乐观更新

```javascript
// 发送消息后立即显示，无需等待后端响应
const sendOptimistic = (content) => {
  const tempMessage = {
    id: Date.now(),
    content,
    senderId: myUserId,
    pending: true // 标记为待发送
  };
  chatStore.addMessage(tempMessage);
};
```

### 4. 消息去重

```javascript
// 避免重复添加同一条消息
const addMessageUnique = (message) => {
  const exists = chatStore.currentMessages.some(m => m.id === message.id);
  if (!exists) {
    chatStore.addMessage(message);
  }
};
```

---

## 📚 相关文档

- [WebSocket 使用指南](./WEBSOCKET_USAGE_GUIDE.md)
- [全局 Loading 状态管理](./MEMORY.md#全局loading状态管理规范与最佳实践)
- [即时通讯功能规范](./MEMORY.md#即时通讯功能实现规范与最佳实践)

---

**最后更新:** 2026-04-17  
**维护者:** 前端开发团队
