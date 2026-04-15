<template>
  <div class="home-container">
    <el-card class="home-card">
      <div class="home-content">
        <div class="welcome-section">
          <h1>欢迎使用Easy云盘</h1>
          <p>安全、高效的云端文件存储平台</p>
        </div>
        <div class="user-info" v-if="userInfo">
          <p>欢迎您，{{ userInfo.nickName || userInfo.email }}</p>
          <p v-if="spaceInfo.isVip">VIP用户</p>
          <p v-else>普通用户</p>
        </div>
        <div class="actions">
          <el-button type="primary" @click="goToFileManager">进入文件管理</el-button>
          <el-button @click="handleLogout">退出登录</el-button>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getUserInfo, logout } from '@/api/user'
import { useUserStore } from '@/store/user'

const router = useRouter()
const userStore = useUserStore()

// 从用户状态管理获取信息
const userInfo = computed(() => userStore.userInfo)
const spaceInfo = computed(() => userStore.spaceInfo)

// 获取用户信息
const fetchUserInfo = async () => {
  const userId = localStorage.getItem('userId')   // ✅ 从本地存储获取 userId
  if (!userId) {
    ElMessage.error('用户信息丢失，请重新登录')
    router.push('/login')
    return
  }
  try {
    await userStore.fetchUserInfo()
    await userStore.fetchSpaceInfo()
    await userStore.fetchVipStatus()
  } catch (error) {
    console.error('获取用户信息失败:', error)
  }
}

// 进入文件管理
const goToFileManager = () => {
  router.push('/files')
}

const handleLogout = async () => {
  try {
    await logout()
  } catch (err) {
    console.error('退出登录接口失败，强制跳转:', err)
  } finally {
    localStorage.removeItem('token')
    userStore.clearUserInfo()
    ElMessage.success('已退出登录')
    router.push('/login')
  }
}

// 组件挂载时获取用户信息
onMounted(() => {
  // 从localStorage加载VIP状态和空间信息
  userStore.loadFromLocalStorage();
  fetchUserInfo()
})


</script>

<style scoped>
.home-container {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: url('/images/login-illustration.png') center center / cover no-repeat;
  display: flex;
  justify-content: center;
  align-items: center;
}

.home-card {
  width: 90%;
  max-width: 600px;
  padding: 40px;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
  border-radius: 16px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
}

.home-content {
  text-align: center;
}

.welcome-section h1 {
  font-size: 32px;
  color: #333;
  margin-bottom: 16px;
}

.welcome-section p {
  font-size: 18px;
  color: #666;
  margin-bottom: 32px;
}

.user-info {
  margin-bottom: 32px;
}

.user-info p {
  font-size: 16px;
  color: #555;
  margin: 8px 0;
}

.actions {
  display: flex;
  gap: 16px;
  justify-content: center;
}

.actions .el-button {
  padding: 12px 24px;
  font-size: 16px;
}
</style>