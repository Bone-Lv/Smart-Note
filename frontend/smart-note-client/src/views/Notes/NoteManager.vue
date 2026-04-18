<!-- src/views/Notes/NoteManager.vue -->
<template>
  <div class="note-manager">
    <div class="sidebar">
      <div class="folder-tree">
        <h3>笔记本</h3>
        <div class="folder-item root-folder" :class="{ active: selectedFolderId === null }" @click="selectFolder(null)">
          <span>全部笔记</span>
          <span class="count">{{ totalNotesCount }}</span>
        </div>
        <div class="folder-item root-folder" :class="{ active: selectedFolderId === -1 }" @click="selectFolder(-1)">
          <span>未分类</span>
          <span class="count">{{ unclassifiedNotesCount }}</span>
        </div>
        <div class="sub-folders">
          <div 
            v-for="folder in folders" 
            :key="folder.id"
            class="folder-item"
            :class="{ active: selectedFolderId === folder.id }"
            @click="selectFolder(folder.id)"
          >
            <span class="folder-icon">📁</span>
            <span class="folder-name">{{ folder.name }}</span>
            <span class="count">{{ folder.noteCount }}</span>
          </div>
        </div>
      </div>
      
      <div class="actions">
        <button @click="createNewFolder" class="btn-primary">新建文件夹</button>
        <button @click="createNewNote" class="btn-secondary">新建笔记</button>
      </div>
    </div>
    
    <div class="content">
      <div class="search-bar">
        <input 
          v-model="searchKeyword" 
          type="text" 
          placeholder="搜索笔记标题或标签..."
          @input="debouncedSearch"
        />
        <select v-model="sortOrder" @change="refreshNotes">
          <option value="time">按时间</option>
          <option value="title">按标题</option>
        </select>
      </div>
      
      <div class="notes-list">
        <NoteSkeleton v-for="n in skeletonCount" v-show="loading" :key="`skeleton-${n}`" />
        
        <div 
          v-for="note in notes" 
          :key="note.id" 
          class="note-item"
          @click="viewNote(note.id)"
        >
          <h3 class="note-title">{{ note.title }}</h3>
          <div class="note-meta">
            <span class="note-date">{{ formatDate(note.updateTime) }}</span>
            <span class="note-tags" v-if="note.tags">
              <span v-for="tag in note.tags.split(',')" :key="tag" class="tag">{{ tag.trim() }}</span>
            </span>
          </div>
          <div class="note-preview">{{ truncate(note.content, 100) }}</div>
        </div>
        
        <div v-if="!loading && notes.length === 0" class="empty-state">
          <p>暂无笔记</p>
          <button @click="createNewNote" class="btn-primary">创建第一篇笔记</button>
        </div>
      </div>
      
      <div v-if="hasMore && !loading" class="load-more">
        <button @click="loadMoreNotes" :disabled="loading">加载更多</button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { debounce } from 'lodash-es'
import { useCursorPagination } from '@/hooks/useCursorPagination'
import { getNoteList, getFolderTree, createFolder } from '@/api/note'
import { NoteVO, FolderVO, NoteQueryDTO } from '@/types/api'
import NoteSkeleton from '@/components/NoteSkeleton.vue'

const router = useRouter()

// 状态
const folders = ref<FolderVO[]>([])
const selectedFolderId = ref<number | null>(null)
const searchKeyword = ref('')
const sortOrder = ref<'time' | 'title'>('time')
const skeletonCount = ref(3)

// 分页hook
const {
  items: notes,
  loading,
  hasMore,
  loadMore: loadMoreNotes,
  refresh: refreshNotes
} = useCursorPagination<NoteVO>({
  fetchData: async (cursor, limit = 20) => {
    const query: NoteQueryDTO = {
      pageSize: limit,
      cursor,
      keyword: searchKeyword.value || undefined,
      sortOrder: sortOrder.value,
      folderId: selectedFolderId.value === -1 ? null : selectedFolderId.value ?? undefined
    }
    
    const response = await getNoteList(query)
    if (response.code === 200) {
      return {
        data: response.data.records,
        nextCursor: response.data.nextCursor,
        hasNext: response.data.hasNext
      }
    }
    throw new Error(response.msg || '获取笔记列表失败')
  }
})

// 计算属性
const totalNotesCount = computed(() => {
  return folders.value.reduce((sum, folder) => sum + folder.noteCount, 0)
})

const unclassifiedNotesCount = computed(() => {
  // 这里应该从API获取未分类笔记数量
  return 0
})

// 格式化日期
const formatDate = (dateStr: string) => {
  return new Date(dateStr).toLocaleDateString()
}

// 截断文本
const truncate = (str: string, length: number) => {
  if (str.length <= length) return str
  return str.substring(0, length) + '...'
}

// 选择文件夹
const selectFolder = (folderId: number | null) => {
  selectedFolderId.value = folderId
  refreshNotes()
}

// 创建新笔记
const createNewNote = () => {
  // 这里应该打开新建笔记的模态框或跳转到新建页面
  router.push('/notes/new')
}

// 创建新文件夹
const createNewFolder = async () => {
  const name = prompt('请输入文件夹名称:')
  if (name) {
    try {
      await createFolder({ name, parentId: selectedFolderId.value })
      alert('文件夹创建成功')
      loadFolders()
    } catch (error) {
      console.error('创建文件夹失败:', error)
      alert('创建文件夹失败')
    }
  }
}

// 查看笔记
const viewNote = (noteId: number) => {
  router.push(`/notes/${noteId}`)
}

// 加载文件夹
const loadFolders = async () => {
  try {
    const response = await getFolderTree()
    if (response.code === 200) {
      folders.value = response.data
    }
  } catch (error) {
    console.error('加载文件夹失败:', error)
  }
}

// 防抖搜索
const debouncedSearch = debounce(() => {
  refreshNotes()
}, 300)

onMounted(() => {
  loadFolders()
  refreshNotes()
})
</script>

<style scoped>
.note-manager {
  display: flex;
  height: 100%;
}

.sidebar {
  width: 250px;
  border-right: 1px solid #eee;
  padding: 20px;
  display: flex;
  flex-direction: column;
}

.folder-tree h3 {
  margin-top: 0;
  margin-bottom: 15px;
}

.folder-item {
  padding: 8px 12px;
  border-radius: 4px;
  cursor: pointer;
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 5px;
}

.folder-item:hover {
  background-color: #f8f9fa;
}

.folder-item.active {
  background-color: #e7f3ff;
  color: #007bff;
}

.root-folder {
  font-weight: bold;
}

.sub-folders {
  margin-top: 15px;
}

.folder-icon {
  margin-right: 8px;
}

.count {
  background-color: #e9ecef;
  border-radius: 12px;
  padding: 2px 8px;
  font-size: 0.8em;
}

.actions {
  margin-top: auto;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.btn-primary, .btn-secondary {
  padding: 8px 16px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  text-align: center;
}

.btn-primary {
  background-color: #007bff;
  color: white;
}

.btn-secondary {
  background-color: #6c757d;
  color: white;
}

.content {
  flex: 1;
  padding: 20px;
}

.search-bar {
  display: flex;
  gap: 10px;
  margin-bottom: 20px;
}

.search-bar input, .search-bar select {
  padding: 8px 12px;
  border: 1px solid #ddd;
  border-radius: 4px;
}

.notes-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 20px;
}

.note-item {
  border: 1px solid #eee;
  border-radius: 8px;
  padding: 15px;
  cursor: pointer;
  transition: box-shadow 0.2s;
}

.note-item:hover {
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
}

.note-title {
  margin: 0 0 10px 0;
  color: #333;
}

.note-meta {
  display: flex;
  justify-content: space-between;
  margin-bottom: 10px;
  font-size: 0.9em;
  color: #666;
}

.note-tags {
  display: flex;
  gap: 5px;
}

.tag {
  background-color: #e9ecef;
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 0.8em;
}

.note-preview {
  color: #666;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.empty-state {
  text-align: center;
  padding: 40px;
  color: #666;
}

.load-more {
  text-align: center;
  margin-top: 20px;
}

.load-more button {
  padding: 10px 20px;
  background-color: #007bff;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}
</style>