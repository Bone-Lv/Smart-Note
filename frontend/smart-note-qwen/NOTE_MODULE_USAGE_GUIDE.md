# 笔记模块完整使用指南

## 📋 概述

笔记模块是智能笔记协作平台的核心功能，支持：
- 笔记的创建、编辑、删除
- 实时协作编辑（多人在线编辑同一笔记）
- 版本管理和历史回退
- 笔记分享和权限管理
- AI 智能分析
- 批注功能

---

## 🔌 API 接口汇总

### 基础操作

| 功能 | 接口 | 方法 | 说明 |
|------|------|------|------|
| 创建笔记 | `/note` | POST | 创建新笔记 |
| 笔记列表 | `/note/list` | GET | 分页、搜索、筛选 |
| 最近查看 | `/note/recent` | GET | 最近查看的笔记 |
| 最常看3篇 | `/note/top3-frequent` | GET | 查看频率最高的笔记 |
| 笔记详情 | `/note/{noteId}` | GET | 获取笔记完整信息 |
| 更新笔记 | `/note/{noteId}` | PUT | 创建新版本 |
| 同步内容 | `/note/{noteId}/sync` | PUT | 不创建新版本 |
| 删除笔记 | `/note/{noteId}` | DELETE | 移入回收站 |
| 移动笔记 | `/note/{noteId}/move` | PUT | 移动到指定文件夹 |

### 可见性与分享

| 功能 | 接口 | 方法 | 说明 |
|------|------|------|------|
| 设置可见性 | `/note/{noteId}/visibility` | PUT | PRIVATE/FRIENDS/PUBLIC |
| 生成分享链接 | `/note/{noteId}/share` | POST | 生成分享码 |
| 分享码查看 | `/note/share/{shareCode}` | GET | 无需登录 |
| 公开笔记 | `/note/public/{noteId}` | GET | 无需登录 |
| 好友分享 | `/note/shared/{noteId}` | GET | 查看好友分享 |
| 权限列表 | `/note/{noteId}/permissions` | GET | 查看权限设置 |

### 导入导出

| 功能 | 接口 | 方法 | 说明 |
|------|------|------|------|
| 上传图片 | `/note/upload-image` | POST | multipart/form-data |
| 导入Markdown | `/note/import/md` | POST | 支持文件上传 |
| 导入PDF | `/note/import/pdf` | POST | 只读笔记 |
| 导出Markdown | `/note/{noteId}/export/md` | GET | 下载 .md 文件 |
| 导出PDF | `/note/{noteId}/export/pdf` | GET | 下载 .pdf 文件 |

### 版本管理

| 功能 | 接口 | 方法 | 说明 |
|------|------|------|------|
| 版本历史 | `/note/{noteId}/versions` | GET | 所有版本列表 |
| 版本详情 | `/note/{noteId}/versions/{version}` | GET | 指定版本内容 |
| 版本回退 | `/note/{noteId}/versions/{version}/rollback` | POST | 恢复到指定版本 |

### AI 分析

| 功能 | 接口 | 方法 | 说明 |
|------|------|------|------|
| AI分析 | `/note/{noteId}/ai-analyze` | POST | 生成摘要、要点、标签 |

### 批注

| 功能 | 接口 | 方法 | 说明 |
|------|------|------|------|
| 批注列表 | `/note/{noteId}/annotations` | GET | 所有批注 |
| 创建批注 | `/note/{noteId}/annotations` | POST | 添加新批注 |
| 更新批注 | `/note/annotations/{annotationId}` | PUT | 修改批注 |
| 删除批注 | `/note/annotations/{annotationId}` | DELETE | 删除批注 |

### 编辑锁

| 功能 | 接口 | 方法 | 说明 |
|------|------|------|------|
| 获取编辑锁 | `/note/{noteId}/lock` | POST | 请求编辑权限 |
| 释放编辑锁 | `/note/{noteId}/lock` | DELETE | 释放编辑权限 |

---

## 📖 使用示例

### 1. 创建笔记

```javascript
import { createNoteApi } from '@/api/note.js';

const createNote = async () => {
  try {
    const noteData = {
      title: '我的新笔记',
      content: '# 笔记内容\n\n这是一段 Markdown 文本。',
      tags: '学习,笔记,Vue',
      folderId: 123, // 可选
      noteType: 1 // 1-Markdown, 2-PDF
    };
    
    const result = await createNoteApi(noteData);
    console.log('创建成功:', result);
    
    // 跳转到笔记详情页
    router.push(`/app/note/${result.data.noteId}`);
  } catch (error) {
    console.error('创建失败:', error);
  }
};
```

### 2. 获取笔记列表

```javascript
import { getNoteListApi } from '@/api/note.js';

const fetchNotes = async () => {
  try {
    const params = {
      page: 1,
      pageSize: 10,
      keyword: 'Vue', // 搜索关键词（可选）
      tag: '学习', // 标签过滤（可选）
      folderId: 123, // 文件夹过滤（可选）
      sortBy: 'updateTime' // 排序方式：createTime/updateTime/viewCount
    };
    
    const result = await getNoteListApi(params);
    notes.value = result.data.records;
    total.value = result.data.total;
  } catch (error) {
    console.error('获取笔记列表失败:', error);
  }
};
```

### 3. 获取最近查看的笔记

```javascript
import { getRecentViewedNotesApi } from '@/api/note.js';

const fetchRecentNotes = async () => {
  try {
    const result = await getRecentViewedNotesApi(10);
    recentNotes.value = result.data;
  } catch (error) {
    console.error('获取最近笔记失败:', error);
  }
};
```

---

## 🔄 实时协作编辑完整流程

### 步骤 1: 打开笔记详情页

```vue
<script setup>
import { ref, onMounted, onUnmounted } from 'vue';
import { useRoute } from 'vue-router';
import { getNoteDetailApi } from '@/api/note.js';
import wsService from '@/utils/websocket.js';
import { useUserStore } from '@/stores/userStore.js';

const route = useRoute();
const userStore = useUserStore();

const noteId = ref(route.params.noteId);
const noteContent = ref('');
const noteTitle = ref('');
const canEdit = ref(false);
const editingUserId = ref(null);
const loading = ref(true);

// 1. 加载笔记详情
const loadNoteDetail = async () => {
  try {
    loading.value = true;
    const result = await getNoteDetailApi(noteId.value);
    
    noteContent.value = result.data.content;
    noteTitle.value = result.data.title;
    canEdit.value = result.data.canEdit || false;
    editingUserId.value = result.data.currentEditorId || null;
    
    // 2. 如果用户已登录，连接 WebSocket
    if (userStore.isLoggedIn) {
      wsService.connect();
      
      // 3. 通知后端开始查看
      wsService.send('note_view_start', { noteId: noteId.value });
      
      // 4. 注册消息监听器
      setupWebSocketListeners();
    }
  } catch (error) {
    console.error('加载笔记失败:', error);
    ElMessage.error('笔记加载失败');
  } finally {
    loading.value = false;
  }
};

// 5. 设置 WebSocket 监听器
const setupWebSocketListeners = () => {
  // 监听其他人的编辑
  wsService.on('note_content_updated', handleContentUpdated);
  
  // 监听编辑锁授予
  wsService.on('edit_lock_granted', handleLockGranted);
  
  // 监听编辑锁拒绝
  wsService.on('edit_lock_denied', handleLockDenied);
  
  // 监听编辑锁释放
  wsService.on('edit_lock_released', handleLockReleased);
};

// 6. 处理内容更新
const handleContentUpdated = (message) => {
  // 如果是其他人编辑的，更新本地内容
  if (message.noteId === noteId.value && !canEdit.value) {
    noteContent.value = message.content;
    ElMessage.info('笔记内容已更新');
  }
};

// 7. 处理编辑锁授予
const handleLockGranted = (message) => {
  if (message.noteId === noteId.value) {
    canEdit.value = true;
    editingUserId.value = null;
    ElMessage.success('已进入编辑模式');
  }
};

// 8. 处理编辑锁拒绝
const handleLockDenied = (message) => {
  if (message.noteId === noteId.value) {
    canEdit.value = false;
    editingUserId.value = message.editorId;
    ElMessage.warning(`用户 ${message.editorId} 正在编辑该笔记`);
  }
};

// 9. 处理编辑锁释放
const handleLockReleased = (message) => {
  if (message.noteId === noteId.value) {
    editingUserId.value = null;
    ElMessage.info('编辑权限已释放');
  }
};

onMounted(() => {
  loadNoteDetail();
});

// 10. 清理资源
onUnmounted(() => {
  // 释放编辑锁
  if (canEdit.value) {
    wsService.send('note_edit_release', { noteId: noteId.value });
  }
  
  // 通知停止查看
  wsService.send('note_view_end', { noteId: noteId.value });
  
  // 移除监听器
  wsService.off('note_content_updated', handleContentUpdated);
  wsService.off('edit_lock_granted', handleLockGranted);
  wsService.off('edit_lock_denied', handleLockDenied);
  wsService.off('edit_lock_released', handleLockReleased);
});
</script>
```

### 步骤 2: 请求编辑权限

```vue
<script setup>
import { acquireEditLockApi } from '@/api/note.js';
import wsService from '@/utils/websocket.js';

const requestEdit = async () => {
  try {
    // 1. 通过 HTTP API 获取编辑锁
    const result = await acquireEditLockApi(noteId.value);
    
    if (result.data === true) {
      // 2. 通过 WebSocket 通知后端
      wsService.send('note_edit_request', { noteId: noteId.value });
    } else {
      ElMessage.warning('该笔记正在被其他用户编辑');
    }
  } catch (error) {
    console.error('请求编辑权限失败:', error);
    ElMessage.error('请求编辑权限失败');
  }
};
</script>

<template>
  <div>
    <el-button 
      v-if="!canEdit && !editingUserId"
      @click="requestEdit"
      type="primary"
    >
      编辑笔记
    </el-button>
    
    <div v-if="editingUserId" class="editing-status">
      <el-alert 
        title="该笔记正在被编辑" 
        type="warning" 
        :closable="false"
        show-icon
      >
        用户 {{ editingUserId }} 正在编辑此笔记
      </el-alert>
    </div>
    
    <div v-if="canEdit" class="edit-mode">
      <el-alert 
        title="编辑模式" 
        type="success" 
        :closable="false"
        show-icon
      >
        您正在编辑此笔记
      </el-alert>
    </div>
  </div>
</template>
```

### 步骤 3: 编辑时同步内容（防抖）

```vue
<script setup>
import { ref, watch } from 'vue';
import { syncNoteContentApi } from '@/api/note.js';
import wsService from '@/utils/websocket.js';
import { debounce } from '@/utils/debounce.js';

const currentVersion = ref(1);

// 防抖同步函数（500ms）
const debouncedSync = debounce(async (content) => {
  try {
    // 1. 通过 HTTP API 保存到数据库（不创建新版本）
    await syncNoteContentApi(noteId.value, {
      content,
      title: noteTitle.value,
      tags: currentTags.value
    });
    
    // 2. 通过 WebSocket 广播给其他在线用户
    wsService.send('note_content_update', {
      noteId: noteId.value,
      content,
      version: currentVersion.value
    });
  } catch (error) {
    console.error('同步内容失败:', error);
    ElMessage.error('保存失败，请重试');
  }
}, 500);

// 监听内容变化
watch(noteContent, (newContent) => {
  if (canEdit.value) {
    debouncedSync(newContent);
  }
});
</script>
```

### 步骤 4: 停止编辑

```vue
<script setup>
import { releaseEditLockApi } from '@/api/note.js';
import wsService from '@/utils/websocket.js';

const stopEditing = async () => {
  try {
    // 1. 释放 HTTP 编辑锁
    await releaseEditLockApi(noteId.value);
    
    // 2. 通过 WebSocket 通知
    wsService.send('note_edit_release', { noteId: noteId.value });
    
    // 3. 更新本地状态
    canEdit.value = false;
    
    ElMessage.success('已退出编辑模式');
  } catch (error) {
    console.error('停止编辑失败:', error);
  }
};
</script>

<template>
  <div>
    <el-button 
      v-if="canEdit"
      @click="stopEditing"
      type="warning"
    >
      停止编辑
    </el-button>
  </div>
</template>
```

---

## 📝 版本管理

### 查看版本历史

```javascript
import { getNoteVersionsApi } from '@/api/note.js';

const fetchVersions = async () => {
  try {
    const result = await getNoteVersionsApi(noteId.value);
    versions.value = result.data;
  } catch (error) {
    console.error('获取版本历史失败:', error);
  }
};
```

### 查看版本详情

```javascript
import { getNoteVersionDetailApi } from '@/api/note.js';

const viewVersion = async (version) => {
  try {
    const result = await getNoteVersionDetailApi(noteId.value, version);
    versionContent.value = result.data.content;
  } catch (error) {
    console.error('获取版本详情失败:', error);
  }
};
```

### 版本回退

```javascript
import { rollbackNoteVersionApi } from '@/api/note.js';

const rollbackVersion = async (version) => {
  try {
    await ElMessageBox.confirm(
      `确定要回退到版本 ${version} 吗？`,
      '版本回退',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    );
    
    await rollbackNoteVersionApi(noteId.value, version);
    ElMessage.success('版本回退成功');
    
    // 刷新笔记内容
    await loadNoteDetail();
  } catch (error) {
    if (error !== 'cancel') {
      console.error('版本回退失败:', error);
      ElMessage.error('版本回退失败');
    }
  }
};
```

---

##  AI 智能分析

```javascript
import { analyzeNoteWithAIApi } from '@/api/note.js';

const analyzeNote = async (forceRefresh = false) => {
  try {
    loading.value = true;
    const result = await analyzeNoteWithAIApi(noteId.value, forceRefresh);
    
    aiAnalysis.value = {
      summary: result.data.summary,
      keyPoints: result.data.keyPoints,
      suggestedTags: result.data.suggestedTags
    };
    
    ElMessage.success('AI 分析完成');
  } catch (error) {
    console.error('AI 分析失败:', error);
    ElMessage.error('AI 分析失败');
  } finally {
    loading.value = false;
  }
};
```

---

## 📌 批注功能

### 创建批注

```javascript
import { createAnnotationApi } from '@/api/note.js';

const createAnnotation = async () => {
  try {
    const annotationData = {
      content: '这是一个重要的知识点',
      lineNumber: 10,
      selectionText: '被批注的文本内容'
    };
    
    const result = await createAnnotationApi(noteId.value, annotationData);
    annotations.value.push(result.data);
    
    ElMessage.success('批注创建成功');
  } catch (error) {
    console.error('创建批注失败:', error);
    ElMessage.error('创建批注失败');
  }
};
```

### 获取批注列表

```javascript
import { getNoteAnnotationsApi } from '@/api/note.js';

const fetchAnnotations = async () => {
  try {
    const result = await getNoteAnnotationsApi(noteId.value);
    annotations.value = result.data;
  } catch (error) {
    console.error('获取批注列表失败:', error);
  }
};
```

### 删除批注

```javascript
import { deleteAnnotationApi } from '@/api/note.js';

const deleteAnnotation = async (annotationId) => {
  try {
    await ElMessageBox.confirm('确定要删除这个批注吗？', '删除批注', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    });
    
    await deleteAnnotationApi(annotationId);
    annotations.value = annotations.value.filter(a => a.id !== annotationId);
    
    ElMessage.success('批注删除成功');
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除批注失败:', error);
      ElMessage.error('删除批注失败');
    }
  }
};
```

---

## 🔗 笔记分享

### 生成分享链接

```javascript
import { generateShareLinkApi } from '@/api/note.js';

const generateShareLink = async () => {
  try {
    const result = await generateShareLinkApi(noteId.value);
    
    shareLink.value = result.data.shareUrl;
    shareCode.value = result.data.shareCode;
    
    // 复制到剪贴板
    await navigator.clipboard.writeText(shareLink.value);
    ElMessage.success('分享链接已复制到剪贴板');
  } catch (error) {
    console.error('生成分享链接失败:', error);
    ElMessage.error('生成分享链接失败');
  }
};
```

### 设置笔记可见性

```javascript
import { updateNoteVisibilityApi } from '@/api/note.js';

const setVisibility = async (visibility, friendUserIds = []) => {
  try {
    await updateNoteVisibilityApi(noteId.value, {
      visibility, // PRIVATE / FRIENDS_VIEW / FRIENDS_EDIT / PUBLIC
      friendUserIds // 好友 ID 列表（visibility 为 FRIENDS 时必填）
    });
    
    ElMessage.success('可见性设置成功');
  } catch (error) {
    console.error('设置可见性失败:', error);
    ElMessage.error('设置可见性失败');
  }
};
```

---

## ⚠️ 注意事项

### 1. 内存管理

**❌ 错误做法：**
```javascript
onMounted(() => {
  wsService.on('note_content_updated', handler);
});
// ❌ 忘记在 onUnmounted 中移除监听器
```

**✅ 正确做法：**
```javascript
onMounted(() => {
  wsService.on('note_content_updated', handler);
});

onUnmounted(() => {
  wsService.off('note_content_updated', handler);
  wsService.send('note_edit_release', { noteId });
  wsService.send('note_view_end', { noteId });
});
```

### 2. 防抖优化

**✅ 推荐做法：**
```javascript
const debouncedSync = debounce(syncContent, 500);

watch(noteContent, (newContent) => {
  debouncedSync(newContent);
});
```

**❌ 不推荐：**
```javascript
// 每次输入都发送请求
watch(noteContent, async (newContent) => {
  await syncNoteContentApi(noteId, { content: newContent });
});
```

### 3. 编辑锁管理

- ✅ 同一时间只能有一个编辑者
- ✅ 编辑者断开连接时，后端自动释放锁
- ✅ 组件卸载时必须释放编辑锁
- ✅ 收到 `edit_lock_denied` 时显示只读模式

### 4. 版本冲突处理

```javascript
try {
  await updateNoteApi(noteId, {
    title: newTitle,
    content: newContent,
    version: currentVersion.value,
    saveAsNewVersion: true
  });
} catch (error) {
  if (error.response?.status === 409) {
    // 版本冲突
    ElMessage.warning('笔记已被其他人修改，请刷新后重试');
    await loadNoteDetail(); // 重新加载最新内容
  }
}
```

---

## 📚 相关文档

- [WebSocket 使用指南](./WEBSOCKET_USAGE_GUIDE.md)
- [防抖节流工具函数](./MEMORY.md#防抖节流工具函数规范与应用场景)
- [笔记模块实现规范](./MEMORY.md#笔记模块完整实现规范)

---

**最后更新:** 2026-04-17  
**维护者:** 前端开发团队
