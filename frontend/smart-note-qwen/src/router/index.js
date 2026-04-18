import { createRouter, createWebHistory } from 'vue-router';
import { useUserStore } from '../stores/userStore.js';
import { checkAuthApi, getUserInfoApi } from '../api/auth.js';

const routes = [
  {
    path: '/',
    component: () => import('../layouts/AuthLayout.vue'),
    redirect: '/login',  // ⚠️ 添加默认重定向到登录页
    children: [
      {
        path: 'login',
        name: 'Login',
        component: () => import('../views/Auth/Login.vue'),
        meta: { requiresAuth: false }
      },
      {
        path: 'register',
        name: 'Register',
        component: () => import('../views/Auth/Register.vue'),
        meta: { requiresAuth: false }
      },
      {
        path: 'forgot-password',
        name: 'ForgotPassword',
        component: () => import('../views/Auth/ForgotPassword.vue'),
        meta: { requiresAuth: false }
      }
    ]
  },
  {
    path: '/app',
    component: () => import('../layouts/DefaultLayout.vue'),
    meta: { requiresAuth: true },
    children: [
      {
        path: '',
        name: 'Dashboard',
        component: () => import('../views/Dashboard.vue'),
        meta: { requiresAuth: true }
      },
      {
        path: 'notes',
        name: 'NoteManager',
        component: () => import('../views/Notes/NoteManager.vue'),
        meta: { requiresAuth: true }
      },
      {
        path: 'note/:noteId',
        name: 'NoteDetail',
        component: () => import('../views/Notes/NoteDetail.vue'),
        props: true,
        meta: { requiresAuth: true }
      },
      {
        path: 'recycle-bin',
        name: 'RecycleBin',
        component: () => import('../views/Notes/RecycleBin.vue'),
        meta: { requiresAuth: true }
      },
      {
        path: 'conversations',
        name: 'ConversationList',
        component: () => import('../views/Social/ConversationList.vue'),
        meta: { requiresAuth: true }
      },
      {
        path: 'groups',
        name: 'GroupChat',
        component: () => import('../views/Social/GroupChat.vue'),
        meta: { requiresAuth: true }
      },
      {
        path: 'friends',
        name: 'FriendList',
        component: () => import('../views/Social/FriendList.vue'),
        meta: { requiresAuth: true }
      },
      {
        path: 'friend-requests',
        name: 'FriendRequests',
        component: () => import('../views/Social/FriendRequests.vue'),
        meta: { requiresAuth: true }
      },
      {
        path: 'chat/private/:friendUserId',
        name: 'PrivateChat',
        component: () => import('../views/Social/PrivateChat.vue'),
        props: true,
        meta: { requiresAuth: true }
      },
      {
        path: 'chat/group/:groupId',
        name: 'GroupChatDetail',
        component: () => import('../views/Social/GroupChat.vue'),
        props: true,
        meta: { requiresAuth: true }
      },
      {
        path: 'profile',
        name: 'UserProfile',
        component: () => import('../views/User/Profile.vue'),
        meta: { requiresAuth: true }
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/login'
  }
];

const router = createRouter({
  history: createWebHistory(),
  routes
});

// 路由守卫 - 通过 meta.requiresAuth 标记和 userStore 状态判断
router.beforeEach(async (to, from, next) => {
  const userStore = useUserStore();
  
  // 检查目标路由是否需要认证
  const requiresAuth = to.matched.some(record => record.meta.requiresAuth);
  
  if (requiresAuth) {
    // 需要认证的页面
    if (!userStore.isLoggedIn) {
      // 用户未登录，调用后端接口验证
      try {
        // ⚠️ 认证请求不显示全局loading，避免影响用户体验
        const response = await checkAuthApi();
        
        // 验证成功，更新用户状态
        // 注意：response.data 可能是 { code: 200, data: {...} } 或直接是用户数据
        const apiData = response.data;
        const userData = apiData.data || apiData;
        
        if (userData && (apiData.code === 200 || apiData.code === undefined)) {
          try {
            // 调用 /user 接口获取完整用户信息（包含头像、用户名等）
            const userInfoResponse = await getUserInfoApi();
            const fullUserData = userInfoResponse.data.data || userInfoResponse.data;
            userStore.setUser(fullUserData);
          } catch (error) {
            // 即使获取用户信息失败，也使用认证数据（至少知道用户已登录）
            userStore.setUser(userData);
          }
          
          next();
        } else {
          next('/login');
        }
      } catch (error) {
        // 认证失败，跳转到登录页
        next('/login');
      }
    } else {
      // 已经登录，直接允许访问
      next();
    }
  } else {
    // 公开页面（登录/注册），直接访问
    // 如果已经登录且访问登录页，重定向到主页
    if (userStore.isLoggedIn && to.name === 'Login') {
      next('/app');
    } else {
      next();
    }
  }
});

export default router;