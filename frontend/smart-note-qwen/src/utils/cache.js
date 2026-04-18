import { openDB } from 'idb';

// 数据库名称和版本
const DB_NAME = 'smart-note-db';
const DB_VERSION = 1;

// 存储对象名称
const STORES = {
  NOTES: 'notes',           // 笔记草稿缓存
  NOTE_CONTENTS: 'noteContents', // 笔记内容缓存
  USER_INFO: 'userInfo'     // 用户信息缓存
};

let dbInstance = null;

/**
 * 初始化数据库
 */
export const initDB = async () => {
  if (dbInstance) return dbInstance;

  try {
    dbInstance = await openDB(DB_NAME, DB_VERSION, {
      upgrade(db) {
        // 创建笔记草稿存储
        if (!db.objectStoreNames.contains(STORES.NOTES)) {
          const noteStore = db.createObjectStore(STORES.NOTES, { keyPath: 'id' });
          noteStore.createIndex('updateTime', 'updateTime', { unique: false });
        }

        // 创建笔记内容存储
        if (!db.objectStoreNames.contains(STORES.NOTE_CONTENTS)) {
          db.createObjectStore(STORES.NOTE_CONTENTS, { keyPath: 'noteId' });
        }

        // 创建用户信息存储
        if (!db.objectStoreNames.contains(STORES.USER_INFO)) {
          db.createObjectStore(STORES.USER_INFO, { keyPath: 'key' });
        }

        console.log('✅ IndexedDB 初始化成功');
      }
    });

    return dbInstance;
  } catch (error) {
    console.error('❌ IndexedDB 初始化失败:', error);
    throw error;
  }
};

/**
 * 保存笔记草稿
 * @param {number} noteId - 笔记ID
 * @param {object} data - 笔记数据
 */
export const saveNoteDraft = async (noteId, data) => {
  try {
    const db = await initDB();
    await db.put(STORES.NOTES, {
      id: noteId,
      ...data,
      updateTime: new Date().toISOString()
    });
    console.log('💾 笔记草稿已保存:', noteId);
    return true;
  } catch (error) {
    console.error('❌ 保存笔记草稿失败:', error);
    return false;
  }
};

/**
 * 获取笔记草稿
 * @param {number} noteId - 笔记ID
 */
export const getNoteDraft = async (noteId) => {
  try {
    const db = await initDB();
    const draft = await db.get(STORES.NOTES, noteId);
    if (draft) {
      console.log('📄 读取笔记草稿:', noteId);
    }
    return draft || null;
  } catch (error) {
    console.error('❌ 获取笔记草稿失败:', error);
    return null;
  }
};

/**
 * 删除笔记草稿
 * @param {number} noteId - 笔记ID
 */
export const deleteNoteDraft = async (noteId) => {
  try {
    const db = await initDB();
    await db.delete(STORES.NOTES, noteId);
    console.log('🗑️ 笔记草稿已删除:', noteId);
    return true;
  } catch (error) {
    console.error('❌ 删除笔记草稿失败:', error);
    return false;
  }
};

/**
 * 保存笔记内容
 * @param {number} noteId - 笔记ID
 * @param {string} content - 笔记内容
 */
export const saveNoteContent = async (noteId, content) => {
  try {
    const db = await initDB();
    await db.put(STORES.NOTE_CONTENTS, {
      noteId,
      content,
      updateTime: new Date().toISOString()
    });
    console.log('💾 笔记内容已缓存:', noteId);
    return true;
  } catch (error) {
    console.error('❌ 保存笔记内容失败:', error);
    return false;
  }
};

/**
 * 获取笔记内容
 * @param {number} noteId - 笔记ID
 */
export const getNoteContent = async (noteId) => {
  try {
    const db = await initDB();
    const cached = await db.get(STORES.NOTE_CONTENTS, noteId);
    return cached ? cached.content : null;
  } catch (error) {
    console.error('❌ 获取笔记内容失败:', error);
    return null;
  }
};

/**
 * 保存用户信息
 * @param {string} key - 键名
 * @param {any} value - 值
 */
export const saveUserInfo = async (key, value) => {
  try {
    const db = await initDB();
    await db.put(STORES.USER_INFO, { key, value });
    return true;
  } catch (error) {
    console.error('❌ 保存用户信息失败:', error);
    return false;
  }
};

/**
 * 获取用户信息
 * @param {string} key - 键名
 */
export const getUserInfo = async (key) => {
  try {
    const db = await initDB();
    const data = await db.get(STORES.USER_INFO, key);
    return data ? data.value : null;
  } catch (error) {
    console.error('❌ 获取用户信息失败:', error);
    return null;
  }
};

/**
 * 清除所有缓存
 */
export const clearAllCache = async () => {
  try {
    const db = await initDB();
    await db.clear(STORES.NOTES);
    await db.clear(STORES.NOTE_CONTENTS);
    await db.clear(STORES.USER_INFO);
    console.log('🧹 所有缓存已清除');
    return true;
  } catch (error) {
    console.error('❌ 清除缓存失败:', error);
    return false;
  }
};

/**
 * 获取缓存统计信息
 */
export const getCacheStats = async () => {
  try {
    const db = await initDB();
    const [noteCount, contentCount] = await Promise.all([
      db.count(STORES.NOTES),
      db.count(STORES.NOTE_CONTENTS)
    ]);
    
    return {
      drafts: noteCount,
      contents: contentCount,
      total: noteCount + contentCount
    };
  } catch (error) {
    console.error('❌ 获取缓存统计失败:', error);
    return { drafts: 0, contents: 0, total: 0 };
  }
};

