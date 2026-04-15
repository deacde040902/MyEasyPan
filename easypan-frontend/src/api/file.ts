import { request } from '@/utils/request'
import qs from 'qs'

// 统一响应格式
export interface ApiResponse<T = any> {
  status: string
  code: number
  info: string
  data: T
}

// 文件列表查询参数
export interface FileListQuery {
  filePid?: string
  category?: string
  fileName?: string
  pageNo?: number
  pageSize?: number
}

// 文件信息项
export interface FileInfo {
  fileId: string
  filePid: string
  fileName: string
  fileSuffix?: string
  fileSize: number
  fileSizeStr: string
  isFolder: number
  createTime: string
  lastOpTime: string
  userId: string
}

// 分页结果
export interface FileListResponse {
  total: number
  pageSize: number
  pageNo: number
  pageTotal: number
  records: FileInfo[]
}

// ==================== 文件基础操作 ====================

// 加载文件列表
export const loadDataList = (params: FileListQuery): Promise<ApiResponse<FileListResponse>> => {
  return request({
    url: '/file/loadDataList',
    method: 'POST',
    data: params
  })
}

// 创建文件夹
export const createFolder = (params: { filePid: string; folderName: string }) => {
  const formData = new URLSearchParams()
  formData.append('filePid', params.filePid)
  formData.append('folderName', params.folderName)
  return request({
    url: '/file/createFolder',
    method: 'POST',
    data: formData,
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' }
  })
}

// 重命名文件
export const renameFile = (params: { fileId: string; newFileName: string }) => {
  return request({
    url: '/file/rename',
    method: 'POST',
    data: qs.stringify(params),
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' }
  })
}
// 删除文件
export const deleteFile = (params: { fileId: string }) => {
  return request({
    url: '/file/delete',
    method: 'POST',
    data: qs.stringify(params),
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' }
  })
}

// 移动文件
export const moveFile = (params: { fileId: string; targetPid: string }) => {
  return request({
    url: '/file/move',
    method: 'POST',
    data: qs.stringify(params),
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' }
  })
}

// 获取回收站列表
export const getRecycleList = (params: { pageNo?: number; pageSize?: number }) => {
  return request({
    url: '/file/recycle/list',
    method: 'POST',
    data: params
  })
}

// 恢复文件
export const recoverFile = (fileId: string) => {
  return request({
    url: '/file/recycle/recover',
    method: 'POST',
    data: qs.stringify({ fileId }),
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' }
  })
}

// 永久删除文件
export const permanentDelete = (fileId: string) => {
  return request({
    url: '/file/recycle/delete',
    method: 'POST',
    data: qs.stringify({ fileId }),
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' }
  })
}

// 创建下载链接
export const createDownloadUrl = (fileId: string) => {
  return request({
    url: '/file/createDownloadUrl',
    method: 'POST',
    data: qs.stringify({ fileId }),
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' }
  })
}

// 下载文件
export const downloadFile = (code: string, fileId: string) => {
  return request({
    url: `/file/downloadFile/${code}`,
    method: 'GET',
    responseType: 'blob',
    params: { fileId }
  })
}

// ==================== 文件分享相关 ====================

// 创建分享
export const createShare = (params: { fileId: string; expireType: number }) => {
  return request({
    url: '/file/share/create',
    method: 'POST',
    data: qs.stringify(params),
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' }
  })
}

// 获取当前用户的分享列表（无分页参数，直接返回全部）
export const getShareList = () => {
  return request({
    url: '/file/share/list',
    method: 'POST'
  })
}

export const getFolderTree = () => {
  return request({
    url: '/file/getFolderTree',
    method: 'POST'
  })
}

// 取消分享
export const cancelShare = (shareId: string) => {
  return request({
    url: '/file/share/cancel',
    method: 'POST',
    data: qs.stringify({ shareId }),
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' }
  })
}

// 校验分享提取码（公开接口）
export const verifyShareCode = (shareId: string, shareCode: string) => {
  return request({
    url: '/file/share/verifyCode',
    method: 'POST',
    data: qs.stringify({ shareId, shareCode }),
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' }
  })
}

// 获取分享文件信息（公开接口）
export const getShareFileInfo = (shareId: string) => {
  return request({
    url: '/file/share/getFileInfo',
    method: 'GET',
    params: { shareId }
  })
}

// 保存分享文件到当前用户网盘
export const saveShareFile = (shareId: string, filePid: string) => {
  return request({
    url: '/file/share/save',
    method: 'POST',
    data: qs.stringify({ shareId, filePid }),
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' }
  })
}

// ==================== 分片上传相关 ====================

// 初始化分片上传
export const initChunkUpload = (params: {
  fileMd5: string
  fileName: string
  fileSize: number
  chunkSize: number
  filePid: string
}) => {
  const formData = new FormData()
  Object.entries(params).forEach(([key, value]) => {
    formData.append(key, value.toString())
  })
  return request({
    url: '/file/chunk/init',
    method: 'POST',
    data: formData
  })
}

// 上传分片
export const uploadChunk = (params: {
  uploadId: string
  chunkIndex: number
  file: File
}) => {
  const formData = new FormData()
  formData.append('uploadId', params.uploadId)
  formData.append('chunkIndex', params.chunkIndex.toString())
  formData.append('file', params.file)
  return request({
    url: '/file/chunk/upload',
    method: 'POST',
    data: formData
  })
}

// 合并分片
export const mergeChunk = (params: { uploadId: string }) => {
  const formData = new FormData()
  formData.append('uploadId', params.uploadId)
  return request({
    url: '/file/chunk/merge',
    method: 'POST',
    data: formData
  })
}

// 查询已上传分片
export const getUploadedChunks = (params: { uploadId: string }) => {
  return request({
    url: '/file/chunk/getUploadedChunks',
    method: 'GET',
    params
  })
}