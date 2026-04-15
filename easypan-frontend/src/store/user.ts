import { defineStore } from 'pinia'
import { getUserInfo, getUseSpace } from '@/api/user'
import { getVipStatus } from '@/api/vip'

export const useUserStore = defineStore('user', {
  state: () => ({
    userInfo: {
      userId: '',
      email: '',
      nickName: '',
      avatar: '',
      createTime: ''
    },
    vipStatus: {
      effectiveVip: false,
      vipLevel: 0,
      vipExpireTime: null as Date | null,
      totalSpace: 0,
      useSpace: 0,
      useSpaceStr: '0B',
      totalSpaceStr: '0B'
    },
    spaceInfo: {
      useSpace: 0,
      totalSpace: 0,
      useSpaceStr: '0B',
      totalSpaceStr: '0B',
      usageRate: '0.00%',
      isVip: false,
      vipLevelName: '普通会员'
    }
  }),

  actions: {
    setUserInfo(info: any) {
      // 确保即使后端返回的字段名与前端期望的不一致，也能正确处理
      this.userInfo = {
        userId: info.userId || info.user_id || '',
        email: info.email || '',
        nickName: info.nickName || info.name || info.nickname || '',
        avatar: info.avatar || info.avatarPath || '',
        createTime: info.createTime || info.create_time || ''
      }
    },

    setVipStatus(info: any) {
      this.vipStatus = info
    },

    setSpaceInfo(info: any) {
      this.spaceInfo = info
    },

    /**
     * 获取当前登录用户的信息
     * 注意：需要从 localStorage 中获取 userId
     */
    async fetchUserInfo() {
      const userId = localStorage.getItem('userId')
      if (!userId) {
        console.warn('用户未登录或 userId 丢失，无法获取用户信息')
        return
      }

      try {
        console.log('获取用户信息，userId:', userId)
        const res = await getUserInfo(userId)
        if (res.code === 200) {
          this.setUserInfo(res.data)
        } else {
          console.error('获取用户信息失败:', res.info)
          // 如果获取用户信息失败，清除localStorage中的token和userId，然后跳转到登录页面
          localStorage.removeItem('token')
          localStorage.removeItem('userId')
          localStorage.removeItem('isAdmin')
          this.clearUserInfo()
          // 跳转到登录页面
          window.location.href = '/login'
        }
      } catch (error) {
        console.error('获取用户信息异常:', error)
        // 如果获取用户信息异常，清除localStorage中的token和userId，然后跳转到登录页面
        localStorage.removeItem('token')
        localStorage.removeItem('userId')
        localStorage.removeItem('isAdmin')
        this.clearUserInfo()
        // 跳转到登录页面
        window.location.href = '/login'
      }
    },

    /**
     * 获取VIP状态
     */
    async fetchVipStatus() {
      try {
        const res = await getVipStatus()
        if (res.code === 200) {
          this.setVipStatus(res.data)
        } else {
          console.error('获取VIP状态失败:', res.info)
        }
      } catch (error) {
        console.error('获取VIP状态异常:', error)
      }
    },

    /**
     * 获取空间信息
     */
    async fetchSpaceInfo() {
      try {
        const res = await getUseSpace()
        if (res.code === 200) {
          this.setSpaceInfo(res.data)
        } else {
          console.error('获取空间信息失败:', res.info)
        }
      } catch (error) {
        console.error('获取空间信息异常:', error)
      }
    },

    /**
     * 模拟开通VIP
     */
    mockOpenVip(months: number = 1) {
      // 计算过期时间
      const expireTime = new Date();
      if (this.vipStatus.vipExpireTime && this.vipStatus.vipExpireTime > new Date()) {
        // 如果当前已是VIP且未过期，在原有过期时间上叠加
        expireTime.setTime(this.vipStatus.vipExpireTime.getTime());
        expireTime.setMonth(expireTime.getMonth() + months);
      } else {
        // 新开通或已过期，从当前时间开始计算
        expireTime.setMonth(expireTime.getMonth() + months);
      }
      
      // 更新VIP状态
      this.vipStatus = {
        effectiveVip: true,
        vipLevel: 1,
        vipExpireTime: expireTime,
        totalSpace: 1024 * 1024 * 1024 * 100, // 100GB
        useSpace: this.vipStatus.useSpace,
        useSpaceStr: this.vipStatus.useSpaceStr,
        totalSpaceStr: '100GB'
      };
      
      // 更新空间信息
      this.spaceInfo = {
        useSpace: this.spaceInfo.useSpace,
        totalSpace: 1024 * 1024 * 1024 * 100, // 100GB
        useSpaceStr: this.spaceInfo.useSpaceStr,
        totalSpaceStr: '100GB',
        usageRate: '0.00%',
        isVip: true,
        vipLevelName: 'VIP会员'
      };
      
      // 存储到localStorage
      localStorage.setItem('vipStatus', JSON.stringify(this.vipStatus));
      localStorage.setItem('spaceInfo', JSON.stringify(this.spaceInfo));
    },

    /**
     * 从localStorage加载VIP状态和空间信息
     */
    loadFromLocalStorage() {
      const vipStatusStr = localStorage.getItem('vipStatus');
      const spaceInfoStr = localStorage.getItem('spaceInfo');
      
      if (vipStatusStr) {
        try {
          this.vipStatus = JSON.parse(vipStatusStr);
        } catch (error) {
          console.error('解析VIP状态失败:', error);
        }
      }
      
      if (spaceInfoStr) {
        try {
          this.spaceInfo = JSON.parse(spaceInfoStr);
        } catch (error) {
          console.error('解析空间信息失败:', error);
        }
      }
    },

    /**
     * 清空用户信息（用于退出登录）
     */
    clearUserInfo() {
      this.userInfo = {
        userId: '',
        email: '',
        nickName: '',
        avatar: '',
        createTime: ''
      };
      
      this.vipStatus = {
        effectiveVip: false,
        vipLevel: 0,
        vipExpireTime: null,
        totalSpace: 0,
        useSpace: 0,
        useSpaceStr: '0B',
        totalSpaceStr: '0B'
      };
      
      this.spaceInfo = {
        useSpace: 0,
        totalSpace: 0,
        useSpaceStr: '0B',
        totalSpaceStr: '0B',
        usageRate: '0.00%',
        isVip: false,
        vipLevelName: '普通会员'
      };
      
      // 清除localStorage中的信息
      localStorage.removeItem('vipStatus');
      localStorage.removeItem('spaceInfo');
    }
  }
})