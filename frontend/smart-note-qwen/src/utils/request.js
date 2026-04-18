import axios from 'axios';
import { useUserStore } from '../stores/userStore.js';
import { useLoadingStore } from '../stores/loadingStore.js';
import { ElMessage } from 'element-plus';

const baseURL = 'http://localhost:8080';

/**
 * 自定义 JSON 解析器：将 Long 类型数字转换为字符串，避免精度丢失
 * 使用正则表达式匹配超过安全整数范围的数字
 */
const transformLongToString = (data) => {
  if (typeof data === 'string') {
    // 匹配所有超过 15 位的数字（Long 类型通常 18-19 位）
    // 将其转换为字符串格式 "数字"
    return data.replace(/:\s*(\d{15,})/g, ': "$1"');
  }
  return data;
};

const request = axios.create({
  baseURL,
  timeout: 30000,
  withCredentials: true, // ⚠️ 必须设置为true，允许携带Cookie
  headers: {
    'Content-Type': 'application/json'
  },
  // ✅ 关键修复：在 JSON 解析之前转换 Long 类型
  transformResponse: [
    (data) => {
      // 先处理 Long 类型精度问题
      const processedData = transformLongToString(data);
      // 再解析 JSON
      return JSON.parse(processedData);
    }
  ]
});

// 请求拦截器 - 自动管理Loading状态
request.interceptors.request.use(
  (config) => {
    // 如果未设置hideLoading，则显示loading
    if (!config.hideLoading) {
      try {
        const loadingStore = useLoadingStore();
        loadingStore.startLoading();
      } catch (error) {
        // Pinia 可能未初始化，忽略错误
        console.warn('⚠️ LoadingStore 未初始化:', error);
      }
    }
    return config;
  },
  (error) => {
    // 请求错误时也要关闭loading
    try {
      const loadingStore = useLoadingStore();
      loadingStore.endLoading();
    } catch (error) {
      // 忽略错误
    }
    return Promise.reject(error);
  }
);

// 响应拦截器
request.interceptors.response.use(
  (response) => {
    // 关闭loading
    try {
      if (!response.config.hideLoading) {
        const loadingStore = useLoadingStore();
        loadingStore.endLoading();
      }
    } catch (error) {
      // 忽略错误
    }
    
    // 检查后端返回的业务错误码（即使 HTTP 状态码是 200）
    const responseData = response.data;
    if (responseData && typeof responseData.code === 'number' && responseData.code !== 200) {
      // 业务错误（如 3002: 已是好友关系）
      const errorMessage = responseData.message || responseData.msg || '操作失败';
      ElMessage.error(errorMessage);
      return Promise.reject(new Error(errorMessage));
    }
    
    return response;
  },
  (error) => {
    // 关闭loading
    try {
      if (!error.config?.hideLoading) {
        const loadingStore = useLoadingStore();
        loadingStore.endLoading();
      }
    } catch (error) {
      // 忽略错误
    }
    
    // 获取后端返回的错误信息（优先使用 message 字段）
    const responseData = error.response?.data;
    const errorMessage = responseData?.message || responseData?.msg || error.message || '请求失败';
    const errorCode = responseData?.code;
    
    // 根据HTTP状态码处理不同错误
    if (error.response?.status === 401) {
      // Token过期，跳转到登录页
      const msg = errorMessage !== '请求失败' ? errorMessage : '登录已过期，请重新登录';
      ElMessage.warning(msg);
      const userStore = useUserStore();
      userStore.logout();
      window.location.href = '/login';
    } else if (error.response?.status === 403) {
      const msg = errorMessage !== '请求失败' ? errorMessage : '没有权限访问该资源';
      ElMessage.error(msg);
    } else if (error.response?.status === 404) {
      const msg = errorMessage !== '请求失败' ? errorMessage : '请求的资源不存在';
      ElMessage.error(msg);
    } else if (error.response?.status === 500) {
      const msg = errorMessage !== '请求失败' ? errorMessage : '服务器错误，请稍后重试';
      ElMessage.error(msg);
    } else {
      // 其他错误，显示后端返回的错误信息
      ElMessage.error(errorMessage);
    }
    
    return Promise.reject(error);
  }
);

export default request;
