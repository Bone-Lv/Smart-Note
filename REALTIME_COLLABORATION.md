# 笔记实时协作编辑功能说明

## 功能概述

实现了基于**独占编辑锁 + 实时内容广播**的笔记协作编辑功能：
- **互斥机制**：同一时间仅允许一人获取编辑锁，其他人只读
- **实时同步**：编辑者的内容变更通过 WebSocket 实时广播给所有读者
- **防抖保存**：自动保存不递增版本号，手动保存才创建新版本

## 数据库变更

执行 `sql/note_editing_lock.sql` 添加以下字段：
```sql
ALTER TABLE `note` 
ADD COLUMN `editing_user_id` BIGINT DEFAULT NULL COMMENT '当前正在编辑的用户ID',
ADD COLUMN `editing_lock_time` DATETIME DEFAULT NULL COMMENT '编辑锁获取时间';
```

## 核心组件

### 1. ChatWebSocketHandler 扩展

新增三种 WebSocket 消息类型：

#### 1.1 请求编辑锁
```javascript
// 客户端发送
{
  "type": "note_edit_request",
  "noteId": 123456
}

// 服务端响应 - 成功
{
  "type": "edit_lock_granted",
  "noteId": 123456,
  "success": true,
  "message": "已获得编辑权限"
}

// 服务端响应 - 失败
{
  "type": "edit_lock_denied",
  "noteId": 123456,
  "success": false,
  "editorId": 789,
  "message": "该笔记正在被其他用户编辑"
}
```

#### 1.2 释放编辑锁
```javascript
// 客户端发送
{
  "type": "note_edit_release",
  "noteId": 123456
}
```

#### 1.3 实时内容同步
```javascript
// 编辑者发送（每次内容变更）
{
  "type": "note_content_update",
  "noteId": 123456,
  "content": "新的笔记内容...",
  "version": 5
}

// 其他用户接收（实时广播）
{
  "type": "note_content_updated",
  "noteId": 123456,
  "editorId": 1001,
  "content": "新的笔记内容...",
  "version": 5,
  "timestamp": 1713123456789
}
```

#### 1.4 编辑锁强制释放通知
当编辑者断开连接时，自动释放锁并通知所有人：
```javascript
{
  "type": "edit_lock_released",
  "noteId": 123456,
  "message": "编辑者已离开，现在可以编辑"
}
```

### 2. REST API 接口

#### 2.1 获取编辑锁
```http
POST /note/{noteId}/lock
Authorization: Bearer {token}

Response:
{
  "code": 200,
  "data": true  // 是否成功获取锁
}
```

#### 2.2 释放编辑锁
```http
DELETE /note/{noteId}/lock
Authorization: Bearer {token}
```

#### 2.3 实时同步内容（防抖保存）
```http
PUT /note/{noteId}/sync
Authorization: Bearer {token}
Content-Type: application/json

{
  "content": "更新的内容",
  "title": "可选的新标题",
  "tags": "可选的新标签"
}
```

**特点**：
- ✅ 不递增版本号
- ✅ 不归档历史版本
- ✅ 不进行版本冲突校验
- ✅ 后保存者直接覆盖先保存者
- ✅ 保证编辑流畅性

#### 2.4 正式保存（创建新版本）
使用原有的 `PUT /note/{noteId}` 接口，传递 `saveAsNewVersion=true`：
```http
PUT /note/{noteId}
Authorization: Bearer {token}
Content-Type: application/json

{
  "content": "最终内容",
  "title": "最终标题",
  "tags": "最终标签",
  "version": 5,  // 当前版本号，用于冲突检测
  "saveAsNewVersion": true  // 确认为新版本
}
```

**特点**：
- ✅ 递增版本号
- ✅ 归档当前版本到历史记录
- ✅ 执行严格的版本号校验
- ✅ 版本不一致时返回冲突错误

## 前端实现示例

### Vue 3 示例

```vue
<template>
  <div class="note-editor">
    <div v-if="!canEdit" class="readonly-hint">
      {{ editingUser ? `用户 ${editingUser} 正在编辑...` : '只读模式' }}
    </div>
    
    <textarea 
      v-model="noteContent"
      :disabled="!canEdit"
      @input="handleContentChange"
    />
    
    <button @click="saveAsNewVersion" :disabled="!canEdit">
      保存为新版本
    </button>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue';
import { useWebSocket } from '@/composables/useWebSocket';

const props = defineProps({
  noteId: Number
});

const noteContent = ref('');
const canEdit = ref(false);
const editingUser = ref(null);
let ws = null;
let debounceTimer = null;

// 初始化 WebSocket
onMounted(async () => {
  ws = new WebSocket(`ws://localhost:8080/ws/chat?token=${getToken()}`);
  
  ws.onopen = () => {
    console.log('WebSocket 连接成功');
    // 请求编辑锁
    requestEditLock();
  };
  
  ws.onmessage = (event) => {
    const message = JSON.parse(event.data);
    handleWebSocketMessage(message);
  };
  
  ws.onclose = () => {
    console.log('WebSocket 连接关闭');
    releaseEditLock();
  };
});

// 处理 WebSocket 消息
function handleWebSocketMessage(message) {
  switch (message.type) {
    case 'edit_lock_granted':
      if (message.success) {
        canEdit.value = true;
        console.log('获得编辑权限');
      }
      break;
      
    case 'edit_lock_denied':
      canEdit.value = false;
      editingUser.value = message.editorId;
      console.log('无法获得编辑权限:', message.message);
      break;
      
    case 'note_content_updated':
      // 接收其他人的实时更新
      if (message.editorId !== getCurrentUserId()) {
        noteContent.value = message.content;
        console.log('收到实时内容更新');
      }
      break;
      
    case 'edit_lock_released':
      if (message.noteId === props.noteId) {
        editingUser.value = null;
        console.log('编辑锁已释放，可以尝试获取');
      }
      break;
  }
}

// 请求编辑锁
function requestEditLock() {
  ws.send(JSON.stringify({
    type: 'note_edit_request',
    noteId: props.noteId
  }));
}

// 释放编辑锁
function releaseEditLock() {
  if (canEdit.value) {
    ws.send(JSON.stringify({
      type: 'note_edit_release',
      noteId: props.noteId
    }));
    canEdit.value = false;
  }
}

// 内容变更处理（防抖）
function handleContentChange() {
  if (!canEdit.value) return;
  
  // 清除之前的定时器
  clearTimeout(debounceTimer);
  
  // 设置新的定时器（500ms 防抖）
  debounceTimer = setTimeout(() => {
    syncContent();
  }, 500);
}

// 实时同步内容
async function syncContent() {
  try {
    // 通过 WebSocket 广播给其他人
    ws.send(JSON.stringify({
      type: 'note_content_update',
      noteId: props.noteId,
      content: noteContent.value,
      version: currentVersion.value
    }));
    
    // 通过 HTTP API 保存到数据库（防抖保存）
    await fetch(`/note/${props.noteId}/sync`, {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${getToken()}`
      },
      body: JSON.stringify({
        content: noteContent.value
      })
    });
  } catch (error) {
    console.error('同步内容失败:', error);
  }
}

// 保存为新版本
async function saveAsNewVersion() {
  try {
    const response = await fetch(`/note/${props.noteId}`, {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${getToken()}`
      },
      body: JSON.stringify({
        content: noteContent.value,
        version: currentVersion.value,
        saveAsNewVersion: true
      })
    });
    
    if (response.ok) {
      console.log('新版本保存成功');
      // 刷新版本号
      await loadNoteDetail();
    } else {
      const error = await response.json();
      if (error.message.includes('已被其他人修改')) {
        alert('笔记已被其他人修改，请刷新后重试');
      }
    }
  } catch (error) {
    console.error('保存失败:', error);
  }
}

// 组件卸载时清理
onUnmounted(() => {
  releaseEditLock();
  if (ws) {
    ws.close();
  }
  clearTimeout(debounceTimer);
});
</script>
```

## 工作流程

### 场景1：用户A开始编辑

1. 用户A打开笔记详情页面
2. 前端建立 WebSocket 连接
3. 前端发送 `note_edit_request` 消息
4. 后端检查无人编辑，授予编辑锁
5. 用户A进入编辑模式

### 场景2：用户B尝试编辑

1. 用户B打开同一篇笔记
2. 前端建立 WebSocket 连接
3. 前端发送 `note_edit_request` 消息
4. 后端检测到用户A正在编辑，拒绝请求
5. 用户B进入只读模式，显示"用户A正在编辑"

### 场景3：用户A编辑内容

1. 用户A输入内容
2. 前端防抖500ms后发送 `note_content_update` 消息
3. 后端验证编辑锁，广播给所有其他在线用户
4. 用户B实时看到内容更新
5. 后端调用 `syncNoteContent` 保存到数据库（不增加版本号）

### 场景4：用户A保存新版本

1. 用户A点击"保存为新版本"按钮
2. 前端调用 `PUT /note/{noteId}` 接口，传递 `saveAsNewVersion=true`
3. 后端归档当前版本到历史表
4. 后端递增版本号
5. 返回新版本号给前端

### 场景5：用户A断开连接

1. WebSocket 连接关闭
2. 后端 `afterConnectionClosed` 触发
3. 自动释放用户A持有的编辑锁
4. 广播 `edit_lock_released` 消息给所有在线用户
5. 用户B收到通知，可以尝试获取编辑锁

## 注意事项

1. **编辑锁超时**：当前实现没有超时机制，如果编辑者浏览器崩溃未正常关闭，需要等待会话超时或手动清理
2. **并发控制**：使用数据库乐观锁（`WHERE editing_user_id IS NULL`）确保同一时间只有一个编辑者
3. **性能优化**：防抖时间建议设置为 300-500ms，平衡实时性和服务器压力
4. **权限校验**：只有笔记作者或被授权为"可编辑"的好友才能获取编辑锁
5. **PDF笔记**：PDF类型的笔记不支持编辑，直接抛出异常

## 测试步骤

1. 执行 SQL 脚本添加编辑锁字段
2. 启动应用
3. 使用两个浏览器窗口，分别登录不同账号
4. 账号A打开笔记，应能成功获取编辑锁
5. 账号B打开同一笔记，应被拒绝编辑
6. 账号A修改内容，账号B应实时看到更新
7. 账号A点击保存，应创建新版本
8. 账号A关闭窗口，账号B应收到锁释放通知
