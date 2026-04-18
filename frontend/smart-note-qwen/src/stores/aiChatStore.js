import { defineStore } from 'pinia';
import { ref, computed } from 'vue';
import { 
  sendAIMessageApi,
  chatWithDocumentApi,
  getChatHistoryApi,
  getConversationsApi,
  deleteConversationApi,
  clearAllConversationsApi
} from '../api/ai.js';
import { ElMessage } from 'element-plus';

export const useAIChatStore = defineStore('aiChat', () => {
  // 状态
  const conversations = ref([]); // 会话列表
  const currentConversation = ref(null); // 当前会话
  const currentMessages = ref([]); // 当前消息列表
  const loading = ref(false); // 加载状态
  const isStreaming = ref(false); // 是否正在流式输出
  const streamAborter = ref(null); // 流式输出中止器
  const currentCursor = ref(null); // 当前游标
  const hasMore = ref(true); // 是否还有更多历史

  // 计算属性
  const currentConversationId = computed(() => 
    currentConversation.value?.id || null
  );

  // ==================== 会话管理 ====================

  /**
   * 获取会话列表
   */
  const fetchConversations = async () => {
    try {
      loading.value = true;
      const result = await getConversationsApi();
      conversations.value = result.data || [];
    } catch (error) {
      console.error('获取会话列表失败:', error);
      ElMessage.error('获取会话列表失败');
    } finally {
      loading.value = false;
    }
  };

  /**
   * 设置当前会话
   */
  const setCurrentConversation = (conversation) => {
    currentConversation.value = conversation;
    currentMessages.value = [];
    currentCursor.value = null;
    hasMore.value = true;
  };

  /**
   * 创建新会话
   */
  const createNewConversation = () => {
    currentConversation.value = null;
    currentMessages.value = [];
    currentCursor.value = null;
    hasMore.value = true;
  };

  /**
   * 删除会话
   */
  const deleteConversation = async (conversationId) => {
    try {
      await deleteConversationApi(conversationId);
      
      // 从列表中移除
      conversations.value = conversations.value.filter(c => c.id !== conversationId);
      
      // 如果删除的是当前会话，创建新会话
      if (currentConversation.value?.id === conversationId) {
        createNewConversation();
      }
      
      ElMessage.success('会话已删除');
    } catch (error) {
      console.error('删除会话失败:', error);
      ElMessage.error('删除会话失败');
    }
  };

  /**
   * 清空所有会话
   */
  const clearAllConversations = async () => {
    try {
      await clearAllConversationsApi();
      conversations.value = [];
      createNewConversation();
      ElMessage.success('已清空所有会话');
    } catch (error) {
      console.error('清空会话失败:', error);
      ElMessage.error('清空会话失败');
    }
  };

  // ==================== 消息管理 ====================

  /**
   * 加载聊天历史
   */
  const loadHistory = async (isLoadMore = false) => {
    try {
      loading.value = true;
      
      const params = {
        cursor: currentCursor.value,
        pageSize: 20
      };
      
      // 如果有当前会话，加载该会话的历史
      if (currentConversation.value?.id) {
        params.conversationId = currentConversation.value.id;
      }
      
      const result = await getChatHistoryApi(params);
      const { records, nextCursor, hasMore: more } = result.data;
      
      if (isLoadMore) {
        // 加载更多历史消息，插入到列表头部
        currentMessages.value = [...records.reverse(), ...currentMessages.value];
      } else {
        // 首次加载
        currentMessages.value = records.reverse();
      }
      
      currentCursor.value = nextCursor;
      hasMore.value = more;
    } catch (error) {
      console.error('加载历史失败:', error);
      ElMessage.error('加载历史记录失败');
    } finally {
      loading.value = false;
    }
  };

  /**
   * 加载更多历史消息
   */
  const loadMoreHistory = async () => {
    if (!hasMore.value || loading.value) return;
    await loadHistory(true);
  };

  /**
   * 添加消息到列表
   */
  const addMessage = (message) => {
    currentMessages.value.push(message);
  };

  /**
   * 更新最后一条消息（用于流式输出）
   */
  const updateLastMessage = (content) => {
    const lastIndex = currentMessages.value.length - 1;
    if (lastIndex >= 0) {
      currentMessages.value[lastIndex].content += content;
    }
  };

  // ==================== 发送消息 ====================

  /**
   * 发送 AI 对话消息（流式输出）
   */
  const sendAIMessage = async (content, files = []) => {
    try {
      isStreaming.value = true;
      
      // 创建中止控制器
      streamAborter.value = new AbortController();
      
      // 添加用户消息
      const userMessage = {
        id: Date.now(),
        role: 'user',
        content,
        createTime: new Date().toISOString()
      };
      addMessage(userMessage);
      
      // 添加 AI 回复占位
      const aiMessage = {
        id: Date.now() + 1,
        role: 'assistant',
        content: '',
        createTime: new Date().toISOString(),
        isStreaming: true
      };
      addMessage(aiMessage);
      
      // 调用 SSE 接口
      const response = await sendAIMessageApi({
        content,
        conversationId: currentConversationId.value,
        files
      });
      
      // 处理 SSE 流
      const reader = response.body.getReader();
      const decoder = new TextDecoder();
      let buffer = '';
      
      while (true) {
        const { done, value } = await reader.read();
        
        if (done) {
          break;
        }
        
        // 检查是否被中止
        if (streamAborter.value.signal.aborted) {
          reader.cancel();
          break;
        }
        
        // 解码数据
        buffer += decoder.decode(value, { stream: true });
        
        // 解析 SSE 数据
        const lines = buffer.split('\n');
        buffer = lines.pop(); // 保留不完整的行
        
        for (const line of lines) {
          if (line.startsWith('data:')) {
            const data = line.slice(5).trim();
            if (data) {
              // 更新 AI 消息内容
              updateLastMessage(data);
            }
          }
        }
      }
      
      // 标记流式输出结束
      aiMessage.isStreaming = false;
      
      // 更新会话列表
      if (!currentConversation.value) {
        await fetchConversations();
        // 设置最新会话为当前会话
        if (conversations.value.length > 0) {
          currentConversation.value = conversations.value[0];
        }
      }
      
    } catch (error) {
      if (error.name === 'AbortError') {
        console.log('流式输出已停止');
        // 标记最后一条消息为已停止
        const lastIndex = currentMessages.value.length - 1;
        if (lastIndex >= 0) {
          currentMessages.value[lastIndex].isStreaming = false;
          currentMessages.value[lastIndex].content += '\n\n[已停止生成]';
        }
      } else {
        console.error('发送消息失败:', error);
        ElMessage.error('发送消息失败');
        // 移除失败的消息
        currentMessages.value.pop(); // 移除 AI 消息
        currentMessages.value.pop(); // 移除用户消息
      }
    } finally {
      isStreaming.value = false;
      streamAborter.value = null;
    }
  };

  /**
   * 停止生成
   */
  const stopGeneration = () => {
    if (streamAborter.value) {
      streamAborter.value.abort();
    }
  };

  /**
   * 与文件对话
   */
  const chatWithDocument = async (content, files) => {
    try {
      isStreaming.value = true;
      
      // 创建中止控制器
      streamAborter.value = new AbortController();
      
      // 构建 FormData
      const formData = new FormData();
      formData.append('content', content);
      
      if (currentConversationId.value) {
        formData.append('conversationId', currentConversationId.value);
      }
      
      // 添加文件
      if (files && files.length > 0) {
        files.forEach(file => {
          formData.append('files', file);
        });
      }
      
      // 添加用户消息
      const userMessage = {
        id: Date.now(),
        role: 'user',
        content,
        files: files.map(f => f.name),
        createTime: new Date().toISOString()
      };
      addMessage(userMessage);
      
      // 添加 AI 回复占位
      const aiMessage = {
        id: Date.now() + 1,
        role: 'assistant',
        content: '',
        createTime: new Date().toISOString(),
        isStreaming: true
      };
      addMessage(aiMessage);
      
      // 调用 SSE 接口
      const response = await chatWithDocumentApi(formData);
      
      // 处理 SSE 流（与 sendAIMessage 相同）
      const reader = response.body.getReader();
      const decoder = new TextDecoder();
      let buffer = '';
      
      while (true) {
        const { done, value } = await reader.read();
        
        if (done) break;
        
        if (streamAborter.value.signal.aborted) {
          reader.cancel();
          break;
        }
        
        buffer += decoder.decode(value, { stream: true });
        const lines = buffer.split('\n');
        buffer = lines.pop();
        
        for (const line of lines) {
          if (line.startsWith('data:')) {
            const data = line.slice(5).trim();
            if (data) {
              updateLastMessage(data);
            }
          }
        }
      }
      
      aiMessage.isStreaming = false;
      
      // 更新会话列表
      if (!currentConversation.value) {
        await fetchConversations();
        if (conversations.value.length > 0) {
          currentConversation.value = conversations.value[0];
        }
      }
      
    } catch (error) {
      if (error.name === 'AbortError') {
        console.log('流式输出已停止');
        const lastIndex = currentMessages.value.length - 1;
        if (lastIndex >= 0) {
          currentMessages.value[lastIndex].isStreaming = false;
          currentMessages.value[lastIndex].content += '\n\n[已停止生成]';
        }
      } else {
        console.error('文件对话失败:', error);
        ElMessage.error('文件对话失败');
        currentMessages.value.pop();
        currentMessages.value.pop();
      }
    } finally {
      isStreaming.value = false;
      streamAborter.value = null;
    }
  };

  return {
    // 状态
    conversations,
    currentConversation,
    currentMessages,
    loading,
    isStreaming,
    currentCursor,
    hasMore,
    currentConversationId,
    
    // 方法
    fetchConversations,
    setCurrentConversation,
    createNewConversation,
    deleteConversation,
    clearAllConversations,
    loadHistory,
    loadMoreHistory,
    addMessage,
    updateLastMessage,
    sendAIMessage,
    stopGeneration,
    chatWithDocument
  };
});
