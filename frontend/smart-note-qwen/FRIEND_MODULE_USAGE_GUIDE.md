# 好友模块使用指南

## 📋 概述

好友模块提供完整的好友管理功能，包括：
- 查找并添加好友（通过邮箱或手机号）
- 好友申请管理（发送、接收、处理）
- 好友分组管理（创建、删除、移动）
- 好友信息管理（备注、删除）
- 按分组展示好友列表

---

## 🔌 API 接口汇总

| 功能 | 接口 | 方法 | 说明 |
|------|------|------|------|
| 搜索用户 | `/friend/search` | GET | account（邮箱或手机号） |
| 发送好友申请 | `/friend/request` | POST | targetUserId, message |
| 获取收到的申请 | `/friend/requests/received` | GET | - |
| 处理好友申请 | `/friend/request/handle` | PUT | requestId, approved |
| 获取好友列表 | `/friend/list` | GET | groupId（可选） |
| 获取好友分组 | `/friend/groups` | GET | - |
| 创建好友分组 | `/friend/group` | POST | groupName（query参数） |
| 删除好友分组 | `/friend/group/{groupId}` | DELETE | - |
| 更新好友备注 | `/friend/remark` | PUT | friendUserId, remark |
| 移动好友到分组 | `/friend/move` | PUT | friendUserId, groupId |
| 删除好友 | `/friend/{friendUserId}` | DELETE | - |

---

## 📖 使用示例

### 1. 初始化好友 Store

```vue
<script setup>
import { onMounted } from 'vue';
import { useFriendStore } from '@/stores/friendStore.js';

const friendStore = useFriendStore();

onMounted(async () => {
  // 加载所有数据
  await friendStore.refreshAll();
});
</script>
```

### 2. 搜索并添加好友

```vue
<script setup>
import { ref } from 'vue';
import { useFriendStore } from '@/stores/friendStore.js';
import { ElMessage } from 'element-plus';

const friendStore = useFriendStore();

const searchAccount = ref('');
const searchResult = ref(null);
const requestMessage = ref('');

// 搜索用户
const handleSearch = async () => {
  if (!searchAccount.value) {
    ElMessage.warning('请输入邮箱或手机号');
    return;
  }
  
  try {
    const user = await friendStore.searchUser(searchAccount.value);
    searchResult.value = user;
  } catch (error) {
    console.error('搜索失败:', error);
  }
};

// 发送好友申请
const sendRequest = async () => {
  try {
    await friendStore.sendFriendRequest(
      searchResult.value.id,
      requestMessage.value
    );
    
    // 清空搜索结果
    searchResult.value = null;
    searchAccount.value = '';
    requestMessage.value = '';
  } catch (error) {
    console.error('发送申请失败:', error);
  }
};
</script>

<template>
  <div class="add-friend">
    <h3>添加好友</h3>
    
    <!-- 搜索框 -->
    <el-input
      v-model="searchAccount"
      placeholder="输入邮箱或手机号"
      @keyup.enter="handleSearch"
    >
      <template #append>
        <el-button @click="handleSearch">搜索</el-button>
      </template>
    </el-input>
    
    <!-- 搜索结果 -->
    <div v-if="searchResult" class="search-result">
      <img :src="searchResult.avatar" alt="头像" />
      <div class="info">
        <p>用户名: {{ searchResult.username }}</p>
        <p>邮箱: {{ searchResult.email }}</p>
      </div>
      
      <!-- 验证消息 -->
      <el-input
        v-model="requestMessage"
        type="textarea"
        placeholder="请输入验证消息（可选）"
        :rows="3"
      />
      
      <el-button type="primary" @click="sendRequest">
        发送好友申请
      </el-button>
    </div>
  </div>
</template>
```

### 3. 显示好友列表（按分组）

```vue
<script setup>
import { computed } from 'vue';
import { useRouter } from 'vue-router';
import { useFriendStore } from '@/stores/friendStore.js';

const router = useRouter();
const friendStore = useFriendStore();

// 按分组的好友列表
const groupedFriends = computed(() => friendStore.groupedFriends);

// 选中的分组
const selectedGroupId = computed(() => friendStore.selectedGroupId);

// 切换分组
const selectGroup = (groupId) => {
  friendStore.setSelectedGroup(groupId);
};

// 发起聊天
const startChat = (friend) => {
  router.push(`/app/chat/private/${friend.id}`);
};

// 查看资料
const viewProfile = (friend) => {
  // 打开资料对话框
};
</script>

<template>
  <div class="friend-list">
    <!-- 分组列表 -->
    <div class="groups">
      <div 
        v-for="group in friendStore.groups" 
        :key="group.id"
        class="group-item"
        :class="{ active: selectedGroupId === group.id }"
        @click="selectGroup(group.id)"
      >
        <span>{{ group.name }}</span>
        <span class="count">{{ groupedFriends[group.id]?.length || 0 }}</span>
      </div>
    </div>
    
    <!-- 好友列表 -->
    <div class="friends">
      <div 
        v-for="friend in groupedFriends[selectedGroupId] || []" 
        :key="friend.id"
        class="friend-item"
        @click="startChat(friend)"
      >
        <img :src="friend.avatar" alt="头像" />
        <div class="info">
          <p class="name">{{ friend.remark || friend.username }}</p>
          <p class="status">{{ friend.online ? '在线' : '离线' }}</p>
        </div>
      </div>
    </div>
  </div>
</template>
```

### 4. 处理好友申请

```vue
<script setup>
import { computed } from 'vue';
import { useFriendStore } from '@/stores/friendStore.js';

const friendStore = useFriendStore();

// 收到的好友申请
const requests = computed(() => friendStore.receivedRequests);

// 同意申请
const handleAccept = async (requestId) => {
  try {
    await friendStore.acceptRequest(requestId);
  } catch (error) {
    console.error('处理失败:', error);
  }
};

// 拒绝申请
const handleReject = async (requestId) => {
  try {
    await friendStore.rejectRequest(requestId);
  } catch (error) {
    console.error('处理失败:', error);
  }
};
</script>

<template>
  <div class="friend-requests">
    <h3>好友申请 ({{ requests.length }})</h3>
    
    <div v-for="request in requests" :key="request.id" class="request-item">
      <img :src="request.applicantAvatar" alt="头像" />
      <div class="info">
        <p class="name">{{ request.applicantName }}</p>
        <p class="message">{{ request.message || '无验证消息' }}</p>
        <p class="time">{{ request.createTime }}</p>
      </div>
      <div class="actions">
        <el-button type="primary" size="small" @click="handleAccept(request.id)">
          同意
        </el-button>
        <el-button size="small" @click="handleReject(request.id)">
          拒绝
        </el-button>
      </div>
    </div>
    
    <el-empty v-if="requests.length === 0" description="暂无好友申请" />
  </div>
</template>
```

### 5. 管理好友分组

```vue
<script setup>
import { ref } from 'vue';
import { useFriendStore } from '@/stores/friendStore.js';
import { ElMessageBox } from 'element-plus';

const friendStore = useFriendStore();

const newGroupName = ref('');

// 创建分组
const handleCreateGroup = async () => {
  if (!newGroupName.value) {
    ElMessage.warning('请输入分组名称');
    return;
  }
  
  try {
    await friendStore.createGroup(newGroupName.value);
    newGroupName.value = '';
  } catch (error) {
    console.error('创建失败:', error);
  }
};

// 删除分组
const handleDeleteGroup = async (groupId) => {
  try {
    await friendStore.deleteGroup(groupId);
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败:', error);
    }
  }
};
</script>

<template>
  <div class="group-management">
    <h3>好友分组</h3>
    
    <!-- 创建分组 -->
    <div class="create-group">
      <el-input
        v-model="newGroupName"
        placeholder="输入分组名称"
        @keyup.enter="handleCreateGroup"
      >
        <template #append>
          <el-button @click="handleCreateGroup">创建</el-button>
        </template>
      </el-input>
    </div>
    
    <!-- 分组列表 -->
    <div class="group-list">
      <div 
        v-for="group in friendStore.groups" 
        :key="group.id"
        class="group-item"
      >
        <span>{{ group.name }}</span>
        <el-button 
          size="small" 
          type="danger"
          @click="handleDeleteGroup(group.id)"
        >
          删除
        </el-button>
      </div>
    </div>
  </div>
</template>
```

### 6. 更新好友备注

```vue
<script setup>
import { ref } from 'vue';
import { useFriendStore } from '@/stores/friendStore.js';
import { ElMessageBox } from 'element-plus';

const friendStore = useFriendStore();

const updateRemark = async (friend) => {
  try {
    const { value } = await ElMessageBox.prompt('请输入新备注', '修改备注', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      inputValue: friend.remark || ''
    });
    
    await friendStore.updateRemark(friend.id, value);
  } catch (error) {
    if (error !== 'cancel') {
      console.error('更新失败:', error);
    }
  }
};
</script>

<template>
  <el-button @click="updateRemark(friend)">
    修改备注
  </el-button>
</template>
```

### 7. 移动好友到分组

```vue
<script setup>
import { useFriendStore } from '@/stores/friendStore.js';
import { ElMessageBox } from 'element-plus';

const friendStore = useFriendStore();

const moveFriend = async (friend) => {
  try {
    // 选择目标分组
    const { value } = await ElMessageBox.prompt(
      '请输入目标分组ID',
      '移动好友',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        inputType: 'number'
      }
    );
    
    await friendStore.moveFriend(friend.id, parseInt(value));
  } catch (error) {
    if (error !== 'cancel') {
      console.error('移动失败:', error);
    }
  }
};
</script>
```

### 8. 删除好友

```vue
<script setup>
import { useFriendStore } from '@/stores/friendStore.js';

const friendStore = useFriendStore();

const handleDelete = async (friendId) => {
  try {
    await friendStore.deleteFriend(friendId);
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败:', error);
    }
  }
};
</script>

<template>
  <el-button type="danger" @click="handleDelete(friend.id)">
    删除好友
  </el-button>
</template>
```

---

## 🔧 关键实现要点

### 1. 搜索并添加好友流程

```javascript
// 1. 输入邮箱或手机号
const account = 'user@example.com';

// 2. 调用搜索 API
const user = await friendStore.searchUser(account);

// 3. 显示用户信息
// 4. 用户点击"添加好友"
// 5. 填写验证消息（可选）
// 6. 发送好友申请
await friendStore.sendFriendRequest(user.id, '你好，我想加你为好友');
```

### 2. 处理好友申请流程

```javascript
// 同意申请
await friendStore.acceptRequest(requestId);
// 自动刷新好友列表

// 拒绝申请
await friendStore.rejectRequest(requestId);
// 从申请列表中移除
```

### 3. 好友分组管理

```javascript
// 创建分组
await friendStore.createGroup('工作伙伴');

// 删除分组（好友移到默认分组）
await friendStore.deleteGroup(groupId);

// 移动好友到分组
await friendStore.moveFriend(friendUserId, groupId);
```

### 4. 按分组展示好友

```javascript
// 计算属性：按分组的好友列表
const groupedFriends = computed(() => {
  const result = {};
  
  // 初始化所有分组
  groups.value.forEach(group => {
    result[group.id] = [];
  });
  
  // 将好友分配到对应分组
  friends.value.forEach(friend => {
    const groupId = friend.groupId || 'default';
    if (!result[groupId]) {
      result[groupId] = [];
    }
    result[groupId].push(friend);
  });
  
  return result;
});
```

---

## ⚠️ 注意事项

### 1. 二次确认操作

```javascript
// ✅ 删除分组前确认
await ElMessageBox.confirm(
  '删除分组后，该分组下的好友将移到默认分组，确定继续吗？',
  '删除分组',
  { type: 'warning' }
);

// ✅ 删除好友前确认
await ElMessageBox.confirm(
  '确定要删除该好友吗？删除后将无法恢复。',
  '删除好友',
  {
    type: 'warning',
    confirmButtonClass: 'el-button--danger'
  }
);
```

### 2. 本地状态同步

```javascript
// ✅ 更新备注后同步本地状态
const friend = friends.value.find(f => f.id === friendUserId);
if (friend) {
  friend.remark = remark;
}

// ✅ 移动好友后同步本地状态
const friend = friends.value.find(f => f.id === friendUserId);
if (friend) {
  friend.groupId = groupId;
}

// ✅ 删除好友后从本地列表移除
friends.value = friends.value.filter(f => f.id !== friendUserId);
```

### 3. 用户体验优化

- 搜索用户时显示 loading 状态
- 发送好友申请后清空表单
- 处理好友申请后自动刷新列表
- 所有操作使用 ElMessage 提示结果
- 删除操作必须二次确认

### 4. 好友列表展示

- 优先显示备注名，若无则显示用户名
- 显示在线状态（在线/离线）
- 按分组折叠/展开好友列表
- 支持点击好友发起聊天

---

## 📚 相关文档

- [即时通讯模块](./CHAT_MODULE_USAGE_GUIDE.md)
- [UI 交互与 API 错误处理规范](./MEMORY.md#ui交互与api错误处理规范)
- [前后端协作中的前端功能屏蔽与降级策略](./MEMORY.md#前后端协作中的前端功能屏蔽与降级策略)

---

**最后更新:** 2026-04-17  
**维护者:** 前端开发团队
