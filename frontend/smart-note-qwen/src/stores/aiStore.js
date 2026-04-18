import { defineStore } from 'pinia';
import { ref } from 'vue';
import { 
  sendAIMessageApi,  // ✅ 修正：使用正确的函数名
  chatWithDocumentApi,
  getChatHistoryApi, 
  getConversationsApi,
  deleteConversationApi,
  clearAllConversationsApi
} from '../api/ai.js';

export const useAiStore = defineStore('ai', () => {
  const conversations = ref([]);
  const currentConversation = ref(null);
  const messages = ref([]);
  const isLoading = ref(false);
  const isChatOpen = ref(false);
  
  // 游标分页状态
  const currentCursor = ref(null);
  const hasMore = ref(true);

  const setCurrentConversation = (conversation) => {
    currentConversation.value = conversation;
  };

  const addMessage = (message) => {
    messages.value.push(message);
  };
  
  const updateLastMessage = (content) => {
    if (messages.value.length > 0) {
      const lastMessage = messages.value[messages.value.length - 1];
      if (lastMessage.role === 'assistant') {
        lastMessage.content = content;
      }
    }
  };

  const startNewConversation = () => {
    currentConversation.value = null;
    messages.value = [];
    currentCursor.value = null;
    hasMore.value = true;
  };

  // 发送文本消息（SSE流式）
  const sendMessage = async (content, files = []) => {
    if (!content.trim() && files.length === 0) return;

    isLoading.value = true;
    
    try {
      // 添加用户消息到本地
      const userMessage = {
        id: Date.now(),
        role: 'user',
        content: content,
        files: files.map(f => f.name || f),
        createTime: new Date().toISOString()
      };
      addMessage(userMessage);

      // 准备请求数据
      const requestData = {
        content: content,
        conversationId: currentConversation.value?.conversationId || '',
        files: files.filter(f => typeof f === 'string') // 已上传的文件URL
      };

      // 发送消息到后端（SSE流式）
      const response = await sendAIMessageApi(requestData);
      
      // 创建AI回复消息占位
      const aiMessageId = Date.now() + 1;
      addMessage({
        id: aiMessageId,
        role: 'assistant',
        content: '',
        createTime: new Date().toISOString()
      });
      
      // 流式处理AI回复
      let aiResponse = '';
      const reader = response.body.getReader();
      const decoder = new TextDecoder();
      
      while (true) {
        const { done, value } = await reader.read();
        if (done) break;
        
        const chunk = decoder.decode(value, { stream: true });
        const lines = chunk.split('\n');
        
        for (const line of lines) {
          if (line.startsWith('data: ')) {
            const data = line.slice(6);
            if (data !== '[DONE]') {
              aiResponse += data;
              // 实时更新UI
              updateLastMessage(aiResponse);
            }
          }
        }
      }

      // 更新会话列表
      await loadConversations();
      
      return { success: true };
    } catch (error) {
      console.error('Send message error:', error);
      return { success: false, error: error.message };
    } finally {
      isLoading.value = false;
    }
  };
  
  // 与文件对话（SSE流式）
  const sendMessageWithFiles = async (content, fileObjects) => {
    if (!content.trim() && fileObjects.length === 0) return;

    isLoading.value = true;
    
    try {
      // 添加用户消息到本地
      const userMessage = {
        id: Date.now(),
        role: 'user',
        content: content,
        files: fileObjects.map(f => f.name),
        createTime: new Date().toISOString()
      };
      addMessage(userMessage);

      // 准备FormData
      const formData = new FormData();
      formData.append('content', content);
      
      if (currentConversation.value?.conversationId) {
        formData.append('conversationId', currentConversation.value.conversationId);
      }
      
      // 添加文件
      fileObjects.forEach(file => {
        formData.append('files', file);
      });

      // 发送消息到后端（SSE流式）
      const response = await chatWithDocumentApi(formData);
      
      // 创建AI回复消息占位
      const aiMessageId = Date.now() + 1;
      addMessage({
        id: aiMessageId,
        role: 'assistant',
        content: '',
        createTime: new Date().toISOString()
      });
      
      // 流式处理AI回复
      let aiResponse = '';
      const reader = response.body.getReader();
      const decoder = new TextDecoder();
      
      while (true) {
        const { done, value } = await reader.read();
        if (done) break;
        
        const chunk = decoder.decode(value, { stream: true });
        const lines = chunk.split('\n');
        
        for (const line of lines) {
          if (line.startsWith('data: ')) {
            const data = line.slice(6);
            if (data !== '[DONE]') {
              aiResponse += data;
              // 实时更新UI
              updateLastMessage(aiResponse);
            }
          }
        }
      }

      // 更新会话列表
      await loadConversations();
      
      return { success: true };
    } catch (error) {
      console.error('Send message with files error:', error);
      return { success: false, error: error.message };
    } finally {
      isLoading.value = false;
    }
  };

  // 加载聊天历史（支持游标分页）
  const loadChatHistory = async (conversationId = null, cursor = null, isLoadMore = false) => {
    try {
      const response = await getChatHistoryApi({
        conversationId,
        cursor,
        pageSize: 20
      });
      
      const data = response.data.data;
      const newMessages = data.records || [];
      
      if (isLoadMore) {
        // 加载更多时，将新消息插入到前面
        messages.value = [...newMessages, ...messages.value];
      } else {
        // 首次加载时，替换所有消息
        messages.value = newMessages;
      }
      
      // 更新游标
      currentCursor.value = data.nextCursor;
      hasMore.value = data.hasMore;
      
      return response.data;
    } catch (error) {
      console.error('Load chat history error:', error);
      throw error;
    }
  };
  
  // 加载更多历史消息
  const loadMoreHistory = async () => {
    if (!hasMore.value || !currentConversation.value) return;
    
    await loadChatHistory(
      currentConversation.value.conversationId,
      currentCursor.value,
      true
    );
  };

  // 加载会话列表
  const loadConversations = async () => {
    try {
      const response = await getConversationsApi();
      conversations.value = response.data.data || [];
      return response.data;
    } catch (error) {
      console.error('Load conversations error:', error);
      throw error;
    }
  };
  
  // 删除会话
  const deleteConversation = async (conversationId) => {
    try {
      await deleteConversationApi(conversationId);
      
      // 如果删除的是当前会话，清空状态
      if (currentConversation.value?.conversationId === conversationId) {
        startNewConversation();
      }
      
      // 更新会话列表
      await loadConversations();
      
      return { success: true };
    } catch (error) {
      console.error('Delete conversation error:', error);
      return { success: false, error: error.message };
    }
  };
  
  // 清空所有会话
  const clearAllConversations = async () => {
    try {
      await clearAllConversationsApi();
      startNewConversation();
      conversations.value = [];
      return { success: true };
    } catch (error) {
      console.error('Clear all conversations error:', error);
      return { success: false, error: error.message };
    }
  };

  const toggleChat = () => {
    isChatOpen.value = !isChatOpen.value;
  };

  return {
    conversations,
    currentConversation,
    messages,
    isLoading,
    isChatOpen,
    currentCursor,
    hasMore,
    setCurrentConversation,
    addMessage,
    startNewConversation,
    sendMessage,
    sendMessageWithFiles,
    loadChatHistory,
    loadMoreHistory,
    loadConversations,
    deleteConversation,
    clearAllConversations,
    toggleChat
  };
});