<!-- src/layouts/AuthLayout.vue -->
<template>
  <div class="auth-layout">
    <div class="auth-container">
      <div class="logo">智能笔记</div>
      <slot />
    </div>
  </div>
</template>

<script setup lang="ts">
// 认证布局
</script>

<style scoped>
.auth-layout {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #f8f9fa;
}

.auth-container {
  width: 100%;
  max-width: 400px;
  padding: 40px;
  background-color: white;
  border-radius: 8px;
  box-shadow: 0 0 20px rgba(0,0,0,0.1);
}

.logo {
  text-align: center;
  font-size: 2rem;
  font-weight: bold;
  color: #007bff;
  margin-bottom: 30px;
}
</style>