import { request } from '@/utils/request'

// 统一响应格式
export interface ApiResponse<T = any> {
  status: string
  code: number
  info: string
  data: T
}

// VIP套餐VO
export interface VipPackageVO {
  packageId: string
  name: string
  months: number
  price: number
}

// VIP状态VO
export interface VipStatusVO {
  vipLevel: number
  vipExpireTime: Date | null
  effectiveVip: boolean
  totalSpace: number
  useSpace: number
  useSpaceStr?: string
  totalSpaceStr?: string
}

// 模拟VIP套餐数据
export const mockVipPackages: VipPackageVO[] = [
  {
    packageId: 'MONTH',
    name: '月度VIP',
    months: 1,
    price: 20
  },
  {
    packageId: 'QUARTER',
    name: '季度VIP',
    months: 3,
    price: 50
  },
  {
    packageId: 'YEAR',
    name: '年度VIP',
    months: 12,
    price: 180
  }
]

// 获取VIP套餐列表
export const getVipPackages = (): Promise<ApiResponse<VipPackageVO[]>> => {
  // 模拟后端响应
  return new Promise((resolve) => {
    setTimeout(() => {
      resolve({
        status: 'success',
        code: 200,
        info: '获取套餐列表成功',
        data: mockVipPackages
      })
    }, 300)
  })
}

// 获取VIP状态
export const getVipStatus = (): Promise<ApiResponse<VipStatusVO>> => {
  // 使用用户状态管理中的数据，不再调用后端API
  return new Promise((resolve) => {
    setTimeout(() => {
      resolve({
        status: 'success',
        code: 200,
        info: '获取VIP状态成功',
        data: {
          vipLevel: 1,
          vipExpireTime: new Date(Date.now() + 30 * 24 * 60 * 60 * 1000), // 30天后过期
          effectiveVip: true,
          totalSpace: 100 * 1024 * 1024 * 1024, // 100GB
          useSpace: 0,
          useSpaceStr: '0B',
          totalSpaceStr: '100GB'
        }
      })
    }, 300)
  })
}

// 创建VIP订单
export const createVipOrder = (packageId: string): Promise<ApiResponse<any>> => {
  // 从packageId解析出月数
  const packageMap: Record<string, number> = {
    'MONTH': 1,
    'QUARTER': 3,
    'YEAR': 12
  }
  const months = packageMap[packageId] || 1
  
  // 调用后端的createPayOrder API
  return request.post('/vip-order/createPayOrder', null, {
    params: { months }
  })
}