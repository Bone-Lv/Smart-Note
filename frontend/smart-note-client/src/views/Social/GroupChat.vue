<!-- src/views/Social/GroupChat.vue -->
<template>
  <div class="group-chat">
    <div class="chat-header">
      <button @click="goBack" class="back-btn">←</button>
      <div class="group-info">
        <img :src="groupInfo?.avatar || '/default-group.png'" class="group-avatar" alt="Group Avatar" />
        <div class="group-details">
          <div class="group-name">{{ groupInfo?.groupName }}</div>
          <div class="member-count">{{ groupInfo?.memberCount }}人</div>
        </div>
      </div>
      <button @click="showMembers = true" class="members-btn">成员</button>
    </div>
    
    <div class="chat-messages" ref="messagesContainer">
      <div 
        v-for="message in displayedMessages" 
        :key="message.id"
        class="message-item"
        :class="{ 'my-message': message.senderId === currentUser?.id }"
      >
        <img :src="message.senderAvatar || '/default-avatar.png'" class="message-avatar" alt="Avatar" />
        <div class="message-content">
          <div class="message-sender">{{ message.senderUsername }}</div>
          <div class="message-text">{{ message.content }}</div>
          <div class="message-time">{{ formatTime(message.createTime) }}</div>
        </div>
      </div>
      
      <div v-if="loadingMore" class="loading-more">加载中...</div>
    </div>
    
    <div class="chat-input">
      <input 
        v-model="messageInput" 
        type="text" 
        placeholder="输入消息..."
        @keypress.enter="sendMessage"
      />
      <button @click="sendMessage" :disabled="!messageInput.trim()">发送</button>
    </div>
    
    <!-- 群成员模态框 -->
    <div v-if="showMembers" class="modal-overlay" @click="showMembers = false">
      <div class="modal-content members-modal" @click.stop>
        <div class="modal-header">
          <h3>群成员</h3>
          <button @click="showMembers = false" class="close-btn">×</button>
        </div>
        <div class="members-list">
          <div v-for="member in groupMembers" :key="member.id" class="member-item">
            <img :src="member.avatar || '/default-avatar.png'" class="avatar" alt="Avatar" />
            <div class="member-info">
              <div class="name">{{ member.username }}</div>
              <div class="role">{{ getRoleText(member.role) }}</div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, nextTick, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { userStore } from '@/stores/userStore'
import { getGroupDetail, getGroupMessageHistory, sendGroupMessage } from '@/api/social'
import { ChatGroupVO, GroupMessageVO } from '@/types/api'

const route = useRoute()
const router = useRouter()
const user = userStore()

const groupId = computed(() => Number(route.params.id))

// 状态
const groupInfo = ref<ChatGroupVO | null>(null)
const messages = ref<GroupMessageVO[]>([])
const displayedMessages = ref<GroupMessageVO[]>([])
const messageInput = ref('')
const showMembers = ref(false)
const groupMembers = ref<any[]>([])
const loadingMore = ref(false)
const hasMore = ref(true)
const nextCursor = ref<number | null>(null)

// 计算属性
const currentUser = computed(() => user.userInfo)

// 格式化时间
const formatTime = (timeStr: string) => {
  return new Date(timeStr).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
}

// 获取角色文本
const getRoleText = (role: number) => {
  switch (role) {
    case 2: return '群主'
    case 1: return '管理员'
    default: return '成员'
  }
}

// 加载群组详情
const loadGroupInfo = async () => {
  try {
    const response = await getGroupDetail(groupId.value)
    if (response.code === 200) {
      groupInfo.value = response.data
    }
  } catch (error) {
    console.error('加载群组详情失败:', error)
    router.back()
  }
}

// 加载消息历史
const loadMessageHistory = async (cursor?: number | null) => {
  try {
    loadingMore.value = true
    const response = await getGroupMessageHistory({
      groupId: groupId.value,
      pageSize: 20,
      cursor: cursor || undefined
    })
    
    if (response.code === 200) {
      const newMessages = response.data.records
      messages.value = [...newMessages, ...messages.value]
      
      if (cursor === undefined) {
        // 首次加载，显示最新消息
        displayedMessages.value = [...newMessages]
      } else {
        // 加载更多，合并到显示列表
        displayedMessages.value = [...newMessages, ...displayedMessages.value]
      }
      
      nextCursor.value = response.data.nextCursor
      hasMore.value = response.data.hasNext
    }
  } catch (error) {
    console.error('加载消息历史失败:', error)
  } finally {
    loadingMore.value = false
  }
}

// 发送消息
const sendMessage = async () => {
  if (!messageInput.value.trim()) return
  
  try {
    // 这里应该调用发送消息的API
    console.log('发送消息:', messageInput.value)
    
    // 模拟发送成功
    const newMessage: GroupMessageVO = {
      id: Date.now(), // 模拟ID
      groupId: groupId.value,
      senderId: currentUser.value!.id,
      senderUsername: currentUser.value!.username,
      senderAvatar: currentUser.value!.avatar,
      messageType: 1, // 文本消息
      content: messageInput.value,
      createTime: new Date().toISOString()
    }
    
    messages.value = [...messages.value, newMessage]
    displayedMessages.value = [...displayedMessages.value, newMessage]
    messageInput.value = ''
    
    // 滚动到底部
    await nextTick()
    scrollToBottom()
  } catch (error) {
    console.error('发送消息失败:', error)
    alert('发送消息失败')
  }
}

// 滚动到底部
const scrollToBottom = () => {
  const container = document.querySelector('.chat-messages')
  if (container) {
    container.scrollTop = container.scrollHeight
  }
}

// 加载更多消息
const loadMoreMessages = () => {
  if (hasMore.value && !loadingMore.value) {
    loadMessageHistory(nextCursor.value)
  }
}

// 返回上一页
const goBack = () => {
  router.back()
}

// 监听滚动事件加载更多
const handleScroll = (e: Event) => {
  const element = e.target as HTMLElement
  if (element.scrollTop < 100 && hasMore.value && !loadingMore.value) {
    loadMoreMessages()
  }
}

// 初始化
onMounted(async () => {
  await loadGroupInfo()
  await loadMessageHistory()
  
  // 监听滚动事件
  const container = document.querySelector('.chat-messages')
  if (container) {
    container.addEventListener('scroll', handleScroll)
  }
  
  // 滚动到底部
  await nextTick()
  scrollToBottom()
})

// 组件卸载时移除事件监听
onUnmounted(() => {
  const container = document.querySelector('.chat-messages')
  if (container) {
    container.removeEventListener('scroll', handleScroll)
  }
})
</script>

<style scoped>
.group-chat {
  display: flex;
  flex-direction: column;
  height: 100%;
  background-color: white;
}

.chat-header {
  display: flex;
  align-items: center;
  padding: 10px 15px;
  border-bottom: 1px solid #eee;
  background-color: #f8f9fa;
}

.back-btn, .members-btn {
  background: none;
  border: none;
  font-size: 18px;
  cursor: pointer;
  padding: 5px 10px;
  border-radius: 4px;
}

.back-btn:hover, .members-btn:hover {
  background-color: #e9ecef;
}

.group-info {
  flex: 1;
  display: flex;
  align-items: center;
  margin: 0 15px;
}

.group-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  margin-right: 10px;
  object-fit: cover;
}

.group-details {
  display: flex;
  flex-direction: column;
}

.group-name {
  font-weight: bold;
}

.member-count {
  font-size: 0.9em;
  color: #666;
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 15px;
  display: flex;
  flex-direction: column;
}

.message-item {
  display: flex;
  margin-bottom: 15px;
  max-width: 80%;
}

.message-item.my-message {
  align-self: flex-end;
}

.message-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  margin-right: 10px;
  object-fit: cover;
}

.message-item.my-message .message-avatar {
  margin-right: 0;
  margin-left: 10px;
}

.message-content {
  display: flex;
  flex-direction: column;
  background-color: #f1f3f4;
  padding: 8px 12px;
  border-radius: 18px;
  max-width: 100%;
}

.message-item.my-message .message-content {
  background-color: #d0ebff;
}

.message-sender {
  font-size: 0.8em;
  font-weight: bold;
  margin-bottom: 2px;
  color: #666;
}

.message-item.my-message .message-sender {
  color: #007bff;
}

.message-text {
  word-wrap: break-word;
  line-height: 1.4;
}

.message-time {
  font-size: 0.7em;
  color: #999;
  text-align: right;
  margin-top: 4px;
}

.loading-more {
  text-align: center;
  padding: 10px;
  color: #666;
}

.chat-input {
  display: flex;
  padding: 10px;
  border-top: 1px solid #eee;
  background-color: white;
}

.chat-input input {
  flex: 1;
  padding: 8px 12px;
  border: 1px solid #ddd;
  border-radius: 20px;
  margin-right: 10px;
}

.chat-input button {
  padding: 8px 16px;
  background-color: #007bff;
  color: white;
  border: none;
  border-radius: 20px;
  cursor: pointer;
}

.chat-input button:disabled {
  background-color: #ccc;
  cursor: not-allowed;
}

.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
}

.members-modal {
  width: 400px;
  max-height: 60vh;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 15px 20px;
  border-bottom: 1px solid #eee;
}

.close-btn {
  background: none;
  border: none;
  font-size: 24px;
  cursor: pointer;
  padding: 0;
  width: 30px;
  height: 30px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.members-list {
  padding: 15px;
  max-height: 400px;
  overflow-y: auto;
}

.member-item {
  display: flex;
  align-items: center;
  padding: 10px;
  border-radius: 6px;
}

.member-item:hover {
  background-color: #f8f9fa;
}

.member-info {
  margin-left: 10px;
  flex: 1;
}

.role {
  font-size: 0.8em;
  color: #666;
}
</style>