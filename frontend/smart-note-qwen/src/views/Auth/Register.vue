<template>
  <div class="register-page">
    <h2>创建账户</h2>
    
    <form @submit.prevent="handleRegister" class="register-form">
      <div class="form-group">
        <label for="username">用户名</label>
        <input 
          id="username" 
          v-model="formData.username" 
          type="text" 
          placeholder="请输入用户名（字母、数字、下划线）"
          required
        >
      </div>
      
      <div class="form-group">
        <label for="email">邮箱</label>
        <input 
          id="email" 
          v-model="formData.email" 
          type="email" 
          placeholder="请输入邮箱"
          required
        >
      </div>
      
      <div class="form-group">
        <label for="phone">手机号</label>
        <input 
          id="phone" 
          v-model="formData.phone" 
          type="tel" 
          placeholder="请输入手机号"
          required
        >
      </div>
      
      <div class="form-row">
        <div class="form-group" style="flex: 1; margin-right: 10px;">
          <label for="verificationCode">验证码</label>
          <input 
            id="verificationCode" 
            v-model="formData.verificationCode" 
            type="text" 
            placeholder="请输入验证码"
            maxlength="6"
            required
          >
        </div>
        <div class="form-group" style="margin-top: 24px;">
          <button 
            type="button" 
            class="send-code-btn" 
            @click="sendVerificationCode"
            :disabled="codeSending || countdown > 0"
          >
            {{ countdown > 0 ? `${countdown}s后重发` : '发送验证码' }}
          </button>
        </div>
      </div>
      
      <div class="form-group">
        <label for="password">密码</label>
        <input 
          id="password" 
          v-model="formData.password" 
          type="password" 
          placeholder="请输入密码（8-20位）"
          minlength="8"
          maxlength="20"
          required
        >
      </div>
      
      <div class="form-group">
        <label for="confirmPassword">确认密码</label>
        <input 
          id="confirmPassword" 
          v-model="formData.confirmPassword" 
          type="password" 
          placeholder="请再次输入密码"
          required
        >
      </div>
      
      <button type="submit" class="register-btn" :disabled="loading">
        <span v-if="!loading">注册</span>
        <span v-else>注册中...</span>
      </button>
      
      <p class="login-text">
        已有账户？<router-link to="/login">立即登录</router-link>
      </p>
    </form>
    
    <!-- 加载遮罩 -->
    <div v-if="loading" class="loading-overlay">
      <div class="loading-spinner"></div>
    </div>
  </div>
</template>

<script>
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { useUserStore } from '../../stores/userStore.js';
import { sendVerificationCodeApi, registerByVerificationCodeApi } from '../../api/auth.js';

export default {
  name: 'Register',
  setup() {
    const router = useRouter();
    const userStore = useUserStore();
    
    const formData = ref({
      username: '',
      email: '',
      phone: '',
      verificationCode: '',
      password: '',
      confirmPassword: ''
    });
    
    const loading = ref(false);
    const codeSending = ref(false);
    const countdown = ref(0);
    
    const validateForm = () => {
      if (!formData.value.username.trim()) {
        ElMessage.warning('请输入用户名');
        return false;
      }
      
      if (!/^[a-zA-Z0-9_]+$/.test(formData.value.username)) {
        ElMessage.warning('用户名只能包含字母、数字和下划线');
        return false;
      }
      
      if (!formData.value.email.trim()) {
        ElMessage.warning('请输入邮箱');
        return false;
      }
      
      if (!formData.value.phone.trim()) {
        ElMessage.warning('请输入手机号');
        return false;
      }
      
      if (!/^1[3-9]\d{9}$/.test(formData.value.phone)) {
        ElMessage.warning('请输入正确的手机号格式');
        return false;
      }
      
      if (!formData.value.verificationCode.trim()) {
        ElMessage.warning('请输入验证码');
        return false;
      }
      
      if (!formData.value.password.trim()) {
        ElMessage.warning('请输入密码');
        return false;
      }
      
      if (formData.value.password.length < 8 || formData.value.password.length > 20) {
        ElMessage.warning('密码长度必须在8-20位之间');
        return false;
      }
      
      if (formData.value.password !== formData.value.confirmPassword) {
        ElMessage.warning('两次输入的密码不一致');
        return false;
      }
      
      return true;
    };
    
    const handleRegister = async () => {
      if (!validateForm()) {
        return;
      }
      
      loading.value = true;
      
      try {
        const result = await userStore.register({
          username: formData.value.username,
          email: formData.value.email,
          phone: formData.value.phone,
          verificationCode: formData.value.verificationCode,
          password: formData.value.password,
          confirmPassword: formData.value.confirmPassword
        });
        
        if (result.success) {
          ElMessage.success('✅ 注册成功！');
          router.push('/login');
        } else {
          ElMessage.error(result.error || '注册失败');
        }
      } catch (error) {
        console.error('Registration error:', error);
        ElMessage.error('注册失败，请重试');
      } finally {
        loading.value = false;
      }
    };
    
    const sendVerificationCode = async () => {
      if (!formData.value.email) {
        ElMessage.warning('请输入邮箱');
        return;
      }
      
      codeSending.value = true;
      
      try {
        await sendVerificationCodeApi(formData.value.email);
        ElMessage.success('✅ 验证码已发送');
        
        // 开始倒计时
        countdown.value = 60;
        const timer = setInterval(() => {
          countdown.value--;
          if (countdown.value <= 0) {
            clearInterval(timer);
          }
        }, 1000);
      } catch (error) {
        console.error('Send code error:', error);
        ElMessage.error('发送验证码失败');
      } finally {
        codeSending.value = false;
      }
    };
    
    return {
      formData,
      loading,
      codeSending,
      countdown,
      handleRegister,
      sendVerificationCode
    };
  }
};
</script>

<style scoped>
.register-page {
  width: 100%;
  max-width: 400px;
  margin: 0 auto;
}

.register-page h2 {
  text-align: center;
  color: #333;
  margin-bottom: 32px;
  font-size: 24px;
}

.register-form {
  width: 100%;
}

.form-group {
  margin-bottom: 20px;
}

.form-group label {
  display: block;
  margin-bottom: 6px;
  color: #333;
  font-weight: 500;
}

.form-group input {
  width: 100%;
  padding: 12px 16px;
  border: 1px solid #ddd;
  border-radius: 6px;
  font-size: 16px;
  transition: border-color 0.3s;
}

.form-group input:focus {
  outline: none;
  border-color: #409eff;
}

.form-row {
  display: flex;
  gap: 10px;
}

.send-code-btn {
  padding: 12px 16px;
  background: #f0f9ff;
  color: #409eff;
  border: 1px solid #b3d8ff;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.3s;
}

.send-code-btn:hover:not(:disabled) {
  background: #ecf5ff;
}

.send-code-btn:disabled {
  background: #f5f7fa;
  color: #ccc;
  cursor: not-allowed;
}

.register-btn {
  width: 100%;
  padding: 14px;
  background: #67c23a;
  color: white;
  border: none;
  border-radius: 6px;
  font-size: 16px;
  font-weight: 500;
  cursor: pointer;
  transition: background 0.3s;
}

.register-btn:hover:not(:disabled) {
  background: #85ce61;
}

.register-btn:disabled {
  background: #ccc;
  cursor: not-allowed;
}

.login-text {
  text-align: center;
  margin-top: 24px;
  color: #666;
}

.login-text a {
  color: #409eff;
  text-decoration: none;
  font-weight: 500;
}

.login-text a:hover {
  text-decoration: underline;
}

.loading-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(255, 255, 255, 0.8);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 2000;
}

.loading-spinner {
  width: 40px;
  height: 40px;
  border: 4px solid #e0e0e0;
  border-top: 4px solid #409eff;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}
</style>