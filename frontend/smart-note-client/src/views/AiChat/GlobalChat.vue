<!-- src/views/AiChat/GlobalChat.vue -->
<template>
  <div class="global-chat">
    <div class="chat-header">
      <h3>AI助手</h3>
      <div class="header-actions">
        <button @click="newConversation" title="新建对话" class="icon-btn">+</button>
        <button @click="toggleMinimize" title="最小化" class="icon-btn">{{ isMinimized ? '+' : '−' }}</button>
        <button @click="$emit('close')" title="关闭" class="icon-btn">×</button>
      </div>
    </div>
    
    <div v-if="!isMinimized" class="chat-body">
      <div v-if="conversations.length > 0" class="conversation-selector">
        <div 
          v-for="conv in conversations" 
          :key="conv.conversationId"
          class="conversation-item"
          :class="{ active: conv.conversationId === currentConversationId }"
          @click="selectConversation(conv.conversationId)"
        >
          <div class="conv-title">{{ conv.lastMessage?.substring(0, 20) || '新对话' }}...</div>
          <div class="conv-time">{{ formatDate(conv.lastMessageTime) }}</div>
          <button @click.stop="deleteConversation(conv.conversationId)" class="delete-btn">×</button>
        </div>
      </div>
      
      <div class="chat-messages" ref="messagesContainer">
        <div 
          v-for="msg in currentMessages" 
          :key="msg.id"
          class="message-item"
          :class="{ 'user-message': msg.role === 'user', 'assistant-message': msg.role === 'assistant' }"
        >
          <div class="message-avatar">
            {{ msg.role === 'user' ? '👤' : '🤖' }}
          </div>
          <div class="message-content">
            <div class="message-text" v-html="formatMessage(msg.content)"></div>
            <div class="message-time">{{ formatTime(msg.createTime) }}</div>
          </div>
        </div>
        
        <div v-if="isReceiving" class="typing-indicator">
          <span></span>
          <span></span>
          <span></span>
        </div>
      </div>
      
      <div class="chat-input-area">
        <textarea 
          v-model="inputMessage" 
          placeholder="输入消息... (Enter发送, Shift+Enter换行)"
          @keydown="handleKeyDown"
          ref="inputRef"
        ></textarea>
        <button @click="sendMessage" :disabled="!inputMessage.trim() || isReceiving" class="send-btn">
          {{ isReceiving ? '发送中...' : '发送' }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, nextTick, onMounted } from 'vue'
import { aiStore } from '@/stores/aiStore'
import { getConversations, getChatHistory, sendMessage as sendApiMessage, deleteConversation as deleteApiConversation } from '@/api/ai'
import { ChatMessageVO } from '@/types/api'

const emit = defineEmits(['close'])

const ai = aiStore()
const inputMessage = ref('')
const isReceiving = ref(false)
const isMinimized = ref(false)
const inputRef = ref<HTMLTextAreaElement>()

// 计算属性
const conversations = computed(() => ai.conversations)
const currentConversationId = computed(() => ai.currentConversationId)
const currentMessages = computed(() => {
  if (!currentConversationId.value) return []
  return ai.messages[currentConversationId.value] || []
})

// 格式化消息（支持简单的Markdown）
const formatMessage = (content: string) => {
  // 简单的Markdown格式化
  let formatted = content
    .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>') // 粗体
    .replace(/\*(.*?)\*/g, '<em>$1</em>') // 斜体
    .replace(/`(.*?)`/g, '<code>$1</code>') // 行内代码
    .replace(/\n/g, '<br>') // 换行
  
  return formatted
}

// 格式化时间
const formatTime = (timeStr: string) => {
  return new Date(timeStr).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
}

// 格式化日期
const formatDate = (dateStr?: string) => {
  if (!dateStr) return ''
  return new Date(dateStr).toLocaleDateString()
}

// 发送消息
const sendMessage = async () => {
  if (!inputMessage.value.trim() || isReceiving.value) return
  
  const messageContent = inputMessage.value
  inputMessage.value = ''
  
  try {
    isReceiving.value = true
    
    // 添加用户消息到本地
    const userMsg: ChatMessageVO = {
      id: Date.now(),
      role: 'user',
      content: messageContent,
      createTime: new Date().toISOString()
    }
    
    ai.addMessage(currentConversationId.value || 'temp', userMsg)
    
    // 发送请求到API
    const response = await sendApiMessage({
      content: messageContent,
      conversationId: currentConversationId.value || undefined
    })
    
    if (response.code === 200 && Array.isArray(response.data)) {
      // 流式处理AI回复
      let fullResponse = ''
      for (const chunk of response.data) {
        fullResponse += chunk
        
        // 更新最后一条消息的内容
        if (ai.messages[currentConversationId.value!] && ai.messages[currentConversationId.value!].length > 0) {
          const lastMsg = ai.messages[currentConversationId.value!][ai.messages[currentConversationId.value!].length - 1]
          if (lastMsg.role === 'assistant') {
            lastMsg.content = fullResponse
          } else {
            // 如果最后一条不是AI消息，添加新的AI消息
            const aiMsg: ChatMessageVO = {
              id: Date.now(),
              role: 'assistant',
              content: fullResponse,
              createTime: new Date().toISOString()
            }
            ai.addMessage(currentConversationId.value!, aiMsg)
          }
        } else {
          // 添加AI消息
          const aiMsg: ChatMessageVO = {
            id: Date.now(),
            role: 'assistant',
            content: fullResponse,
            createTime: new Date().toISOString()
          }
          ai.addMessage(currentConversationId.value!, aiMsg)
        }
        
        // 滚动到底部
        await nextTick()
        scrollToBottom()
        
        // 模拟流式效果
        await new Promise(resolve => setTimeout(resolve, 50))
      }
    }
  } catch (error) {
    console.error('发送消息失败:', error)
    // 添加错误消息
    const errorMsg: ChatMessageVO = {
      id: Date.now(),
      role: 'assistant',
      content: '抱歉，AI助手暂时无法响应，请稍后再试。',
      createTime: new Date().toISOString()
    }
    ai.addMessage(currentConversationId.value || 'temp', errorMsg)
  } finally {
    isReceiving.value = false
  }
}

// 新建对话
const newConversation = () => {
  ai.setCurrentConversation(null)
  // 清空输入框
  inputMessage.value = ''
}

// 选择对话
const selectConversation = (id: string) => {
  ai.setCurrentConversation(id)
  // 清空输入框
  inputMessage.value = ''
}

// 删除对话
const deleteConversation = async (id: string) => {
  if (confirm('确定要删除这个对话吗？')) {
    try {
      await deleteApiConversation(id)
      ai.removeConversation(id)
      if (currentConversationId.value === id) {
        ai.setCurrentConversation(conversations.value[0]?.conversationId || null)
      }
    } catch (error) {
      console.error('删除对话失败:', error)
      alert('删除对话失败')
    }
  }
}

// 切换最小化
const toggleMinimize = () => {
  isMinimized.value = !isMinimized.value
}

// 滚动到底部
const scrollToBottom = () => {
  const container = document.querySelector('.chat-messages')
  if (container) {
    container.scrollTop = container.scrollHeight
  }
}

// 处理按键事件
const handleKeyDown = (e: KeyboardEvent) => {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
    sendMessage()
  }
}

// 加载会话列表
const loadConversations = async () => {
  try {
    const response = await getConversations()
    if (response.code === 200) {
      response.data.forEach(conv => {
        ai.addConversation(conv)
      })
      
      // 设置当前会话
      if (response.data.length > 0 && !currentConversationId.value) {
        ai.setCurrentConversation(response.data[0].conversationId)
      }
    }
  } catch (error) {
    console.error('加载会话列表失败:', error)
  }
}

onMounted(async () => {
  await loadConversations()
  
  // 自动聚焦输入框
  if (inputRef.value) {
    inputRef.value.focus()
  }
  
  // 滚动到底部
  await nextTick()
  scrollToBottom()
})
</script>

<style scoped>
.global-chat {
  position: fixed;
  bottom: 100px;
  right: 30px;
  width: 400px;
  height: 500px;
  background-color: white;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.15);
  display: flex;
  flex-direction: column;
  z-index: 1000;
  border: 1px solid #e9ecef;
}

.chat-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 15px;
  background-color: #f8f9fa;
  border-bottom: 1px solid #e9ecef;
  border-radius: 8px 8px 0 0;
}

.chat-header h3 {
  margin: 0;
  font-size: 1rem;
}

.header-actions {
  display: flex;
  gap: 5px;
}

.icon-btn {
  background: none;
  border: 1px solid #ddd;
  width: 28px;
  height: 28px;
  border-radius: 4px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
}

.icon-btn:hover {
  background-color: #e9ecef;
}

.chat-body {
  display: flex;
  flex: 1;
  overflow: hidden;
}

.conversation-selector {
  width: 120px;
  border-right: 1px solid #e9ecef;
  overflow-y: auto;
  background-color: #f8f9fa;
}

.conversation-item {
  padding: 10px;
  border-bottom: 1px solid #e9ecef;
  cursor: pointer;
  position: relative;
}

.conversation-item:hover {
  background-color: #e9ecef;
}

.conversation-item.active {
  background-color: #007bff;
  color: white;
}

.conv-title {
  font-size: 0.85rem;
  margin-bottom: 4px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.conv-time {
  font-size: 0.7rem;
  color: #666;
}

.delete-btn {
  position: absolute;
  right: 5px;
  top: 5px;
  background: none;
  border: none;
  font-size: 16px;
  cursor: pointer;
  opacity: 0;
}

.conversation-item:hover .delete-btn {
  opacity: 1;
}

.chat-messages {
  flex: 1;
  padding: 15px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
}

.message-item {
  display: flex;
  margin-bottom: 15px;
  max-width: 90%;
}

.user-message {
  align-self: flex-end;
}

.assistant-message {
  align-self: flex-start;
}

.message-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 10px;
  flex-shrink: 0;
}

.user-message .message-avatar {
  background-color: #007bff;
  color: white;
}

.assistant-message .message-avatar {
  background-color: #6f42c1;
  color: white;
}

.message-content {
  display: flex;
  flex-direction: column;
}

.message-text {
  background-color: #f1f3f4;
  padding: 10px 12px;
  border-radius: 18px;
  line-height: 1.4;
  max-width: 100%;
  word-wrap: break-word;
}

.user-message .message-text {
  background-color: #d0ebff;
}

.message-time {
  font-size: 0.7rem;
  color: #999;
  text-align: right;
  margin-top: 4px;
}

.typing-indicator {
  display: flex;
  align-items: center;
  padding: 10px;
  align-self: flex-start;
}

.typing-indicator span {
  height: 8px;
  width: 8px;
  background-color: #999;
  border-radius: 50%;
  display: inline-block;
  margin: 0 2px;
  animation: typing 1s infinite;
}

.typing-indicator span:nth-child(2) {
  animation-delay: 0.2s;
}

.typing-indicator span:nth-child(3) {
  animation-delay: 0.4s;
}

@keyframes typing {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-5px); }
}

.chat-input-area {
  padding: 10px;
  border-top: 1px solid #e9ecef;
  display: flex;
}

.chat-input-area textarea {
  flex: 1;
  padding: 8px 12px;
  border: 1px solid #ddd;
  border-radius: 4px;
  resize: none;
  height: 60px;
  font-family: inherit;
}

.send-btn {
  margin-left: 10px;
  padding: 8px 16px;
  background-color: #007bff;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}

.send-btn:disabled {
  background-color: #ccc;
  cursor: not-allowed;
}
</style>