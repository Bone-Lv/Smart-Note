<template>
  <div 
    v-if="aiStore.isChatOpen" 
    class="global-chat"
    :style="chatWindowStyle"
  >
    <!-- 拖拽手柄（顶部） -->
    <div class="chat-header" @mousedown="startChatDrag">
      <div class="header-info">
        <i class="fas fa-robot header-icon"></i>
        <span class="header-title">AI助手</span>
      </div>
      <div class="header-actions">
        <button @click.stop="toggleChat" class="minimize-btn" title="最小化">
          <i class="fas fa-minus"></i>
        </button>
        <button @click.stop="closeChat" class="close-btn" title="关闭">
          <i class="fas fa-times"></i>
        </button>
      </div>
    </div>
    
    <!-- 缩放手柄（右下角） -->
    <div class="resize-handle" @mousedown="startResize"></div>
    
    <div class="chat-conversations" v-if="!currentConversation">
      <div class="conversations-header">
        <h3>对话历史</h3>
        <button @click="startNewConversation" class="new-conversation-btn">
          <i class="fas fa-plus"></i>
          新对话
        </button>
      </div>
      
      <div class="conversations-list">
        <div 
          v-for="conversation in aiStore.conversations" 
          :key="conversation.conversationId" 
          class="conversation-item"
          @click="selectConversation(conversation)"
        >
          <div class="conversation-preview">
            <i class="fas fa-comment"></i>
            <div class="conversation-text">
              <p class="conversation-title">{{ conversation.lastMessage || '新对话' }}</p>
              <p class="conversation-time">{{ formatDate(conversation.lastMessageTime) }}</p>
            </div>
          </div>
          <button 
            @click.stop="deleteConversationItem(conversation.conversationId)" 
            class="delete-conversation-btn"
            title="删除会话"
          >
            <i class="fas fa-trash-alt"></i>
          </button>
        </div>
        
        <div v-if="aiStore.conversations.length === 0" class="no-conversations">
          <i class="fas fa-comment-slash"></i>
          <p>暂无对话历史</p>
        </div>
      </div>
    </div>
    
    <div class="chat-messages" v-else>
      <div class="messages-header">
        <button @click="backToConversations" class="back-btn">
          <i class="fas fa-arrow-left"></i>
        </button>
        <span class="conversation-title">AI助手</span>
        <button @click="clearCurrentConversation" class="clear-btn">
          <i class="fas fa-trash"></i>
        </button>
      </div>
      
      <!-- 使用虚拟滚动消息列表 -->
      <VirtualMessageList
        :messages="aiStore.messages"
        :loading-more="loadingMore"
        :has-more="aiStore.hasMore"
        @scroll="handleScroll"
        ref="messageListRef"
      />
      
      <div class="message-input-section">
        <!-- 文件预览区域 -->
        <div v-if="selectedFiles.length > 0" class="file-preview-area">
          <div 
            v-for="(file, index) in selectedFiles" 
            :key="index" 
            class="file-preview-item"
          >
            <i class="fas fa-file-pdf" v-if="file.type === 'application/pdf'"></i>
            <i class="fas fa-image" v-else-if="file.type.startsWith('image/')"></i>
            <i class="fas fa-file" v-else></i>
            <span class="file-name">{{ file.name }}</span>
            <button @click="removeFile(index)" class="remove-file-btn">
              <i class="fas fa-times"></i>
            </button>
          </div>
        </div>
        
        <div class="input-wrapper">
          <textarea 
            v-model="messageInput" 
            class="message-input" 
            placeholder="输入消息...（支持Shift+Enter换行）"
            @keydown.enter.exact.prevent="sendMessage"
            @keydown.shift.enter="insertNewline"
            :disabled="aiStore.isLoading"
            rows="1"
          ></textarea>
          
          <div class="input-actions">
            <!-- 文件上传按钮 -->
            <label class="upload-file-btn" title="上传PDF或图片">
              <i class="fas fa-paperclip"></i>
              <input 
                type="file" 
                accept=".pdf,image/*" 
                multiple
                @change="handleFileSelect"
                style="display: none"
              >
            </label>
            
            <!-- 发送按钮 -->
            <button 
              @click="sendMessage" 
              class="send-btn" 
              :disabled="(!messageInput.trim() && selectedFiles.length === 0) || aiStore.isLoading"
            >
              <i class="fas fa-paper-plane"></i>
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
  
  <button 
    v-else 
    @click="toggleChat"
    @mousedown="startDrag"
    class="chat-toggle-btn"
    :style="buttonPosition"
  >
    <i class="fas fa-robot"></i>
    <span>AI助手</span>
  </button>
</template>

<script>
import { ref, nextTick, onMounted, onUnmounted, watch, computed } from 'vue';
import { useAiStore } from '../../stores/aiStore.js';
import { ElMessage } from 'element-plus';
import VirtualMessageList from '../../components/VirtualMessageList.vue';
import MarkdownIt from 'markdown-it';

export default {
  name: 'GlobalChat',
  components: {
    VirtualMessageList
  },
  setup() {
    const aiStore = useAiStore();
    
    const messageInput = ref('');
    const messagesContainer = ref(null);
    const selectedFiles = ref([]);
    const fileInput = ref(null);
    const loadingMore = ref(false);
    const messageListRef = ref(null);
    
    // 浮窗按钮拖拽相关状态
    const isDragging = ref(false);
    const hasMoved = ref(false); // 标记是否发生了移动
    const buttonRight = ref(30); // 初始 right 值
    const buttonBottom = ref(30); // 初始 bottom 值
    const startX = ref(0);
    const startY = ref(0);
    const startRight = ref(30);
    const startBottom = ref(30);
    
    // 聊天窗口拖拽和缩放相关状态
    const chatWindowRight = ref(30); // 聊天窗口 right 值
    const chatWindowBottom = ref(90); // 聊天窗口 bottom 值
    const chatWindowWidth = ref(400); // 聊天窗口宽度
    const chatWindowHeight = ref(500); // 聊天窗口高度
    const isChatDragging = ref(false); // 聊天窗口是否正在拖拽
    const isChatResizing = ref(false); // 聊天窗口是否正在缩放
    const chatDragStartX = ref(0);
    const chatDragStartY = ref(0);
    const chatDragStartRight = ref(30);
    const chatDragStartBottom = ref(90);
    const resizeStartX = ref(0);
    const resizeStartY = ref(0);
    const resizeStartWidth = ref(400);
    const resizeStartHeight = ref(500);
    
    const md = new MarkdownIt({
      html: true,
      linkify: true,
      typographer: true
    });
    
    // 计算按钮位置
    const buttonPosition = computed(() => ({
      right: `${buttonRight.value}px`,
      bottom: `${buttonBottom.value}px`
    }));
    
    // 计算聊天窗口样式
    const chatWindowStyle = computed(() => ({
      right: `${chatWindowRight.value}px`,
      bottom: `${chatWindowBottom.value}px`,
      width: `${chatWindowWidth.value}px`,
      height: `${chatWindowHeight.value}px`
    }));

    // 开始拖拽
    const startDrag = (event) => {
      isDragging.value = true;
      startX.value = event.clientX;
      startY.value = event.clientY;
      startRight.value = buttonRight.value;
      startBottom.value = buttonBottom.value;
      
      document.addEventListener('mousemove', onDrag);
      document.addEventListener('mouseup', stopDrag);
      
      event.preventDefault(); // 防止选中文本
    };
    
    // 拖拽中
    const onDrag = (event) => {
      if (!isDragging.value) return;
      
      const deltaX = event.clientX - startX.value;
      const deltaY = event.clientY - startY.value;
      
      // 标记发生了移动（超过5像素才算拖拽）
      if (Math.abs(deltaX) > 5 || Math.abs(deltaY) > 5) {
        hasMoved.value = true;
      }
      
      // 注意：使用 right/bottom 定位时，鼠标移动方向与值的变化方向相反
      buttonRight.value = startRight.value - deltaX;
      buttonBottom.value = startBottom.value - deltaY;
      
      // 边界限制
      const windowWidth = window.innerWidth;
      const windowHeight = window.innerHeight;
      buttonRight.value = Math.max(0, Math.min(windowWidth - 150, buttonRight.value));
      buttonBottom.value = Math.max(0, Math.min(windowHeight - 50, buttonBottom.value));
    };
    
    // 停止按钮拖拽
    const stopDrag = () => {
      isDragging.value = false;
      document.removeEventListener('mousemove', onDrag);
      document.removeEventListener('mouseup', stopDrag);
    };
    
    // 开始聊天窗口拖拽
    const startChatDrag = (event) => {
      // 如果点击的是按钮，不拖拽
      if (event.target.tagName === 'BUTTON') return;
      
      isChatDragging.value = true;
      chatDragStartX.value = event.clientX;
      chatDragStartY.value = event.clientY;
      chatDragStartRight.value = chatWindowRight.value;
      chatDragStartBottom.value = chatWindowBottom.value;
      
      document.addEventListener('mousemove', onChatDrag);
      document.addEventListener('mouseup', stopChatDrag);
      
      event.preventDefault();
    };
    
    // 聊天窗口拖拽中
    const onChatDrag = (event) => {
      if (!isChatDragging.value) return;
      
      const deltaX = event.clientX - chatDragStartX.value;
      const deltaY = event.clientY - chatDragStartY.value;
      
      // 注意：使用 right/bottom 定位时，方向与鼠标相反
      chatWindowRight.value = chatDragStartRight.value - deltaX;
      chatWindowBottom.value = chatDragStartBottom.value - deltaY;
      
      // 边界限制：确保窗口不超出当前视口
      const windowWidth = window.innerWidth;
      const windowHeight = window.innerHeight;
      
      // 限制 right 值，确保窗口左边缘不会超出屏幕左侧
      chatWindowRight.value = Math.max(0, Math.min(windowWidth - chatWindowWidth.value, chatWindowRight.value));
      // 限制 bottom 值，确保窗口上边缘不会超出屏幕顶部
      chatWindowBottom.value = Math.max(0, Math.min(windowHeight - chatWindowHeight.value, chatWindowBottom.value));
    };
    
    // 停止聊天窗口拖拽
    const stopChatDrag = () => {
      isChatDragging.value = false;
      document.removeEventListener('mousemove', onChatDrag);
      document.removeEventListener('mouseup', stopChatDrag);
    };
    
    // 开始缩放
    const startResize = (event) => {
      isChatResizing.value = true;
      resizeStartX.value = event.clientX;
      resizeStartY.value = event.clientY;
      resizeStartWidth.value = chatWindowWidth.value;
      resizeStartHeight.value = chatWindowHeight.value;
      
      document.addEventListener('mousemove', onResize);
      document.addEventListener('mouseup', stopResize);
      
      event.preventDefault();
      event.stopPropagation();
    };
    
    // 缩放中
    const onResize = (event) => {
      if (!isChatResizing.value) return;
      
      const deltaX = event.clientX - resizeStartX.value;
      const deltaY = event.clientY - resizeStartY.value;
      
      // 计算新的宽度和高度
      let newWidth = Math.max(300, Math.min(800, resizeStartWidth.value + deltaX));
      let newHeight = Math.max(400, Math.min(900, resizeStartHeight.value + deltaY));
      
      // 确保缩放后的窗口不超出当前视口
      const windowWidth = window.innerWidth;
      const windowHeight = window.innerHeight;
      
      // 如果窗口右边缘超出屏幕，限制宽度
      if (chatWindowRight.value + newWidth > windowWidth) {
        newWidth = windowWidth - chatWindowRight.value;
      }
      
      // 如果窗口上边缘超出屏幕，限制高度
      if (chatWindowBottom.value + newHeight > windowHeight) {
        newHeight = windowHeight - chatWindowBottom.value;
      }
      
      // 确保最小尺寸
      newWidth = Math.max(300, newWidth);
      newHeight = Math.max(400, newHeight);
      
      chatWindowWidth.value = newWidth;
      chatWindowHeight.value = newHeight;
    };
    
    // 停止缩放
    const stopResize = () => {
      isChatResizing.value = false;
      document.removeEventListener('mousemove', onResize);
      document.removeEventListener('mouseup', stopResize);
    };
    
    // 切换聊天窗口
    const toggleChat = () => {
      // 如果发生了拖拽移动，不触发切换
      if (hasMoved.value) {
        hasMoved.value = false;
        return;
      }
      
      if (!aiStore.isChatOpen) {
        // 打开聊天窗口时，将其位置设置到按钮附近
        aiStore.isChatOpen = true;
        
        // 计算按钮位置
        const buttonElement = document.querySelector('.chat-toggle-btn');
        if (buttonElement) {
          const rect = buttonElement.getBoundingClientRect();
          const windowWidth = window.innerWidth;
          const windowHeight = window.innerHeight;
          
          // 将聊天窗口定位到按钮上方
          chatWindowRight.value = buttonRight.value;
          chatWindowBottom.value = buttonBottom.value + rect.height + 10;
          
          // 边界检查：确保窗口不超出屏幕
          if (chatWindowRight.value + chatWindowWidth.value > windowWidth) {
            chatWindowRight.value = Math.max(0, windowWidth - chatWindowWidth.value);
          }
          
          if (chatWindowBottom.value + chatWindowHeight.value > windowHeight) {
            chatWindowBottom.value = Math.max(0, windowHeight - chatWindowHeight.value);
          }
        }
      } else {
        // 关闭聊天窗口
        aiStore.isChatOpen = false;
      }
    };
    
    // 关闭聊天窗口
    const closeChat = () => {
      aiStore.isChatOpen = false;
      // 关闭时记录当前位置，下次打开时保持
      // 不重置位置，保持用户调整后的位置
    };
    
    // 开始新对话
    const startNewConversation = () => {
      aiStore.startNewConversation();
    };
    
    // 选择对话
    const selectConversation = (conversation) => {
      aiStore.setCurrentConversation(conversation);
      aiStore.loadChatHistory(conversation.conversationId);
    };
    
    // 删除会话
    const deleteConversationItem = async (conversationId) => {
      try {
        const result = await aiStore.deleteConversation(conversationId);
        if (result.success) {
          ElMessage.success('✅ 会话已删除');
        } else {
          ElMessage.error('删除失败：' + result.error);
        }
      } catch (error) {
        console.error('Delete conversation error:', error);
        ElMessage.error('删除会话失败');
      }
    };
    
    // 返回对话列表
    const backToConversations = () => {
      aiStore.setCurrentConversation(null);
    };
    
    // 发送消息
    const sendMessage = async () => {
      if ((!messageInput.value.trim() && selectedFiles.value.length === 0) || aiStore.isLoading) return;
      
      const content = messageInput.value.trim();
      const files = [...selectedFiles.value];
      
      // 清空输入
      messageInput.value = '';
      selectedFiles.value = [];
      
      try {
        let result;
        
        if (files.length > 0) {
          // 有文件，使用文件对话接口
          result = await aiStore.sendMessageWithFiles(content, files);
        } else {
          // 纯文本消息
          result = await aiStore.sendMessage(content);
        }
        
        if (!result.success) {
          ElMessage.error('发送失败：' + result.error);
        }
      } catch (error) {
        console.error('Send message error:', error);
        ElMessage.error('发送消息失败');
      }
    };
    
    // 处理文件选择
    const handleFileSelect = (event) => {
      const files = Array.from(event.target.files);
      
      if (files.length === 0) return;
      
      // 验证文件
      files.forEach(file => {
        // 检查文件类型
        if (!file.type.startsWith('image/') && file.type !== 'application/pdf') {
          ElMessage.warning(`不支持的文件类型：${file.name}`);
          return;
        }
        
        // 检查文件大小（限制10MB）
        if (file.size > 10 * 1024 * 1024) {
          ElMessage.warning(`文件过大：${file.name}（最大10MB）`);
          return;
        }
        
        selectedFiles.value.push(file);
      });
      
      // 清空input，允许重复选择同一文件
      event.target.value = '';
    };
    
    // 移除文件
    const removeFile = (index) => {
      selectedFiles.value.splice(index, 1);
    };
    
    // 清空当前对话
    const clearCurrentConversation = () => {
      if (confirm('确定要清空当前对话吗？')) {
        aiStore.messages = [];
        aiStore.startNewConversation();
      }
    };
    
    // 插入换行符
    const insertNewline = (event) => {
      event.target.value += '\n';
      event.preventDefault();
    };
    
    // 滚动到底部
    const scrollToBottom = async () => {
      await nextTick();
      // 虚拟滚动组件的滚动处理
      if (messageListRef.value && messageListRef.value.$el) {
        const scroller = messageListRef.value.$el.querySelector('.scroller');
        if (scroller) {
          scroller.scrollTop = scroller.scrollHeight;
        }
      }
    };
    
    // 处理滚动事件（加载更多）
    const handleScroll = async (event) => {
      // 虚拟滚动组件会传递滚动事件
      if (aiStore.hasMore && !loadingMore.value && aiStore.currentConversation) {
        // 检查是否滚动到顶部
        if (event && event.target && event.target.scrollTop < 50) {
          loadingMore.value = true;
          
          try {
            await aiStore.loadMoreHistory();
            
            // 等待DOM更新后恢复滚动位置
            await nextTick();
          } catch (error) {
            console.error('Load more error:', error);
          } finally {
            loadingMore.value = false;
          }
        }
      }
    };
    
    // 格式化助手消息
    const formatAssistantMessage = (content) => {
      return md.render(content);
    };
    
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
    
    // 格式化日期
    const formatDate = (dateString) => {
      if (!dateString) return '';
      return new Date(dateString).toLocaleDateString('zh-CN');
    };
    
    // 监听消息变化滚动到底部
    watch(() => aiStore.messages, () => {
      scrollToBottom();
    }, { deep: true });
    
    // 监听加载状态变化滚动到底部
    watch(() => aiStore.isLoading, () => {
      if (!aiStore.isLoading) {
        scrollToBottom();
      }
    });
    
    // 加载对话列表
    onMounted(async () => {
      if (aiStore.isChatOpen) {
        await aiStore.loadConversations();
      }
    });
    
    return {
      aiStore,
      messageInput,
      messagesContainer,
      selectedFiles,
      fileInput,
      loadingMore,
      messageListRef,
      buttonPosition,
      chatWindowStyle,
      toggleChat,
      closeChat,
      startDrag,
      startChatDrag,
      startResize,
      startNewConversation,
      selectConversation,
      deleteConversationItem,
      backToConversations,
      sendMessage,
      handleFileSelect,
      removeFile,
      handleScroll,
      clearCurrentConversation,
      insertNewline,
      formatAssistantMessage,
      getMessageIcon,
      formatTime,
      formatDate
    };
  }
};
</script>

<style scoped>
.global-chat {
  position: fixed;
  /* 位置和尺寸由 style 绑定动态控制 */
  background: white;
  border-radius: 12px;
  box-shadow: 0 8px 32px rgba(0,0,0,0.15);
  display: flex;
  flex-direction: column;
  z-index: 999;
  overflow: hidden;
  min-width: 300px;
  min-height: 400px;
}

.chat-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  background: #409eff;
  color: white;
  cursor: move; /* 显示移动光标 */
  user-select: none; /* 防止拖拽时选中文本 */
}

.header-info {
  display: flex;
  align-items: center;
  gap: 8px;
}

.header-icon {
  font-size: 18px;
}

.header-title {
  font-weight: 500;
  font-size: 16px;
}

.header-actions {
  display: flex;
  gap: 8px;
}

.minimize-btn, .close-btn {
  background: none;
  border: none;
  color: white;
  cursor: pointer;
  padding: 4px;
  border-radius: 4px;
  width: 30px;
  height: 30px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.minimize-btn:hover, .close-btn:hover {
  background: rgba(255, 255, 255, 0.2);
}

.chat-conversations {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.conversations-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  border-bottom: 1px solid #eee;
  background: #f8f9fa;
}

.conversations-header h3 {
  margin: 0;
  color: #333;
  font-size: 16px;
}

.new-conversation-btn {
  padding: 6px 12px;
  background: #409eff;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 12px;
  display: flex;
  align-items: center;
  gap: 4px;
}

.new-conversation-btn:hover {
  background: #66b1ff;
}

.conversations-list {
  flex: 1;
  overflow-y: auto;
  padding: 8px 0;
}

.conversation-item {
  padding: 12px 16px;
  cursor: pointer;
  transition: background 0.3s;
  border-bottom: 1px solid #f5f7fa;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.conversation-item:hover {
  background: #f5f7fa;
}

.conversation-item:hover .delete-conversation-btn {
  opacity: 1;
}

.delete-conversation-btn {
  background: none;
  border: none;
  color: #999;
  cursor: pointer;
  padding: 6px;
  border-radius: 4px;
  opacity: 0;
  transition: all 0.3s;
  display: flex;
  align-items: center;
  justify-content: center;
}

.delete-conversation-btn:hover {
  background: #ffebee;
  color: #f56c6c;
}

.conversation-preview {
  display: flex;
  align-items: center;
  gap: 12px;
}

.conversation-preview i {
  color: #409eff;
  font-size: 16px;
}

.conversation-text {
  flex: 1;
}

.conversation-title {
  margin: 0 0 2px 0;
  color: #333;
  font-size: 14px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.conversation-time {
  margin: 0;
  color: #999;
  font-size: 12px;
}

.no-conversations {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
  color: #999;
}

.no-conversations i {
  font-size: 48px;
  margin-bottom: 16px;
  opacity: 0.5;
}

.no-conversations p {
  margin: 0;
  font-size: 14px;
}

.chat-messages {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.messages-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid #eee;
  background: #f8f9fa;
}

.back-btn, .clear-btn {
  background: none;
  border: none;
  color: #666;
  cursor: pointer;
  padding: 6px;
  border-radius: 4px;
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.back-btn:hover, .clear-btn:hover {
  background: #f5f7fa;
  color: #409eff;
}

.conversation-title {
  color: #333;
  font-weight: 500;
  font-size: 14px;
}

.message-input-section {
  padding: 12px;
  border-top: 1px solid #eee;
  background: white;
}

.file-preview-area {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 8px;
  max-height: 100px;
  overflow-y: auto;
}

.file-preview-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 10px;
  background: #f0f9ff;
  border: 1px solid #d1e7ff;
  border-radius: 6px;
  font-size: 12px;
}

.file-preview-item i {
  color: #409eff;
  font-size: 14px;
}

.file-name {
  max-width: 150px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: #333;
}

.remove-file-btn {
  background: none;
  border: none;
  color: #999;
  cursor: pointer;
  padding: 2px;
  border-radius: 3px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
}

.remove-file-btn:hover {
  background: #ffebee;
  color: #f56c6c;
}

.input-wrapper {
  display: flex;
  gap: 8px;
  align-items: flex-end;
}

.message-input {
  flex: 1;
  padding: 10px 12px;
  border: 1px solid #ddd;
  border-radius: 18px;
  resize: none;
  font-size: 14px;
  min-height: 36px;
  max-height: 120px;
  outline: none;
  font-family: inherit;
  line-height: 1.5;
}

.message-input:focus {
  border-color: #409eff;
  box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.1);
}

.message-input:disabled {
  background: #f5f7fa;
  cursor: not-allowed;
}

.input-actions {
  display: flex;
  gap: 8px;
  align-items: center;
}

.upload-file-btn {
  width: 36px;
  height: 36px;
  border: 1px solid #ddd;
  border-radius: 50%;
  background: white;
  color: #666;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  transition: all 0.3s;
}

.upload-file-btn:hover {
  border-color: #409eff;
  color: #409eff;
  background: #f0f9ff;
}

.send-btn {
  width: 36px;
  height: 36px;
  border: none;
  border-radius: 50%;
  background: #409eff;
  color: white;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  transition: all 0.3s;
}

.send-btn:hover:not(:disabled) {
  background: #66b1ff;
  transform: scale(1.05);
}

.send-btn:disabled {
  background: #ccc;
  cursor: not-allowed;
}

.chat-toggle-btn {
  position: fixed;
  /* bottom 和 right 由 style 绑定动态控制 */
  background: #409eff;
  color: white;
  border: none;
  border-radius: 50px;
  padding: 12px 20px;
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: move; /* 显示可拖拽光标 */
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.4);
  z-index: 999;
  transition: transform 0.3s; /* 移除 background 和 box-shadow 的 transition，避免拖拽延迟感 */
  user-select: none; /* 防止拖拽时选中文本 */
}

.chat-toggle-btn:hover {
  background: #66b1ff;
  transform: translateY(-2px);
}

.chat-toggle-btn i {
  font-size: 18px;
}

.chat-toggle-btn span {
  font-size: 14px;
  font-weight: 500;
}

.resize-handle {
  position: absolute;
  right: 0;
  bottom: 0;
  width: 15px;
  height: 15px;
  cursor: nwse-resize;
  z-index: 1000;
}

.resize-handle::after {
  content: '';
  position: absolute;
  right: 3px;
  bottom: 3px;
  width: 8px;
  height: 8px;
  border-right: 2px solid #ccc;
  border-bottom: 2px solid #ccc;
}
</style>