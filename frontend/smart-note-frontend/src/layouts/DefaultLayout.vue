<template>
  <div class="default-layout">
    <header class="layout-header">
      <div class="header-content">
        <div class="logo">
          <h1>Smart Note</h1>
        </div>
        <nav class="nav-menu">
          <router-link to="/notes" class="nav-item">笔记</router-link>
          <router-link to="/social" class="nav-item">社交</router-link>
          <router-link to="/groups" class="nav-item">群聊</router-link>
        </nav>
        <div class="user-info">
          <div class="user-avatar" @click="toggleProfileMenu">
            <img :src="userStore.user?.avatar || '/default-avatar.png'" alt="头像" />
          </div>
          <div v-if="showProfileMenu" class="profile-menu">
            <div class="menu-item" @click="goToProfile">个人资料</div>
            <div class="menu-item" @click="changePassword">修改密码</div>
            <div class="menu-item" @click="logout">退出登录</div>
          </div>
        </div>
      </div>
    </header>

    <main class="layout-main">
      <router-view />
    </main>

    <!-- 全局AI聊天悬浮窗 -->
    <div v-if="aiStore.isVisible" class="ai-chat-container">
      <GlobalChat @close="aiStore.setShowAIChat(false)" />
    </div>

    <!-- AI聊天按钮 -->
    <button 
      v-else 
      class="ai-chat-toggle" 
      @click="aiStore.setShowAIChat(true)"
      :class="{ 'has-unread': aiStore.currentMessages.length > 0 }"
    >
      💬 AI助手
    </button>
  </div>
</template>

<script>
import { useUserStore } from '../stores/userStore.js';
import { useAiStore } from '../stores/aiStore.js';
import GlobalChat from '../views/AiChat/GlobalChat.vue';

export default {
  name: 'DefaultLayout',
  components: {
    GlobalChat
  },
  setup() {
    const userStore = useUserStore();
    const aiStore = useAiStore();
    return { userStore, aiStore };
  },
  data() {
    return {
      showProfileMenu: false
    };
  },
  methods: {
    toggleProfileMenu() {
      this.showProfileMenu = !this.showProfileMenu;
    },
    
    goToProfile() {
      // 跳转到个人资料页面
      this.$router.push('/profile');
      this.showProfileMenu = false;
    },
    
    changePassword() {
      // 跳转到修改密码页面
      this.$router.push('/settings/password');
      this.showProfileMenu = false;
    },
    
    async logout() {
      try {
        await this.userStore.logout();
        this.$router.push('/auth/login');
      } catch (error) {
        console.error('退出登录失败:', error);
      }
    }
  },
  mounted() {
    document.addEventListener('click', (e) => {
      if (!this.$el.contains(e.target)) {
        this.showProfileMenu = false;
      }
    });
  }
};
</script>

<style scoped>
.default-layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.layout-header {
  background: #ffffff;
  border-bottom: 1px solid #e1e5e9;
  position: sticky;
  top: 0;
  z-index: 100;
}

.header-content {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  height: 60px;
}

.logo h1 {
  margin: 0;
  color: #0969da;
  font-size: 20px;
}

.nav-menu {
  display: flex;
  gap: 30px;
}

.nav-item {
  text-decoration: none;
  color: #24292f;
  padding: 8px 12px;
  border-radius: 6px;
  transition: background-color 0.2s;
}

.nav-item.router-link-active,
.nav-item:hover {
  background: #f6f8fa;
  color: #0969da;
}

.user-info {
  position: relative;
}

.user-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  overflow: hidden;
  cursor: pointer;
  border: 2px solid #e1e5e9;
}

.user-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.profile-menu {
  position: absolute;
  top: 50px;
  right: 0;
  background: white;
  border: 1px solid #e1e5e9;
  border-radius: 6px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.1);
  min-width: 150px;
  z-index: 1000;
}

.menu-item {
  padding: 12px 16px;
  cursor: pointer;
  border-bottom: 1px solid #f0f0f0;
}

.menu-item:last-child {
  border-bottom: none;
}

.menu-item:hover {
  background: #f6f8fa;
}

.layout-main {
  flex: 1;
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
  width: 100%;
}

.ai-chat-toggle {
  position: fixed;
  bottom: 30px;
  right: 30px;
  width: 60px;
  height: 60px;
  border-radius: 50%;
  background: #0969da;
  color: white;
  border: none;
  cursor: pointer;
  font-size: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 12px rgba(0,0,0,0.15);
  z-index: 999;
}

.ai-chat-toggle:hover {
  background: #085fac;
  transform: scale(1.05);
}

.ai-chat-toggle.has-unread::after {
  content: '';
  position: absolute;
  top: 5px;
  right: 5px;
  width: 12px;
  height: 12px;
  background: #ff4757;
  border-radius: 50%;
}

.ai-chat-container {
  position: fixed;
  bottom: 100px;
  right: 30px;
  z-index: 998;
}
</style>