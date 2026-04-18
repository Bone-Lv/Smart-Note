// src/router/index.ts
import { createRouter, createWebHistory } from 'vue-router'
import { userStore } from '@/stores/userStore'

const routes = [
  {
    path: '/',
    redirect: '/notes'
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Auth/Login.vue'),
    meta: { requiresAuth: false }
  },
  {
    path: '/notes',
    name: 'Notes',
    component: () => import('@/views/Notes/NoteManager.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/notes/:id',
    name: 'NoteDetail',
    component: () => import('@/views/Notes/NoteDetail.vue'),
    meta: { requiresAuth: true },
    props: true
  },
  {
    path: '/social',
    name: 'Social',
    component: () => import('@/views/Social/FriendList.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/groups/:id',
    name: 'GroupChat',
    component: () => import('@/views/Social/GroupChat.vue'),
    meta: { requiresAuth: true },
    props: true
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const store = userStore()
  const isLoggedIn = store.isLoggedIn()
  
  if (to.meta.requiresAuth && !isLoggedIn) {
    next('/login')
  } else if (to.path === '/login' && isLoggedIn) {
    next('/notes')
  } else {
    next()
  }
})

export default router