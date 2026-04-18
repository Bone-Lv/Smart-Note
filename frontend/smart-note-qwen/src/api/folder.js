import request from '../utils/request.js';

// ==================== 文件夹管理 ====================

/**
 * 创建文件夹
 * @param {object} data - { name, parentId, sortOrder }
 * @returns {Promise}
 */
export const createFolderApi = (data) => {
  return request.post('/note/folder', data);
};

/**
 * 获取文件夹树
 * @returns {Promise}
 */
export const getFolderTreeApi = () => {
  return request.get('/note/folder/tree');
};

/**
 * 获取子文件夹列表
 * @param {number|null} parentId - 父文件夹ID，null表示根目录
 * @returns {Promise}
 */
export const getFolderChildrenApi = (parentId = null) => {
  return request.get('/note/folder/children', {
    params: parentId !== null ? { parentId } : {}
  });
};

/**
 * 重命名文件夹
 * @param {number} folderId - 文件夹ID
 * @param {object} data - { name }
 * @returns {Promise}
 */
export const renameFolderApi = (folderId, data) => {
  return request.put(`/note/folder/${folderId}`, data);
};

/**
 * 移动文件夹
 * @param {number} folderId - 文件夹ID
 * @param {object} data - { parentId }
 * @returns {Promise}
 */
export const moveFolderApi = (folderId, data) => {
  return request.put(`/note/folder/${folderId}/move`, data);
};

/**
 * 删除文件夹
 * @param {number} folderId - 文件夹ID
 * @param {boolean} deleteNotes - 是否同时删除笔记（默认false）
 * @returns {Promise}
 */
export const deleteFolderApi = (folderId, deleteNotes = false) => {
  return request.delete(`/note/folder/${folderId}`, {
    data: { deleteNotes }
  });
};
