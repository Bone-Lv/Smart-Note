// src/hooks/useNoteEditor.ts
import { ref, computed } from 'vue'
import { useWebSocket } from './useWebSocket'
import { NoteVO } from '@/types/api'

interface NoteEditorHookReturn {
  isEditing: boolean
  hasEditLock: boolean
  lockOwner: string | null
  canEdit: boolean
  requestEdit: () => void
  releaseEdit: () => void
  saveNote: (note: NoteVO, content: string, title: string) => Promise<void>
}

export function useNoteEditor(noteId: number) {
  const { sendMessage, isConnected } = useWebSocket()
  const isEditing = ref(false)
  const hasEditLock = ref(false)
  const lockOwner = ref<string | null>(null)

  const canEdit = computed(() => {
    return hasEditLock.value || !isConnected.value
  })

  const requestEdit = () => {
    if (!isConnected.value) {
      // 如果WebSocket未连接，假设有编辑权限
      hasEditLock.value = true
      isEditing.value = true
      return
    }
    
    sendMessage({
      type: 'note_edit_request',
      data: { noteId },
      timestamp: Date.now()
    })
  }

  const releaseEdit = () => {
    if (!isConnected.value) {
      hasEditLock.value = false
      isEditing.value = false
      return
    }
    
    sendMessage({
      type: 'note_edit_release',
      data: { noteId },
      timestamp: Date.now()
    })
    
    hasEditLock.value = false
    isEditing.value = false
  }

  const saveNote = async (note: NoteVO, content: string, title: string) => {
    // 在保存前检查版本
    if (note.version !== undefined) {
      // 这里应该调用API保存笔记
      console.log('Saving note:', { ...note, content, title, version: note.version + 1 })
      // 模拟API调用
      await new Promise(resolve => setTimeout(resolve, 500))
      
      // 如果WebSocket连接，则通过WebSocket同步内容
      if (isConnected.value) {
        sendMessage({
          type: 'note_content_update',
          data: { 
            noteId: note.id, 
            content, 
            title, 
            version: note.version + 1 
          },
          timestamp: Date.now()
        })
      }
    }
  }

  // 监听WebSocket消息来更新编辑状态
  // 在实际实现中，您需要在这里处理WebSocket事件

  return {
    isEditing,
    hasEditLock,
    lockOwner,
    canEdit,
    requestEdit,
    releaseEdit,
    saveNote
  }
}