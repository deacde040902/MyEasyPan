<template>
  <div class="recycle-container">
    <div class="page-header">
      <el-button :icon="ArrowLeft" @click="router.push('/files')">返回文件管理</el-button>
    </div>

    <el-card class="recycle-card">
      <template #header>
        <div class="card-header">
          <span>回收站</span>
          <el-button type="danger" @click="handleClearAll" :disabled="fileList.length === 0">
            清空回收站
          </el-button>
        </div>
      </template>

      <el-table
        :data="fileList"
        v-loading="loading"
        style="width: 100%"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="55" />
        <el-table-column label="文件名" min-width="300">
          <template #default="{ row }">
            <div class="file-name">
              <el-icon v-if="row.isFolder === 1"><Folder /></el-icon>
              <el-icon v-else><Document /></el-icon>
              <span>{{ row.fileName }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="fileSizeStr" label="大小" width="120" />
        <el-table-column label="删除时间" width="180">
          <template #default="{ row }">
            {{ formatDate(row.deleteTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200">
          <template #default="{ row }">
            <el-button link @click="handleRecover(row)">恢复</el-button>
            <el-button link @click="handlePermanentDelete(row)">彻底删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div v-if="!loading && fileList.length === 0" class="empty-state">
        <el-empty description="回收站为空" />
      </div>

      <div v-if="fileList.length > 0" class="pagination">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50]"
          :total="totalCount"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="fetchRecycleList"
          @current-change="fetchRecycleList"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Folder, Document, ArrowLeft } from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'
import { getRecycleList, recoverFile, permanentDelete } from '@/api/file'
import type { FileInfo } from '@/api/file'

const router = useRouter()
const loading = ref(false)
const fileList = ref<FileInfo[]>([])
const selectedFiles = ref<FileInfo[]>([])
const currentPage = ref(1)
const pageSize = ref(15)
const totalCount = ref(0)

const formatDate = (dateStr?: string) => {
  if (!dateStr) return ''
  return new Date(dateStr).toLocaleString()
}

const fetchRecycleList = async () => {
  loading.value = true
  try {
    const res = await getRecycleList({
      pageNo: currentPage.value,
      pageSize: pageSize.value
    }) as any
    if (res.code === 200) {
      fileList.value = res.data.records || []
      totalCount.value = res.data.total || 0
    } else {
      ElMessage.error(res.info || '获取回收站列表失败')
    }
  } catch (error) {
    console.error('获取回收站列表失败:', error)
    ElMessage.error('获取回收站列表失败')
  } finally {
    loading.value = false
  }
}

const handleSelectionChange = (selection: FileInfo[]) => {
  selectedFiles.value = selection
}

const handleRecover = async (row: FileInfo) => {
  try {
    const res = await recoverFile(row.fileId) as any
    if (res.code === 200) {
      ElMessage.success('文件已恢复')
      fetchRecycleList()
    } else {
      ElMessage.error(res.info || '恢复失败')
    }
  } catch (error) {
    console.error('恢复失败:', error)
    ElMessage.error('恢复失败')
  }
}

const handlePermanentDelete = async (row: FileInfo) => {
  try {
    await ElMessageBox.confirm(
      `确定要彻底删除「${row.fileName}」吗？删除后无法恢复！`,
      '警告',
      { type: 'warning' }
    )
    const res = await permanentDelete(row.fileId) as any
    if (res.code === 200) {
      ElMessage.success('已彻底删除')
      fetchRecycleList()
    } else {
      ElMessage.error(res.info || '删除失败')
    }
  } catch (error) {
    // 用户取消
  }
}

const handleClearAll = async () => {
  try {
    await ElMessageBox.confirm(
      '确定要清空回收站吗？所有文件将被彻底删除且无法恢复！',
      '警告',
      { type: 'error' }
    )
    // 逐个删除（或调用批量删除接口，目前没有批量，循环调用）
    for (const file of fileList.value) {
      await permanentDelete(file.fileId)
    }
    ElMessage.success('回收站已清空')
    fetchRecycleList()
  } catch (error) {
    // 取消操作
  }
}

onMounted(() => {
  fetchRecycleList()
})
</script>

<style scoped>
.recycle-container {
  padding: 20px;
  height: 100vh;
  background-color: #f5f7fa;
}
.page-header {
  margin-bottom: 16px;
}
.recycle-card {
  max-width: 1200px;
  margin: 0 auto;
}
.card-header {
  display: flex;
  justify-content: space-between;
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
.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
</style>