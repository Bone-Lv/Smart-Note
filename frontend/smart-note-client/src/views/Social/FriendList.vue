<!-- src/views/Social/FriendList.vue -->
<template>
  <div class="social-page">
    <div class="sidebar">
      <div class="tabs">
        <button 
          :class="{ active: activeTab === 'friends' }" 
          @click="activeTab = 'friends'"
        >
          好友
        </button>
        <button 
          :class="{ active: activeTab === 'groups' }" 
          @click="activeTab = 'groups'"
        >
          群组
        </button>
        <button 
          :class="{ active: activeTab === 'requests' }" 
          @click="activeTab = 'requests'"
        >
          申请
        </button>
      </div>
      
      <div class="tab-content">
        <!-- 好友列表 -->
        <div v-if="activeTab === 'friends'" class="friends-tab">
          <div class="search-box">
            <input 
              v-model="searchQuery" 
              type="text" 
              placeholder="搜索好友..."
              @input="filterFriends"
            />
          </div>
          
          <div class="friends-list">
            <div 
              v-for="friend in filteredFriends" 
              :key="friend.id"
              class="friend-item"
              @click="startChat(friend.friendUserId)"
            >
              <img :src="friend.friendAvatar || '/default-avatar.png'" class="avatar" alt="Avatar" />
              <div class="friend-info">
                <div class="name">{{ friend.remark || friend.friendUsername }}</div>
                <div class="motto">{{ friend.friendMotto }}</div>
              </div>
              <button @click.stop="showFriendMenu(friend)" class="menu-btn">⋮</button>
            </div>
          </div>
        </div>
        
        <!-- 群组列表 -->
        <div v-if="activeTab === 'groups'" class="groups-tab">
          <button @click="createGroup" class="btn-primary">创建群聊</button>
          
          <div class="groups-list">
            <div 
              v-for="group in groups" 
              :key="group.id"
              class="group-item"
              @click="joinGroup(group.id)"
            >
              <img :src="group.avatar || '/default-group.png'" class="avatar" alt="Group Avatar" />
              <div class="group-info">
                <div class="name">{{ group.groupName }}</div>
                <div class="member-count">{{ group.memberCount }}人</div>
              </div>
            </div>
          </div>
        </div>
        
        <!-- 申请列表 -->
        <div v-if="activeTab === 'requests'" class="requests-tab">
          <div class="requests-list">
            <div 
              v-for="request in requests" 
              :key="request.id"
              class="request-item"
            >
              <img :src="request.applicantAvatar || '/default-avatar.png'" class="avatar" alt="Avatar" />
              <div class="request-info">
                <div class="name">{{ request.applicantUsername }}</div>
                <div class="message">{{ request.applyMessage || '我想添加你为好友' }}</div>
              </div>
              <div class="request-actions">
                <button @click="handleRequest(request.id, true)" class="btn-success">同意</button>
                <button @click="handleRequest(request.id, false)" class="btn-danger">拒绝</button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
    
    <div class="main-content">
      <div v-if="conversations.length === 0" class="empty-conversations">
        <p>暂无聊天记录</p>
        <p>选择好友开始聊天吧！</p>
      </div>
      
      <div v-else class="conversations-list">
        <div 
          v-for="conv in conversations" 
          :key="conv.conversationType === 'private' ? conv.friendUserId : conv.groupId"
          class="conversation-item"
          :class="{ active: isActiveConversation(conv) }"
          @click="selectConversation(conv)"
        >
          <img 
            :src="conv.friendAvatar || conv.groupAvatar || '/default-avatar.png'" 
            class="avatar" 
            alt="Avatar" 
          />
          <div class="conversation-info">
            <div class="name">
              {{ conv.friendUsername || conv.groupName }}
              <span v-if="conv.unreadCount > 0" class="unread-badge">{{ conv.unreadCount }}</span>
            </div>
            <div class="last-message">{{ conv.lastMessage || '开始聊天' }}</div>
          </div>
          <div class="conversation-time">{{ formatDate(conv.lastMessageTime) }}</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getFriendList, getReceivedRequests, getMyGroups, getConversationList, handleFriendRequest, createGroup as apiCreateGroup, joinGroup as apiJoinGroup } from '@/api/social'
import { FriendVO, FriendRequestVO, ChatGroupVO, ConversationSessionVO } from '@/types/api'

const router = useRouter()

// 状态
const activeTab = ref<'friends' | 'groups' | 'requests'>('friends')
const friends = ref<FriendVO[]>([])
const groups = ref<ChatGroupVO[]>([])
const requests = ref<FriendRequestVO[]>([])
const conversations = ref<ConversationSessionVO[]>([])
const searchQuery = ref('')

// 过滤后的好友列表
const filteredFriends = computed(() => {
  if (!searchQuery.value) return friends.value
  return friends.value.filter(friend => 
    friend.friendUsername.includes(searchQuery.value) || 
    (friend.remark && friend.remark.includes(searchQuery.value))
  )
})

// 格式化日期
const formatDate = (dateStr?: string) => {
  if (!dateStr) return ''
  return new Date(dateStr).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
}

// 检查是否是当前会话
const isActiveConversation = (conv: ConversationSessionVO) => {
  // 这里应该根据当前路由判断
  return false
}

// 选择会话
const selectConversation = (conv: ConversationSessionVO) => {
  if (conv.conversationType === 'private' && conv.friendUserId) {
    // 这里应该跳转到私聊页面
    console.log('私聊', conv.friendUserId)
  } else if (conv.conversationType === 'group' && conv.groupId) {
    router.push(`/groups/${conv.groupId}`)
  }
}

// 开始聊天
const startChat = (userId: number) => {
  // 这里应该跳转到与指定用户聊天的页面
  console.log('开始聊天', userId)
}

// 显示好友菜单
const showFriendMenu = (friend: FriendVO) => {
  // 这里可以显示好友操作菜单
  console.log('好友菜单', friend)
}

// 处理好友申请
const handleRequest = async (requestId: number, accept: boolean) => {
  try {
    await handleFriendRequest({ friendId: requestId, accept })
    // 重新加载申请列表
    loadRequests()
    alert(accept ? '已同意好友申请' : '已拒绝好友申请')
  } catch (error) {
    console.error('处理好友申请失败:', error)
    alert('操作失败')
  }
}

// 创建群聊
const createGroup = async () => {
  const groupName = prompt('请输入群聊名称:')
  if (groupName) {
    try {
      await apiCreateGroup({ groupName })
      alert('群聊创建成功')
      loadGroups()
    } catch (error) {
      console.error('创建群聊失败:', error)
      alert('创建群聊失败')
    }
  }
}

// 加入群聊
const joinGroup = (groupId: number) => {
  router.push(`/groups/${groupId}`)
}

// 加载数据
const loadFriends = async () => {
  try {
    const response = await getFriendList()
    if (response.code === 200) {
      friends.value = response.data
    }
  } catch (error) {
    console.error('加载好友列表失败:', error)
  }
}

const loadGroups = async () => {
  try {
    const response = await getMyGroups()
    if (response.code === 200) {
      groups.value = response.data
    }
  } catch (error) {
    console.error('加载群组列表失败:', error)
  }
}

const loadRequests = async () => {
  try {
    const response = await getReceivedRequests()
    if (response.code === 200) {
      requests.value = response.data
    }
  } catch (error) {
    console.error('加载申请列表失败:', error)
  }
}

const loadConversations = async () => {
  try {
    const response = await getConversationList()
    if (response.code === 200) {
      conversations.value = response.data
    }
  } catch (error) {
    console.error('加载会话列表失败:', error)
  }
}

// 过滤好友
const filterFriends = () => {
  // 过滤逻辑在computed属性中
}

onMounted(() => {
  loadFriends()
  loadGroups()
  loadRequests()
  loadConversations()
})
</script>

<style scoped>
.social-page {
  display: flex;
  height: 100%;
  background-color: #f8f9fa;
}

.sidebar {
  width: 300px;
  background-color: white;
  border-right: 1px solid #e9ecef;
  display: flex;
  flex-direction: column;
}

.tabs {
  display: flex;
  border-bottom: 1px solid #e9ecef;
}

.tabs button {
  flex: 1;
  padding: 15px;
  border: none;
  background: none;
  cursor: pointer;
  font-size: 16px;
}

.tabs button.active {
  border-bottom: 3px solid #007bff;
  color: #007bff;
}

.tab-content {
  flex: 1;
  overflow-y: auto;
  padding: 15px;
}

.search-box {
  margin-bottom: 15px;
}

.search-box input {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid #ddd;
  border-radius: 4px;
  box-sizing: border-box;
}

.friends-list, .groups-list, .requests-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.friend-item, .group-item, .request-item {
  display: flex;
  align-items: center;
  padding: 10px;
  border-radius: 6px;
  cursor: pointer;
}

.friend-item:hover, .group-item:hover {
  background-color: #f8f9fa;
}

.avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  margin-right: 10px;
  object-fit: cover;
}

.friend-info, .group-info, .request-info {
  flex: 1;
}

.name {
  font-weight: bold;
  margin-bottom: 2px;
}

.motto, .member-count, .message {
  font-size: 0.9em;
  color: #666;
}

.menu-btn {
  background: none;
  border: none;
  font-size: 18px;
  cursor: pointer;
  padding: 5px;
  border-radius: 4px;
}

.menu-btn:hover {
  background-color: #f8f9fa;
}

.btn-primary {
  width: 100%;
  padding: 10px;
  background-color: #007bff;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  margin-bottom: 15px;
}

.request-actions {
  display: flex;
  gap: 5px;
}

.btn-success, .btn-danger {
  padding: 5px 10px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}

.btn-success {
  background-color: #28a745;
  color: white;
}

.btn-danger {
  background-color: #dc3545;
  color: white;
}

.main-content {
  flex: 1;
  padding: 20px;
}

.empty-conversations {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: #666;
}

.conversations-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.conversation-item {
  display: flex;
  align-items: center;
  padding: 12px;
  border-radius: 6px;
  cursor: pointer;
}

.conversation-item:hover {
  background-color: #f8f9fa;
}

.conversation-item.active {
  background-color: #e7f3ff;
}

.conversation-info {
  flex: 1;
  margin: 0 10px;
}

.conversation-time {
  font-size: 0.8em;
  color: #666;
}

.unread-badge {
  background-color: #dc3545;
  color: white;
  border-radius: 10px;
  padding: 2px 6px;
  font-size: 0.7em;
  margin-left: 5px;
}
</style>