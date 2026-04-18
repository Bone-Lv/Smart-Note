<template>
  <div class="chat-message-list" ref="listContainer">
    <RecycleScroller
      ref="scroller"
      class="scroller"
      :items="messages"
      :item-size="getMessageHeight"
      :buffer="200"
      key-field="id"
      v-slot="{ item }"
      @scroll="handleScroll"
    >
      <div 
        class="message-item"
        :class="{ 'message-own': isOwnMessage(item) }"
      >
        <!-- 对方头像 -->
        <div v-if="!isOwnMessage(item) && showAvatar" class="message-sender">
          <img 
            :src="getSenderAvatar(item)" 
            alt="sender avatar" 
            class="sender-avatar"
            loading="lazy"
            decoding="async"
          >
        </div>
        
        <div class="message-content">
          <div 
            class="message-bubble" 
            :class="{ 'bubble-own': isOwnMessage(item) }"
          >
            <!-- 文本消息 -->
            <div v-if="item.messageType === 1 || !item.messageType" class="text-message markdown-body">
              <div v-if="isMarkdownEnabled" v-html="renderMarkdown(item.content)"></div>
              <div v-else>{{ item.content }}</div>
            </div>
            
            <!-- 图片消息（添加懒加载） -->
            <div v-else-if="item.messageType === 2" class="image-message">
              <img 
                :src="item.imageUrl || item.content" 
                alt="image" 
                class="message-image"
                loading="lazy"
                decoding="async"
                @click="$emit('preview-image', item.imageUrl || item.content)"
              >
            </div>
            
            <!-- 文件消息 -->
            <div v-else class="file-message">
              <i class="fas fa-file"></i>
              <span>{{ item.fileName || '文件' }}</span>
              <a :href="item.fileUrl" download class="download-link">
                <i class="fas fa-download"></i>
              </a>
            </div>
          </div>
          <div class="message-time">{{ formatTime(item.createTime) }}</div>
        </div>
        
        <!-- 自己的头像（可选） -->
        <div v-if="isOwnMessage(item) && showOwnAvatar" class="message-sender">
          <img 
            :src="currentUserAvatar" 
            alt="my avatar" 
            class="sender-avatar"
            loading="lazy"
            decoding="async"
          >
        </div>
      </div>
    </RecycleScroller>
    
    <!-- 加载更多提示 -->
    <div v-if="loadingMore" class="loading-more">
      <i class="fas fa-spinner fa-spin"></i>
      <span>加载中...</span>
    </div>
    <div v-else-if="!hasMore && messages.length > 0" class="no-more-messages">
      没有更多消息了
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
  },
  currentUserId: {
    type: [String, Number],
    required: true
  },
  currentUserAvatar: {
    type: String,
    default: '/default-avatar.png'
  },
  showAvatar: {
    type: Boolean,
    default: true
  },
  showOwnAvatar: {
    type: Boolean,
    default: false
  },
  enableMarkdown: {
    type: Boolean,
    default: false
  }
});

const emit = defineEmits(['scroll', 'preview-image']);

// Markdown渲染缓存
const renderCache = new Map();
const MAX_CACHE_SIZE = 100;

// 判断是否是自己发送的消息
const isOwnMessage = (message) => {
  return message.senderId == props.currentUserId;
};

// 获取发送者头像
const getSenderAvatar = (message) => {
  return message.senderAvatar || '/default-avatar.png';
};

// 格式化时间
const formatTime = (dateString) => {
  if (!dateString) return '';
  const date = new Date(dateString);
  return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' });
};

// 渲染Markdown（带缓存）
const isMarkdownEnabled = computed(() => props.enableMarkdown);
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
  scroller
});
</script>

<style scoped>
.chat-message-list {
  flex: 1;
  overflow: hidden;
  position: relative;
}

.scroller {
  height: 100%;
  padding: 16px;
}

.message-item {
  display: flex;
  gap: 12px;
  align-items: flex-start;
  padding: 8px 0;
}

.message-own {
  flex-direction: row-reverse;
}

.message-sender {
  flex-shrink: 0;
}

.sender-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  object-fit: cover;
}

.message-content {
  display: flex;
  flex-direction: column;
  max-width: 70%;
}

.message-own .message-content {
  align-items: flex-end;
}

.message-bubble {
  padding: 10px 14px;
  border-radius: 18px;
  line-height: 1.5;
  font-size: 14px;
  word-wrap: break-word;
  background: white;
  box-shadow: 0 1px 3px rgba(0,0,0,0.1);
}

.bubble-own {
  background: #409eff;
  color: white;
}

.text-message {
  white-space: pre-wrap;
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
  background: rgba(0, 0, 0, 0.1);
  padding: 2px 6px;
  border-radius: 3px;
  font-family: 'Consolas', 'Monaco', monospace;
  font-size: 13px;
}

.bubble-own .markdown-body :deep(code) {
  background: rgba(255, 255, 255, 0.2);
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

.bubble-own .markdown-body :deep(blockquote) {
  border-left-color: white;
  color: rgba(255, 255, 255, 0.9);
}

.image-message {
  max-width: 300px;
}

.message-image {
  width: 100%;
  border-radius: 8px;
  cursor: pointer;
  transition: transform 0.2s;
}

.message-image:hover {
  transform: scale(1.02);
}

.file-message {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  background: rgba(0, 0, 0, 0.05);
  border-radius: 6px;
}

.bubble-own .file-message {
  background: rgba(255, 255, 255, 0.2);
}

.download-link {
  color: inherit;
  text-decoration: none;
}

.download-link:hover {
  opacity: 0.8;
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
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  background: rgba(255, 255, 255, 0.95);
  z-index: 10;
}

.loading-more i {
  font-size: 16px;
}
</style>
