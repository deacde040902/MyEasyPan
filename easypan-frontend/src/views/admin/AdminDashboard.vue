<template>
  <div class="admin-dashboard">
    <!-- 顶部导航 -->
    <div class="admin-header">
      <div class="header-left">
        <h1>Easy云盘 - 管理员后台</h1>
      </div>
      <div class="header-right">
        <span class="admin-name">{{ userInfo?.nickName || userInfo?.email }}</span>
        <el-button link @click="handleLogout">退出登录</el-button>
      </div>
    </div>

    <!-- 侧边栏 -->
    <div class="admin-sidebar">
      <el-menu
        :default-active="activeMenu"
        class="sidebar-menu"
        @select="handleMenuSelect"
      >
        <el-menu-item index="dashboard">
          <el-icon><HomeFilled /></el-icon>
          <span>仪表盘</span>
        </el-menu-item>
        <el-menu-item index="users">
          <el-icon><User /></el-icon>
          <span>用户管理</span>
        </el-menu-item>
        <el-menu-item index="files">
          <el-icon><Folder /></el-icon>
          <span>文件管理</span>
        </el-menu-item>
        <el-menu-item index="settings">
          <el-icon><Setting /></el-icon>
          <span>系统设置</span>
        </el-menu-item>
      </el-menu>
    </div>

    <!-- 主内容区 -->
    <div class="admin-content">
      <div class="content-header">
        <h2>{{ pageTitle }}</h2>
      </div>
      <div class="content-body">
        <!-- 仪表盘统计 -->
        <div class="stats-grid" v-if="activeMenu === 'dashboard'">
          <el-card class="stat-card">
            <div class="stat-content">
              <div class="stat-number">{{ totalUsers }}</div>
              <div class="stat-label">总用户数</div>
            </div>
          </el-card>
          <el-card class="stat-card">
            <div class="stat-content">
              <div class="stat-number">{{ totalFiles }}</div>
              <div class="stat-label">总文件数</div>
            </div>
          </el-card>
          <el-card class="stat-card">
            <div class="stat-content">
              <div class="stat-number">{{ totalStorage }}</div>
              <div class="stat-label">总存储空间</div>
            </div>
          </el-card>
          <el-card class="stat-card">
            <div class="stat-content">
              <div class="stat-number">{{ vipUsers }}</div>
              <div class="stat-label">VIP用户数</div>
            </div>
          </el-card>
        </div>

        <!-- 用户管理 -->
        <div v-if="activeMenu === 'users'" class="users-section">
          <div class="users-header">
            <el-input
              v-model="searchKeyword"
              placeholder="搜索用户（邮箱/昵称）"
              prefix-icon="Search"
              @keyup.enter="fetchUsers"
            />
            <el-button type="primary" @click="fetchUsers">搜索</el-button>
          </div>
          <el-table
            :data="userList"
            style="width: 100%"
            border
          >
            <el-table-column prop="userId" label="用户ID" width="180" />
            <el-table-column prop="nickName" label="昵称" />
            <el-table-column prop="email" label="邮箱" />
            <el-table-column prop="vipLevel" label="VIP等级">
              <template #default="{ row }">
                {{ row.vipLevel === 1 ? 'VIP' : '普通用户' }}
              </template>
            </el-table-column>
            <el-table-column prop="status" label="状态">
              <template #default="{ row }">
                <el-switch
                  v-model="row.switchStatus"
                  @change="updateUserStatus(row.userId, row.switchStatus)"
                />
              </template>
            </el-table-column>
            <el-table-column prop="totalSpace" label="存储空间">
              <template #default="{ row }">
                {{ formatFileSize(row.totalSpace) }}
              </template>
            </el-table-column>
            <el-table-column label="操作">
              <template #default="{ row }">
                <el-button link @click="editUserSpace(row)">修改空间</el-button>
              </template>
            </el-table-column>
          </el-table>
          <div class="pagination">
            <el-pagination
              v-model:current-page="currentPage"
              v-model:page-size="pageSize"
              :page-sizes="[10, 20, 50, 100]"
              layout="total, sizes, prev, pager, next, jumper"
              :total="totalUsers"
              @size-change="handleSizeChange"
              @current-change="handleCurrentChange"
            />
          </div>
        </div>

        <!-- 文件管理 -->
        <div v-if="activeMenu === 'files'" class="files-section">
          <div class="files-header">
            <el-input
              v-model="fileSearchKeyword"
              placeholder="搜索文件"
              prefix-icon="Search"
              @keyup.enter="fetchFiles"
            />
            <el-button type="primary" @click="fetchFiles">搜索</el-button>
          </div>
          <el-table
            :data="fileList"
            style="width: 100%"
            border
          >
            <el-table-column prop="fileId" label="文件ID" width="180" />
            <el-table-column prop="fileName" label="文件名" />
            <el-table-column prop="userId" label="用户ID" width="180" />
            <el-table-column prop="fileSize" label="大小">
              <template #default="{ row }">
                {{ formatFileSize(row.fileSize) }}
              </template>
            </el-table-column>
            <el-table-column prop="createTime" label="创建时间" width="180" />
            <el-table-column label="操作">
              <template #default="{ row }">
                <el-button link @click="downloadFile(row)">下载</el-button>
                <el-button link type="danger" @click="deleteFile(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
          <div class="pagination">
            <el-pagination
              v-model:current-page="fileCurrentPage"
              v-model:page-size="filePageSize"
              :page-sizes="[10, 20, 50, 100]"
              layout="total, sizes, prev, pager, next, jumper"
              :total="totalFiles"
              @size-change="handleFileSizeChange"
              @current-change="handleFileCurrentChange"
            />
          </div>
        </div>

        <!-- 系统设置 -->
        <div v-if="activeMenu === 'settings'" class="settings-section">
          <el-card>
            <h3>系统设置</h3>
            <el-form label-width="120px">
              <el-form-item label="系统名称">
                <el-input v-model="systemSettings.systemName" />
              </el-form-item>
              <el-form-item label="默认存储空间">
                <el-input v-model="systemSettings.defaultSpace" suffix="GB" />
              </el-form-item>
              <el-form-item label="VIP存储空间">
                <el-input v-model="systemSettings.vipSpace" suffix="GB" />
              </el-form-item>
              <el-form-item>
                <el-button type="primary" @click="saveSettings">保存设置</el-button>
              </el-form-item>
            </el-form>
          </el-card>
        </div>
      </div>
    </div>

    <!-- 修改空间对话框 -->
    <el-dialog v-model="editSpaceDialogVisible" title="修改用户存储空间">
      <el-form :model="editSpaceForm">
        <el-form-item label="用户ID">
          <el-input v-model="editSpaceForm.userId" disabled />
        </el-form-item>
        <el-form-item label="当前空间">
          <el-input v-model="editSpaceForm.currentSpace" suffix="GB" disabled />
        </el-form-item>
        <el-form-item label="新空间大小">
          <el-input v-model="editSpaceForm.newSpace" suffix="GB" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editSpaceDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitEditSpace">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  HomeFilled,
  User,
  Folder,
  Setting
} from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'
import { logout } from '@/api/user'
import {
  getUserList,
  changeUserStatus,
  changeUserSpace,
  getFileList,
  deleteFile as adminDeleteFile,
  downloadFile as adminDownloadFile,
  getDashboardStats,
  saveSystemSettings
} from '@/api/admin'

const router = useRouter()
const userStore = useUserStore()

// 从用户状态管理获取信息
const userInfo = computed(() => userStore.userInfo)

// 页面状态
const activeMenu = ref('dashboard')
const pageTitle = computed(() => {
  const titles = {
    dashboard: '仪表盘',
    users: '用户管理',
    files: '文件管理',
    settings: '系统设置'
  }
  return titles[activeMenu.value as keyof typeof titles] || '仪表盘'
})

// 统计数据
const totalUsers = ref(0)
const totalFiles = ref(0)
const totalStorage = ref('0GB')
const vipUsers = ref(0)

// 用户管理
interface User {
  userId: string
  nickName: string
  email: string
  vipLevel: number
  status: number
  totalSpace: number
  switchStatus: boolean
}
const userList = ref<User[]>([])
const searchKeyword = ref('')
const currentPage = ref(1)
const pageSize = ref(10)

// 文件管理
const fileList = ref([])
const fileSearchKeyword = ref('')
const fileCurrentPage = ref(1)
const filePageSize = ref(10)

// 系统设置
const systemSettings = reactive({
  systemName: 'Easy云盘',
  defaultSpace: '5',
  vipSpace: '100'
})

// 修改空间对话框
const editSpaceDialogVisible = ref(false)
const editSpaceForm = reactive({
  userId: '',
  currentSpace: '',
  newSpace: ''
})

// 处理菜单选择
const handleMenuSelect = (index: string) => {
  activeMenu.value = index
  if (index === 'users') {
    fetchUsers()
  } else if (index === 'files') {
    fetchFiles()
  }
}

// 获取用户列表
const fetchUsers = async () => {
  try {
    const res = await getUserList({
      page: currentPage.value,
      pageSize: pageSize.value,
      keyword: searchKeyword.value
    })
    const users = res.data.records || []
    // 为每个用户添加 switchStatus 属性，避免页面加载时触发状态修改
    userList.value = users.map((user: any) => ({
      ...user,
      switchStatus: user.status === 1
    }))
    totalUsers.value = res.data.total || 0
  } catch (error) {
    console.error('获取用户列表失败:', error)
    ElMessage.error('获取用户列表失败')
  }
}

// 获取文件列表
const fetchFiles = async () => {
  try {
    const res = await getFileList({
      page: fileCurrentPage.value,
      pageSize: filePageSize.value,
      keyword: fileSearchKeyword.value
    })
    fileList.value = res.data.records || []
    totalFiles.value = res.data.total || 0
  } catch (error) {
    console.error('获取文件列表失败:', error)
    ElMessage.error('获取文件列表失败')
  }
}

// 修改用户状态
const updateUserStatus = async (userId: string, status: boolean) => {
  try {
    // 将布尔值转换为整数（true -> 1, false -> 0）
    const statusInt = status ? 1 : 0
    
    // 检查用户是否是管理员，如果是管理员，则不允许修改其状态
    const currentUserId = localStorage.getItem('userId')
    if (userId === currentUserId) {
      ElMessage.error('不能修改自己的状态')
      // 恢复原始状态
      const user = userList.value.find(u => u.userId === userId)
      if (user) {
        user.switchStatus = !status
      }
      return
    }
    
    await changeUserStatus(userId, statusInt)
    
    // 更新用户列表中的状态
    const user = userList.value.find(u => u.userId === userId)
    if (user) {
      user.status = statusInt
      user.switchStatus = status
    }
    
    // 显示成功提示
    ElMessage({
      message: `用户 ${userId} 状态已修改为 ${statusInt === 1 ? '启用' : '禁用'}`,
      type: 'success',
      duration: 2000
    })
  } catch (error) {
    console.error('修改用户状态失败:', error)
    ElMessage.error('修改用户状态失败')
    // 恢复原始状态
    const user = userList.value.find(u => u.userId === userId)
    if (user) {
      user.switchStatus = user.status === 1
    }
  }
}

// 编辑用户空间
const editUserSpace = (user: any) => {
  editSpaceForm.userId = user.userId
  editSpaceForm.currentSpace = (user.totalSpace / (1024 * 1024 * 1024)).toString()
  editSpaceForm.newSpace = editSpaceForm.currentSpace
  editSpaceDialogVisible.value = true
}

// 提交修改空间
const submitEditSpace = async () => {
  try {
    const totalSpace = parseFloat(editSpaceForm.newSpace) * 1024 * 1024 * 1024
    await changeUserSpace(editSpaceForm.userId, totalSpace)
    ElMessage.success(`用户 ${editSpaceForm.userId} 存储空间已修改为 ${editSpaceForm.newSpace}GB`)
    editSpaceDialogVisible.value = false
    // 重新加载用户列表
    fetchUsers()
  } catch (error) {
    console.error('修改用户空间失败:', error)
    ElMessage.error('修改用户空间失败')
  }
}

// 下载文件
const downloadFile = async (file: any) => {
  try {
    const blob = await adminDownloadFile(file.fileId)
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = file.fileName
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    URL.revokeObjectURL(url)
    ElMessage.success(`文件 ${file.fileName} 下载成功`)
  } catch (error) {
    console.error('下载文件失败:', error)
    ElMessage.error('下载文件失败')
  }
}

// 删除文件
const deleteFile = async (file: any) => {
  try {
    await adminDeleteFile(file.fileId)
    ElMessage.success(`文件 ${file.fileName} 已删除`)
    // 重新加载文件列表
    fetchFiles()
  } catch (error) {
    console.error('删除文件失败:', error)
    ElMessage.error('删除文件失败')
  }
}

// 保存设置
const saveSettings = async () => {
  try {
    await saveSystemSettings(systemSettings)
    ElMessage.success('系统设置已保存')
  } catch (error) {
    console.error('保存系统设置失败:', error)
    ElMessage.error('保存系统设置失败')
  }
}

// 处理分页
const handleSizeChange = (size: number) => {
  pageSize.value = size
  fetchUsers()
}

const handleCurrentChange = (page: number) => {
  currentPage.value = page
  fetchUsers()
}

const handleFileSizeChange = (size: number) => {
  filePageSize.value = size
  fetchFiles()
}

const handleFileCurrentChange = (page: number) => {
  fileCurrentPage.value = page
  fetchFiles()
}

// 格式化文件大小
const formatFileSize = (size: number): string => {
  if (size < 1024) {
    return size + 'B'
  } else if (size < 1024 * 1024) {
    return (size / 1024).toFixed(2) + 'KB'
  } else if (size < 1024 * 1024 * 1024) {
    return (size / (1024 * 1024)).toFixed(2) + 'MB'
  } else {
    return (size / (1024 * 1024 * 1024)).toFixed(2) + 'GB'
  }
}

// 退出登录
const handleLogout = async () => {
  try {
    await logout()
  } catch (err) {
    console.error('退出登录接口失败，强制跳转:', err)
  } finally {
    localStorage.removeItem('token')
    localStorage.removeItem('userId')
    localStorage.removeItem('isAdmin')
    userStore.clearUserInfo()
    ElMessage.success('已退出登录')
    router.push('/login')
  }
}

// 获取仪表盘统计数据
const fetchDashboardStats = async () => {
  try {
    console.log('开始获取仪表盘统计数据')
    const res = await getDashboardStats()
    console.log('获取仪表盘统计数据成功:', res)
    if (res && res.code === 200 && res.data) {
      totalUsers.value = res.data.totalUsers || 0
      totalFiles.value = res.data.totalFiles || 0
      totalStorage.value = res.data.totalStorage || '0GB'
      vipUsers.value = res.data.vipUsers || 0
      console.log('更新后的数据:', {
        totalUsers: totalUsers.value,
        totalFiles: totalFiles.value,
        totalStorage: totalStorage.value,
        vipUsers: vipUsers.value
      })
    } else {
      console.error('获取仪表盘统计数据失败：响应格式不正确', res)
    }
  } catch (error) {
    console.error('获取仪表盘统计数据失败:', error)
  }
}

onMounted(async () => {
  console.log('onMounted function started')
  // 检查是否是管理员
  const isAdmin = localStorage.getItem('isAdmin')
  console.log('isAdmin:', isAdmin)
  if (!isAdmin) {
    ElMessage.error('您不是管理员，无权访问此页面')
    router.push('/login')
    return
  }
  
  // 加载用户信息
  userStore.loadFromLocalStorage()
  userStore.fetchUserInfo()
  
  // 加载真实数据
  await fetchDashboardStats()
  await fetchUsers()
  await fetchFiles()
  
  console.log('onMounted function ended')
})
</script>

<style scoped>
.admin-dashboard {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background-color: #f5f7fa;
}

.admin-header {
  height: 60px;
  background: white;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 20px;
  z-index: 100;
}

.header-left h1 {
  margin: 0;
  font-size: 20px;
  color: #333;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.admin-name {
  font-size: 14px;
  color: #666;
}

.admin-sidebar {
  width: 200px;
  background: white;
  border-right: 1px solid #e4e7ed;
  height: calc(100vh - 60px);
  position: fixed;
  left: 0;
  top: 60px;
  overflow-y: auto;
}

.sidebar-menu {
  border-right: none;
  height: 100%;
}

.admin-content {
  flex: 1;
  margin-left: 200px;
  padding: 20px;
  overflow-y: auto;
  height: calc(100vh - 60px);
}

.content-header {
  margin-bottom: 20px;
}

.content-header h2 {
  margin: 0;
  font-size: 20px;
  color: #333;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 20px;
  margin-bottom: 30px;
}

.stat-card {
  border-radius: 8px;
  overflow: hidden;
}

.stat-content {
  text-align: center;
  padding: 20px;
}

.stat-number {
  font-size: 24px;
  font-weight: bold;
  color: #409EFF;
  margin-bottom: 8px;
}

.stat-label {
  font-size: 14px;
  color: #666;
}

.users-section,
.files-section,
.settings-section {
  background: white;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

.users-header,
.files-header {
  display: flex;
  gap: 10px;
  margin-bottom: 20px;
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

@media (max-width: 768px) {
  .admin-sidebar {
    width: 100px;
  }
  
  .admin-content {
    margin-left: 100px;
  }
  
  .stats-grid {
    grid-template-columns: 1fr;
  }
}
</style>