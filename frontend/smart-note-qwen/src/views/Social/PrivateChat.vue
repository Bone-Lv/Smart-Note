<template>
  <div class="private-chat">
    <div class="chat-header">
      <div class="header-info">
        <button @click="goBack" class="back-btn">
          <i class="fas fa-arrow-left"></i>
        </button>
        <img 
          :src="friendInfo?.friendAvatar || '/default-avatar.svg'" 
          alt="avatar" 
          class="friend-avatar"
        >
        <div class="friend-details">
          <h3>{{ friendInfo?.remark || friendInfo?.friendUsername || '加载中...' }}</h3>
          <p class="status-text">
            <span class="status-dot" :class="getStatusClass(friendInfo?.status)"></span>
            {{ getStatusText(friendInfo?.status) }}
          </p>
        </div>
      </div>
      <div class="header-actions">
        <button @click="showMoreOptions = !showMoreOptions" class="more-btn">
          <i class="fas fa-ellipsis-v"></i>
        </button>
      </div>
    </div>
    
    <!-- 更多选项面板 -->
    <div v-if="showMoreOptions" class="more-options-panel">
      <div class="option-item" @click="clearChatHistory">
        <i class="fas fa-trash-alt"></i>
        <span>清空聊天记录</span>
      </div>
      <div class="option-item danger" @click="deleteFriend">
        <i class="fas fa-user-times"></i>
        <span>删除好友</span>
      </div>
    </div>
    
    <!-- 消息区域 -->
    <div class="messages-section">
      <div class="messages-container" ref="messagesContainer" @scroll="handleScroll">
        <!-- 加载更多提示 -->
        <div v-if="loadingMore" class="loading-more">
          <i class="fas fa-spinner fa-spin"></i>
          <span>加载中...</span>
        </div>
        <div v-else-if="!hasMore && messages.length > 0" class="no-more-messages">
          没有更多消息了
        </div>
        
        <div 
          v-for="message in messages" 
          :key="message.id" 
          class="message-item"
          :class="{ 'message-own': String(message.senderId) === String(currentUserId) }"
        >
          <!-- 头像 -->
          <div class="message-sender" :class="{ 'message-sender-own': String(message.senderId) === String(currentUserId) }">
            <img 
              :src="message.senderAvatar || (String(message.senderId) === String(currentUserId) ? userStore.userInfo?.avatar : null) || '/default-avatar.svg'" 
              alt="avatar" 
              class="sender-avatar"
            >
          </div>
          
          <!-- 消息内容 -->
          <div class="message-content">
            <div 
              class="message-bubble" 
              :class="{ 'bubble-own': String(message.senderId) === String(currentUserId) }"
            >
              <!-- 文本消息 -->
              <div v-if="message.messageType === 1" class="text-message">
                {{ message.content }}
              </div>
              
              <!-- 图片消息 -->
              <div v-else-if="message.messageType === 2" class="image-message">
                <img 
                  :src="message.imageUrl || message.content" 
                  alt="image" 
                  class="message-image"
                  @click="previewImage(message.imageUrl || message.content)"
                >
              </div>
              
              <!-- 文件消息 -->
              <div v-else class="file-message">
                <i class="fas fa-file"></i>
                <span>{{ message.fileName || '文件' }}</span>
                <a :href="message.fileUrl" download class="download-link">
                  <i class="fas fa-download"></i>
                </a>
              </div>
            </div>
            <div class="message-time">{{ formatTime(message.createTime) }}</div>
          </div>
        </div>
        
        <div v-if="messages.length === 0 && !loading" class="empty-state">
          <i class="fas fa-comments"></i>
          <p>开始聊天吧</p>
        </div>
      </div>
      
      <!-- 消息输入区域 -->
      <div class="input-section">
        <div class="input-tools">
          <button @click="triggerImageUpload" class="tool-btn" title="发送图片">
            <i class="fas fa-image"></i>
          </button>
          <input 
            ref="imageInput"
            type="file" 
            accept="image/*" 
            style="display: none"
            @change="handleImageSelect"
          >
        </div>
        
        <div class="input-area">
          <textarea
            v-model="messageInput"
            placeholder="输入消息..."
            rows="3"
            @keydown.enter.exact.prevent="sendMessage"
            @keydown.enter.shift.exact="insertNewline"
          ></textarea>
          <button 
            @click="sendMessage" 
            class="send-btn" 
            :disabled="(!messageInput.trim() && !selectedImage) || sending"
          >
            <i class="fas fa-paper-plane"></i>
          </button>
        </div>
        
        <!-- 图片预览 -->
        <div v-if="selectedImage" class="image-preview">
          <img :src="imagePreviewUrl" alt="preview">
          <button @click="cancelImage" class="cancel-btn">
            <i class="fas fa-times"></i>
          </button>
        </div>
      </div>
    </div>
    
    <!-- 加载遮罩 -->
    <div v-if="loading" class="loading-overlay">
      <div class="loading-spinner"></div>
    </div>
  </div>
</template>

<script>
import { ref, computed, onMounted, onUnmounted, nextTick, watch } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { ElMessage, ElNotification, ElMessageBox } from 'element-plus';
import { useUserStore } from '../../stores/userStore.js';
import { useWebSocket, WS_MESSAGE_TYPES } from '../../hooks/useWebSocket.js';
import { 
  getPrivateMessageHistoryApi, 
  sendPrivateMessageApi,
  clearPrivateChatHistoryApi,
  deleteFriendApi,
  getFriendListApi
} from '../../api/social.js';

export default {
  name: 'PrivateChat',
  props: {
    friendUserId: {
      type: [String, Number],
      required: true
    }
  },
  setup(props) {
    const router = useRouter();
    const route = useRoute();
    const userStore = useUserStore();
    const { connect, disconnect, isConnected, on, off, markRead } = useWebSocket();
    
    // 状态变量
    const messages = ref([]);
    const friendInfo = ref(null);
    const loading = ref(false);
    const loadingMore = ref(false);
    const hasMore = ref(true);
    const currentCursor = ref(null);
    const messageInput = ref('');
    const selectedImage = ref(null);
    const imagePreviewUrl = ref('');
    const showMoreOptions = ref(false);
    const messagesContainer = ref(null);
    const imageInput = ref(null);
    
    // 计算属性
    const currentUserId = computed(() => userStore.userInfo?.userId || userStore.userInfo?.id);
    
    // 返回上一页
    const goBack = () => {
      router.back();
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
    
    // 获取状态文本
    const getStatusText = (status) => {
      switch (status) {
        case '1': return '在线';
        case '2': return '离开';
        case '3': return '忙碌';
        default: return '离线';
      }
    };
    
    // 加载好友信息
    const loadFriendInfo = async () => {
      try {
        const response = await getFriendListApi();
        const friends = response.data.data || [];
        friendInfo.value = friends.find(f => f.friendUserId == props.friendUserId);
      } catch (error) {
        console.error('Load friend info error:', error);
      }
    };
    
    // 加载历史消息（支持游标分页）
    const loadMessages = async (isLoadMore = false) => {
      if (!props.friendUserId) return;
      
      if (isLoadMore) {
        if (!hasMore.value || loadingMore.value) return;
        loadingMore.value = true;
      } else {
        loading.value = true;
        currentCursor.value = null;
        hasMore.value = true;
      }
      
      try {
        const response = await getPrivateMessageHistoryApi({
          friendUserId: props.friendUserId,
          cursor: isLoadMore ? currentCursor.value : null,
          pageSize: 20
        });
        
        const data = response.data.data;
        let newMessages = data.records || [];
        
        // 后端已按时间正序返回（旧->新），无需反转
        
        // 标准化消息字段
        newMessages = newMessages.map(msg => {
          const senderId = msg.senderId;
          return {
            ...msg,
            senderId: senderId,
            // 如果是自己发送的消息，使用当前用户的头像
            senderAvatar: String(senderId) === String(currentUserId.value) 
              ? userStore.userInfo?.avatar || '/default-avatar.svg'
              : msg.senderAvatar || '/default-avatar.svg'
          };
        });
        
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
        
        // 滚动处理
        await nextTick();
        if (isLoadMore) {
          // 加载更多时保持滚动位置
          if (messagesContainer.value) {
            messagesContainer.value.scrollTop = newMessages.length * 60;
          }
        } else {
          // 首次加载时滚动到底部
          scrollToBottom();
        }
      } catch (error) {
        console.error('Load messages error:', error);
        ElMessage.error('加载消息失败');
      } finally {
        loading.value = false;
        loadingMore.value = false;
      }
    };
    
    // 加载更多消息
    const loadMoreMessages = async () => {
      await loadMessages(true);
    };
    
    const sending = ref(false); // 发送锁
    
    const sendMessage = async () => {
      console.log('📤 开始发送私聊消息');
      console.log('  输入内容:', messageInput.value);
      console.log('  是否有图片:', selectedImage.value);
      console.log('  好友ID:', props.friendUserId);
      console.log('  当前用户ID:', currentUserId.value);
      
      // 防止重复发送
      if (sending.value) {
        console.log('⚠️ 正在发送中，跳过');
        return;
      }
      
      if ((!messageInput.value.trim() && !selectedImage.value) || !props.friendUserId) {
        console.log('⚠️ 消息内容为空或未选择好友，取消发送');
        return;
      }
      
      try {
        sending.value = true; // 锁定发送
        console.log('  锁定发送状态');
        
        // 创建临时消息对象（乐观更新）
        const tempMessage = {
          id: `temp_${Date.now()}`, // 临时 ID，使用字符串前缀区分
          senderId: currentUserId.value,
          receiverId: props.friendUserId,
          messageType: selectedImage.value ? 2 : 1,
          content: selectedImage.value ? '' : messageInput.value,
          imageUrl: selectedImage.value ? imagePreviewUrl.value : null,
          senderAvatar: userStore.userInfo?.avatar || '/default-avatar.svg',
          createTime: new Date().toISOString(),
          isTemp: true // 标记为临时消息
        };
        
        console.log('📝 创建临时消息:', tempMessage);
        
        // 立即添加到消息列表（乐观更新）
        messages.value.push(tempMessage);
        await nextTick();
        scrollToBottom();
        console.log('✅ 临时消息已添加到列表，当前消息数量:', messages.value.length);
        
        let messageData;
        
        if (selectedImage.value) {
          // 发送图片消息
          messageData = {
            receiverId: props.friendUserId,
            messageType: 2,
            content: '',
            imageFile: selectedImage.value
          };
          console.log('  准备发送图片消息');
        } else {
          // 发送文本消息
          messageData = {
            receiverId: props.friendUserId,
            messageType: 1,
            content: messageInput.value
          };
          console.log('  准备发送文本消息');
        }
        
        console.log('📡 正在调用API发送消息...');
        console.log('  请求数据:', messageData);
        
        // 调用 API 发送消息
        const response = await sendPrivateMessageApi(messageData);
        console.log('✅ API调用成功，后端响应:', response);
        
        // 清空输入
        messageInput.value = '';
        cancelImage();
        console.log('  已清空输入框');
        
        // 注意：后端会通过 WebSocket 推送真实消息回来
        console.log('⏳ 等待WebSocket推送消息...');
      } catch (error) {
        console.error('Send message error:', error);
        ElMessage.error('发送消息失败');
        
        // 如果发送失败，移除临时消息
        const tempIndex = messages.value.findIndex(m => m.id === tempMessage.id);
        if (tempIndex !== -1) {
          messages.value.splice(tempIndex, 1);
        }
      } finally {
        sending.value = false; // 释放锁
      }
    };
    
    // 触发图片上传
    const triggerImageUpload = () => {
      imageInput.value?.click();
    };
    
    // 处理图片选择
    const handleImageSelect = (event) => {
      const file = event.target.files[0];
      if (!file) return;
      
      // 验证文件类型
      if (!file.type.startsWith('image/')) {
        ElMessage.warning('请选择图片文件');
        return;
      }
      
      // 验证文件大小（限制5MB）
      if (file.size > 5 * 1024 * 1024) {
        ElMessage.warning('图片大小不能超过5MB');
        return;
      }
      
      selectedImage.value = file;
      
      // 生成预览URL
      const reader = new FileReader();
      reader.onload = (e) => {
        imagePreviewUrl.value = e.target.result;
      };
      reader.readAsDataURL(file);
    };
    
    // 取消图片
    const cancelImage = () => {
      selectedImage.value = null;
      imagePreviewUrl.value = '';
      if (imageInput.value) {
        imageInput.value.value = '';
      }
    };
    
    // 预览图片
    const previewImage = (imageUrl) => {
      window.open(imageUrl, '_blank');
    };
    
    // 插入换行
    const insertNewline = (event) => {
      const textarea = event.target;
      const start = textarea.selectionStart;
      const end = textarea.selectionEnd;
      const value = textarea.value;
      
      messageInput.value = value.substring(0, start) + '\n' + value.substring(end);
      
      nextTick(() => {
        textarea.selectionStart = textarea.selectionEnd = start + 1;
      });
    };
    
    // 滚动到底部
    const scrollToBottom = () => {
      if (messagesContainer.value) {
        messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight;
      }
    };
    
    // 处理滚动事件（加载更多）
    const handleScroll = async (event) => {
      const container = event.target;
      
      // 当滚动到顶部附近时加载更多
      if (container.scrollTop < 50 && hasMore.value && !loadingMore.value) {
        await loadMoreMessages();
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
        return '昨天 ' + date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' });
      }
      
      // 更早
      return date.toLocaleDateString('zh-CN', { month: '2-digit', day: '2-digit' }) + ' ' + 
             date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' });
    };
    
    // 清空聊天记录
    const clearChatHistory = async () => {
      try {
        await ElMessageBox.confirm('确定要清空聊天记录吗？此操作不可恢复。', '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        });
        
        await clearPrivateChatHistoryApi(props.friendUserId);
        ElMessage.success('✅ 聊天记录已清空');
        messages.value = [];
        showMoreOptions.value = false;
      } catch (error) {
        if (error !== 'cancel') {
          console.error('Clear chat history error:', error);
          ElMessage.error('清空聊天记录失败');
        }
      }
    };
    
    // 删除好友
    const deleteFriend = async () => {
      try {
        await ElMessageBox.confirm(`确定要删除好友 "${friendInfo.value?.remark || friendInfo.value?.username}" 吗？`, '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        });
        
        await deleteFriendApi(props.friendUserId);
        ElMessage.success('✅ 好友已删除');
        router.push('/app/friends');
      } catch (error) {
        if (error !== 'cancel') {
          console.error('Delete friend error:', error);
          ElMessage.error('删除好友失败');
        }
      }
    };
    
    // 监听路由参数变化
    watch(() => props.friendUserId, async (newId) => {
      if (newId) {
        await Promise.all([
          loadFriendInfo(),
          loadMessages()
        ]);
        
        // 标记已读
        if (isConnected.value && messages.value.length > 0) {
          const lastMessage = messages.value[messages.value.length - 1];
          markRead({
            friendUserId: newId,
            upToMessageId: lastMessage.id
          });
        }
      }
    }, { immediate: true });
    
    // 定义消息处理器（在 onMounted 外部，以便 onUnmounted 能访问）
    const handlePrivateMessage = (message) => {
      console.log('📨 收到WebSocket新消息:');
      console.log('  消息对象:', message);
      console.log('  消息类型:', message.type);
      console.log('  消息ID:', message.messageId);
      console.log('  发送者ID:', message.senderId);
      console.log('  接收者ID:', message.receiverId);
      console.log('  消息内容:', message.content);
      console.log('  当前聊天好友ID:', props.friendUserId);
      console.log('  当前消息列表数量:', messages.value.length);
      
      // 检查是否是当前聊天的消息（通过senderId或receiverId匹配）
      const isCurrentChat = String(message.senderId) === String(props.friendUserId) || 
                           String(message.receiverId) === String(props.friendUserId);
      
      console.log('  是否是当前聊天的消息:', isCurrentChat);
      
      if (isCurrentChat) {
        console.log('✅ 消息属于当前聊天窗口，准备处理');
        
        // 检查消息是否已经存在（避免重复添加）
        const exists = messages.value.some(m => String(m.id) === String(message.messageId));
        console.log('  消息是否已存在（通过ID）:', exists);
        
        if (!exists) {
          // 转换为前端消息格式
          const newMessage = {
            id: message.messageId,
            senderId: message.senderId,
            receiverId: message.receiverId,
            senderName: message.senderName,
            senderAvatar: message.senderAvatar,
            content: message.content,
            messageType: message.messageType,
            createTime: message.createTime,
            isTemp: false
          };
          
          console.log('  转换后的消息格式:', newMessage);
          
          // 如果存在临时消息（乐观更新），替换为真实消息
          const tempIndex = messages.value.findIndex(m => {
            const match = m.isTemp && 
              String(m.senderId) === String(message.senderId) && 
              m.content === message.content;
            if (m.isTemp) {
              console.log('  检查临时消息:', {
                id: m.id,
                isTemp: m.isTemp,
                senderId: m.senderId,
                content: m.content,
                匹配: match
              });
            }
            return match;
          });
          
          console.log('  找到临时消息索引:', tempIndex);
          
          if (tempIndex !== -1) {
            // 替换临时消息为真实消息
            console.log('🔄 替换临时消息为真实消息');
            console.log('  临时消息:', messages.value[tempIndex]);
            console.log('  真实消息:', newMessage);
            messages.value.splice(tempIndex, 1, newMessage);
          } else {
            // 直接添加新消息
            console.log('➕ 添加新消息到列表');
            messages.value.push(newMessage);
          }
          
          console.log('  ✅ 处理完成，当前消息数量:', messages.value.length);
          scrollToBottom();
          
          // 标记已读（如果是别人发的消息）
          if (String(message.senderId) !== String(currentUserId.value) && isConnected.value) {
            console.log('👤 标记消息为已读');
            markRead({
              friendUserId: props.friendUserId,
              upToMessageId: message.messageId
            });
          }
        } else {
          console.log('⚠️ 消息已存在，跳过处理');
        }
      } else {
        console.log('ℹ️ 消息不属于当前聊天窗口，显示通知');
        // 其他好友的消息，显示通知
        ElNotification({
          title: `新消息 - ${message.senderName}`,
          message: message.content,
          type: 'info',
          duration: 3000
        });
      }
    };
    
    onMounted(async () => {
      console.log('🚀 PrivateChat 组件已挂载');
      console.log('  当前路由:', route.path);
      console.log('  好友ID:', props.friendUserId);
      
      // 连接WebSocket
      console.log('📡 正在连接WebSocket...');
      console.log('  isConnected:', isConnected.value);
      connect();
      
      // 监听私聊消息
      console.log('👂 正在注册私聊消息监听器...');
      on(WS_MESSAGE_TYPES.PRIVATE_MESSAGE, handlePrivateMessage);
      console.log('  ✅ 私聊消息监听器已注册');
    });
    
    onUnmounted(() => {
      // 移除消息处理器
      off(WS_MESSAGE_TYPES.PRIVATE_MESSAGE, handlePrivateMessage);
      
      disconnect();
    });
    
    return {
      messages,
      friendInfo,
      loading,
      loadingMore,
      hasMore,
      messageInput,
      selectedImage,
      imagePreviewUrl,
      showMoreOptions,
      messagesContainer,
      imageInput,
      currentUserId,
      sending,
      goBack,
      getStatusClass,
      getStatusText,
      sendMessage,
      triggerImageUpload,
      handleImageSelect,
      cancelImage,
      previewImage,
      insertNewline,
      handleScroll,
      formatTime,
      clearChatHistory,
      deleteFriend
    };
  }
};
</script>

<style scoped>
.private-chat {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: #f5f7fa;
}

.chat-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 24px;
  background: white;
  border-bottom: 1px solid #eee;
}

.header-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.back-btn {
  background: none;
  border: none;
  font-size: 18px;
  color: #666;
  cursor: pointer;
  padding: 8px;
  border-radius: 4px;
  transition: all 0.3s;
}

.back-btn:hover {
  background: #f5f7fa;
  color: #409eff;
}

.friend-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  object-fit: cover;
}

.friend-details h3 {
  margin: 0 0 4px 0;
  color: #333;
  font-size: 16px;
}

.status-text {
  margin: 0;
  font-size: 12px;
  color: #999;
  display: flex;
  align-items: center;
  gap: 4px;
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  display: inline-block;
}

.status-dot.online {
  background: #67c23a;
}

.status-dot.away {
  background: #e6a23c;
}

.status-dot.busy {
  background: #f56c6c;
}

.status-dot.offline {
  background: #dcdfe6;
}

.more-btn {
  background: none;
  border: none;
  font-size: 18px;
  color: #666;
  cursor: pointer;
  padding: 8px;
  border-radius: 4px;
  transition: all 0.3s;
}

.more-btn:hover {
  background: #f5f7fa;
  color: #409eff;
}

.more-options-panel {
  position: absolute;
  top: 60px;
  right: 24px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.15);
  z-index: 100;
  overflow: hidden;
}

.option-item {
  padding: 12px 20px;
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
  transition: background 0.3s;
  color: #333;
}

.option-item:hover {
  background: #f5f7fa;
}

.option-item.danger {
  color: #f56c6c;
}

.option-item i {
  width: 16px;
}

.messages-section {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.messages-container {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 16px;
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
}

.loading-more i {
  font-size: 16px;
}

.message-item {
  display: flex;
  flex-direction: row;
  align-items: flex-end;
  gap: 8px;
  margin-bottom: 16px;
}

/* 自己的消息靠右对齐 */
.message-item.message-own {
  justify-content: flex-end;
}

.message-sender {
  flex-shrink: 0;
}

.message-sender-own {
  order: 2; /* 自己的头像通过order放在最后（右边） */
}

.sender-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  object-fit: cover;
}

.message-content {
  display: flex;
  flex-direction: column;
  gap: 4px;
  max-width: 70%;
}

/* 自己的消息内容靠右 */
.message-item.message-own .message-content {
  align-items: flex-end;
  order: 1;
}

.message-bubble {
  padding: 12px 16px;
  border-radius: 12px;
  background: white;
  box-shadow: 0 2px 8px rgba(0,0,0,0.08);
  word-wrap: break-word;
}

.message-bubble.bubble-own {
  background: #409eff;
  color: white;
}

.text-message {
  line-height: 1.6;
  font-size: 14px;
}

.image-message {
  max-width: 300px;
}

.message-image {
  width: 100%;
  border-radius: 8px;
  cursor: pointer;
  transition: opacity 0.3s;
}

.message-image:hover {
  opacity: 0.8;
}

.file-message {
  display: flex;
  align-items: center;
  gap: 8px;
}

.download-link {
  color: inherit;
  text-decoration: none;
}

.message-time {
  font-size: 12px;
  color: #999;
}

.message-item.message-own .message-time {
  text-align: right;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  color: #999;
}

.empty-state i {
  font-size: 48px;
  margin-bottom: 16px;
}

.input-section {
  background: white;
  border-top: 1px solid #eee;
  padding: 16px;
}

.input-tools {
  display: flex;
  gap: 8px;
  margin-bottom: 8px;
}

.tool-btn {
  background: none;
  border: none;
  font-size: 18px;
  color: #666;
  cursor: pointer;
  padding: 8px;
  border-radius: 4px;
  transition: all 0.3s;
}

.tool-btn:hover {
  background: #f5f7fa;
  color: #409eff;
}

.input-area {
  display: flex;
  gap: 12px;
  align-items: flex-end;
}

.input-area textarea {
  flex: 1;
  padding: 12px;
  border: 1px solid #dcdfe6;
  border-radius: 8px;
  resize: none;
  font-family: inherit;
  font-size: 14px;
  line-height: 1.6;
  transition: border-color 0.3s;
}

.input-area textarea:focus {
  outline: none;
  border-color: #409eff;
}

.send-btn {
  background: #409eff;
  color: white;
  border: none;
  border-radius: 8px;
  padding: 12px 16px;
  cursor: pointer;
  font-size: 16px;
  transition: all 0.3s;
}

.send-btn:hover:not(:disabled) {
  background: #66b1ff;
}

.send-btn:disabled {
  background: #dcdfe6;
  cursor: not-allowed;
}

.image-preview {
  margin-top: 12px;
  position: relative;
  display: inline-block;
}

.image-preview img {
  max-width: 200px;
  max-height: 150px;
  border-radius: 8px;
  border: 1px solid #dcdfe6;
}

.cancel-btn {
  position: absolute;
  top: -8px;
  right: -8px;
  background: #f56c6c;
  color: white;
  border: none;
  border-radius: 50%;
  width: 24px;
  height: 24px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
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
