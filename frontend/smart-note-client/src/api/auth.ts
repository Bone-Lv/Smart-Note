// src/api/auth.ts
import request from '@/utils/request'
import { ApiResponse, LoginVO, UpdatePasswordDTO, UpdateUserDTO } from '@/types/api'

// 用户登录
export const login = (data: { account: string; password: string }) => {
  return request.post<ApiResponse<LoginVO>>('/user/login', data)
}

// 验证码登录
export const loginByCode = (data: { email: string; verificationCode: string }) => {
  return request.post<ApiResponse<LoginVO>>('/user/login-by-code', data)
}

// 用户注册
export const register = (data: { 
  username: string; 
  email: string; 
  phone: string; 
  verificationCode: string; 
  password: string; 
  confirmPassword: string 
}) => {
  return request.post<ApiResponse<any>>('/user/register-by-code', data)
}

// 发送验证码
export const sendVerificationCode = (data: { email: string }) => {
  return request.post<ApiResponse<any>>('/user/send-code', data)
}

// 重置密码
export const resetPassword = (data: { 
  email: string; 
  verificationCode: string; 
  newPassword: string; 
  confirmPassword: string 
}) => {
  return request.post<ApiResponse<any>>('/user/reset-password', data)
}

// 获取用户信息
export const getUserInfo = () => {
  return request.get<ApiResponse<LoginVO['user']>>('/user')
}

// 更新用户信息
export const updateUserInfo = (data: UpdateUserDTO) => {
  return request.put<ApiResponse<any>>('/user', data)
}

// 修改密码
export const updatePassword = (data: UpdatePasswordDTO) => {
  return request.put<ApiResponse<any>>('/user/password', data)
}

// 退出登录
export const logout = () => {
  return request.post<ApiResponse<any>>('/user/logout')
}

// 上传头像
export const uploadAvatar = (file: File) => {
  const formData = new FormData()
  formData.append('file', file)
  return request.post<ApiResponse<string>>('/user/avatar', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}