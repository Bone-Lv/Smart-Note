<template>
  <div class="folder-tree">
    <div class="folder-tree-header">
      <h3>文件夹</h3>
      <button @click="showCreateDialog = true" class="create-folder-btn" title="创建文件夹">
        <i class="fas fa-plus"></i>
      </button>
    </div>
    
    <!-- 搜索框 -->
    <div class="folder-search">
      <i class="fas fa-search"></i>
      <input 
        v-model="searchKeyword" 
        type="text" 
        placeholder="搜索文件夹..."
        @input="handleSearch"
      >
    </div>
    
    <!-- 全部笔记 -->
    <div 
      class="folder-item"
      :class="{ active: !currentFolderId }"
      @click="selectFolder(null)"
    >
      <i class="fas fa-th-large"></i>
      <span>全部笔记</span>
    </div>
    
    <!-- 文件夹树 -->
    <div class="folder-list">
      <FolderTreeNode
        v-for="folder in filteredFolderTree"
        :key="folder.id"
        :folder="folder"
        :current-folder-id="currentFolderId"
        :search-keyword="searchKeyword"
        @select="selectFolder"
        @edit="handleEditFolder"
        @delete="handleDeleteFolder"
      />
    </div>
    
    <!-- 创建文件夹对话框 -->
    <el-dialog
      v-model="showCreateDialog"
      title="创建文件夹"
      width="450px"
    >
      <el-form :model="createForm" label-width="90px">
        <el-form-item label="文件夹名称">
          <el-input 
            v-model="createForm.name" 
            placeholder="请输入文件夹名称"
            @keyup.enter="handleCreateFolder"
          />
        </el-form-item>
        <el-form-item label="父文件夹">
          <el-select v-model="createForm.parentId" placeholder="选择父文件夹（可选）" clearable style="width: 100%">
            <el-option 
              v-for="folder in allFolders" 
              :key="folder.id" 
              :label="folder.name" 
              :value="folder.id"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreateDialog = false">取消</el-button>
        <el-button type="primary" @click="handleCreateFolder" :loading="creating">
          创建
        </el-button>
      </template>
    </el-dialog>
    
    <!-- 编辑文件夹对话框 -->
    <el-dialog
      v-model="showEditDialog"
      title="编辑文件夹"
      width="450px"
    >
      <el-form :model="editForm" label-width="90px">
        <el-form-item label="文件夹名称">
          <el-input 
            v-model="editForm.name" 
            placeholder="请输入文件夹名称"
            @keyup.enter="handleUpdateFolder"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showEditDialog = false">取消</el-button>
        <el-button type="primary" @click="handleUpdateFolder" :loading="updating">
          保存
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import { useFolderStore } from '../stores/folderStore.js';
import FolderTreeNode from './FolderTreeNode.vue';
import { ElMessageBox } from 'element-plus';

const folderStore = useFolderStore();

const showCreateDialog = ref(false);
const showEditDialog = ref(false);
const creating = ref(false);
const updating = ref(false);
const searchKeyword = ref('');

const createForm = ref({
  name: '',
  parentId: null
});

const editForm = ref({
  id: null,
  name: ''
});

// 计算属性
const folderTree = computed(() => folderStore.folderTree);
const currentFolderId = computed(() => folderStore.currentFolderId);
const allFolders = computed(() => folderStore.allFolders);

// 过滤后的文件夹树（支持搜索）
const filteredFolderTree = computed(() => {
  if (!searchKeyword.value) {
    return folderTree.value;
  }
  
  const keyword = searchKeyword.value.toLowerCase();
  
  const filterFolders = (folders) => {
    return folders.filter(folder => {
      const matchName = folder.name.toLowerCase().includes(keyword);
      const filteredChildren = folder.children ? filterFolders(folder.children) : [];
      
      // 如果当前节点匹配或有匹配的子节点，则保留
      return matchName || filteredChildren.length > 0;
    }).map(folder => ({
      ...folder,
      children: folder.children ? filterFolders(folder.children) : []
    }));
  };
  
  return filterFolders(folderTree.value);
});

// 加载文件夹树
const loadFolders = async () => {
  try {
    await folderStore.loadFolderTree();
  } catch (error) {
    console.error('Load folders error:', error);
  }
};

// 搜索处理
const handleSearch = () => {
  // 搜索逻辑已在computed中实现
};

// 选择文件夹
const selectFolder = (folderId) => {
  folderStore.setCurrentFolder(folderId);
};

// 创建文件夹
const handleCreateFolder = async () => {
  if (!createForm.value.name.trim()) {
    ElMessageBox.alert('请输入文件夹名称', '提示', { type: 'warning' });
    return;
  }
  
  // TODO: 颜色功能需要后端支持，暂时禁用
  // if (!createForm.value.color) {
  //   createForm.value.color = '#ffa500'; // 默认橙色
  // }
  
  creating.value = true;
  try {
    await folderStore.createFolder(createForm.value);
    showCreateDialog.value = false;
    createForm.value = { name: '', parentId: null };
    await loadFolders();
  } catch (error) {
    console.error('Create folder error:', error);
  } finally {
    creating.value = false;
  }
};

// 编辑文件夹
const handleEditFolder = (folder) => {
  editForm.value = {
    id: folder.id,
    name: folder.name
  };
  showEditDialog.value = true;
};

// 更新文件夹
const handleUpdateFolder = async () => {
  if (!editForm.value.name.trim()) {
    ElMessageBox.alert('请输入文件夹名称', '提示', { type: 'warning' });
    return;
  }
  
  updating.value = true;
  try {
    await folderStore.updateFolder(editForm.value.id, {
      name: editForm.value.name
    });
    showEditDialog.value = false;
    await loadFolders();
  } catch (error) {
    console.error('Update folder error:', error);
  } finally {
    updating.value = false;
  }
};

// 删除文件夹
const handleDeleteFolder = async (folder) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除文件夹"${folder.name}"吗？文件夹内的笔记将移至根目录。`,
      '删除确认',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    );
    
    await folderStore.deleteFolder(folder.id);
    await loadFolders();
  } catch (error) {
    if (error !== 'cancel') {
      console.error('Delete folder error:', error);
    }
  }
};

// 组件挂载时加载文件夹
onMounted(() => {
  loadFolders();
});
</script>

<style scoped>
.folder-tree {
  background: white;
  border-radius: 8px;
  padding: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  height: 100%;
  display: flex;
  flex-direction: column;
}

.folder-tree-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  padding-bottom: 12px;
  border-bottom: 1px solid #eee;
}

.folder-tree-header h3 {
  margin: 0;
  font-size: 16px;
  color: #333;
}

.create-folder-btn {
  width: 32px;
  height: 32px;
  border: none;
  border-radius: 50%;
  background: #409eff;
  color: white;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  transition: all 0.3s;
}

.create-folder-btn:hover {
  background: #66b1ff;
  transform: scale(1.1);
}

/* 搜索框 */
.folder-search {
  position: relative;
  margin-bottom: 12px;
}

.folder-search i {
  position: absolute;
  left: 10px;
  top: 50%;
  transform: translateY(-50%);
  color: #999;
  font-size: 14px;
}

.folder-search input {
  width: 100%;
  padding: 8px 12px 8px 32px;
  border: 1px solid #ddd;
  border-radius: 6px;
  font-size: 14px;
  outline: none;
  transition: border-color 0.3s;
}

.folder-search input:focus {
  border-color: #409eff;
}

.folder-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 12px;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.3s;
  margin-bottom: 4px;
}

.folder-item:hover {
  background: #f5f7fa;
}

.folder-item.active {
  background: #ecf5ff;
  color: #409eff;
}

.folder-item i {
  font-size: 16px;
}

.folder-list {
  flex: 1;
  overflow-y: auto;
  max-height: calc(100vh - 300px);
}
</style>
