import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { title: '登录注册' }
  },
  {
    path: '/',
    redirect: '/home'
  },
  {
    path: '/home',
    name: 'Home',
    component: () => import('@/views/Home.vue'),
    meta: { title: '公开日记广场' }
  },
  {
    path: '/diary/write',
    name: 'DiaryWrite',
    component: () => import('@/views/DiaryWrite.vue'),
    meta: { title: '写日记', requiresAuth: true }
  },
  {
    path: '/diary/edit/:id',
    name: 'DiaryEdit',
    component: () => import('@/views/DiaryWrite.vue'),
    meta: { title: '编辑日记', requiresAuth: true }
  },
  {
    path: '/diary/detail/:id',
    name: 'DiaryDetail',
    component: () => import('@/views/DiaryDetail.vue'),
    meta: { title: '日记详情' }
  },
  {
    path: '/personal',
    name: 'Personal',
    component: () => import('@/views/Personal.vue'),
    meta: { title: '个人中心', requiresAuth: true }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫
router.beforeEach((to, from, next) => {
  // 设置页面标题
  document.title = to.meta.title ? `${to.meta.title} - 佘记` : '佘记'
  
  // 需要登录的页面
  if (to.meta.requiresAuth) {
    const userStore = useUserStore()
    if (!userStore.token) {
      next({
        path: '/login',
        query: { redirect: to.fullPath }
      })
    } else {
      next()
    }
  } else {
    next()
  }
})

export default router
