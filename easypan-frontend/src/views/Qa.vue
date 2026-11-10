<template>
  <div class="qa-page">
    <!-- 顶栏 -->
    <div class="qa-header">
      <el-page-header @back="goBack">
        <template #content>
          <div class="header-title">
            <span class="header-badge">
              <el-icon><ChatDotRound /></el-icon>
            </span>
            <div class="header-text">
              <span class="header-name">AI 文件对话</span>
              <span class="header-sub">基于你的云盘文件，回答可溯源</span>
            </div>
          </div>
        </template>
        <template #extra>
          <el-button :loading="indexingAll" text bg @click="handleIndexAll">重建全部索引</el-button>
        </template>
      </el-page-header>
    </div>

    <!-- 未建立索引时的引导 -->
    <div v-if="showIndexAll" class="qa-index-guide">
      <el-alert type="warning" :closable="false" show-icon>
        <template #title>
          还没有文件建立问答索引。点击右侧按钮为全部支持的文档（txt、md、pdf、word、html 等）一键建立索引。
        </template>
      </el-alert>
      <el-button type="primary" style="margin-top: 10px" :loading="indexingAll" @click="handleIndexAll">
        一键为全部文档建立索引
      </el-button>
    </div>

    <!-- 消息区 -->
    <div class="qa-messages" ref="messagesRef">
      <div v-for="(msg, index) in messages" :key="index" :class="['msg-item', msg.role]">
        <div class="avatar" :class="msg.role">
          <el-icon v-if="msg.role === 'assistant'"><ChatDotRound /></el-icon>
          <el-icon v-else><User /></el-icon>
        </div>
        <div class="msg-bubble">
          <div class="msg-text">{{ msg.content }}</div>
          <!-- 已索引文件列表（欢迎消息） -->
          <div v-if="msg.fileList && msg.fileList.length" class="msg-files">
            <div class="files-title">已索引文件</div>
            <div v-for="f in msg.fileList" :key="f.fileId" class="file-row">
              <el-icon class="file-row-icon"><Document /></el-icon>
              <span class="file-row-name">{{ f.fileName }}</span>
              <span class="file-row-chunks">{{ f.chunkCount }} 个切片</span>
            </div>
          </div>
          <!-- 溯源引用 -->
          <div v-if="msg.role === 'assistant' && msg.sources && msg.sources.length" class="msg-sources">
            <div class="sources-title">来源引用</div>
            <div v-for="(src, i) in msg.sources" :key="i" class="source-item">
              <div class="source-head">
                <span class="source-idx">[{{ i + 1 }}]</span>
                <el-icon class="source-file-icon"><Document /></el-icon>
                <span class="source-name">{{ src.fileName }}</span>
                <span v-if="src.chunkIndex !== null" class="source-chunk">片段 #{{ src.chunkIndex }}</span>
                <span v-if="src.score" class="source-score">相关度 {{ (src.score * 100).toFixed(0) }}%</span>
              </div>
              <div class="source-snippet">{{ src.snippet }}</div>
            </div>
          </div>
        </div>
      </div>
      <div v-if="streaming" class="msg-item assistant">
        <div class="avatar assistant"><el-icon><ChatDotRound /></el-icon></div>
        <div class="msg-bubble">
          <div class="msg-text typing">正在检索你的文件并生成回答…</div>
        </div>
      </div>
    </div>

    <!-- 输入区 -->
    <div class="qa-input">
      <el-input
        v-model="question"
        class="qa-input-box"
        placeholder="向你的文件提问，例如：总结一下上周的报告内容"
        :disabled="streaming"
        @keyup.enter="ask"
        clearable
        size="large"
      >
        <template #prefix>
          <el-icon><Edit /></el-icon>
        </template>
      </el-input>
      <el-button type="primary" size="large" class="qa-send-btn" :loading="streaming" @click="ask">发送</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, nextTick, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ChatDotRound, Document, Edit, User } from '@element-plus/icons-vue'
import { askStream, getIndexedFiles, indexAllFiles, type QaSource, type IndexedFile } from '@/api/qa'

const router = useRouter()
const question = ref('')
const streaming = ref(false)
const showIndexAll = ref(false)
const indexingAll = ref(false)
const messages = ref<Array<{ role: 'user' | 'assistant'; content: string; sources?: QaSource[]; fileList?: IndexedFile[] }>>([])
const messagesRef = ref<HTMLElement>()

let abortController: AbortController | null = null

const scrollToBottom = () => {
  nextTick(() => {
    messagesRef.value?.scrollTo({ top: messagesRef.value.scrollHeight, behavior: 'smooth' })
  })
}

const goBack = () => router.back()

onMounted(async () => {
  try {
    const res = await getIndexedFiles()
    const list = res.data || []
    if (list.length === 0) {
      // 尚无已索引文件：给出引导，用户可一键索引历史文档
      showIndexAll.value = true
      messages.value.push({
        role: 'assistant',
        content: '欢迎！当前还没有已建立索引的文件。你可以点击上方按钮一键索引，或先到文件管理中上传文档，然后在文件列表点击「问答」按钮为单个文件建立索引。'
      })
    } else {
      messages.value.push({
        role: 'assistant',
        content: `已就绪！当前共有 ${list.length} 个文件建立了问答索引（共 ${list.reduce((s, f) => s + f.chunkCount, 0)} 个切片），随时向我提问。`,
        fileList: list
      })
    }
  } catch {
    // 接口异常时静默，不打断页面
  }
})

// 一键为全部文档建立索引
const handleIndexAll = async () => {
  indexingAll.value = true
  try {
    const res = await indexAllFiles()
    const r = res.data
    if (!r || r.success === 0) {
      ElMessage.warning('没有可索引的文档文件（支持 txt、md、pdf、word、html 等格式）')
    } else {
      let info = `索引完成：成功 ${r.success} 个文件，共生成 ${r.chunkCount} 个切片`
      if (r.failed > 0) {
        info += `；失败 ${r.failed} 个（${r.failedNames}）`
      }
      ElMessage.success(info)
      showIndexAll.value = false
      messages.value.push({ role: 'assistant', content: `✅ ${info}，现在可以向我提问了！` })
    }
  } catch {
    // 错误信息由拦截器统一提示
  } finally {
    indexingAll.value = false
  }
}

const ask = async () => {
  const q = question.value.trim()
  if (!q || streaming.value) return
  question.value = ''
  streaming.value = true
  messages.value.push({ role: 'user', content: q })
  const replyIndex = messages.value.push({ role: 'assistant', content: '' }) - 1
  scrollToBottom()

  abortController = new AbortController()
  await askStream(
    q,
    {
      onSources: (sources) => {
        messages.value[replyIndex].sources = sources
      },
      onMessage: (chunk) => {
        messages.value[replyIndex].content += chunk
        scrollToBottom()
      },
      onDone: () => {
        streaming.value = false
        abortController = null
        if (!messages.value[replyIndex].content) {
          messages.value[replyIndex].content = '（未返回内容）'
        }
        scrollToBottom()
      },
      onError: (err) => {
        streaming.value = false
        abortController = null
        ElMessage.error(String(err))
        if (!messages.value[replyIndex].content) {
          messages.value[replyIndex].content = '回答生成失败，请稍后重试。'
        }
      }
    },
    abortController.signal
  )
}
</script>

<style scoped>
.qa-page {
  display: flex;
  flex-direction: column;
  height: 100vh;
  max-width: 880px;
  margin: 0 auto;
  padding: 14px 20px 0;
  box-sizing: border-box;
}

/* ===== 顶栏 ===== */
.qa-header {
  padding-bottom: 12px;
  border-bottom: 1px solid var(--el-border-color-lighter);
}
.header-title {
  display: flex;
  align-items: center;
  gap: 10px;
}
.header-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  border-radius: 10px;
  background: var(--el-color-primary);
  color: #fff;
  font-size: 18px;
}
.header-text {
  display: flex;
  flex-direction: column;
}
.header-name {
  font-size: 16px;
  font-weight: 600;
  color: var(--el-text-color-primary);
  line-height: 1.3;
}
.header-sub {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

/* ===== 索引引导 ===== */
.qa-index-guide {
  margin-top: 12px;
  padding: 10px 12px;
  background: var(--el-color-warning-light-9);
  border: 1px solid var(--el-color-warning-light-7);
  border-radius: 10px;
}

/* ===== 消息区 ===== */
.qa-messages {
  flex: 1;
  overflow-y: auto;
  padding: 20px 4px 10px;
}
.msg-item {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  margin-bottom: 18px;
}
.msg-item.user {
  flex-direction: row-reverse;
}
.avatar {
  flex-shrink: 0;
  width: 34px;
  height: 34px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 17px;
}
.avatar.assistant {
  background: var(--el-color-primary);
}
.avatar.user {
  background: var(--el-color-success);
}
.msg-bubble {
  max-width: 78%;
  padding: 12px 15px;
  border-radius: 12px;
  font-size: 14px;
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
}
.msg-item.user .msg-bubble {
  background: var(--el-color-primary);
  color: #fff;
  border-top-right-radius: 4px;
}
.msg-item.assistant .msg-bubble {
  background: var(--el-bg-color);
  border: 1px solid var(--el-border-color-lighter);
  color: var(--el-text-color-primary);
  border-top-left-radius: 4px;
}

/* ===== 已索引文件列表（欢迎消息） ===== */
.msg-files {
  margin-top: 10px;
  padding-top: 10px;
  border-top: 1px dashed var(--el-border-color);
}
.files-title {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  margin-bottom: 6px;
  font-weight: 600;
}
.file-row {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 10px;
  margin-bottom: 5px;
  background: var(--el-fill-color-light);
  border-radius: 8px;
  font-size: 13px;
}
.file-row-icon {
  color: var(--el-color-primary);
  font-size: 14px;
}
.file-row-name {
  color: var(--el-text-color-primary);
  font-weight: 500;
  word-break: break-all;
}
.file-row-chunks {
  margin-left: auto;
  flex-shrink: 0;
  font-size: 11px;
  color: var(--el-text-color-secondary);
  background: var(--el-fill-color);
  border-radius: 4px;
  padding: 1px 6px;
}

/* ===== 溯源引用 ===== */
.msg-sources {
  margin-top: 12px;
  padding-top: 10px;
  border-top: 1px dashed var(--el-border-color);
}
.sources-title {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  margin-bottom: 8px;
  font-weight: 600;
}
.source-item {
  font-size: 12px;
  margin-bottom: 8px;
  background: var(--el-fill-color-light);
  border-radius: 8px;
  padding: 8px 10px;
}
.source-head {
  display: flex;
  align-items: center;
  gap: 5px;
  color: var(--el-text-color-primary);
}
.source-idx {
  color: var(--el-color-primary);
  font-weight: 600;
}
.source-file-icon {
  color: var(--el-color-primary);
  font-size: 13px;
}
.source-name {
  font-weight: 500;
}
.source-chunk {
  margin-left: 4px;
  color: var(--el-text-color-secondary);
}
.source-score {
  margin-left: auto;
  color: var(--el-color-success);
  font-size: 11px;
  background: var(--el-color-success-light-9);
  border-radius: 4px;
  padding: 1px 6px;
}
.source-snippet {
  margin-top: 5px;
  color: var(--el-text-color-secondary);
  line-height: 1.5;
}

/* 打字中动画 */
.typing::after {
  content: '';
  animation: blink 1.2s infinite;
}
@keyframes blink {
  0%, 100% { content: ''; }
  50% { content: '▍'; }
}

/* ===== 输入区 ===== */
.qa-input {
  display: flex;
  gap: 10px;
  padding: 12px 0 16px;
  border-top: 1px solid var(--el-border-color-lighter);
  background: var(--el-bg-color);
}
.qa-input-box {
  flex: 1;
}
.qa-send-btn {
  min-width: 90px;
}
</style>
