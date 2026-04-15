<template>
  <div class="share-container">
    <div class="page-header">
      <el-button :icon="ArrowLeft" @click="router.push('/files')">返回文件管理</el-button>
    </div>
    <el-card class="share-card">
      <template #header>
        <div class="card-header">
E:\Workspace-java\easypan\esay-backend          <span>我的分享</span>
          <div class="header-buttons">
            <el-button type="danger" @click="cancelAllShares" :disabled="!isLoggedIn || shareList.length === 0" :loading="cancellingAll">
              一键取消所有分享
            </el-button>
            <el-button type="primary" @click="showCreateDialog" :disabled="!isLoggedIn">新建分享</el-button>
          </div>
        </div>
      </template>

      <div v-if="!isLoggedIn" class="login-tip">
        <el-empty description="请先登录查看分享列表" />
        <el-button type="primary" @click="router.push('/login')">去登录</el-button>
      </div>

      <template v-else>
        <el-table v-if="shareList.length > 0 || loading" :data="shareList" v-loading="loading" style="width: 100%">
          <el-table-column prop="fileName" label="文件名" min-width="200">
            <template #default="{ row }">
              <div class="file-name">
                <el-icon v-if="row.isFolder"><Folder /></el-icon>
                <el-icon v-else><Document /></el-icon>
                <span>{{ row.fileName }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="fileSizeStr" label="大小" width="120" />
          <el-table-column label="分享码" width="120">
            <template #default="{ row }">
              <span>{{ row.shareCode }}</span>
            </template>
          </el-table-column>
          <el-table-column label="过期时间" width="180">
            <template #default="{ row }">
              <span>{{ row.expireTime ? formatDate(row.expireTime) : '永久有效' }}</span>
            </template>
          </el-table-column>
          <el-table-column label="创建时间" width="180">
            <template #default="{ row }">
              <span>{{ formatDate(row.createTime) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="150">
            <template #default="{ row }">
              <el-button link @click="copyShareLink(row)">复制链接</el-button>
              <el-button link @click="handleCancelShare(row)">取消</el-button>
            </template>
          </el-table-column>
        </el-table>

        <div v-else-if="!loading && shareList.length === 0" class="empty-state">
          <el-empty description="暂无分享" />
        </div>
      </template>
    </el-card>

    <el-dialog v-model="dialogVisible" title="创建分享" width="500px">
      <el-form :model="shareForm" label-width="100px">
        <el-form-item label="选择文件">
          <div>
          <el-tree-select
            v-model="shareForm.fileId"
            :data="fileTree"
            :props="{ label: 'fileName', children: 'children', value: 'fileId' }"
            placeholder="请选择文件或文件夹"
            check-strictly
            filterable
            style="width: 100%"
          />
          <input ref="localFileInput" type="file" multiple style="display: none" @change="handleLocalFileSelect" />
        </div>
        </el-form-item>
        <el-form-item label="有效期">
          <el-radio-group v-model="shareForm.expireType">
            <el-radio :label="1">1天</el-radio>
            <el-radio :label="2">7天</el-radio>
            <el-radio :label="3">永久</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleCreateShare" :loading="creating">创建</el-button>
        <el-button type="success" @click="showLocalFileDialog">本地文件分享</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Folder, Document, ArrowLeft } from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'
import { getShareList, createShare, cancelShare, getFolderTree, initChunkUpload, getUploadedChunks, uploadChunk, mergeChunk, loadDataList } from '@/api/file'
import type { FileInfo, ApiResponse } from '@/api/file'
import SparkMD5 from 'spark-md5'

interface TreeNode extends FileInfo {
  children?: TreeNode[]
}

const router = useRouter()
const loading = ref(false)
const shareList = ref<any[]>([])
const dialogVisible = ref(false)
const creating = ref(false)
const cancellingAll = ref(false)
const shareForm = ref({ fileId: '', expireType: 1 })
const fileTree = ref<TreeNode[]>([])
const localFileInput = ref<HTMLInputElement>()
const uploadingFiles = ref(new Map<string, number>())

// 检查用户是否登录
const isLoggedIn = computed(() => {
  return localStorage.getItem('token') !== null
})

const formatDate = (dateStr: string) => new Date(dateStr).toLocaleString()

const buildFolderTree = (files: FileInfo[], parentId = '0'): TreeNode[] => {
  // 首先构建文件夹树
  const folders = files
    .filter(f => f.isFolder === 1 && f.filePid === parentId)
    .map(folder => ({ ...folder, children: buildFolderTree(files, folder.fileId) }))

  // 然后添加文件夹下的文件
  const filesInFolder = files
    .filter(f => f.isFolder === 0 && f.filePid === parentId)
    .map(file => ({ ...file, children: [] }))

  return [...folders, ...filesInFolder]
}

const fetchFolderTree = async () => {
  try {
    console.log('Fetching folder tree...')
    console.log('Token:', localStorage.getItem('token'))
    const res = await getFolderTree() as any
    console.log('Folder tree response:', res)

    if (res.code === 200) {
      console.log('Folder tree data:', res.data)
      const folders = res.data as FileInfo[]
      fileTree.value = buildFolderTree(folders, '0')
    } else {
      const errorMessage = res.info || '获取文件夹树失败'
      console.error('Folder tree error:', errorMessage)
      console.error('Response data:', res)
      ElMessage.error(errorMessage)
    }
  } catch (error: any) {
    console.error('获取文件夹树失败:', error)
    console.error('Error details:', error.response?.data)
    console.error('Error config:', error.config)
    const errorMessage = error.response?.data?.info || error.message || '获取文件夹树失败'
    ElMessage.error(errorMessage)
  }
}

const fetchShareList = async () => {
  loading.value = true
  try {
    console.log('Fetching share list...')
    console.log('Token:', localStorage.getItem('token'))
    const res = await getShareList() as any
    console.log('Share list response:', res)

    if (res.code === 200) {
      console.log('Share list data:', res.data)
      shareList.value = res.data
    } else {
      const errorMessage = res.info || '获取分享列表失败'
      console.error('Share list error:', errorMessage)
      console.error('Response data:', res)
      ElMessage.error(errorMessage)
    }
  } catch (error: any) {
    console.error('获取分享列表失败:', error)
    console.error('Error details:', error.response?.data)
    console.error('Error config:', error.config)
    const errorMessage = error.response?.data?.info || error.message || '获取分享列表失败'
    ElMessage.error(errorMessage)
  } finally {
    loading.value = false
  }
}

const showCreateDialog = () => {
  shareForm.value = { fileId: '', expireType: 1 }
  dialogVisible.value = true
}

const handleCreateShare = async () => {
  if (!shareForm.value.fileId) return ElMessage.warning('请选择文件')
  creating.value = true
  try {
    const res = await createShare({
      fileId: shareForm.value.fileId,
      expireType: shareForm.value.expireType
    }) as any
    if (res.code === 200) {
      ElMessage.success('分享创建成功')
      dialogVisible.value = false
      fetchShareList()
    } else {
      ElMessage.error(res.info || '创建失败')
    }
  } catch (error) {
    console.error('创建分享失败:', error)
    ElMessage.error('创建分享失败')
  } finally {
    creating.value = false
  }
}

// 本地文件上传和分享
const showLocalFileDialog = () => {
  localFileInput.value?.click()
}

const handleLocalFileSelect = async (e: Event) => {
  const files = (e.target as HTMLInputElement).files
  if (files) {
    for (const file of Array.from(files)) {
      await uploadAndShareLocalFile(file)
    }
  }
  if (localFileInput.value) localFileInput.value.value = ''
}

const uploadAndShareLocalFile = async (file: File) => {
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
      filePid: '0' // 上传到根目录
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
      const promise = new Promise<void>((resolve, reject) => {
        const reader = new FileReader()
        reader.onload = async (e) => {
          try {
            const blob = new Blob([e.target?.result as ArrayBuffer])
            const chunkFile = new File([blob], `chunk-${i}`, { type: file.type })
            await uploadChunk({ uploadId, chunkIndex: i, file: chunkFile })
            uploadedCount++
            uploadingFiles.value.set(file.name, Math.round((uploadedCount / totalChunks) * 100))
            resolve()
          } catch (error) {
            reject(error)
          }
        }
        reader.onerror = () => reject(new Error('分片读取失败'))
        reader.readAsArrayBuffer(chunk)
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

    // 获取上传后的文件ID
    const fileList = await loadDataList({ filePid: '0', fileName: file.name, pageSize: 1 })
    const uploadedFile = fileList.data.records[0]

    if (uploadedFile) {
      // 创建分享
      const shareRes = await createShare({
        fileId: uploadedFile.fileId,
        expireType: shareForm.value.expireType
      }) as any
      if (shareRes.code === 200) {
        ElMessage.success(`${file.name} 上传并分享成功`)
        fetchShareList()
      } else {
        ElMessage.error('分享创建失败')
      }
    } else {
      ElMessage.error('文件上传成功但未找到，请刷新页面')
    }
  } catch (error: any) {
    console.error('上传并分享失败:', error)
    uploadingFiles.value.delete(file.name)
    ElMessage.error(`上传并分享失败: ${error.info || error.message}`)
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

const handleCancelShare = async (row: any) => {
  try {
    await ElMessageBox.confirm(`确定取消分享「${row.fileName}」？`, '提示', { type: 'warning' })
    const res = await cancelShare(row.shareId) as any
    if (res.code === 200) {
      ElMessage.success('已取消分享')
      fetchShareList()
    } else {
      ElMessage.error(res.info || '取消失败')
    }
  } catch (error) {
    // 用户取消操作
  }
}

const cancelAllShares = async () => {
  try {
    await ElMessageBox.confirm(`确定取消所有分享？此操作不可恢复。`, '提示', { 
      type: 'warning',
      confirmButtonText: '确定',
      cancelButtonText: '取消'
    })
    cancellingAll.value = true
    
    const cancelPromises = shareList.value.map(async (share) => {
      try {
        const res = await cancelShare(share.shareId) as any
        return res.code === 200
      } catch (error) {
        return false
      }
    })
    
    const results = await Promise.all(cancelPromises)
    const successCount = results.filter(Boolean).length
    
    if (successCount > 0) {
      ElMessage.success(`成功取消 ${successCount} 个分享`)
      fetchShareList()
    } else {
      ElMessage.warning('取消分享失败')
    }
  } catch (error) {
    // 用户取消操作
  } finally {
    cancellingAll.value = false
  }
}

const copyShareLink = (row: any) => {
  // 构建完整的分享链接
  const link = `${window.location.origin}/share/${row.shareId}`
  navigator.clipboard.writeText(link)
    .then(() => {
      // 显示分享链接和分享码
      ElMessage({ 
        message: `分享链接已复制到剪贴板\n分享码：${row.shareCode}`, 
        type: 'success',
        duration: 5000
      })
    })
    .catch(() => ElMessage.error('复制失败'))
}

onMounted(() => {
  if (isLoggedIn.value) {
    fetchShareList()
    fetchFolderTree()
  }
})
</script>

<style scoped>
.share-container {
  padding: 20px;
  height: 100vh;
  background-color: #f5f7fa;
}
.page-header {
  margin-bottom: 16px;
}
.share-card {
  max-width: 1200px;
  margin: 0 auto;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-buttons {
  display: flex;
  gap: 10px;
  align-items: center;
}
.file-name {
  display: flex;
  align-items: center;
  gap: 8px;
}
.empty-state {
  padding: 40px 0;
}

.login-tip {
  padding: 40px 0;
  text-align: center;
}

.login-tip .el-button {
  margin-top: 20px;
}
</style>