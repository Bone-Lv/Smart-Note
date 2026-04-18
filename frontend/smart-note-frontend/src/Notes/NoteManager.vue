<template>
  <div class="note-manager">
    <div class="sidebar">
      <div class="sidebar-header">
        <h3>笔记管理</h3>
        <button class="btn btn-primary" @click="createNewNote">新建笔记</button>
      </div>
      
      <div class="folder-tree">
        <div class="folder-item" :class="{ active: currentFolderId === null }" @click="selectFolder(null)">
          <span class="folder-icon">📁</span>
          <span class="folder-name">全部笔记</span>
          <span class="folder-count">{{ totalNotes }}</span>
        </div>
        
        <div 
          v-for="folder in folderTree" 
          :key="folder.id"
          class="folder-item"
          :class="{ active: currentFolderId === folder.id }"
          @click="selectFolder(folder.id)"
        >
          <span class="folder-icon">📂</span>
          <span class="folder-name">{{ folder.name }}</span>
          <span class="folder-count">{{ folder.noteCount }}</span>
        </div>
      </div>
      
      <div class="search-section">
        <input 
          type="text" 
          v-model="searchKeyword" 
          placeholder="搜索笔记..."
          class="search-input"
        />
      </div>
    </div>
    
    <div class="main-content">
      <div class="toolbar">
        <div class="view-options">
          <button 
            :class="['view-btn', { active: viewMode === 'list' }]" 
            @click="viewMode = 'list'"
          >
            列表
          </button>
          <button 
            :class="['view-btn', { active: viewMode === 'grid' }]" 
            @click="viewMode = 'grid'"
          >
            网格
          </button>
        </div>
        
        <div class="sort-options">
          <select v-model="sortOrder" @change="loadNotes">
            <option value="time">按时间排序</option>
            <option value="title">按标题排序</option>
          </select>
        </div>
      </div>
      
      <div v-if="loading" class="loading">
        加载中...
      </div>
      
      <div v-else-if="notes.length === 0" class="empty-state">
        <p>暂无笔记</p>
        <button class="btn btn-primary" @click="createNewNote">创建第一篇笔记</button>
      </div>
      
      <div v-else :class="['notes-container', viewMode]">
        <div 
          v-for="note in notes" 
          :key="note.id"
          class="note-card"
          @click="viewNote(note.id)"
        >
          <div class="note-header">
            <h4 class="note-title">{{ note.title }}</h4>
            <div class="note-actions">
              <button class="action-btn" @click.stop="editNote(note.id)">✏️</button>
              <button class="action-btn" @click.stop="deleteNote(note.id)">🗑️</button>
            </div>
          </div>
          
          <div class="note-preview">
            {{ note.content.substring(0, 100) }}{{ note.content.length > 100 ? '...' : '' }}
          </div>
          
          <div class="note-meta">
            <span class="note-date">{{ formatDate(note.createTime) }}</span>
            <span class="note-tags">
              <span 
                v-for="tag in (note.tags || '').split(',').filter(t => t)" 
                :key="tag" 
                class="tag"
              >
                #{{ tag }}
              </span>
            </span>
          </div>
        </div>
      </div>
      
      <div v-if="hasMore" class="load-more">
        <button class="btn btn-secondary" @click="loadMoreNotes" :disabled="loading">
          {{ loading ? '加载中...' : '加载更多' }}
        </button>
      </div>
    </div>
  </div>
</template>

<script>
import { noteApi } from '../../api/note.js';
import { useCursorPagination } from '../../hooks/useCursorPagination.js';

export default {
  name: 'NoteManager',
  data() {
    return {
      folderTree: [],
      notes: [],
      currentFolderId: null,
      searchKeyword: '',
      sortOrder: 'time',
      viewMode: 'list',
      loading: false,
      totalNotes: 0,
      nextCursor: null,
      hasMore: true
    };
  },
  async mounted() {
    await this.loadFolders();
    await this.loadNotes();
  },
  methods: {
    async loadFolders() {
      try {
        const response = await noteApi.getFolderTree();
        this.folderTree = response.data || [];
      } catch (error) {
        console.error('加载文件夹失败:', error);
      }
    },

    async loadNotes() {
      this.loading = true;
      try {
        const queryDTO = {
          page: 1,
          pageSize: 20,
          keyword: this.searchKeyword || undefined,
          sortOrder: this.sortOrder,
          folderId: this.currentFolderId
        };

        const response = await noteApi.getNoteList(queryDTO);
        this.notes = response.data?.records || [];
        this.totalNotes = response.data?.total || 0;
        this.nextCursor = response.data?.nextCursor || null;
        this.hasMore = response.data?.hasNext || false;
      } catch (error) {
        console.error('加载笔记失败:', error);
      } finally {
        this.loading = false;
      }
    },

    async loadMoreNotes() {
      if (!this.hasMore || this.loading) return;

      this.loading = true;
      try {
        const queryDTO = {
          pageSize: 20,
          cursor: this.nextCursor,
          keyword: this.searchKeyword || undefined,
          sortOrder: this.sortOrder,
          folderId: this.currentFolderId
        };

        const response = await noteApi.getNoteList(queryDTO);
        this.notes = [...this.notes, ...(response.data?.records || [])];
        this.nextCursor = response.data?.nextCursor || null;
        this.hasMore = response.data?.hasNext || false;
      } catch (error) {
        console.error('加载更多笔记失败:', error);
      } finally {
        this.loading = false;
      }
    },

    selectFolder(folderId) {
      this.currentFolderId = folderId;
      this.loadNotes();
    },

    createNewNote() {
      // 创建新笔记并跳转到编辑页面
      this.$router.push('/notes/new');
    },

    viewNote(noteId) {
      this.$router.push(`/notes/${noteId}`);
    },

    editNote(noteId) {
      this.$router.push(`/notes/${noteId}/edit`);
    },

    async deleteNote(noteId) {
      if (!confirm('确定要删除这篇笔记吗？删除后会进入回收站，5分钟后自动彻底删除。')) {
        return;
      }

      try {
        await noteApi.deleteNote(noteId);
        this.notes = this.notes.filter(note => note.id !== noteId);
        this.totalNotes--;
        alert('笔记已删除');
      } catch (error) {
        alert('删除笔记失败: ' + error.message);
      }
    },

    formatDate(dateString) {
      if (!dateString) return '';
      const date = new Date(dateString);
      return date.toLocaleDateString();
    }
  },
  watch: {
    searchKeyword() {
      this.loadNotes();
    }
  }
};
</script>

<style scoped>
.note-manager {
  display: flex;
  height: 100%;
  gap: 20px;
}

.sidebar {
  width: 280px;
  background: #f6f8fa;
  border-radius: 8px;
  padding: 20px;
  height: fit-content;
}

.sidebar-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.sidebar-header h3 {
  margin: 0;
  color: #24292f;
}

.folder-tree {
  margin-bottom: 20px;
}

.folder-item {
  display: flex;
  align-items: center;
  padding: 10px 12px;
  border-radius: 6px;
  cursor: pointer;
  margin-bottom: 4px;
  transition: background-color 0.2s;
}

.folder-item:hover {
  background: #eaecef;
}

.folder-item.active {
  background: #0969da;
  color: white;
}

.folder-icon {
  margin-right: 8px;
}

.folder-name {
  flex: 1;
}

.folder-count {
  background: #d0d7de;
  border-radius: 12px;
  padding: 2px 8px;
  font-size: 12px;
}

.search-input {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #d0d7de;
  border-radius: 6px;
  font-size: 14px;
}

.main-content {
  flex: 1;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding: 0 10px;
}

.view-options {
  display: flex;
  gap: 8px;
}

.view-btn {
  padding: 6px 12px;
  border: 1px solid #d0d7de;
  background: white;
  border-radius: 4px;
  cursor: pointer;
}

.view-btn.active {
  background: #0969da;
  color: white;
  border-color: #0969da;
}

.sort-options select {
  padding: 6px 12px;
  border: 1px solid #d0d7de;
  border-radius: 4px;
}

.notes-container {
  display: grid;
  gap: 16px;
}

.notes-container.list {
  grid-template-columns: 1fr;
}

.notes-container.grid {
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
}

.note-card {
  background: white;
  border: 1px solid #e1e5e9;
  border-radius: 8px;
  padding: 16px;
  cursor: pointer;
  transition: box-shadow 0.2s;
}

.note-card:hover {
  box-shadow: 0 4px 12px rgba(0,0,0,0.1);
}

.note-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 12px;
}

.note-title {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #24292f;
  flex: 1;
}

.note-actions {
  display: flex;
  gap: 8px;
}

.action-btn {
  background: none;
  border: none;
  cursor: pointer;
  padding: 4px;
  border-radius: 4px;
}

.action-btn:hover {
  background: #f6f8fa;
}

.note-preview {
  color: #656d76;
  font-size: 14px;
  line-height: 1.5;
  margin-bottom: 12px;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.note-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12px;
  color: #656d76;
}

.note-tags {
  display: flex;
  gap: 4px;
}

.tag {
  background: #f6f8fa;
  padding: 2px 6px;
  border-radius: 12px;
  font-size: 11px;
}

.loading, .empty-state {
  text-align: center;
  padding: 40px 0;
  color: #656d76;
}

.empty-state button {
  margin-top: 16px;
}

.load-more {
  text-align: center;
  margin-top: 20px;
}

.btn {
  padding: 8px 16px;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
}

.btn-primary {
  background: #0969da;
  color: white;
}

.btn-primary:hover:not(:disabled) {
  background: #085fac;
}

.btn-secondary {
  background: #f6f8fa;
  color: #24292f;
  border: 1px solid #d0d7de;
}

.btn-secondary:hover:not(:disabled) {
  background: #eaecef;
}

.btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
</style>