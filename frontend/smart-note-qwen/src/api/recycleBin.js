import request from '../utils/request.js';

// ==================== 回收站功能 ====================

/**
 * 获取回收站笔记列表
 * @returns {Promise}
 */
export const getRecycleBinNotesApi = () => {
  return request.get('/recycle-bin/notes');
};

/**
 * 获取回收站文件夹列表
 * @returns {Promise}
 */
export const getRecycleBinFoldersApi = () => {
  return request.get('/recycle-bin/folders');
};

/**
 * 还原笔记
 * @param {number} noteId - 笔记ID
 * @returns {Promise}
 */
export const restoreNoteApi = (noteId) => {
  return request.post(`/recycle-bin/notes/${noteId}/restore`);
};

/**
 * 还原文件夹
 * @param {number} folderId - 文件夹ID
 * @returns {Promise}
 */
export const restoreFolderApi = (folderId) => {
  return request.post(`/recycle-bin/folders/${folderId}/restore`);
};

/**
 * 彻底删除笔记
 * @param {number} noteId - 笔记ID
 * @returns {Promise}
 */
export const permanentlyDeleteNoteApi = (noteId) => {
  return request.delete(`/recycle-bin/notes/${noteId}`);
};

/**
 * 彻底删除文件夹
 * @param {number} folderId - 文件夹ID
 * @returns {Promise}
 */
export const permanentlyDeleteFolderApi = (folderId) => {
  return request.delete(`/recycle-bin/folders/${folderId}`);
};

/**
 * 清空回收站
 * @returns {Promise}
 */
export const emptyRecycleBinApi = () => {
  return request.delete('/recycle-bin/empty');
};
