<template>
  <div class="register-container">
    <!-- 1. 全屏背景层 (保持不变，但在CSS中需确保铺满) -->
    <div class="register-bg"></div>

    <!-- 2. 左侧内容区 -->
    <div class="left-section">
      <div class="content-wrapper">
        <img src="/images/login-illustration.png" alt="logo" class="logo-img" />
        <h1 class="title">Easy云盘</h1>
        <p class="subtitle">安全、高效的云端文件存储平台</p>
      </div>
    </div>

    <!-- 3. 右侧注册表单 (关键修改区域) -->
    <div class="right-section">
      <div class="login-card">
        <div class="form-header">
          <h2>用户注册</h2>
          <p>创建新账号以开始使用</p>
        </div>

        <el-form
          ref="registerFormRef"
          :model="registerForm"
          :rules="registerFormRules"
          label-width="0px"
        >
          <el-form-item prop="email">
            <el-input
              v-model="registerForm.email"
              placeholder="请输入邮箱"
              prefix-icon="Message"
              type="email"
            ></el-input>
          </el-form-item>

          <el-form-item prop="checkCode">
            <div class="check-code-wrapper">
              <el-input
                v-model="registerForm.checkCode"
                placeholder="请输入图片验证码"
                prefix-icon="Picture"
                class="check-code-input"
              />
              <img
                :src="captchaUrl"
                @click="refreshCaptcha"
                class="captcha-image"
                alt="验证码"
              />
            </div>
          </el-form-item>

          <el-form-item prop="emailCode">
            <div class="email-code-wrapper">
              <el-input
                v-model="registerForm.emailCode"
                placeholder="请输入邮箱验证码"
                prefix-icon="Bell"
              />
              <el-button
                type="primary"
                size="default"
                @click="sendEmailCodeApi"
                :disabled="disabled"
                class="code-btn"
              >
                {{ codeText }}
              </el-button>
            </div>
          </el-form-item>

          <el-form-item prop="nickName">
            <el-input
              v-model="registerForm.nickName"
              placeholder="请输入昵称"
              prefix-icon="User"
            ></el-input>
          </el-form-item>

          <el-form-item prop="password">
            <el-input
              v-model="registerForm.password"
              placeholder="请输入密码"
              prefix-icon="Lock"
              show-password
            ></el-input>
          </el-form-item>

          <el-form-item>
            <el-button
              type="primary"
              class="register-btn"
              @click="handleRegister"
              :loading="loading"
            >
              注册
            </el-button>
          </el-form-item>

          <div class="register-links">
            <el-link type="primary" @click="goLogin">已有账号？返回登录</el-link>
          </div>
        </el-form>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { useRouter } from 'vue-router'
import { register as apiRegister, getCheckCode, sendEmailCode as apiSendEmailCode } from '@/api/user'

const router = useRouter()
const registerFormRef = ref<FormInstance>()

const registerForm = reactive({
  email: '',
  emailCode: '',
  nickName: '',
  password: '',
  checkCode: '',
  checkCodeKey: ''    // 新增：存储图片验证码的key
})

const captchaUrl = ref('')
const loading = ref(false)
const codeText = ref('获取验证码')
const disabled = ref(false)

let timer: any = null

const registerFormRules: FormRules = {
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '邮箱格式不正确', trigger: 'blur' }
  ],
  emailCode: [{ required: true, message: '请输入邮箱验证码', trigger: 'blur' }],
  nickName: [{ required: true, message: '请输入昵称', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度 6-20 位', trigger: 'blur' }
  ],
  checkCode: [{ required: true, message: '请输入图片验证码', trigger: 'blur' }]
}

// 刷新图片验证码
const refreshCaptcha = async () => {
  try {
    const res = await getCheckCode()
    // 假设返回结构：{ code:200, data: { checkCode: "base64图片", checkCodeKey: "xxx" } }
    captchaUrl.value = res.data.checkCode
    registerForm.checkCodeKey = res.data.checkCodeKey   // 保存key
  } catch (err) {
    console.error('验证码刷新失败:', err)
    ElMessage.error('验证码刷新失败')
  }
}

// 发送邮箱验证码
const sendEmailCodeApi = async () => {
  if (!registerForm.email) {
    ElMessage.warning('请先填写邮箱')
    return
  }
  if (!registerForm.checkCode || !registerForm.checkCodeKey) {
    ElMessage.warning('请先填写图片验证码')
    return
  }
  try {
    // 修正：传递完整的参数（包含 checkCodeKey 和 type）
    await apiSendEmailCode({
      email: registerForm.email,
      checkCode: registerForm.checkCode,
      checkCodeKey: registerForm.checkCodeKey,
      type: '0'   // '0' 表示注册场景
    })
    ElMessage.success('验证码发送成功，请查收邮箱')
    startTimer()
  } catch (err: any) {
    ElMessage.error(err.message || '发送失败，请检查邮箱和验证码')
    refreshCaptcha()   // 发送失败后刷新验证码
  }
}

const startTimer = () => {
  let count = 60
  disabled.value = true
  codeText.value = `${count}秒后重新获取`
  timer = setInterval(() => {
    count--
    codeText.value = `${count}秒后重新获取`
    if (count <= 0) {
      clearInterval(timer)
      codeText.value = '获取验证码'
      disabled.value = false
    }
  }, 1000)
}

// 注册提交
const handleRegister = async () => {
  if (!registerFormRef.value) return
  try {
    await registerFormRef.value.validate()
    loading.value = true
    // 修正：注册时需要提交 checkCodeKey
    await apiRegister({
      email: registerForm.email,
      emailCode: registerForm.emailCode,
      nickName: registerForm.nickName,
      password: registerForm.password,
      checkCode: registerForm.checkCode,
      checkCodeKey: registerForm.checkCodeKey
    })
    ElMessage.success('注册成功，请登录')
    router.push('/login')
  } catch (err: any) {
    ElMessage.error(err.message || '注册失败')
    refreshCaptcha()   // 注册失败后刷新验证码
  } finally {
    loading.value = false
  }
}

const goLogin = () => {
  router.push('/login')
}

onMounted(() => {
  refreshCaptcha()
})
</script>

<style scoped>
/* 1. 容器设置为全屏，并直接承载背景图 */
.register-container {
  position: relative;
  width: 100vw;
  height: 100vh;
  overflow: hidden;
  /* 核心：背景图铺满整个容器 */
  background: url('/images/login-illustration.png') center center / cover no-repeat;
}

/* 2. 黑色遮罩层，用于压暗背景，提高文字可读性 */
.register-bg {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  /* 这里的透明度可以根据需要微调，0.2-0.4 比较合适 */
  background-color: rgba(0, 0, 0, 0.25); 
  z-index: 1;
}

/* 3. 左侧内容区 */
.left-section {
  position: absolute;
  top: 0;
  left: 0;
  width: 50%; /* 占据左半边 */
  height: 100%;
  z-index: 2; /* 在遮罩层之上 */
  display: flex;
  align-items: center;
  padding-left: 8%;
  box-sizing: border-box;
}

.content-wrapper {
  color: #fff;
  animation: fadeInUp 1s ease; /* 简单的进入动画 */
}

.logo-img {
  width: 320px;
  margin-bottom: 24px;
  border-radius: 12px;
  /* 给Logo加一点阴影，防止和背景图混在一起 */
  box-shadow: 0 8px 24px rgba(0,0,0,0.2); 
}

.title {
  font-size: 48px;
  font-weight: 700;
  margin: 0 0 16px 0;
  /* 文字阴影增强对比度 */
  text-shadow: 0 4px 12px rgba(0,0,0,0.3);
}

.subtitle {
  font-size: 22px;
  opacity: 0.9;
  margin: 0;
  text-shadow: 0 2px 8px rgba(0,0,0,0.2);
}

/* 4. 右侧表单区 (关键修改区域) */
.right-section {
  position: absolute;
  top: 0;
  right: 0;
  width: 480px; /* 固定宽度 */
  height: 100%;
  z-index: 2; /* 在遮罩层之上 */
  display: flex;
  align-items: center;
  justify-content: center;
  /* 关键：移除原来的背景色，或者设为透明 */
  background: transparent;
}

/* 登录卡片容器：实现半透明毛玻璃效果 */
.login-card {
  width: 100%;
  padding: 50px 40px;
  margin: 0 20px; /* 稍微留点边距 */
  /* 
     核心样式：
     rgba(255, 255, 255, 0.85): 85% 不透明度的白色
     backdrop-filter: blur(10px): 背景模糊，实现毛玻璃
  */
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px); /* Safari 兼容 */
  
  border-radius: 16px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1); /* 柔和的阴影 */
  border: 1px solid rgba(255, 255, 255, 0.3); /* 可选：增加一点边框质感 */
  animation: fadeInRight 1s ease;
}

.form-header {
  text-align: center;
  margin-bottom: 30px;
}

.form-header h2 {
  margin: 0;
  font-size: 28px;
  color: #333;
  font-weight: 600;
}

.form-header p {
  margin-top: 8px;
  color: #666;
  font-size: 14px;
}

/* 按钮样式微调 */
.register-btn {
  width: 100%;
  height: 48px;
  font-size: 16px;
  margin-top: 10px;
  /* 如果登录页是渐变色，这里保持一致 */
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
}

.register-links {
  text-align: center;
  margin-top: 20px;
}

/* 验证码布局 */
.check-code-wrapper, .email-code-wrapper {
  display: flex;
  gap: 10px;
  align-items: center;
}

.check-code-input {
  flex: 1;
}

.captcha-image {
  height: 44px;
  border-radius: 4px;
  cursor: pointer;
  border: 1px solid #dcdfe6;
}

.code-btn {
  flex-shrink: 0;
  width: 110px;
}

/* 简单的动画定义 */
@keyframes fadeInUp {
  from { opacity: 0; transform: translateY(30px); }
  to { opacity: 1; transform: translateY(0); }
}
@keyframes fadeInRight {
  from { opacity: 0; transform: translateX(30px); }
  to { opacity: 1; transform: translateX(0); }
}
</style>