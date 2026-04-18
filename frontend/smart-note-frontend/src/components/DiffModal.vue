<template>
  <div v-if="visible" class="diff-modal-overlay" @click="closeModal">
    <div class="diff-modal-content" @click.stop>
      <div class="diff-modal-header">
        <h3>版本对比</h3>
        <button class="close-btn" @click="closeModal">×</button>
      </div>
      
      <div class="diff-modal-body">
        <div class="version-selector">
          <select v-model="selectedVersion1">
            <option value="">选择第一个版本</option>
            <option 
              v-for="version in versions" 
              :key="version.version" 
              :value="version.version"
            >
              {{ version.version }} - {{ formatDate(version.createTime) }}
            </option>
          </select>
          
          <select v-model="selectedVersion2">
            <option value="">选择第二个版本</option>
            <option 
              v-for="version in versions" 
              :key="version.version" 
              :value="version.version"
            >
              {{ version.version }} - {{ formatDate(version.createTime) }}
            </option>
          </select>
        </div>
        
        <div class="diff-content" v-if="diffHtml">
          <div v-html="diffHtml"></div>
        </div>
        
        <div class="no-diff" v-else>
          <p>请选择两个不同的版本进行对比</p>
        </div>
      </div>
      
      <div class="diff-modal-footer">
        <button class="btn btn-primary" @click="rollbackToVersion">回退到选定版本</button>
        <button class="btn btn-secondary" @click="closeModal">关闭</button>
      </div>
    </div>
  </div>
</template>

<script>
import { diffChars } from 'diff';

export default {
  name: 'DiffModal',
  props: {
    visible: {
      type: Boolean,
      default: false
    },
    versions: {
      type: Array,
      default: () => []
    },
    currentNoteId: {
      type: Number,
      required: true
    }
  },
  data() {
    return {
      selectedVersion1: '',
      selectedVersion2: '',
      diffHtml: ''
    };
  },
  watch: {
    selectedVersion1() {
      this.generateDiff();
    },
    selectedVersion2() {
      this.generateDiff();
    }
  },
  methods: {
    closeModal() {
      this.$emit('close');
    },

    async generateDiff() {
      if (!this.selectedVersion1 || !this.selectedVersion2 || this.selectedVersion1 === this.selectedVersion2) {
        this.diffHtml = '';
        return;
      }

      try {
        // 获取两个版本的内容
        const [version1Data, version2Data] = await Promise.all([
          this.$api.note.getVersionDetail(this.currentNoteId, this.selectedVersion1),
          this.$api.note.getVersionDetail(this.currentNoteId, this.selectedVersion2)
        ]);

        const content1 = version1Data.data.content || '';
        const content2 = version2Data.data.content || '';

        // 生成差异
        const diff = diffChars(content1, content2);
        this.diffHtml = this.formatDiff(diff);
      } catch (error) {
        console.error('生成差异失败:', error);
      }
    },

    formatDiff(diff) {
      let result = '<div class="diff-wrapper">';
      
      diff.forEach(part => {
        const text = part.value.replace(/\n/g, '<br>');
        
        if (part.added) {
          result += `<ins style="background:#e6ffe6;">${text}</ins>`;
        } else if (part.removed) {
          result += `<del style="background:#ffe6e6;">${text}</del>`;
        } else {
          result += `<span>${text}</span>`;
        }
      });
      
      result += '</div>';
      return result;
    },

    async rollbackToVersion() {
      if (!this.selectedVersion2) {
        alert('请选择要回退到的版本');
        return;
      }

      if (!confirm('确定要回退到选定版本吗？此操作不可逆。')) {
        return;
      }

      try {
        await this.$api.note.rollbackToVersion(this.currentNoteId, this.selectedVersion2);
        alert('版本回退成功！');
        this.closeModal();
        this.$emit('version-rollbacked');
      } catch (error) {
        alert('版本回退失败：' + error.message);
      }
    },

    formatDate(dateString) {
      if (!dateString) return '';
      const date = new Date(dateString);
      return date.toLocaleString();
    }
  }
};
</script>

<style scoped>
.diff-modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
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
  overflow: hidden;
}

.diff-modal-header {
  padding: 16px 24px;
  border-bottom: 1px solid #e1e5e9;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.diff-modal-header h3 {
  margin: 0;
  font-size: 18px;
}

.close-btn {
  background: none;
  border: none;
  font-size: 24px;
  cursor: pointer;
  padding: 0;
  width: 30px;
  height: 30px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.diff-modal-body {
  padding: 24px;
  flex: 1;
  overflow-y: auto;
}

.version-selector {
  display: flex;
  gap: 16px;
  margin-bottom: 24px;
}

.version-selector select {
  flex: 1;
  padding: 8px 12px;
  border: 1px solid #d0d7de;
  border-radius: 6px;
}

.diff-content {
  background: #fafbfc;
  border: 1px solid #e1e5e9;
  border-radius: 6px;
  padding: 16px;
  overflow-x: auto;
  max-height: 400px;
}

.no-diff {
  text-align: center;
  color: #656d76;
  padding: 40px 0;
}

.diff-modal-footer {
  padding: 16px 24px;
  border-top: 1px solid #e1e5e9;
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.btn {
  padding: 8px 16px;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
}

.btn-primary {
  background: #0969da;
  color: white;
}

.btn-secondary {
  background: #f6f8fa;
  color: #24292f;
  border: 1px solid #d0d7de;
}

.diff-wrapper ins {
  background: #e6ffe6;
  text-decoration: none;
}

.diff-wrapper del {
  background: #ffe6e6;
  text-decoration: line-through;
}
</style>