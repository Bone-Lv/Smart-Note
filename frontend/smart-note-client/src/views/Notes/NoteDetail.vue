<!-- src/views/Notes/NoteDetail.vue -->
<template>
  <div class="note-detail">
    <div class="note-header">
      <input 
        v-if="canEdit" 
        v-model="editingTitle" 
        type="text" 
        class="note-title-input"
        @blur="saveNote"
      />
      <h1 v-else class="note-title">{{ note?.title }}</h1>
      
      <div class="note-actions">
        <button v-if="canEdit" @click="toggleEditMode" class="btn-edit">
          {{ isEditing ? '退出编辑' : '编辑' }}
        </button>
        <button @click="showVersionHistory = true" class="btn-secondary">版本历史</button>
        <button @click="showShareDialog = true" class="btn-secondary">分享</button>
        <button @click="deleteNoteConfirm" class="btn-danger">删除</button>
      </div>
    </div>
    
    <div class="note-content">
      <MarkdownRenderer 
        :content="note?.content || ''" 
        :annotations="annotations"
        :note-id="noteId"
        :version="note?.version || 0"
        @add-annotation="handleAddAnnotation"
      />
    </div>
    
    <div class="annotations-section">
      <h3>批注</h3>
      <div v-for="annotation in annotations" :key="annotation.id" class="annotation-item">
        <div class="annotation-header">
          <span class="author">{{ annotation.username }}</span>
          <span class="time">{{ formatDate(annotation.createTime) }}</span>
        </div>
        <div class="annotation-content">{{ annotation.content }}</div>
        <div class="annotation-target">批注原文: "{{ annotation.targetContent }}"</div>
      </div>
    </div>
    
    <!-- 版本历史模态框 -->
    <div v-if="showVersionHistory" class="modal-overlay" @click="showVersionHistory = false">
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <h3>版本历史</h3>
          <button @click="showVersionHistory = false" class="close-btn">×</button>
        </div>
        <div class="version-list">
          <div 
            v-for="version in versionHistory" 
            :key="version.id"
            class="version-item"
            @click="viewVersion(version.version)"
          >
            <div class="version-info">
              <span class="version-number">版本 {{ version.version }}</span>
              <span class="version-time">{{ formatDate(version.createTime) }}</span>
            </div>
            <div class="version-summary">{{ truncate(version.content, 100) }}</div>
          </div>
        </div>
      </div>
    </div>
    
    <!-- 分享对话框 -->
    <div v-if="showShareDialog" class="modal-overlay" @click="showShareDialog = false">
      <div class="modal-content share-dialog" @click.stop>
        <div class="modal-header">
          <h3>分享笔记</h3>
          <button @click="showShareDialog = false" class="close-btn">×</button>
        </div>
        <div class="share-options">
          <div class="visibility-option">
            <label>
              <input 
                type="radio" 
                v-model="visibility" 
                value="0" 
              />
              仅自己可见
            </label>
          </div>
          <div class="visibility-option">
            <label>
              <input 
                type="radio" 
                v-model="visibility" 
                value="1" 
              />
              部分好友可见
            </label>
          </div>
          <div class="visibility-option">
            <label>
              <input 
                type="radio" 
                v-model="visibility" 
                value="2" 
              />
              部分好友可编辑
            </label>
          </div>
          <div class="visibility-option">
            <label>
              <input 
                type="radio" 
                v-model="visibility" 
                value="3" 
              />
              所有人可见
            </label>
          </div>
          
          <div v-if="visibility !== '0'" class="friends-selection">
            <h4>选择好友</h4>
            <div class="friend-list">
              <label v-for="friend in friends" :key="friend.id" class="friend-item">
                <input 
                  type="checkbox" 
                  :value="friend.friendUserId"
                  v-model="selectedFriends"
                />
                {{ friend.friendUsername }}
              </label>
            </div>
          </div>
          
          <div class="share-actions">
            <button @click="updateNoteVisibility" class="btn-primary">确认分享</button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useNoteEditor } from '@/hooks/useNoteEditor'
import { getNoteDetail, updateNote, deleteNote as apiDeleteNote, getNoteAnnotations, createAnnotation, getVersionHistory, getNotePermissions } from '@/api/note'
import { getFriendList } from '@/api/social'
import { NoteVO, AnnotationVO, FriendVO } from '@/types/api'
import MarkdownRenderer from '@/components/MarkdownRenderer.vue'

const route = useRoute()
const router = useRouter()
const noteId = computed(() => Number(route.params.id))

// 状态
const note = ref<NoteVO | null>(null)
const annotations = ref<AnnotationVO[]>([])
const versionHistory = ref<any[]>([])
const friends = ref<FriendVO[]>([])
const editingTitle = ref('')
const showVersionHistory = ref(false)
const showShareDialog = ref(false)
const visibility = ref<'0' | '1' | '2' | '3'>(note?.value?.visibility || '0')
const selectedFriends = ref<number[]>([])

// 编辑器hook
const { canEdit, requestEdit, releaseEdit } = useNoteEditor(noteId.value)

// 计算属性
const isEditing = computed(() => canEdit.value)

// 加载笔记详情
const loadNote = async () => {
  try {
    const response = await getNoteDetail(noteId.value)
    if (response.code === 200) {
      note.value = response.data
      editingTitle.value = response.data.title
      visibility.value = response.data.visibility as any
      
      // 加载批注
      const annResponse = await getNoteAnnotations(noteId.value)
      if (annResponse.code === 200) {
        annotations.value = annResponse.data
      }
      
      // 加载版本历史
      const verResponse = await getVersionHistory(noteId.value)
      if (verResponse.code === 200) {
        versionHistory.value = verResponse.data
      }
      
      // 加载好友列表（用于分享）
      const friendResponse = await getFriendList()
      if (friendResponse.code === 200) {
        friends.value = friendResponse.data
      }
      
      // 加载权限信息
      const permResponse = await getNotePermissions(noteId.value)
      if (permResponse.code === 200) {
        selectedFriends.value = permResponse.data.map(p => p.friendUserId)
      }
    }
  } catch (error) {
    console.error('加载笔记详情失败:', error)
    router.push('/notes')
  }
}

// 保存笔记
const saveNote = async () => {
  if (!note.value || !editingTitle.value.trim()) return
  
  try {
    await updateNote(note.value.id, {
      title: editingTitle.value,
      version: note.value.version
    })
    note.value.title = editingTitle.value
    alert('笔记已保存')
  } catch (error) {
    console.error('保存笔记失败:', error)
    alert('保存失败')
  }
}

// 切换编辑模式
const toggleEditMode = () => {
  if (isEditing.value) {
    releaseEdit()
  } else {
    requestEdit()
  }
}

// 处理添加批注
const handleAddAnnotation = async (annotationData: any) => {
  try {
    await createAnnotation(noteId.value, annotationData)
    // 重新加载批注
    const response = await getNoteAnnotations(noteId.value)
    if (response.code === 200) {
      annotations.value = response.data
    }
    alert('批注已添加')
  } catch (error) {
    console.error('添加批注失败:', error)
    alert('添加批注失败')
  }
}

// 查看特定版本
const viewVersion = (version: number) => {
  // 这里应该导航到特定版本的查看页面
  console.log(`查看版本 ${version}`)
  showVersionHistory.value = false
}

// 更新笔记可见性
const updateNoteVisibility = async () => {
  try {
    // 这里应该调用API更新可见性
    console.log('更新可见性:', visibility.value, selectedFriends.value)
    showShareDialog.value = false
    alert('分享设置已更新')
  } catch (error) {
    console.error('更新分享设置失败:', error)
    alert('更新失败')
  }
}

// 删除笔记确认
const deleteNoteConfirm = () => {
  if (confirm('确定要删除这篇笔记吗？删除后将进入回收站，5分钟后自动彻底删除。')) {
    deleteNote()
  }
}

// 删除笔记
const deleteNote = async () => {
  try {
    await apiDeleteNote(noteId.value)
    alert('笔记已删除')
    router.push('/notes')
  } catch (error) {
    console.error('删除笔记失败:', error)
    alert('删除失败')
  }
}

// 格式化日期
const formatDate = (dateStr: string) => {
  return new Date(dateStr).toLocaleString()
}

// 截断文本
const truncate = (str: string, length: number) => {
  if (str.length <= length) return str
  return str.substring(0, length) + '...'
}

watch(() => route.params.id, (newId) => {
  if (newId) {
    loadNote()
  }
})

onMounted(() => {
  loadNote()
})
</script>

<style scoped>
.note-detail {
  max-width: 1000px;
  margin: 0 auto;
  padding: 20px;
}

.note-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 15px;
  border-bottom: 1px solid #eee;
}

.note-title {
  margin: 0;
  font-size: 2rem;
  color: #333;
}

.note-title-input {
  font-size: 2rem;
  border: none;
  border-bottom: 1px solid #007bff;
  outline: none;
  width: 100%;
}

.note-actions {
  display: flex;
  gap: 10px;
}

.btn-edit, .btn-secondary, .btn-danger {
  padding: 8px 16px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}

.btn-edit {
  background-color: #28a745;
  color: white;
}

.btn-secondary {
  background-color: #6c757d;
  color: white;
}

.btn-danger {
  background-color: #dc3545;
  color: white;
}

.note-content {
  margin-bottom: 30px;
}

.annotations-section h3 {
  margin-top: 0;
  margin-bottom: 15px;
  color: #333;
}

.annotation-item {
  border: 1px solid #eee;
  border-radius: 8px;
  padding: 15px;
  margin-bottom: 10px;
}

.annotation-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
  font-size: 0.9em;
  color: #666;
}

.author {
  font-weight: bold;
  color: #333;
}

.annotation-content {
  margin-bottom: 8px;
  line-height: 1.4;
}

.annotation-target {
  font-style: italic;
  color: #888;
  font-size: 0.9em;
}

.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
}

.modal-content {
  background: white;
  border-radius: 8px;
  width: 80%;
  max-width: 600px;
  max-height: 80vh;
  overflow-y: auto;
}

.share-dialog {
  width: 500px;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 15px 20px;
  border-bottom: 1px solid #eee;
}

.close-btn {
  background: none;
  border: none;
  font-size: 24px;
  cursor: pointer;
  padding: 0;
  width: 30px;
  height: 30px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.version-list {
  padding: 20px;
}

.version-item {
  border: 1px solid #eee;
  border-radius: 6px;
  padding: 15px;
  margin-bottom: 10px;
  cursor: pointer;
}

.version-item:hover {
  background-color: #f8f9fa;
}

.version-info {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
}

.version-number {
  font-weight: bold;
}

.version-time {
  color: #666;
  font-size: 0.9em;
}

.version-summary {
  color: #666;
  line-height: 1.4;
}

.share-options {
  padding: 20px;
}

.visibility-option {
  margin-bottom: 15px;
}

.visibility-option label {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
}

.friends-selection h4 {
  margin-top: 0;
  margin-bottom: 10px;
}

.friend-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  max-height: 200px;
  overflow-y: auto;
  padding: 10px;
  border: 1px solid #eee;
  border-radius: 4px;
}

.friend-item {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
}

.share-actions {
  margin-top: 20px;
  text-align: right;
}

.btn-primary {
  padding: 8px 16px;
  background-color: #007bff;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}
</style>