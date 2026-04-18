<template>
  <div class="login-page">
    <h2>登录</h2>
    
    <form @submit.prevent="handleLogin" class="login-form">
      <div class="form-group">
        <label for="account">邮箱/手机号</label>
        <input 
          type="text" 
          id="account" 
          v-model="formData.account" 
          placeholder="请输入邮箱或手机号"
          required
        />
      </div>
      
      <div class="form-group">
        <label for="password">密码</label>
        <input 
          type="password" 
          id="password" 
          v-model="formData.password" 
          placeholder="请输入密码"
          required
        />
      </div>
      
      <div class="form-actions">
        <button type="submit" class="btn btn-primary" :disabled="loading">
          {{ loading ? '登录中...' : '登录' }}
        </button>
        <button type="button" class="btn btn-secondary" @click="switchToRegister">
          注册账号
        </button>
      </div>
      
      <div class="form-divider">
        <span>或</span>
      </div>
      
      <div class="form-group">
        <label for="email">邮箱</label>
        <input 
          type="email" 
          id="email" 
          v-model="codeForm.email" 
          placeholder="请输入邮箱"
        />
      </div>
      
      <div class="form-row">
        <div class="form-group flex-grow">
          <label for="verificationCode">验证码</label>
          <input 
            type="text" 
            id="verificationCode" 
            v-model="codeForm.verificationCode" 
            placeholder="请输入验证码"
          />
        </div>
        <button 
          type="button" 
          class="btn btn-outline" 
          :disabled="countdown > 0 || codeLoading"
          @click="sendCode"
        >
          {{ countdown > 0 ? `${countdown}s` : '发送验证码' }}
        </button>
      </div>
      
      <button 
        type="button" 
        class="btn btn-primary" 
        :disabled="codeLoading" 
        @click="handleLoginByCode"
      >
        {{ codeLoading ? '登录中...' : '验证码登录' }}
      </button>
    </form>
  </div>
</template>

<script>
import router from '@/router/index.js';
import { authApi } from '../../api/auth.js';

export default {
  name: 'Login',
  data() {
    return {
      formData: {
        account: '',
        password: ''
      },
      codeForm: {
        email: '',
        verificationCode: ''
      },
      loading: false,
      codeLoading: false,
      countdown: 0
    };
  },
  methods: {
    async handleLogin() {
      if (!this.formData.account || !this.formData.password) {
        alert('请填写完整的登录信息');
        return;
      }

      this.loading = true;
      try {
        const response = await authApi.login(this.formData);
        localStorage.setItem('token', response.data.token);
        this.$router.push('/');
      } catch (error) {
        alert('登录失败: ' + error.message);
      } finally {
        this.loading = false;
      }
    },

    async handleLoginByCode() {
      if (!this.codeForm.email || !this.codeForm.verificationCode) {
        alert('请填写完整的验证码登录信息');
        return;
      }

      this.codeLoading = true;
      try {
        const response = await authApi.loginByCode(this.codeForm);
        localStorage.setItem('token', response.data.token);
        this.$router.push('/');
      } catch (error) {
        alert('验证码登录失败: ' + error.message);
      } finally {
        this.codeLoading = false;
      }
    },

    async sendCode() {
      if (!this.codeForm.email) {
        alert('请输入邮箱');
        return;
      }

      try {
        await authApi.sendVerificationCode({ email: this.codeForm.email });
        alert('验证码已发送');
        this.countdown = 60;
        const timer = setInterval(() => {
          this.countdown--;
          if (this.countdown <= 0) {
            clearInterval(timer);
          }
        }, 1000);
      } catch (error) {
        alert('发送验证码失败: ' + error.message);
      }
    },

    switchToRegister() {
      this.$router.push('/auth/register');
    }
  }
};
</script>

<style scoped>
.login-page {
  width: 100%;
}

.login-page h2 {
  margin: 0 0 24px 0;
  font-size: 24px;
  font-weight: 600;
  color: #24292f;
}

.login-form {
  width: 100%;
}

.form-group {
  margin-bottom: 20px;
}

.form-group label {
  display: block;
  margin-bottom: 8px;
  font-weight: 500;
  color: #24292f;
  font-size: 14px;
}

.form-group input {
  width: 100%;
  padding: 12px 16px;
  border: 1px solid #d0d7de;
  border-radius: 6px;
  font-size: 14px;
  box-sizing: border-box;
  transition: all 0.2s;
}

.form-group input:focus {
  outline: none;
  border-color: #0969da;
  box-shadow: 0 0 0 3px rgba(9, 105, 218, 0.1);
}

.form-group input::placeholder {
  color: #656d76;
}

.form-actions {
  display: flex;
  gap: 12px;
  margin-bottom: 24px;
}

.form-actions .btn {
  flex: 1;
}

.form-divider {
  text-align: center;
  margin: 24px 0;
  position: relative;
}

.form-divider::before {
  content: '';
  position: absolute;
  top: 50%;
  left: 0;
  right: 0;
  height: 1px;
  background: #e1e5e9;
  z-index: 1;
}

.form-divider span {
  background: white;
  position: relative;
  z-index: 2;
  padding: 0 16px;
  color: #656d76;
  font-size: 14px;
}

.form-row {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
  align-items: flex-end;
}

.flex-grow {
  flex: 1;
}

.btn {
  padding: 12px 24px;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  text-align: center;
  transition: all 0.2s;
  height: 44px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.btn-primary {
  background: #0969da;
  color: white;
}

.btn-primary:hover:not(:disabled) {
  background: #085fac;
  transform: translateY(-1px);
  box-shadow: 0 2px 8px rgba(9, 105, 218, 0.3);
}

.btn-secondary {
  background: #f6f8fa;
  color: #24292f;
  border: 1px solid #d0d7de;
}

.btn-secondary:hover:not(:disabled) {
  background: #eaecef;
}

.btn-outline {
  background: transparent;
  color: #0969da;
  border: 1px solid #0969da;
  white-space: nowrap;
}

.btn-outline:hover:not(:disabled) {
  background: #f6f8fa;
}

.btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
</style>