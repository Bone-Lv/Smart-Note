<!-- src/layouts/DefaultLayout.vue -->
<template>
  <div class="layout">
    <header class="header">
      <div class="logo">智能笔记</div>
      <nav class="nav">
        <router-link to="/notes">笔记</router-link>
        <router-link to="/social">社交</router-link>
        <router-link to="/ai">AI助手</router-link>
      </nav>
      <div class="user-info">
        <span>{{ userInfo?.username }}</span>
        <button @click="handleLogout">退出</button>
      </div>
    </header>
    
    <main class="main">
      <slot />
    </main>
    
    <!-- 全局AI聊天悬浮按钮 -->
    <div class="ai-chat-toggle" @click="toggleChat">
      <span>🤖</span>
    </div>
    
    <!-- AI聊天组件 -->
    <Teleport to="body">
      <GlobalChat v-if="isChatVisible" @close="toggleChat" />
    </Teleport>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { userStore } from '@/stores/userStore'
import { aiStore } from '@/stores/aiStore'
import GlobalChat from '@/views/AiChat/GlobalChat.vue'

const router = useRouter()
const user = userStore()
const ai = aiStore()

const userInfo = computed(() => user.userInfo)
const isChatVisible = computed(() => ai.isChatVisible)

const handleLogout = () => {
  user.logout()
  router.push('/login')
}

const toggleChat = () => {
  ai.toggleChatVisibility()
}
</script>

<style scoped>
.layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 20px;
  height: 60px;
  background-color: #fff;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
}

.logo {
  font-size: 1.5rem;
  font-weight: bold;
  color: #007bff;
}

.nav a {
  margin: 0 15px;
  text-decoration: none;
  color: #333;
}

.nav a.router-link-active {
  color: #007bff;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 15px;
}

.user-info button {
  padding: 5px 10px;
  background-color: #dc3545;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}

.main {
  flex: 1;
  padding: 20px;
}

.ai-chat-toggle {
  position: fixed;
  bottom: 30px;
  right: 30px;
  width: 60px;
  height: 60px;
  background-color: #007bff;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 24px;
  cursor: pointer;
  box-shadow: 0 4px 8px rgba(0,0,0,0.2);
  z-index: 100;
}
</style>