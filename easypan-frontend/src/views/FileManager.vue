<template>
  <div class="file-manager">
    <!-- 顶部导航 -->
    <div class="top-nav">
      <div class="nav-left">
        <h1>Easy云盘</h1>
      </div>
      <div class="nav-right">
      <el-avatar
        :src="getAvatarUrl(userInfo?.userId)"
        @click="goToProfile"
        style="cursor: pointer"
        />
        <span class="username">{{ userInfo?.nickName || userInfo?.email }}</span>
        <span v-if="userVipStatus.effectiveVip" class="vip-badge-small">
          <span class="vip-icon-small">👑</span>
          VIP
        </span>
        <el-button link @click="toggleTheme" class="theme-toggle-btn">
          <el-icon :size="20">
            <component :is="isDarkTheme ? Sunny : Moon" />
          </el-icon>
        </el-button>
        <el-button link @click="handleLogout">退出</el-button>
      </div>
    </div>

    <div class="main-content">
      <!-- 左侧边栏 -->
      <div class="sidebar">
        <el-menu :default-active="activeMenu" class="sidebar-menu" @select="handleMenuSelect">
          <el-menu-item index="home">
            <el-icon><HomeFilled /></el-icon>
            <span>首页</span>
          </el-menu-item>

          <el-sub-menu index="files">
            <template #title>
              <el-icon><Folder /></el-icon>
              <span>文件</span>
            </template>
            <el-menu-item index="all">全部</el-menu-item>
            <el-menu-item index="image">图片</el-menu-item>
            <el-menu-item index="video">视频</el-menu-item>
            <el-menu-item index="audio">音频</el-menu-item>
            <el-menu-item index="document">文档</el-menu-item>
            <el-menu-item index="other">其他</el-menu-item>
          </el-sub-menu>

          <el-menu-item index="share">
            <el-icon><Share /></el-icon>
            <span>分享</span>
          </el-menu-item>

          <el-menu-item index="recycle">
            <el-icon><Delete /></el-icon>
            <span>回收站</span>
          </el-menu-item>

          <el-menu-item index="/vip">
            <el-icon><Present /></el-icon>
            <span>会员中心</span>
          </el-menu-item>
        </el-menu>

        <!-- 最近访问文件 -->
        <div class="recent-files">
          <h3>最近访问</h3>
          <ul v-if="recentFiles.length > 0">
            <li v-for="file in recentFiles" :key="file.fileId" @click="handleRecentFileClick(file)">
              <el-icon>{{ getFileIcon(file.fileSuffix) }}</el-icon>
              <span class="recent-file-name">{{ file.fileName }}</span>
            </li>
          </ul>
          <p v-else class="empty-recent">暂无最近访问文件</p>
        </div>

        <!-- 空间使用情况 -->
        <div class="space-info">
          <p>空间使用情况</p>
          <el-progress :percentage="spaceUsage" :format="formatSpace" :color="spaceColor" />
          <p class="space-text">{{ usedSpace }} / {{ totalSpace }}</p>
        </div>
      </div>

      <!-- 右侧主内容区 -->
      <div class="content-area">
        <!-- 操作栏 -->
        <div class="toolbar">
          <el-button v-if="currentPid !== '0'" @click="goToParentFolder">
            <el-icon><ArrowLeft /></el-icon>
            返回上级
          </el-button>
          <el-button type="primary" @click="triggerFileInput">
            <el-icon><Upload /></el-icon>
            上传
          </el-button>
          <input ref="fileInput" type="file" multiple style="display: none" @change="handleFileSelect" />
          <el-button type="success" @click="handleCreateFolder">
            <el-icon><FolderAdd /></el-icon>
            新建文件夹
          </el-button>
          <el-button type="danger" @click="handleBatchDelete" :disabled="!selectedFiles.length">
            <el-icon><Delete /></el-icon>
            批量删除
          </el-button>
          <el-button type="warning" @click="handleBatchMove" :disabled="!selectedFiles.length">
            <el-icon><ArrowRight /></el-icon>
            批量移动
          </el-button>

          <div class="search-box">
            <el-input
              v-model="searchKeyword"
              placeholder="搜索文件"
              @keyup.enter="handleSearch"
              clearable
            >
              <template #prefix>
                <el-icon><Search /></el-icon>
              </template>
            </el-input>
          </div>

          <el-select v-model="sortBy" @change="handleSort" placeholder="排序">
            <el-option label="按名称" value="name" />
            <el-option label="按大小" value="size" />
            <el-option label="按时间" value="time" />
          </el-select>

          <el-button @click="refreshFileList">
            <el-icon><Refresh /></el-icon>
            刷新
          </el-button>
        </div>

        <!-- 上传进度 -->
        <div v-if="uploadingFiles.size > 0" class="upload-progress">
          <h3>上传进度</h3>
          <div v-for="[fileName, progressValue] in uploadingFiles" :key="fileName" class="upload-item">
            <span>{{ fileName }}</span>
            <el-progress :percentage="progressValue" />
          </div>
        </div>

        <!-- 文件列表 -->
        <div class="file-list" @dragover.prevent @drop.prevent="handleDrop">
          <el-table
            :data="fileList"
            @selection-change="handleSelectionChange"
            @row-contextmenu="handleRowContextMenu"
            style="width: 100%"
            v-loading="loading"
            row-key="fileId"
          >
            <el-table-column type="selection" width="55" />
            <el-table-column label="文件名" min-width="300">
              <template #default="{ row }">
                <div class="file-name" @mouseenter="(e) => showPreview(row, e)" @mouseleave="hidePreview" @dblclick="handleFolderDoubleClick(row)">
                  <el-icon v-if="row.isFolder === 1" class="folder-icon" style="cursor: pointer;">
                    <Folder />
                  </el-icon>
                  <el-icon v-else class="file-icon">
                    <component :is="getFileIcon(row.fileSuffix)" />
                  </el-icon>
                  <span :style="{ cursor: row.isFolder === 1 ? 'pointer' : 'default' }">{{ row.fileName }}</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="大小" width="120">
              <template #default="{ row }">
                {{ row.fileSizeStr }}
              </template>
            </el-table-column>
            <el-table-column label="修改时间" width="180">
              <template #default="{ row }">
                {{ formatDate(row.lastOpTime) }}
              </template>
            </el-table-column>
            <el-table-column label="操作" width="350">
              <template #default="{ row }">
                <div class="file-actions">
                  <el-button link @click="handleFileDetail(row)">详情</el-button>
                  <el-button link @click="handleDownload(row)">下载</el-button>
                  <el-button link @click="handleRename(row)">重命名</el-button>
                  <el-button link @click="handleDelete(row)">删除</el-button>
                  <el-button link type="primary" @click="handleShare(row)">分享</el-button>
                </div>
              </template>
            </el-table-column>
          </el-table>

          <!-- 空状态 -->
          <div v-if="!loading && fileList.length === 0" class="empty-state">
            <el-empty description="当前目录为空">
              <el-button type="primary" @click="triggerFileInput">上传文件</el-button>
              <el-button @click="handleCreateFolder">新建目录</el-button>
            </el-empty>
          </div>

          <!-- 分页 -->
          <div v-if="fileList.length > 0" class="pagination">
            <el-pagination
              v-model:current-page="currentPage"
              v-model:page-size="pageSize"
              :page-sizes="[10, 20, 50, 100]"
              :total="totalCount"
              layout="total, sizes, prev, pager, next, jumper"
              @size-change="handleSizeChange"
              @current-change="handleCurrentChange"
            />
          </div>
        </div>
      </div>
    </div>

    <!-- 文件详情对话框 -->
    <el-dialog v-model="fileDetailVisible" title="文件详情" width="500px">
      <el-descriptions :column="1" border v-if="selectedFile">
        <el-descriptions-item label="文件名称">{{ selectedFile.fileName }}</el-descriptions-item>
        <el-descriptions-item label="文件大小">{{ selectedFile.fileSizeStr }}</el-descriptions-item>
        <el-descriptions-item label="文件类型">{{ getFileTypeName(selectedFile.fileSuffix, selectedFile.fileName) }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ formatDate(selectedFile.createTime) }}</el-descriptions-item>
        <el-descriptions-item label="修改时间">{{ formatDate(selectedFile.lastOpTime) }}</el-descriptions-item>
        <el-descriptions-item label="文件路径">{{ selectedFile.filePath || '/' }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>

    <!-- 批量移动对话框 -->
    <el-dialog v-model="batchMoveVisible" title="批量移动" width="500px">
      <el-form label-width="80px">
        <el-form-item label="目标文件夹">
          <el-select v-model="targetFolderId" placeholder="选择目标文件夹">
            <el-option label="根目录" value="0" />
            <el-option 
              v-for="folder in folders" 
              :key="folder.fileId" 
              :label="folder.fileName" 
              :value="folder.fileId" 
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="batchMoveVisible = false">取消</el-button>
        <el-button type="primary" @click="submitBatchMove" :loading="batchMoveLoading">确定</el-button>
      </template>
    </el-dialog>

    <!-- 右键菜单 -->
    <div
      v-if="contextMenuVisible"
      class="context-menu"
      :style="{ left: contextMenuPosition.x + 'px', top: contextMenuPosition.y + 'px' }"
      @click.stop
    >
      <ul>
        <li @click="handleContextMenuClick('detail')">
          <el-icon><InfoFilled /></el-icon>
          查看详情
        </li>
        <li @click="handleContextMenuClick('download')">
          <el-icon><Download /></el-icon>
          下载
        </li>
        <li @click="handleContextMenuClick('rename')">
          <el-icon><Edit /></el-icon>
          重命名
        </li>
        <li @click="handleContextMenuClick('delete')">
          <el-icon><Delete /></el-icon>
          删除
        </li>
        <li @click="handleContextMenuClick('share')">
          <el-icon><Share /></el-icon>
          分享
        </li>
      </ul>
    </div>

    <!-- 文件预览弹窗 -->
    <div
      v-if="previewVisible"
      class="file-preview"
      :style="{ left: previewPosition.x + 'px', top: previewPosition.y + 'px' }"
    >
      <div class="preview-header">
        <span>{{ previewFile?.fileName }}</span>
        <el-button link @click="hidePreview" class="preview-close">
          <el-icon><Close /></el-icon>
        </el-button>
      </div>
      <div class="preview-content">
        <div v-if="previewLoading" class="preview-loading">
          <el-icon class="is-loading"><Loading /></el-icon>
          <span>加载中...</span>
        </div>
        <div v-else-if="previewError" class="preview-error">
          {{ previewError }}
        </div>
        <img
          v-else-if="isImageFile(previewFile?.fileSuffix)"
          :src="previewUrl"
          class="preview-image"
          alt="预览"
        />
        <video
          v-else-if="isVideoFile(previewFile?.fileSuffix)"
          :src="previewUrl"
          class="preview-video"
          controls
          autoplay
          muted
        ></video>
        <audio
          v-else-if="isAudioFile(previewFile?.fileSuffix)"
          :src="previewUrl"
          class="preview-audio"
          controls
        ></audio>
        <div v-else-if="isTextFile(previewFile?.fileSuffix)" class="preview-text">
          <pre>{{ previewText }}</pre>
        </div>
        <div v-else class="preview-not-supported">
          不支持在线预览此文件类型
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRouter } from 'vue-router'
import {
  HomeFilled,
  Folder,
  Share,
  Delete,
  Upload,
  FolderAdd,
  ArrowRight,
  ArrowLeft,
  Search,
  Refresh,
  Document,
  Picture,
  VideoCamera,
  Mic,
  Files,
  Box,
  Present,
  Sunny,
  Moon,
  InfoFilled,
  Download,
  Edit,
  Close,
  Loading
} from '@element-plus/icons-vue'
import { getUserInfo, getUseSpace, logout } from '@/api/user'
import {
  loadDataList,
  createFolder,
  deleteFile,
  renameFile,
  moveFile,
  initChunkUpload,
  uploadChunk,
  mergeChunk,
  getUploadedChunks,
  createDownloadUrl,
  downloadFile,
  createShare
} from '@/api/file'
import type { FileInfo } from '@/api/file'
import SparkMD5 from 'spark-md5'
import { useUserStore } from '@/store/user'
const userStore = useUserStore()

// 从用户状态管理获取信息
const userInfo = computed(() => userStore.userInfo)
const userVipStatus = computed(() => userStore.vipStatus)
const spaceInfo = computed(() => userStore.spaceInfo)
const spaceUsage = computed(() => {
  if (!spaceInfo.value.totalSpace) return 0
  return Math.round((spaceInfo.value.useSpace / spaceInfo.value.totalSpace) * 100)
})
const usedSpace = computed(() => spaceInfo.value.useSpaceStr)
const totalSpace = computed(() => spaceInfo.value.totalSpaceStr)

const router = useRouter()
const fileList = ref<FileInfo[]>([])
const fileInput = ref<HTMLInputElement>()
const loading = ref(false)
const selectedFiles = ref<FileInfo[]>([])
const currentPage = ref(1)
const pageSize = ref(15)
const totalCount = ref(0)
const searchKeyword = ref('')
const currentPid = ref('0')
const activeMenu = ref('home')
const uploadingFiles = ref(new Map<string, number>())
const currentCategory = ref('all')
const sortBy = ref('name')
const sortOrder = ref('asc')
const isDarkTheme = ref(localStorage.getItem('theme') === 'dark')
const fileDetailVisible = ref(false)
const selectedFile = ref<any>(null)
const recentFiles = ref<any[]>([])
const batchMoveVisible = ref(false)
const targetFolderId = ref('0')
const batchMoveLoading = ref(false)
const folders = ref<any[]>([])
const contextMenuVisible = ref(false)
const contextMenuPosition = ref({ x: 0, y: 0 })
const contextMenuRow = ref<any>(null)

// 文件预览相关变量
const previewVisible = ref(false)
const previewPosition = ref({ x: 0, y: 0 })
const previewFile = ref<any>(null)
const previewUrl = ref('')
const previewText = ref('')
const previewLoading = ref(false)
const previewError = ref('')

// 计算空间颜色
const spaceColor = (): string => {
  if (spaceUsage.value < 50) return '#67c23a'
  if (spaceUsage.value < 80) return '#e6a23c'
  return '#f56c6c'
}

const formatSpace = (percentage: number): string => `${percentage}%`

const formatDate = (dateStr: string): string => new Date(dateStr).toLocaleString()

// 处理文件排序
const handleSort = () => {
  // 切换排序顺序
  sortOrder.value = sortOrder.value === 'asc' ? 'desc' : 'asc'
  
  // 对文件列表进行排序
  fileList.value.sort((a, b) => {
    switch (sortBy.value) {
      case 'name':
        return sortOrder.value === 'asc' 
          ? a.fileName.localeCompare(b.fileName) 
          : b.fileName.localeCompare(a.fileName)
      case 'size':
        return sortOrder.value === 'asc' 
          ? (a.fileSize || 0) - (b.fileSize || 0) 
          : (b.fileSize || 0) - (a.fileSize || 0)
      case 'time':
        return sortOrder.value === 'asc' 
          ? new Date(a.lastOpTime).getTime() - new Date(b.lastOpTime).getTime() 
          : new Date(b.lastOpTime).getTime() - new Date(a.lastOpTime).getTime()
      default:
        return 0
    }
  })
}

// 切换主题
const toggleTheme = () => {
  isDarkTheme.value = !isDarkTheme.value
  localStorage.setItem('theme', isDarkTheme.value ? 'dark' : 'light')
  applyTheme()
}

// 应用主题
const applyTheme = () => {
  if (isDarkTheme.value) {
    document.documentElement.classList.add('dark-theme')
  } else {
    document.documentElement.classList.remove('dark-theme')
  }
}



// 打开文件详情
const handleFileDetail = (row: any) => {
  selectedFile.value = row
  fileDetailVisible.value = true
}

// 获取文件类型名称
const getFileTypeName = (suffix?: string, fileName?: string) => {
  if (!suffix && !fileName) return '文件夹'
  
  // 提取文件后缀名
  let fileSuffix = suffix
  if (!fileSuffix && fileName) {
    const lastDotIndex = fileName.lastIndexOf('.')
    if (lastDotIndex > 0) {
      fileSuffix = fileName.substring(lastDotIndex + 1)
    }
  }
  
  if (!fileSuffix) return '文件夹'
  
  const typeMap: Record<string, string> = {
    jpg: '图片',
    jpeg: '图片',
    png: '图片',
    gif: '图片',
    webp: '图片',
    bmp: '图片',
    svg: '图片',
    ico: '图片',
    mp4: '视频',
    avi: '视频',
    mkv: '视频',
    mov: '视频',
    wmv: '视频',
    flv: '视频',
    rmvb: '视频',
    webm: '视频',
    mp3: '音频',
    wav: '音频',
    flac: '音频',
    aac: '音频',
    ogg: '音频',
    wma: '音频',
    m4a: '音频',
    pdf: '文档',
    doc: '文档',
    docx: '文档',
    txt: '文档',
    xls: '文档',
    xlsx: '文档',
    ppt: '文档',
    pptx: '文档',
    md: '文档',
    zip: '压缩包',
    rar: '压缩包',
    '7z': '压缩包',
    exe: '其他',
    apk: '其他',
    iso: '其他',
    dmg: '其他',
    log: '其他'
  }
  return typeMap[fileSuffix.toLowerCase()] || '其他文件'
}

// 加载最近访问文件
const loadRecentFiles = () => {
  const recentFilesStr = localStorage.getItem('recentFiles')
  if (recentFilesStr) {
    try {
      recentFiles.value = JSON.parse(recentFilesStr)
    } catch (error) {
      console.error('解析最近访问文件失败:', error)
      recentFiles.value = []
    }
  }
}

// 添加最近访问文件
const addRecentFile = (file: any) => {
  // 移除已存在的相同文件
  recentFiles.value = recentFiles.value.filter(f => f.fileId !== file.fileId)
  
  // 添加到开头
  recentFiles.value.unshift(file)
  
  // 限制最多显示5个
  if (recentFiles.value.length > 5) {
    recentFiles.value = recentFiles.value.slice(0, 5)
  }
  
  // 保存到localStorage
  localStorage.setItem('recentFiles', JSON.stringify(recentFiles.value))
}

// 处理最近访问文件点击
const handleRecentFileClick = (file: any) => {
  // 这里可以添加跳转到文件所在目录的逻辑
  ElMessage.info(`打开文件: ${file.fileName}`)
  // 可以根据文件类型进行预览或下载
  if (file.isFolder === 1) {
    // 处理文件夹点击
    currentPid.value = file.fileId
    fetchFileList()
  } else {
    // 处理文件点击
    handleFileDetail(file)
  }
}

// 处理批量移动
const handleBatchMove = async () => {
  if (selectedFiles.value.length === 0) {
    ElMessage.warning('请选择要移动的文件')
    return
  }
  
  // 加载文件夹列表
  await loadFolders()
  batchMoveVisible.value = true
}

// 加载文件夹列表
const loadFolders = async () => {
  try {
    // 这里应该调用后端接口获取文件夹列表
    // 暂时使用模拟数据
    folders.value = fileList.value.filter(file => file.isFolder === 1)
  } catch (error) {
    console.error('加载文件夹列表失败:', error)
    ElMessage.error('加载文件夹列表失败')
  }
}

// 提交批量移动
const submitBatchMove = async () => {
  if (!targetFolderId.value) {
    ElMessage.warning('请选择目标文件夹')
    return
  }
  
  batchMoveLoading.value = true
  try {
    const movePromises = selectedFiles.value.map(async (file) => {
      try {
        await moveFile({ 
          fileId: file.fileId, 
          targetPid: targetFolderId.value 
        })
        return true
      } catch (error) {
        console.error(`移动文件 ${file.fileName} 失败:`, error)
        return false
      }
    })
    
    const results = await Promise.all(movePromises)
    const successCount = results.filter(Boolean).length
    
    ElMessage.success(`成功移动 ${successCount} 个文件`)
    batchMoveVisible.value = false
    selectedFiles.value = []
    fetchFileList()
  } catch (error) {
    console.error('批量移动失败:', error)
    ElMessage.error('批量移动失败')
  } finally {
    batchMoveLoading.value = false
  }
}

const handleFolderDoubleClick = (row: any) => {
  if (row.isFolder === 1) {
    currentPid.value = row.fileId
    fetchFileList()
  }
}

const goToParentFolder = () => {
  if (currentPid.value === '0') return
  
  // 直接返回根目录
  currentPid.value = '0'
  fetchFileList()
}

// 处理右键菜单
const handleRowContextMenu = (event: MouseEvent, row: any) => {
  event.preventDefault()
  contextMenuRow.value = row
  contextMenuPosition.value = {
    x: event.clientX,
    y: event.clientY
  }
  contextMenuVisible.value = true
  
  // 点击其他地方关闭右键菜单
  document.addEventListener('click', closeContextMenu)
}

// 关闭右键菜单
const closeContextMenu = () => {
  contextMenuVisible.value = false
  document.removeEventListener('click', closeContextMenu)
}

// 处理右键菜单点击
const handleContextMenuClick = (action: string) => {
  closeContextMenu()
  
  if (!contextMenuRow.value) return
  
  switch (action) {
    case 'detail':
      handleFileDetail(contextMenuRow.value)
      break
    case 'download':
      handleDownload(contextMenuRow.value)
      break
    case 'rename':
      handleRename(contextMenuRow.value)
      break
    case 'delete':
      handleDelete(contextMenuRow.value)
      break
    case 'share':
      handleShare(contextMenuRow.value)
      break
  }
}

// onMounted中同步用户信息
onMounted(async () => {
  // 从localStorage加载VIP状态和空间信息
  userStore.loadFromLocalStorage();
  
  // 应用主题
  applyTheme();
  
  // 加载最近访问文件
  loadRecentFiles();
  
  await userStore.fetchUserInfo()   
  fetchSpaceInfo()
  fetchFileList()
})

// 获取空间使用情况
const fetchSpaceInfo = async () => {
  try {
    await userStore.fetchSpaceInfo()
  } catch (error) {
    console.error('获取空间信息失败:', error)
  }
}

// 加载文件列表
const fetchFileList = async () => {
  loading.value = true
  console.log('Fetching file list with params:', {
    filePid: currentPid.value,
    category: currentCategory.value,
    fileName: searchKeyword.value,
    pageNo: currentPage.value,
    pageSize: pageSize.value
  })
  try {
    const params = {
      filePid: currentPid.value,
      category: currentCategory.value,
      fileName: searchKeyword.value,
      pageNo: currentPage.value,
      pageSize: pageSize.value
    }
    const response = await loadDataList(params)
    console.log('File list response:', response.data)

    // 使用后端返回的数据
    fileList.value = response.data.records
    totalCount.value = response.data.total
  } catch (error: any) {
    console.error('加载文件列表失败:', error)
    if (error.response?.status === 401) {
      ElMessage.error('登录已过期，请重新登录')
      localStorage.removeItem('token')
      router.push('/login')
    } else {
      ElMessage.error(error.info || '加载文件列表失败')
    }
  } finally {
    loading.value = false
  }
}

// 根据文件后缀分类文件 - 已移除，由后端处理

const refreshFileList = () => fetchFileList()

const handleSearch = () => {
  currentPage.value = 1
  fetchFileList()
}

const handleSelectionChange = (selection: FileInfo[]) => {
  selectedFiles.value = selection
}

const handleMenuSelect = (index: string) => {
  activeMenu.value = index

  // 分类菜单映射（全部、图片、视频、音频、文档、其他）
  const categoryMap: Record<string, string> = {
    all: 'all',
    image: 'image',
    video: 'video',
    audio: 'audio',
    document: 'document',
    other: 'other'
  }

  // 1. 处理具体分类菜单
  if (categoryMap[index]) {
    currentCategory.value = categoryMap[index]
    currentPid.value = '0'          // 设置为根目录，后端会查询所有子目录
    currentPage.value = 1
    fetchFileList()
    return
  }

  // 2. 处理“首页”
  if (index === 'home') {
    currentCategory.value = 'all'
    currentPid.value = '0'
    currentPage.value = 1
    fetchFileList()
    return
  }

  // 3. 处理“文件”主菜单（显示全部文件）
  if (index === 'files') {
    currentCategory.value = 'all'
    currentPid.value = '0'
    currentPage.value = 1
    fetchFileList()
    return
  }

  // 4. 其他菜单跳转（分享、回收站、会员中心）
  if (index === 'share') {
    router.push('/share')
  } else if (index === 'recycle') {
    router.push('/recycle')
  } else if (index === '/vip') {
    router.push('/vip')
  }
}

const handleSizeChange = (size: number) => {
  pageSize.value = size
  fetchFileList()
}

const handleCurrentChange = (page: number) => {
  currentPage.value = page
  fetchFileList()
}

const triggerFileInput = () => fileInput.value?.click()

const handleFileSelect = async (e: Event) => {
  const files = (e.target as HTMLInputElement).files
  if (files) {
    for (const file of Array.from(files)) {
      await uploadFile(file)
    }
  }
  if (fileInput.value) fileInput.value.value = ''
}

// 上传单个文件
const uploadFile = async (file: File) => {
  if (!file || file.type === '') {
    ElMessage.warning('请选择文件进行上传')
    return
  }
  try {
    uploadingFiles.value.set(file.name, 0)
    ElMessage.info(`开始上传 ${file.name}`)
    const fileMd5 = await calculateFileMD5(file)
    const chunkSize = 2 * 1024 * 1024
    const initRes = await initChunkUpload({
      fileMd5,
      fileName: file.name,
      fileSize: file.size,
      chunkSize,
      filePid: currentPid.value
    })
    const uploadId = initRes.data
    const chunksRes = await getUploadedChunks({ uploadId })
    const uploadedChunks = chunksRes.data || []
    const totalChunks = Math.ceil(file.size / chunkSize)
    let uploadedCount = uploadedChunks.length
    const uploadPromises: Promise<void>[] = []
    const concurrentLimit = 5

    for (let i = 0; i < totalChunks; i++) {
      if (uploadedChunks.includes(i)) continue
      const start = i * chunkSize
      const end = Math.min(start + chunkSize, file.size)
      const chunk = file.slice(start, end)
      const chunkFile = new File([chunk], `chunk-${i}`, { type: file.type })
      const promise = uploadChunk({ uploadId, chunkIndex: i, file: chunkFile }).then(() => {
        uploadedCount++
        uploadingFiles.value.set(file.name, Math.round((uploadedCount / totalChunks) * 100))
      })
      uploadPromises.push(promise)
      if (uploadPromises.length >= concurrentLimit) {
        await Promise.race(uploadPromises)
        uploadPromises.splice(0, uploadPromises.findIndex(p => p === uploadPromises[0]))
      }
    }
    await Promise.all(uploadPromises)
    await mergeChunk({ uploadId })
    uploadingFiles.value.delete(file.name)
    ElMessage.success(`${file.name} 上传成功`)
    refreshFileList()
  } catch (error: any) {
    console.error('上传失败:', error)
    uploadingFiles.value.delete(file.name)
    ElMessage.error(`上传失败: ${error.info || error.message}`)
  }
}

// 计算文件MD5
const calculateFileMD5 = (file: File): Promise<string> => {
  return new Promise((resolve, reject) => {
    const spark = new SparkMD5.ArrayBuffer()
    const chunkSize = 2 * 1024 * 1024
    let offset = 0
    const readNext = () => {
      const reader = new FileReader()
      const slice = file.slice(offset, offset + chunkSize)
      reader.onload = (e) => {
        spark.append(e.target?.result as ArrayBuffer)
        offset += chunkSize
        if (offset < file.size) readNext()
        else resolve(spark.end())
      }
      reader.onerror = () => reject(new Error('文件读取失败'))
      reader.readAsArrayBuffer(slice)
    }
    readNext()
  })
}

const getFileIcon = (suffix?: string) => {
  if (!suffix) return Folder
  const map: Record<string, any> = {
    jpg: Picture,
    jpeg: Picture,
    png: Picture,
    gif: Picture,
    bmp: Picture,
    webp: Picture,
    svg: Picture,
    ico: Picture,
    mp4: VideoCamera,
    avi: VideoCamera,
    mkv: VideoCamera,
    mov: VideoCamera,
    wmv: VideoCamera,
    flv: VideoCamera,
    rmvb: VideoCamera,
    webm: VideoCamera,
    mp3: Mic,
    wav: Mic,
    flac: Mic,
    aac: Mic,
    ogg: Mic,
    wma: Mic,
    m4a: Mic,
    pdf: Document,
    doc: Document,
    docx: Document,
    txt: Files,
    xls: Document,
    xlsx: Document,
    ppt: Document,
    pptx: Document,
    md: Files,
    zip: Box,
    rar: Box,
    '7z': Box,
    tar: Box,
    gz: Box,
    exe: Box,
    dmg: Box,
    apk: Box,
    iso: Box,
    log: Box,
    torrent: Box
  }
  return map[suffix.toLowerCase()] || Files
}

// 新建文件夹
const handleCreateFolder = async () => {
  const { value: folderName } = await ElMessageBox.prompt('请输入文件夹名称', '新建文件夹', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    inputPattern: /^.{1,50}$/,
    inputErrorMessage: '文件夹名称长度1-50个字符'
  })
  try {
    await createFolder({ filePid: currentPid.value, folderName })
    ElMessage.success('文件夹创建成功')
    refreshFileList()
  } catch (err) {
    ElMessage.error('创建文件夹失败')
    console.error(err)
  }
}

// 重命名
const handleRename = async (row: FileInfo) => {
  const { value: newName } = await ElMessageBox.prompt('请输入新的文件名', '重命名', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    inputValue: row.fileName,
    inputPattern: /.+/,
    inputErrorMessage: '文件名不能为空'
  })
  try {
    await renameFile({ fileId: row.fileId, newFileName: newName }) 
    ElMessage.success('重命名成功')
    refreshFileList()
  } catch (error) {
    console.error('重命名失败:', error)
  }
}

// 删除文件
const handleDelete = async (row: FileInfo) => {
  try {
    await ElMessageBox.confirm('确定要删除这个文件吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await deleteFile({ fileId: row.fileId })
    ElMessage.success('删除成功')
    refreshFileList()
  } catch (error) {
    if (error !== 'cancel') console.error('删除失败:', error)
  }
}

// 批量删除
const handleBatchDelete = async () => {
  try {
    await ElMessageBox.confirm(`确定要删除选中的 ${selectedFiles.value.length} 个文件吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await Promise.all(selectedFiles.value.map(file => deleteFile({ fileId: file.fileId })))
    ElMessage.success('批量删除成功')
    selectedFiles.value = []
    refreshFileList()
  } catch (error) {
    if (error !== 'cancel') console.error('批量删除失败:', error)
  }
}



// 下载文件
const handleDownload = async (row: FileInfo) => {
  if (row.isFolder === 1) {
    ElMessage.warning('暂不支持下载文件夹')
    return
  }
  try {
    // 获取token
    const token = localStorage.getItem('token')
    if (!token) {
      ElMessage.error('登录已过期，请重新登录')
      return
    }
    
    // 使用previewFile接口下载文件，这个接口是为普通用户设计的
    const response = await fetch(`/api/file/preview/${row.fileId}`, {
      headers: {
        'token': token
      }
    })
    
    if (response.ok) {
      // 创建Blob并下载
      const blob = await response.blob()
      // 确保Blob的类型是application/octet-stream，这样浏览器会下载而不是预览
      const blobWithCorrectType = new Blob([blob], { type: 'application/octet-stream' })
      const url = URL.createObjectURL(blobWithCorrectType)
      const a = document.createElement('a')
      a.href = url
      a.download = row.fileName
      document.body.appendChild(a)
      a.click()
      document.body.removeChild(a)
      URL.revokeObjectURL(url)
      
      ElMessage.success('文件下载开始')
    } else {
      ElMessage.error('下载失败')
    }
  } catch (error) {
    console.error('下载失败:', error)
    ElMessage.error('下载失败')
  }
}

// 分享文件
const handleShare = async (row: FileInfo) => {
  try {
    // 先创建分享
    const res = await createShare({
      fileId: row.fileId,
      expireType: 1 // 默认1天有效期
    }) as any
    if (res.code === 200) {
      ElMessage.success('分享创建成功')
      // 跳转到分享页面
      router.push('/share')
    } else {
      ElMessage.error(res.info || '创建分享失败')
    }
  } catch (error) {
    console.error('创建分享失败:', error)
    ElMessage.error('创建分享失败')
  }
}

// 退出登录
const handleLogout = async () => {
  try {
    await logout()
  } catch (err) {
    console.error('登出接口异常')
  } finally {
    localStorage.removeItem('token')
    localStorage.removeItem('userId')
    ElMessage.success('已退出登录')
    router.push('/login')
  }
}

// 处理拖拽
const handleDrop = (e: DragEvent) => {
  e.preventDefault()
  const files = e.dataTransfer?.files
  if (files) {
    Array.from(files).forEach(file => {
      if (file.type !== '') uploadFile(file)
    })
  }
}

const goToProfile = () => {
  router.push('/profile')
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

// 组件挂载时获取头像
onMounted(async () => {
  // 从localStorage加载VIP状态和空间信息
  userStore.loadFromLocalStorage();
  
  // 应用主题
  applyTheme();
  
  // 加载最近访问文件
  loadRecentFiles();
  
  await userStore.fetchUserInfo()
  // 获取头像
  if (userInfo.value.userId) {
    await fetchAvatar(userInfo.value.userId)
  }
  fetchSpaceInfo()
  fetchFileList()
})

// 文件预览相关函数
const showPreview = (row: any, event?: MouseEvent) => {
  if (row.isFolder === 1) return // 文件夹不预览
  
  console.log('showPreview called for:', row.fileName, 'fileId:', row.fileId)
  
  // 检查文件大小，大文件不预览
  const fileSize = row.fileSize || 0
  if (fileSize > 100 * 1024 * 1024) { // 100MB
    previewFile.value = row
    previewError.value = '文件过大，不支持在线预览'
    previewVisible.value = true
    setPreviewPosition(event)
    console.log('File too large, showing error')
    return
  }
  
  // 从文件名中提取后缀名
  const fileName = row.fileName || ''
  const lastDotIndex = fileName.lastIndexOf('.')
  const fileSuffix = lastDotIndex > -1 ? fileName.substring(lastDotIndex + 1) : ''
  
  console.log('Extracted file suffix:', fileSuffix)
  console.log('Is image file:', isImageFile(fileSuffix))
  console.log('Is video file:', isVideoFile(fileSuffix))
  console.log('Is audio file:', isAudioFile(fileSuffix))
  console.log('Is text file:', isTextFile(fileSuffix))
  
  // 创建一个新对象，包含正确的fileSuffix
  const previewRow = {
    ...row,
    fileSuffix: fileSuffix
  }
  
  previewFile.value = previewRow
  previewLoading.value = true
  previewError.value = ''
  setPreviewPosition(event)
  
  // 生成预览URL
  const token = localStorage.getItem('token') || ''
  
  console.log('Generated preview URL:', `/api/file/preview/${row.fileId}`)
  console.log('Token present:', !!token)
  
  // 对于文本文件，需要获取文本内容
  if (isTextFile(fileSuffix)) {
    console.log('Fetching text content')
    fetchTextContent(row.fileId, token)
  } else if (isImageFile(fileSuffix) || isVideoFile(fileSuffix) || isAudioFile(fileSuffix)) {
    console.log('Fetching media content')
    fetchMediaContent(row.fileId, token, fileSuffix)
  } else {
    previewLoading.value = false
    console.log('Not a supported file type, loading complete')
  }
  
  previewVisible.value = true
  console.log('Preview visible set to true')
}

const hidePreview = () => {
  previewVisible.value = false
  previewFile.value = null
  previewUrl.value = ''
  previewText.value = ''
  previewLoading.value = false
  previewError.value = ''
}

const setPreviewPosition = (event?: MouseEvent) => {
  if (event) {
    previewPosition.value = {
      x: event.clientX + 10,
      y: event.clientY + 10
    }
  } else {
    // 默认位置
    previewPosition.value = {
      x: 100,
      y: 100
    }
  }
}

const fetchTextContent = async (fileId: string, token: string) => {
  try {
    const headers: HeadersInit = {}
    if (token) {
      headers['token'] = token
    }
    
    const response = await fetch(`/api/file/preview/${fileId}`, {
      headers
    })
    
    if (!response.ok) {
      throw new Error('获取文件内容失败')
    }
    
    previewText.value = await response.text()
  } catch (error) {
    console.error('获取文件内容失败:', error)
    previewError.value = '获取文件内容失败'
  } finally {
    previewLoading.value = false
  }
}

const fetchMediaContent = async (fileId: string, token: string, fileSuffix: string) => {
  try {
    const headers: HeadersInit = {}
    if (token) {
      headers['token'] = token
    }
    
    console.log('Fetching media content for fileId:', fileId)
    console.log('File suffix:', fileSuffix)
    
    const response = await fetch(`/api/file/preview/${fileId}`, {
      headers
    })
    
    console.log('Response status:', response.status)
    console.log('Response status text:', response.statusText)
    console.log('Response headers:', Object.fromEntries(response.headers))
    
    if (!response.ok) {
      throw new Error(`获取文件内容失败: ${response.status} ${response.statusText}`)
    }
    
    const contentType = response.headers.get('content-type') || ''
    console.log('Content type:', contentType)
    
    // 检查是否返回的是JSON错误信息
    if (contentType.includes('application/json')) {
      const errorData = await response.json()
      console.log('Error data:', errorData)
      throw new Error(`获取文件内容失败: ${errorData.info || '未知错误'}`)
    }
    
    const blob = await response.blob()
    console.log('Blob size:', blob.size)
    console.log('Blob type:', blob.type)
    
    // 检查Blob大小，如果太小可能是错误信息
    if (blob.size < 100) {
      console.log('Small blob detected, checking content...')
      const text = await blob.text()
      console.log('Blob text:', text)
      try {
        const errorData = JSON.parse(text)
        throw new Error(`获取文件内容失败: ${errorData.info || '未知错误'}`)
      } catch (e) {
        // 如果不是JSON，继续处理
        console.log('Blob is not JSON, proceeding...')
      }
    }
    
    const blobUrl = URL.createObjectURL(blob)
    previewUrl.value = blobUrl
    console.log('Generated blob URL:', blobUrl)
  } catch (error) {
    console.error('获取媒体内容失败:', error)
    previewError.value = `获取文件内容失败: ${error instanceof Error ? error.message : String(error)}`
  } finally {
    previewLoading.value = false
  }
}

const isImageFile = (suffix?: string) => {
  if (!suffix) return false
  const imageSuffixes = ['jpg', 'jpeg', 'png', 'gif', 'webp', 'bmp', 'svg', 'ico']
  return imageSuffixes.includes(suffix.toLowerCase())
}

const isVideoFile = (suffix?: string) => {
  if (!suffix) return false
  const videoSuffixes = ['mp4', 'avi', 'mkv', 'mov', 'wmv', 'flv', 'rmvb', 'webm']
  return videoSuffixes.includes(suffix.toLowerCase())
}

const isAudioFile = (suffix?: string) => {
  if (!suffix) return false
  const audioSuffixes = ['mp3', 'wav', 'flac', 'aac', 'ogg', 'wma', 'm4a']
  return audioSuffixes.includes(suffix.toLowerCase())
}

const isTextFile = (suffix?: string) => {
  if (!suffix) return false
  const textSuffixes = ['txt', 'md', 'json', 'xml', 'html', 'css', 'js', 'ts']
  return textSuffixes.includes(suffix.toLowerCase())
}


</script>

<style scoped>
.file-manager {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background-color: #f5f7fa;
}
.top-nav {
  height: 60px;
  background: white;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 20px;
}
.nav-left h1 {
  margin: 0;
  font-size: 20px;
  color: #333;
}
.nav-right {
  display: flex;
  align-items: center;
  gap: 16px;
}
.username {
  font-size: 14px;
  color: #666;
}
.theme-toggle-btn {
  color: #666;
}
.theme-toggle-btn:hover {
  color: #409EFF;
}

.vip-badge-small {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  background: linear-gradient(135deg, #ffd700, #ff8c00);
  color: #fff;
  padding: 2px 8px;
  border-radius: 12px;
  font-weight: bold;
  font-size: 12px;
  margin-left: 8px;
}

.vip-icon-small {
  font-size: 10px;
}
.main-content {
  flex: 1;
  display: flex;
  overflow: hidden;
}
.sidebar {
  width: 240px;
  background: white;
  border-right: 1px solid #e4e7ed;
  display: flex;
  flex-direction: column;
}
.sidebar-menu {
  border-right: none;
  flex: 1;
}
.recent-files {
  padding: 20px;
  border-top: 1px solid #e4e7ed;
}
.recent-files h3 {
  margin: 0 0 15px 0;
  font-size: 14px;
  font-weight: bold;
  color: #333;
}
.recent-files ul {
  list-style: none;
  padding: 0;
  margin: 0;
}
.recent-files li {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 0;
  cursor: pointer;
  font-size: 13px;
  color: #666;
  transition: color 0.3s;
}
.recent-files li:hover {
  color: #409EFF;
}
.recent-file-name {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  flex: 1;
}
.empty-recent {
  font-size: 13px;
  color: #999;
  margin: 0;
}

.space-info {
  padding: 20px;
  border-top: 1px solid #e4e7ed;
}
.space-info p {
  margin: 0 0 10px 0;
  font-size: 14px;
  color: #666;
}
.space-text {
  font-size: 12px;
  color: #999;
  margin-top: 8px;
}
.content-area {
  flex: 1;
  padding: 20px;
  overflow-y: auto;
}
.toolbar {
  display: flex;
  gap: 16px;
  align-items: center;
  margin-bottom: 20px;
  flex-wrap: wrap;
}
.search-box {
  flex: 1;
  max-width: 300px;
}
.upload-progress {
  background: white;
  border-radius: 8px;
  padding: 20px;
  margin-bottom: 20px;
}
.upload-progress h3 {
  margin: 0 0 16px 0;
  font-size: 16px;
  color: #333;
}
.upload-item {
  margin-bottom: 12px;
}
.upload-item span {
  display: inline-block;
  width: 200px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  vertical-align: middle;
}
.file-list {
  background: white;
  border-radius: 8px;
  padding: 20px;
  transition: background-color 0.3s;
}
.file-list.drag-over {
  background-color: #f0f9ff;
  border: 2px dashed #409eff;
}

/* 确保深色主题下文件列表背景为黑色 */
.dark-theme .file-list {
  background: #1a1a1a;
  border: 1px solid #333333;
}

.dark-theme .file-list.drag-over {
  background-color: #1f2a3a;
  border: 2px dashed #409eff;
}
.file-name {
  display: flex;
  align-items: center;
  gap: 8px;
}

.file-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.file-actions .el-button {
  white-space: nowrap;
}
.folder-icon {
  color: #409eff;
}
.file-icon {
  color: #909399;
}
.empty-state {
  padding: 40px 0;
}
.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

/* 添加额外的样式修复 */
.file-manager > .main-content {
  min-height: 0;
}
.content-area {
  min-height: 0;
}
.file-list {
  min-height: 0;
}
.el-table {
  min-height: 0;
}

/* 深色主题样式 */
.dark-theme {
  --el-bg-color: #1a1a1a;
  --el-bg-color-page: #121212;
  --el-bg-color-overlay: #242424;
  --el-text-color-primary: #e6e6e6;
  --el-text-color-regular: #b3b3b3;
  --el-text-color-secondary: #999999;
  --el-border-color: #333333;
  --el-border-color-light: #404040;
  --el-border-color-lighter: #505050;
  --el-border-color-extra-light: #606060;
  --el-fill-color: #2a2a2a;
  --el-fill-color-light: #333333;
  --el-fill-color-lighter: #404040;
  --el-fill-color-blank: #1a1a1a;
  background-color: #121212;
  color: #e6e6e6;
}

/* 确保整个页面背景为深色 */
.dark-theme html,
.dark-theme body {
  background-color: #121212;
  color: #e6e6e6;
}

/* 确保app-container也为深色 */
.dark-theme .app-container {
  background-color: #121212;
}

.dark-theme .file-manager {
  background-color: #121212;
}

.dark-theme .top-nav {
  background: #1a1a1a;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.3);
}

.dark-theme .nav-left h1 {
  color: #e6e6e6;
}

.dark-theme .username {
  color: #b3b3b3;
}

.dark-theme .theme-toggle-btn {
  color: #b3b3b3;
}

.dark-theme .theme-toggle-btn:hover {
  color: #409EFF;
}

.dark-theme .sidebar {
  background: #1a1a1a !important;
  border-right: 1px solid #333333;
}

.dark-theme .sidebar-menu .el-menu {
  background-color: #1a1a1a !important;
  border-right: none;
}

.dark-theme .sidebar-menu .el-menu-item {
  color: #b3b3b3;
}

.dark-theme .sidebar-menu .el-menu-item:hover {
  color: #409EFF;
  background-color: #2a2a2a;
}

.dark-theme .sidebar-menu .el-menu-item.is-active {
  color: #409EFF;
  background-color: #2a2a2a;
}

.dark-theme .sidebar-menu .el-sub-menu {
  background-color: #1a1a1a !important;
}

.dark-theme .sidebar-menu .el-sub-menu__title {
  background-color: #1a1a1a !important;
  color: #b3b3b3 !important;
}

.dark-theme .sidebar-menu .el-sub-menu__title:hover {
  background-color: #2a2a2a !important;
  color: #409EFF !important;
}

/* 确保下拉菜单在深色主题下正确显示 */
.dark-theme .el-menu--popup {
  background-color: #1a1a1a !important;
  border: 1px solid #333333 !important;
}

.dark-theme .el-menu--popup .el-menu-item {
  background-color: #1a1a1a !important;
  color: #b3b3b3 !important;
}

.dark-theme .el-menu--popup .el-menu-item:hover {
  background-color: #2a2a2a !important;
  color: #409EFF !important;
}

.dark-theme .el-menu--popup .el-menu-item.is-active {
  background-color: #2a2a2a !important;
  color: #409EFF !important;
}

/* 确保文件下拉菜单在深色主题下正确显示 */
.dark-theme .el-menu .el-sub-menu .el-menu {
  background-color: #1a1a1a !important;
  border: 1px solid #333333 !important;
}

.dark-theme .el-menu .el-sub-menu .el-menu .el-menu-item {
  background-color: #1a1a1a !important;
  color: #b3b3b3 !important;
}

.dark-theme .el-menu .el-sub-menu .el-menu .el-menu-item:hover {
  background-color: #2a2a2a !important;
  color: #409EFF !important;
}

.dark-theme .el-menu .el-sub-menu .el-menu .el-menu-item.is-active {
  background-color: #2a2a2a !important;
  color: #409EFF !important;
}

.dark-theme .recent-files h3 {
  color: #e6e6e6;
}

.dark-theme .recent-files li {
  color: #b3b3b3;
}

.dark-theme .recent-files li:hover {
  color: #409EFF;
}

.dark-theme .empty-recent {
  color: #999999;
}

.dark-theme .space-info p {
  color: #b3b3b3;
}

.dark-theme .space-text {
  color: #999999;
}

.dark-theme .toolbar {
  background-color: #1a1a1a;
  border-bottom: 1px solid #333333;
  padding: 10px 20px;
}

.dark-theme .el-button {
  color: #b3b3b3;
}

.dark-theme .el-button:hover {
  color: #409EFF;
}

.dark-theme .el-input__wrapper {
  background-color: #2a2a2a;
  border-color: #404040;
}

.dark-theme .el-input__input {
  color: #e6e6e6;
}

.dark-theme .el-select {
  color: #b3b3b3;
}

.dark-theme .el-select .el-input__wrapper {
  background-color: #2a2a2a;
  border-color: #404040;
}

.dark-theme .el-table {
  background-color: #1a1a1a;
  border-color: #333333;
}

.dark-theme .el-table th {
  background-color: #2a2a2a;
  color: #e6e6e6;
  border-color: #333333;
}

.dark-theme .el-table td {
  background-color: #1a1a1a;
  color: #b3b3b3;
  border-color: #333333;
}

.dark-theme .el-table tr:hover > td {
  background-color: #2a2a2a;
}

.dark-theme .pagination .el-pagination__item {
  background-color: #2a2a2a;
  border-color: #404040;
  color: #b3b3b3;
}

.dark-theme .pagination .el-pagination__item:hover {
  color: #409EFF;
  border-color: #409EFF;
}

.dark-theme .pagination .el-pagination__item--active {
  background-color: #409EFF;
  border-color: #409EFF;
  color: #fff;
}

.dark-theme .upload-progress {
  background: #1a1a1a;
  border: 1px solid #333333;
}

.dark-theme .upload-progress h3 {
  color: #e6e6e6;
}

.dark-theme .empty-state {
  background: #1a1a1a;
  border: 1px solid #333333;
}

.dark-theme .empty-state .el-empty__description {
  color: #999999;
}

.dark-theme .content-area {
  background-color: #121212;
}

.dark-theme .main-content {
  background-color: #121212;
}

.dark-theme .toolbar {
  background-color: #1a1a1a;
  border-bottom: 1px solid #333333;
  padding: 10px 20px;
  margin-bottom: 20px;
  border-radius: 8px;
}

.dark-theme .file-list {
  background: #1a1a1a;
  border: 1px solid #333333;
}

.dark-theme .upload-progress {
  background: #1a1a1a;
  border: 1px solid #333333;
}

.dark-theme .upload-progress h3 {
  color: #e6e6e6;
}

.dark-theme .empty-state {
  background: #1a1a1a;
  border: 1px solid #333333;
}

/* 右键菜单样式 */
.context-menu {
  position: fixed;
  background: white;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  z-index: 1000;
  min-width: 150px;
}

.context-menu ul {
  list-style: none;
  padding: 0;
  margin: 0;
}

.context-menu li {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  cursor: pointer;
  font-size: 14px;
  color: #333;
  transition: background-color 0.3s;
}

.context-menu li:hover {
  background-color: #f5f7fa;
}

.dark-theme .context-menu {
  background: #242424;
  border: 1px solid #404040;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.3);
}

.dark-theme .context-menu li {
  color: #e6e6e6;
}

.dark-theme .context-menu li:hover {
  background-color: #333333;
}

/* 文件预览弹窗样式 */
.file-preview {
  position: fixed;
  z-index: 1000;
  background: white;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  width: 400px;
  max-width: 90vw;
  max-height: 600px;
  overflow: hidden;
}

.preview-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: #f5f7fa;
  border-bottom: 1px solid #e4e7ed;
}

.preview-header span {
  font-size: 14px;
  font-weight: 500;
  color: #333;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  flex: 1;
}

.preview-close {
  padding: 4px;
  margin: -4px;
}

.preview-content {
  padding: 16px;
  max-height: 500px;
  overflow-y: auto;
}

.preview-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 200px;
  color: #666;
}

.preview-loading .el-icon {
  margin-right: 8px;
}

.preview-error {
  color: #f56c6c;
  text-align: center;
  padding: 40px 0;
}

.preview-image {
  max-width: 100%;
  max-height: 400px;
  object-fit: contain;
}

.preview-video {
  width: 100%;
  max-height: 400px;
}

.preview-audio {
  width: 100%;
  margin-top: 10px;
}

.preview-text {
  max-height: 400px;
  overflow-y: auto;
  background: #f5f7fa;
  padding: 12px;
  border-radius: 4px;
  font-family: 'Courier New', Courier, monospace;
  font-size: 14px;
  line-height: 1.5;
}

.preview-text pre {
  margin: 0;
  white-space: pre-wrap;
  word-wrap: break-word;
}

.preview-not-supported {
  text-align: center;
  padding: 40px 0;
  color: #999;
}

/* 深色主题下的预览弹窗样式 */
.dark-theme .file-preview {
  background: #1a1a1a;
  border-color: #333333;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
}

.dark-theme .preview-header {
  background: #2a2a2a;
  border-bottom-color: #333333;
}

.dark-theme .preview-header span {
  color: #e6e6e6;
}

.dark-theme .preview-loading {
  color: #b3b3b3;
}

.dark-theme .preview-text {
  background: #2a2a2a;
  color: #e6e6e6;
}

.dark-theme .preview-not-supported {
  color: #999999;
}

/* 对话框样式 */
.dark-theme .el-dialog {
  background-color: #1a1a1a;
  border-color: #333333;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
}

.dark-theme .el-dialog__header {
  background-color: #2a2a2a;
  border-bottom-color: #333333;
}

.dark-theme .el-dialog__title {
  color: #e6e6e6;
}

.dark-theme .el-dialog__body {
  background-color: #1a1a1a;
  color: #b3b3b3;
}

.dark-theme .el-dialog__footer {
  background-color: #1a1a1a;
  border-top-color: #333333;
}

/* 表单样式 */
.dark-theme .el-form-item__label {
  color: #b3b3b3;
}

.dark-theme .el-form-item__error {
  color: #f56c6c;
}

/* 消息提示样式 */
.dark-theme .el-message {
  background-color: #2a2a2a;
  border-color: #333333;
  color: #e6e6e6;
}

.dark-theme .el-message--success {
  background-color: #1f3a2b;
  border-color: #2d5a3f;
  color: #67c23a;
}

.dark-theme .el-message--warning {
  background-color: #3a321f;
  border-color: #5a4d2d;
  color: #e6a23c;
}

.dark-theme .el-message--error {
  background-color: #3a1f1f;
  border-color: #5a2d2d;
  color: #f56c6c;
}

.dark-theme .el-message--info {
  background-color: #1f2a3a;
  border-color: #2d3f5a;
  color: #409eff;
}

/* 选择器下拉菜单 */
.dark-theme .el-select-dropdown {
  background-color: #1a1a1a;
  border-color: #333333;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.3);
}

.dark-theme .el-select-dropdown__item {
  color: #b3b3b3;
}

.dark-theme .el-select-dropdown__item:hover {
  background-color: #2a2a2a;
  color: #e6e6e6;
}

.dark-theme .el-select-dropdown__item.selected {
  background-color: #1f2a3a;
  color: #409eff;
}

/* 进度条 */
.dark-theme .el-progress__text {
  color: #b3b3b3;
}

/* 按钮样式增强 */
.dark-theme .el-button--primary {
  background-color: #409EFF;
  border-color: #409EFF;
  color: #fff;
}

.dark-theme .el-button--success {
  background-color: #67c23a;
  border-color: #67c23a;
  color: #fff;
}

.dark-theme .el-button--warning {
  background-color: #e6a23c;
  border-color: #e6a23c;
  color: #fff;
}

.dark-theme .el-button--danger {
  background-color: #f56c6c;
  border-color: #f56c6c;
  color: #fff;
}

/* 输入框聚焦样式 */
.dark-theme .el-input__wrapper.is-focus {
  box-shadow: 0 0 0 1px #409EFF inset;
}

/* 表格样式增强 */
.dark-theme .el-table__empty-text {
  color: #999999;
}

.dark-theme .el-table__row--striped {
  background-color: rgba(255, 255, 255, 0.05);
}

/* 分页样式增强 */
.dark-theme .el-pagination__sizes .el-select .el-input__wrapper {
  background-color: #2a2a2a;
  border-color: #404040;
}

.dark-theme .el-pagination__sizes .el-select .el-input__wrapper .el-input__inner {
  color: #b3b3b3;
}

/* 右键菜单增强 */
.dark-theme .context-menu {
  background: #242424;
  border: 1px solid #404040;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.3);
}

.dark-theme .context-menu li {
  color: #e6e6e6;
}

.dark-theme .context-menu li:hover {
  background-color: #333333;
}

/* 滚动条样式 */
.dark-theme ::-webkit-scrollbar {
  width: 8px;
  height: 8px;
}

.dark-theme ::-webkit-scrollbar-track {
  background: #1a1a1a;
}

.dark-theme ::-webkit-scrollbar-thumb {
  background: #404040;
  border-radius: 4px;
}

.dark-theme ::-webkit-scrollbar-thumb:hover {
  background: #505050;
}
</style>