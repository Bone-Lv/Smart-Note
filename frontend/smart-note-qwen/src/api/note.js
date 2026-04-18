import request from '../utils/request.js';

// ==================== 笔记基础操作 ====================

// 创建笔记
export const createNoteApi = (noteData) => {
  return request.post('/note', noteData);
};

// 获取笔记列表（分页）
export const getNoteListApi = (params) => {
  return request.get('/note/list', { params });
};

// 获取笔记详情
export const getNoteDetailApi = (noteId) => {
  return request.get(`/note/${noteId}`);
};

// 更新笔记（创建新版本）
export const updateNoteApi = (noteId, noteData) => {
  return request.put(`/note/${noteId}`, noteData);
};

// 同步笔记内容（不创建新版本）
export const syncNoteContentApi = (noteId, data) => {
  return request.put(`/note/${noteId}/sync`, data);
};

// 删除笔记（移入回收站）
export const deleteNoteApi = (noteId) => {
  return request.delete(`/note/${noteId}`);
};

// 移动笔记到指定文件夹
export const moveNoteApi = (noteId, folderId) => {
  return request.put(`/note/${noteId}/move`, { folderId });
};

// ==================== 笔记导入导出 ====================

// 导入Markdown文件
export const importMarkdownApi = (file, folderId) => {
  const formData = new FormData();
  formData.append('file', file);
  if (folderId) {
    formData.append('folderId', folderId);
  }
  return request.post('/note/import/md', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  });
};

// 导入PDF文件
export const importPdfApi = (file, folderId) => {
  const formData = new FormData();
  formData.append('file', file);
  if (folderId) {
    formData.append('folderId', folderId);
  }
  return request.post('/note/import/pdf', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  });
};

// 导出Markdown
export const exportMarkdownApi = (noteId) => {
  return request.get(`/note/${noteId}/export/md`, {
    responseType: 'blob'
  });
};

// 导出PDF
export const exportPdfApi = (noteId) => {
  return request.get(`/note/${noteId}/export/pdf`, {
    responseType: 'blob'
  });
};

// ==================== 版本管理 ====================

// 获取版本历史
export const getNoteVersionsApi = (noteId) => {
  return request.get(`/note/${noteId}/versions`);
};

// 查看指定版本详情
export const getNoteVersionDetailApi = (noteId, version) => {
  return request.get(`/note/${noteId}/versions/${version}`);
};

// 版本回退
export const rollbackNoteVersionApi = (noteId, version) => {
  return request.post(`/note/${noteId}/versions/${version}/rollback`);
};

// ==================== AI智能分析 ====================

// AI分析笔记
export const analyzeNoteWithAIApi = (noteId, forceRefresh = false) => {
  return request.post(`/note/${noteId}/ai-analyze`, { forceRefresh });
};

// ==================== 笔记可见性与分享 ====================

// 设置笔记可见性
export const updateNoteVisibilityApi = (noteId, data) => {
  return request.put(`/note/${noteId}/visibility`, data);
};

// 生成分享链接
export const generateShareLinkApi = (noteId) => {
  return request.post(`/note/${noteId}/share`);
};

// 通过分享码查看笔记（无需登录）
export const getNoteByShareCodeApi = (shareCode) => {
  return request.get(`/note/share/${shareCode}`);
};

// 查看公开笔记（无需登录）
export const getPublicNoteApi = (noteId) => {
  return request.get(`/note/public/${noteId}`);
};

// 查看好友分享的笔记
export const getSharedNoteApi = (noteId) => {
  return request.get(`/note/shared/${noteId}`);
};

// 获取笔记权限列表
export const getNotePermissionsApi = (noteId) => {
  return request.get(`/note/${noteId}/permissions`);
};

// ==================== 笔记辅助功能 ====================

// 上传笔记图片
export const uploadNoteImageApi = (file) => {
  const formData = new FormData();
  formData.append('file', file);
  return request.post('/note/upload-image', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  });
};

// ==================== 编辑锁管理 ====================

// 获取笔记编辑锁
export const acquireEditLockApi = (noteId) => {
  return request.post(`/note/${noteId}/lock`);
};

// 释放笔记编辑锁
export const releaseEditLockApi = (noteId) => {
  return request.delete(`/note/${noteId}/lock`);
};

// ==================== 笔记批注 ====================

// 查看笔记批注列表
export const getNoteAnnotationsApi = (noteId) => {
  return request.get(`/note/${noteId}/annotations`);
};

// 创建批注
export const createAnnotationApi = (noteId, annotationData) => {
  return request.post(`/note/${noteId}/annotations`, annotationData);
};

// 更新批注
export const updateAnnotationApi = (annotationId, annotationData) => {
  return request.put(`/note/annotations/${annotationId}`, annotationData);
};

// 删除批注
export const deleteAnnotationApi = (annotationId) => {
  return request.delete(`/note/annotations/${annotationId}`);
};

// ==================== 最近与高频笔记 ====================

// 获取最近查看的笔记
export const getRecentViewedNotesApi = (limit = 10) => {
  return request.get('/note/recent', { params: { limit } });
};

// 获取最常看的3篇笔记
export const getTop3FrequentNotesApi = () => {
  return request.get('/note/top3-frequent');
};
