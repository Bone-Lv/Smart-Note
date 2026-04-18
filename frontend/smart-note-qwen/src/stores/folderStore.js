import { defineStore } from 'pinia';
import { ref, computed } from 'vue';
import { 
  createFolderApi,
  getFolderTreeApi,
  getFolderChildrenApi,
  renameFolderApi,
  moveFolderApi,
  deleteFolderApi
} from '../api/folder.js';
import { ElMessage, ElMessageBox } from 'element-plus';

export const useFolderStore = defineStore('folder', () => {
  // 状态
  const folderTree = ref([]); // 文件夹树
  const selectedFolder = ref(null); // 当前选中的文件夹
  const expandedFolders = ref(new Set()); // 展开的文件夹ID集合
  const loading = ref(false); // 加载状态

  // 计算属性
  const selectedFolderId = computed(() => 
    selectedFolder.value?.id || null
  );

  const isRootSelected = computed(() => 
    selectedFolder.value === null
  );

  // ==================== 文件夹树管理 ====================

  /**
   * 获取文件夹树
   */
  const fetchFolderTree = async () => {
    try {
      loading.value = true;
      const result = await getFolderTreeApi();
      folderTree.value = result.data || [];
    } catch (error) {
      console.error('获取文件夹树失败:', error);
      ElMessage.error('获取文件夹树失败');
    } finally {
      loading.value = false;
    }
  };

  /**
   * 获取子文件夹列表
   */
  const fetchFolderChildren = async (parentId = null) => {
    try {
      const result = await getFolderChildrenApi(parentId);
      return result.data || [];
    } catch (error) {
      console.error('获取子文件夹失败:', error);
      return [];
    }
  };

  /**
   * 设置选中的文件夹
   */
  const setSelectedFolder = (folder) => {
    selectedFolder.value = folder;
  };

  /**
   * 展开/折叠文件夹
   */
  const toggleFolder = (folderId) => {
    if (expandedFolders.value.has(folderId)) {
      expandedFolders.value.delete(folderId);
    } else {
      expandedFolders.value.add(folderId);
    }
  };

  /**
   * 检查文件夹是否展开
   */
  const isFolderExpanded = (folderId) => {
    return expandedFolders.value.has(folderId);
  };

  // ==================== 文件夹操作 ====================

  /**
   * 创建文件夹
   */
  const createFolder = async (data) => {
    try {
      loading.value = true;
      
      const result = await createFolderApi(data);
      
      // 刷新文件夹树
      await fetchFolderTree();
      
      ElMessage.success('文件夹创建成功');
      return result.data;
    } catch (error) {
      console.error('创建文件夹失败:', error);
      ElMessage.error(error.response?.data?.msg || '创建文件夹失败');
      throw error;
    } finally {
      loading.value = false;
    }
  };

  /**
   * 重命名文件夹
   */
  const renameFolder = async (folderId, newName) => {
    try {
      loading.value = true;
      
      await renameFolderApi(folderId, { name: newName });
      
      // 更新本地树结构（无需重新加载）
      updateFolderInTree(folderId, { name: newName });
      
      // 如果重命名的是当前选中的文件夹，更新选中状态
      if (selectedFolder.value?.id === folderId) {
        selectedFolder.value.name = newName;
      }
      
      ElMessage.success('重命名成功');
    } catch (error) {
      console.error('重命名文件夹失败:', error);
      ElMessage.error(error.response?.data?.msg || '重命名失败');
      throw error;
    } finally {
      loading.value = false;
    }
  };

  /**
   * 移动文件夹
   */
  const moveFolder = async (folderId, targetParentId) => {
    try {
      loading.value = true;
      
      await moveFolderApi(folderId, { parentId: targetParentId });
      
      // 刷新文件夹树
      await fetchFolderTree();
      
      ElMessage.success('移动成功');
    } catch (error) {
      console.error('移动文件夹失败:', error);
      
      // 检测循环引用错误
      if (error.response?.status === 400) {
        ElMessage.error('不能将文件夹移动到自身或子文件夹下');
      } else {
        ElMessage.error(error.response?.data?.msg || '移动失败');
      }
      
      throw error;
    } finally {
      loading.value = false;
    }
  };

  /**
   * 删除文件夹
   */
  const deleteFolder = async (folderId, deleteNotes = false) => {
    try {
      // 确认删除
      await ElMessageBox.confirm(
        deleteNotes 
          ? '删除文件夹将同时删除其中的所有笔记，此操作不可恢复，确定继续吗？'
          : '删除文件夹后，其中的笔记将移到根目录，确定继续吗？',
        '删除文件夹',
        {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning',
          distinguishCancelAndClose: true
        }
      );
      
      loading.value = true;
      
      await deleteFolderApi(folderId, deleteNotes);
      
      // 刷新文件夹树
      await fetchFolderTree();
      
      // 如果删除的是当前选中的文件夹，切换到根目录
      if (selectedFolder.value?.id === folderId) {
        selectedFolder.value = null;
      }
      
      ElMessage.success('删除成功');
    } catch (error) {
      if (error === 'cancel' || error === 'close') {
        // 用户取消操作
        return;
      }
      
      console.error('删除文件夹失败:', error);
      ElMessage.error(error.response?.data?.msg || '删除失败');
      throw error;
    } finally {
      loading.value = false;
    }
  };

  // ==================== 工具函数 ====================

  /**
   * 更新树结构中的文件夹信息
   */
  const updateFolderInTree = (folderId, updates) => {
    const updateNode = (nodes) => {
      for (const node of nodes) {
        if (node.id === folderId) {
          Object.assign(node, updates);
          return true;
        }
        if (node.children && node.children.length > 0) {
          if (updateNode(node.children)) {
            return true;
          }
        }
      }
      return false;
    };
    
    updateNode(folderTree.value);
  };

  /**
   * 查找文件夹节点
   */
  const findFolderNode = (folderId) => {
    const findNode = (nodes) => {
      for (const node of nodes) {
        if (node.id === folderId) {
          return node;
        }
        if (node.children && node.children.length > 0) {
          const found = findNode(node.children);
          if (found) return found;
        }
      }
      return null;
    };
    
    return findNode(folderTree.value);
  };

  /**
   * 获取文件夹路径（面包屑）
   */
  const getFolderPath = (folderId) => {
    const path = [];
    
    const findPath = (nodes, targetId, currentPath = []) => {
      for (const node of nodes) {
        const newPath = [...currentPath, node];
        
        if (node.id === targetId) {
          path.push(...newPath);
          return true;
        }
        
        if (node.children && node.children.length > 0) {
          if (findPath(node.children, targetId, newPath)) {
            return true;
          }
        }
      }
      return false;
    };
    
    findPath(folderTree.value, folderId);
    return path;
  };

  return {
    // 状态
    folderTree,
    selectedFolder,
    expandedFolders,
    loading,
    selectedFolderId,
    isRootSelected,
    
    // 方法
    fetchFolderTree,
    fetchFolderChildren,
    setSelectedFolder,
    toggleFolder,
    isFolderExpanded,
    createFolder,
    renameFolder,
    moveFolder,
    deleteFolder,
    updateFolderInTree,
    findFolderNode,
    getFolderPath
  };
});
