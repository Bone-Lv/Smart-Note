<template>
  <div class="note-manager">
    <div class="manager-header">
      <h2>我的笔记</h2>
      <div class="manager-actions">
        <button @click="showCreateNoteModal = true" class="create-btn">
          <i class="fas fa-plus"></i>
          新建笔记
        </button>
        <button @click="refreshNotes" class="refresh-btn">
          <i class="fas fa-sync-alt"></i>
        </button>
      </div>
    </div>
    
    <div class="manager-content">
      <!-- 文件夹树 -->
      <div class="folder-tree">
        <div class="tree-header">
          <h3>文件夹</h3>
          <button @click="showCreateFolderModal = true" class="add-folder-btn" title="新建文件夹">
            <i class="fas fa-plus"></i>
          </button>
        </div>
        <div class="tree-content">
          <div 
            class="tree-item" 
            :class="{ active: selectedFolderId === null }"
            @click="selectFolder(null)"
          >
            <i class="fas fa-home"></i>
            <span>全部笔记</span>
            <span class="item-count">{{ rootNoteCount }}</span>
          </div>
          <div 
            v-for="folder in folderTree" 
            :key="folder.id" 
            class="tree-item"
            :class="{ active: selectedFolderId === folder.id }"
            @click="selectFolder(folder.id)"
          >
            <i class="fas fa-folder"></i>
            <span>{{ folder.name }}</span>
            <span class="item-count">{{ folder.noteCount }}</span>
          </div>
        </div>
      </div>
      
      <!-- 笔记列表 -->
      <div class="notes-list">
        <div class="list-header">
          <div class="search-box">
            <i class="fas fa-search"></i>
            <input 
              v-model="searchKeyword" 
              type="text" 
              placeholder="搜索笔记标题或标签..."
              @keyup.enter="searchNotes"
            >
          </div>
          <div class="sort-options">
            <select v-model="sortOrder" @change="loadNotes">
              <option value="time">按时间倒序</option>
              <option value="title">按标题排序</option>
            </select>
          </div>
        </div>
        
        <div class="notes-grid">
          <div 
            v-for="note in notes" 
            :key="note.id" 
            class="note-card"
            @click="goToNote(note.id)"
          >
            <div class="note-header">
              <h3>{{ note.title }}</h3>
              <div class="note-actions">
                <button @click.stop="showNoteOptions(note)" class="options-btn">
                  <i class="fas fa-ellipsis-v"></i>
                </button>
              </div>
            </div>
            <p class="note-preview">{{ truncateContent(note.content, 100) }}</p>
            <div class="note-footer">
              <span class="note-date">{{ formatDate(note.updateTime) }}</span>
              <div class="note-tags" v-if="note.tags">
                <span 
                  v-for="tag in note.tags.split(',')" 
                  :key="tag" 
                  class="tag"
                >
                  #{{ tag.trim() }}
                </span>
              </div>
            </div>
          </div>
          
          <div v-if="notes.length === 0 && !loading" class="empty-state">
            <i class="fas fa-book-open"></i>
            <p>暂无笔记</p>
            <button @click="showCreateNoteModal = true" class="create-first-btn">
              创建第一篇笔记
            </button>
          </div>
        </div>
        
        <!-- 加载更多 -->
        <div v-if="hasMore" class="load-more">
          <button @click="loadMore" :disabled="loading">
            <span v-if="!loading">加载更多</span>
            <span v-else>加载中...</span>
          </button>
        </div>
      </div>
    </div>
    
    <!-- 创建笔记模态框 -->
    <div v-if="showCreateNoteModal" class="modal-overlay" @click="showCreateNoteModal = false">
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <h3>新建笔记</h3>
          <button @click="showCreateNoteModal = false" class="close-btn">×</button>
        </div>
        <form @submit.prevent="createNewNote" class="modal-form">
          <div class="form-group">
            <label for="noteTitle">标题</label>
            <input 
              id="noteTitle" 
              v-model="newNote.title" 
              type="text" 
              placeholder="请输入笔记标题"
              required
            >
          </div>
          <div class="form-group">
            <label for="noteFolder">文件夹</label>
            <select v-model="newNote.folderId">
              <option :value="null">无文件夹</option>
              <option 
                v-for="folder in folderTree" 
                :key="folder.id" 
                :value="folder.id"
              >
                {{ folder.name }}
              </option>
            </select>
          </div>
          <div class="form-group">
            <label for="noteTags">标签</label>
            <input 
              id="noteTags" 
              v-model="newNote.tags" 
              type="text" 
              placeholder="多个标签用逗号分隔"
            >
          </div>
          <div class="form-actions">
            <button type="submit" :disabled="creatingNote">创建</button>
            <button type="button" @click="showCreateNoteModal = false">取消</button>
          </div>
        </form>
      </div>
    </div>
    
    <!-- 创建文件夹模态框 -->
    <div v-if="showCreateFolderModal" class="modal-overlay" @click="showCreateFolderModal = false">
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <h3>新建文件夹</h3>
          <button @click="showCreateFolderModal = false" class="close-btn">×</button>
        </div>
        <form @submit.prevent="createNewFolder" class="modal-form">
          <div class="form-group">
            <label for="folderName">文件夹名称</label>
            <input 
              id="folderName" 
              v-model="newFolder.name" 
              type="text" 
              placeholder="请输入文件夹名称"
              required
            >
          </div>
          <div class="form-group">
            <label for="parentFolder">父文件夹</label>
            <select v-model="newFolder.parentId">
              <option :value="null">无父文件夹</option>
              <option 
                v-for="folder in folderTree" 
                :key="folder.id" 
                :value="folder.id"
              >
                {{ folder.name }}
              </option>
            </select>
          </div>
          <div class="form-actions">
            <button type="submit" :disabled="creatingFolder">创建</button>
            <button type="button" @click="showCreateFolderModal = false">取消</button>
          </div>
        </form>
      </div>
    </div>
    
    <!-- 笔记选项弹窗 -->
    <div v-if="showNoteOptionsPanel" class="options-panel" :style="optionsPanelStyle">
      <div class="options-item" @click="editNote(currentNote)">
        <i class="fas fa-edit"></i>
        编辑
      </div>
      <div class="options-item" @click="duplicateNote(currentNote)">
        <i class="fas fa-copy"></i>
        复制
      </div>
      <div class="options-item" @click="moveNoteToFolder(currentNote)">
        <i class="fas fa-folder"></i>
        移动到文件夹
      </div>
      <div class="options-item" @click="shareNote(currentNote)">
        <i class="fas fa-share-alt"></i>
        分享
      </div>
      <div class="options-item danger" @click="deleteNote(currentNote)">
        <i class="fas fa-trash"></i>
        删除
      </div>
    </div>
    
    <!-- 加载遮罩 -->
    <div v-if="loading" class="loading-overlay">
      <div class="loading-spinner"></div>
    </div>
  </div>
</template>

<script>
import { ref, onMounted, watch } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { useUserStore } from '../../stores/userStore.js';
import { useNoteEditor } from '../../hooks/useNoteEditor.js';
import { 
  getNoteListApi, 
  createNoteApi, 
  deleteNoteApi,
  generateShareLinkApi
} from '../../api/note.js';
import { 
  getFolderTreeApi, 
  createFolderApi 
} from '../../api/folder.js';

export default {
  name: 'NoteManager',
  setup() {
    const router = useRouter();
    const userStore = useUserStore();
    
    // 状态变量
    const notes = ref([]);
    const folderTree = ref([]);
    const selectedFolderId = ref(null);
    const searchKeyword = ref('');
    const sortOrder = ref('time');
    const loading = ref(false);
    const hasMore = ref(true);
    const nextCursor = ref(null);
    
    // 模态框状态
    const showCreateNoteModal = ref(false);
    const showCreateFolderModal = ref(false);
    
    // 新建笔记/文件夹数据
    const newNote = ref({
      title: '',
      content: '# 新笔记\n\n在这里开始写作...',
      folderId: null,
      tags: ''
    });
    
    const newFolder = ref({
      name: '',
      parentId: null
    });
    
    const creatingNote = ref(false);
    const creatingFolder = ref(false);
    
    // 笔记选项面板
    const showNoteOptionsPanel = ref(false);
    const currentNote = ref(null);
    const optionsPanelStyle = ref({});
    
    // 统计数据
    const rootNoteCount = ref(0);
    
    // 加载笔记列表
    const loadNotes = async (reset = true) => {
      console.log('📝 开始加载笔记列表...', { reset, folderId: selectedFolderId.value });
      
      if (reset) {
        loading.value = true;
        notes.value = [];
        nextCursor.value = null;
        hasMore.value = true;
      }
      
      try {
        const queryDTO = {
          page: reset ? 1 : undefined,
          pageSize: 20,
          cursor: reset ? undefined : nextCursor.value,
          keyword: searchKeyword.value || undefined,
          sortOrder: sortOrder.value,
          folderId: selectedFolderId.value
        };
        
        console.log('📡 发送请求:', queryDTO);
        const response = await getNoteListApi(queryDTO);
        console.log('📥 收到响应:', response);
        
        const result = response.data.data;
        
        if (reset) {
          notes.value = result.records || [];
        } else {
          notes.value = [...notes.value, ...(result.records || [])];
        }
        
        nextCursor.value = result.nextCursor;
        hasMore.value = result.hasNext;
        
        console.log('✅ 笔记加载完成，共', notes.value.length, '条');
      } catch (error) {
        console.error('❌ 加载笔记失败:', error);
        ElMessage.error('加载笔记失败');
      } finally {
        loading.value = false;
      }
    };
    
    // 加载更多
    const loadMore = () => {
      if (!hasMore.value || loading.value) return;
      loadNotes(false);
    };
    
    // 加载文件夹树
    const loadFolders = async () => {
      try {
        const response = await getFolderTreeApi();
        folderTree.value = response.data.data || [];
        
        // 计算根目录笔记数
        rootNoteCount.value = folderTree.value.reduce((count, folder) => count + folder.noteCount, 0);
      } catch (error) {
        console.error('❌ 加载文件夹失败:', error);
        ElMessage.error('加载文件夹失败');
      }
    };
    
    // 创建新笔记
    const createNewNote = async () => {
      if (!newNote.value.title.trim()) {
        ElMessage.warning('请输入笔记标题');
        return;
      }
      
      creatingNote.value = true;
      
      try {
        await createNoteApi(newNote.value);
        ElMessage.success('✅ 笔记创建成功！');
        showCreateNoteModal.value = false;
        newNote.value = {
          title: '',
          content: '# 新笔记\n\n在这里开始写作...',
          folderId: null,
          tags: ''
        };
        
        // 重新加载笔记
        await loadNotes();
      } catch (error) {
        console.error('❌ 创建笔记失败:', error);
        ElMessage.error('创建笔记失败');
      } finally {
        creatingNote.value = false;
      }
    };
    
    // 创建新文件夹
    const createNewFolder = async () => {
      if (!newFolder.value.name.trim()) {
        ElMessage.warning('请输入文件夹名称');
        return;
      }
      
      creatingFolder.value = true;
      
      try {
        await createFolderApi(newFolder.value);
        ElMessage.success('✅ 文件夹创建成功！');
        showCreateFolderModal.value = false;
        newFolder.value = {
          name: '',
          parentId: null
        };
        
        // 重新加载文件夹
        await loadFolders();
      } catch (error) {
        console.error('❌ 创建文件夹失败:', error);
        ElMessage.error('创建文件夹失败');
      } finally {
        creatingFolder.value = false;
      }
    };
    
    // 选择文件夹
    const selectFolder = (folderId) => {
      selectedFolderId.value = folderId;
      loadNotes();
    };
    
    // 搜索笔记
    const searchNotes = () => {
      loadNotes();
    };
    
    // 刷新笔记
    const refreshNotes = () => {
      loadNotes();
    };
    
    // 跳转到笔记详情
    const goToNote = (noteId) => {
      router.push(`/note/${noteId}`);
    };
    
    // 显示笔记选项
    const showNoteOptions = (note, event) => {
      currentNote.value = note;
      showNoteOptionsPanel.value = true;
      
      // 计算选项面板位置
      const rect = event.target.getBoundingClientRect();
      optionsPanelStyle.value = {
        top: `${rect.bottom + 5}px`,
        left: `${rect.left}px`
      };
    };
    
    // 关闭选项面板
    const closeOptionsPanel = (event) => {
      if (!event.target.closest('.options-panel') && !event.target.classList.contains('options-btn')) {
        showNoteOptionsPanel.value = false;
      }
    };
    
    // 编辑笔记
    const editNote = (note) => {
      goToNote(note.id);
      showNoteOptionsPanel.value = false;
    };
    
    // 复制笔记
    const duplicateNote = async (note) => {
      try {
        const duplicateData = {
          title: `${note.title} (副本)`,
          content: note.content,
          tags: note.tags,
          folderId: note.folderId
        };
        await createNoteApi(duplicateData);
        ElMessage.success('✅ 笔记复制成功！');
        await loadNotes();
      } catch (error) {
        console.error('❌ 复制笔记失败:', error);
        ElMessage.error('复制笔记失败');
      }
      showNoteOptionsPanel.value = false;
    };
    
    // 移动到文件夹
    const moveNoteToFolder = (note) => {
      ElMessage.info('移动笔记功能正在开发中');
      showNoteOptionsPanel.value = false;
    };
    
    // 分享笔记
    const shareNote = async (note) => {
      try {
        const response = await generateShareLinkApi(note.id);
        const shareInfo = response.data.data;
        
        // 复制到剪贴板
        const shareUrl = shareInfo.shareUrl || `${window.location.origin}/note/shared/${note.id}`;
        await navigator.clipboard.writeText(shareUrl);
        
        if (shareInfo.shareCode) {
          ElMessage.success('✅ 分享码已复制到剪贴板！');
        } else {
          ElMessage.success('✅ 分享链接已复制到剪贴板！');
        }
      } catch (error) {
        console.error('❌ 分享笔记失败:', error);
        ElMessage.error('分享笔记失败');
      }
      showNoteOptionsPanel.value = false;
    };
    
    // 删除笔记
    const deleteNote = async (note) => {
      if (confirm(`确定要删除笔记"${note.title}"吗？`)) {
        try {
          await deleteNoteApi(note.id);
          ElMessage.success('✅ 笔记已删除！');
          await loadNotes();
        } catch (error) {
          console.error('❌ 删除笔记失败:', error);
          ElMessage.error('删除笔记失败');
        }
      }
      showNoteOptionsPanel.value = false;
    };
    
    // 截断内容
    const truncateContent = (content, maxLength) => {
      if (!content) return '';
      return content.length > maxLength ? content.substring(0, maxLength) + '...' : content;
    };
    
    // 格式化日期
    const formatDate = (dateString) => {
      if (!dateString) return '';
      return new Date(dateString).toLocaleDateString('zh-CN');
    };
    
    // 监听点击外部关闭选项面板
    watch(showNoteOptionsPanel, (newValue) => {
      if (newValue) {
        document.addEventListener('click', closeOptionsPanel);
      } else {
        document.removeEventListener('click', closeOptionsPanel);
      }
    });
    
    onMounted(() => {
      console.log('✅ NoteManager 组件已挂载');
      loadNotes();
      loadFolders();
    });
    
    // 监听文件夹选择变化
    watch(selectedFolderId, () => {
      loadNotes();
    });
    
    // 监听搜索关键词变化（防抖）
    let searchTimer = null;
    watch(searchKeyword, (newVal) => {
      if (searchTimer) clearTimeout(searchTimer);
      searchTimer = setTimeout(() => {
        loadNotes();
      }, 500);
    });
    
    return {
      notes,
      folderTree,
      selectedFolderId,
      searchKeyword,
      sortOrder,
      loading,
      hasMore,
      showCreateNoteModal,
      showCreateFolderModal,
      newNote,
      newFolder,
      creatingNote,
      creatingFolder,
      showNoteOptionsPanel,
      currentNote,
      optionsPanelStyle,
      rootNoteCount,
      loadNotes,
      loadMore,
      createNewNote,
      createNewFolder,
      selectFolder,
      searchNotes,
      refreshNotes,
      goToNote,
      showNoteOptions,
      editNote,
      duplicateNote,
      moveNoteToFolder,
      shareNote,
      deleteNote,
      truncateContent,
      formatDate
    };
  }
};
</script>

<style scoped>
.note-manager {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: #f5f7fa;
}

.manager-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 24px;
  background: white;
  border-bottom: 1px solid #eee;
  flex-shrink: 0;
}

.manager-header h2 {
  margin: 0;
  color: #333;
}

.manager-actions {
  display: flex;
  gap: 12px;
}

.create-btn, .refresh-btn {
  padding: 8px 16px;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
  display: flex;
  align-items: center;
  gap: 6px;
}

.create-btn {
  background: #409eff;
  color: white;
}

.create-btn:hover {
  background: #66b1ff;
}

.refresh-btn {
  background: #f5f7fa;
  color: #666;
  border: 1px solid #dcdfe6;
}

.refresh-btn:hover {
  background: #ecf5ff;
  color: #409eff;
}

.manager-content {
  display: flex;
  gap: 20px;
  flex: 1;
  overflow: hidden;
}

.folder-tree {
  width: 240px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.tree-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  border-bottom: 1px solid #eee;
}

.tree-header h3 {
  margin: 0;
  color: #333;
}

.add-folder-btn {
  background: none;
  border: none;
  color: #409eff;
  cursor: pointer;
  font-size: 16px;
}

.tree-content {
  flex: 1;
  overflow-y: auto;
  padding: 8px 0;
}

.tree-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  cursor: pointer;
  transition: background 0.3s;
}

.tree-item:hover {
  background: #f5f7fa;
}

.tree-item.active {
  background: #ecf5ff;
  color: #409eff;
}

.item-count {
  margin-left: auto;
  font-size: 12px;
  color: #999;
  background: #f0f0f0;
  padding: 2px 6px;
  border-radius: 10px;
}

.notes-list {
  flex: 1;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.list-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  border-bottom: 1px solid #eee;
  gap: 16px;
}

.search-box {
  flex: 1;
  position: relative;
}

.search-box i {
  position: absolute;
  left: 12px;
  top: 50%;
  transform: translateY(-50%);
  color: #999;
}

.search-box input {
  width: 100%;
  padding: 8px 12px 8px 36px;
  border: 1px solid #ddd;
  border-radius: 6px;
  font-size: 14px;
}

.sort-options select {
  padding: 8px 12px;
  border: 1px solid #ddd;
  border-radius: 6px;
  font-size: 14px;
}

.notes-grid {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 16px;
}

.note-card {
  background: #fafafa;
  border-radius: 8px;
  padding: 16px;
  cursor: pointer;
  transition: transform 0.3s, box-shadow 0.3s;
  border: 1px solid #eee;
}

.note-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 16px rgba(0,0,0,0.15);
  border-color: #409eff;
}

.note-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 8px;
}

.note-header h3 {
  margin: 0;
  color: #333;
  font-size: 16px;
  flex: 1;
  word-break: break-word;
}

.options-btn {
  background: none;
  border: none;
  color: #999;
  cursor: pointer;
  padding: 4px;
  border-radius: 4px;
}

.options-btn:hover {
  background: #f0f0f0;
  color: #333;
}

.note-preview {
  margin: 0 0 12px 0;
  color: #666;
  font-size: 14px;
  line-height: 1.5;
  word-break: break-word;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
  /* 标准属性（现代浏览器支持） */
  display: box;
  line-clamp: 3;
  box-orient: vertical;
}

.note-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12px;
  color: #999;
}

.note-tags {
  display: flex;
  gap: 4px;
  flex-wrap: wrap;
}

.tag {
  background: #e1f0ff;
  color: #409eff;
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 11px;
}

.empty-state {
  grid-column: 1 / -1;
  text-align: center;
  padding: 60px 20px;
  color: #999;
}

.empty-state i {
  font-size: 48px;
  margin-bottom: 16px;
  opacity: 0.5;
}

.empty-state p {
  margin: 0 0 16px 0;
  font-size: 16px;
}

.create-first-btn {
  padding: 10px 20px;
  background: #409eff;
  color: white;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
}

.create-first-btn:hover {
  background: #66b1ff;
}

.load-more {
  grid-column: 1 / -1;
  text-align: center;
  padding: 20px;
}

.load-more button {
  padding: 8px 20px;
  background: #f5f7fa;
  color: #666;
  border: 1px solid #dcdfe6;
  border-radius: 6px;
  cursor: pointer;
}

.load-more button:hover:not(:disabled) {
  background: #ecf5ff;
  color: #409eff;
}

.load-more button:disabled {
  background: #f5f7fa;
  color: #ccc;
  cursor: not-allowed;
}

.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
}

.modal-content {
  background: white;
  border-radius: 8px;
  width: 90%;
  max-width: 400px;
  max-height: 80vh;
  overflow-y: auto;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 24px;
  border-bottom: 1px solid #eee;
}

.modal-header h3 {
  margin: 0;
  color: #333;
}

.close-btn {
  background: none;
  border: none;
  font-size: 24px;
  cursor: pointer;
  color: #999;
  padding: 0;
  width: 30px;
  height: 30px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.close-btn:hover {
  color: #333;
}

.modal-form {
  padding: 24px;
}

.form-group {
  margin-bottom: 20px;
}

.form-group label {
  display: block;
  margin-bottom: 6px;
  color: #333;
  font-weight: 500;
}

.form-group input,
.form-group select {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid #ddd;
  border-radius: 6px;
  font-size: 14px;
}

.form-actions {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
  margin-top: 24px;
}

.form-actions button {
  padding: 8px 16px;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
}

.form-actions button:first-child {
  background: #409eff;
  color: white;
}

.form-actions button:first-child:hover:not(:disabled) {
  background: #66b1ff;
}

.form-actions button:last-child {
  background: #f5f7fa;
  color: #666;
  border: 1px solid #dcdfe6;
}

.form-actions button:last-child:hover {
  background: #ecf5ff;
  color: #409eff;
}

.options-panel {
  position: fixed;
  background: white;
  border-radius: 6px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.15);
  z-index: 1001;
  min-width: 150px;
}

.options-item {
  padding: 12px 16px;
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  transition: background 0.3s;
}

.options-item:hover {
  background: #f5f7fa;
}

.options-item.danger {
  color: #f56c6c;
}

.options-item.danger:hover {
  background: #fef0f0;
}

.loading-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(255, 255, 255, 0.8);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 2000;
}

.loading-spinner {
  width: 40px;
  height: 40px;
  border: 4px solid #e0e0e0;
  border-top: 4px solid #409eff;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}
</style>