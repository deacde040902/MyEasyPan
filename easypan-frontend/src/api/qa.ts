import { request } from '@/utils/request'
import { type ApiResponse } from './file'

// 已索引文件条目
export interface IndexedFile {
  fileId: string
  fileName: string
  chunkCount: number
}

// 溯源引用条目
export interface QaSource {
  fileId: string
  fileName: string
  snippet: string
  score: number | null
  chunkIndex: number | null
}

// 查询已建立索引的文件列表
export const getIndexedFiles = (): Promise<ApiResponse<IndexedFile[]>> => {
  return request({
    url: '/qa/indexedFiles',
    method: 'POST'
  })
}

// 为指定文件建立/重建问答索引
export const indexFile = (fileId: string): Promise<ApiResponse<string>> => {
  return request({
    url: '/qa/index',
    method: 'POST',
    data: new URLSearchParams({ fileId }),
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' }
  })
}

// 批量索引结果
export interface IndexAllResult {
  total: number
  success: number
  failed: number
  chunkCount: number
  failedNames: string
}

// 为当前用户所有支持索引的文档批量建立索引
export const indexAllFiles = (): Promise<ApiResponse<IndexAllResult>> => {
  return request({
    url: '/qa/indexAll',
    method: 'POST'
  })
}

/**
 * 文件问答（SSE 流式）
 *
 * 使用原生 fetch + ReadableStream 解析 SSE（EventSource 仅支持 GET）。
 * 事件流：sources → message* → done
 */
export const askStream = (
  question: string,
  handlers: {
    onSources: (sources: QaSource[]) => void
    onMessage: (chunk: string) => void
    onDone: () => void
    onError: (err: any) => void
  },
  signal?: AbortSignal
): Promise<void> => {
  const token = localStorage.getItem('token') || ''
  return fetch('/api/qa/ask', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded',
      token
    },
    body: new URLSearchParams({ question }),
    signal
  }).then(async (response) => {
    if (!response.ok || !response.body) {
      handlers.onError(`请求失败 (${response.status})`)
      return
    }
    const reader = response.body.getReader()
    const decoder = new TextDecoder('utf-8')
    let buffer = ''

    const dispatch = (eventName: string, payload: string) => {
      if (eventName === 'sources') {
        try {
          handlers.onSources(JSON.parse(payload))
        } catch {
          handlers.onSources([])
        }
      } else if (eventName === 'message') {
        handlers.onMessage(payload)
      } else if (eventName === 'done') {
        handlers.onDone()
      }
    }

    for (;;) {
      const { done, value } = await reader.read()
      if (done) break
      buffer += decoder.decode(value, { stream: true })
      // SSE 事件以空行分隔
      const parts = buffer.split('\n\n')
      buffer = parts.pop() || ''
      for (const part of parts) {
        let eventName = 'message'
        const dataLines: string[] = []
        for (const line of part.split('\n')) {
          if (line.startsWith('event:')) {
            eventName = line.slice(6).trim()
          } else if (line.startsWith('data:')) {
            dataLines.push(line.slice(5).replace(/^ /, ''))
          }
        }
        dispatch(eventName, dataLines.join('\n'))
      }
    }
  }).catch((err) => {
    if (err?.name !== 'AbortError') {
      handlers.onError(err?.message || '网络异常')
    }
  })
}
