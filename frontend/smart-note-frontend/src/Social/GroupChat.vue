<template>
  <div class="group-chat-page">
    <div class="page-header">
      <h2>群聊</h2>
      <button class="btn btn-primary" @click="showCreateGroupModal = true">创建群聊</button>
    </div>
    
    <div class="groups-container">
      <div 
        v-for="group in groups" 
        :key="group.id"
        class="group-card"
        @click="enterGroupChat(group.id)"
      >
        <div class="group-avatar">
          <img :src="group.avatar || '/default-group-avatar.png'" alt="群头像" />
        </div>
        <div class="group-info">
          <div class="group-name">{{ group.groupName }}</div>
          <div class="group-meta">
            <span>{{ group.memberCount }}人</span>
            <span v-if="group.ownerUsername">群主：{{ group.ownerUsername }}</span>
          </div>
          <div v-if="group.lastMessage" class="group-last-message">
            {{ group.lastMessage }}
          </div>
        </div>
        <div class="group-unread" v-if="group.unreadCount > 0">
          {{ group.unreadCount }}
        </div>
      </div>
    </div>
    
    <!-- 创建群聊模态框 -->
    <div v-if="showCreateGroupModal" class="modal-overlay" @click="showCreateGroupModal = false">
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <h3>创建群聊</h3>
          <button class="close-btn" @click="showCreateGroupModal = false">×</button>
        </div>
        <div class="modal-body">
          <div class="form-group">
            <label>群聊名称</label>
            <input v-model="newGroup.name" placeholder="请输入群聊名称" class="form-input" />
          </div>
          <div class="form-group">
            <label>群聊头像</label>
            <input type="file" @change="handleAvatarUpload" class="form-input" />
          </div>
          <div class="form-group">
            <label>邀请好友</label>
            <div class="friend-search">
              <input 
                v-model="friendSearch" 
                placeholder="搜索好友..."
                @input="searchFriends"
                class="form-input"
              />
            </div>
            <div class="selected-friends">
              <div 
                v-for="friend in selectedFriends" 
                :key="friend.id"
                class="selected-friend"
              >
                <span>{{ friend.remark || friend.friendUsername }}</span>
                <button @click="removeSelectedFriend(friend.id)" class="remove-btn">×</button>
              </div>
            </div>
            <div class="available-friends">
              <div 
                v-for="friend in availableFriends" 
                :key="friend.id"
                class="friend-option"
                @click="selectFriend(friend)"
              >
                {{ friend.remark || friend.friendUsername }}
              </div>
            </div>
          </div>
          <button 
            class="btn btn-primary" 
            @click="createGroup"
            :disabled="!newGroup.name || creatingGroup"
          >
            {{ creatingGroup ? '创建中...' : '创建群聊' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { socialApi } from '../../api/social.js';

export default {
  name: 'GroupChat',
  data() {
    return {
      groups: [],
      showCreateGroupModal: false,
      newGroup: {
        name: '',
        avatar: null
      },
      creatingGroup: false,
      friendSearch: '',
      allFriends: [],
      selectedFriends: [],
      availableFriends: []
    };
  },
  async mounted() {
    await this.loadGroups();
    await this.loadFriends();
  },
  methods: {
    async loadGroups() {
      try {
        const response = await socialApi.getMyGroups();
        this.groups = response.data || [];
      } catch (error) {
        console.error('加载群聊列表失败:', error);
      }
    },

    async loadFriends() {
      try {
        const response = await socialApi.getFriendList();
        this.allFriends = response.data || [];
        this.availableFriends = this.allFriends;
      } catch (error) {
        console.error('加载好友列表失败:', error);
      }
    },

    enterGroupChat(groupId) {
      this.$router.push(`/chat/group/${groupId}`);
    },

    handleAvatarUpload(event) {
      this.newGroup.avatar = event.target.files[0];
    },

    searchFriends() {
      if (!this.friendSearch.trim()) {
        this.availableFriends = this.allFriends.filter(friend => 
          !this.selectedFriends.some(sf => sf.id === friend.id)
        );
        return;
      }

      const searchTerm = this.friendSearch.toLowerCase();
      this.availableFriends = this.allFriends.filter(friend => 
        !this.selectedFriends.some(sf => sf.id === friend.id) &&
        (friend.remark || friend.friendUsername).toLowerCase().includes(searchTerm)
      );
    },

    selectFriend(friend) {
      this.selectedFriends.push(friend);
      this.availableFriends = this.availableFriends.filter(f => f.id !== friend.id);
      this.friendSearch = '';
    },

    removeSelectedFriend(friendId) {
      const friend = this.selectedFriends.find(f => f.id === friendId);
      this.selectedFriends = this.selectedFriends.filter(f => f.id !== friendId);
      this.availableFriends.unshift(friend);
    },

    async createGroup() {
      if (!this.newGroup.name) {
        alert('请输入群聊名称');
        return;
      }

      this.creatingGroup = true;
      try {
        const memberIds = this.selectedFriends.map(f => f.friendUserId);
        
        const createGroupDto = {
          groupName: this.newGroup.name,
          memberIds: memberIds
        };

        const response = await socialApi.createGroup(createGroupDto);
        alert('群聊创建成功！');
        this.showCreateGroupModal = false;
        this.resetCreateGroupForm();
        await this.loadGroups();
      } catch (error) {
        alert('创建群聊失败: ' + error.message);
      } finally {
        this.creatingGroup = false;
      }
    },

    resetCreateGroupForm() {
      this.newGroup = { name: '', avatar: null };
      this.selectedFriends = [];
      this.availableFriends = this.allFriends;
      this.friendSearch = '';
    }
  }
};
</script>

<style scoped>
.group-chat-page {
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

.groups-container {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 16px;
}

.group-card {
  display: flex;
  align-items: center;
  padding: 12px;
  border: 1px solid #e1e5e9;
  border-radius: 8px;
  background: white;
  cursor: pointer;
  transition: box-shadow 0.2s;
  position: relative;
}

.group-card:hover {
  box-shadow: 0 4px 12px rgba(0,0,0,0.1);
}

.group-avatar {
  width: 40px;
  height: 40px;
  border-radius: 8px;
  overflow: hidden;
  margin-right: 12px;
}

.group-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.group-info {
  flex: 1;
}

.group-name {
  font-weight: 600;
  color: #24292f;
  margin-bottom: 4px;
}

.group-meta {
  font-size: 12px;
  color: #656d76;
  display: flex;
  gap: 10px;
  margin-bottom: 4px;
}

.group-last-message {
  font-size: 12px;
  color: #656d76;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.group-unread {
  position: absolute;
  top: -5px;
  right: -5px;
  background: #dc3545;
  color: white;
  border-radius: 50%;
  width: 20px;
  height: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: bold;
}

.btn {
  padding: 8px 16px;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
  transition: background-color 0.2s;
}

.btn-primary {
  background: #0969da;
  color: white;
}

.btn-primary:hover:not(:disabled) {
  background: #085fac;
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

.form-group {
  margin-bottom: 20px;
}

.form-group label {
  display: block;
  margin-bottom: 5px;
  font-weight: 500;
  color: #24292f;
}

.form-input {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid #d0d7de;
  border-radius: 4px;
  font-size: 14px;
  box-sizing: border-box;
}

.friend-search {
  margin-bottom: 10px;
}

.selected-friends {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 10px;
}

.selected-friend {
  background: #0969da;
  color: white;
  padding: 4px 8px;
  border-radius: 12px;
  font-size: 12px;
  display: flex;
  align-items: center;
  gap: 4px;
}

.remove-btn {
  background: none;
  border: none;
  color: white;
  cursor: pointer;
  font-size: 14px;
  padding: 0;
  width: 16px;
  height: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.available-friends {
  max-height: 150px;
  overflow-y: auto;
  border: 1px solid #e1e5e9;
  border-radius: 4px;
  padding: 8px;
}

.friend-option {
  padding: 6px 8px;
  border-radius: 4px;
  cursor: pointer;
  margin-bottom: 2px;
}

.friend-option:hover {
  background: #f6f8fa;
}
</style>