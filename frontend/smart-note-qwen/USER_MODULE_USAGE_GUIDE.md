# 用户模块使用指南

## 📋 概述

用户模块提供完整的用户管理功能，包括：
- 用户注册（邮箱验证码）
- 用户登录（账号密码/邮箱验证码）
- 用户信息管理（头像、用户名、手机号、座右铭）
- 密码管理（修改密码、重置密码）
- 认证状态检查（路由守卫）

---

## 🔌 API 接口汇总

| 功能 | 接口 | 方法 | 说明 |
|------|------|------|------|
| 注册 | `/user/register-by-code` | POST | 邮箱验证码注册 |
| 账号密码登录 | `/user/login` | POST | account, password |
| 验证码登录 | `/user/login-by-code` | POST | email, verificationCode |
| 退出登录 | `/user/logout` | POST | 清除 Cookie |
| 检查认证 | `/user/check-auth` | GET | 路由守卫用 |
| 获取用户信息 | `/user` | GET | 完整用户信息 |
| 更新用户信息 | `/user` | PUT | username, phone, motto, avatar |
| 上传头像 | `/user/avatar` | POST | multipart/form-data |
| 修改密码 | `/user/password` | PUT | oldPassword, newPassword, confirmPassword |
| 发送验证码 | `/user/send-code` | POST | email |
| 重置密码 | `/user/reset-password` | POST | email, verificationCode, newPassword, confirmPassword |

---

## 📖 使用示例

### 1. 用户注册

```vue
<script setup>
import { ref } from 'vue';
import { registerByVerificationCodeApi, sendVerificationCodeApi } from '@/api/auth.js';
import { ElMessage } from 'element-plus';

const formData = ref({
  username: '',
  email: '',
  phone: '',
  verificationCode: '',
  password: '',
  confirmPassword: ''
});

const sendingCode = ref(false);
const countdown = ref(0);

// 发送验证码
const sendVerificationCode = async () => {
  if (!formData.value.email) {
    ElMessage.warning('请先输入邮箱');
    return;
  }
  
  try {
    sendingCode.value = true;
    await sendVerificationCodeApi(formData.value.email);
    
    ElMessage.success('验证码已发送，请查看邮箱');
    
    // 开始倒计时
    countdown.value = 60;
    const timer = setInterval(() => {
      countdown.value--;
      if (countdown.value <= 0) {
        clearInterval(timer);
      }
    }, 1000);
  } catch (error) {
    ElMessage.error(error.response?.data?.msg || '发送失败');
  } finally {
    sendingCode.value = false;
  }
};

// 注册
const handleRegister = async () => {
  try {
    await registerByVerificationCodeApi(formData.value);
    ElMessage.success('注册成功，请登录');
    router.push('/login');
  } catch (error) {
    ElMessage.error(error.response?.data?.msg || '注册失败');
  }
};
</script>
```

### 2. 账号密码登录

```vue
<script setup>
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import { useUserStore } from '@/stores/userStore.js';
import { ElMessage } from 'element-plus';

const router = useRouter();
const userStore = useUserStore();

const loginForm = ref({
  account: '',
  password: ''
});

const loading = ref(false);

const handleLogin = async () => {
  try {
    loading.value = true;
    const result = await userStore.login(loginForm.value);
    
    if (result.success) {
      ElMessage.success('登录成功');
      router.push('/app');
    }
  } catch (error) {
    ElMessage.error(error.response?.data?.msg || '登录失败');
  } finally {
    loading.value = false;
  }
};
</script>
```

### 3. 邮箱验证码登录

```vue
<script setup>
import { ref } from 'vue';
import { loginByVerificationCodeApi, sendVerificationCodeApi } from '@/api/auth.js';
import { useUserStore } from '@/stores/userStore.js';
import { ElMessage } from 'element-plus';

const userStore = useUserStore();
const router = useRouter();

const loginForm = ref({
  email: '',
  verificationCode: ''
});

const sendingCode = ref(false);
const countdown = ref(0);

// 发送验证码
const sendCode = async () => {
  try {
    sendingCode.value = true;
    await sendVerificationCodeApi(loginForm.value.email);
    
    ElMessage.success('验证码已发送');
    
    countdown.value = 60;
    const timer = setInterval(() => {
      countdown.value--;
      if (countdown.value <= 0) {
        clearInterval(timer);
      }
    }, 1000);
  } catch (error) {
    ElMessage.error('发送失败');
  } finally {
    sendingCode.value = false;
  }
};

// 登录
const handleLogin = async () => {
  try {
    const result = await loginByVerificationCodeApi(loginForm.value);
    
    // 更新用户状态
    userStore.setUser(result.data);
    
    ElMessage.success('登录成功');
    router.push('/app');
  } catch (error) {
    ElMessage.error('登录失败');
  }
};
</script>
```

### 4. 获取用户信息

```vue
<script setup>
import { onMounted } from 'vue';
import { useUserStore } from '@/stores/userStore.js';

const userStore = useUserStore();

onMounted(async () => {
  // 获取用户信息
  await userStore.fetchUserInfo();
  
  console.log('用户信息:', userStore.userInfo);
});
</script>

<template>
  <div class="user-profile">
    <img :src="userStore.userInfo?.avatar" alt="头像" />
    <h2>{{ userStore.userInfo?.username }}</h2>
    <p>邮箱: {{ userStore.userInfo?.email }}</p>
    <p>手机: {{ userStore.userInfo?.phone }}</p>
    <p>座右铭: {{ userStore.userInfo?.motto }}</p>
  </div>
</template>
```

### 5. 更新用户信息

```vue
<script setup>
import { ref } from 'vue';
import { useUserStore } from '@/stores/userStore.js';
import { ElMessage } from 'element-plus';

const userStore = useUserStore();

const editForm = ref({
  username: userStore.userInfo?.username || '',
  phone: userStore.userInfo?.phone || '',
  motto: userStore.userInfo?.motto || ''
});

const loading = ref(false);

const handleUpdate = async () => {
  try {
    loading.value = true;
    await userStore.fetchUserInfo(); // 或者调用 updateUserInfoApi
    
    ElMessage.success('更新成功');
  } catch (error) {
    ElMessage.error('更新失败');
  } finally {
    loading.value = false;
  }
};
</script>
```

### 6. 上传头像

```vue
<script setup>
import { ref } from 'vue';
import { useUserStore } from '@/stores/userStore.js';
import { uploadAvatarApi } from '@/api/auth.js';
import { ElMessage } from 'element-plus';

const userStore = useUserStore();
const previewUrl = ref('');
const uploading = ref(false);

// 选择文件
const handleFileChange = async (event) => {
  const file = event.target.files[0];
  if (!file) return;
  
  // 验证文件类型
  if (!file.type.startsWith('image/')) {
    ElMessage.error('请选择图片文件');
    return;
  }
  
  // 验证文件大小（最大 2MB）
  if (file.size > 2 * 1024 * 1024) {
    ElMessage.error('图片大小不能超过 2MB');
    return;
  }
  
  // 预览
  previewUrl.value = URL.createObjectURL(file);
  
  try {
    uploading.value = true;
    
    // 上传头像
    const result = await uploadAvatarApi(file);
    
    // 更新用户信息
    await userStore.fetchUserInfo();
    
    ElMessage.success('头像上传成功');
  } catch (error) {
    ElMessage.error('上传失败');
  } finally {
    uploading.value = false;
    // 释放 URL 对象
    if (previewUrl.value) {
      URL.revokeObjectURL(previewUrl.value);
    }
  }
};
</script>

<template>
  <div class="avatar-upload">
    <div class="avatar-preview">
      <img :src="previewUrl || userStore.userInfo?.avatar" alt="头像" />
      <div v-if="uploading" class="upload-overlay">
        <el-icon class="is-loading"><Loading /></el-icon>
      </div>
    </div>
    
    <input 
      type="file" 
      accept="image/*"
      @change="handleFileChange"
      style="display: none"
      ref="fileInput"
    />
    
    <el-button @click="$refs.fileInput.click()" type="primary">
      更换头像
    </el-button>
  </div>
</template>
```

### 7. 修改密码

```vue
<script setup>
import { ref } from 'vue';
import { updatePasswordApi } from '@/api/auth.js';
import { ElMessage, ElMessageBox } from 'element-plus';

const passwordForm = ref({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
});

const loading = ref(false);

const handleChangePassword = async () => {
  // 验证新密码
  if (passwordForm.value.newPassword !== passwordForm.value.confirmPassword) {
    ElMessage.warning('两次输入的密码不一致');
    return;
  }
  
  try {
    loading.value = true;
    
    await updatePasswordApi(passwordForm.value);
    
    ElMessage.success('密码修改成功，请重新登录');
    
    // 清空表单
    passwordForm.value = {
      oldPassword: '',
      newPassword: '',
      confirmPassword: ''
    };
    
    // 可选：退出登录
    await ElMessageBox.confirm('密码已修改，是否需要重新登录？', '提示', {
      confirmButtonText: '重新登录',
      cancelButtonText: '继续使用'
    });
    
    // 用户选择重新登录
    // router.push('/login');
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.response?.data?.msg || '修改失败');
    }
  } finally {
    loading.value = false;
  }
};
</script>
```

### 8. 重置密码（忘记密码）

```vue
<script setup>
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import { sendVerificationCodeApi, resetPasswordApi } from '@/api/auth.js';
import { ElMessage } from 'element-plus';

const router = useRouter();

const resetForm = ref({
  email: '',
  verificationCode: '',
  newPassword: '',
  confirmPassword: ''
});

const sendingCode = ref(false);
const countdown = ref(0);
const loading = ref(false);

// 发送验证码
const sendCode = async () => {
  try {
    sendingCode.value = true;
    await sendVerificationCodeApi(resetForm.value.email);
    
    ElMessage.success('验证码已发送到邮箱');
    
    countdown.value = 60;
    const timer = setInterval(() => {
      countdown.value--;
      if (countdown.value <= 0) {
        clearInterval(timer);
      }
    }, 1000);
  } catch (error) {
    ElMessage.error('发送失败');
  } finally {
    sendingCode.value = false;
  }
};

// 重置密码
const handleReset = async () => {
  // 验证密码
  if (resetForm.value.newPassword !== resetForm.value.confirmPassword) {
    ElMessage.warning('两次输入的密码不一致');
    return;
  }
  
  try {
    loading.value = true;
    
    await resetPasswordApi(resetForm.value);
    
    ElMessage.success('密码重置成功，请登录');
    router.push('/login');
  } catch (error) {
    ElMessage.error(error.response?.data?.msg || '重置失败');
  } finally {
    loading.value = false;
  }
};
</script>
```

---

## 🔐 路由守卫实现

```javascript
// router/index.js
import { useUserStore } from '../stores/userStore.js';
import { checkAuthApi } from '../api/auth.js';

router.beforeEach(async (to, from, next) => {
  const userStore = useUserStore();
  const requiresAuth = to.matched.some(record => record.meta.requiresAuth);
  
  if (requiresAuth && !userStore.isLoggedIn) {
    try {
      // 调用后端 API 验证 Cookie 是否有效
      const result = await checkAuthApi();
      
      if (result.data?.authenticated) {
        // 认证成功，更新用户状态
        userStore.setUser(result.data.user || result.data);
        next();
      } else {
        // 认证失败，跳转登录页
        next('/login');
      }
    } catch (error) {
      // 认证失败，跳转登录页
      next('/login');
    }
  } else {
    next();
  }
});
```

---

##  关键实现要点

### 1. HttpOnly Cookie 认证

```javascript
// ✅ 正确：Token 存储在 HttpOnly Cookie 中
// 前端无需手动存储 Token
await loginApi({ account, password });

// ❌ 错误：不要手动存储 Token
localStorage.setItem('token', token);
```

### 2. 验证码发送流程

```javascript
// 1. 调用 API 发送验证码
await sendVerificationCodeApi(email);

// 2. 开始 60 秒倒计时
countdown.value = 60;
const timer = setInterval(() => {
  countdown.value--;
  if (countdown.value <= 0) {
    clearInterval(timer);
  }
}, 1000);

// 3. 倒计时期间禁用按钮
<el-button :disabled="countdown > 0">
  {{ countdown > 0 ? `${countdown}s 后重发` : '发送验证码' }}
</el-button>
```

### 3. 头像上传流程

```javascript
// 1. 选择文件并验证
const file = event.target.files[0];
if (!file.type.startsWith('image/')) {
  ElMessage.error('请选择图片文件');
  return;
}

// 2. 前端预览
previewUrl.value = URL.createObjectURL(file);

// 3. 上传到 OSS
const result = await uploadAvatarApi(file);

// 4. 刷新用户信息
await userStore.fetchUserInfo();
```

### 4. 密码验证

```javascript
// 修改密码时验证
if (newPassword !== confirmPassword) {
  ElMessage.warning('两次输入的密码不一致');
  return;
}

// 密码强度验证（可选）
const passwordRegex = /^(?=.*[A-Za-z])(?=.*\d)[A-Za-z\d]{8,20}$/;
if (!passwordRegex.test(newPassword)) {
  ElMessage.warning('密码必须包含字母和数字，长度 8-20 位');
  return;
}
```

---

## ⚠️ 注意事项

### 1. 认证状态管理

```javascript
// ✅ 使用 userStore.isLoggedIn 判断登录状态
if (userStore.isLoggedIn) {
  // 用户已登录
}

// ❌ 不要检查 localStorage 中的 token
if (localStorage.getItem('token')) { // 错误
```

### 2. 退出登录

```javascript
// ✅ 调用 API 退出，后端清除 Cookie
await userStore.logout();

// ❌ 不要手动清除 localStorage
localStorage.removeItem('token'); // 错误
```

### 3. 验证码有效期

- 验证码发送后 60 秒内不能重复发送
- 验证码有效期 5 分钟
- 倒计时期间按钮禁用

### 4. 文件上传限制

- 头像文件大小不超过 2MB
- 仅支持图片格式（jpg, png, gif 等）
- 上传前前端预览

### 5. 密码安全

- 密码长度 8-20 位
- 必须包含字母和数字
- 修改密码需要输入旧密码
- 重置密码需要邮箱验证码

---

## 📚 相关文档

- [HttpOnly Cookie 认证机制](./HTTPONLY_COOKIE_AUTH.md)
- [Vue Router 路由鉴权规范](./MEMORY.md#vue-router路由鉴权与状态管理规范)
- [UI 交互与 API 错误处理](./MEMORY.md#ui交互与api错误处理规范)

---

**最后更新:** 2026-04-17  
**维护者:** 前端开发团队
