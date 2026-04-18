import request from '../utils/request.js';

export const noteApi = {
  // 创建笔记
  createNote(data) {
    return request.post('/note', data);
  },

  // 获取笔记列表
  getNoteList(queryDTO) {
    return request.get('/note/list', { params: queryDTO });
  },

  // 获取笔记详情
  getNoteDetail(noteId) {
    return request.get(`/note/${noteId}`);
  },

  // 更新笔记
  updateNote(noteId, data) {
    return request.put(`/note/${noteId}`, data);
  },

  // 删除笔记
  deleteNote(noteId) {
    return request.delete(`/note/${noteId}`);
  },

  // 设置笔记可见性
  setNoteVisibility(noteId, data) {
    return request.put(`/note/${noteId}/visibility`, data);
  },

  // 获取笔记版本历史
  getVersionHistory(noteId) {
    return request.get(`/note/${noteId}/versions`);
  },

  // 获取指定版本详情
  getVersionDetail(noteId, version) {
    return request.get(`/note/${noteId}/versions/${version}`);
  },

  // 版本回退
  rollbackToVersion(noteId, version) {
    return request.post(`/note/${noteId}/versions/${version}/rollback`);
  },

  // 获取笔记权限列表
  getNotePermissions(noteId) {
    return request.get(`/note/${noteId}/permissions`);
  },

  // 生成分享信息
  generateShareInfo(noteId) {
    return request.post(`/note/${noteId}/share`);
  },

  // 获取共享笔记
  getSharedNote(noteId) {
    return request.get(`/note/shared/${noteId}`);
  },

  // 通过分享码获取笔记
  getNoteByShareCode(shareCode) {
    return request.get(`/note/share/${shareCode}`);
  },

  // 获取公开笔记
  getPublicNote(noteId) {
    return request.get(`/note/public/${noteId}`);
  },

  // 获取最近查看的笔记
  getRecentViewedNotes(limit = 10) {
    return request.get('/note/recent', { params: { limit } });
  },

  // 获取最常看的3篇笔记
  getTop3FrequentNotes() {
    return request.get('/note/top3-frequent');
  },

  // 导出为PDF
  exportAsPdf(noteId) {
    return request.get(`/note/${noteId}/export/pdf`, { responseType: 'blob' });
  },

  // 导出为Markdown
  exportAsMarkdown(noteId) {
    return request.get(`/note/${noteId}/export/md`, { responseType: 'blob' });
  },

  // AI智能分析笔记
  analyzeNote(noteId, data = {}) {
    return request.post(`/note/${noteId}/ai-analyze`, data);
  },

  // 创建批注
  createAnnotation(noteId, data) {
    return request.post(`/note/${noteId}/annotations`, data);
  },

  // 获取笔记批注列表
  getNoteAnnotations(noteId) {
    return request.get(`/note/${noteId}/annotations`);
  },

  // 更新批注
  updateAnnotation(annotationId, data) {
    return request.put(`/note/annotations/${annotationId}`, data);
  },

  // 删除批注
  deleteAnnotation(annotationId) {
    return request.delete(`/note/annotations/${annotationId}`);
  },

  // 上传笔记图片
  uploadImage(file) {
    const formData = new FormData();
    formData.append('file', file);
    return request.post('/note/upload-image', formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    });
  },

  // 导入PDF笔记
  importPdfNote(folderId, file) {
    const formData = new FormData();
    formData.append('file', file);
    const params = folderId ? { folderId } : {};
    return request.post('/note/import/pdf', formData, {
      params,
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    });
  },

  // 导入Markdown笔记
  importMarkdownNote(folderId, file) {
    const formData = new FormData();
    formData.append('file', file);
    const params = folderId ? { folderId } : {};
    return request.post('/note/import/md', formData, {
      params,
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    });
  },

  // 获取文件夹树
  getFolderTree() {
    return request.get('/note/folder/tree');
  },

  // 创建文件夹
  createFolder(data) {
    return request.post('/note/folder', data);
  },

  // 重命名文件夹
  renameFolder(folderId, data) {
    return request.put(`/note/folder/${folderId}`, data);
  },

  // 删除文件夹
  deleteFolder(folderId, deleteNotes = false) {
    return request.delete(`/note/folder/${folderId}`, {
      data: { deleteNotes }
    });
  },

  // 获取回收站笔记列表
  getRecycleBinNotes() {
    return request.get('/recycle-bin/notes');
  },

  // 获取回收站文件夹列表
  getRecycleBinFolders() {
    return request.get('/recycle-bin/folders');
  },

  // 还原笔记
  restoreNote(noteId) {
    return request.post(`/recycle-bin/notes/${noteId}/restore`);
  },

  // 还原文件夹
  restoreFolder(folderId) {
    return request.post(`/recycle-bin/folders/${folderId}/restore`);
  },

  // 彻底删除笔记
  permanentlyDeleteNote(noteId) {
    return request.delete(`/recycle-bin/notes/${noteId}`);
  },

  // 彻底删除文件夹
  permanentlyDeleteFolder(folderId) {
    return request.delete(`/recycle-bin/folders/${folderId}`);
  },

  // 清空回收站
  emptyRecycleBin() {
    return request.delete('/recycle-bin/empty');
  }
};