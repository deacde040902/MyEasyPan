<template>
  <div class="share-page">
    <div class="page-header">
      <el-button :icon="ArrowLeft" @click="goBack">返回</el-button>
      <h1>文件分享</h1>
    </div>

    <div class="share-content">
      <el-card class="share-card">
        <template #header>
          <div class="card-header">
            <span>分享链接</span>
          </div>
        </template>

        <div v-if="shareInfo" class="share-info">
          <div class="share-details">
            <div class="file-info">
              <div class="file-name">
                <el-icon v-if="shareInfo.isFolder">
                  <Folder />
                </el-icon>
                <el-icon v-else>
                  <component :is="getFileIcon(shareInfo.fileSuffix)" />
                </el-icon>
                <span>{{ shareInfo.fileName }}</span>
              </div>
              <div class="file-size">
                {{ shareInfo.fileSizeStr }}
              </div>
              <div class="file-expire">
                <span>有效期：</span>
                <span>{{ shareInfo.expireTime ? formatDate(shareInfo.expireTime) : '永久有效' }}</span>
              </div>
            </div>

            <div class="share-actions">
              <div class="share-link">
                <el-input
                  v-model="shareUrl"
                  :readonly="true"
                  placeholder="分享链接"
                  class="share-input"
                >
                  <template #append>
                    <el-button @click="copyShareLink">复制</el-button>
                  </template>
                </el-input>
              </div>

              <div class="share-code">
                <el-input
                  v-model="shareCode"
                  :readonly="true"
                  placeholder="分享码"
                  class="share-input"
                >
                  <template #append>
                    <el-button @click="copyShareCode">复制</el-button>
                  </template>
                </el-input>
              </div>

              <div class="share-actions-buttons">
                <el-button type="primary" @click="saveToMyCloud">保存到我的云盘</el-button>
                <el-button @click="goBack">返回</el-button>
              </div>
            </div>
          </div>
        </div>

        <div v-else class="empty-state">
          <el-empty description="分享链接无效或已过期" />
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, Folder, Document, Picture, VideoCamera, Mic, Files, Box } from '@element-plus/icons-vue'
import { getShareFileInfo, saveShareFile } from '@/api/file'

const route = useRoute()
const router = useRouter()
const shareInfo = ref<any>(null)
const shareUrl = ref('')
const shareCode = ref('')

const shareId = route.params.shareId as string

const formatDate = (dateStr: string) => new Date(dateStr).toLocaleString()

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
    mp4: VideoCamera,
    avi: VideoCamera,
    mkv: VideoCamera,
    mov: VideoCamera,
    wmv: VideoCamera,
    flv: VideoCamera,
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
    zip: Box,
    rar: Box,
    '7z': Box,
    tar: Box,
    gz: Box,
    exe: Box,
    dmg: Box,
    apk: Box,
    torrent: Box
  }
  return map[suffix.toLowerCase()] || Files
}

const fetchShareInfo = async () => {
  try {
    const res = await getShareFileInfo(shareId) as any
    if (res.code === 200) {
      shareInfo.value = res.data
      shareUrl.value = `${window.location.origin}/share/${shareId}`
      shareCode.value = res.data.shareCode
    } else {
      ElMessage.error(res.info || '获取分享信息失败')
    }
  } catch (error) {
    console.error('获取分享信息失败:', error)
    ElMessage.error('获取分享信息失败')
  }
}

const copyShareLink = () => {
  navigator.clipboard.writeText(shareUrl.value)
    .then(() => ElMessage.success('分享链接已复制到剪贴板'))
    .catch(() => ElMessage.error('复制失败'))
}

const copyShareCode = () => {
  navigator.clipboard.writeText(shareCode.value)
    .then(() => ElMessage.success('分享码已复制到剪贴板'))
    .catch(() => ElMessage.error('复制失败'))
}

const saveToMyCloud = async () => {
  if (!shareInfo.value) {
    ElMessage.warning('分享信息无效')
    return
  }

  try {
    await ElMessageBox.confirm('确定要将此文件保存到您的云盘吗？', '确认保存', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    const res = await saveShareFile(shareId, '0') as any
    if (res.code === 200) {
      ElMessage.success('文件已保存到您的云盘')
      router.push('/files')
    } else {
      ElMessage.error(res.info || '保存失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('保存分享文件失败:', error)
      ElMessage.error('保存失败')
    }
  }
}

const goBack = () => {
  router.push('/files')
}

onMounted(() => {
  fetchShareInfo()
})
</script>

<style scoped>
.share-page {
  padding: 20px;
  height: 100vh;
  background-color: #f5f7fa;
}

.page-header {
  margin-bottom: 16px;
  display: flex;
  align-items: center;
  gap: 16px;
}

.share-content {
  max-width: 800px;
  margin: 0 auto;
}

.share-card {
  width: 100%;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.share-info {
  padding: 20px;
}

.share-details {
  display: flex;
  gap: 40px;
  flex-wrap: wrap;
}

.file-info {
  flex: 1;
  min-width: 300px;
}

.file-name {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
  font-size: 18px;
  font-weight: bold;
}

.file-size {
  margin-bottom: 8px;
  color: #666;
}

.file-expire {
  margin-bottom: 16px;
  color: #666;
}

.share-actions {
  flex: 1;
  min-width: 300px;
}

.share-link, .share-code {
  margin-bottom: 20px;
}

.share-input {
  width: 100%;
}

.share-actions-buttons {
  display: flex;
  gap: 12px;
  margin-top: 20px;
}

.empty-state {
  padding: 40px 0;
}
</style>