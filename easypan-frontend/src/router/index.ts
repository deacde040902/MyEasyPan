import { createRouter, createWebHistory } from 'vue-router'
import Login from '@/views/Login.vue'
import Home from '@/views/Home.vue'
import FileManager from '@/views/FileManager.vue'
import VipPage from '@/views/VipPage.vue'
import Share from '@/views/Share.vue'
import Recycle from '@/views/Recycle.vue'
import Profile from '@/views/Profile.vue'
import SharePage from '@/views/SharePage.vue'

// 路由配置
const routes = [
  {
    path: '/',
    redirect: '/login'
  },
  {
    path: '/login',
    name: 'Login',
    component: Login,
    meta: {
      title: '登录 - Easy云盘'
    }
  },
  {
    path: '/home',
    name: 'Home',
    component: Home,
    meta: {
      title: 'Easy云盘',
      requiresAuth: true
    }
  },
  {
    path: '/files',
    name: 'FileManager',
    component: FileManager,
    meta: {
      title: '文件管理 - Easy云盘',
      requiresAuth: true
    }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/Register.vue'),
    meta: {
      title: '注册 - Easy云盘'
    }
  },
  {
    path: '/forget-password',
    name: 'ForgetPassword',
    component: () => import('@/views/ForgetPassword.vue'),
    meta: {
      title: '忘记密码 - Easy云盘'
    }
  },
  {
    path: '/vip',
    name: 'Vip',
    component: VipPage,
    meta: { requiresAuth: true }
  },
  {
    path: '/share',
    name: 'Share',
    component: Share,
    meta: { requiresAuth: false }
  },
  {
    path: '/share/:shareId',
    name: 'SharePage',
    component: SharePage,
    meta: { requiresAuth: false }
  },
  {
    path: '/recycle',
    name: 'Recycle',
    component: Recycle,
    meta: { requiresAuth: true }
  },
  {
    path: '/profile',
    name: 'Profile',
    component: Profile,
    meta: { requiresAuth: true }
  },
  // 管理员路由
  {
    path: '/admin/login',
    name: 'AdminLogin',
    component: () => import('@/views/admin/AdminLogin.vue'),
    meta: {
      title: '管理员登录 - Easy云盘'
    }
  },
  {
    path: '/admin/dashboard',
    name: 'AdminDashboard',
    component: () => import('@/views/admin/AdminDashboard.vue'),
    meta: {
      title: '管理员后台 - Easy云盘',
      requiresAuth: true,
      requiresAdmin: true
    }
  }
]

// 创建路由
const router = createRouter({
  history: createWebHistory(),
  routes
})

// 全局前置守卫
router.beforeEach((to, from, next) => {
  // 设置页面标题
  if (to.meta.title) {
    document.title = to.meta.title as string
  }

  // 检查是否需要登录权限
  const token = localStorage.getItem('token')
  if (to.meta.requiresAuth && !token) {
    // 需要登录但没有token
    next({
      path: '/login',
      query: { redirect: to.fullPath }
    })
  } else if (to.meta.requiresAdmin) {
    // 检查是否需要管理员权限
    const isAdmin = localStorage.getItem('isAdmin')
    if (!isAdmin) {
      // 不是管理员，重定向到登录页
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