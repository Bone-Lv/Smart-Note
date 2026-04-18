<template>
  <div class="default-layout">
    <!-- 顶部导航栏 -->
    <header class="navbar">
      <div class="nav-brand">
        <router-link to="/app" class="brand-link">
          <h2>智能笔记</h2>
        </router-link>
      </div>
      <nav class="nav-menu">
        <router-link to="/app" class="nav-item" active-class="active">
          <i class="fas fa-home"></i>
          <span>首页</span>
        </router-link>
        <router-link to="/app/notes" class="nav-item" active-class="active">
          <i class="fas fa-book"></i>
          <span>笔记</span>
        </router-link>
        <router-link to="/app/conversations" class="nav-item" active-class="active">
          <i class="fas fa-comment-dots"></i>
          <span>消息</span>
        </router-link>
        <router-link to="/app/friends" class="nav-item" active-class="active">
          <i class="fas fa-users"></i>
          <span>好友</span>
        </router-link>
        <router-link to="/app/groups" class="nav-item" active-class="active">
          <i class="fas fa-comments"></i>
          <span>群聊</span>
        </router-link>
        <div class="user-menu" @click="toggleUserMenu">
          <img :src="userStore.userInfo?.avatar || '/default-avatar.svg'" alt="avatar" class="avatar">
          <span>{{ userStore.userInfo?.username || '用户' }}</span>
          <i class="fas fa-chevron-down"></i>
        </div>
      </nav>
    </header>

    <!-- 用户菜单下拉 -->
    <div v-if="showUserMenu" class="user-dropdown" @click="hideUserMenu">
      <div class="dropdown-content" @click.stop>
        <div class="user-info">
          <img :src="userStore.userInfo?.avatar || '/default-avatar.svg'" alt="avatar" class="large-avatar">
          <div class="user-details">
            <h4>{{ userStore.userInfo?.username || '用户' }}</h4>
            <p>{{ userStore.userInfo?.motto || '暂无座右铭' }}</p>
          </div>
        </div>
        <ul class="menu-options">
          <li @click="goToProfile"><i class="fas fa-user"></i>个人资料</li>
          <li @click="changePassword"><i class="fas fa-lock"></i>修改密码</li>
          <li @click="logout"><i class="fas fa-sign-out-alt"></i>退出登录</li>
        </ul>
      </div>
    </div>

    <!-- 修改密码对话框 -->
    <div v-if="showPasswordDialog" class="modal-overlay" @click="closePasswordDialog">
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <h3>修改密码</h3>
          <button class="close-btn" @click="closePasswordDialog">&times;</button>
        </div>
        <div class="modal-body">
          <div class="form-group">
            <label>旧密码</label>
            <input type="password" v-model="passwordForm.oldPassword" placeholder="请输入旧密码">
          </div>
          <div class="form-group">
            <label>新密码</label>
            <input type="password" v-model="passwordForm.newPassword" placeholder="请输入新密码">
          </div>
          <div class="form-group">
            <label>确认新密码</label>
            <input type="password" v-model="passwordForm.confirmPassword" placeholder="请再次输入新密码">
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn btn-cancel" @click="closePasswordDialog">取消</button>
          <button class="btn btn-primary" @click="handlePasswordChange">确定</button>
        </div>
      </div>
    </div>

    <!-- 主内容区 -->
    <main class="main-content">
      <router-view :key="$route.fullPath" />
    </main>
  </div>
</template>

<script>
import { ref, onMounted, onUnmounted } from 'vue';
import { useRouter } from 'vue-router';
import { useUserStore } from '../stores/userStore.js';
import { useAiStore } from '../stores/aiStore.js';
import { useFriendStore } from '../stores/friendStore.js';
import { updatePasswordApi } from '../api/auth.js';
import { ElMessage } from 'element-plus';
import { useWebSocket, WS_MESSAGE_TYPES } from '../hooks/useWebSocket.js';

export default {
  name: 'DefaultLayout',
  setup() {
    const router = useRouter();
    const userStore = useUserStore();
    const aiStore = useAiStore();
    const friendStore = useFriendStore();
    const wsService = useWebSocket();
    
    const showUserMenu = ref(false);
    const showPasswordDialog = ref(false);
    const passwordForm = ref({
      oldPassword: '',
      newPassword: '',
      confirmPassword: ''
    });

    // 处理好友上线通知
    const handleFriendOnline = (message) => {
      console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
      console.log('🟢 收到好友上线通知');
      console.log('   完整消息:', JSON.stringify(message, null, 2));
      console.log('   friendUserId:', message.friendUserId);
      console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
      
      friendStore.updateFriendOnlineStatus(message.friendUserId, 'online');
      ElMessage.success(`好友已上线`);
    };

    // 处理好友下线通知
    const handleFriendOffline = (message) => {
      console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
      console.log('🔴 收到好友下线通知');
      console.log('   完整消息:', JSON.stringify(message, null, 2));
      console.log('   friendUserId:', message.friendUserId);
      console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
      
      friendStore.updateFriendOnlineStatus(message.friendUserId, 'offline');
    };

    // 注册WebSocket消息监听器
    const registerWebSocketListeners = () => {
      console.log('📡 注册全局WebSocket监听器...');
      wsService.on(WS_MESSAGE_TYPES.FRIEND_ONLINE, handleFriendOnline);
      wsService.on(WS_MESSAGE_TYPES.FRIEND_OFFLINE, handleFriendOffline);
      console.log('✅ 好友在线状态监听器已注册');
    };

    // 移除WebSocket消息监听器
    const unregisterWebSocketListeners = () => {
      console.log('🗑️ 移除全局WebSocket监听器...');
      wsService.off(WS_MESSAGE_TYPES.FRIEND_ONLINE, handleFriendOnline);
      wsService.off(WS_MESSAGE_TYPES.FRIEND_OFFLINE, handleFriendOffline);
      console.log('✅ 好友在线状态监听器已移除');
    };

    const toggleUserMenu = () => {
      showUserMenu.value = !showUserMenu.value;
    };

    const hideUserMenu = () => {
      showUserMenu.value = false;
    };

    const goToProfile = () => {
      hideUserMenu();
      router.push('/app/profile');
    };

    const changePassword = () => {
      hideUserMenu();
      showPasswordDialog.value = true;
    };

    const closePasswordDialog = () => {
      showPasswordDialog.value = false;
      passwordForm.value = {
        oldPassword: '',
        newPassword: '',
        confirmPassword: ''
      };
    };

    const handlePasswordChange = async () => {
      // 验证新密码
      if (!passwordForm.value.oldPassword) {
        ElMessage.warning('请输入旧密码');
        return;
      }
      
      if (!passwordForm.value.newPassword) {
        ElMessage.warning('请输入新密码');
        return;
      }
      
      if (passwordForm.value.newPassword !== passwordForm.value.confirmPassword) {
        ElMessage.warning('两次输入的密码不一致');
        return;
      }
      
      // 密码长度验证
      if (passwordForm.value.newPassword.length < 8 || passwordForm.value.newPassword.length > 20) {
        ElMessage.warning('密码长度必须在8-20位之间');
        return;
      }
      
      try {
        await updatePasswordApi({
          oldPassword: passwordForm.value.oldPassword,
          newPassword: passwordForm.value.newPassword,
          confirmPassword: passwordForm.value.confirmPassword
        });
        
        ElMessage.success('密码修改成功，请重新登录');
        closePasswordDialog();
        
        // 退出登录
        setTimeout(async () => {
          await userStore.logout();
          router.push('/login');
        }, 1500);
      } catch (error) {
        console.error('修改密码失败:', error);
        // 错误提示已由 Axios 拦截器统一处理
      }
    };

    const logout = async () => {
      await userStore.logout();
      router.push('/login');
    };

    // AI聊天窗口
    const toggleChat = () => {
      // 使用 aiStore 来切换聊天窗口
      aiStore.toggleChat();
    };

    // 点击外部关闭菜单
    const handleClickOutside = (event) => {
      if (!event.target.closest('.user-menu') && !event.target.closest('.user-dropdown')) {
        showUserMenu.value = false;
      }
    };

    onMounted(() => {
      document.addEventListener('click', handleClickOutside);
      
      // 🚀 用户上线后自动连接 WebSocket
      console.log('🔌 初始化 WebSocket 连接...');
      wsService.connect();
      
      // 注册WebSocket监听器
      registerWebSocketListeners();
    });

    onUnmounted(() => {
      document.removeEventListener('click', handleClickOutside);
      // 移除WebSocket监听器
      unregisterWebSocketListeners();
    });

    return {
      userStore,
      showUserMenu,
      showPasswordDialog,
      passwordForm,
      toggleUserMenu,
      hideUserMenu,
      goToProfile,
      changePassword,
      closePasswordDialog,
      handlePasswordChange,
      logout,
      toggleChat,
      registerWebSocketListeners,
      unregisterWebSocketListeners
    };
  }
};
</script>

<style scoped>
.default-layout {
  height: 100vh;
  display: flex;
  flex-direction: column;
}

.navbar {
  background: white;
  padding: 0 24px;
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
  z-index: 100;
}

.nav-brand h2 {
  color: #409eff;
  margin: 0;
}

.brand-link {
  text-decoration: none;
  cursor: pointer;
  transition: opacity 0.3s;
}

.brand-link:hover {
  opacity: 0.8;
}

.nav-menu {
  display: flex;
  align-items: center;
  gap: 32px;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #666;
  text-decoration: none;
  font-size: 14px;
  padding: 8px 16px;
  border-radius: 6px;
  transition: all 0.3s;
}

.nav-item:hover {
  background: #f5f7fa;
  color: #409eff;
}

.nav-item.active {
  background: #ecf5ff;
  color: #409eff;
  font-weight: 500;
}

.user-menu {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  padding: 6px 12px;
  border-radius: 20px;
  transition: background 0.3s;
}

.user-menu:hover {
  background: #f5f7fa;
}

.avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  object-fit: cover;
}

.user-dropdown {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 999;
}

.dropdown-content {
  position: absolute;
  top: 60px;
  right: 24px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.15);
  min-width: 250px;
  overflow: hidden;
}

.user-info {
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 12px;
  border-bottom: 1px solid #eee;
}

.large-avatar {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  object-fit: cover;
}

.user-details h4 {
  margin: 0 0 4px 0;
  font-size: 16px;
}

.user-details p {
  margin: 0;
  font-size: 12px;
  color: #999;
}

.menu-options {
  list-style: none;
  padding: 0;
  margin: 0;
}

.menu-options li {
  padding: 12px 20px;
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
  transition: background 0.3s;
}

.menu-options li:hover {
  background: #f8f9fa;
}

.menu-options li i {
  width: 16px;
  color: #666;
}

.main-content {
  flex: 1;
  overflow: auto;
  background: #f5f7fa;
}

/* 模态框样式 */
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

.modal-content {
  background: white;
  border-radius: 8px;
  width: 400px;
  max-width: 90%;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  overflow: hidden;
}

.modal-header {
  padding: 16px 20px;
  border-bottom: 1px solid #eee;
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
  color: #999;
  line-height: 1;
}

.close-btn:hover {
  color: #666;
}

.modal-body {
  padding: 20px;
}

.form-group {
  margin-bottom: 16px;
}

.form-group label {
  display: block;
  margin-bottom: 8px;
  font-size: 14px;
  color: #666;
}

.form-group input {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
  box-sizing: border-box;
}

.form-group input:focus {
  border-color: #409eff;
  outline: none;
}

.modal-footer {
  padding: 12px 20px;
  border-top: 1px solid #eee;
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.btn {
  padding: 8px 16px;
  border-radius: 4px;
  font-size: 14px;
  cursor: pointer;
  border: none;
  transition: all 0.3s;
}

.btn-cancel {
  background: #f5f7fa;
  color: #666;
}

.btn-cancel:hover {
  background: #e4e7ed;
}

.btn-primary {
  background: #409eff;
  color: white;
}

.btn-primary:hover {
  background: #66b1ff;
}
</style>
