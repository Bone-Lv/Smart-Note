import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/userStore'

// 布局组件
const DefaultLayout = () => import('@/layouts/DefaultLayout.vue')
const AuthLayout = () => import('@/layouts/AuthLayout.vue')

// 视图组件 (按需加载)
const Login = () => import('@/views/Auth/Login.vue')
const NoteManager = () => import('@/views/Notes/NoteManager.vue') // 首页 - 笔记列表
const NoteDetail = () => import('@/views/Notes/NoteDetail.vue')   // 笔记详情
const FriendList = () => import('@/views/Social/FriendList.vue')  // 好友列表
const GroupChat = () => import('@/views/Social/GroupChat.vue')    // 群聊

const routes = [
  {
    path: '/auth',
    component: AuthLayout,
    meta: { layout: 'auth' }, // ✅ 添加 layout 标识
    children: [
      { path: 'login', name: 'Login', component: Login },
      // 如果有注册页可以在这里添加
    ]
  },
  {
    path: '/',
    component: DefaultLayout,
    meta: { requiresAuth: true }, // 需要登录
    children: [
      {
        path: '', // 根路径
        name: 'Home',
        component: NoteManager // ✅ 修正：指向笔记管理页面
      },
      {
        path: 'note/:id',
        name: 'NoteDetail',
        component: NoteDetail,
        props: true
      },
      {
        path: 'social/friends',
        name: 'FriendList',
        component: FriendList
      },
      {
        path: 'social/group/:id',
        name: 'GroupChat',
        component: GroupChat,
        props: true
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes
})

// 路由守卫：检查登录状态
router.beforeEach(async (to, from, next) => {
  const userStore = useUserStore()
  
  // 如果需要登录权限
  if (to.meta.requiresAuth) {
    // 如果用户信息不存在,尝试从后端获取
    if (!userStore.isAuthenticated) {
      try {
        await userStore.fetchUserInfo()
        next()
      } catch (error) {
        // 获取用户信息失败,说明未登录
        next({ name: 'Login' })
      }
    } else {
      next()
    }
  } else {
    next()
  }
})

export default router