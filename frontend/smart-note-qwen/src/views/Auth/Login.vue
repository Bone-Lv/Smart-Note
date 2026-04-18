<template>
  <div class="login-page">
    <h2>欢迎回来</h2>
    
    <form @submit.prevent="handleLogin" class="login-form">
      <div class="form-group">
        <label for="account">邮箱/手机号</label>
        <input 
          id="account" 
          v-model="formData.account" 
          type="text" 
          placeholder="请输入邮箱或手机号"
          required
        >
      </div>
      
      <div class="form-group">
        <label for="password">密码</label>
        <input 
          id="password" 
          v-model="formData.password" 
          type="password" 
          placeholder="请输入密码"
          required
        >
      </div>
      
      <div class="form-options">
        <label class="checkbox-label">
          <input type="checkbox" v-model="rememberMe">
          <span>记住我</span>
        </label>
        <a href="#" @click.prevent="goToForgotPassword" class="forgot-link">忘记密码？</a>
      </div>
      
      <button type="submit" class="login-btn" :disabled="loading">
        <span v-if="!loading">登录</span>
        <span v-else>登录中...</span>
      </button>
      
      <div class="divider">或</div>
      
      <button type="button" class="code-login-btn" @click="showCodeLogin = true" :disabled="loading">
        验证码登录
      </button>
      
      <p class="signup-text">
        还没有账号？<router-link to="/register">立即注册</router-link>
      </p>
    </form>
    
    <!-- 验证码登录弹窗 -->
    <div v-if="showCodeLogin" class="modal-overlay">
      <div class="modal-content">
        <div class="modal-header">
          <h3>验证码登录</h3>
          <button @click="showCodeLogin = false" class="close-btn">×</button>
        </div>
        
        <form @submit.prevent="handleCodeLogin" class="code-login-form">
          <div class="form-group">
            <label for="email">邮箱</label>
            <input 
              id="email" 
              v-model="codeFormData.email" 
              type="email" 
              placeholder="请输入邮箱"
              required
            >
          </div>
          
          <div class="form-row">
            <div class="form-group" style="flex: 1; margin-right: 10px;">
              <label for="verificationCode">验证码</label>
              <input 
                id="verificationCode" 
                v-model="codeFormData.verificationCode" 
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
          
          <button type="submit" class="login-btn" :disabled="codeLoading">
            <span v-if="!codeLoading">登录</span>
            <span v-else>登录中...</span>
          </button>
        </form>
      </div>
    </div>
    
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
import { sendVerificationCodeApi, loginByVerificationCodeApi } from '../../api/auth.js';

export default {
  name: 'Login',
  setup() {
    const router = useRouter();
    const userStore = useUserStore();
    
    const formData = ref({
      account: '',
      password: ''
    });
    
    const codeFormData = ref({
      email: '',
      verificationCode: ''
    });
    
    const rememberMe = ref(false);
    const loading = ref(false);
    const codeLoading = ref(false);
    const showCodeLogin = ref(false);
    const codeSending = ref(false);
    const countdown = ref(0);
    
    const handleLogin = async () => {
      if (!formData.value.account.trim() || !formData.value.password.trim()) {
        ElMessage.warning('请输入邮箱/手机号和密码');
        return;
      }
      
      loading.value = true;
      
      try {
        const result = await userStore.login(formData.value);
        
        if (result.success) {
          // 保存记住我选项
          if (rememberMe.value) {
            localStorage.setItem('rememberMe', 'true');
            localStorage.setItem('lastAccount', formData.value.account);
          } else {
            localStorage.removeItem('rememberMe');
            localStorage.removeItem('lastAccount');
          }
          
          ElMessage.success('✅ 登录成功！');
          router.push('/app');
        } else {
          ElMessage.error(result.error || '登录失败');
        }
      } catch (error) {
        console.error('Login error:', error);
        ElMessage.error('登录失败，请重试');
      } finally {
        loading.value = false;
      }
    };
    
    const sendVerificationCode = async () => {
      if (!codeFormData.value.email) {
        ElMessage.warning('请输入邮箱');
        return;
      }
      
      codeSending.value = true;
      
      try {
        await sendVerificationCodeApi(codeFormData.value.email);
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
    
    const handleCodeLogin = async () => {
      if (!codeFormData.value.email || !codeFormData.value.verificationCode) {
        ElMessage.warning('请输入邮箱和验证码');
        return;
      }
      
      codeLoading.value = true;
      
      try {
        const res = await loginByVerificationCodeApi({
          email: codeFormData.value.email,
          verificationCode: codeFormData.value.verificationCode
        });
        
        // 假设 API 封装返回的是 axios 响应对象，且业务数据在 res.data.data 中
        // 如果 API 封装已经提取了 data，则直接使用 res
        const result = res.data?.data || res.data;
        
        if (!result) {
          ElMessage.error('登录响应数据异常');
          return;
        }
        
        // Token已由后端通过Cookie自动设置
        // 根据后端返回结构，可能是 result.user 或直接是 result
        const userInfo = result.user || result;
        
        if (userInfo) {
          userStore.setUser(userInfo);
          ElMessage.success('✅ 登录成功！');
          router.push('/app');
        } else {
          ElMessage.error('获取用户信息失败');
        }
      } catch (error) {
        console.error('Code login error:', error);
        ElMessage.error(error.response?.data?.message || '验证码登录失败');
      } finally {
        codeLoading.value = false;
      }
    };
    
    const goToForgotPassword = () => {
      // 跳转到忘记密码页面
      router.push('/forgot-password');
    };
    
    return {
      formData,
      codeFormData,
      rememberMe,
      loading,
      codeLoading,
      showCodeLogin,
      codeSending,
      countdown,
      handleLogin,
      sendVerificationCode,
      handleCodeLogin,
      goToForgotPassword
    };
  }
};
</script>

<style scoped>
.login-page {
  width: 100%;
  max-width: 400px;
  margin: 0 auto;
}

.login-page h2 {
  text-align: center;
  color: #333;
  margin-bottom: 32px;
  font-size: 24px;
}

.login-form {
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

.form-options {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  font-size: 14px;
}

.checkbox-label {
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  color: #666;
}

.checkbox-label input {
  margin: 0;
}

.forgot-link {
  color: #409eff;
  text-decoration: none;
}

.forgot-link:hover {
  text-decoration: underline;
}

.login-btn, .code-login-btn {
  width: 100%;
  padding: 14px;
  background: #409eff;
  color: white;
  border: none;
  border-radius: 6px;
  font-size: 16px;
  font-weight: 500;
  cursor: pointer;
  transition: background 0.3s;
}

.login-btn:hover:not(:disabled), 
.code-login-btn:hover:not(:disabled) {
  background: #66b1ff;
}

.login-btn:disabled, 
.code-login-btn:disabled {
  background: #ccc;
  cursor: not-allowed;
}

.divider {
  text-align: center;
  margin: 24px 0;
  position: relative;
  color: #999;
}

.divider::before {
  content: '';
  position: absolute;
  top: 50%;
  left: 0;
  right: 0;
  height: 1px;
  background: #eee;
  z-index: 1;
}

.divider span {
  position: relative;
  z-index: 2;
  background: white;
  padding: 0 12px;
}

.signup-text {
  text-align: center;
  margin-top: 24px;
  color: #666;
}

.signup-text a {
  color: #409eff;
  text-decoration: none;
  font-weight: 500;
}

.signup-text a:hover {
  text-decoration: underline;
}

.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
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
  max-width: 400px;
  max-height: 80vh;
  overflow-y: auto;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 24px;
  border-bottom: 1px solid #eee;
}

.modal-header h3 {
  margin: 0;
  color: #333;
}

.close-btn {
  background: none;
  border: none;
  font-size: 24px;
  cursor: pointer;
  color: #999;
  padding: 0;
  width: 30px;
  height: 30px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.close-btn:hover {
  color: #333;
}

.code-login-form {
  padding: 24px;
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