<template>
  <el-dialog
    v-model="visible"
    title="批量移动笔记"
    width="500px"
    :close-on-click-modal="false"
    @close="handleClose"
  >
    <div class="batch-move-content">
      <!-- 已选笔记列表 -->
      <div class="selected-notes">
        <h4>已选择 {{ selectedNotes.length }} 个笔记：</h4>
        <div class="notes-preview">
          <div 
            v-for="note in selectedNotes.slice(0, 5)" 
            :key="note.id"
            class="note-item"
          >
            <i class="fas fa-file-alt"></i>
            <span class="note-title">{{ note.title }}</span>
          </div>
          <div v-if="selectedNotes.length > 5" class="more-hint">
            ...还有 {{ selectedNotes.length - 5 }} 个笔记
          </div>
        </div>
      </div>
      
      <!-- 目标文件夹选择 -->
      <div class="target-folder">
        <el-form label-width="100px">
          <el-form-item label="目标文件夹">
            <el-select 
              v-model="targetFolderId" 
              placeholder="选择目标文件夹"
              clearable
              style="width: 100%"
            >
              <el-option 
                label="根目录（无文件夹）" 
                :value="null"
              />
              <el-option 
                v-for="folder in allFolders" 
                :key="folder.id" 
                :label="folder.name" 
                :value="folder.id"
              />
            </el-select>
          </el-form-item>
        </el-form>
      </div>
      
      <!-- 进度提示 -->
      <div v-if="moving" class="progress-info">
        <el-progress 
          :percentage="progress" 
          :status="progressStatus"
        />
        <p class="progress-text">{{ progressText }}</p>
      </div>
    </div>
    
    <template #footer>
      <el-button @click="handleClose" :disabled="moving">取消</el-button>
      <el-button 
        type="primary" 
        @click="handleMove" 
        :loading="moving"
        :disabled="!targetFolderId && targetFolderId !== null"
      >
        确认移动
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, computed, watch } from 'vue';
import { ElMessage } from 'element-plus';
import { useFolderStore } from '../stores/folderStore.js';
import { moveNoteApi } from '../api/note.js';

const props = defineProps({
  modelValue: {
    type: Boolean,
    required: true
  },
  selectedNotes: {
    type: Array,
    required: true,
    default: () => []
  }
});

const emit = defineEmits(['update:modelValue', 'moved']);

const folderStore = useFolderStore();

// 状态
const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
});

const targetFolderId = ref(null);
const moving = ref(false);
const progress = ref(0);
const progressStatus = ref('');
const progressText = ref('');

// 所有文件夹列表
const allFolders = computed(() => folderStore.allFolders);

// 关闭对话框
const handleClose = () => {
  if (!moving.value) {
    visible.value = false;
    resetState();
  }
};

// 重置状态
const resetState = () => {
  targetFolderId.value = null;
  progress.value = 0;
  progressStatus.value = '';
  progressText.value = '';
};

// 执行批量移动
const handleMove = async () => {
  if (props.selectedNotes.length === 0) {
    ElMessage.warning('没有选择任何笔记');
    return;
  }
  
  moving.value = true;
  progress.value = 0;
  progressStatus.value = '';
  
  const total = props.selectedNotes.length;
  let successCount = 0;
  let failCount = 0;
  const failedNotes = [];
  
  try {
    // 循环调用移动接口
    for (let i = 0; i < total; i++) {
      const note = props.selectedNotes[i];
      progressText.value = `正在移动: ${note.title} (${i + 1}/${total})`;
      
      try {
        await moveNoteApi(note.id, targetFolderId.value);
        successCount++;
        progress.value = Math.round(((i + 1) / total) * 100);
      } catch (error) {
        console.error(`移动笔记 "${note.title}" 失败:`, error);
        failCount++;
        failedNotes.push(note.title);
      }
    }
    
    // 显示结果
    if (failCount === 0) {
      ElMessage.success(`✅ 成功移动 ${successCount} 个笔记`);
      progressStatus.value = 'success';
      progressText.value = '全部完成！';
    } else if (successCount === 0) {
      ElMessage.error(`❌ 移动失败，共 ${failCount} 个笔记`);
      progressStatus.value = 'exception';
    } else {
      ElMessage.warning({
        message: `⚠️ 部分成功：${successCount} 个成功，${failCount} 个失败`,
        duration: 5000
      });
      progressStatus.value = 'warning';
      
      if (failedNotes.length <= 3) {
        console.warn('失败的笔记:', failedNotes);
      }
    }
    
    // 通知父组件
    emit('moved', targetFolderId.value);
    
    // 延迟关闭对话框
    setTimeout(() => {
      handleClose();
    }, 1000);
    
  } catch (error) {
    console.error('批量移动出错:', error);
    ElMessage.error('批量移动失败');
    progressStatus.value = 'exception';
  } finally {
    moving.value = false;
  }
};

// 监听对话框打开，重置状态
watch(visible, (newVal) => {
  if (newVal) {
    resetState();
  }
});
</script>

<style scoped>
.batch-move-content {
  padding: 10px 0;
}

.selected-notes {
  margin-bottom: 20px;
}

.selected-notes h4 {
  margin: 0 0 10px 0;
  font-size: 14px;
  color: #666;
}

.notes-preview {
  max-height: 150px;
  overflow-y: auto;
  border: 1px solid #eee;
  border-radius: 6px;
  padding: 10px;
  background: #f9f9f9;
}

.note-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 0;
  font-size: 13px;
  color: #333;
}

.note-item i {
  color: #409eff;
}

.note-title {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.more-hint {
  padding: 6px 0;
  font-size: 12px;
  color: #999;
  text-align: center;
}

.target-folder {
  margin-bottom: 20px;
}

.progress-info {
  padding: 15px;
  background: #f5f7fa;
  border-radius: 6px;
}

.progress-text {
  margin: 10px 0 0 0;
  font-size: 13px;
  color: #666;
  text-align: center;
}
</style>
