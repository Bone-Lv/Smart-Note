<template>
  <div class="login-box">
    <el-card>
      <h2>登录系统</h2>
      <el-button type="primary" @click="login" style="width:100%">登录</el-button>
    </el-card>
  </div>
</template>
<script setup>
import { useUserStore } from '@/stores/userStore'
import { useRouter } from 'vue-router'
const store = useUserStore()
const router = useRouter()
const login = () => { store.setToken('mock-token'); router.push('/') }
</script>
<style scoped>
.login-box { height: 100vh; display: flex; justify-content: center; align-items: center; }
</style>