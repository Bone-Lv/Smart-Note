<template>
  <div class="register-page">
    <h2>注册</h2>
    
    <form @submit.prevent="handleRegister" class="register-form">
      <div class="form-group">
        <label for="username">用户名</label>
        <input 
          type="text" 
          id="username" 
          v-model="formData.username" 
          placeholder="请输入用户名"
          required
        />
      </div>
      
      <div class="form-group">
        <label for="email">邮箱</label>
        <input 
          type="email" 
          id="email" 
          v-model="formData.email" 
          placeholder="请输入邮箱"
          required
        />
      </div>
      
      <div class="form-group">
        <label for="phone">手机号</label>
        <input 
          type="tel" 
          id="phone" 
          v-model="formData.phone" 
          placeholder="请输入手机号"
          required
        />
      </div>
      
      <div class="form-group">
        <label for="password">密码</label>
        <input 
          type="password" 
          id="password" 
          v-model="formData.password" 
          placeholder="请输入密码（至少8位）"
          required
          minlength="8"
        />
      </div>
      
      <div class="form-group">
        <label for="confirmPassword">确认密码</label>
        <input 
          type="password" 
          id="confirmPassword" 
          v-model="formData.confirmPassword" 
          placeholder="请再次输入密码"
          required
        />
      </div>
      
      <div class="form-row">
        <div class="form-group flex-grow">
          <label for="verificationCode">验证码</label>
          <input 
            type="text" 
            id="verificationCode" 
            v-model="formData.verificationCode" 
            placeholder="请输入验证码"
            required
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
      
      <button type="submit" class="btn btn-primary" :disabled="loading">
        {{ loading ? '注册中...' : '注册' }}
      </button>
      
      <div class="form-footer">
        <span>已有账号？</span>
        <router-link to="/auth/login" class="link">立即登录</router-link>
      </div>
    </form>
  </div>
</template>

<script>
import { authApi } from '../../api/auth.js';

export default {
  name: 'Register',
  data() {
    return {
      formData: {
        username: '',
        email: '',
        phone: '',
        password: '',
        confirmPassword: '',
        verificationCode: ''
      },
      loading: false,
      codeLoading: false,
      countdown: 0
    };
  },
  methods: {
    async handleRegister() {
      if (!this.validateForm()) {
        return;
      }

      if (this.formData.password !== this.formData.confirmPassword) {
        alert('两次输入的密码不一致');
        return;
      }

      this.loading = true;
      try {
        const registerData = {
          username: this.formData.username,
          email: this.formData.email,
          phone: this.formData.phone,
          password: this.formData.password,
          confirmPassword: this.formData.confirmPassword,
          verificationCode: this.formData.verificationCode
        };

        await authApi.registerByCode(registerData);
        alert('注册成功，请登录');
        this.$router.push('/auth/login');
      } catch (error) {
        alert('注册失败: ' + error.message);
      } finally {
        this.loading = false;
      }
    },

    validateForm() {
      if (!this.formData.username) {
        alert('请输入用户名');
        return false;
      }
      if (!this.formData.email) {
        alert('请输入邮箱');
        return false;
      }
      if (!this.formData.phone) {
        alert('请输入手机号');
        return false;
      }
      if (!this.formData.password || this.formData.password.length < 8) {
        alert('密码至少需要8位');
        return false;
      }
      if (!this.formData.confirmPassword) {
        alert('请确认密码');
        return false;
      }
      if (!this.formData.verificationCode) {
        alert('请输入验证码');
        return false;
      }
      return true;
    },

    async sendCode() {
      if (!this.formData.email) {
        alert('请输入邮箱');
        return;
      }

      try {
        await authApi.sendVerificationCode({ email: this.formData.email });
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
    }
  }
};
</script>

<style scoped>
.register-page {
  width: 100%;
}

.register-form {
  width: 100%;
}

.form-group {
  margin-bottom: 20px;
}

.form-group label {
  display: block;
  margin-bottom: 5px;
  font-weight: 500;
  color: #24292f;
}

.form-group input {
  width: 100%;
  padding: 12px 16px;
  border: 1px solid #d0d7de;
  border-radius: 6px;
  font-size: 16px;
  box-sizing: border-box;
}

.form-group input:focus {
  outline: none;
  border-color: #0969da;
  box-shadow: 0 0 0 3px rgba(9, 105, 218, 0.1);
}

.form-row {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
}

.flex-grow {
  flex: 1;
}

.btn {
  padding: 12px 20px;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 16px;
  text-align: center;
  transition: background-color 0.2s;
  width: 100%;
}

.btn-primary {
  background: #0969da;
  color: white;
}

.btn-primary:hover:not(:disabled) {
  background: #085fac;
}

.btn-outline {
  background: transparent;
  color: #0969da;
  border: 1px solid #0969da;
}

.btn-outline:hover:not(:disabled) {
  background: #f6f8fa;
}

.btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.form-footer {
  text-align: center;
  margin-top: 20px;
  color: #656d76;
}

.link {
  color: #0969da;
  text-decoration: none;
  margin-left: 5px;
}

.link:hover {
  text-decoration: underline;
}
</style>