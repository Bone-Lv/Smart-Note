import { defineStore } from 'pinia';
import { ref } from 'vue';
import { loginApi, registerByVerificationCodeApi, getUserInfoApi, logoutApi } from '../api/auth.js';
import wsService from '../utils/websocket.js';

export const useUserStore = defineStore('user', () => {
  // ⚠️ Token由HttpOnly Cookie自动管理，前端不再存储
  const userInfo = ref({});
  const isLoggedIn = ref(false);

  const setUser = (userData) => {
    userInfo.value = userData;
    isLoggedIn.value = true;
  };

  const clearUser = () => {
    userInfo.value = {};
    isLoggedIn.value = false;
  };

  const login = async (credentials) => {
    try {
      const response = await loginApi(credentials);
      const { data } = response.data;
      
      // ⚠️ Token已由后端通过Set-Cookie响应头自动设置到Cookie中
      // 前端只需保存用户信息
      
      // 防御性编程：检查data和user是否存在
      if (!data) {
        console.error('登录响应数据为空:', response);
        return { success: false, error: '登录响应数据异常' };
      }
      
      if (data.user) {
        setUser(data.user);
      } else if (data) {
        // 如果后端直接返回用户数据而不是包装在user字段中
        setUser(data);
      } else {
        console.error('登录响应中没有用户数据:', data);
        return { success: false, error: '登录响应数据异常' };
      }
      
      // 🚀 登录成功后自动连接 WebSocket
      console.log('🔌 登录成功，正在连接 WebSocket...');
      try {
        wsService.connect();
      } catch (wsError) {
        console.warn('⚠️ WebSocket 连接失败，将在进入主页面后重试:', wsError);
      }
      
      return { success: true, data };
    } catch (error) {
      console.error('Login error:', error);
      return { success: false, error: error.response?.data?.msg || error.message };
    }
  };

  const register = async (userData) => {
    try {
      const response = await registerByVerificationCodeApi(userData);
      return { success: true, data: response.data };
    } catch (error) {
      console.error('Registration error:', error);
      return { success: false, error: error.response?.data?.msg || error.message };
    }
  };

  const fetchUserInfo = async () => {
    try {
      const response = await getUserInfoApi();
      setUser(response.data.data);
      return response.data;
    } catch (error) {
      console.error('Fetch user info error:', error);
      throw error;
    }
  };

  const logout = async () => {
    try {
      await logoutApi();
      // ⚠️ 后端会清除Cookie，前端只需清除用户信息
      clearUser();
      
      // ✅ 登出时断开 WebSocket 连接
      wsService.disconnect();
    } catch (error) {
      console.error('Logout error:', error);
    }
  };

  return {
    userInfo,
    isLoggedIn,
    setUser,
    clearUser,
    login,
    register,
    fetchUserInfo,
    logout
  };
});