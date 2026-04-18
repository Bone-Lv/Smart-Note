<!-- src/components/MarkdownRenderer.vue -->
<template>
  <div class="markdown-renderer">
    <div v-html="renderedContent" ref="contentRef" class="content"></div>
    
    <!-- 批注弹窗 -->
    <div 
      v-if="showAnnotationModal" 
      class="annotation-modal"
      @click="closeAnnotationModal"
    >
      <div class="annotation-content" @click.stop>
        <h3>批注</h3>
        <textarea 
          v-model="currentAnnotation.content" 
          placeholder="请输入批注内容..."
          rows="4"
        ></textarea>
        <div class="modal-actions">
          <button @click="saveAnnotation">保存</button>
          <button @click="closeAnnotationModal">取消</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, nextTick } from 'vue'
import { createMarkdownIt } from '@/utils/markdown-it-plugins'
import { CreateAnnotationDTO, AnnotationVO } from '@/types/api'

interface Props {
  content: string
  annotations?: AnnotationVO[]
  noteId: number
  version: number
}

const props = defineProps<Props>()
const emit = defineEmits(['addAnnotation', 'updateAnnotation'])

const contentRef = ref<HTMLDivElement>()
const showAnnotationModal = ref(false)
const currentAnnotation = ref<CreateAnnotationDTO>({
  content: '',
  targetContent: '',
  startPosition: 0,
  endPosition: 0
})

const md = createMarkdownIt()

// 渲染Markdown内容
const renderedContent = computed(() => {
  return md.render(props.content)
})

// 显示批注弹窗
const showAnnotationPopup = (event: MouseEvent) => {
  const selection = window.getSelection()
  if (!selection || selection.toString().length === 0) return
  
  const selectedText = selection.toString()
  const range = selection.getRangeAt(0)
  const preSelectionRange = range.cloneRange()
  preSelectionRange.selectNodeContents(contentRef.value!)
  preSelectionRange.setEnd(range.startContainer, range.startOffset)
  const start = preSelectionRange.toString().length
  const end = start + selectedText.length
  
  currentAnnotation.value = {
    content: '',
    targetContent: selectedText,
    startPosition: start,
    endPosition: end
  }
  
  showAnnotationModal.value = true
  selection.removeAllRanges()
}

// 保存批注
const saveAnnotation = () => {
  if (currentAnnotation.value.content.trim()) {
    emit('addAnnotation', { ...currentAnnotation.value, noteId: props.noteId })
  }
  closeAnnotationModal()
}

// 关闭批注弹窗
const closeAnnotationModal = () => {
  showAnnotationModal.value = false
  currentAnnotation.value = {
    content: '',
    targetContent: '',
    startPosition: 0,
    endPosition: 0
  }
}

onMounted(() => {
  // 添加鼠标抬起事件监听，用于选择文本以添加批注
  if (contentRef.value) {
    contentRef.value.addEventListener('mouseup', showAnnotationPopup)
  }
})
</script>

<style scoped>
.markdown-renderer {
  position: relative;
}

.content {
  padding: 20px;
  line-height: 1.6;
}

.content :deep(h1), .content :deep(h2), .content :deep(h3) {
  margin-top: 1.5em;
  margin-bottom: 0.5em;
}

.content :deep(pre) {
  background-color: #f6f8fa;
  border-radius: 6px;
  padding: 16px;
  overflow: auto;
  font-size: 85%;
  line-height: 1.45;
}

.content :deep(code) {
  background-color: rgba(175, 184, 193, 0.2);
  border-radius: 6px;
  padding: 0.2em 0.4em;
  font-size: 85%;
}

.annotation-modal {
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

.annotation-content {
  background: white;
  padding: 20px;
  border-radius: 8px;
  width: 400px;
  max-width: 90vw;
}

.annotation-content h3 {
  margin-top: 0;
}

.annotation-content textarea {
  width: 100%;
  padding: 10px;
  border: 1px solid #ccc;
  border-radius: 4px;
  resize: vertical;
}

.modal-actions {
  margin-top: 15px;
  text-align: right;
}

.modal-actions button {
  margin-left: 10px;
  padding: 8px 16px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}

.modal-actions button:first-child {
  background-color: #007bff;
  color: white;
}

.modal-actions button:last-child {
  background-color: #6c757d;
  color: white;
}
</style>