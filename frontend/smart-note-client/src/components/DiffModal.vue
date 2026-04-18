<!-- src/components/DiffModal.vue -->
<template>
  <div v-if="show" class="diff-modal-overlay" @click="closeModal">
    <div class="diff-modal-content" @click.stop>
      <div class="diff-header">
        <h3>版本对比</h3>
        <button class="close-btn" @click="closeModal">&times;</button>
      </div>
      
      <div class="diff-info">
        <div class="diff-version">
          <label>版本 {{ versionA }}:</label>
          <select v-model="selectedVersionA" @change="loadVersionContent(selectedVersionA, 'A')">
            <option v-for="version in versions" :key="version.version" :value="version.version">
              {{ version.version }} - {{ formatDate(version.createTime) }}
            </option>
          </select>
        </div>
        <div class="diff-version">
          <label>版本 {{ versionB }}:</label>
          <select v-model="selectedVersionB" @change="loadVersionContent(selectedVersionB, 'B')">
            <option v-for="version in versions" :key="version.version" :value="version.version">
              {{ version.version }} - {{ formatDate(version.createTime) }}
            </option>
          </select>
        </div>
      </div>
      
      <div class="diff-container">
        <div class="diff-column">
          <h4>版本 {{ selectedVersionA }}</h4>
          <pre class="diff-content">{{ contentA }}</pre>
        </div>
        <div class="diff-column">
          <h4>版本 {{ selectedVersionB }}</h4>
          <pre class="diff-content">{{ contentB }}</pre>
        </div>
      </div>
      
      <div class="diff-result" v-html="diffHtml"></div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { diffChars } from 'diff'
import { NoteVersionHistoryVO } from '@/types/api'

interface Props {
  show: boolean
  noteId: number
  versions: NoteVersionHistoryVO[]
}

const props = defineProps<Props>()
const emit = defineEmits(['close'])

const selectedVersionA = ref<number>(props.versions[0]?.version || 1)
const selectedVersionB = ref<number>(props.versions[1]?.version || 2)
const contentA = ref('')
const contentB = ref('')

// 计算差异HTML
const diffHtml = computed(() => {
  if (!contentA.value || !contentB.value) return ''
  
  const diffResult = diffChars(contentA.value, contentB.value)
  let result = '<div class="diff-result-content">'
  
  diffResult.forEach((part) => {
    const className = part.added ? 'diff-added' : part.removed ? 'diff-removed' : 'diff-equal'
    result += `<span class="${className}">${part.value}</span>`
  })
  
  result += '</div>'
  return result
})

// 加载版本内容
const loadVersionContent = async (version: number, target: 'A' | 'B') => {
  // 这里应该调用API获取特定版本的内容
  // 模拟加载过程
  const versionData = props.versions.find(v => v.version === version)
  if (target === 'A') {
    contentA.value = versionData?.content || ''
  } else {
    contentB.value = versionData?.content || ''
  }
}

// 格式化日期
const formatDate = (dateStr: string) => {
  return new Date(dateStr).toLocaleString()
}

// 关闭模态框
const closeModal = () => {
  emit('close')
}

// 初始化加载内容
loadVersionContent(selectedVersionA.value, 'A')
loadVersionContent(selectedVersionB.value, 'B')
</script>

<style scoped>
.diff-modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
}

.diff-modal-content {
  background: white;
  width: 90%;
  max-width: 1200px;
  height: 80vh;
  max-height: 800px;
  border-radius: 8px;
  display: flex;
  flex-direction: column;
}

.diff-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 15px 20px;
  border-bottom: 1px solid #eee;
}

.diff-header h3 {
  margin: 0;
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

.diff-info {
  padding: 15px 20px;
  border-bottom: 1px solid #eee;
  display: flex;
  gap: 20px;
}

.diff-version {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.diff-version label {
  font-weight: bold;
}

.diff-version select {
  padding: 5px;
  border: 1px solid #ccc;
  border-radius: 4px;
}

.diff-container {
  display: flex;
  flex: 1;
  overflow: hidden;
}

.diff-column {
  flex: 1;
  padding: 10px;
  border-right: 1px solid #eee;
  display: flex;
  flex-direction: column;
}

.diff-column:last-child {
  border-right: none;
}

.diff-column h4 {
  margin-top: 0;
  margin-bottom: 10px;
}

.diff-content {
  flex: 1;
  overflow-y: auto;
  white-space: pre-wrap;
  word-break: break-word;
  background-color: #f8f9fa;
  padding: 10px;
  border-radius: 4px;
}

.diff-result {
  padding: 15px 20px;
  flex: 1;
  overflow-y: auto;
  background-color: #fafafa;
}

.diff-result-content {
  line-height: 1.5;
}

.diff-added {
  background-color: #d4edda;
  color: #155724;
}

.diff-removed {
  background-color: #f8d7da;
  color: #721c24;
}

.diff-equal {
  background-color: transparent;
}
</style>