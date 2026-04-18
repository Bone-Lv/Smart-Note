import axios from 'axios';

const request = axios.create({
  baseURL: 'http://localhost:8080',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json'
  },
  withCredentials: true // 允许跨域请求携带 Cookie
});

// 请求拦截器
request.interceptors.request.use(
  config => {
    // 如果后端使用 HttpOnly Cookie,不需要手动设置 token
    // 浏览器会自动携带 Cookie
    // 如果后端还需要 header 中的 token,可以取消下面的注释
    // const token = Cookies.get('token');
    // if (token) {
    //   config.headers['Authorization'] = `Bearer ${token}`;
    // }
    return config;
  },
  error => {
    return Promise.reject(error);
  }
);

// 响应拦截器
request.interceptors.response.use(
  response => {
    return response.data;
  },
  error => {
    if (error.response?.status === 401) {
      // 清除 Cookie 中的 token
      Cookies.remove('token');
      window.location.href = '/auth/login';
    }
    return Promise.reject(error);
  }
);

export default request;