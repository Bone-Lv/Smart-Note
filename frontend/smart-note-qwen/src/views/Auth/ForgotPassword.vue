<template>
  <div class="forgot-password-page">
    <h2>重置密码</h2>
    <p class="subtitle">请输入您的邮箱和验证码以重置密码</p>
    
    <form @submit.prevent="handleResetPassword" class="forgot-password-form">
      <div class="form-group">
        <label for="email">邮箱</label>
        <input 
          id="email" 
          v-model="formData.email" 
          type="email" 
          placeholder="请输入注册时使用的邮箱"
          required
        >
      </div>
      
      <div class="form-group verification-group">
        <div class="verification-input">
          <label for="verificationCode">验证码</label>
          <input 
            id="verificationCode" 
            v-model="formData.verificationCode" 
            type="text" 
            placeholder="请输入6位验证码"
            maxlength="6"
            required
          >
        </div>
        <button 
          type="button" 
          class="send-code-btn"
          @click="sendVerificationCode"
          :disabled="codeSending || countdown > 0"
        >
          <span v-if="countdown === 0 && !codeSending">发送验证码</span>
          <span v-else-if="codeSending">发送中...</span>
          <span v-else>{{ countdown }}s后重发</span>
        </button>
      </div>
      
      <div class="form-group">
        <label for="newPassword">新密码</label>
        <input 
          id="newPassword" 
          v-model="formData.newPassword" 
          type="password" 
          placeholder="请输入新密码（8-20位）"
          required
        >
      </div>
      
      <div class="form-group">
        <label for="confirmPassword">确认密码</label>
        <input 
          id="confirmPassword" 
          v-model="formData.confirmPassword" 
          type="password" 
          placeholder="请再次输入新密码"
          required
        >
      </div>
      
      <button type="submit" class="reset-btn" :disabled="loading">
        <span v-if="!loading">重置密码</span>
        <span v-else>重置中...</span>
      </button>
      
      <p class="login-text">
        想起密码了？<router-link to="/login">返回登录</router-link>
      </p>
    </form>
  </div>
</template>

<script>
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { sendVerificationCodeApi, resetPasswordApi } from '../../api/auth.js';

export default {
  name: 'ForgotPassword',
  setup() {
    const router = useRouter();
    
    const formData = ref({
      email: '',
      verificationCode: '',
      newPassword: '',
      confirmPassword: ''
    });
    
    const loading = ref(false);
    const codeSending = ref(false);
    const countdown = ref(0);
    
    // 表单验证
    const validateForm = () => {
      if (!formData.value.email.trim()) {
        ElMessage.warning('请输入邮箱');
        return false;
      }
      
      if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(formData.value.email)) {
        ElMessage.warning('请输入正确的邮箱格式');
        return false;
      }
      
      if (!formData.value.verificationCode.trim()) {
        ElMessage.warning('请输入验证码');
        return false;
      }
      
      if (formData.value.verificationCode.length !== 6) {
        ElMessage.warning('验证码必须为6位');
        return false;
      }
      
      if (!formData.value.newPassword.trim()) {
        ElMessage.warning('请输入新密码');
        return false;
      }
      
      if (formData.value.newPassword.length < 8 || formData.value.newPassword.length > 20) {
        ElMessage.warning('密码长度必须在8-20位之间');
        return false;
      }
      
      if (formData.value.newPassword !== formData.value.confirmPassword) {
        ElMessage.warning('两次输入的密码不一致');
        return false;
      }
      
      return true;
    };
    
    // 发送验证码
    const sendVerificationCode = async () => {
      if (!formData.value.email.trim()) {
        ElMessage.warning('请先输入邮箱');
        return;
      }
      
      if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(formData.value.email)) {
        ElMessage.warning('请输入正确的邮箱格式');
        return;
      }
      
      codeSending.value = true;
      
      try {
        await sendVerificationCodeApi(formData.value.email);
        ElMessage.success('✅ 验证码已发送到您的邮箱');
        
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
        ElMessage.error(error.response?.data?.msg || '发送验证码失败');
      } finally {
        codeSending.value = false;
      }
    };
    
    // 重置密码
    const handleResetPassword = async () => {
      if (!validateForm()) {
        return;
      }
      
      loading.value = true;
      
      try {
        await resetPasswordApi({
          email: formData.value.email,
          verificationCode: formData.value.verificationCode,
          newPassword: formData.value.newPassword,
          confirmPassword: formData.value.confirmPassword
        });
        
        ElMessage.success('✅ 密码重置成功！请使用新密码登录');
        router.push('/login');
      } catch (error) {
        console.error('Reset password error:', error);
        ElMessage.error(error.response?.data?.msg || '密码重置失败，请重试');
      } finally {
        loading.value = false;
      }
    };
    
    return {
      formData,
      loading,
      codeSending,
      countdown,
      sendVerificationCode,
      handleResetPassword
    };
  }
};
</script>

<style scoped>
.forgot-password-page {
  max-width: 420px;
  margin: 0 auto;
  padding: 40px;
  background: white;
  border-radius: 12px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
}

h2 {
  text-align: center;
  color: #333;
  margin-bottom: 10px;
  font-size: 28px;
}

.subtitle {
  text-align: center;
  color: #666;
  margin-bottom: 30px;
  font-size: 14px;
}

.forgot-password-form {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.form-group label {
  font-size: 14px;
  color: #555;
  font-weight: 500;
}

.form-group input {
  padding: 12px 16px;
  border: 1px solid #ddd;
  border-radius: 8px;
  font-size: 14px;
  transition: all 0.3s;
}

.form-group input:focus {
  outline: none;
  border-color: #667eea;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
}

.verification-group {
  display: flex;
  gap: 10px;
}

.verification-input {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.send-code-btn {
  padding: 12px 16px;
  background: #667eea;
  color: white;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-size: 14px;
  white-space: nowrap;
  transition: all 0.3s;
  margin-top: 28px;
}

.send-code-btn:hover:not(:disabled) {
  background: #5568d3;
  transform: translateY(-1px);
}

.send-code-btn:disabled {
  background: #ccc;
  cursor: not-allowed;
  opacity: 0.6;
}

.reset-btn {
  padding: 14px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 16px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s;
  margin-top: 10px;
}

.reset-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(102, 126, 234, 0.3);
}

.reset-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.login-text {
  text-align: center;
  color: #666;
  font-size: 14px;
  margin-top: 10px;
}

.login-text a {
  color: #667eea;
  text-decoration: none;
  font-weight: 500;
}

.login-text a:hover {
  text-decoration: underline;
}
</style>
