import { defineStore } from 'pinia';
import { authApi } from '../api/auth.js';

export const useUserStore = defineStore('user', {
  state: () => ({
    user: null,
    token: null, // HttpOnly Cookie 无法被前端读取,所以初始化为 null
    isAuthenticated: false,
    unreadMessageCount: 0,
    offlineMessageCount: 0
  }),

  getters: {
    userInfo: (state) => state.user,
    isLoggedIn: (state) => !!state.isAuthenticated
  },

  actions: {
    async login(credentials) {
      try {
        const response = await authApi.login(credentials);
        // token 已经通过 HttpOnly Cookie 存储在浏览器中
        // 前端无法读取 HttpOnly Cookie,只需更新用户信息
        this.user = response.data.user;
        this.isAuthenticated = true;
        return response;
      } catch (error) {
        throw error;
      }
    },

    async loginByCode(credentials) {
      try {
        const response = await authApi.loginByCode(credentials);
        this.user = response.data.user;
        this.isAuthenticated = true;
        return response;
      } catch (error) {
        throw error;
      }
    },

    async register(userData) {
      try {
        const response = await authApi.registerByCode(userData);
        return response;
      } catch (error) {
        throw error;
      }
    },

    async fetchUserInfo() {
      try {
        const response = await authApi.getUserInfo();
        this.user = response.data;
        this.isAuthenticated = true;
        return response;
      } catch (error) {
        throw error;
      }
    },

    async updateUserInfo(userData) {
      try {
        const response = await authApi.updateUserInfo(userData);
        this.user = { ...this.user, ...userData };
        return response;
      } catch (error) {
        throw error;
      }
    },

    async logout() {
      try {
        await authApi.logout();
      } finally {
        this.token = null;
        this.user = null;
        this.isAuthenticated = false;
      }
    },

    setUnreadMessageCount(count) {
      this.unreadMessageCount = count;
    },

    setOfflineMessageCount(count) {
      this.offlineMessageCount = count;
    }
  }
});