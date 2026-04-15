<template>
  <div class="admin-login-container">
    <!-- 全屏背景层 -->
    <div class="global-bg"></div>

    <!-- 左侧展示区 -->
    <div class="left-section">
      <div class="content-wrapper">
        <img src="/images/login-illustration.png" alt="logo" class="logo-img" />
        <h1 class="title">Easy云盘</h1>
        <p class="subtitle">安全、高效的云端文件存储平台</p>
      </div>
    </div>

    <!-- 右侧登录区 -->
    <div class="right-section">
      <div class="login-card">
        <div class="form-header">
          <h2>管理员登录</h2>
          <p>请使用管理员账号密码登录</p>
        </div>

        <el-form
          ref="loginFormRef"
          :model="loginForm"
          :rules="loginRules"
          class="form-content"
          label-position="top"
        >
          <el-form-item prop="email">
            <el-input
              v-model="loginForm.email"
              placeholder="请输入管理员邮箱"
              prefix-icon="Message"
              clearable
            />
          </el-form-item>

          <el-form-item prop="password">
            <el-input
              v-model="loginForm.password"
              type="password"
              placeholder="请输入密码"
              prefix-icon="Lock"
              show-password
            />
          </el-form-item>

          <el-form-item prop="code">
            <div class="code-wrapper">
              <el-input
                v-model="loginForm.code"
                placeholder="请输入验证码"
                class="code-input"
              />
              <img
                :src="codeImg"
                @click="refreshCode"
                class="code-img"
                alt="验证码"
                title="点击刷新"
              />
            </div>
          </el-form-item>

          <div class="form-extra">
            <el-checkbox v-model="loginForm.rememberMe">记住我</el-checkbox>
          </div>

          <div class="login-actions">
            <el-button type="primary" class="action-btn" @click="handleLogin">
              管理员登录
            </el-button>
            <el-button type="default" class="action-btn" @click="goToLogin">
              返回用户登录
            </el-button>
          </div>

        </el-form>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { checkCodeLogin, getCheckCode, type LoginParams, type LoginResponse } from '@/api/user'

const router = useRouter()
const loginFormRef = ref()
const codeImg = ref('')
const checkCodeKey = ref('')

const loginForm = reactive({
  email: '',
  password: '',
  code: '',
  rememberMe: false
})

const loginRules = {
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '邮箱格式不正确', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码至少6位', trigger: 'blur' }
  ],
  code: [
    { required: true, message: '请输入验证码', trigger: 'blur' }
  ]
}

// 刷新验证码
const refreshCode = async () => {
  try {
    const res = await getCheckCode()
    if (res.code === 200 && res.data) {
      codeImg.value = res.data.checkCode
      checkCodeKey.value = res.data.checkCodeKey
    } else {
      ElMessage.error('验证码获取失败')
    }
  } catch (err) {
    console.error('验证码错误：', err)
    ElMessage.error('网络异常，请点击刷新验证码')
  }
}

// 登录处理
const handleLogin = async () => {
  try {
    const res = await checkCodeLogin({
      email: loginForm.email,
      password: loginForm.password,
      checkCode: loginForm.code,
      checkCodeKey: checkCodeKey.value
    })
    
    console.log('登录响应数据:', res)
    if (res.code === 200 && res.status === 'success') {
      // 检查是否是管理员
      console.log('是否是管理员:', res.data.isAdmin)
      if (res.data.isAdmin === '1') {
        // 先设置localStorage
        localStorage.setItem('token', res.data.token)
        localStorage.setItem('userId', res.data.userId)
        localStorage.setItem('isAdmin', '1')
        // 然后显示成功消息并跳转
        ElMessage.success('管理员登录成功')
        // 使用setTimeout确保消息显示后再跳转
        console.log('准备跳转到管理员后台')
        setTimeout(() => {
          console.log('执行跳转')
          router.push('/admin/dashboard')
        }, 500)
      } else {
        ElMessage.error('您不是管理员，无法登录管理员后台')
      }
    } else {
      ElMessage.error(res.info || '登录失败')
    }
  } catch (err: any) {
    console.error('登录异常：', err)
    if (err?.response?.data) {
      ElMessage.error(err.response.data.info || '登录失败')
    } else {
      ElMessage.error('网络连接失败，请重试')
    }
  }
}

const goToLogin = () => {
  router.push('/login')
}

onMounted(() => {
  // 刷新验证码
  refreshCode()
})
</script>

<style scoped>
.admin-login-container {
  position: relative;
  width: 100vw;
  height: 100vh;
  overflow: hidden;
  display: flex;
  font-family: 'Helvetica Neue', Helvetica, 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', '微软雅黑', Arial, sans-serif;
}

.global-bg {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: url('/images/login-illustration.png') center center / cover no-repeat;
  z-index: 0;
}

.left-section {
  position: relative;
  z-index: 1;
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}

.content-wrapper {
  text-align: center;
  color: #fff;
  z-index: 2;
}

.logo-img {
  width: 320px;
  margin-bottom: 24px;
  border-radius: 12px;
  box-shadow: 0 4px 20px rgba(0,0,0,0.2);
}

.title {
  font-size: 48px;
  font-weight: 700;
  margin: 0 0 16px 0;
  text-shadow: 0 2px 10px rgba(0, 0, 0, 0.3);
}

.subtitle {
  font-size: 20px;
  font-weight: 400;
  opacity: 0.9;
  text-shadow: 0 2px 6px rgba(0, 0, 0, 0.2);
}

.right-section {
  position: relative;
  z-index: 1;
  width: 480px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 40px;
}

.login-card {
  width: 100%;
  padding: 40px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.3);
}

.form-header {
  text-align: center;
  margin-bottom: 32px;
}

.form-header h2 {
  margin: 0;
  font-size: 24px;
  color: #333;
  font-weight: 600;
}

.form-header p {
  margin-top: 8px;
  font-size: 14px;
  color: #666;
}

.code-wrapper {
  display: flex;
  gap: 12px;
  align-items: center;
}

.code-img {
  width: 100px;
  height: 40px;
  cursor: pointer;
  border-radius: 4px;
  margin-top: 4px;
  border: 1px solid #eee;
}

.form-extra {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin: 10px 0 24px;
}

.login-actions {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-bottom: 24px;
}

.action-btn {
  width: 100%;
  height: 48px;
  font-size: 16px;
  font-weight: 500;
  box-sizing: border-box;
  margin: 0;
  padding: 0 16px;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  text-align: center;
  border: 1px solid transparent;
}
</style>