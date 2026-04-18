import { defineStore } from 'pinia';
import { ref, computed } from 'vue';
import { 
  getConversationsApi, 
  getPrivateHistoryApi, 
  getGroupHistoryApi,
  markAllReadApi,
  markGroupAllReadApi,
  sendPrivateMessageApi,
  sendGroupMessageApi
} from '../api/message.js';
import wsService from '../utils/websocket.js';

export const useChatStore = defineStore('chat', () => {
  // 状态
  const conversations = ref([]); // 会话列表
  const currentConversation = ref(null); // 当前会话
  const currentMessages = ref([]); // 当前消息列表
  const unreadCount = ref(0); // 总未读数
  const currentCursor = ref(null); // 当前游标
  const hasMore = ref(true); // 是否还有更多消息
  const loading = ref(false); // 加载状态

  // 计算属性
  const privateConversations = computed(() => 
    conversations.value.filter(c => c.type === 'private')
  );

  const groupConversations = computed(() => 
    conversations.value.filter(c => c.type === 'group')
  );

  // ==================== 会话列表管理 ====================

  /**
   * 获取会话列表
   */
  const fetchConversations = async () => {
    try {
      loading.value = true;
      const result = await getConversationsApi();
      conversations.value = result.data || [];
      
      // 计算总未读数
      unreadCount.value = conversations.value.reduce((sum, conv) => {
        return sum + (conv.unreadCount || 0);
      }, 0);
    } catch (error) {
      console.error('获取会话列表失败:', error);
    } finally {
      loading.value = false;
    }
  };

  /**
   * 更新会话（收到新消息时调用）
   */
  const updateConversation = (conversationId, newMessage) => {
    const index = conversations.value.findIndex(c => c.id === conversationId);
    
    if (index !== -1) {
      // 更新现有会话
      const conv = conversations.value[index];
      conv.lastMessage = newMessage.content;
      conv.lastMessageTime = newMessage.createTime || new Date().toISOString();
      
      // 如果不是当前会话，增加未读数
      if (currentConversation.value?.id !== conversationId) {
        conv.unreadCount = (conv.unreadCount || 0) + 1;
        unreadCount.value++;
      }
      
      // 移到列表顶部
      conversations.value.splice(index, 1);
      conversations.value.unshift(conv);
    } else {
      // 新会话
      conversations.value.unshift({
        id: conversationId,
        lastMessage: newMessage.content,
        lastMessageTime: newMessage.createTime || new Date().toISOString(),
        unreadCount: 1
      });
      unreadCount.value++;
    }
  };

  // ==================== 消息管理 ====================

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
   * 加载消息历史
   */
  const loadMessages = async (conversation, isLoadMore = false) => {
    try {
      loading.value = true;
      
      let result;
      const params = {
        cursor: currentCursor.value,
        pageSize: 20
      };

      if (conversation.type === 'private') {
        result = await getPrivateHistoryApi(conversation.targetId, params);
      } else {
        result = await getGroupHistoryApi(conversation.groupId, params);
      }

      const { records, nextCursor, hasMore: more } = result.data;
      
      if (isLoadMore) {
        // 加载更多历史消息，插入到列表头部
        currentMessages.value = [...records.reverse(), ...currentMessages.value];
      } else {
        // 首次加载
        currentMessages.value = records.reverse(); // 反转使最新消息在底部
      }
      
      currentCursor.value = nextCursor;
      hasMore.value = more;
    } catch (error) {
      console.error('加载消息失败:', error);
    } finally {
      loading.value = false;
    }
  };

  /**
   * 加载更多历史消息
   */
  const loadMoreMessages = async () => {
    if (!hasMore.value || loading.value || !currentConversation.value) return;
    await loadMessages(currentConversation.value, true);
  };

  /**
   * 添加新消息（收到 WebSocket 消息或发送成功后）
   */
  const addMessage = (message) => {
    currentMessages.value.push(message);
  };

  // ==================== 发送消息 ====================

  /**
   * 发送私聊消息
   */
  const sendPrivateMessage = async (receiverId, content, messageType = 1, imageFile = null) => {
    try {
      const formData = new FormData();
      formData.append('receiverId', receiverId);
      formData.append('content', content);
      formData.append('messageType', messageType);
      
      if (imageFile) {
        formData.append('imageFile', imageFile);
      }

      const result = await sendPrivateMessageApi(formData);
      
      // 乐观更新：立即添加到消息列表
      addMessage(result.data);
      
      return { success: true, data: result.data };
    } catch (error) {
      console.error('发送消息失败:', error);
      return { success: false, error };
    }
  };

  /**
   * 发送群聊消息
   */
  const sendGroupMessage = async (groupId, content, messageType = 1, imageFile = null) => {
    try {
      const formData = new FormData();
      formData.append('groupId', groupId);
      formData.append('content', content);
      formData.append('messageType', messageType);
      
      if (imageFile) {
        formData.append('imageFile', imageFile);
      }

      const result = await sendGroupMessageApi(formData);
      
      // 乐观更新
      addMessage(result.data);
      
      return { success: true, data: result.data };
    } catch (error) {
      console.error('发送群消息失败:', error);
      return { success: false, error };
    }
  };

  // ==================== 已读管理 ====================

  /**
   * 标记会话为已读
   */
  const markAsRead = async (conversation) => {
    try {
      if (conversation.type === 'private') {
        await markAllReadApi(conversation.targetId);
        
        // WebSocket 批量标记（更快）
        const lastMessage = currentMessages.value[currentMessages.value.length - 1];
        if (lastMessage) {
          wsService.send('mark_read', {
            friendUserId: conversation.targetId,
            upToMessageId: lastMessage.id
          });
        }
      } else {
        await markGroupAllReadApi(conversation.groupId);
      }

      // 清除未读数
      const index = conversations.value.findIndex(c => c.id === conversation.id);
      if (index !== -1) {
        const unread = conversations.value[index].unreadCount || 0;
        conversations.value[index].unreadCount = 0;
        unreadCount.value = Math.max(0, unreadCount.value - unread);
      }
    } catch (error) {
      console.error('标记已读失败:', error);
    }
  };

  // ==================== WebSocket 消息处理 ====================

  /**
   * 处理收到的私聊消息
   */
  const handlePrivateMessage = (message) => {
    // 判断是否在当前聊天窗口
    if (currentConversation.value?.type === 'private' && 
        currentConversation.value?.targetId === message.senderId) {
      // 在当前窗口，直接添加
      addMessage(message);
    } else {
      // 不在当前窗口，更新会话列表
      updateConversation(message.senderId, message);
    }
  };

  /**
   * 处理收到的群聊消息
   */
  const handleGroupMessage = (message) => {
    const groupId = message.groupId;
    
    // 判断是否在当前聊天窗口
    if (currentConversation.value?.type === 'group' && 
        currentConversation.value?.groupId === groupId) {
      // 在当前窗口，直接添加
      addMessage(message);
    } else {
      // 不在当前窗口，更新会话列表
      updateConversation(groupId, message);
    }
  };

  /**
   * 处理离线消息数
   */
  const handleOfflineMessageCount = (message) => {
    if (message.count > 0) {
      unreadCount.value += message.count;
      // 可以选择立即拉取离线消息或等用户打开时再加载
      fetchConversations();
    }
  };

  // ==================== 初始化 ====================

  /**
   * 初始化聊天 store，注册 WebSocket 监听器
   */
  const init = () => {
    // 获取会话列表
    fetchConversations();

    // 注册 WebSocket 监听器
    wsService.on('private_message', handlePrivateMessage);
    wsService.on('group_message', handleGroupMessage);
    wsService.on('offline_message_count', handleOfflineMessageCount);
  };

  /**
   * 清理资源
   */
  const destroy = () => {
    // 移除 WebSocket 监听器
    wsService.off('private_message', handlePrivateMessage);
    wsService.off('group_message', handleGroupMessage);
    wsService.off('offline_message_count', handleOfflineMessageCount);
  };

  return {
    // 状态
    conversations,
    currentConversation,
    currentMessages,
    unreadCount,
    currentCursor,
    hasMore,
    loading,
    
    // 计算属性
    privateConversations,
    groupConversations,
    
    // 方法
    fetchConversations,
    updateConversation,
    setCurrentConversation,
    loadMessages,
    loadMoreMessages,
    addMessage,
    sendPrivateMessage,
    sendGroupMessage,
    markAsRead,
    handlePrivateMessage,
    handleGroupMessage,
    handleOfflineMessageCount,
    init,
    destroy
  };
});
