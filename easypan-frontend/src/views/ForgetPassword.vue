<template>
  <div class="forget-container">
    <!-- 1. 全屏背景层 -->
    <div class="global-bg"></div>

    <!-- 2. 左侧品牌展示区 -->
    <div class="left-section">
      <div class="content-wrapper">
        <img src="/images/login-illustration.png" alt="logo" class="logo-img" />
        <h1 class="title">Easy云盘</h1>
        <p class="subtitle">安全、高效的云端文件存储平台</p>
      </div>
    </div>

    <!-- 3. 右侧表单区 (半透明卡片) -->
    <div class="right-section">
      <div class="form-card">
        <div class="form-header">
          <h2>找回密码</h2>
          <p>请输入您的邮箱完成验证</p>
        </div>

        <el-form
          ref="forgetFormRef"
          :model="forgetForm"
          :rules="rules"
          class="form-content"
          label-position="top"
        >
          <!-- 邮箱 -->
          <el-form-item prop="email" label="注册邮箱">
            <el-input
              v-model="forgetForm.email"
              placeholder="请输入注册邮箱"
              prefix-icon="Message"
              clearable
            />
          </el-form-item>

          <!-- 图片验证码 -->
          <el-form-item prop="checkCode" label="图片验证码">
            <div class="code-group">
              <el-input
                v-model="forgetForm.checkCode"
                placeholder="请输入验证码"
                prefix-icon="Picture"
              />
              <img
                :src="checkCodeUrl"
                @click="refreshCheckCode"
                class="code-img"
                alt="点击刷新"
                title="点击刷新验证码"
              />
            </div>
          </el-form-item>

          <!-- 邮箱验证码 -->
          <el-form-item prop="emailCode" label="邮箱验证码">
            <div class="code-group">
              <el-input
                v-model="forgetForm.emailCode"
                placeholder="请输入邮箱验证码"
                prefix-icon="Bell"
              />
              <el-button
                type="primary"
                :disabled="codeDisabled"
                @click="handleSendEmailCode"
                class="code-btn"
              >
                {{ codeText }}
              </el-button>
            </div>
          </el-form-item>

          <!-- 新密码 -->
          <el-form-item prop="newPassword" label="新密码">
            <el-input
              v-model="forgetForm.newPassword"
              placeholder="请输入新密码"
              prefix-icon="Lock"
              show-password
            />
          </el-form-item>

          <!-- 确认密码 -->
          <el-form-item prop="confirmPassword" label="确认密码">
            <el-input
              v-model="forgetForm.confirmPassword"
              placeholder="请再次输入新密码"
              prefix-icon="Lock"
              show-password
            />
          </el-form-item>

          <!-- 提交按钮 -->
          <el-button
            type="primary"
            class="submit-btn"
            :loading="loading"
            @click="handleSubmit"
          >
            重置密码
          </el-button>

          <!-- 返回登录 -->
          <div class="back-login">
            <el-link type="primary" @click="goToLogin" :underline="false">
              返回登录
            </el-link>
          </div>
        </el-form>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { getCheckCode, sendEmailCode, resetPwd } from '@/api/user'

const router = useRouter()
const forgetFormRef = ref<FormInstance>()
const loading = ref(false)

// 表单数据
const forgetForm = reactive({
  email: '',
  checkCode: '',
  emailCode: '',
  newPassword: '',
  confirmPassword: ''
})

// 图片验证码
const checkCodeUrl = ref('')
const checkCodeKey = ref('')

// 刷新验证码
const refreshCheckCode = async () => {
  try {
    const res = await getCheckCode()
    checkCodeUrl.value = res.data.checkCode
    checkCodeKey.value = res.data.checkCodeKey
  } catch (err) {
    console.error('验证码加载失败:', err)
    ElMessage.error('验证码加载失败')
  }
}

// 倒计时逻辑
const codeText = ref('获取验证码')
const codeDisabled = ref(false)
let timer: NodeJS.Timeout | null = null

const handleSendEmailCode = async () => {
  // 简单校验邮箱
  if (!forgetForm.email) {
    ElMessage.warning('请先输入邮箱')
    return
  }
  if (!forgetForm.checkCode) {
    ElMessage.warning('请先输入图片验证码')
    return
  }

  try {
    // 调用 API (API 需要 { email, checkCode } 对象)
    await sendEmailCode({
      email: forgetForm.email,
      checkCode: forgetForm.checkCode,
      checkCodeKey: checkCodeKey.value,   // 从刷新验证码时保存的 key
      type: '1'    
    })
    ElMessage.success('验证码发送成功')

    // 开启倒计时
    codeDisabled.value = true
    let count = 60
    codeText.value = `${count}s`
    timer = setInterval(() => {
      count--
      codeText.value = `${count}s`
      if (count <= 0 && timer) {
        clearInterval(timer)
        codeDisabled.value = false
        codeText.value = '重新获取'
      }
    }, 1000)
  } catch (err: any) {
    console.error('发送验证码失败:', err)
    ElMessage.error(err.info || err.message || '发送失败')
    refreshCheckCode()
  }
}

// 表单验证规则
const rules: FormRules = {
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '邮箱格式不正确', trigger: 'blur' }
  ],
  checkCode: [{ required: true, message: '请输入图片验证码', trigger: 'blur' }],
  emailCode: [{ required: true, message: '请输入邮箱验证码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于6位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    {
      // 修复：自定义验证器参数类型
      validator: (_rule: any, value: string, callback: Function) => {
        if (value !== forgetForm.newPassword) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

// 提交重置
const handleSubmit = async () => {
  if (!forgetFormRef.value) return
  try {
    await forgetFormRef.value.validate()
    loading.value = true

    // 调用重置密码 API
    // 根据后端接口，需要传递 checkCode 参数
    await resetPwd({
      email: forgetForm.email,
      emailCode: forgetForm.emailCode,
      newPwd: forgetForm.newPassword,
      confirmPwd: forgetForm.confirmPassword,
      checkCode: forgetForm.checkCode,
      checkCodeKey: checkCodeKey.value
    })

    ElMessage.success('密码重置成功')
    router.push('/login')
  } catch (err: any) {
    console.error('重置密码失败:', err)
    ElMessage.error(err.info || err.message || '重置失败')
  } finally {
    loading.value = false
  }
}

const goToLogin = () => {
  router.push('/login')
}

// 初始化
onMounted(() => {
  refreshCheckCode()
})
</script>

<style scoped>
/* 1. 容器全屏背景 */
.forget-container {
  display: flex;
  width: 100vw;
  height: 100vh;
  overflow: hidden;
  position: relative;
  /* 使用你的背景图，确保路径正确 */
  background: url('/images/login-illustration.png') center/cover no-repeat;
}

/* 2. 背景遮罩层 (可选，增加文字可读性) */
.global-bg {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  /* 轻微的黑色遮罩，让白色文字更清晰 */
  background: rgba(0, 0, 0, 0.15);
  z-index: 1;
  pointer-events: none;
}

/* 3. 左侧内容 */
.left-section {
  position: relative;
  z-index: 2;
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  padding-left: 8%;
}

.content-wrapper {
  text-align: left;
  animation: fadeInUp 1s ease;
}

.logo-img {
  width: 320px;
  margin-bottom: 24px;
  border-radius: 12px;
  /* 给Logo加个阴影防止和背景混在一起 */
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.2);
}

.title {
  font-size: 48px;
  font-weight: 700;
  margin: 0 0 16px;
  text-shadow: 0 2px 8px rgba(0, 0, 0, 0.3);
}

.subtitle {
  font-size: 20px;
  opacity: 0.9;
  margin: 0;
  text-shadow: 0 2px 6px rgba(0, 0, 0, 0.2);
}

/* 4. 右侧表单区 (关键样式) */
.right-section {
  position: relative;
  z-index: 2;
  width: 480px;
  height: 100%;
  /* 使用半透明白色背景 */
  background: rgba(255, 255, 255, 0.85);
  /* 毛玻璃效果 */
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  /* 左侧阴影，增加层次感 */
  box-shadow: -8px 0 32px rgba(0, 0, 0, 0.1);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px;
}

.form-card {
  width: 100%;
  max-width: 360px;
}

.form-header {
  text-align: center;
  margin-bottom: 32px;
}

.form-header h2 {
  font-size: 28px;
  font-weight: 600;
  color: #333;
  margin: 0 0 8px;
}

.form-header p {
  font-size: 14px;
  color: #666;
  margin: 0;
}

/* 表单样式微调 */
:deep(.el-form-item__label) {
  font-weight: 500;
  color: #333;
  font-size: 14px;
}

.code-group {
  display: flex;
  gap: 10px;
  align-items: center;
}

.code-img {
  height: 40px;
  border-radius: 4px;
  cursor: pointer;
  border: 1px solid #eee;
}

.code-btn {
  flex-shrink: 0;
  width: 100px;
}

.submit-btn {
  width: 100%;
  height: 44px;
  margin-top: 10px;
  font-size: 16px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
}

.back-login {
  text-align: center;
  margin-top: 20px;
}

/* 动画 */
@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(30px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* 响应式 */
@media (max-width: 992px) {
  .left-section {
    display: none; /* 小屏幕隐藏左侧 */
  }
  .right-section {
    width: 100%;
    background: rgba(255, 255, 255, 0.95); /* 小屏幕增加不透明度以保证可读性 */
  }
}
</style>