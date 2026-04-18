<template>
  <div id="app">
    <router-view />
    <!-- AI悬浮聊天窗口 -->
    <GlobalChat v-if="isLoggedIn" />
  </div>
</template>

<script>
import { computed, onMounted, onUnmounted } from 'vue';
import { useUserStore } from './stores/userStore.js';
import GlobalChat from './views/AiChat/GlobalChat.vue';
import wsService from './utils/websocket.js';

export default {
  name: 'App',
  components: {
    GlobalChat
  },
  setup() {
    const userStore = useUserStore();
    
    // ✅ 使用 isLoggedIn 而不是 token（HttpOnly Cookie 机制）
    const isLoggedIn = computed(() => userStore.isLoggedIn);

    // ✅ 组件挂载时初始化 WebSocket 连接
    onMounted(() => {
      if (userStore.isLoggedIn) {
        console.log('🔌 用户已登录，初始化 WebSocket 连接...');
        wsService.connect();
      }
    });

    // ✅ 组件卸载时断开连接（可选，通常保持长连接）
    // onUnmounted(() => {
    //   wsService.disconnect();
    // });

    return {
      isLoggedIn
    };
  }
};
</script>

<style>
#app {
  font-family: Avenir, Helvetica, Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  height: 100vh;
}
</style>
