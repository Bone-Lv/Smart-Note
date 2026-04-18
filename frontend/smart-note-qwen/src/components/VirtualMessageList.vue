<template>
  <div class="virtual-message-list" ref="listContainer">
    <!-- 加载更多提示 -->
    <div v-if="loadingMore" class="loading-more">
      <i class="fas fa-spinner fa-spin"></i>
      <span>加载中...</span>
    </div>
    
    <div class="message-container">
      <div 
        v-for="(item, index) in messages" 
        :key="item.id"
        class="message-item"
        :class="{ 'message-user': item.role === 'user', 'message-assistant': item.role === 'assistant' }"
      >
        <div class="message-avatar">
          <i :class="getMessageIcon(item.role)"></i>
        </div>
        <div class="message-content">
          <!-- 用户消息 -->
          <div v-if="item.role === 'user'">
            <div class="message-text">{{ item.content }}</div>
            
            <!-- 显示附加的文件 -->
            <div v-if="item.files && item.files.length > 0" class="message-files">
              <div 
                v-for="(file, fileIndex) in item.files" 
                :key="fileIndex" 
                class="attached-file"
              >
                <i class="fas fa-file-pdf" v-if="file.endsWith('.pdf')"></i>
                <i class="fas fa-image" v-else-if="file.match(/\.(jpg|jpeg|png|gif)$/i)"></i>
                <i class="fas fa-file" v-else></i>
                <span>{{ file }}</span>
              </div>
            </div>
          </div>
          
          <!-- AI助手消息（支持Markdown、代码高亮、LaTeX） -->
          <div v-else class="message-text markdown-body">
            <div v-html="renderMarkdown(item.content)"></div>
          </div>
          
          <div class="message-time">{{ formatTime(item.createTime) }}</div>
        </div>
      </div>
      
      <div v-if="!hasMore && messages.length > 0" class="no-more-messages">
        没有更多消息了
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, ref, onUnmounted } from 'vue';
import { RecycleScroller } from 'vue-virtual-scroller';
import 'vue-virtual-scroller/dist/vue-virtual-scroller.css';
import md from '../utils/markdownRenderer.js';
// import markdownWorkerManager from '../utils/markdownWorker.js'; // 如需使用Worker，取消注释

const props = defineProps({
  messages: {
    type: Array,
    required: true
  },
  loadingMore: {
    type: Boolean,
    default: false
  },
  hasMore: {
    type: Boolean,
    default: true
  }
});

const emit = defineEmits(['scroll']);

// Markdown渲染缓存
const renderCache = new Map();
const MAX_CACHE_SIZE = 100;

// 是否使用Worker渲染（可根据性能测试决定是否启用）
const useWorkerRender = ref(false); // 默认false，需要时改为true

// 获取消息图标
const getMessageIcon = (role) => {
  return role === 'user' ? 'fas fa-user' : 'fas fa-robot';
};

// 格式化时间
const formatTime = (dateString) => {
  if (!dateString) return '';
  const date = new Date(dateString);
  return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' });
};

// 渲染Markdown（带缓存）
const renderMarkdown = (content) => {
  if (!content) return '';
  
  // 检查缓存
  if (renderCache.has(content)) {
    return renderCache.get(content);
  }
  
  // 渲染并缓存
  const result = md.render(content);
  
  // 限制缓存大小
  if (renderCache.size >= MAX_CACHE_SIZE) {
    // 删除最早的缓存项
    const firstKey = renderCache.keys().next().value;
    renderCache.delete(firstKey);
  }
  
  renderCache.set(content, result);
  return result;
};

// 估算消息高度
const getMessageHeight = (item) => {
  // 基础高度
  let height = 80;
  
  // 根据内容长度增加高度
  if (item.content) {
    const lines = item.content.split('\n').length;
    height += Math.min(lines * 20, 400); // 最多增加400px
  }
  
  // 如果有文件，增加额外高度
  if (item.files && item.files.length > 0) {
    height += item.files.length * 30;
  }
  
  return height;
};

// 处理滚动事件
const handleScroll = (event) => {
  emit('scroll', event);
};

// 组件卸载时清理
onUnmounted(() => {
  // 如果使用Worker，在这里销毁
  // markdownWorkerManager.destroy();
});

defineExpose({
  scrollToBottom: () => {
    // 实现将在父组件中处理
  }
});
</script>

<style scoped>
.virtual-message-list {
  flex: 1;
  overflow-y: auto;
  position: relative;
  padding: 16px;
}

.message-container {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.message-item {
  display: flex;
  gap: 12px;
  align-items: flex-start;
}

.message-user {
  align-self: flex-end;
  flex-direction: row-reverse;
}

.message-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: #e3e5e8;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #666;
  flex-shrink: 0;
}

.message-user .message-avatar {
  background: #409eff;
  color: white;
}

.message-content {
  display: flex;
  flex-direction: column;
  max-width: 80%;
}

.message-user .message-content {
  align-items: flex-end;
}

.message-text {
  padding: 10px 14px;
  border-radius: 18px;
  line-height: 1.6;
  font-size: 14px;
  word-wrap: break-word;
  white-space: pre-wrap;
}

.message-user .message-text {
  background: #409eff;
  color: white;
}

.message-assistant .message-text {
  background: white;
  color: #333;
  box-shadow: 0 1px 3px rgba(0,0,0,0.1);
}

/* Markdown样式 */
.markdown-body {
  max-width: 100%;
  overflow-x: auto;
}

.markdown-body :deep(h1),
.markdown-body :deep(h2),
.markdown-body :deep(h3),
.markdown-body :deep(h4),
.markdown-body :deep(h5),
.markdown-body :deep(h6) {
  margin-top: 16px;
  margin-bottom: 8px;
  font-weight: 600;
}

.markdown-body :deep(p) {
  margin: 8px 0;
}

.markdown-body :deep(code) {
  background: #f0f0f0;
  padding: 2px 6px;
  border-radius: 3px;
  font-family: 'Consolas', 'Monaco', monospace;
  font-size: 13px;
}

.markdown-body :deep(pre) {
  background: #282c34;
  padding: 12px;
  border-radius: 6px;
  overflow-x: auto;
  margin: 12px 0;
}

.markdown-body :deep(pre code) {
  background: none;
  padding: 0;
  color: #abb2bf;
}

.markdown-body :deep(ul),
.markdown-body :deep(ol) {
  padding-left: 24px;
  margin: 8px 0;
}

.markdown-body :deep(li) {
  margin: 4px 0;
}

.markdown-body :deep(blockquote) {
  border-left: 4px solid #409eff;
  padding-left: 12px;
  margin: 12px 0;
  color: #666;
}

.markdown-body :deep(table) {
  border-collapse: collapse;
  width: 100%;
  margin: 12px 0;
}

.markdown-body :deep(th),
.markdown-body :deep(td) {
  border: 1px solid #ddd;
  padding: 8px 12px;
  text-align: left;
}

.markdown-body :deep(th) {
  background: #f5f7fa;
  font-weight: 600;
}

.markdown-body :deep(a) {
  color: #409eff;
  text-decoration: none;
}

.markdown-body :deep(a:hover) {
  text-decoration: underline;
}

/* KaTeX公式样式 */
.katex-block {
  margin: 12px 0;
  overflow-x: auto;
}

.message-files {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 6px;
}

.attached-file {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 4px 8px;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 4px;
  font-size: 12px;
}

.attached-file i {
  font-size: 12px;
}

.message-time {
  font-size: 11px;
  color: #999;
  margin-top: 4px;
  text-align: right;
}

.loading-more, .no-more-messages {
  text-align: center;
  padding: 12px;
  color: #999;
  font-size: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  width: 100%;
}

.loading-more {
  position: sticky;
  top: 0;
  background: rgba(255, 255, 255, 0.95);
  z-index: 10;
  margin: -16px -16px 16px -16px; /* Compensate for parent padding if needed or just layout naturally */
  border-bottom: 1px solid #eee;
}

.loading-more i {
  font-size: 16px;
}
</style>
