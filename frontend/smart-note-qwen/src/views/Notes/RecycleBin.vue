<template>
  <div class="recycle-bin">
    <div class="header">
      <h2>回收站</h2>
      <el-alert
        v-if="recycleBinStore.totalItems > 0"
        title="注意：删除的项目将在 5 分钟后自动彻底删除"
        type="warning"
        :closable="false"
        show-icon
      />
    </div>

    <!-- 标签页 -->
    <el-tabs v-model="activeTab" @tab-change="handleTabChange" class="recycle-tabs">
      <el-tab-pane label="笔记" name="notes">
        <!-- 笔记列表 -->
        <el-table 
          v-loading="recycleBinStore.loading"
          :data="recycleBinStore.notes" 
          style="width: 100%"
          empty-text="暂无回收站笔记"
        >
          <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip />
          <el-table-column prop="deleteTime" label="删除时间" width="180">
            <template #default="{ row }">
              <span :class="{ 'countdown': isWithinFiveMinutes(row.deleteTime) }">
                {{ formatDeleteTime(row.deleteTime) }}
              </span>
            </template>
          </el-table-column>
          <el-table-column prop="folderName" label="原文件夹" width="120" show-overflow-tooltip />
          <el-table-column label="操作" width="200" fixed="right">
            <template #default="{ row }">
              <el-button 
                size="small" 
                type="primary"
                @click="handleRestoreNote(row.id)"
                :loading="restoringNote === row.id"
              >
                还原
              </el-button>
              <el-button 
                size="small" 
                type="danger"
                @click="handleDeleteNote(row.id)"
                :loading="deletingNote === row.id"
              >
                彻底删除
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="文件夹" name="folders">
        <!-- 文件夹列表 -->
        <el-table 
          v-loading="recycleBinStore.loading"
          :data="recycleBinStore.folders" 
          style="width: 100%"
          empty-text="暂无回收站文件夹"
        >
          <el-table-column prop="name" label="文件夹名称" min-width="200" show-overflow-tooltip>
            <template #default="{ row }">
              <el-icon><Folder /></el-icon>
              <span style="margin-left: 8px">{{ row.name }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="noteCount" label="包含笔记数" width="120" />
          <el-table-column prop="deleteTime" label="删除时间" width="180">
            <template #default="{ row }">
              <span :class="{ 'countdown': isWithinFiveMinutes(row.deleteTime) }">
                {{ formatDeleteTime(row.deleteTime) }}
              </span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="200" fixed="right">
            <template #default="{ row }">
              <el-button 
                size="small" 
                type="primary"
                @click="handleRestoreFolder(row.id)"
                :loading="restoringFolder === row.id"
              >
                还原
              </el-button>
              <el-button 
                size="small" 
                type="danger"
                @click="handleDeleteFolder(row.id)"
                :loading="deletingFolder === row.id"
              >
                彻底删除
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>

    <!-- 底部操作栏 -->
    <div v-if="recycleBinStore.totalItems > 0" class="footer-actions">
      <el-button 
        type="danger"
        @click="handleEmptyRecycleBin"
        :loading="emptying"
      >
        <el-icon><Delete /></el-icon>
        清空回收站 ({{ recycleBinStore.totalItems }})
      </el-button>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, computed } from 'vue';
import { useRecycleBinStore } from '@/stores/recycleBinStore.js';
import { Folder, Delete } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';

const recycleBinStore = useRecycleBinStore();
const activeTab = ref('notes');
const timer = ref(null);

// 操作中的项目 ID
const restoringNote = ref(null);
const deletingNote = ref(null);
const restoringFolder = ref(null);
const deletingFolder = ref(null);
const emptying = ref(false);

// 初始化
onMounted(async () => {
  await recycleBinStore.refreshAll();
  
  // 启动倒计时定时器
  timer.value = setInterval(() => {
    // 触发重新渲染，更新倒计时
    recycleBinStore.notes = [...recycleBinStore.notes];
    recycleBinStore.folders = [...recycleBinStore.folders];
  }, 1000);
});

// 组件卸载时清理定时器
onUnmounted(() => {
  if (timer.value) {
    clearInterval(timer.value);
  }
});

// 切换标签页
const handleTabChange = (tab) => {
  recycleBinStore.setActiveTab(tab);
};

// 格式化删除时间
const formatDeleteTime = (deleteTime) => {
  return recycleBinStore.formatDeleteTime(deleteTime);
};

// 判断是否在 5 分钟内
const isWithinFiveMinutes = (deleteTime) => {
  if (!deleteTime) return false;
  const diff = Date.now() - new Date(deleteTime).getTime();
  return diff <= 5 * 60 * 1000;
};

// 还原笔记
const handleRestoreNote = async (noteId) => {
  try {
    restoringNote.value = noteId;
    await recycleBinStore.restoreNote(noteId);
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('还原失败');
    }
  } finally {
    restoringNote.value = null;
  }
};

// 彻底删除笔记
const handleDeleteNote = async (noteId) => {
  try {
    deletingNote.value = noteId;
    await recycleBinStore.permanentlyDeleteNote(noteId);
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败');
    }
  } finally {
    deletingNote.value = null;
  }
};

// 还原文件夹
const handleRestoreFolder = async (folderId) => {
  try {
    restoringFolder.value = folderId;
    await recycleBinStore.restoreFolder(folderId);
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('还原失败');
    }
  } finally {
    restoringFolder.value = null;
  }
};

// 彻底删除文件夹
const handleDeleteFolder = async (folderId) => {
  try {
    deletingFolder.value = folderId;
    await recycleBinStore.permanentlyDeleteFolder(folderId);
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败');
    }
  } finally {
    deletingFolder.value = null;
  }
};

// 清空回收站
const handleEmptyRecycleBin = async () => {
  try {
    emptying.value = true;
    await recycleBinStore.emptyRecycleBin();
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('清空失败');
    }
  } finally {
    emptying.value = false;
  }
};
</script>

<style scoped>
.recycle-bin {
  padding: 20px;
  height: 100%;
  display: flex;
  flex-direction: column;
}

.header {
  margin-bottom: 20px;
}

.header h2 {
  margin: 0 0 12px 0;
  font-size: 24px;
  font-weight: 600;
}

.recycle-tabs {
  flex: 1;
  display: flex;
  flex-direction: column;
}

:deep(.el-tabs__content) {
  flex: 1;
  overflow: auto;
}

:deep(.el-table) {
  border-radius: 8px;
}

:deep(.el-table__empty-block) {
  min-height: 300px;
}

.footer-actions {
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px solid #e4e7ed;
  display: flex;
  justify-content: flex-end;
}

.countdown {
  color: #e6a23c;
  font-weight: 600;
}

.el-icon {
  vertical-align: middle;
}
</style>
