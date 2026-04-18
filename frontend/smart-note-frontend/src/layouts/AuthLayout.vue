<template>
  <div class="auth-layout">
    <div class="auth-container">
      <div class="auth-card">
        <div class="auth-logo">
          <h1>Smart Note</h1>
          <p>智能笔记系统</p>
        </div>
        <div class="auth-content">
          <router-view />
        </div>
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
  min-height: 100vh;
  background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
}

.auth-container {
  width: 100%;
  max-width: 400px;
  padding: 20px;
}

.auth-card {
  background: white;
  border-radius: 12px;
  box-shadow: 0 8px 32px rgba(0,0,0,0.1);
  overflow: hidden;
}

.auth-logo {
  text-align: center;
  padding: 40px 20px 20px;
  background: #0969da;
  color: white;
}

.auth-logo h1 {
  margin: 0;
  font-size: 28px;
  font-weight: bold;
}

.auth-logo p {
  margin: 8px 0 0;
  opacity: 0.9;
}

.auth-content {
  padding: 40px 30px;
}
</style>