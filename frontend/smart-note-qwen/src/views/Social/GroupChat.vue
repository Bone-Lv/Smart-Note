<template>
  <div class="group-chat">
    <div class="chat-header">
      <h2>群聊</h2>
      <div class="header-actions">
        <button @click="showCreateGroupModal = true" class="create-group-btn">
          <i class="fas fa-plus"></i>
          创建群聊
        </button>
        <button @click="loadGroups" class="refresh-btn">
          <i class="fas fa-sync-alt"></i>
        </button>
      </div>
    </div>
    
    <div class="chat-content">
      <!-- 群聊列表 -->
      <div class="groups-section">
        <div class="section-header">
          <h3>我的群聊</h3>
          <span class="group-count">{{ myGroups.length }}</span>
        </div>
        
        <div class="groups-list">
          <div 
            v-for="group in myGroups" 
            :key="group.id" 
            class="group-item"
            :class="{ active: selectedGroupId === group.id }"
            @click="selectGroup(group)"
          >
            <div class="group-info">
              <div class="group-name">{{ group.groupName }}</div>
              <div class="group-preview">{{ group.lastMessage || '暂无消息' }}</div>
            </div>
            <div class="group-meta">
              <span class="member-count">{{ group.memberCount || 0 }}人</span>
            </div>
          </div>
          
          <div v-if="myGroups.length === 0 && !loading" class="empty-groups">
            <i class="fas fa-comments"></i>
            <p>暂无群聊</p>
            <button @click="showCreateGroupModal = true" class="create-first-btn">
              创建群聊
            </button>
          </div>
        </div>
      </div>
      
      <!-- 群聊详情 -->
      <div v-if="selectedGroup" class="group-detail-section">
        <div class="group-detail-header">
          <div class="group-header-info">
            <div class="group-header-text">
              <h3>{{ selectedGroup.groupName }}</h3>
              <p>{{ selectedGroup.memberCount }}人在线</p>
            </div>
          </div>
          
          <div class="group-actions">
            <button @click="showGroupMembers = !showGroupMembers" class="members-btn">
              <i class="fas fa-users"></i>
              成员
            </button>
            <button @click="showGroupSettings = true" class="settings-btn">
              <i class="fas fa-cog"></i>
            </button>
          </div>
        </div>
        
        <!-- 群聊消息区域 -->
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
              :class="{ 'message-own': String(message.senderId) === String(userId) }"
            >
              <!-- 头像 -->
              <div class="message-sender" :class="{ 'message-sender-own': String(message.senderId) === String(userId) }">
                <img 
                  :src="message.senderAvatar || (String(message.senderId) === String(userId) ? userStore.userInfo?.avatar : null) || getDefaultAvatar()" 
                  :alt="message.senderUsername" 
                  class="sender-avatar"
                  @error="handleAvatarError"
                >
              </div>
              
              <!-- 消息内容 -->
              <div class="message-content">
                <!-- 群聊显示发送者名字 -->
                <div v-if="String(message.senderId) !== String(userId)" class="sender-name-group">
                  <span class="sender-name">{{ message.senderUsername }}</span>
                </div>
                
                <div 
                  class="message-bubble" 
                  :class="{ 'bubble-own': String(message.senderId) === String(userId) }"
                >
                  <div v-if="message.messageType === 1" class="text-message">
                    {{ message.content }}
                  </div>
                  <div v-else-if="message.messageType === 2" class="image-message">
                    <img :src="message.content" alt="image" class="message-image">
                  </div>
                  <div v-else class="file-message">
                    <i class="fas fa-file"></i>
                    <span>{{ message.content }}</span>
                  </div>
                </div>
                <div class="message-time">{{ formatTime(message.createTime) }}</div>
              </div>
            </div>
          </div>
          
          <!-- 消息输入区域 -->
          <div class="message-input-section">
            <div class="input-tools">
              <button @click="showEmojiPicker = !showEmojiPicker" class="tool-btn">
                <i class="fas fa-smile"></i>
              </button>
              <button @click="uploadImage" class="tool-btn">
                <i class="fas fa-image"></i>
              </button>
              <button @click="uploadFile" class="tool-btn">
                <i class="fas fa-paperclip"></i>
              </button>
            </div>
            
            <textarea 
              v-model="messageInput" 
              class="message-input" 
              placeholder="输入消息..."
              @keydown.enter.exact.prevent="sendMessage"
              @keydown.shift.enter="insertNewline"
            ></textarea>
            
            <button @click="sendMessage" class="send-btn" :disabled="!messageInput.trim()">
              <i class="fas fa-paper-plane"></i>
            </button>
          </div>
        </div>
      </div>
      
      <!-- 群成员侧边栏 -->
      <div v-if="showGroupMembers && selectedGroup" class="group-members-section">
        <div class="members-header">
          <h4>群成员 ({{ groupMembers.length }})</h4>
          <button @click="showGroupMembers = false" class="close-btn">×</button>
        </div>
        
        <div class="members-list">
          <div 
            v-for="member in groupMembers" 
            :key="member.userId"
            class="member-item"
          >
            <div class="member-info">
              <h5>{{ member.username }}</h5>
              <p v-if="member.role">{{ getRoleText(member.role) }}</p>
            </div>
            <div v-if="String(member.userId) !== String(userId)" class="member-actions">
              <button @click="startPrivateChat(member)" class="chat-btn">
                <i class="fas fa-comment"></i>
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
    
    <!-- 创建群聊模态框 -->
    <div v-if="showCreateGroupModal" class="modal-overlay" @click="showCreateGroupModal = false">
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <h3>创建群聊</h3>
          <button @click="showCreateGroupModal = false" class="close-btn">×</button>
        </div>
        
        <form @submit.prevent="createNewGroup" class="modal-form">
          <div class="form-group">
            <label for="groupName">群聊名称</label>
            <input 
              id="groupName" 
              v-model="newGroup.groupName" 
              type="text" 
              placeholder="请输入群聊名称"
              maxlength="10"
              required
            >
          </div>
          
          <div class="form-group">
            <label>选择成员</label>
            <div class="friends-selection">
              <div 
                v-for="friend in allFriends" 
                :key="friend.friendUserId"
                class="friend-checkbox"
              >
                <label class="checkbox-label">
                  <input 
                    type="checkbox" 
                    :value="friend.friendUserId"
                    v-model="newGroup.memberIds"
                  >
                  <span>{{ friend.remark || friend.friendUsername }}</span>
                </label>
              </div>
            </div>
          </div>
          
          <div class="form-actions">
            <button type="submit" :disabled="creatingGroup">
              <span v-if="!creatingGroup">创建</span>
              <span v-else>创建中...</span>
            </button>
            <button type="button" @click="showCreateGroupModal = false">取消</button>
          </div>
        </form>
      </div>
    </div>
    
    <!-- 重命名群聊模态框 -->
    <div v-if="showRenameModal" class="modal-overlay sub-modal" @click="showRenameModal = false">
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <h3>重命名群聊</h3>
          <button @click="showRenameModal = false" class="close-btn">×</button>
        </div>
        
        <form @submit.prevent="renameGroup" class="modal-form">
          <div class="form-group">
            <label>新群名称</label>
            <input 
              v-model="newGroupName" 
              type="text" 
              placeholder="请输入新群名称"
              maxlength="20"
              required
            >
          </div>
          
          <div class="form-actions">
            <button type="submit">确定</button>
            <button type="button" @click="showRenameModal = false">取消</button>
          </div>
        </form>
      </div>
    </div>
    
    <!-- 转让群主模态框 -->
    <div v-if="showTransferModal" class="modal-overlay sub-modal" @click="showTransferModal = false">
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <h3>转让群主</h3>
          <button @click="showTransferModal = false" class="close-btn">×</button>
        </div>
        
        <div class="modal-body">
          <p class="warning-text">⚠️ 转让后您将成为普通成员，此操作不可撤销</p>
          
          <div class="form-group">
            <label>选择新群主</label>
            <select v-model="transferTargetUserId" required>
              <option value="">请选择成员</option>
              <option 
                v-for="member in groupMembers.filter(m => String(m.userId) !== String(userId))"
                :key="member.userId"
                :value="member.userId"
              >
                {{ member.username }} ({{ getRoleText(member.role) }})
              </option>
            </select>
          </div>
          
          <div class="form-actions">
            <button @click="transferOwner" :disabled="!transferTargetUserId">确定转让</button>
            <button type="button" @click="showTransferModal = false">取消</button>
          </div>
        </div>
      </div>
    </div>
    
    <!-- 群聊设置模态框 -->
    <div v-if="showGroupSettings" class="modal-overlay" @click="showGroupSettings = false">
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <h3>群聊设置</h3>
          <button @click="showGroupSettings = false" class="close-btn">×</button>
        </div>
        
        <div class="modal-body">
          <div class="setting-item">
            <h4>群聊信息</h4>
            <div class="group-info">
              <div class="group-basic-info">
                <p><strong>群名称:</strong> {{ selectedGroup.groupName }}</p>
                <p><strong>群主:</strong> {{ selectedGroup.ownerUsername }}</p>
                <p><strong>成员数:</strong> {{ selectedGroup.memberCount }}</p>
              </div>
              <div v-if="isGroupOwner" class="owner-actions">
                <button @click="showRenameModal = true" class="action-btn">
                  <i class="fas fa-edit"></i> 重命名
                </button>
                <button @click="showTransferModal = true" class="action-btn">
                  <i class="fas fa-exchange-alt"></i> 转让群主
                </button>
              </div>
            </div>
          </div>
          
          <div class="setting-item">
            <h4>成员管理</h4>
            <div class="members-management">
              <div 
                v-for="member in groupMembers" 
                :key="member.userId"
                class="member-management-item"
              >
                <div class="member-basic">
                  <span class="member-name">{{ member.username }}</span>
                  <span class="member-role">{{ getRoleText(member.role) }}</span>
                </div>
                <div v-if="isGroupOwner && String(member.userId) !== String(userId)" class="member-actions">
                  <button 
                    v-if="member.role !== 2" 
                    @click="toggleAdmin(member)"
                    class="small-btn"
                  >
                    {{ member.role === 1 ? '取消管理员' : '设为管理员' }}
                  </button>
                  <button 
                    v-if="member.role === 0"
                    @click="removeMember(member)"
                    class="small-btn danger"
                  >
                    移除
                  </button>
                </div>
              </div>
            </div>
          </div>
          
          <div class="setting-item">
            <h4>群聊操作</h4>
            <div class="setting-actions">
              <button @click="clearGroupChat" class="danger-btn">
                <i class="fas fa-trash"></i>
                清空聊天记录
              </button>
              <button v-if="!isGroupOwner" @click="exitGroup" class="danger-btn">
                <i class="fas fa-sign-out-alt"></i>
                退出群聊
              </button>
              <button v-if="isGroupOwner" @click="disbandGroup" class="danger-btn">
                <i class="fas fa-bomb"></i>
                解散群聊
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick, watch } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { ElMessage, ElMessageBox, ElNotification } from 'element-plus';
import { useUserStore } from '../../stores/userStore.js';
import { useFriendStore } from '../../stores/friendStore.js';
import { useWebSocket, WS_MESSAGE_TYPES } from '../../hooks/useWebSocket.js';
import { 
  getMyGroupsApi, 
  getGroupMessageHistoryApi, 
  getGroupDetailApi,
  getGroupMembersApi,
  renameGroupApi,
  transferGroupOwnerApi,
  setGroupAdminApi,
  removeGroupMemberApi,
  createGroupApi,
  sendGroupMessageApi,
  leaveGroupApi,
  clearGroupChatHistoryApi as clearGroupChatApi,
  markAllGroupMessagesAsReadApi,
  disbandGroupApi
} from '../../api/social.js';

const router = useRouter();
const route = useRoute();
const userStore = useUserStore();
const friendStore = useFriendStore();
const { connect, disconnect, on, off, isConnected, markRead } = useWebSocket();

// 数据
const myGroups = ref([]);
const selectedGroup = ref(null);
const selectedGroupId = ref(null);
const messages = ref([]);
const groupMembers = ref([]);
const messageInput = ref('');
const loading = ref(false);
const loadingMore = ref(false);
const creatingGroup = ref(false);
const hasMore = ref(true);
const currentCursor = ref(null);
const showCreateGroupModal = ref(false);
const showGroupSettings = ref(false);
const showGroupMembers = ref(false);
const showRenameModal = ref(false);
const showTransferModal = ref(false);
const newGroupName = ref('');
const transferTargetUserId = ref(null);
const showEmojiPicker = ref(false);
const messagesContainer = ref(null);

// 所有好友
const allFriends = computed(() => friendStore.friends || []);

// 新群聊数据
const newGroup = ref({
  groupName: '',
  memberIds: []
});

// 计算属性
const userId = computed(() => userStore.userInfo?.id || userStore.userInfo?.userId);
const isGroupOwner = computed(() => selectedGroup.value?.ownerId === userId.value);

// 处理群聊解散消息
const handleGroupDisbanded = (message) => {
  console.log('📢 收到群聊解散通知:', message);
  
  const groupId = message.groupId;
  const groupName = message.groupName || '该群聊';
  
  // 显示通知
  ElNotification({
    title: '群聊解散',
    message: `群聊"${groupName}"已被群主解散`,
    type: 'warning',
    duration: 5000
  });
  
  // 如果当前正在查看该群聊，清空状态
  if (String(selectedGroupId.value) === String(groupId)) {
    selectedGroup.value = null;
    selectedGroupId.value = null;
    messages.value = [];
    groupMembers.value = [];
    showGroupSettings.value = false;
    showGroupMembers.value = false;
  }
  
  // 从群聊列表中移除该群
  myGroups.value = myGroups.value.filter(g => String(g.id) !== String(groupId));
  
  ElMessage.warning(`群聊"${groupName}"已解散`);
};

// 加载群聊列表
const loadGroups = async () => {
  loading.value = true;
  
  try {
    const response = await getMyGroupsApi();
    myGroups.value = response.data.data || [];
  } catch (error) {
    console.error('Load groups error:', error);
    ElMessage.error('加载群聊列表失败');
  } finally {
    loading.value = false;
  }
};

// 选择群聊
const selectGroup = async (group) => {
  selectedGroup.value = group;
  selectedGroupId.value = group.id;
  messages.value = [];
  currentCursor.value = null;
  hasMore.value = true;
  
  await Promise.all([
    loadGroupMessages(),
    loadGroupMembers()
  ]);
};

// 加载群聊消息
const loadGroupMessages = async (isLoadMore = false) => {
  if (!selectedGroupId.value) return;
  
  console.log('📥 加载群聊消息');
  console.log('  群聊ID:', selectedGroupId.value);
  console.log('  是否加载更多:', isLoadMore);
  console.log('  当前游标:', currentCursor.value);
  
  if (isLoadMore) {
    if (!hasMore.value || loadingMore.value) {
      console.log('  ⚠️ 停止加载: hasMore=', hasMore.value, 'loadingMore=', loadingMore.value);
      return;
    }
    loadingMore.value = true;
  } else {
    loading.value = true;
    currentCursor.value = null;
    hasMore.value = true;
  }
  
  try {
    const params = {
      groupId: selectedGroupId.value,
      cursor: isLoadMore ? currentCursor.value : null,
      pageSize: 20
    };
    
    console.log('  📡 请求参数:', params);
    const response = await getGroupMessageHistoryApi(params);
    console.log('  ✅ API响应:', response);
    
    // 添加空值校验
    const data = response.data.data;
    if (!data) {
      console.log('  ⚠️ 响应数据为空');
      messages.value = [];
      return;
    }
    
    const newMessages = data.records || [];
    console.log('  📝 原始消息数量:', newMessages.length);
    
    // 字段映射：确保所有必要字段都存在
    const mappedMessages = newMessages.map(msg => ({
      id: msg.id,
      groupId: msg.groupId,
      senderId: msg.senderId,
      senderUsername: msg.senderUsername,
      senderAvatar: msg.senderAvatar || null,
      messageType: msg.messageType,
      content: msg.content,
      createTime: msg.createTime
    }));
    
    if (isLoadMore) {
      // 加载更多时，将新消息插入到前面
      console.log('  ➕ 插入消息到列表头部');
      messages.value = [...mappedMessages, ...messages.value];
    } else {
      // 首次加载时，替换所有消息
      console.log('  🔄 替换整个消息列表');
      messages.value = mappedMessages;
    }
    
    // 更新游标
    currentCursor.value = data.nextCursor;
    hasMore.value = data.hasMore;
    
    console.log('  📊 状态更新: nextCursor=', currentCursor.value, 'hasMore=', hasMore.value);
    console.log('  ✅ 消息加载完成，当前消息总数:', messages.value.length);
    
    // 滚动处理
    await nextTick();
    if (isLoadMore) {
      // 加载更多时保持滚动位置
      const container = document.querySelector('.messages-container');
      if (container) {
        console.log('  📜 保持滚动位置 (估算偏移):', newMessages.length * 60);
        container.scrollTop = newMessages.length * 60; // 估算高度
      }
    } else {
      // 首次加载时滚动到底部
      console.log('  📜 滚动到底部');
      scrollToBottom();
      
      // 标记已读（只标记别人的消息）
      if (!isLoadMore && messages.value.length > 0 && isConnected.value) {
        console.log('  👁️ 标记群聊已读');
        // 通过WebSocket标记整个群聊已读（新协议只需要groupId）
        markRead({
          groupId: selectedGroupId.value
        });
      }
    }
  } catch (error) {
    console.error('❌ 加载群聊消息失败:', error);
    console.error('  错误详情:', error.response || error.message);
    ElMessage.error('加载群聊消息失败');
  } finally {
    loading.value = false;
    loadingMore.value = false;
  }
};

// 加载更多消息
const loadMoreMessages = async () => {
  await loadGroupMessages(true);
};

// 滚动到底部
const scrollToBottom = () => {
  if (messagesContainer.value) {
    messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight;
  }
};

// 处理滚动事件
const handleScroll = (event) => {
  const container = event.target;
  if (container.scrollTop === 0 && hasMore.value && !loadingMore.value) {
    loadMoreMessages();
  }
};

// 加载群成员
const loadGroupMembers = async () => {
  if (!selectedGroupId.value) return;
  
  try {
    const response = await getGroupMembersApi(selectedGroupId.value);
    
    // 后端返回的是数组，不是嵌套对象
    const members = response.data.data || [];
    
    // 映射字段，确保前端使用统一的格式
    groupMembers.value = members.map(member => ({
      userId: member.userId,
      username: member.username,
      avatar: member.avatar,
      role: member.role,
      nickname: member.nickname,
      joinTime: member.joinTime
    }));
  } catch (error) {
    console.error('Load group members error:', error);
    ElMessage.error('加载群成员失败');
  }
};

// 发送消息
const sendMessage = async () => {
  console.log('📤 开始发送消息');
  console.log('  输入内容:', messageInput.value);
  console.log('  选中的群聊ID:', selectedGroupId.value);
  console.log('  当前用户ID:', userId.value);
  
  if (!messageInput.value.trim() || !selectedGroupId.value) {
    console.log('⚠️ 消息内容为空或未选择群聊，取消发送');
    return;
  }
  
  const content = messageInput.value.trim();
  
  // 1. 乐观更新：立即将消息添加到列表
  const tempMessage = {
    id: Date.now(), // 临时ID
    groupId: selectedGroupId.value,
    senderId: userId.value,
    senderUsername: userStore.userInfo?.username || '我',
    senderAvatar: userStore.userInfo?.avatar || '',
    content: content,
    messageType: 1,
    createTime: new Date().toISOString(),
    isTemp: true // 标记为临时消息
  };
  
  console.log('📝 创建临时消息:', tempMessage);
  messages.value.push(tempMessage);
  messageInput.value = '';
  scrollToBottom();
  console.log('✅ 临时消息已添加到列表，当前消息数量:', messages.value.length);
  
  try {
    // 2. 发送到后端
    const messageData = {
      groupId: selectedGroupId.value,
      messageType: 1,
      content: content
    };
    
    console.log('📡 正在调用API发送消息...');
    console.log('  请求数据:', messageData);
    const response = await sendGroupMessageApi(messageData);
    console.log('✅ API调用成功，后端响应:', response);
    
    // 3. 后端会通过WebSocket推送消息回来，handleGroupMessage会自动处理
    console.log('⏳ 等待WebSocket推送消息...');
  } catch (error) {
    console.error('❌ 发送消息失败:', error);
    console.error('  错误详情:', error.response || error.message);
    ElMessage.error('发送消息失败');
    
    // 4. 错误回滚：移除临时消息
    const index = messages.value.findIndex(m => m.id === tempMessage.id);
    if (index !== -1) {
      messages.value.splice(index, 1);
      console.log('🔄 已回滚临时消息，当前消息数量:', messages.value.length);
    }
  }
};

// 插入新行
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

// 创建新群聊
const createNewGroup = async () => {
  if (!newGroup.value.groupName.trim()) {
    ElMessage.warning('请输入群聊名称');
    return;
  }
  
  creatingGroup.value = true;
  
  try {
    const requestData = {
      groupName: newGroup.value.groupName,
      memberIds: newGroup.value.memberIds
    };
    
    await createGroupApi(requestData);
    
    ElMessage.success('群聊创建成功');
    showCreateGroupModal.value = false;
    
    // 重置表单
    newGroup.value = {
      groupName: '',
      memberIds: []
    };
    
    // 刷新群聊列表
    await loadGroups();
  } catch (error) {
    console.error('Create group error:', error);
    ElMessage.error('创建群聊失败');
  } finally {
    creatingGroup.value = false;
  }
};

// 退出群聊
const exitGroup = async () => {
  try {
    await ElMessageBox.confirm(
      '确定要退出该群聊吗？',
      '提示',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    );
    
    await leaveGroupApi(selectedGroupId.value);
    ElMessage.success('已退出群聊');
    
    // 清空选择
    selectedGroup.value = null;
    selectedGroupId.value = null;
    messages.value = [];
    groupMembers.value = [];
    
    // 刷新列表
    await loadGroups();
  } catch (error) {
    if (error !== 'cancel') {
      console.error('Exit group error:', error);
      ElMessage.error('退出群聊失败');
    }
  }
};

// 解散群聊（仅群主）
const disbandGroup = async () => {
  try {
    await ElMessageBox.confirm(
      `确定要解散群聊"${selectedGroup.value?.groupName}"吗？\n\n此操作不可撤销，所有成员将被移除，聊天记录将被清空。`,
      '解散群聊',
      {
        confirmButtonText: '确定解散',
        cancelButtonText: '取消',
        type: 'error',
        confirmButtonClass: 'el-button--danger'
      }
    );
    
    await disbandGroupApi(selectedGroupId.value);
    ElMessage.success('群聊已解散');
    
    // 清空选择
    selectedGroup.value = null;
    selectedGroupId.value = null;
    messages.value = [];
    groupMembers.value = [];
    
    // 刷新列表
    await loadGroups();
    
    // 关闭设置面板
    showGroupSettings.value = false;
  } catch (error) {
    if (error !== 'cancel') {
      console.error('Disband group error:', error);
      ElMessage.error('解散群聊失败');
    }
  }
};

// 清空聊天记录
const clearGroupChat = async () => {
  try {
    await ElMessageBox.confirm(
      '确定要清空聊天记录吗？此操作不可恢复。',
      '提示',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    );
    
    await clearGroupChatApi(selectedGroupId.value);
    ElMessage.success('聊天记录已清空');
    messages.value = [];
  } catch (error) {
    if (error !== 'cancel') {
      console.error('Clear chat error:', error);
      ElMessage.error('清空聊天记录失败');
    }
  }
};

// 发起私聊
const startPrivateChat = (member) => {
  router.push(`/app/chat/private/${member.userId}`);
};

// 获取默认头像
const getDefaultAvatar = () => {
  // 返回内联SVG作为默认头像
  return 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iNDAiIGhlaWdodD0iNDAiIHZpZXdCb3g9IjAgMCA0MCA0MCIgZmlsbD0ibm9uZSIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj48cmVjdCB3aWR0aD0iNDAiIGhlaWdodD0iNDAiIHJ4PSIyMCIgZmlsbD0iIzQwOUVGRiIvPjxwYXRoIGQ9Ik0yMCAxOEMyMi4yMDkxIDE4IDI0IDIwLjIwOTEgMjQgMjJWMjhIMTZWMjJDMTYgMjAuMjA5MSAxNy43OTA5IDE4IDIwIDE4WiIgZmlsbD0id2hpdGUiLz48L3N2Zz4=';
};

// 处理头像加载错误
const handleAvatarError = (event) => {
  event.target.src = getDefaultAvatar();
};

// 上传图片
const uploadImage = () => {
  ElMessage.info('图片上传功能开发中...');
};

// 上传文件
const uploadFile = () => {
  ElMessage.info('文件上传功能开发中...');
};

// 获取角色文本
const getRoleText = (role) => {
  const roleMap = {
    0: '成员',
    1: '管理员',
    2: '群主'
  };
  return roleMap[role] || '成员';
};

// 格式化时间
const formatTime = (time) => {
  if (!time) return '';
  const date = new Date(time);
  const now = new Date();
  
  // 今天
  if (date.toDateString() === now.toDateString()) {
    return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' });
  }
  
  // 昨天
  const yesterday = new Date(now);
  yesterday.setDate(yesterday.getDate() - 1);
  if (date.toDateString() === yesterday.toDateString()) {
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
  return date.toLocaleDateString('zh-CN');
};

// ==================== 群管理功能 ====================

// 重命名群聊
const renameGroup = async () => {
  if (!newGroupName.value.trim()) {
    ElMessage.warning('请输入群名称');
    return;
  }
  
  try {
    await ElMessageBox.confirm(
      `确定要将群名称修改为"${newGroupName.value}"吗？`,
      '重命名群聊',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    );
    
    await renameGroupApi(selectedGroupId.value, newGroupName.value.trim());
    
    // 更新本地数据
    selectedGroup.value.groupName = newGroupName.value.trim();
    
    // 重新加载群列表
    await loadGroups();
    
    showRenameModal.value = false;
    newGroupName.value = '';
    ElMessage.success('群名称修改成功');
  } catch (error) {
    if (error === 'cancel' || error === 'close') {
      return;
    }
    console.error('Rename group error:', error);
  }
};

// 转让群主
const transferOwner = async () => {
  if (!transferTargetUserId.value) {
    ElMessage.warning('请选择新群主');
    return;
  }
  
  try {
    const targetMember = groupMembers.value.find(m => String(m.userId) === String(transferTargetUserId.value));
    
    await ElMessageBox.confirm(
      `确定要将群主转让给"${targetMember?.username}"吗？转让后您将成为普通成员，此操作不可撤销。`,
      '转让群主',
      {
        confirmButtonText: '确定转让',
        cancelButtonText: '取消',
        type: 'warning',
        confirmButtonClass: 'el-button--danger'
      }
    );
    
    await transferGroupOwnerApi(selectedGroupId.value, transferTargetUserId.value);
    
    // 重新加载群详情和成员列表
    await loadGroupMembers();
    
    showTransferModal.value = false;
    transferTargetUserId.value = null;
    ElMessage.success('群主转让成功');
  } catch (error) {
    if (error === 'cancel' || error === 'close') {
      return;
    }
    console.error('Transfer owner error:', error);
  }
};

// 设置或取消管理员
const toggleAdmin = async (member) => {
  const isAdmin = member.role === 1;
  const action = isAdmin ? '取消管理员' : '设为管理员';
  
  try {
    await ElMessageBox.confirm(
      `确定要${action}"${member.username}"吗？`,
      action,
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    );
    
    await setGroupAdminApi(selectedGroupId.value, member.userId, !isAdmin);
    
    // 更新本地数据
    member.role = isAdmin ? 0 : 1;
    
    ElMessage.success(`${action}成功`);
  } catch (error) {
    if (error === 'cancel' || error === 'close') {
      return;
    }
    console.error('Toggle admin error:', error);
  }
};

// 移除群成员
const removeMember = async (member) => {
  try {
    await ElMessageBox.confirm(
      `确定要将"${member.username}"移除出群吗？`,
      '移除成员',
      {
        confirmButtonText: '确定移除',
        cancelButtonText: '取消',
        type: 'warning',
        confirmButtonClass: 'el-button--danger'
      }
    );
    
    await removeGroupMemberApi(selectedGroupId.value, member.userId);
    
    // 从本地列表移除
    groupMembers.value = groupMembers.value.filter(m => String(m.userId) !== String(member.userId));
    
    ElMessage.success('成员已移除');
  } catch (error) {
    if (error === 'cancel' || error === 'close') {
      return;
    }
    console.error('Remove member error:', error);
  }
};

// 定义消息处理器
const handleGroupMessage = (message) => {
  console.log('📨 收到WebSocket新消息:');
  console.log('  消息对象:', message);
  console.log('  消息类型:', message.type);
  console.log('  消息ID:', message.messageId);
  console.log('  群聊ID:', message.groupId);
  console.log('  发送者ID:', message.senderId);
  console.log('  发送者名称:', message.senderName);
  console.log('  消息内容:', message.content);
  console.log('  当前选中的群聊ID:', selectedGroupId.value);
  console.log('  当前消息列表数量:', messages.value.length);
  
  // 只处理当前选中的群聊
  if (String(message.groupId) === String(selectedGroupId.value)) {
    console.log('✅ 消息属于当前群聊，准备处理');
    
    // 检查消息是否已存在（避免重复）
    const exists = messages.value.some(m => String(m.id) === String(message.messageId));
    console.log('  消息是否已存在（通过ID）:', exists);
    
    if (exists) {
      console.log('⚠️ 消息已存在，跳过处理');
      return;
    }
    
    // 转换为前端消息格式
    const newMessage = {
      id: message.messageId,
      groupId: message.groupId,
      senderId: message.senderId,
      senderUsername: message.senderName,
      senderAvatar: message.senderAvatar,
      content: message.content,
      messageType: message.messageType,
      createTime: message.createTime,
      isTemp: false
    };
    
    console.log('  转换后的消息格式:', newMessage);
    
    // 如果是自己的消息，查找并替换临时消息
    const isOwnMessage = String(message.senderId) === String(userId.value);
    console.log('  是否是自己的消息:', isOwnMessage);
    
    if (isOwnMessage) {
      // 查找临时消息（通过内容+发送者判断）
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
        // 替换临时消息为正式消息
        console.log('🔄 替换临时消息为正式消息');
        console.log('  临时消息:', messages.value[tempIndex]);
        console.log('  正式消息:', newMessage);
        messages.value.splice(tempIndex, 1, newMessage);
        scrollToBottom();
        console.log('  ✅ 替换完成，当前消息数量:', messages.value.length);
        return;
      } else {
        console.log('  ⚠️ 未找到匹配的临时消息');
      }
    }
    
    // 添加新消息
    console.log('➕ 添加新消息到列表');
    messages.value.push(newMessage);
    console.log('  ✅ 添加完成，当前消息数量:', messages.value.length);
    scrollToBottom();
    
    // 标记已读（只标记别人的消息）
    if (!isOwnMessage && isConnected.value) {
      console.log('👤 这是别人的消息，标记已读');
      markRead({
        groupId: selectedGroupId.value
      });
    } else {
      console.log('  ️ 跳过标记已读（自己的消息或WebSocket未连接）');
    }
  } else {
    console.log('ℹ️ 消息属于其他群聊，更新未读数');
    const group = myGroups.value.find(g => g.id === message.groupId);
    console.log('  找到群聊:', group ? group.groupName : '未找到');
    if (group) {
      group.unreadCount = (group.unreadCount || 0) + 1;
      console.log('  未读数:', group.unreadCount);
    }
  }
};

// 组件挂载时加载群聊列表
onMounted(async () => {
  console.log('🚀 GroupChat 组件已挂载');
  console.log('  当前路由:', route.path);
  console.log('  路由参数:', route.params);
  
  await friendStore.refreshAll();
  console.log('  ✅ 好友列表已加载');
  
  await loadGroups();
  console.log('  ✅ 群聊列表已加载，数量:', myGroups.value.length);
  
  // 连接WebSocket
  console.log('📡 正在连接WebSocket...');
  console.log('  isConnected:', isConnected.value);
  connect();
  
  // 监听群聊消息
  console.log('👂 正在注册群聊消息监听器...');
  on(WS_MESSAGE_TYPES.GROUP_MESSAGE, handleGroupMessage);
  console.log('  ✅ 群聊消息监听器已注册');
  
  // 监听群聊解散消息
  console.log('👂 正在注册群聊解散监听器...');
  on(WS_MESSAGE_TYPES.GROUP_DISBANDED, handleGroupDisbanded);
  console.log('  ✅ 群聊解散监听器已注册');
  
  // 保存处理器引用
  window._groupMessageHandler = handleGroupMessage;
  window._groupDisbandedHandler = handleGroupDisbanded;
});

// 组件卸载时清理 WebSocket 监听器
onUnmounted(() => {
  if (window._groupMessageHandler) {
    off(WS_MESSAGE_TYPES.GROUP_MESSAGE, window._groupMessageHandler);
    delete window._groupMessageHandler;
  }
  
  if (window._groupDisbandedHandler) {
    off(WS_MESSAGE_TYPES.GROUP_DISBANDED, window._groupDisbandedHandler);
    delete window._groupDisbandedHandler;
  }
  
  disconnect();
});

</script>

<style scoped>
.group-chat {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f5f7fa;
}

.chat-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px;
  background: white;
  border-bottom: 1px solid #e4e7ed;
}

.chat-header h2 {
  margin: 0;
  font-size: 20px;
  color: #303133;
}

.header-actions {
  display: flex;
  gap: 10px;
}

.create-group-btn, .refresh-btn {
  padding: 8px 16px;
  border: none;
  border-radius: 4px;
  background: #409eff;
  color: white;
  cursor: pointer;
  font-size: 14px;
  transition: background 0.3s;
}

.create-group-btn:hover {
  background: #66b1ff;
}

.refresh-btn {
  background: #909399;
}

.refresh-btn:hover {
  background: #a6a9ad;
}

.chat-content {
  flex: 1;
  display: flex;
  overflow: hidden;
}

.groups-section {
  width: 300px;
  background: white;
  border-right: 1px solid #e4e7ed;
  display: flex;
  flex-direction: column;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 15px;
  border-bottom: 1px solid #e4e7ed;
}

.section-header h3 {
  margin: 0;
  font-size: 16px;
  color: #303133;
}

.group-count {
  font-size: 14px;
  color: #909399;
}

.groups-list {
  flex: 1;
  overflow-y: auto;
}

.group-item {
  padding: 15px;
  cursor: pointer;
  transition: background 0.3s;
  border-bottom: 1px solid #f0f0f0;
}

.group-item:hover {
  background: #f5f7fa;
}

.group-item.active {
  background: #ecf5ff;
}

.group-info {
  margin-bottom: 5px;
}

.group-name {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 4px;
}

.group-preview {
  font-size: 12px;
  color: #909399;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.group-meta {
  display: flex;
  justify-content: flex-end;
  align-items: center;
}

.member-count {
  font-size: 12px;
  color: #c0c4cc;
}

.empty-groups {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px;
  color: #909399;
}

.empty-groups i {
  font-size: 48px;
  margin-bottom: 16px;
}

.create-first-btn {
  margin-top: 16px;
  padding: 8px 20px;
  border: none;
  border-radius: 4px;
  background: #409eff;
  color: white;
  cursor: pointer;
  font-size: 14px;
}

.group-detail-section {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.group-detail-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 15px 20px;
  background: white;
  border-bottom: 1px solid #e4e7ed;
}

.group-header-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.group-header-text h3 {
  margin: 0;
  font-size: 18px;
  color: #303133;
}

.group-header-text p {
  margin: 4px 0 0;
  font-size: 13px;
  color: #909399;
}

.group-actions {
  display: flex;
  gap: 10px;
}

.members-btn, .settings-btn {
  padding: 6px 12px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  background: white;
  color: #606266;
  cursor: pointer;
  font-size: 13px;
  transition: all 0.3s;
}

.members-btn:hover, .settings-btn:hover {
  border-color: #409eff;
  color: #409eff;
}

.messages-section {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: #f5f7fa;
}

.messages-container {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
}

.loading-more, .no-more-messages {
  text-align: center;
  padding: 10px;
  color: #909399;
  font-size: 13px;
}

.loading-more i {
  margin-right: 8px;
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

/* 群聊发送者名称 */
.sender-name-group {
  margin-bottom: 4px;
}

.sender-name {
  font-size: 13px;
  color: #909399;
  font-weight: 500;
}

.message-time {
  font-size: 12px;
  color: #c0c4cc;
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

.message-input-section {
  padding: 15px 20px;
  background: white;
  border-top: 1px solid #e4e7ed;
}

.input-tools {
  display: flex;
  gap: 10px;
  margin-bottom: 10px;
}

.tool-btn {
  padding: 6px 10px;
  border: none;
  border-radius: 4px;
  background: #f5f7fa;
  color: #606266;
  cursor: pointer;
  font-size: 16px;
  transition: background 0.3s;
}

.tool-btn:hover {
  background: #e4e7ed;
}

.message-input {
  width: 100%;
  min-height: 80px;
  max-height: 150px;
  padding: 10px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  font-size: 14px;
  resize: vertical;
  font-family: inherit;
}

.message-input:focus {
  outline: none;
  border-color: #409eff;
}

.send-btn {
  margin-top: 10px;
  padding: 8px 20px;
  border: none;
  border-radius: 4px;
  background: #409eff;
  color: white;
  cursor: pointer;
  font-size: 14px;
  transition: background 0.3s;
}

.send-btn:hover:not(:disabled) {
  background: #66b1ff;
}

.send-btn:disabled {
  background: #c0c4cc;
  cursor: not-allowed;
}

.group-members-section {
  width: 280px;
  background: white;
  border-left: 1px solid #e4e7ed;
  display: flex;
  flex-direction: column;
}

.members-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 15px;
  border-bottom: 1px solid #e4e7ed;
}

.members-header h4 {
  margin: 0;
  font-size: 15px;
  color: #303133;
}

.close-btn {
  border: none;
  background: none;
  font-size: 20px;
  color: #909399;
  cursor: pointer;
}

.members-list {
  flex: 1;
  overflow-y: auto;
  padding: 10px;
}

.member-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px;
  border-radius: 6px;
  transition: background 0.3s;
}

.member-item:hover {
  background: #f5f7fa;
}

.member-info {
  flex: 1;
}

.member-info h5 {
  margin: 0 0 4px;
  font-size: 14px;
  color: #303133;
}

.member-info p {
  margin: 0;
  font-size: 12px;
  color: #909399;
}

.member-actions .chat-btn {
  padding: 4px 8px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  background: white;
  color: #606266;
  cursor: pointer;
  font-size: 12px;
}

.member-actions .chat-btn:hover {
  border-color: #409eff;
  color: #409eff;
}

.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

/* 子模态框（如重命名、转让群主）需要更高的层级 */
.modal-overlay.sub-modal {
  z-index: 2000;
}

.modal-content {
  background: white;
  border-radius: 8px;
  width: 500px;
  max-height: 80vh;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px;
  border-bottom: 1px solid #e4e7ed;
}

.modal-header h3 {
  margin: 0;
  font-size: 18px;
  color: #303133;
}

.modal-header .close-btn {
  font-size: 24px;
}

.modal-form {
  padding: 20px;
  overflow-y: auto;
}

.form-group {
  margin-bottom: 20px;
}

.form-group label {
  display: block;
  margin-bottom: 8px;
  font-size: 14px;
  color: #606266;
  font-weight: 600;
}

.form-group input[type="text"] {
  width: 100%;
  padding: 10px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  font-size: 14px;
}

.form-group input[type="text"]:focus {
  outline: none;
  border-color: #409eff;
}

.friends-selection {
  max-height: 200px;
  overflow-y: auto;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  padding: 10px;
}

.friend-checkbox {
  padding: 8px;
  border-bottom: 1px solid #f0f0f0;
}

.friend-checkbox:last-child {
  border-bottom: none;
}

.checkbox-label {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  font-size: 14px;
  color: #606266;
}

.checkbox-label input[type="checkbox"] {
  width: 16px;
  height: 16px;
  cursor: pointer;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 20px;
}

.form-actions button {
  padding: 10px 20px;
  border: none;
  border-radius: 4px;
  font-size: 14px;
  cursor: pointer;
  transition: background 0.3s;
}

.form-actions button[type="submit"] {
  background: #409eff;
  color: white;
}

.form-actions button[type="submit"]:hover:not(:disabled) {
  background: #66b1ff;
}

.form-actions button[type="submit"]:disabled {
  background: #c0c4cc;
  cursor: not-allowed;
}

.form-actions button[type="button"] {
  background: #f5f7fa;
  color: #606266;
}

.form-actions button[type="button"]:hover {
  background: #e4e7ed;
}

.modal-body {
  padding: 20px;
  overflow-y: auto;
}

.setting-item {
  margin-bottom: 30px;
}

.setting-item h4 {
  margin: 0 0 15px;
  font-size: 16px;
  color: #303133;
  border-bottom: 2px solid #409eff;
  padding-bottom: 8px;
}

.group-info {
  padding: 15px;
  background: #f5f7fa;
  border-radius: 6px;
}

.group-basic-info p {
  margin: 8px 0;
  color: #606266;
}

.owner-actions {
  margin-top: 15px;
  display: flex;
  gap: 10px;
}

.action-btn {
  padding: 8px 16px;
  background: #409eff;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.3s;
}

.action-btn:hover {
  background: #66b1ff;
}

.members-management {
  max-height: 300px;
  overflow-y: auto;
  border: 1px solid #e4e7ed;
  border-radius: 6px;
}

.member-management-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 15px;
  border-bottom: 1px solid #f0f0f0;
  transition: background 0.3s;
}

.member-management-item:last-child {
  border-bottom: none;
}

.member-management-item:hover {
  background: #f5f7fa;
}

.member-basic {
  display: flex;
  align-items: center;
  gap: 10px;
}

.member-name {
  font-weight: 500;
  color: #303133;
}

.member-role {
  font-size: 12px;
  color: #909399;
  padding: 2px 8px;
  background: #f0f0f0;
  border-radius: 10px;
}

.member-actions {
  display: flex;
  gap: 8px;
}

.small-btn {
  padding: 4px 12px;
  font-size: 12px;
  border: 1px solid #dcdfe6;
  background: white;
  color: #606266;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.3s;
}

.small-btn:hover {
  border-color: #409eff;
  color: #409eff;
}

.small-btn.danger {
  color: #f56c6c;
  border-color: #f56c6c;
}

.small-btn.danger:hover {
  background: #f56c6c;
  color: white;
}

.warning-text {
  color: #e6a23c;
  font-size: 14px;
  margin-bottom: 20px;
  padding: 10px;
  background: #fdf6ec;
  border-left: 3px solid #e6a23c;
  border-radius: 4px;
}

.setting-actions {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.danger-btn {
  padding: 10px 20px;
  border: 1px solid #f56c6c;
  border-radius: 4px;
  background: white;
  color: #f56c6c;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.3s;
}

.danger-btn:hover {
  background: #f56c6c;
  color: white;
}
</style>