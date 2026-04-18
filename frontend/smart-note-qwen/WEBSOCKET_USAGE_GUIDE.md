# WebSocket 使用指南

## 📋 概述

本项目使用 WebSocket 实现实时笔记协作和即时通讯功能。WebSocket 服务已封装为单例模式，全局共享一个连接。

---

## 🔌 连接管理

### 自动连接

WebSocket 连接在以下时机自动管理：

| 时机 | 行为 | 位置 |
|------|------|------|
| 用户登录成功 | 自动建立连接 | [App.vue](file://d:\code\repo\Smart-Note\frontend\smart-note-qwen\src\App.vue#L25-L29) |
| 用户退出登录 | 自动断开连接 | [userStore.js](file://d:\code\repo\Smart-Note\frontend\smart-note-qwen\src\stores\userStore.js#L71-L81) |
| 连接断开 | 自动重连（指数退避） | [websocket.js](file://d:\code\repo\Smart-Note\frontend\smart-note-qwen\src\utils\websocket.js#L150-L175) |

### 手动控制

```javascript
import wsService from '@/utils/websocket.js';

// 手动连接
wsService.connect();

// 手动断开
wsService.disconnect();

// 检查连接状态
const state = wsService.getConnectionState();
// 返回值: 'CONNECTING' | 'OPEN' | 'CLOSING' | 'CLOSED'
```

---

## 📨 消息类型

### 客户端 → 服务端

| 类型 | 说明 | 使用场景 |
|------|------|---------|
| `ping` | 心跳请求 | 自动发送，每 30 秒一次 |
| `mark_read` | 标记消息已读 | 用户查看聊天消息时 |
| `note_edit_request` | 请求编辑锁 | 打开笔记准备编辑时 |
| `note_edit_release` | 释放编辑锁 | 关闭笔记或停止编辑时 |
| `note_content_update` | 同步笔记内容 | 编辑笔记时（防抖） |
| `note_view_start` | 开始查看笔记 | 打开笔记详情页时 |
| `note_view_end` | 停止查看笔记 | 离开笔记详情页时 |

### 服务端 → 客户端

| 类型 | 说明 | 触发时机 |
|------|------|---------|
| `connected` | 连接成功 | WebSocket 连接建立 |
| `pong` | 心跳响应 | 收到 ping 后 |
| `offline_message_count` | 离线消息数 | 连接成功后推送 |
| `edit_lock_granted` | 编辑锁授予 | 请求编辑锁成功 |
| `edit_lock_denied` | 编辑锁拒绝 | 其他人正在编辑 |
| `edit_lock_released` | 编辑锁释放 | 编辑者离开 |
| `note_content_updated` | 笔记内容更新 | 其他人编辑时 |
| `private_message` | 私聊消息 | 收到私聊消息 |
| `group_message` | 群聊消息 | 收到群聊消息 |
| `note_permission` | 笔记权限变更 | 好友分享笔记 |

---

## 📖 使用示例

### 1. 基础用法 - 监听消息

```vue
<script setup>
import { onMounted, onUnmounted } from 'vue';
import wsService from '@/utils/websocket.js';

// 定义消息处理器
const handlePrivateMessage = (message) => {
  console.log('收到私聊消息:', message);
  // 处理消息...
};

// 组件挂载时注册监听
onMounted(() => {
  wsService.on('private_message', handlePrivateMessage);
});

// 组件卸载时移除监听（防止内存泄漏）
onUnmounted(() => {
  wsService.off('private_message', handlePrivateMessage);
});
</script>
```

### 2. 笔记实时协作

#### 打开笔记详情

```javascript
import wsService from '@/utils/websocket.js';

// 1. 加载笔记内容
const note = await getNoteDetailApi(noteId);

// 2. 通知后端开始查看
wsService.send('note_view_start', { noteId });

// 3. 监听其他人的编辑
wsService.on('note_content_updated', (message) => {
  if (message.noteId === noteId) {
    // 实时更新界面
    noteContent.value = message.content;
  }
});
```

#### 请求编辑权限

```javascript
// 1. 请求编辑锁
wsService.send('note_edit_request', { noteId });

// 2. 监听响应
wsService.on('edit_lock_granted', (message) => {
  if (message.noteId === noteId) {
    canEdit.value = true;
    ElMessage.success('已获得编辑权限');
  }
});

wsService.on('edit_lock_denied', (message) => {
  if (message.noteId === noteId) {
    canEdit.value = false;
    ElMessage.warning('该笔记正在被其他用户编辑');
  }
});
```

#### 编辑时同步内容（防抖）

```javascript
import { debounce } from '@/utils/debounce.js';

let debounceTimer = null;

const handleContentChange = (newContent) => {
  noteContent.value = newContent;
  
  // 清除之前的定时器
  if (debounceTimer) {
    clearTimeout(debounceTimer);
  }
  
  // 防抖 500ms 后同步
  debounceTimer = setTimeout(() => {
    syncContent(newContent);
  }, 500);
};

const syncContent = async (content) => {
  // 1. 通过 HTTP API 保存到数据库（不创建新版本）
  await syncNoteContentApi(noteId, {
    content,
    title: currentTitle.value,
    tags: currentTags.value
  });
  
  // 2. 通过 WebSocket 广播给其他在线用户
  wsService.send('note_content_update', {
    noteId,
    content,
    version: currentVersion.value
  });
};
```

#### 停止编辑

```javascript
const stopEditing = () => {
  // 1. 释放编辑锁
  wsService.send('note_edit_release', { noteId });
  canEdit.value = false;
  
  // 2. 通知停止查看
  wsService.send('note_view_end', { noteId });
};

// 组件卸载时自动释放
onUnmounted(() => {
  stopEditing();
});
```

### 3. 聊天功能

#### 发送消息已读标记

```javascript
// 批量标记已读（推荐）
wsService.send('mark_read', {
  friendUserId: 789,
  upToMessageId: 12345 // 标记此 ID 之前的所有消息
});
```

#### 接收新消息

```vue
<script setup>
import { ref, onMounted, onUnmounted } from 'vue';
import wsService from '@/utils/websocket.js';

const messages = ref([]);

const handleNewMessage = (message) => {
  // 添加到消息列表
  messages.value.push(message);
  
  // 自动滚动到底部
  scrollToBottom();
};

onMounted(() => {
  wsService.on('private_message', handleNewMessage);
});

onUnmounted(() => {
  wsService.off('private_message', handleNewMessage);
});
</script>
```

---

## 🔧 高级用法

### 自定义消息处理器

```javascript
import wsService from '@/utils/websocket.js';

// 注册自定义处理器
wsService.on('note_content_updated', (message) => {
  // 你的业务逻辑
  console.log('笔记更新:', message);
});

// 移除特定处理器
wsService.off('note_content_updated', handlerFunction);

// 移除该类型的所有处理器
wsService.off('note_content_updated');
```

### 检查连接状态

```javascript
const state = wsService.getConnectionState();

switch (state) {
  case 'OPEN':
    console.log('连接正常');
    break;
  case 'CONNECTING':
    console.log('正在连接...');
    break;
  case 'CLOSED':
    console.log('连接已关闭');
    break;
}
```

---

## ⚠️ 注意事项

### 1. 内存管理

**❌ 错误做法：**
```javascript
// 忘记移除监听器，导致内存泄漏
onMounted(() => {
  wsService.on('private_message', handleMessage);
});
// ❌ onUnmounted 中没有移除监听器
```

**✅ 正确做法：**
```javascript
onMounted(() => {
  wsService.on('private_message', handleMessage);
});

onUnmounted(() => {
  wsService.off('private_message', handleMessage); // ✅ 移除监听器
});
```

### 2. 防抖优化

编辑笔记时必须使用防抖，避免频繁发送消息：

```javascript
// ✅ 推荐：防抖 500ms
const debouncedSync = debounce(syncContent, 500);

// ❌ 不推荐：每次输入都发送
const handleInput = () => {
  wsService.send('note_content_update', { ... });
};
```

### 3. 错误处理

WebSocket 自动重连，但需要处理极端情况：

```javascript
// 监听错误事件
wsService.on('error', (message) => {
  ElMessage.error('WebSocket 连接异常');
});

// 监听断开事件
wsService.on('disconnected', (message) => {
  console.log('连接断开:', message.code);
});
```

### 4. 发送消息前检查连接

```javascript
const success = wsService.send('note_edit_request', { noteId });

if (!success) {
  ElMessage.warning('WebSocket 未连接，请稍后重试');
}
```

---

##  完整流程示例

### 笔记协作完整流程

```javascript
import { ref, onMounted, onUnmounted } from 'vue';
import wsService from '@/utils/websocket.js';
import { debounce } from '@/utils/debounce.js';

export default {
  setup() {
    const noteId = ref(123);
    const noteContent = ref('');
    const canEdit = ref(false);
    
    // 1. 打开笔记
    onMounted(async () => {
      // 加载笔记内容
      const note = await getNoteDetailApi(noteId.value);
      noteContent.value = note.content;
      
      // 通知开始查看
      wsService.send('note_view_start', { noteId: noteId.value });
      
      // 监听其他人的编辑
      wsService.on('note_content_updated', handleContentUpdate);
      
      // 监听编辑锁状态
      wsService.on('edit_lock_granted', handleLockGranted);
      wsService.on('edit_lock_denied', handleLockDenied);
    });
    
    // 2. 请求编辑
    const requestEdit = () => {
      wsService.send('note_edit_request', { noteId: noteId.value });
    };
    
    // 3. 处理编辑锁授予
    const handleLockGranted = (message) => {
      if (message.noteId === noteId.value) {
        canEdit.value = true;
        ElMessage.success('已进入编辑模式');
      }
    };
    
    // 4. 处理编辑锁拒绝
    const handleLockDenied = (message) => {
      if (message.noteId === noteId.value) {
        canEdit.value = false;
        ElMessage.warning('其他用户正在编辑');
      }
    };
    
    // 5. 内容同步（防抖）
    const syncContent = debounce((content) => {
      // HTTP API 保存
      syncNoteContentApi(noteId.value, { content });
      
      // WebSocket 广播
      wsService.send('note_content_update', {
        noteId: noteId.value,
        content,
        version: currentVersion.value
      });
    }, 500);
    
    // 6. 处理其他人的编辑
    const handleContentUpdate = (message) => {
      if (message.noteId === noteId.value && !canEdit.value) {
        noteContent.value = message.content;
      }
    };
    
    // 7. 停止编辑
    const stopEditing = () => {
      wsService.send('note_edit_release', { noteId: noteId.value });
      wsService.send('note_view_end', { noteId: noteId.value });
      canEdit.value = false;
    };
    
    // 8. 清理
    onUnmounted(() => {
      stopEditing();
      wsService.off('note_content_updated', handleContentUpdate);
      wsService.off('edit_lock_granted', handleLockGranted);
      wsService.off('edit_lock_denied', handleLockDenied);
    });
    
    return {
      noteContent,
      canEdit,
      requestEdit,
      stopEditing
    };
  }
};
```

---

## 📚 相关文档

- [HttpOnly Cookie 认证机制](./HTTPONLY_COOKIE_AUTH.md)
- [即时通讯功能规范](./MEMORY.md#即时通讯功能实现规范与最佳实践)
- [防抖节流工具函数](./MEMORY.md#防抖节流工具函数规范与应用场景)

---

**最后更新:** 2026-04-17  
**维护者:** 前端开发团队
