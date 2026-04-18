<template>
  <div class="friend-list-page">
    <div class="page-header">
      <h2>好友列表</h2>
      <button class="btn btn-primary" @click="showSearchModal = true">添加好友</button>
    </div>
    
    <div class="friend-categories">
      <div 
        class="category-tab" 
        :class="{ active: activeCategory === 'all' }"
        @click="activeCategory = 'all'"
      >
        全部好友 ({{ friends.length }})
      </div>
      <div 
        class="category-tab" 
        :class="{ active: activeCategory === 'online' }"
        @click="activeCategory = 'online'"
      >
        在线好友
      </div>
      <div 
        class="category-tab" 
        :class="{ active: activeCategory === 'pending' }"
        @click="loadPendingRequests"
      >
        待处理 ({{ pendingRequests.length }})
      </div>
    </div>
    
    <div class="friends-container">
      <div v-if="activeCategory === 'all'" class="friends-grid">
        <div 
          v-for="friend in filteredFriends" 
          :key="friend.id"
          class="friend-card"
        >
          <div class="friend-avatar">
            <img :src="friend.friendAvatar || '/default-avatar.png'" alt="头像" />
          </div>
          <div class="friend-info">
            <div class="friend-name">
              {{ friend.remark || friend.friendUsername }}
              <span v-if="friend.remark" class="original-name">({{ friend.friendUsername }})</span>
            </div>
            <div class="friend-motto">{{ friend.friendMotto || '这个人很懒，什么也没留下' }}</div>
          </div>
          <div class="friend-actions">
            <button class="btn btn-sm btn-primary" @click="startChat(friend.friendUserId)">发消息</button>
            <button class="btn btn-sm btn-secondary" @click="showFriendMenu(friend)">...</button>
          </div>
        </div>
      </div>
      
      <div v-if="activeCategory === 'pending'" class="pending-requests">
        <div 
          v-for="request in pendingRequests" 
          :key="request.id"
          class="request-item"
        >
          <div class="request-user">
            <img :src="request.applicantAvatar || '/default-avatar.png'" alt="头像" />
            <div class="request-info">
              <div class="request-name">{{ request.applicantUsername }}</div>
              <div class="request-message">{{ request.applyMessage || '请求添加您为好友' }}</div>
            </div>
          </div>
          <div class="request-actions">
            <button class="btn btn-sm btn-success" @click="handleFriendRequest(request.id, true)">同意</button>
            <button class="btn btn-sm btn-danger" @click="handleFriendRequest(request.id, false)">拒绝</button>
          </div>
        </div>
      </div>
    </div>
    
    <!-- 搜索好友模态框 -->
    <div v-if="showSearchModal" class="modal-overlay" @click="showSearchModal = false">
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <h3>添加好友</h3>
          <button class="close-btn" @click="showSearchModal = false">×</button>
        </div>
        <div class="modal-body">
          <div class="search-section">
            <input 
              v-model="searchAccount" 
              placeholder="请输入邮箱或手机号"
              class="search-input"
              @keyup.enter="searchUser"
            />
            <button class="btn btn-primary" @click="searchUser">搜索</button>
          </div>
          
          <div v-if="searchResult" class="search-result">
            <div class="result-user">
              <img :src="searchResult.avatar || '/default-avatar.png'" alt="头像" />
              <div class="result-info">
                <div class="result-name">{{ searchResult.username }}</div>
                <div class="result-motto">{{ searchResult.motto || '这个人很懒，什么也没留下' }}</div>
              </div>
            </div>
            <div class="send-request-section">
              <textarea 
                v-model="requestMessage" 
                placeholder="请输入验证消息（可选）"
                class="request-textarea"
              ></textarea>
              <button class="btn btn-primary" @click="sendFriendRequest">发送好友请求</button>
            </div>
          </div>
        </div>
      </div>
    </div>
    
    <!-- 好友菜单 -->
    <div v-if="showFriendMenuId" class="menu-overlay" @click="showFriendMenuId = null">
      <div class="menu-content" :style="{ top: menuPosition.y + 'px', left: menuPosition.x + 'px' }">
        <div class="menu-item" @click="updateFriendRemark(selectedFriend)">修改备注</div>
        <div class="menu-item" @click="moveToGroup(selectedFriend)">移动分组</div>
        <div class="menu-item danger" @click="removeFriend(selectedFriend)">删除好友</div>
      </div>
    </div>
  </div>
</template>

<script>
import { socialApi } from '../../api/social.js';

export default {
  name: 'FriendList',
  data() {
    return {
      friends: [],
      pendingRequests: [],
      activeCategory: 'all',
      showSearchModal: false,
      searchAccount: '',
      searchResult: null,
      requestMessage: '',
      showFriendMenuId: null,
      selectedFriend: null,
      menuPosition: { x: 0, y: 0 }
    };
  },
  computed: {
    filteredFriends() {
      if (this.activeCategory === 'online') {
        // 这里可以根据实际在线状态过滤
        return this.friends;
      }
      return this.friends;
    }
  },
  async mounted() {
    await this.loadFriends();
  },
  methods: {
    async loadFriends() {
      try {
        const response = await socialApi.getFriendList();
        this.friends = response.data || [];
      } catch (error) {
        console.error('加载好友列表失败:', error);
      }
    },

    async loadPendingRequests() {
      this.activeCategory = 'pending';
      try {
        const response = await socialApi.getReceivedRequests();
        this.pendingRequests = response.data || [];
      } catch (error) {
        console.error('加载待处理请求失败:', error);
      }
    },

    async searchUser() {
      if (!this.searchAccount) {
        alert('请输入搜索条件');
        return;
      }

      try {
        const response = await socialApi.searchUser(this.searchAccount);
        this.searchResult = response.data;
      } catch (error) {
        alert('搜索用户失败: ' + error.message);
      }
    },

    async sendFriendRequest() {
      if (!this.searchResult) {
        alert('请先搜索用户');
        return;
      }

      try {
        await socialApi.sendFriendRequest({
          account: this.searchAccount,
          applyMessage: this.requestMessage
        });
        alert('好友请求已发送');
        this.showSearchModal = false;
        this.resetSearch();
      } catch (error) {
        alert('发送好友请求失败: ' + error.message);
      }
    },

    async handleFriendRequest(requestId, accept) {
      try {
        await socialApi.handleFriendRequest({
          friendId: requestId,
          accept: accept
        });
        
        if (accept) {
          alert('已同意好友请求');
        } else {
          alert('已拒绝好友请求');
        }
        
        // 重新加载待处理请求
        await this.loadPendingRequests();
      } catch (error) {
        alert('处理好友请求失败: ' + error.message);
      }
    },

    showFriendMenu(friend) {
      this.selectedFriend = friend;
      // 获取点击位置
      const rect = event.target.getBoundingClientRect();
      this.menuPosition = {
        x: rect.left,
        y: rect.bottom
      };
      this.showFriendMenuId = friend.id;
    },

    async updateFriendRemark(friend) {
      const newRemark = prompt('请输入新的备注名:', friend.remark || friend.friendUsername);
      if (newRemark !== null) {
        try {
          await socialApi.updateFriendRemark({
            friendUserId: friend.friendUserId,
            remark: newRemark
          });
          await this.loadFriends(); // 重新加载
          alert('备注已更新');
        } catch (error) {
          alert('更新备注失败: ' + error.message);
        }
      }
    },

    moveToGroup(friend) {
      // 这里可以实现移动分组的功能
      alert('移动分组功能待实现');
    },

    async removeFriend(friend) {
      if (confirm(`确定要删除好友 ${friend.remark || friend.friendUsername} 吗？`)) {
        try {
          await socialApi.deleteFriend(friend.friendUserId);
          this.friends = this.friends.filter(f => f.id !== friend.id);
          alert('好友已删除');
        } catch (error) {
          alert('删除好友失败: ' + error.message);
        }
      }
    },

    startChat(userId) {
      // 跳转到聊天页面
      this.$router.push(`/chat/private/${userId}`);
    },

    resetSearch() {
      this.searchAccount = '';
      this.searchResult = null;
      this.requestMessage = '';
    }
  }
};
</script>

<style scoped>
.friend-list-page {
  padding: 20px;
  max-width: 1200px;
  margin: 0 auto;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.page-header h2 {
  margin: 0;
  color: #24292f;
}

.friend-categories {
  display: flex;
  gap: 10px;
  margin-bottom: 20px;
}

.category-tab {
  padding: 10px 20px;
  background: #f6f8fa;
  border-radius: 6px;
  cursor: pointer;
  transition: background-color 0.2s;
}

.category-tab:hover {
  background: #eaecef;
}

.category-tab.active {
  background: #0969da;
  color: white;
}

.friends-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 16px;
}

.friend-card {
  display: flex;
  align-items: center;
  padding: 12px;
  border: 1px solid #e1e5e9;
  border-radius: 8px;
  background: white;
  transition: box-shadow 0.2s;
}

.friend-card:hover {
  box-shadow: 0 4px 12px rgba(0,0,0,0.1);
}

.friend-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  overflow: hidden;
  margin-right: 12px;
}

.friend-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.friend-info {
  flex: 1;
}

.friend-name {
  font-weight: 600;
  color: #24292f;
  margin-bottom: 4px;
}

.original-name {
  font-weight: normal;
  color: #656d76;
  font-size: 12px;
}

.friend-motto {
  font-size: 12px;
  color: #656d76;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.friend-actions {
  display: flex;
  gap: 8px;
}

.pending-requests {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.request-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px;
  border: 1px solid #e1e5e9;
  border-radius: 8px;
  background: white;
}

.request-user {
  display: flex;
  align-items: center;
  gap: 12px;
}

.request-user img {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  object-fit: cover;
}

.request-info {
  display: flex;
  flex-direction: column;
}

.request-name {
  font-weight: 600;
  color: #24292f;
}

.request-message {
  font-size: 12px;
  color: #656d76;
}

.request-actions {
  display: flex;
  gap: 8px;
}

.btn {
  padding: 6px 12px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  transition: background-color 0.2s;
}

.btn-sm {
  padding: 4px 8px;
  font-size: 12px;
}

.btn-primary {
  background: #0969da;
  color: white;
}

.btn-primary:hover:not(:disabled) {
  background: #085fac;
}

.btn-secondary {
  background: #f6f8fa;
  color: #24292f;
  border: 1px solid #d0d7de;
}

.btn-secondary:hover:not(:disabled) {
  background: #eaecef;
}

.btn-success {
  background: #28a745;
  color: white;
}

.btn-success:hover:not(:disabled) {
  background: #218838;
}

.btn-danger {
  background: #dc3545;
  color: white;
}

.btn-danger:hover:not(:disabled) {
  background: #c82333;
}

.btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
}

.modal-content {
  background: white;
  border-radius: 8px;
  width: 90%;
  max-width: 500px;
  max-height: 80vh;
  overflow-y: auto;
}

.modal-header {
  padding: 16px 24px;
  border-bottom: 1px solid #e1e5e9;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.modal-header h3 {
  margin: 0;
  font-size: 18px;
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

.modal-body {
  padding: 24px;
}

.search-section {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
}

.search-input {
  flex: 1;
  padding: 8px 12px;
  border: 1px solid #d0d7de;
  border-radius: 4px;
}

.search-result {
  border: 1px solid #e1e5e9;
  border-radius: 6px;
  padding: 16px;
}

.result-user {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.result-user img {
  width: 50px;
  height: 50px;
  border-radius: 50%;
  object-fit: cover;
}

.result-info {
  flex: 1;
}

.result-name {
  font-weight: 600;
  color: #24292f;
  margin-bottom: 4px;
}

.result-motto {
  font-size: 12px;
  color: #656d76;
}

.request-textarea {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid #d0d7de;
  border-radius: 4px;
  resize: vertical;
  min-height: 60px;
  margin-bottom: 12px;
  box-sizing: border-box;
}

.menu-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 1001;
}

.menu-content {
  position: fixed;
  background: white;
  border: 1px solid #e1e5e9;
  border-radius: 6px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.15);
  z-index: 1002;
  min-width: 150px;
}

.menu-item {
  padding: 10px 16px;
  cursor: pointer;
  border-bottom: 1px solid #f0f0f0;
}

.menu-item:last-child {
  border-bottom: none;
}

.menu-item:hover {
  background: #f6f8fa;
}

.menu-item.danger {
  color: #dc3545;
}

.menu-item.danger:hover {
  background: #fff5f5;
}
</style>