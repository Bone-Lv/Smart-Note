<template>
  <div class="global-ai-chat" :class="{ minimized: isMinimized }">
    <div class="chat-header" @mousedown="startDrag" @dblclick="toggleMinimize">
      <div class="header-left">
        <div class="ai-avatar">🤖</div>
        <span class="ai-name">AI助手</span>
      </div>
      <div class="header-right">
        <button class="header-btn" @click="toggleMinimize">
          {{ isMinimized ? '+' : '−' }}
        </button>
        <button class="header-btn close-btn" @click="$emit('close')">×</button>
      </div>
    </div>
    
    <div v-show="!isMinimized" class="chat-body">
      <div class="messages-container" ref="messagesContainer">
        <div 
          v-for="message in currentMessages" 
          :key="message.id"
          :class="['message', message.role]"
        >
          <div class="message-avatar">
            {{ message.role === 'user' ? '👤' : '🤖' }}
          </div>
          <div class="message-content">
            <div class="message-text" v-html="formatMessage(message.content)"></div>
            <div class="message-time">{{ formatDate(message.createTime) }}</div>
          </div>
        </div>
        
        <div v-if="isLoading" class="message assistant">
          <div class="message-avatar">🤖</div>
          <div class="message-content">
            <div class="typing-indicator">
              <span></span>
              <span></span>
              <span></span>
            </div>
          </div>
        </div>
      </div>
      
      <div class="input-container">
        <textarea 
          v-model="inputMessage" 
          @keydown.enter.exact.prevent="sendMessage"
          @keydown.enter.shift.exact="insertNewline"
          placeholder="输入消息..."
          class="message-input"
          :disabled="isLoading"
        ></textarea>
        <button 
          class="send-btn" 
          @click="sendMessage" 
          :disabled="isLoading || !inputMessage.trim()"
        >
          发送
        </button>
      </div>
    </div>
  </div>
</template>

<script>
import { useAiStore } from '../../stores/aiStore.js';
import { aiApi } from '../../api/ai.js';

export default {
  name: 'GlobalChat',
  emits: ['close'],
  setup() {
    const aiStore = useAiStore();
    return { aiStore };
  },
  data() {
    return {
      inputMessage: '',
      isLoading: false,
      isMinimized: false,
      dragState: {
        isDragging: false,
        startX: 0,
        startY: 0,
        startLeft: 0,
        startTop: 0
      }
    };
  },
  computed: {
    currentMessages() {
      return this.aiStore.currentMessages || [];
    }
  },
  mounted() {
    // 初始化当前会话
    if (!this.aiStore.currentConversationId) {
      this.aiStore.setCurrentConversation('global-' + Date.now());
    }
    
    // 自动滚动到底部
    this.$nextTick(() => {
      this.scrollToBottom();
    });
  },
  updated() {
    this.$nextTick(() => {
      this.scrollToBottom();
    });
  },
  methods: {
    async sendMessage() {
      if (!this.inputMessage.trim() || this.isLoading) {
        return;
      }

      const message = this.inputMessage.trim();
      this.inputMessage = '';
      
      this.isLoading = true;
      try {
        const response = await aiApi.sendMessage({
          content: message,
          conversationId: this.aiStore.currentConversationId
        });

        // 添加用户消息
        const userMessage = {
          id: Date.now(),
          role: 'user',
          content: message,
          createTime: new Date().toISOString()
        };

        // 添加AI消息
        let aiContent = '';
        for (let i = 0; i < response.data.length; i++) {
          aiContent += response.data[i];
        }

        const aiMessage = {
          id: Date.now() + 1,
          role: 'assistant',
          content: aiContent,
          createTime: new Date().toISOString()
        };

        // 更新状态
        if (!this.aiStore.messages[this.aiStore.currentConversationId]) {
          this.aiStore.messages[this.aiStore.currentConversationId] = [];
        }
        
        this.aiStore.messages[this.aiStore.currentConversationId].push(userMessage, aiMessage);
      } catch (error) {
        console.error('发送消息失败:', error);
        const errorMessage = {
          id: Date.now(),
          role: 'assistant',
          content: '抱歉，发生错误：' + error.message,
          createTime: new Date().toISOString()
        };
        
        if (!this.aiStore.messages[this.aiStore.currentConversationId]) {
          this.aiStore.messages[this.aiStore.currentConversationId] = [];
        }
        this.aiStore.messages[this.aiStore.currentConversationId].push(errorMessage);
      } finally {
        this.isLoading = false;
      }
    },

    insertNewline() {
      this.inputMessage += '\n';
    },

    scrollToBottom() {
      const container = this.$refs.messagesContainer;
      if (container) {
        container.scrollTop = container.scrollHeight;
      }
    },

    toggleMinimize() {
      this.isMinimized = !this.isMinimized;
    },

    startDrag(e) {
      if (e.target.classList.contains('header-btn')) {
        return; // 不在按钮上拖拽
      }
      
      this.dragState.isDragging = true;
      this.dragState.startX = e.clientX;
      this.dragState.startY = e.clientY;
      
      const rect = this.$el.getBoundingClientRect();
      this.dragState.startLeft = rect.left;
      this.dragState.startTop = rect.top;
      
      document.addEventListener('mousemove', this.doDrag);
      document.addEventListener('mouseup', this.stopDrag);
    },

    doDrag(e) {
      if (!this.dragState.isDragging) return;
      
      const deltaX = e.clientX - this.dragState.startX;
      const deltaY = e.clientY - this.dragState.startY;
      
      const newLeft = this.dragState.startLeft + deltaX;
      const newTop = this.dragState.startTop + deltaY;
      
      this.$el.style.left = newLeft + 'px';
      this.$el.style.top = newTop + 'px';
    },

    stopDrag() {
      this.dragState.isDragging = false;
      document.removeEventListener('mousemove', this.doDrag);
      document.removeEventListener('mouseup', this.stopDrag);
    },

    formatDate(dateString) {
      if (!dateString) return '';
      const date = new Date(dateString);
      return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
    },

    formatMessage(content) {
      // 简单的文本格式化，将换行符转换为<br>
      return content.replace(/\n/g, '<br>');
    }
  }
};
</script>

<style scoped>
.global-ai-chat {
  position: fixed;
  width: 350px;
  height: 500px;
  background: white;
  border-radius: 12px;
  box-shadow: 0 8px 32px rgba(0,0,0,0.2);
  display: flex;
  flex-direction: column;
  z-index: 999;
  overflow: hidden;
  border: 1px solid #e1e5e9;
}

.global-ai-chat.minimized {
  height: 40px;
}

.chat-header {
  background: #0969da;
  color: white;
  padding: 10px 15px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  cursor: move;
  user-select: none;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.ai-avatar {
  font-size: 18px;
}

.ai-name {
  font-weight: 600;
}

.header-right {
  display: flex;
  gap: 5px;
}

.header-btn {
  background: rgba(255,255,255,0.2);
  border: none;
  color: white;
  width: 25px;
  height: 25px;
  border-radius: 50%;
  cursor: pointer;
  font-size: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.header-btn:hover {
  background: rgba(255,255,255,0.3);
}

.close-btn:hover {
  background: rgba(255,255,255,0.3);
}

.chat-body {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.messages-container {
  flex: 1;
  overflow-y: auto;
  padding: 15px;
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.message {
  display: flex;
  gap: 10px;
  align-self: flex-start;
}

.message.user {
  align-self: flex-end;
}

.message-avatar {
  width: 30px;
  height: 30px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  font-size: 16px;
}

.message.user .message-avatar {
  background: #0969da;
  color: white;
}

.message.assistant .message-avatar {
  background: #f6f8fa;
  color: #24292f;
}

.message-content {
  max-width: 250px;
}

.message-text {
  background: #f6f8fa;
  padding: 10px 12px;
  border-radius: 18px;
  font-size: 14px;
  line-height: 1.4;
}

.message.user .message-text {
  background: #0969da;
  color: white;
}

.message-time {
  font-size: 10px;
  color: #656d76;
  margin-top: 4px;
  text-align: right;
}

.typing-indicator {
  display: flex;
  gap: 3px;
}

.typing-indicator span {
  width: 8px;
  height: 8px;
  background: #0969da;
  border-radius: 50%;
  animation: typing 1.4s infinite ease-in-out;
}

.typing-indicator span:nth-child(1) { animation-delay: -0.32s; }
.typing-indicator span:nth-child(2) { animation-delay: -0.16s; }

@keyframes typing {
  0%, 80%, 100% { transform: scale(0.8); opacity: 0.5; }
  40% { transform: scale(1); opacity: 1; }
}

.input-container {
  padding: 10px 15px;
  border-top: 1px solid #e1e5e9;
  display: flex;
  gap: 8px;
}

.message-input {
  flex: 1;
  padding: 10px 12px;
  border: 1px solid #d0d7de;
  border-radius: 18px;
  resize: none;
  height: 40px;
  font-size: 14px;
  max-height: 100px;
}

.message-input:focus {
  outline: none;
  border-color: #0969da;
}

.send-btn {
  background: #0969da;
  color: white;
  border: none;
  border-radius: 18px;
  padding: 0 15px;
  cursor: pointer;
  font-size: 14px;
}

.send-btn:hover:not(:disabled) {
  background: #085fac;
}

.send-btn:disabled {
  background: #d0d7de;
  cursor: not-allowed;
}
</style>