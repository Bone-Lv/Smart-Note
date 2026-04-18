// src/utils/request.ts
import axios, { AxiosInstance, InternalAxiosRequestConfig } from 'axios'
import { userStore } from '@/stores/userStore'

const request: AxiosInstance = axios.create({
  baseURL: '/api',
  timeout: 10000
})

// 请求拦截器
request.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const store = userStore()
    if (store.token) {
      config.headers.Authorization = `Bearer ${store.token}`
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// 响应拦截器
request.interceptors.response.use(
  (response) => {
    return response.data
  },
  (error) => {
    if (error.response?.status === 401) {
      // Token失效，跳转到登录页
      const store = userStore()
      store.logout()
      window.location.href = '/login'
    }
    return Promise.reject(error)
  }
)

export default request