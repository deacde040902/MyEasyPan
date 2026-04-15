<template>
  <div class="profile-container">
    <el-card class="profile-card">
      <div class="profile-header">
        <!-- 修复：添加时间戳参数，强制刷新浏览器缓存 -->
        <el-avatar
          :key="avatarKey"
          :size="100"
          :src="getAvatarUrl(userInfo?.userId)"
          @click="triggerAvatarUpload"
          style="cursor: pointer;"
        />
        <input
          ref="avatarInput"
          type="file"
          accept="image/png,image/jpeg,image/jpg"
          style="display: none"
          @change="handleAvatarChange"
        />
        
        <div class="nickname-area">
          <h2>{{ userInfo?.nickName || userInfo?.email }}</h2>
          <el-button link @click="openEditNicknameDialog" :icon="Edit">编辑</el-button>
        </div>
        
        <div class="vip-status">
          <span v-if="userVipStatus.effectiveVip" class="vip-badge">
            <span class="vip-icon">👑</span>
            VIP会员
          </span>
          <span v-else class="normal-badge">普通会员</span>
        </div>
      </div>
      
      <div class="profile-info">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="用户ID">{{ userInfo?.userId }}</el-descriptions-item>
          <el-descriptions-item label="邮箱">{{ userInfo?.email }}</el-descriptions-item>
          <el-descriptions-item label="注册时间">{{ formatDate(userInfo?.createTime) }}</el-descriptions-item>
          <el-descriptions-item label="已用空间">{{ usedSpace }} / {{ totalSpace }}</el-descriptions-item>
        </el-descriptions>
      </div>
      
      <div class="profile-actions">
        <el-button type="primary" @click="router.push('/files')">返回文件管理</el-button>
      </div>
    </el-card>

    <el-dialog v-model="editNicknameDialogVisible" title="修改昵称" width="400px">
      <el-form :model="nicknameForm" :rules="nicknameRules" ref="nicknameFormRef">
        <el-form-item label="新昵称" prop="newNickName">
          <el-input v-model="nicknameForm.newNickName" placeholder="请输入1-20个字符" maxlength="20" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editNicknameDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitNicknameChange" :loading="nicknameLoading">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, FormInstance, FormRules } from 'element-plus'
import { Edit } from '@element-plus/icons-vue'
import { getUserInfo, getUseSpace, uploadAvatar, updateNickName } from '@/api/user'
import { useUserStore } from '@/store/user'

const router = useRouter()
const userStore = useUserStore() // 修复：引入全局用户状态

// 从用户状态管理获取信息
const userInfo = computed(() => userStore.userInfo)
const userVipStatus = computed(() => userStore.vipStatus)
const usedSpace = computed(() => userStore.spaceInfo.useSpaceStr)
const totalSpace = computed(() => userStore.spaceInfo.totalSpaceStr)

const avatarInput = ref<HTMLInputElement>()
const avatarKey = ref(0)

const editNicknameDialogVisible = ref(false)
const nicknameLoading = ref(false)
const nicknameFormRef = ref<FormInstance>()
const nicknameForm = ref({ newNickName: '' })
const nicknameRules: FormRules = {
  newNickName: [
    { required: true, message: '请输入新昵称', trigger: 'blur' },
    { min: 1, max: 20, message: '昵称长度应为1-20个字符', trigger: 'blur' }
  ]
}

// 头像URL缓存，避免重复请求
const avatarUrlCache = new Map<string, string>()

const getAvatarUrl = (userId?: string) => {
  if (!userId) return '/default-avatar.png'
  // 尝试从缓存中获取
  if (avatarUrlCache.has(userId)) {
    return avatarUrlCache.get(userId)!
  }
  // 尝试从用户信息中获取头像路径
  if (userInfo.value.avatar) {
    avatarUrlCache.set(userId, userInfo.value.avatar)
    return userInfo.value.avatar
  }
  // 构造默认头像URL
  return '/default-avatar.png'
}

// 异步获取头像，用于更新缓存
const fetchAvatar = async (userId: string) => {
  try {
    const token = localStorage.getItem('token')
    if (!token) return
    
    const response = await fetch(`/api/userInfo/getAvatar?userId=${userId}&t=${Date.now()}`, {
      headers: {
        'token': token
      }
    })
    
    if (response.ok) {
      const blob = await response.blob()
      const blobUrl = URL.createObjectURL(blob)
      avatarUrlCache.set(userId, blobUrl)
      // 更新用户信息中的头像路径
      userInfo.value.avatar = blobUrl
    }
  } catch (error) {
    console.error('获取头像失败:', error)
  }
}

const fetchUserInfo = async () => {
  const userId = localStorage.getItem('userId')
  if (!userId) {
    ElMessage.error('用户ID不存在，请重新登录')
    router.push('/login')
    return
  }
  try {
    await userStore.fetchUserInfo()
  } catch (err) {
    console.error('获取用户信息失败', err)
  }
}

const fetchSpaceInfo = async () => {
  try {
    await userStore.fetchSpaceInfo()
  } catch (err) {
    console.error('获取空间信息失败', err)
  }
}

const openEditNicknameDialog = () => {
  nicknameForm.value.newNickName = userInfo.value?.nickName || ''
  editNicknameDialogVisible.value = true
}

const submitNicknameChange = async () => {
  if (!nicknameFormRef.value) return
  await nicknameFormRef.value.validate(async (valid) => {
    if (!valid) return
    nicknameLoading.value = true
    try {
      const res = await updateNickName(nicknameForm.value.newNickName)
      if (res.code === 200) {
        ElMessage.success('昵称修改成功')
        editNicknameDialogVisible.value = false
        await fetchUserInfo()
      } else {
        ElMessage.error(res.info || '修改失败')
      }
    } catch (err: any) {
      ElMessage.error(err.message || '修改失败')
    } finally {
      nicknameLoading.value = false
    }
  })
}

const triggerAvatarUpload = () => {
  avatarInput.value?.click()
}

const handleAvatarChange = async (e: Event) => {
  const file = (e.target as HTMLInputElement).files?.[0]
  if (!file) return

  const validTypes = ['image/png', 'image/jpeg', 'image/jpg']
  if (!validTypes.includes(file.type)) {
    ElMessage.error('仅支持 PNG/JPG/JPEG 格式')
    return
  }
  if (file.size > 2 * 1024 * 1024) {
    ElMessage.error('头像大小不能超过 2MB')
    return
  }

  const userId = localStorage.getItem('userId')
  if (!userId) {
    ElMessage.error('用户信息丢失，请重新登录')
    router.push('/login')
    return
  }

  try {
    const formData = new FormData()
    formData.append('file', file)
    formData.append('userId', userId)

    const res = await uploadAvatar(formData)
    if (res.code === 200) {
      ElMessage.success('头像上传成功')
      avatarKey.value = Date.now()
      await fetchUserInfo()
      await fetchSpaceInfo()
      userStore.fetchUserInfo()
    } else {
      ElMessage.error(res.info || '上传失败')
    }
  } catch (err: any) {
    console.error('头像上传失败:', err)
    ElMessage.error(err.message || '上传失败')
  } finally {
    if (avatarInput.value) avatarInput.value.value = ''
  }
}

const formatDate = (dateStr?: string) => {
  if (!dateStr) return '未知'
  return new Date(dateStr).toLocaleString()
}

onMounted(async () => {
  // 从localStorage加载VIP状态和空间信息
  userStore.loadFromLocalStorage();
  
  await fetchUserInfo()
  // 获取头像
  if (userInfo.value.userId) {
    await fetchAvatar(userInfo.value.userId)
  }
  fetchSpaceInfo()
})
</script>

<style scoped>
.profile-container {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
  background-color: #f5f7fa;
}
.profile-card {
  width: 500px;
  padding: 30px;
  text-align: center;
}
.profile-header {
  margin-bottom: 30px;
}
.nickname-area {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  margin-top: 12px;
}
.nickname-area h2 {
  margin: 0;
}
.profile-info {
  margin-bottom: 30px;
  text-align: left;
}

.vip-status {
  margin-top: 12px;
}

.vip-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  background: linear-gradient(135deg, #ffd700, #ff8c00);
  color: #fff;
  padding: 4px 12px;
  border-radius: 20px;
  font-weight: bold;
  font-size: 14px;
}

.vip-icon {
  font-size: 16px;
}

.normal-badge {
  color: #666;
  font-size: 14px;
}
</style>