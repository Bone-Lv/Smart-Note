import request from '../utils/request.js';

export const authApi = {
  // 发送验证码
  sendVerificationCode(data) {
    return request.post('/user/send-code', data);
  },

  // 验证码注册
  registerByCode(data) {
    return request.post('/user/register-by-code', data);
  },

  // 用户登录
  login(data) {
    return request.post('/user/login', data);
  },

  // 验证码登录
  loginByCode(data) {
    return request.post('/user/login-by-code', data);
  },

  // 获取当前用户信息
  getUserInfo() {
    return request.get('/user');
  },

  // 更新用户信息
  updateUserInfo(data) {
    return request.put('/user', data);
  },

  // 修改密码
  updatePassword(data) {
    return request.put('/user/password', data);
  },

  // 上传头像
  uploadAvatar(data) {
    return request.post('/user/avatar', data, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    });
  },

  // 退出登录
  logout() {
    return request.post('/user/logout');
  }
};