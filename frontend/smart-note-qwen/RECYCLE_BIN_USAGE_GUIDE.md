# 回收站模块使用指南

## 📋 概述

回收站模块提供笔记和文件夹的临时存储功能，支持：
- 查看已删除的笔记和文件夹
- 还原操作（恢复到原位置）
- 彻底删除（不可恢复）
- 清空回收站
- 5 分钟倒计时提示（5 分钟后自动彻底删除）

---

## 🔌 API 接口汇总

| 功能 | 接口 | 方法 | 说明 |
|------|------|------|------|
| 获取笔记列表 | `/recycle-bin/notes` | GET | 回收站中的笔记 |
| 获取文件夹列表 | `/recycle-bin/folders` | GET | 回收站中的文件夹 |
| 还原笔记 | `/recycle-bin/notes/{noteId}/restore` | POST | 恢复到原位置 |
| 还原文件夹 | `/recycle-bin/folders/{folderId}/restore` | POST | 恢复文件夹及内容 |
| 彻底删除笔记 | `/recycle-bin/notes/{noteId}` | DELETE | 永久删除 |
| 彻底删除文件夹 | `/recycle-bin/folders/{folderId}` | DELETE | 永久删除 |
| 清空回收站 | `/recycle-bin/empty` | DELETE | 清空所有内容 |

---

## 📖 使用示例

### 1. 初始化回收站 Store

```vue
<script setup>
import { onMounted } from 'vue';
import { useRecycleBinStore } from '@/stores/recycleBinStore.js';

const recycleBinStore = useRecycleBinStore();

onMounted(async () => {
  // 加载回收站数据
  await recycleBinStore.refreshAll();
});
</script>
```

### 2. 显示回收站页面

```vue
<script setup>
import { computed } from 'vue';
import { useRecycleBinStore } from '@/stores/recycleBinStore.js';

const recycleBinStore = useRecycleBinStore();

// 当前激活的标签页
const activeTab = computed(() => recycleBinStore.activeTab);

// 笔记列表
const notes = computed(() => recycleBinStore.notes);

// 文件夹列表
const folders = computed(() => recycleBinStore.folders);

// 总项目数
const totalItems = computed(() => recycleBinStore.totalItems);

// 切换标签页
const handleTabChange = (tab) => {
  recycleBinStore.setActiveTab(tab);
};
</script>

<template>
  <div class="recycle-bin">
    <div class="header">
      <h2>回收站</h2>
      <el-alert
        v-if="totalItems > 0"
        title="注意：删除的项目将在 5 分钟后自动彻底删除"
        type="warning"
        :closable="false"
      />
    </div>

    <!-- 标签页 -->
    <el-tabs v-model="activeTab" @tab-change="handleTabChange">
      <el-tab-pane label="笔记" name="notes">
        <NoteList :notes="notes" />
      </el-tab-pane>
      
      <el-tab-pane label="文件夹" name="folders">
        <FolderList :folders="folders" />
      </el-tab-pane>
    </el-tabs>

    <!-- 清空回收站按钮 -->
    <el-button 
      v-if="totalItems > 0"
      type="danger"
      @click="recycleBinStore.emptyRecycleBin"
    >
      清空回收站
    </el-button>
  </div>
</template>
```

### 3. 笔记列表组件

```vue
<script setup>
import { useRecycleBinStore } from '@/stores/recycleBinStore.js';

defineProps({
  notes: { type: Array, required: true }
});

const recycleBinStore = useRecycleBinStore();

// 格式化删除时间
const formatTime = (deleteTime) => {
  return recycleBinStore.formatDeleteTime(deleteTime);
};

// 还原笔记
const handleRestore = async (noteId) => {
  try {
    await recycleBinStore.restoreNote(noteId);
  } catch (error) {
    console.error('还原失败:', error);
  }
};

// 彻底删除
const handleDelete = async (noteId) => {
  try {
    await recycleBinStore.permanentlyDeleteNote(noteId);
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败:', error);
    }
  }
};
</script>

<template>
  <div class="note-list">
    <el-table :data="notes" style="width: 100%">
      <el-table-column prop="title" label="标题" />
      <el-table-column prop="deleteTime" label="删除时间">
        <template #default="{ row }">
          {{ formatTime(row.deleteTime) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="200">
        <template #default="{ row }">
          <el-button 
            size="small" 
            type="primary"
            @click="handleRestore(row.id)"
          >
            还原
          </el-button>
          <el-button 
            size="small" 
            type="danger"
            @click="handleDelete(row.id)"
          >
            彻底删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>
```

### 4. 文件夹列表组件

```vue
<script setup>
import { useRecycleBinStore } from '@/stores/recycleBinStore.js';

defineProps({
  folders: { type: Array, required: true }
});

const recycleBinStore = useRecycleBinStore();

const formatTime = (deleteTime) => {
  return recycleBinStore.formatDeleteTime(deleteTime);
};

const handleRestore = async (folderId) => {
  try {
    await recycleBinStore.restoreFolder(folderId);
  } catch (error) {
    console.error('还原失败:', error);
  }
};

const handleDelete = async (folderId) => {
  try {
    await recycleBinStore.permanentlyDeleteFolder(folderId);
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败:', error);
    }
  }
};
</script>

<template>
  <div class="folder-list">
    <el-table :data="folders" style="width: 100%">
      <el-table-column prop="name" label="文件夹名称" />
      <el-table-column prop="deleteTime" label="删除时间">
        <template #default="{ row }">
          {{ formatTime(row.deleteTime) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="200">
        <template #default="{ row }">
          <el-button 
            size="small" 
            type="primary"
            @click="handleRestore(row.id)"
          >
            还原
          </el-button>
          <el-button 
            size="small" 
            type="danger"
            @click="handleDelete(row.id)"
          >
            彻底删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>
```

### 5. 倒计时更新

```vue
<script setup>
import { ref, onMounted, onUnmounted } from 'vue';
import { useRecycleBinStore } from '@/stores/recycleBinStore.js';

const recycleBinStore = useRecycleBinStore();
const timer = ref(null);

onMounted(() => {
  // 每秒更新一次倒计时
  timer.value = setInterval(() => {
    // 触发重新渲染，formatDeleteTime 会重新计算
    recycleBinStore.notes = [...recycleBinStore.notes];
    recycleBinStore.folders = [...recycleBinStore.folders];
  }, 1000);
});

onUnmounted(() => {
  if (timer.value) {
    clearInterval(timer.value);
  }
});
</script>
```

---

## 🔧 关键实现要点

### 1. 还原操作流程

```javascript
// 1. 点击"还原"按钮
const handleRestore = async (noteId) => {
  // 2. 调用 API
  await recycleBinStore.restoreNote(noteId);
  
  // 3. 成功后从列表中移除（Store 内部处理）
  // 4. 显示成功提示
  ElMessage.success('笔记已还原');
};
```

### 2. 彻底删除流程

```javascript
// 1. 点击"彻底删除"按钮
const handleDelete = async (noteId) => {
  // 2. 二次确认对话框
  await ElMessageBox.confirm(
    '此操作将永久删除该笔记，无法恢复，确定继续吗？',
    '彻底删除',
    {
      confirmButtonText: '确定删除',
      cancelButtonText: '取消',
      type: 'warning'
    }
  );
  
  // 3. 调用 API
  await recycleBinStore.permanentlyDeleteNote(noteId);
  
  // 4. 从列表中移除
  // 5. 显示成功提示
};
```

### 3. 清空回收站流程

```javascript
const emptyRecycleBin = async () => {
  // 1. 强烈警告提示
  await ElMessageBox.confirm(
    `回收站中共有 ${totalItems.value} 个项目，清空后将永久删除所有内容且无法恢复！\n\n此操作不可撤销，确定要继续吗？`,
    '清空回收站',
    {
      confirmButtonText: '确定清空',
      cancelButtonText: '取消',
      type: 'warning',
      confirmButtonClass: 'el-button--danger'
    }
  );
  
  // 2. 调用 API
  await emptyRecycleBinApi();
  
  // 3. 清空本地列表
  notes.value = [];
  folders.value = [];
  
  // 4. 显示成功提示
  ElMessage.success('回收站已清空');
};
```

### 4. 倒计时显示

```javascript
const formatDeleteTime = (deleteTime) => {
  const date = new Date(deleteTime);
  const now = new Date();
  const diff = now - date;
  
  // 如果超过 5 分钟，显示具体时间
  if (diff > 5 * 60 * 1000) {
    return date.toLocaleString('zh-CN');
  }
  
  // 否则显示倒计时
  const remaining = 5 * 60 * 1000 - diff;
  const minutes = Math.floor(remaining / 60000);
  const seconds = Math.floor((remaining % 60000) / 1000);
  
  return `${minutes}分${seconds}秒后自动删除`;
};
```

---

## ⚠️ 注意事项

### 1. 二次确认

```javascript
// ✅ 彻底删除时必须二次确认
await ElMessageBox.confirm(
  '此操作将永久删除，无法恢复，确定继续吗？',
  '彻底删除',
  {
    type: 'warning',
    confirmButtonClass: 'el-button--danger'
  }
);

// ❌ 不要直接删除
await permanentlyDeleteNoteApi(noteId); // 错误
```

### 2. 清空回收站警告

```javascript
// ✅ 强烈警告，显示项目数量
await ElMessageBox.confirm(
  `回收站中共有 ${totalItems.value} 个项目，清空后将永久删除所有内容且无法恢复！`,
  '清空回收站',
  {
    type: 'warning',
    confirmButtonClass: 'el-button--danger'
  }
);
```

### 3. 倒计时更新

```javascript
// ✅ 每秒更新倒计时
const timer = setInterval(() => {
  // 触发重新渲染
  notes.value = [...notes.value];
}, 1000);

// ✅ 组件卸载时清理定时器
onUnmounted(() => {
  clearInterval(timer);
});
```

### 4. 还原后的位置

- **笔记**：还原到原文件夹或根目录
- **文件夹**：还原包括其下的所有笔记

### 5. 自动删除机制

- 后端会在 5 分钟后自动彻底删除回收站中的项目
- 前端仅显示倒计时提示
- 实际删除由后端定时任务执行

---

## 📚 相关文档

- [文件夹管理模块](./FOLDER_MODULE_USAGE_GUIDE.md)
- [笔记模块](./NOTE_MODULE_USAGE_GUIDE.md)
- [UI 交互与 API 错误处理规范](./MEMORY.md#ui交互与api错误处理规范)

---

**最后更新:** 2026-04-17  
**维护者:** 前端开发团队
