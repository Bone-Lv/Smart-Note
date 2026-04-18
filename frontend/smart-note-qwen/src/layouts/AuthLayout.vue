<template>
  <div class="auth-layout">
    <div class="auth-container">
      <div class="auth-header">
        <h1>智能笔记</h1>
        <p>让知识管理更智能</p>
      </div>
      <div class="auth-form">
        <router-view />
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'AuthLayout'
};
</script>

<style scoped>
.auth-layout {
  height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
}

.auth-container {
  background: white;
  border-radius: 12px;
  box-shadow: 0 10px 30px rgba(0,0,0,0.2);
  width: 100%;
  max-width: 420px;
  padding: 40px;
}

.auth-header {
  text-align: center;
  margin-bottom: 32px;
}

.auth-header h1 {
  color: #333;
  margin: 0 0 8px 0;
  font-size: 28px;
}

.auth-header p {
  color: #666;
  margin: 0;
  font-size: 16px;
}

.auth-form {
  width: 100%;
}
</style>