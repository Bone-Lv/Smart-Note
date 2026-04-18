import { defineStore } from 'pinia';
import { ref, computed } from 'vue';
import { 
  getRecycleBinNotesApi,
  getRecycleBinFoldersApi,
  restoreNoteApi,
  restoreFolderApi,
  permanentlyDeleteNoteApi,
  permanentlyDeleteFolderApi,
  emptyRecycleBinApi
} from '../api/recycleBin.js';
import { ElMessage, ElMessageBox } from 'element-plus';

export const useRecycleBinStore = defineStore('recycleBin', () => {
  // 状态
  const notes = ref([]); // 回收站笔记列表
  const folders = ref([]); // 回收站文件夹列表
  const activeTab = ref('notes'); // 当前激活的标签页（notes/folders）
  const loading = ref(false); // 加载状态

  // 计算属性
  const totalItems = computed(() => notes.value.length + folders.value.length);

  // ==================== 数据获取 ====================

  /**
   * 获取回收站笔记列表
   */
  const fetchNotes = async () => {
    try {
      loading.value = true;
      const result = await getRecycleBinNotesApi();
      notes.value = result.data || [];
    } catch (error) {
      console.error('获取回收站笔记失败:', error);
      ElMessage.error('获取回收站笔记失败');
    } finally {
      loading.value = false;
    }
  };

  /**
   * 获取回收站文件夹列表
   */
  const fetchFolders = async () => {
    try {
      loading.value = true;
      const result = await getRecycleBinFoldersApi();
      folders.value = result.data || [];
    } catch (error) {
      console.error('获取回收站文件夹失败:', error);
      ElMessage.error('获取回收站文件夹失败');
    } finally {
      loading.value = false;
    }
  };

  /**
   * 刷新所有数据
   */
  const refreshAll = async () => {
    await Promise.all([fetchNotes(), fetchFolders()]);
  };

  // ==================== 还原操作 ====================

  /**
   * 还原笔记
   */
  const restoreNote = async (noteId) => {
    try {
      loading.value = true;
      
      await restoreNoteApi(noteId);
      
      // 从列表中移除
      notes.value = notes.value.filter(n => n.id !== noteId);
      
      ElMessage.success('笔记已还原');
    } catch (error) {
      console.error('还原笔记失败:', error);
      ElMessage.error(error.response?.data?.msg || '还原失败');
      throw error;
    } finally {
      loading.value = false;
    }
  };

  /**
   * 还原文件夹
   */
  const restoreFolder = async (folderId) => {
    try {
      loading.value = true;
      
      await restoreFolderApi(folderId);
      
      // 从列表中移除
      folders.value = folders.value.filter(f => f.id !== folderId);
      
      ElMessage.success('文件夹已还原');
    } catch (error) {
      console.error('还原文件夹失败:', error);
      ElMessage.error(error.response?.data?.msg || '还原失败');
      throw error;
    } finally {
      loading.value = false;
    }
  };

  // ==================== 彻底删除 ====================

  /**
   * 彻底删除笔记
   */
  const permanentlyDeleteNote = async (noteId) => {
    try {
      // 二次确认
      await ElMessageBox.confirm(
        '此操作将永久删除该笔记，无法恢复，确定继续吗？',
        '彻底删除',
        {
          confirmButtonText: '确定删除',
          cancelButtonText: '取消',
          type: 'warning',
          confirmButtonClass: 'el-button--danger'
        }
      );
      
      loading.value = true;
      
      await permanentlyDeleteNoteApi(noteId);
      
      // 从列表中移除
      notes.value = notes.value.filter(n => n.id !== noteId);
      
      ElMessage.success('笔记已彻底删除');
    } catch (error) {
      if (error === 'cancel' || error === 'close') {
        // 用户取消操作
        return;
      }
      
      console.error('彻底删除笔记失败:', error);
      ElMessage.error(error.response?.data?.msg || '删除失败');
      throw error;
    } finally {
      loading.value = false;
    }
  };

  /**
   * 彻底删除文件夹
   */
  const permanentlyDeleteFolder = async (folderId) => {
    try {
      // 二次确认
      await ElMessageBox.confirm(
        '此操作将永久删除该文件夹及其中的所有笔记，无法恢复，确定继续吗？',
        '彻底删除',
        {
          confirmButtonText: '确定删除',
          cancelButtonText: '取消',
          type: 'warning',
          confirmButtonClass: 'el-button--danger'
        }
      );
      
      loading.value = true;
      
      await permanentlyDeleteFolderApi(folderId);
      
      // 从列表中移除
      folders.value = folders.value.filter(f => f.id !== folderId);
      
      ElMessage.success('文件夹已彻底删除');
    } catch (error) {
      if (error === 'cancel' || error === 'close') {
        // 用户取消操作
        return;
      }
      
      console.error('彻底删除文件夹失败:', error);
      ElMessage.error(error.response?.data?.msg || '删除失败');
      throw error;
    } finally {
      loading.value = false;
    }
  };

  // ==================== 清空回收站 ====================

  /**
   * 清空回收站
   */
  const emptyRecycleBin = async () => {
    try {
      // 强烈警告提示
      await ElMessageBox.confirm(
        `回收站中共有 ${totalItems.value} 个项目，清空后将永久删除所有内容且无法恢复！\n\n此操作不可撤销，确定要继续吗？`,
        '清空回收站',
        {
          confirmButtonText: '确定清空',
          cancelButtonText: '取消',
          type: 'warning',
          confirmButtonClass: 'el-button--danger',
          distinguishCancelAndClose: true
        }
      );
      
      loading.value = true;
      
      await emptyRecycleBinApi();
      
      // 清空本地列表
      notes.value = [];
      folders.value = [];
      
      ElMessage.success('回收站已清空');
    } catch (error) {
      if (error === 'cancel' || error === 'close') {
        // 用户取消操作
        return;
      }
      
      console.error('清空回收站失败:', error);
      ElMessage.error(error.response?.data?.msg || '清空失败');
      throw error;
    } finally {
      loading.value = false;
    }
  };

  // ==================== 工具函数 ====================

  /**
   * 设置激活的标签页
   */
  const setActiveTab = (tab) => {
    activeTab.value = tab;
  };

  /**
   * 格式化删除时间
   */
  const formatDeleteTime = (deleteTime) => {
    if (!deleteTime) return '';
    
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

  return {
    // 状态
    notes,
    folders,
    activeTab,
    loading,
    totalItems,
    
    // 方法
    fetchNotes,
    fetchFolders,
    refreshAll,
    restoreNote,
    restoreFolder,
    permanentlyDeleteNote,
    permanentlyDeleteFolder,
    emptyRecycleBin,
    setActiveTab,
    formatDeleteTime
  };
});
