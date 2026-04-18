# 文件夹管理模块使用指南

## 📋 概述

文件夹管理模块提供笔记的分类和组织功能，支持：
- 文件夹树形结构展示
- 创建、重命名、移动、删除文件夹
- 文件夹与笔记关联
- 笔记数量统计
- 拖拽排序（可扩展）

---

## 🔌 API 接口汇总

| 功能 | 接口 | 方法 | 说明 |
|------|------|------|------|
| 创建文件夹 | `/note/folder` | POST | name, parentId(可选), sortOrder |
| 获取文件夹树 | `/note/folder/tree` | GET | 完整的树形结构 |
| 获取子文件夹 | `/note/folder/children` | GET | parentId(可选) |
| 重命名文件夹 | `/note/folder/{folderId}` | PUT | { name } |
| 移动文件夹 | `/note/folder/{folderId}/move` | PUT | { parentId } |
| 删除文件夹 | `/note/folder/{folderId}` | DELETE | { deleteNotes: false } |

---

## 📖 使用示例

### 1. 初始化文件夹 Store

```vue
<script setup>
import { onMounted } from 'vue';
import { useFolderStore } from '@/stores/folderStore.js';

const folderStore = useFolderStore();

onMounted(async () => {
  // 加载文件夹树
  await folderStore.fetchFolderTree();
});
</script>
```

### 2. 显示文件夹树

```vue
<script setup>
import { computed } from 'vue';
import { useFolderStore } from '@/stores/folderStore.js';

const folderStore = useFolderStore();

// 文件夹树
const folderTree = computed(() => folderStore.folderTree);

// 选中的文件夹
const selectedFolder = computed(() => folderStore.selectedFolder);

// 是否选中根目录
const isRootSelected = computed(() => folderStore.isRootSelected);

// 选择文件夹
const selectFolder = (folder) => {
  folderStore.setSelectedFolder(folder);
  // 触发笔记列表过滤
  emit('folder-selected', folder?.id || null);
};

// 展开/折叠文件夹
const toggleFolder = (folderId) => {
  folderStore.toggleFolder(folderId);
};

// 检查文件夹是否展开
const isExpanded = (folderId) => {
  return folderStore.isFolderExpanded(folderId);
};
</script>

<template>
  <div class="folder-tree">
    <!-- 根目录 -->
    <div 
      class="folder-item"
      :class="{ active: isRootSelected }"
      @click="selectFolder(null)"
    >
      <el-icon><FolderOpened /></el-icon>
      <span>全部笔记</span>
    </div>

    <!-- 文件夹树 -->
    <div v-for="folder in folderTree" :key="folder.id">
      <FolderTreeNode 
        :folder="folder"
        :is-expanded="isExpanded(folder.id)"
        @select="selectFolder"
        @toggle="toggleFolder"
      />
    </div>
  </div>
</template>
```

### 3. 文件夹树节点组件

```vue
<template>
  <div class="folder-node">
    <!-- 文件夹项 -->
    <div 
      class="folder-item"
      :class="{ active: isSelected, expanded: isExpanded }"
      :style="{ paddingLeft: `${level * 20}px` }"
      @click="handleSelect"
      @contextmenu.prevent="showContextMenu"
    >
      <!-- 展开/折叠图标 -->
      <el-icon 
        v-if="folder.children?.length > 0"
        class="expand-icon"
        @click.stop="handleToggle"
      >
        <component :is="isExpanded ? 'ArrowDown' : 'ArrowRight'" />
      </el-icon>
      <span v-else class="expand-placeholder"></span>

      <!-- 文件夹图标 -->
      <el-icon class="folder-icon">
        <Folder v-if="!isExpanded" />
        <FolderOpened v-else />
      </el-icon>

      <!-- 文件夹名称 -->
      <span class="folder-name">{{ folder.name }}</span>

      <!-- 笔记数量 -->
      <span v-if="folder.noteCount > 0" class="note-count">
        {{ folder.noteCount }}
      </span>
    </div>

    <!-- 子文件夹 -->
    <div v-if="isExpanded && folder.children?.length > 0" class="children">
      <FolderTreeNode
        v-for="child in folder.children"
        :key="child.id"
        :folder="child"
        :level="level + 1"
        :is-expanded="isChildExpanded(child.id)"
        @select="$emit('select', $event)"
        @toggle="handleChildToggle"
      />
    </div>

    <!-- 右键菜单 -->
    <ContextMenu
      v-if="contextMenuVisible"
      :position="contextMenuPosition"
      @close="contextMenuVisible = false"
    >
      <ContextMenuItem @click="handleRename">重命名</ContextMenuItem>
      <ContextMenuItem @click="handleMove">移动到</ContextMenuItem>
      <ContextMenuItem @click="handleDelete" class="danger">删除</ContextMenuItem>
    </ContextMenu>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue';
import { useFolderStore } from '@/stores/folderStore.js';

const props = defineProps({
  folder: { type: Object, required: true },
  level: { type: Number, default: 0 },
  isExpanded: { type: Boolean, default: false }
});

const emit = defineEmits(['select', 'toggle']);

const folderStore = useFolderStore();

const isSelected = computed(() => 
  folderStore.selectedFolder?.id === props.folder.id
);

const contextMenuVisible = ref(false);
const contextMenuPosition = ref({ x: 0, y: 0 });

const handleSelect = () => {
  emit('select', props.folder);
};

const handleToggle = () => {
  emit('toggle', props.folder.id);
};

const showContextMenu = (event) => {
  contextMenuPosition.value = { x: event.clientX, y: event.clientY };
  contextMenuVisible.value = true;
};

const handleRename = async () => {
  try {
    const { value } = await ElMessageBox.prompt('请输入新名称', '重命名文件夹', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      inputValue: props.folder.name
    });
    
    await folderStore.renameFolder(props.folder.id, value);
  } catch (error) {
    if (error !== 'cancel') {
      console.error('重命名失败:', error);
    }
  }
};

const handleMove = async () => {
  // 打开移动对话框
  // 选择目标文件夹
  // 调用 folderStore.moveFolder(folderId, targetParentId)
};

const handleDelete = async () => {
  try {
    await folderStore.deleteFolder(props.folder.id, false);
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败:', error);
    }
  }
};
</script>
```

### 4. 创建文件夹

```vue
<script setup>
import { useFolderStore } from '@/stores/folderStore.js';
import { ElMessageBox } from 'element-plus';

const folderStore = useFolderStore();

const createFolder = async () => {
  try {
    const { value } = await ElMessageBox.prompt('请输入文件夹名称', '新建文件夹', {
      confirmButtonText: '确定',
      cancelButtonText: '取消'
    });
    
    await folderStore.createFolder({
      name: value,
      parentId: folderStore.selectedFolderId, // 在当前选中的文件夹下创建
      sortOrder: 0
    });
  } catch (error) {
    if (error !== 'cancel') {
      console.error('创建失败:', error);
    }
  }
};
</script>

<template>
  <el-button @click="createFolder" type="primary">
    <el-icon><Plus /></el-icon>
    新建文件夹
  </el-button>
</template>
```

### 5. 移动文件夹（拖拽）

```vue
<script setup>
import { useFolderStore } from '@/stores/folderStore.js';

const folderStore = useFolderStore();

const handleDragStart = (event, folder) => {
  event.dataTransfer.setData('folderId', folder.id);
};

const handleDragOver = (event) => {
  event.preventDefault();
};

const handleDrop = async (event, targetFolder) => {
  event.preventDefault();
  
  const folderId = parseInt(event.dataTransfer.getData('folderId'));
  
  try {
    await folderStore.moveFolder(folderId, targetFolder.id);
  } catch (error) {
    console.error('移动失败:', error);
  }
};
</script>

<template>
  <div 
    class="folder-item"
    draggable="true"
    @dragstart="handleDragStart($event, folder)"
    @dragover="handleDragOver"
    @drop="handleDrop($event, folder)"
  >
    {{ folder.name }}
  </div>
</template>
```

### 6. 获取文件夹路径（面包屑）

```vue
<script setup>
import { computed } from 'vue';
import { useFolderStore } from '@/stores/folderStore.js';

const folderStore = useFolderStore();

// 面包屑路径
const breadcrumbPath = computed(() => {
  if (!folderStore.selectedFolderId) {
    return [{ id: null, name: '全部笔记' }];
  }
  
  return folderStore.getFolderPath(folderStore.selectedFolderId);
});

// 导航到指定文件夹
const navigateToFolder = (folderId) => {
  const folder = folderId ? folderStore.findFolderNode(folderId) : null;
  folderStore.setSelectedFolder(folder);
};
</script>

<template>
  <el-breadcrumb separator="/">
    <el-breadcrumb-item 
      v-for="item in breadcrumbPath" 
      :key="item.id"
      @click="navigateToFolder(item.id)"
    >
      {{ item.name }}
    </el-breadcrumb-item>
  </el-breadcrumb>
</template>
```

---

##  关键实现要点

### 1. 文件夹树结构

```javascript
// 树形数据结构
{
  id: 1,
  name: '工作',
  noteCount: 5,
  children: [
    {
      id: 2,
      name: '项目A',
      noteCount: 3,
      children: []
    }
  ]
}
```

### 2. 创建文件夹流程

```javascript
// 1. 弹出输入框
const { value } = await ElMessageBox.prompt('请输入文件夹名称', '新建文件夹');

// 2. 调用 API 创建
await folderStore.createFolder({
  name: value,
  parentId: folderStore.selectedFolderId,
  sortOrder: 0
});

// 3. 自动刷新文件夹树
// 4. 显示成功提示
```

### 3. 重命名文件夹

```javascript
// 优化：无需重新加载整个树
await folderStore.renameFolder(folderId, newName);

// Store 内部更新本地树结构
updateFolderInTree(folderId, { name: newName });
```

### 4. 移动文件夹

```javascript
// 后端会检测循环引用
try {
  await folderStore.moveFolder(folderId, targetParentId);
} catch (error) {
  if (error.response?.status === 400) {
    ElMessage.error('不能将文件夹移动到自身或子文件夹下');
  }
}
```

### 5. 删除文件夹

```javascript
// 询问是否同时删除笔记
await folderStore.deleteFolder(folderId, deleteNotes);

// deleteNotes = false（默认）：笔记移到根目录
// deleteNotes = true：笔记进入回收站
```

---

## ⚠️ 注意事项

### 1. 循环引用检测

```javascript
// ❌ 错误：不能将文件夹移动到自身或子文件夹下
// 后端会返回 400 错误
await folderStore.moveFolder(folderId, folderId);

// ✅ 正确：移动到父文件夹或其他位置
await folderStore.moveFolder(folderId, parentFolderId);
```

### 2. 删除文件夹的影响

```javascript
// 默认：笔记移到根目录
await folderStore.deleteFolder(folderId, false);

// 同时删除笔记
await folderStore.deleteFolder(folderId, true);
```

### 3. 本地状态同步

```javascript
// 重命名时优化：更新本地树，无需重新加载
updateFolderInTree(folderId, { name: newName });

// 移动/创建/删除时：刷新整个树
await fetchFolderTree();
```

### 4. 选中状态管理

```javascript
// 删除当前选中的文件夹时，切换到根目录
if (selectedFolder.value?.id === folderId) {
  selectedFolder.value = null;
}
```

---

## 📚 相关文档

- [笔记模块使用指南](./NOTE_MODULE_USAGE_GUIDE.md)
- [防抖节流工具函数](./MEMORY.md#防抖节流工具函数规范与应用场景)

---

**最后更新:** 2026-04-17  
**维护者:** 前端开发团队
