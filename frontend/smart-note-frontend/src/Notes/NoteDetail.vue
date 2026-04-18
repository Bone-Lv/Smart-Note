<template>
  <div class="note-detail">
    <div class="note-header">
      <div class="note-title-section">
        <h1 v-if="!editingTitle" @dblclick="startEditingTitle" class="note-title">
          {{ noteData.title }}
        </h1>
        <input 
          v-else 
          v-model="tempTitle" 
          @blur="saveTitle" 
          @keyup.enter="saveTitle"
          class="title-input"
          ref="titleInputRef"
        />
        <button class="edit-title-btn" @click="startEditingTitle">✏️</button>
      </div>
      
      <div class="note-meta">
        <span class="note-date">创建于 {{ formatDate(noteData.createTime) }}</span>
        <span class="note-date">更新于 {{ formatDate(noteData.updateTime) }}</span>
        <span class="note-version">版本 {{ noteData.version }}</span>
      </div>
    </div>
    
    <div class="note-content-section">
      <div class="content-area">
        <div v-if="!hasEditLock" class="locked-indicator">
          <p>笔记正在被其他用户编辑</p>
        </div>
        
        <textarea 
          v-if="hasEditLock && editing"
          v-model="editorContent"
          class="editor-textarea"
          @input="handleContentChange"
          @keydown.ctrl.s.prevent="saveNote"
        ></textarea>
        
        <MarkdownRenderer 
          v-else 
          :content="noteData.content" 
          :annotations="annotations"
          :version="noteData.version"
        />
      </div>
      
      <div class="sidebar">
        <div class="sidebar-section">
          <h3>版本历史</h3>
          <div class="version-list">
            <div 
              v-for="version in versions.slice(0, 5)" 
              :key="version.version"
              class="version-item"
              :class="{ current: version.version === noteData.version }"
              @click="switchVersion(version.version)"
            >
              <div class="version-info">
                <span class="version-number">#{{ version.version }}</span>
                <span class="version-date">{{ formatDate(version.createTime) }}</span>
              </div>
              <div class="version-summary">{{ version.content.substring(0, 50) }}...</div>
            </div>
          </div>
          <button class="btn btn-secondary" @click="showDiffModal = true">查看全部版本对比</button>
        </div>
        
        <div class="sidebar-section">
          <h3>批注</h3>
          <div class="annotations-list">
            <div 
              v-for="annotation in annotations" 
              :key="annotation.id"
              class="annotation-item"
            >
              <div class="annotation-header">
                <span class="annotation-author">{{ annotation.username }}</span>
                <span class="annotation-time">{{ formatDate(annotation.createTime) }}</span>
              </div>
              <div class="annotation-content">{{ annotation.content }}</div>
              <div class="annotation-target">{{ annotation.targetContent }}</div>
            </div>
          </div>
          <button class="btn btn-primary" @click="addAnnotation">添加批注</button>
        </div>
        
        <div class="sidebar-section">
          <h3>标签</h3>
          <div class="tags-section">
            <input 
              v-model="newTag" 
              @keyup.enter="addTag" 
              placeholder="添加标签..."
              class="tag-input"
            />
            <div class="tags-list">
              <span 
                v-for="tag in (noteData.tags || '').split(',').filter(t => t)" 
                :key="tag" 
                class="tag"
              >
                #{{ tag }}
                <button @click="removeTag(tag)" class="remove-tag">×</button>
              </span>
            </div>
          </div>
        </div>
        
        <div class="sidebar-section">
          <h3>操作</h3>
          <div class="actions">
            <button 
              v-if="!editing && hasEditLock" 
              class="btn btn-primary" 
              @click="startEditing"
            >
              编辑
            </button>
            <button 
              v-if="editing" 
              class="btn btn-success" 
              @click="saveNote" 
              :disabled="saving"
            >
              {{ saving ? '保存中...' : '保存' }}
            </button>
            <button 
              v-if="editing" 
              class="btn btn-secondary" 
              @click="cancelEditing"
            >
              取消
            </button>
            <button class="btn btn-secondary" @click="aiAnalyze">AI 分析</button>
            <button class="btn btn-secondary" @click="shareNote">分享</button>
            <button class="btn btn-danger" @click="deleteNote">删除</button>
          </div>
        </div>
      </div>
    </div>
    
    <DiffModal 
      v-if="showDiffModal" 
      :visible="showDiffModal"
      :versions="versions"
      :currentNoteId="noteId"
      @close="showDiffModal = false"
      @version-rollbacked="loadNote"
    />
  </div>
</template>

<script>
import { noteApi } from '../../api/note.js';
import { useNoteEditor } from '../../hooks/useNoteEditor.js';
import MarkdownRenderer from '../../components/MarkdownRenderer.vue';
import DiffModal from '../../components/DiffModal.vue';
import { io } from 'socket.io-client';

export default {
  name: 'NoteDetail',
  components: {
    MarkdownRenderer,
    DiffModal
  },
  props: {
    noteId: {
      type: Number,
      required: true
    }
  },
  data() {
    return {
      noteData: {},
      versions: [],
      annotations: [],
      editing: false,
      editingTitle: false,
      tempTitle: '',
      editorContent: '',
      hasEditLock: false,
      saving: false,
      showDiffModal: false,
      newTag: '',
      socket: null
    };
  },
  async mounted() {
    await this.loadNote();
    await this.loadVersions();
    await this.loadAnnotations();
    this.connectWebSocket();
  },
  beforeUnmount() {
    if (this.socket) {
      this.socket.disconnect();
    }
  },
  methods: {
    async loadNote() {
      try {
        const response = await noteApi.getNoteDetail(this.noteId);
        this.noteData = response.data;
        this.editorContent = response.data.content;
      } catch (error) {
        console.error('加载笔记失败:', error);
      }
    },

    async loadVersions() {
      try {
        const response = await noteApi.getVersionHistory(this.noteId);
        this.versions = response.data || [];
      } catch (error) {
        console.error('加载版本历史失败:', error);
      }
    },

    async loadAnnotations() {
      try {
        const response = await noteApi.getNoteAnnotations(this.noteId);
        this.annotations = response.data || [];
      } catch (error) {
        console.error('加载批注失败:', error);
      }
    },

    connectWebSocket() {
      const token = localStorage.getItem('token');
      this.socket = io('ws://localhost:8080', {
        transports: ['websocket'],
        auth: {
          token: token
        }
      });

      this.socket.on('connect', () => {
        console.log('WebSocket连接已建立');
        // 请求编辑锁
        this.requestEditLock();
      });

      this.socket.on('disconnect', () => {
        console.log('WebSocket连接已断开');
      });

      this.socket.on('message', (message) => {
        this.handleWebSocketMessage(message);
      });
    },

    requestEditLock() {
      this.socket.emit('message', {
        type: 'NOTE_EDIT_REQUEST',
        data: { noteId: this.noteId }
      });
    },

    handleWebSocketMessage(message) {
      switch (message.type) {
        case 'EDIT_LOCK_GRANTED':
          this.hasEditLock = true;
          break;
        case 'EDIT_LOCK_DENIED':
          this.hasEditLock = false;
          alert('编辑权限已被他人占用');
          break;
        case 'NOTE_CONTENT_UPDATED':
          if (message.data.noteId === this.noteId && !this.hasEditLock) {
            this.noteData.content = message.data.content;
            this.noteData.version = message.data.version;
          }
          break;
      }
    },

    startEditing() {
      this.editing = true;
      this.editorContent = this.noteData.content;
    },

    cancelEditing() {
      this.editing = false;
      this.editorContent = this.noteData.content;
    },

    async saveNote() {
      if (!this.hasEditLock) {
        alert('没有编辑权限');
        return;
      }

      this.saving = true;
      try {
        const updateData = {
          title: this.noteData.title,
          content: this.editorContent,
          tags: this.noteData.tags,
          version: this.noteData.version
        };

        await noteApi.updateNote(this.noteId, updateData);
        await this.loadNote(); // 重新加载最新数据
        this.editing = false;
        alert('笔记已保存');
      } catch (error) {
        alert('保存失败: ' + error.message);
      } finally {
        this.saving = false;
      }
    },

    handleContentChange() {
      // 可以在这里实现自动保存逻辑
    },

    startEditingTitle() {
      this.editingTitle = true;
      this.tempTitle = this.noteData.title;
      this.$nextTick(() => {
        this.$refs.titleInputRef.focus();
      });
    },

    async saveTitle() {
      if (this.tempTitle.trim() && this.tempTitle !== this.noteData.title) {
        try {
          await noteApi.updateNote(this.noteId, {
            title: this.tempTitle,
            content: this.noteData.content,
            tags: this.noteData.tags,
            version: this.noteData.version
          });
          this.noteData.title = this.tempTitle;
          alert('标题已更新');
        } catch (error) {
          alert('更新标题失败: ' + error.message);
        }
      }
      this.editingTitle = false;
    },

    async switchVersion(versionNumber) {
      try {
        const response = await noteApi.getVersionDetail(this.noteId, versionNumber);
        this.noteData = response.data;
      } catch (error) {
        alert('切换版本失败: ' + error.message);
      }
    },

    addAnnotation() {
      // 这里可以弹出添加批注的模态框
      const targetText = prompt('请输入要批注的内容:');
      if (targetText) {
        const content = prompt('请输入批注内容:');
        if (content) {
          try {
            noteApi.createAnnotation(this.noteId, {
              content,
              targetContent: targetText,
              startPosition: 0, // 这里应该根据实际选择的文本位置计算
              endPosition: targetText.length
            });
            alert('批注已添加');
          } catch (error) {
            alert('添加批注失败: ' + error.message);
          }
        }
      }
    },

    addTag() {
      if (this.newTag.trim()) {
        const tags = this.noteData.tags ? this.noteData.tags.split(',') : [];
        if (!tags.includes(this.newTag.trim())) {
          tags.push(this.newTag.trim());
          this.updateTags(tags);
        }
        this.newTag = '';
      }
    },

    removeTag(tagToRemove) {
      const tags = (this.noteData.tags || '').split(',').filter(t => t && t !== tagToRemove);
      this.updateTags(tags);
    },

    async updateTags(tags) {
      try {
        await noteApi.updateNote(this.noteId, {
          title: this.noteData.title,
          content: this.noteData.content,
          tags: tags.join(','),
          version: this.noteData.version
        });
        this.noteData.tags = tags.join(',');
      } catch (error) {
        alert('更新标签失败: ' + error.message);
      }
    },

    async aiAnalyze() {
      try {
        const response = await noteApi.analyzeNote(this.noteId);
        alert('AI分析已完成，结果已保存');
      } catch (error) {
        alert('AI分析失败: ' + error.message);
      }
    },

    shareNote() {
      try {
        noteApi.generateShareInfo(this.noteId).then(response => {
          const shareInfo = response.data;
          if (shareInfo.shareCode) {
            navigator.clipboard.writeText(shareInfo.shareCode);
            alert('分享码已复制到剪贴板: ' + shareInfo.shareCode);
          } else {
            navigator.clipboard.writeText(shareInfo.shareUrl);
            alert('分享链接已复制到剪贴板: ' + shareInfo.shareUrl);
          }
        });
      } catch (error) {
        alert('生成分享信息失败: ' + error.message);
      }
    },

    async deleteNote() {
      if (confirm('确定要删除这篇笔记吗？删除后会进入回收站，5分钟后自动彻底删除。')) {
        try {
          await noteApi.deleteNote(this.noteId);
          this.$router.push('/notes');
        } catch (error) {
          alert('删除笔记失败: ' + error.message);
        }
      }
    },

    formatDate(dateString) {
      if (!dateString) return '';
      const date = new Date(dateString);
      return date.toLocaleString();
    }
  }
};
</script>

<style scoped>
.note-detail {
  padding: 20px;
  max-width: 1200px;
  margin: 0 auto;
}

.note-header {
  margin-bottom: 20px;
  padding-bottom: 15px;
  border-bottom: 1px solid #e1e5e9;
}

.note-title-section {
  display: flex;
  align-items: center;
  margin-bottom: 10px;
}

.note-title {
  margin: 0;
  font-size: 24px;
  font-weight: 600;
  color: #24292f;
  flex: 1;
}

.title-input {
  flex: 1;
  padding: 8px 12px;
  border: 1px solid #0969da;
  border-radius: 6px;
  font-size: 24px;
  font-weight: 600;
}

.edit-title-btn {
  background: none;
  border: none;
  cursor: pointer;
  padding: 8px;
  margin-left: 8px;
  border-radius: 4px;
}

.edit-title-btn:hover {
  background: #f6f8fa;
}

.note-meta {
  display: flex;
  gap: 20px;
  font-size: 14px;
  color: #656d76;
}

.note-content-section {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 20px;
}

.content-area {
  background: white;
  border: 1px solid #e1e5e9;
  border-radius: 8px;
  padding: 20px;
  min-height: 500px;
}

.locked-indicator {
  text-align: center;
  padding: 40px 0;
  color: #656d76;
  background: #fff9db;
  border-radius: 8px;
  margin-bottom: 10px;
}

.editor-textarea {
  width: 100%;
  height: 500px;
  border: none;
  resize: none;
  font-family: inherit;
  font-size: 16px;
  line-height: 1.6;
  padding: 10px;
  box-sizing: border-box;
}

.editor-textarea:focus {
  outline: none;
}

.sidebar {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.sidebar-section {
  background: #f6f8fa;
  border-radius: 8px;
  padding: 15px;
}

.sidebar-section h3 {
  margin: 0 0 15px 0;
  color: #24292f;
  font-size: 16px;
}

.version-list {
  margin-bottom: 15px;
}

.version-item {
  padding: 10px;
  border: 1px solid #e1e5e9;
  border-radius: 6px;
  margin-bottom: 8px;
  cursor: pointer;
  transition: background-color 0.2s;
}

.version-item:hover {
  background: #eaecef;
}

.version-item.current {
  border-color: #0969da;
  background: #f0f7ff;
}

.version-info {
  display: flex;
  justify-content: space-between;
  margin-bottom: 5px;
}

.version-number {
  font-weight: 600;
}

.version-date {
  color: #656d76;
  font-size: 12px;
}

.version-summary {
  font-size: 12px;
  color: #656d76;
}

.annotations-list {
  max-height: 200px;
  overflow-y: auto;
  margin-bottom: 15px;
}

.annotation-item {
  padding: 8px;
  border: 1px solid #e1e5e9;
  border-radius: 4px;
  margin-bottom: 8px;
  background: white;
}

.annotation-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 5px;
  font-size: 12px;
}

.annotation-author {
  font-weight: 600;
  color: #24292f;
}

.annotation-time {
  color: #656d76;
}

.annotation-content {
  font-size: 14px;
  margin-bottom: 5px;
}

.annotation-target {
  font-size: 12px;
  color: #656d76;
  font-style: italic;
}

.tags-section {
  margin-bottom: 15px;
}

.tag-input {
  width: 100%;
  padding: 8px;
  border: 1px solid #d0d7de;
  border-radius: 4px;
  margin-bottom: 10px;
  box-sizing: border-box;
}

.tags-list {
  display: flex;
  flex-wrap: wrap;
  gap: 5px;
}

.tag {
  background: #0969da;
  color: white;
  padding: 4px 8px;
  border-radius: 12px;
  font-size: 12px;
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.remove-tag {
  background: none;
  border: none;
  color: white;
  cursor: pointer;
  font-size: 14px;
  padding: 0;
  width: 16px;
  height: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.actions {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.btn {
  padding: 8px 12px;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
  text-align: center;
  transition: background-color 0.2s;
}

.btn-primary {
  background: #0969da;
  color: white;
}

.btn-primary:hover:not(:disabled) {
  background: #085fac;
}

.btn-success {
  background: #28a745;
  color: white;
}

.btn-success:hover:not(:disabled) {
  background: #218838;
}

.btn-secondary {
  background: #f6f8fa;
  color: #24292f;
  border: 1px solid #d0d7de;
}

.btn-secondary:hover:not(:disabled) {
  background: #eaecef;
}

.btn-danger {
  background: #dc3545;
  color: white;
}

.btn-danger:hover:not(:disabled) {
  background: #c82333;
}

.btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
</style>