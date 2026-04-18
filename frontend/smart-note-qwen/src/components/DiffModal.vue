<template>
  <div v-if="isOpen" class="diff-modal-overlay" @click="closeModal">
    <div class="diff-modal-content" @click.stop>
      <div class="diff-modal-header">
        <h3>{{ title }}</h3>
        <button @click="closeModal" class="close-button">×</button>
      </div>
      
      <div class="diff-controls">
        <div class="version-selector">
          <label>版本A: </label>
          <select v-model="selectedVersionA">
            <option v-for="version in versions" :key="version.version" :value="version.version">
              {{ version.version }} - {{ formatDate(version.createTime) }}
            </option>
          </select>
        </div>
        
        <div class="version-selector">
          <label>版本B: </label>
          <select v-model="selectedVersionB">
            <option v-for="version in versions" :key="version.version" :value="version.version">
              {{ version.version }} - {{ formatDate(version.createTime) }}
            </option>
          </select>
        </div>
      </div>
      
      <div class="diff-content">
        <div class="diff-section" v-if="diffContent">
          <div class="diff-stats">
            <span class="added">+{{ diffStats.added }} 行新增</span>
            <span class="removed">-{{ diffStats.removed }} 行删除</span>
            <span class="changed">~{{ diffStats.changed }} 行修改</span>
          </div>
          
          <div class="diff-output" v-html="diffContent"></div>
        </div>
        
        <div v-else class="no-diff">
          <p>请选择两个不同的版本进行比较</p>
        </div>
      </div>
      
      <div class="diff-actions">
        <button @click="rollbackToVersion" :disabled="!canRollback" class="rollback-btn">
          回退到版本 {{ selectedVersionB }}
        </button>
        <button @click="closeModal" class="cancel-btn">关闭</button>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, computed, watch } from 'vue';
import Diff from 'diff';
import MarkdownIt from 'markdown-it';

export default {
  name: 'DiffModal',
  props: {
    isOpen: Boolean,
    versions: {
      type: Array,
      default: () => []
    },
    noteId: Number
  },
  emits: ['close', 'rollback'],
  setup(props, { emit }) {
    const selectedVersionA = ref('');
    const selectedVersionB = ref('');
    const diffContent = ref('');
    const title = ref('版本对比');
    
    const md = new MarkdownIt();

    // 初始化版本选择
    watch(() => props.versions, (newVersions) => {
      if (newVersions && newVersions.length >= 2) {
        selectedVersionA.value = newVersions[newVersions.length - 1]?.version || '';
        selectedVersionB.value = newVersions[newVersions.length - 2]?.version || '';
      }
    }, { immediate: true });

    // 计算差异
    watch([selectedVersionA, selectedVersionB], ([versionA, versionB]) => {
      if (versionA && versionB && versionA !== versionB) {
        computeDiff(versionA, versionB);
      } else {
        diffContent.value = '';
      }
    });

    const computeDiff = (versionA, versionB) => {
      const versionAObj = props.versions.find(v => v.version == versionA);
      const versionBObj = props.versions.find(v => v.version == versionB);
      
      if (versionAObj && versionBObj) {
        const contentA = versionAObj.content || '';
        const contentB = versionBObj.content || '';
        
        // 使用diff库计算差异
        const diff = Diff.diffLines(contentA, contentB);
        
        let output = '<div class="diff-lines">';
        diff.forEach(part => {
          const lines = part.value.split('\n').filter(line => line.trim() !== '');
          lines.forEach(line => {
            if (part.added) {
              output += `<div class="diff-line added-line">+ ${md.renderInline(line)}</div>`;
            } else if (part.removed) {
              output += `<div class="diff-line removed-line">- ${md.renderInline(line)}</div>`;
            } else {
              output += `<div class="diff-line common-line">  ${md.renderInline(line)}</div>`;
            }
          });
        });
        output += '</div>';
        
        diffContent.value = output;
      }
    };

    const diffStats = computed(() => {
      if (!diffContent.value) return { added: 0, removed: 0, changed: 0 };
      
      const added = (diffContent.value.match(/added-line/g) || []).length;
      const removed = (diffContent.value.match(/removed-line/g) || []).length;
      const changed = Math.max(added, removed);
      
      return { added, removed, changed };
    });

    const canRollback = computed(() => {
      return props.noteId && selectedVersionB.value && selectedVersionB.value !== '';
    });

    const closeModal = () => {
      emit('close');
    };

    const rollbackToVersion = () => {
      if (canRollback.value) {
        emit('rollback', props.noteId, selectedVersionB.value);
        closeModal();
      }
    };

    const formatDate = (dateString) => {
      return new Date(dateString).toLocaleString();
    };

    return {
      selectedVersionA,
      selectedVersionB,
      diffContent,
      title,
      diffStats,
      canRollback,
      closeModal,
      rollbackToVersion,
      formatDate
    };
  }
};
</script>

<style scoped>
.diff-modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
}

.diff-modal-content {
  background: white;
  border-radius: 8px;
  width: 90%;
  max-width: 1000px;
  max-height: 80vh;
  display: flex;
  flex-direction: column;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.3);
}

.diff-modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 24px;
  border-bottom: 1px solid #eee;
  background: #f8f9fa;
  border-radius: 8px 8px 0 0;
}

.diff-modal-header h3 {
  margin: 0;
  color: #333;
}

.close-button {
  background: none;
  border: none;
  font-size: 24px;
  cursor: pointer;
  color: #999;
  padding: 0;
  width: 30px;
  height: 30px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.close-button:hover {
  color: #333;
}

.diff-controls {
  padding: 16px;
  background: #fafafa;
  border-bottom: 1px solid #eee;
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
}

.version-selector {
  display: flex;
  align-items: center;
  gap: 8px;
}

.version-selector label {
  font-weight: 500;
  color: #555;
}

.version-selector select {
  padding: 6px 12px;
  border: 1px solid #ddd;
  border-radius: 4px;
  background: white;
}

.diff-content {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
}

.diff-stats {
  display: flex;
  gap: 16px;
  margin-bottom: 16px;
  padding: 12px;
  background: #f8f9fa;
  border-radius: 4px;
}

.added {
  color: #28a745;
  font-weight: 500;
}

.removed {
  color: #dc3545;
  font-weight: 500;
}

.changed {
  color: #ffc107;
  font-weight: 500;
}

.diff-output {
  background: #fff;
  border: 1px solid #e1e5e9;
  border-radius: 4px;
  overflow-x: auto;
}

.diff-lines {
  font-family: 'Monaco', 'Consolas', monospace;
  font-size: 14px;
  line-height: 1.5;
}

.diff-line {
  padding: 2px 12px;
  border-left: 3px solid transparent;
}

.added-line {
  background-color: #e6ffec;
  border-left-color: #34d058;
}

.removed-line {
  background-color: #ffeef0;
  border-left-color: #d73a49;
}

.common-line {
  background-color: #fafbfc;
}

.no-diff {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 200px;
  color: #666;
  font-style: italic;
}

.diff-actions {
  padding: 16px;
  border-top: 1px solid #eee;
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  background: #f8f9fa;
}

.rollback-btn, .cancel-btn {
  padding: 8px 16px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
}

.rollback-btn {
  background: #007bff;
  color: white;
}

.rollback-btn:hover:not(:disabled) {
  background: #0056b3;
}

.rollback-btn:disabled {
  background: #ccc;
  cursor: not-allowed;
}

.cancel-btn {
  background: #6c757d;
  color: white;
}

.cancel-btn:hover {
  background: #545b62;
}
</style>