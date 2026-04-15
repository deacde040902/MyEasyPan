import { request } from '@/utils/request'

// 登录参数（完全匹配后端 LoginDTO）
export interface LoginParams {
  email: string
  password: string
  checkCode: string
  checkCodeKey: string
}

// 登录响应
export interface LoginResponse {
  status: string
  code: number
  info: string
  data: {
    token: string
    userId: string      // 用户ID
    nickName?: string   // 昵称（可选，但后端实际返回了）
    isVip?: string      // 是否VIP
    isAdmin?: string    // 是否管理员
  }
}

// 统一响应格式
export interface ApiResponse<T = any> {
  status: string
  code: number
  info: string
  data: T
}

// 注册参数
export interface RegisterParams {
  email: string
  emailCode: string
  nickName: string
  password: string
  checkCode: string
  checkCodeKey: string
}

// --- API 接口 ---

// 1. 获取图片验证码
export const getCheckCode = async (): Promise<ApiResponse<{
  checkCode: string
  checkCodeKey: string
}>> => {
  const response = await request({
    url: '/userInfo/checkCode',
    method: 'GET',
    headers: {
      'Cache-Control': 'no-cache',
      'Pragma': 'no-cache'
    }
  })
  return response as unknown as ApiResponse<{
    checkCode: string
    checkCodeKey: string
  }>
}

// 2. 登录接口
export const checkCodeLogin = async (params: LoginParams): Promise<LoginResponse> => {
  const response = await request({
    url: '/userInfo/login',
    method: 'POST',
    data: params
  })
  return response as unknown as LoginResponse
}

// 3. 管理员登录接口（绕过状态校验）
export const adminLogin = async (params: LoginParams): Promise<LoginResponse> => {
  const response = await request({
    url: '/userInfo/login',
    method: 'POST',
    data: {
      ...params,
      isAdmin: true
    }
  })
  return response as unknown as LoginResponse
}

// 3. 注册接口
export const register = (data: RegisterParams): Promise<ApiResponse<any>> => {
  return request({ url: '/userInfo/register', method: 'POST', data }) as unknown as Promise<ApiResponse<any>>
}

// 4. 发送邮箱验证码
export const sendEmailCode = (data: SendEmailCodeParams): Promise<ApiResponse<any>> => {
  return request({ url: '/userInfo/sendEmailCode', method: 'POST', data }) as unknown as Promise<ApiResponse<any>>
}

// 5. QQ登录回调
export const qqLoginCallback = (code: string): Promise<ApiResponse<any>> => {
  return request({
    url: '/qqlogin/callback',
    method: 'GET',
    params: { code }
  }) as unknown as Promise<ApiResponse<any>>
}

// 6. 获取用户信息
export const getUserInfo = async (userId: string): Promise<ApiResponse<any>> => {
  const response = await request({
    url: `/userInfo/getUserInfo/${userId}`,
    method: 'GET'
  })
  return response as unknown as ApiResponse<any>
}

// 7. 修改密码（登录状态下）
export const changePwd = (data: {
  oldPassword: string;
  newPassword: string;
}): Promise<ApiResponse<any>> => {
  return request({
    url: '/userInfo/changePwd',
    method: 'POST',
    data
  }) as unknown as Promise<ApiResponse<any>>
}

// 8. 重置密码（忘记密码）
export const resetPwd = (data: {
  email: string;
  emailCode: string;
  newPwd: string;
  confirmPwd: string;
  checkCode: string;
  checkCodeKey: string;
}): Promise<ApiResponse<any>> => {
  return request({
    url: '/userInfo/resetPwd',
    method: 'POST',
    data
  }) as unknown as Promise<ApiResponse<any>>
}

// 8. 退出登录
export const logout = async (): Promise<ApiResponse<void>> => {
  const response = await request({
    url: '/logout',
    method: 'GET'
  })
  return response as unknown as ApiResponse<void>
}

// 获取用户空间使用情况（token 由拦截器自动携带）
export const getUseSpace = async (): Promise<ApiResponse<{
  userId: string
  nickName: string
  email: string
  avatarPath: string | null
  useSpace: number
  totalSpace: number
  useSpaceStr: string
  totalSpaceStr: string
  usageRate: string
  isVip: boolean
  vipLevelName: string
}>> => {
  const response = await request({
    url: '/userInfo/getUseSpace',
    method: 'GET'
  })
  return response as unknown as Promise<ApiResponse<{
  userId: string
  nickName: string
  email: string
  avatarPath: string | null
  useSpace: number
  totalSpace: number
  useSpaceStr: string
  totalSpaceStr: string
  usageRate: string
  isVip: boolean
  vipLevelName: string
}>>
}

// 上传/更新用户头像
export const uploadAvatar = (data: FormData): Promise<ApiResponse<string>> => {
  return request({
    url: '/userInfo/uploadAvatar',
    method: 'POST',
    data,
  }) as unknown as Promise<ApiResponse<string>>
}

// 修改用户昵称
export const updateNickName = (newNickName: string): Promise<ApiResponse<any>> => {
  return request({
    url: '/userInfo/updateNickName',
    method: 'POST',
    data: { newNickName }
  }) as unknown as Promise<ApiResponse<any>>
}

// 开通VIP
export const openVip = (months: number): Promise<ApiResponse<any>> => {
  return request({
    url: '/vip/open',
    method: 'POST',
    params: { months }
  }) as unknown as Promise<ApiResponse<any>>
}

export interface SendEmailCodeParams {
  email: string
  checkCode: string
  checkCodeKey: string
  type: string   // '0' 或 '1'
}