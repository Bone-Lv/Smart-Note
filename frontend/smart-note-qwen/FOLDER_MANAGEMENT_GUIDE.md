# 文件夹管理功能实现指南

本文档详细说明了智能笔记项目中文件夹管理功能的实现细节和使用方法。

## 📋 目录

- [一、API接口规范](#一api接口规范)
- [二、功能特性](#二功能特性)
- [三、使用示例](#三使用示例)
- [四、组件说明](#四组件说明)
- [五、注意事项](#五注意事项)

---

## 一、API接口规范

### 1.1 创建文件夹

**接口**: `POST /folder`

**请求体**:
```json
{
  "name": "文件夹名称",
  "parentId": 123 // 可选，父文件夹ID，不传则为根目录
}
```

**前端调用**:
```javascript
import { createFolderApi } from '@/api/note.js';

// 创建根目录文件夹
await createFolderApi({ name: '工作笔记' });

// 创建子文件夹
await createFolderApi({ 
  name: '项目文档', 
  parentId: 123 
});
```

---

### 1.2 获取文件夹树

**接口**: `GET /folder/tree`

**返回**:
```json
{
  "data": [
    {
      "id": 1,
      "name": "工作笔记",
      "children": [
        {
          "id": 2,
          "name": "项目文档",
          "children": []
        }
      ]
    },
    {
      "id": 3,
      "name": "学习笔记",
      "children": []
    }
  ]
}
```

**前端调用**:
```javascript
import { getFolderTreeApi } from '@/api/note.js';

const response = await getFolderTreeApi();
const folderTree = response.data.data;
```

---

### 1.3 更新文件夹

**接口**: `PUT /folder/{folderId}`

**请求体**:
```json
{
  "name": "新名称"
}
```

**前端调用**:
```javascript
import { updateFolderApi } from '@/api/note.js';

await updateFolderApi(123, { name: '新的文件夹名称' });
```

---

### 1.4 删除文件夹

**接口**: `DELETE /folder/{folderId}`

**说明**: 文件夹内的笔记不会被删除，会移到根目录

**前端调用**:
```javascript
import { deleteFolderApi } from '@/api/note.js';

await deleteFolderApi(123);
```

---

## 二、功能特性

### 2.1 核心功能

| 功能 | 说明 | 状态 |
|------|------|------|
| 创建文件夹 | 支持根目录和子文件夹 | ✅ |
| 文件夹树展示 | 递归显示层级结构 | ✅ |
| 重命名文件夹 | 修改文件夹名称 | ✅ |
| 删除文件夹 | 删除后笔记移至根目录 | ✅ |
| 选择文件夹 | 点击筛选该文件夹下的笔记 | ✅ |
| 展开/折叠 | 切换子文件夹显示 | ✅ |

### 2.2 UI特性

- ✅ 树形结构可视化
- ✅ 悬停显示操作按钮
- ✅ 当前选中高亮
- ✅ 图标区分文件夹状态
- ✅ 对话框表单验证

---

## 三、使用示例

### 3.1 在页面中使用FolderTree组件

```vue
<template>
  <div class="note-management">
    <!-- 左侧文件夹树 -->
    <div class="sidebar">
      <FolderTree />
    </div>
    
    <!-- 右侧笔记列表 -->
    <div class="main-content">
      <NoteList :folder-id="currentFolderId" />
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue';
import { useFolderStore } from '@/stores/folderStore.js';
import FolderTree from '@/components/FolderTree.vue';
import NoteList from '@/components/NoteList.vue';

const folderStore = useFolderStore();

// 当前选中的文件夹ID
const currentFolderId = computed(() => folderStore.currentFolderId);
</script>

<style scoped>
.note-management {
  display: flex;
  gap: 20px;
}

.sidebar {
  width: 250px;
  flex-shrink: 0;
}

.main-content {
  flex: 1;
}
</style>
```

---

### 3.2 使用Store管理文件夹状态

```javascript
import { useFolderStore } from '@/stores/folderStore.js';

const folderStore = useFolderStore();

// 加载文件夹树
await folderStore.loadFolderTree();

// 创建文件夹
await folderStore.createFolder({
  name: '新项目',
  parentId: null // 根目录
});

// 更新文件夹
await folderStore.updateFolder(folderId, {
  name: '更新后的名称'
});

// 删除文件夹
await folderStore.deleteFolder(folderId);

// 选择文件夹
folderStore.setCurrentFolder(folderId);

// 获取文件夹信息
const folder = folderStore.getFolderById(folderId);
const childFolders = folderStore.getChildFolders(parentId);
```

---

### 3.3 根据文件夹筛选笔记

```vue
<template>
  <div>
    <h2>{{ currentFolderName || '全部笔记' }}</h2>
    <div v-for="note in filteredNotes" :key="note.id">
      {{ note.title }}
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue';
import { useFolderStore } from '@/stores/folderStore.js';
import { useNoteStore } from '@/stores/noteStore.js';

const folderStore = useFolderStore();
const noteStore = useNoteStore();

// 当前选中的文件夹
const currentFolderId = computed(() => folderStore.currentFolderId);
const currentFolderName = computed(() => {
  if (!currentFolderId.value) return '';
  const folder = folderStore.getFolderById(currentFolderId.value);
  return folder?.name || '';
});

// 根据文件夹筛选笔记
const filteredNotes = computed(() => {
  if (!currentFolderId.value) {
    return noteStore.allNotes; // 显示全部
  }
  return noteStore.allNotes.filter(note => note.folderId === currentFolderId.value);
});
</script>
```

---

### 3.4 拖拽移动文件夹（扩展功能）

```vue
<template>
  <FolderTreeNode
    :folder="folder"
    @dragstart="handleDragStart"
    @drop="handleDrop"
    draggable
  />
</template>

<script setup>
const handleDragStart = (event, folder) => {
  event.dataTransfer.setData('folderId', folder.id);
};

const handleDrop = async (event, targetFolder) => {
  const sourceFolderId = event.dataTransfer.getData('folderId');
  
  // 调用API移动文件夹
  await folderStore.updateFolder(sourceFolderId, {
    parentId: targetFolder.id
  });
};
</script>
```

---

## 四、组件说明

### 4.1 FolderTree 组件

**文件**: `src/components/FolderTree.vue`

**功能**: 文件夹树主组件，包含创建、编辑、删除对话框

**Props**: 无

**Events**: 无

**使用**:
```vue
<FolderTree />
```

---

### 4.2 FolderTreeNode 组件

**文件**: `src/components/FolderTreeNode.vue`

**功能**: 文件夹树节点组件（递归组件）

**Props**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| folder | Object | ✅ | 文件夹对象 |
| currentFolderId | String/Number | ❌ | 当前选中的文件夹ID |

**Events**:
| 事件名 | 参数 | 说明 |
|--------|------|------|
| select | folderId | 选择文件夹 |
| edit | folder | 编辑文件夹 |
| delete | folder | 删除文件夹 |

**使用**:
```vue
<FolderTreeNode
  :folder="folder"
  :current-folder-id="currentFolderId"
  @select="handleSelect"
  @edit="handleEdit"
  @delete="handleDelete"
/>
```

---

### 4.3 folderStore

**文件**: `src/stores/folderStore.js`

**State**:
- `folderTree`: 文件夹树数据
- `loading`: 加载状态
- `currentFolderId`: 当前选中的文件夹ID

**Getters**:
- `allFolders`: 扁平化的所有文件夹列表
- `getFolderById(folderId)`: 根据ID查找文件夹
- `getChildFolders(parentId)`: 获取子文件夹

**Actions**:
- `loadFolderTree()`: 加载文件夹树
- `createFolder(folderData)`: 创建文件夹
- `updateFolder(folderId, folderData)`: 更新文件夹
- `deleteFolder(folderId)`: 删除文件夹
- `setCurrentFolder(folderId)`: 设置当前选中的文件夹
- `clearCurrentFolder()`: 清空当前选中

---

## 五、注意事项

### 5.1 文件夹删除行为

**重要**: 删除文件夹时，该文件夹内的笔记**不会被删除**，而是会自动移至**根目录**。

**后端逻辑**:
```sql
-- 删除文件夹时，将其中的笔记的folder_id设为NULL
UPDATE notes SET folder_id = NULL WHERE folder_id = ?;
DELETE FROM folders WHERE id = ?;
```

**前端提示**:
```javascript
ElMessage.success('✅ 文件夹删除成功，笔记已移至根目录');
```

---

### 5.2 递归组件注意事项

FolderTreeNode是递归组件，需要确保：
1. 组件名称必须设置（`name: 'FolderTreeNode'`）
2. 递归调用时使用相同的组件名
3. 必须有终止条件（`v-if="hasChildren && isExpanded"`）

---

### 5.3 性能优化建议

#### 虚拟滚动
如果文件夹数量很大（1000+），考虑使用虚拟滚动：

```vue
<RecycleScroller
  :items="folderTree"
  :item-size="40"
  key-field="id"
>
  <template #default="{ item }">
    <FolderTreeNode :folder="item" />
  </template>
</RecycleScroller>
```

#### 懒加载
对于深层嵌套的文件夹，可以实现懒加载：

```javascript
// 初始只加载根目录
const loadRootFolders = async () => {
  const response = await getFolderTreeApi({ depth: 1 });
  folderTree.value = response.data.data;
};

// 展开时加载子文件夹
const loadChildren = async (folderId) => {
  const response = await getFolderChildrenApi(folderId);
  // 更新对应节点的children
};
```

---

### 5.4 错误处理

#### 创建文件夹失败
```javascript
try {
  await folderStore.createFolder(formData);
} catch (error) {
  if (error.response?.status === 409) {
    ElMessage.error('文件夹名称已存在');
  } else {
    ElMessage.error('创建文件夹失败');
  }
}
```

#### 删除文件夹失败
```javascript
try {
  await folderStore.deleteFolder(folderId);
} catch (error) {
  if (error.response?.status === 400) {
    ElMessage.error('文件夹不为空，请先移动或删除其中的笔记');
  } else {
    ElMessage.error('删除文件夹失败');
  }
}
```

---

### 5.5 权限控制

如果后端实现了文件夹权限，前端需要：

1. **显示权限标识**:
```vue
<div class="folder-item" :class="{ 'shared': folder.isShared }">
  <i v-if="folder.isShared" class="fas fa-share-alt"></i>
  {{ folder.name }}
</div>
```

2. **限制操作**:
```javascript
const canEdit = computed(() => {
  return folder.permission === 'owner' || folder.permission === 'editor';
});

const canDelete = computed(() => {
  return folder.permission === 'owner';
});
```

---

## 六、完整示例

### 6.1 笔记管理页面

```vue
<template>
  <div class="note-manager">
    <!-- 侧边栏：文件夹树 -->
    <aside class="sidebar">
      <FolderTree />
    </aside>
    
    <!-- 主内容区 -->
    <main class="main">
      <!-- 工具栏 -->
      <div class="toolbar">
        <h2>{{ currentFolderName || '全部笔记' }}</h2>
        <el-button type="primary" @click="createNote">
          <i class="fas fa-plus"></i> 新建笔记
        </el-button>
      </div>
      
      <!-- 笔记列表 -->
      <NoteList :folder-id="currentFolderId" />
    </main>
  </div>
</template>

<script setup>
import { computed } from 'vue';
import { useRouter } from 'vue-router';
import { useFolderStore } from '@/stores/folderStore.js';
import FolderTree from '@/components/FolderTree.vue';
import NoteList from '@/components/NoteList.vue';

const router = useRouter();
const folderStore = useFolderStore();

const currentFolderId = computed(() => folderStore.currentFolderId);
const currentFolderName = computed(() => {
  if (!currentFolderId.value) return '全部笔记';
  const folder = folderStore.getFolderById(currentFolderId.value);
  return folder?.name || '未知文件夹';
});

const createNote = () => {
  router.push({
    path: '/notes/create',
    query: { folderId: currentFolderId.value || undefined }
  });
};
</script>

<style scoped>
.note-manager {
  display: flex;
  height: calc(100vh - 60px);
}

.sidebar {
  width: 260px;
  border-right: 1px solid #eee;
  overflow-y: auto;
}

.main {
  flex: 1;
  padding: 20px;
  overflow-y: auto;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.toolbar h2 {
  margin: 0;
  font-size: 20px;
}
</style>
```

---

## 七、相关文件清单

### API层
- `src/api/note.js` - 添加了文件夹管理的4个API接口

### Store
- `src/stores/folderStore.js` - 文件夹状态管理

### 组件
- `src/components/FolderTree.vue` - 文件夹树主组件
- `src/components/FolderTreeNode.vue` - 文件夹树节点组件（递归）

---

## 八、后续优化建议

1. **拖拽排序**: 支持拖拽调整文件夹顺序和层级
2. **批量操作**: 支持批量移动笔记到不同文件夹
3. **文件夹颜色**: 允许用户自定义文件夹颜色标识
4. **收藏文件夹**: 常用文件夹可以置顶显示
5. **文件夹统计**: 显示每个文件夹下的笔记数量
6. **搜索文件夹**: 快速定位特定文件夹
7. **导入导出**: 支持文件夹结构的导入导出
8. **共享文件夹**: 多人协作的共享文件夹功能

---

**文档版本**: v1.0  
**更新日期**: 2026-04-17  
**维护者**: Smart-Note Team
