import { reactive } from 'vue';
import { useWebSocket, WS_MESSAGE_TYPES } from './useWebSocket.js';
import { acquireEditLockApi, releaseEditLockApi, syncNoteContentApi } from '../api/note.js';

export const useNoteEditor = () => {
  const ws = useWebSocket();
  
  const editorState = reactive({
    hasEditLock: false,
    lockOwner: null,
    isEditing: false,
    currentContent: '',
    currentTitle: '',
    currentVersion: 0,
    noteId: null
  });

  let lockTimeout = null;
  let syncTimer = null;

  /**
   * 请求编辑锁
   * @param {number} noteId - 笔记ID
   */
  const requestEditLock = async (noteId) => {
    try {
      // 通过HTTP API获取锁
      const response = await acquireEditLockApi(noteId);
      const result = response.data.data;
      
      if (result) {
        editorState.hasEditLock = true;
        editorState.lockOwner = 'me';
        editorState.noteId = noteId;
        
        // 注册WebSocket消息监听
        ws.on(WS_MESSAGE_TYPES.EDIT_LOCK_GRANTED, handleLockGranted);
        ws.on(WS_MESSAGE_TYPES.EDIT_LOCK_DENIED, handleLockDenied);
        ws.on(WS_MESSAGE_TYPES.EDIT_LOCK_RELEASED, handleLockReleased);
        ws.on(WS_MESSAGE_TYPES.NOTE_CONTENT_UPDATED, handleContentUpdated);
        
        // 通过WebSocket通知服务器
        ws.requestEditLock(noteId);
        
        // 设置锁超时释放（10分钟）
        if (lockTimeout) {
          clearTimeout(lockTimeout);
        }
        lockTimeout = setTimeout(() => {
          console.warn('⚠️ 编辑锁超时，自动释放');
          releaseEditLock(noteId);
        }, 10 * 60 * 1000);
        
        return { success: true };
      } else {
        return { success: false, message: '无法获取编辑锁' };
      }
    } catch (error) {
      console.error('❌ 请求编辑锁失败:', error);
      return { success: false, error: error.message };
    }
  };

  /**
   * 释放编辑锁
   * @param {number} noteId - 笔记ID
   */
  const releaseEditLock = async (noteId) => {
    try {
      // 通过HTTP API释放锁
      await releaseEditLockApi(noteId);
      
      editorState.hasEditLock = false;
      editorState.lockOwner = null;
      editorState.noteId = null;
      
      // 清除超时定时器
      if (lockTimeout) {
        clearTimeout(lockTimeout);
        lockTimeout = null;
      }
      
      // 通过WebSocket通知服务器
      ws.releaseEditLock(noteId);
      
      // 取消消息监听
      ws.off(WS_MESSAGE_TYPES.EDIT_LOCK_GRANTED, handleLockGranted);
      ws.off(WS_MESSAGE_TYPES.EDIT_LOCK_DENIED, handleLockDenied);
      ws.off(WS_MESSAGE_TYPES.EDIT_LOCK_RELEASED, handleLockReleased);
      ws.off(WS_MESSAGE_TYPES.NOTE_CONTENT_UPDATED, handleContentUpdated);
      
      return { success: true };
    } catch (error) {
      console.error('❌ 释放编辑锁失败:', error);
      return { success: false, error: error.message };
    }
  };

  /**
   * 实时同步内容（防抖后调用）
   * @param {number} noteId - 笔记ID
   * @param {string} title - 标题
   * @param {string} content - 内容
   * @param {number} version - 版本号
   */
  const syncContent = async (noteId, title, content, version) => {
    if (!editorState.hasEditLock) {
      console.warn('⚠️ 没有编辑锁，无法同步内容');
      return;
    }
    
    try {
      // 通过HTTP API保存到数据库（不增加版本号）
      await syncNoteContentApi(noteId, {
        title,
        content,
        version
      });
      
      // 通过WebSocket广播给其他在线用户
      ws.updateNoteContent({
        noteId,
        title,
        content,
        version,
        timestamp: Date.now()
      });
      
      // 更新本地状态
      editorState.currentTitle = title;
      editorState.currentContent = content;
      editorState.currentVersion = version;
    } catch (error) {
      console.error('❌ 同步内容失败:', error);
    }
  };

  /**
   * 启动自动保存（每5秒同步一次）
   * @param {number} noteId - 笔记ID
   * @param {string} title - 标题
   * @param {string} content - 内容
   * @param {number} version - 版本号
   * @param {number} interval - 间隔时间（毫秒），默认5000ms
   */
  const startAutoSave = (noteId, title, content, version, interval = 5000) => {
    if (syncTimer) {
      clearInterval(syncTimer);
    }
    
    syncTimer = setInterval(() => {
      if (editorState.hasEditLock) {
        syncContent(noteId, title, content, version);
      }
    }, interval);
  };

  /**
   * 停止自动保存
   */
  const stopAutoSave = () => {
    if (syncTimer) {
      clearInterval(syncTimer);
      syncTimer = null;
    }
  };

  /**
   * 处理锁被授予
   */
  const handleLockGranted = (message) => {
    if (message.noteId === editorState.noteId) {
      console.log('✅ 编辑锁已授予');
      editorState.hasEditLock = true;
      editorState.lockOwner = 'me';
    }
  };

  /**
   * 处理锁被拒绝
   */
  const handleLockDenied = (message) => {
    if (message.noteId === editorState.noteId) {
      console.warn('⚠️ 编辑锁被拒绝:', message.message);
      editorState.hasEditLock = false;
      editorState.lockOwner = message.editorId || message.owner;
    }
  };

  /**
   * 处理锁被释放
   */
  const handleLockReleased = (message) => {
    if (message.noteId === editorState.noteId) {
      console.log('ℹ️ 编辑锁已释放');
      editorState.hasEditLock = false;
      editorState.lockOwner = null;
    }
  };

  /**
   * 处理内容更新（其他人编辑）
   */
  const handleContentUpdated = (message) => {
    if (message.noteId === editorState.noteId && !editorState.hasEditLock) {
      console.log('📝 收到内容更新:', message);
      // 更新本地内容
      editorState.currentTitle = message.title;
      editorState.currentContent = message.content;
      editorState.currentVersion = message.version;
    }
  };

  /**
   * 开始查看笔记
   * @param {number} noteId - 笔记ID
   */
  const startViewingNote = (noteId) => {
    ws.startViewNote(noteId);
  };

  /**
   * 停止查看笔记
   * @param {number} noteId - 笔记ID
   */
  const stopViewingNote = (noteId) => {
    ws.endViewNote(noteId);
  };

  return {
    editorState,
    requestEditLock,
    releaseEditLock,
    syncContent,
    startAutoSave,
    stopAutoSave,
    startViewingNote,
    stopViewingNote,
    ws
  };
};