import { request } from '@/utils/request'
import type { ApiResponse } from './user'

// 管理员相关API

// 1. 获取用户列表
export const getUserList = async (params: {
  page: number;
  pageSize: number;
  keyword?: string;
}): Promise<ApiResponse<any>> => {
  const response = await request({
    url: '/admin/user/list',
    method: 'GET',
    params
  })
  return response as unknown as ApiResponse<any>
}

// 2. 修改用户状态
export const changeUserStatus = async (userId: string, status: number): Promise<ApiResponse<any>> => {
  const response = await request({
    url: '/admin/user/status',
    method: 'POST',
    params: {
      userId,
      status
    }
  })
  return response as unknown as ApiResponse<any>
}

// 3. 修改用户空间
export const changeUserSpace = async (userId: string, totalSpace: number): Promise<ApiResponse<any>> => {
  const response = await request({
    url: '/admin/user/space',
    method: 'POST',
    params: {
      userId,
      totalSpace
    }
  })
  return response as unknown as ApiResponse<any>
}

// 4. 获取文件列表
export const getFileList = async (params: {
  page: number;
  pageSize: number;
  keyword?: string;
}): Promise<ApiResponse<any>> => {
  const response = await request({
    url: '/admin/file/list',
    method: 'GET',
    params
  })
  return response as unknown as ApiResponse<any>
}

// 5. 删除文件
export const deleteFile = async (fileId: string): Promise<ApiResponse<any>> => {
  const response = await request({
    url: '/admin/file/delete',
    method: 'POST',
    params: {
      fileId
    }
  })
  return response as unknown as ApiResponse<any>
}

// 6. 下载文件
export const downloadFile = async (fileId: string): Promise<Blob> => {
  const response = await request({
    url: '/admin/file/download',
    method: 'GET',
    responseType: 'blob',
    params: {
      fileId
    }
  })
  return response as unknown as Blob
}

// 7. 获取系统设置
export const getSystemSettings = async (): Promise<ApiResponse<any>> => {
  const response = await request({
    url: '/admin/settings',
    method: 'GET'
  })
  return response as unknown as ApiResponse<any>
}

// 8. 保存系统设置
export const saveSystemSettings = async (data: {
  systemName: string;
  defaultSpace: string;
  vipSpace: string;
}): Promise<ApiResponse<any>> => {
  const response = await request({
    url: '/admin/settings',
    method: 'POST',
    data
  })
  return response as unknown as ApiResponse<any>
}

// 9. 获取统计数据
export const getDashboardStats = async (): Promise<ApiResponse<any>> => {
  const response = await request({
    url: '/admin/dashboard/stats',
    method: 'GET'
  })
  return response as unknown as ApiResponse<any>
}
