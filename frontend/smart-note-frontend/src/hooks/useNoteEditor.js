import { ref, reactive, onMounted, onUnmounted } from 'vue';

export const useNoteEditor = (noteId, initialContent = '') => {
  const editorState = reactive({
    content: initialContent,
    title: '',
    tags: [],
    version: 1,
    hasEditLock: false,
    editLockUser: null,
    isSaving: false,
    lastSavedTime: null
  });

  const pendingChanges = ref([]);
  const saveTimer = ref(null);

  // 请求编辑锁
  const requestEditLock = async (wsConnection) => {
    if (!wsConnection) return false;
    
    const lockRequest = {
      type: 'NOTE_EDIT_REQUEST',
      data: { noteId }
    };
    
    wsConnection.send(lockRequest);
    return true;
  };

  // 释放编辑锁
  const releaseEditLock = async (wsConnection) => {
    if (!wsConnection || !editorState.hasEditLock) return false;
    
    const unlockRequest = {
      type: 'NOTE_EDIT_RELEASE',
      data: { noteId }
    };
    
    wsConnection.send(unlockRequest);
    editorState.hasEditLock = false;
    return true;
  };

  // 处理编辑锁相关消息
  const handleEditLockMessage = (message) => {
    switch (message.type) {
      case 'EDIT_LOCK_GRANTED':
        editorState.hasEditLock = true;
        editorState.editLockUser = message.data.userId;
        break;
      case 'EDIT_LOCK_DENIED':
        editorState.hasEditLock = false;
        alert('编辑权限已被他人占用，无法编辑此笔记');
        break;
      case 'EDIT_LOCK_RELEASED':
        editorState.hasEditLock = false;
        editorState.editLockUser = null;
        break;
      case 'NOTE_CONTENT_UPDATED':
        // 接收到其他用户的内容更新
        if (message.data.noteId === noteId && !editorState.hasEditLock) {
          editorState.content = message.data.content;
          editorState.version = message.data.version;
        }
        break;
    }
  };

  // 自动保存内容
  const autoSave = async (saveApiFunction) => {
    if (!editorState.hasEditLock || !editorState.content.trim()) return;

    editorState.isSaving = true;
    
    try {
      const updateData = {
        title: editorState.title,
        content: editorState.content,
        tags: editorState.tags.join(','),
        version: editorState.version
      };

      await saveApiFunction(noteId, updateData);
      editorState.lastSavedTime = new Date();
      console.log('笔记已自动保存');
    } catch (error) {
      console.error('自动保存失败:', error);
    } finally {
      editorState.isSaving = false;
    }
  };

  // 监听内容变化，触发自动保存
  const onContentChange = (newContent, saveApiFunction) => {
    editorState.content = newContent;
    
    // 清除之前的保存定时器
    if (saveTimer.value) {
      clearTimeout(saveTimer.value);
    }
    
    // 设置新的保存定时器（延迟5秒保存）
    saveTimer.value = setTimeout(() => {
      autoSave(saveApiFunction);
    }, 5000);
  };

  // 开始查看笔记
  const startViewing = (wsConnection) => {
    if (!wsConnection) return;
    
    const viewStartMessage = {
      type: 'NOTE_VIEW_START',
      data: { noteId }
    };
    
    wsConnection.send(viewStartMessage);
  };

  // 停止查看笔记
  const stopViewing = (wsConnection) => {
    if (!wsConnection) return;
    
    const viewEndMessage = {
      type: 'NOTE_VIEW_END',
      data: { noteId }
    };
    
    wsConnection.send(viewEndMessage);
  };

  // 清理定时器
  const cleanup = () => {
    if (saveTimer.value) {
      clearTimeout(saveTimer.value);
      saveTimer.value = null;
    }
  };

  onUnmounted(cleanup);

  return {
    editorState,
    requestEditLock,
    releaseEditLock,
    handleEditLockMessage,
    onContentChange,
    startViewing,
    stopViewing,
    cleanup
  };
};