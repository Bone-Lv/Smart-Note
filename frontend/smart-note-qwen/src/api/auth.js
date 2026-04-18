import request from '../utils/request.js';

// ==================== 注册相关 API ====================

// 邮箱验证码注册
export const registerByVerificationCodeApi = (userData) => {
  return request.post('/user/register-by-code', userData);
};

// ==================== 登录相关 API ====================

// 账号密码登录（邮箱或手机号）
export const loginApi = (credentials) => {
  return request.post('/user/login', credentials);
};

// 邮箱验证码登录
export const loginByVerificationCodeApi = (data) => {
  return request.post('/user/login-by-code', data);
};

// ==================== 退出登录 ====================

// 退出登录（后端会清除Cookie）
export const logoutApi = () => {
  return request.post('/user/logout');
};

// ==================== 用户信息管理 ====================

// 获取用户信息
export const getUserInfoApi = () => {
  return request.get('/user');
};

// 更新用户信息
export const updateUserInfoApi = (userData) => {
  return request.put('/user', userData);
};

// 上传头像
export const uploadAvatarApi = (file) => {
  const formData = new FormData();
  formData.append('file', file);
  return request.post('/user/avatar', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  });
};

// ==================== 密码管理 ====================

// 修改密码
export const updatePasswordApi = (passwordData) => {
  return request.put('/user/password', passwordData);
};

// 发送验证码（用于忘记密码）
export const sendVerificationCodeApi = (email) => {
  return request.post('/user/send-code', { email });
};

// 重置密码（忘记密码）
export const resetPasswordApi = (resetData) => {
  return request.post('/user/reset-password', resetData);
};

// ==================== 其他 API ====================

// ⚠️ 检查认证状态（用于路由守卫）
export const checkAuthApi = () => {
  // ⚠️ 路由守卫中的认证请求不显示全局loading
  return request.get('/user/check-auth', { hideLoading: true });
};
