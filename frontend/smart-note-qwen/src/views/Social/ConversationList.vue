<template>
  <div class="conversation-list">
    <div class="list-header">
      <h2>消息</h2>
      <button @click="loadConversations" class="refresh-btn">
        <i class="fas fa-sync-alt"></i>
      </button>
    </div>
    
    <div class="search-section">
      <div class="search-box">
        <i class="fas fa-search"></i>
        <input 
          v-model="searchKeyword" 
          type="text" 
          placeholder="搜索会话..."
        >
      </div>
    </div>
    
    <div class="conversations-container">
      <div 
        v-for="conversation in filteredConversations" 
        :key="conversation.id" 
        class="conversation-item"
        @click="openConversation(conversation)"
      >
        <div class="conversation-avatar">
          <img 
            :src="getAvatarUrl(conversation.avatar)" 
            :alt="conversation.name"
            @error="handleImageError"
          >
          <!-- 在线状态指示器（仅私聊） -->
          <span 
            v-if="conversation.type === 'private' && conversation.status" 
            class="status-indicator" 
            :class="getStatusClass(conversation.status)"
          ></span>
        </div>
        
        <div class="conversation-info">
          <div class="conversation-header">
            <h4 class="conversation-name">{{ conversation.name }}</h4>
            <span class="conversation-time">{{ formatTime(conversation.lastMessageTime) }}</span>
          </div>
          
          <div class="conversation-preview">
            <p class="last-message">{{ conversation.lastMessage || '暂无消息' }}</p>
            <div v-if="conversation.unreadCount > 0" class="unread-badge">
              {{ conversation.unreadCount > 99 ? '99+' : conversation.unreadCount }}
            </div>
          </div>
        </div>
      </div>
      
      <div v-if="filteredConversations.length === 0 && !loading" class="empty-state">
        <i class="fas fa-comments"></i>
        <p>暂无会话</p>
        <router-link to="/app/friends" class="start-chat-btn">
          开始聊天
        </router-link>
      </div>
    </div>
    
    <!-- 加载遮罩 -->
    <div v-if="loading" class="loading-overlay">
      <div class="loading-spinner"></div>
    </div>
  </div>
</template>

<script>
import { ref, computed, onMounted, onUnmounted, onActivated } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { useWebSocket, WS_MESSAGE_TYPES } from '../../hooks/useWebSocket.js';
import { getConversationListApi } from '../../api/social.js';
import { useChatStore } from '../../stores/chatStore.js';

export default {
  name: 'ConversationList',
  setup() {
    const router = useRouter();
    const { connect, disconnect, on, off } = useWebSocket();
    
    // 状态变量
    const conversations = ref([]);
    const searchKeyword = ref('');
    const loading = ref(false);
    
    // 计算属性
    const filteredConversations = computed(() => {
      if (!searchKeyword.value) return conversations.value;
      
      const keyword = searchKeyword.value.toLowerCase();
      return conversations.value.filter(conv => 
        conv.name.toLowerCase().includes(keyword) ||
        (conv.lastMessage && conv.lastMessage.toLowerCase().includes(keyword))
      );
    });
    
    // 加载会话列表
    const loadConversations = async () => {
      loading.value = true;
      
      try {
        const response = await getConversationListApi();
        const data = response.data.data || [];
        
        // 处理后端返回的数据，映射为前端需要的格式
        conversations.value = data.map(conv => ({
          id: conv.id || conv.groupId || conv.friendUserId, // 会话ID
          type: conv.conversationType === 'private' ? 'private' : 'group',
          targetId: conv.conversationType === 'private' ? conv.friendUserId : conv.groupId,
          name: conv.conversationType === 'private' ? conv.friendUsername : conv.groupName,
          avatar: conv.conversationType === 'private' ? conv.friendAvatar : conv.groupAvatar,
          status: conv.status,
          lastMessage: conv.lastMessage || '',
          lastMessageTime: conv.lastMessageTime,
          unreadCount: conv.unreadCount || 0
        }));
        
        console.log('Loaded conversations:', conversations.value);
      } catch (error) {
        console.error('Load conversations error:', error);
        ElMessage.error('加载会话列表失败');
      } finally {
        loading.value = false;
      }
    };
    
    // 打开会话
    const openConversation = (conversation) => {
      if (conversation.type === 'private') {
        router.push(`/app/chat/private/${conversation.targetId}`);
      } else if (conversation.type === 'group') {
        router.push(`/app/groups`);
        // TODO: 可以跳转到群聊详情并选中该群
      }
    };
    
    // 获取状态类
    const getStatusClass = (status) => {
      switch (status) {
        case '1': return 'online';
        case '2': return 'away';
        case '3': return 'busy';
        default: return 'offline';
      }
    };
    
    // 格式化时间
    const formatTime = (dateString) => {
      if (!dateString) return '';
      const date = new Date(dateString);
      const now = new Date();
      const diff = now - date;
      
      // 今天
      if (diff < 24 * 60 * 60 * 1000 && date.getDate() === now.getDate()) {
        return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' });
      }
      
      // 昨天
      const yesterday = new Date(now);
      yesterday.setDate(yesterday.getDate() - 1);
      if (date.getDate() === yesterday.getDate() && date.getMonth() === yesterday.getMonth()) {
        return '昨天';
      }
      
      // 本周
      const weekAgo = new Date(now);
      weekAgo.setDate(weekAgo.getDate() - 7);
      if (date > weekAgo) {
        const days = ['周日', '周一', '周二', '周三', '周四', '周五', '周六'];
        return days[date.getDay()];
      }
      
      // 更早
      return date.toLocaleDateString('zh-CN', { month: '2-digit', day: '2-digit' });
    };
    
    // 获取头像URL
    const getAvatarUrl = (avatar) => {
      // 如果有头像URL，直接返回
      if (avatar) return avatar;
      
      // 否则返回默认头像（使用SVG内联方式，确保始终可用）
      return "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 100 100'%3E%3Ccircle cx='50' cy='50' r='50' fill='%23409eff'/%3E%3Ccircle cx='50' cy='35' r='15' fill='white'/%3E%3Cellipse cx='50' cy='75' rx='25' ry='20' fill='white'/%3E%3C/svg%3E";
    };
    
    // 处理图片加载错误
    const handleImageError = (event) => {
      // 当图片加载失败时，使用默认头像
      event.target.src = "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 100 100'%3E%3Ccircle cx='50' cy='50' r='50' fill='%23409eff'/%3E%3Ccircle cx='50' cy='35' r='15' fill='white'/%3E%3Cellipse cx='50' cy='75' rx='25' ry='20' fill='white'/%3E%3C/svg%3E";
    };
    
    onMounted(() => {
      loadConversations();
      
      // 连接WebSocket
      connect();
      
      // 监听新消息，更新会话列表
      const handleMessage = (message) => {
        // 查找对应的会话
        const convIndex = conversations.value.findIndex(c => 
          (c.type === 'private' && c.targetId == (message.senderId || message.receiverId)) ||
          (c.type === 'group' && c.targetId == message.groupId)
        );
        
        if (convIndex !== -1) {
          // 更新会话信息
          const conv = conversations.value[convIndex];
          conv.lastMessage = message.content;
          conv.lastMessageTime = message.createTime;
          
          // 如果不是当前活跃的会话，增加未读数
          conv.unreadCount = (conv.unreadCount || 0) + 1;
          
          // 将该会话移到顶部
          conversations.value.splice(convIndex, 1);
          conversations.value.unshift(conv);
        } else {
          // 新会话，重新加载列表
          loadConversations();
        }
      };
      
      on(WS_MESSAGE_TYPES.PRIVATE_MESSAGE, handleMessage);
      on(WS_MESSAGE_TYPES.GROUP_MESSAGE, handleMessage);
      
      // 保存处理器引用
      window._conversationMessageHandler = handleMessage;
    });

    onActivated(() => {
      // 当组件被 keep-alive 激活时（例如从聊天详情返回），刷新列表
      loadConversations();
    });
    
    onUnmounted(() => {
      if (window._conversationMessageHandler) {
        off(WS_MESSAGE_TYPES.PRIVATE_MESSAGE, window._conversationMessageHandler);
        off(WS_MESSAGE_TYPES.GROUP_MESSAGE, window._conversationMessageHandler);
        delete window._conversationMessageHandler;
      }
      
      disconnect();
    });
    
    return {
      conversations,
      searchKeyword,
      loading,
      filteredConversations,
      loadConversations,
      openConversation,
      getAvatarUrl,
      handleImageError,
      getStatusClass,
      formatTime
    };
  }
};
</script>

<style scoped>
.conversation-list {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: #f5f7fa;
}

.list-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 24px;
  background: white;
  border-bottom: 1px solid #eee;
}

.list-header h2 {
  margin: 0;
  color: #333;
}

.refresh-btn {
  background: none;
  border: none;
  font-size: 18px;
  color: #666;
  cursor: pointer;
  padding: 8px;
  border-radius: 4px;
  transition: all 0.3s;
}

.refresh-btn:hover {
  background: #f5f7fa;
  color: #409eff;
}

.search-section {
  padding: 16px 24px;
  background: white;
  border-bottom: 1px solid #eee;
}

.search-box {
  position: relative;
}

.search-box i {
  position: absolute;
  left: 12px;
  top: 50%;
  transform: translateY(-50%);
  color: #999;
}

.search-box input {
  width: 100%;
  padding: 10px 12px 10px 36px;
  border: 1px solid #dcdfe6;
  border-radius: 20px;
  font-size: 14px;
  transition: border-color 0.3s;
}

.search-box input:focus {
  outline: none;
  border-color: #409eff;
}

.conversations-container {
  flex: 1;
  overflow-y: auto;
}

.conversation-item {
  display: flex;
  align-items: center;
  padding: 16px 24px;
  background: white;
  border-bottom: 1px solid #f0f0f0;
  cursor: pointer;
  transition: background 0.3s;
}

.conversation-item:hover {
  background: #f8f9fa;
}

.conversation-avatar {
  position: relative;
  margin-right: 12px;
}

.conversation-avatar img {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  object-fit: cover;
}

.status-indicator {
  position: absolute;
  bottom: 2px;
  right: 2px;
  width: 12px;
  height: 12px;
  border-radius: 50%;
  border: 2px solid white;
}

.status-indicator.online {
  background: #67c23a;
}

.status-indicator.away {
  background: #e6a23c;
}

.status-indicator.busy {
  background: #f56c6c;
}

.status-indicator.offline {
  background: #dcdfe6;
}

.conversation-info {
  flex: 1;
  min-width: 0;
}

.conversation-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 4px;
}

.conversation-name {
  margin: 0;
  font-size: 15px;
  color: #333;
  font-weight: 500;
}

.conversation-time {
  font-size: 12px;
  color: #999;
}

.conversation-preview {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.last-message {
  margin: 0;
  font-size: 13px;
  color: #666;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex: 1;
}

.unread-badge {
  background: #f56c6c;
  color: white;
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 10px;
  min-width: 18px;
  text-align: center;
  margin-left: 8px;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 20px;
  color: #999;
}

.empty-state i {
  font-size: 64px;
  margin-bottom: 16px;
  opacity: 0.5;
}

.empty-state p {
  margin: 0 0 24px 0;
  font-size: 14px;
}

.start-chat-btn {
  background: #409eff;
  color: white;
  padding: 10px 24px;
  border-radius: 20px;
  text-decoration: none;
  font-size: 14px;
  transition: all 0.3s;
}

.start-chat-btn:hover {
  background: #66b1ff;
}

.loading-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0,0,0,0.3);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 9999;
}

.loading-spinner {
  width: 40px;
  height: 40px;
  border: 4px solid #f3f3f3;
  border-top: 4px solid #409eff;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}
</style>
