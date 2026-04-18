// src/stores/userStore.ts
import { defineStore } from 'pinia'
import { ref } from 'vue'
import { UserVO } from '@/types/api'

export const userStore = defineStore('user', () => {
  const userInfo = ref<UserVO | null>(null)
  const token = ref<string>('')

  const setUserInfo = (user: UserVO) => {
    userInfo.value = user
  }

  const setToken = (tk: string) => {
    token.value = tk
    localStorage.setItem('token', tk)
  }

  const getToken = (): string => {
    if (!token.value) {
      token.value = localStorage.getItem('token') || ''
    }
    return token.value
  }

  const logout = () => {
    userInfo.value = null
    token.value = ''
    localStorage.removeItem('token')
  }

  const isLoggedIn = (): boolean => {
    return !!getToken()
  }

  return {
    userInfo,
    token,
    setUserInfo,
    setToken,
    getToken,
    logout,
    isLoggedIn
  }
})