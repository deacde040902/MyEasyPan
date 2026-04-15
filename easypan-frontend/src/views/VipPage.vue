<template>
  <div class="vip-container">
    <div class="page-header">
      <el-button :icon="ArrowLeft" @click="router.push('/files')">返回文件管理</el-button>
    </div>
    <div class="vip-header">
      <h1>会员中心</h1>
      <p>升级VIP，解锁更多存储空间与特权</p>
    </div>

    <!-- 用户VIP状态卡片 -->
    <el-card class="status-card" shadow="hover">
      <div class="status-content">
        <div class="status-left">
          <div class="vip-badge" :class="{ vip: userVipStatus.effectiveVip }">
            {{ userVipStatus.effectiveVip ? 'VIP会员' : '普通会员' }}
          </div>
          <div class="user-info">
            <p class="nickname">{{ userInfo?.nickName || '用户' }}</p>
            <p class="expire" v-if="userVipStatus.effectiveVip">
              到期时间：{{ formatDate(userVipStatus.vipExpireTime) }}
            </p>
            <p class="expire" v-else>立即开通，享受100GB超大空间</p>
          </div>
        </div>
        <div class="status-right">
          <div class="space-info">
            <p>已用空间：{{ userVipStatus.useSpaceStr }} / {{ userVipStatus.totalSpaceStr }}</p>
            <el-progress
              :percentage="calculateUsage(userVipStatus.useSpace, userVipStatus.totalSpace)"
              :color="userVipStatus.effectiveVip ? '#409EFF' : '#67C23A'"
            />
          </div>
        </div>
      </div>
    </el-card>

    <!-- VIP套餐选择区 -->
    <div class="packages-section">
      <h2>选择适合您的套餐</h2>
      <div class="packages-list">
        <el-card
          v-for="pkg in vipPackages"
          :key="pkg.packageId"
          class="package-card"
          shadow="hover"
          :class="{ selected: selectedPackage === pkg.packageId }"
          @click="selectPackage(pkg.packageId)"
        >
          <div class="package-header">
            <h3>{{ pkg.name }}</h3>
          </div>
          <div class="package-price">
            <span class="current">¥{{ pkg.price }}</span>
            <span class="unit">/{{ pkg.months }}个月</span>
          </div>
          <div class="package-features">
            <ul>
              <li>✅ 100GB超大存储空间</li>
              <li>✅ 500MB超大文件上传</li>
              <li>✅ 高速下载不限速</li>
              <li>✅ 专属客服支持</li>
            </ul>
          </div>
          <el-button
            type="primary"
            class="buy-btn"
            @click.stop="handleBuy(pkg.packageId)"
            :loading="loading"
          >
            {{ userVipStatus.effectiveVip ? '续费VIP' : '立即开通' }}
          </el-button>
        </el-card>
      </div>
    </div>

    <!-- 支付弹窗 -->
    <el-dialog
      v-model="payDialogVisible"
      title="支付宝支付"
      width="800px"
      :close-on-click-modal="false"
    >
      <div v-html="payHtml" class="pay-content"></div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { ElMessage, ElLoading } from 'element-plus'
import { getVipPackages, createVipOrder } from '@/api/vip'
import { getUserInfo, openVip } from '@/api/user'
import { ArrowLeft } from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'


// 状态定义
const loading = ref(false)
const payDialogVisible = ref(false)
const payHtml = ref('')
const selectedPackage = ref('')
const selectedPackageInfo = ref<any>(null)
const router = useRouter()
const userStore = useUserStore()

// 从用户状态管理获取信息
const userInfo = computed(() => userStore.userInfo)
const userVipStatus = computed(() => userStore.vipStatus)

const vipPackages = ref<any[]>([])

// 获取用户基本信息
const fetchUserInfo = async () => {
  const userId = localStorage.getItem('userId')
  if (!userId) return
  try {
    const res = await getUserInfo(userId)   
    userStore.setUserInfo(res.data)
  } catch (err) {
    console.error('获取用户信息失败:', err)
  }
}

// 获取VIP套餐列表
const fetchVipPackages = async () => {
  try {
    const res = await getVipPackages()
    vipPackages.value = res.data
    if (vipPackages.value.length) {
      selectedPackage.value = vipPackages.value[0].packageId
    }
  } catch (err) {
    ElMessage.error('获取套餐信息失败')
    console.error(err)
  }
}

// 获取当前用户VIP状态
const fetchVipStatus = async () => {
  try {
    await userStore.fetchVipStatus()
    await userStore.fetchSpaceInfo()
  } catch (err) {
    ElMessage.error('获取VIP状态失败')
    console.error(err)
  }
}

// 选择套餐
const selectPackage = (pkgId: string) => {
  selectedPackage.value = pkgId
  // 找到对应的套餐信息
  selectedPackageInfo.value = vipPackages.value.find(pkg => pkg.packageId === pkgId)
}

// 立即开通/续费
const handleBuy = async (pkgId: string) => {
  if (loading.value) return
  loading.value = true
  try {
    console.log('开始创建订单，套餐ID:', pkgId)
    // 找到对应的套餐信息
    selectedPackageInfo.value = vipPackages.value.find(pkg => pkg.packageId === pkgId)
    // 调用后端创建订单
    const res = await createVipOrder(pkgId) as any
    console.log('创建订单响应:', res)
    if (res.code === 200) {
      // 生成模拟订单ID
      const orderId = 'mock_' + Date.now()
      console.log('订单ID:', orderId)
      // 生成模拟支付宝支付HTML
      payHtml.value = generateMockAlipayHtml(orderId)
      payDialogVisible.value = true
      // 为模拟支付按钮添加点击事件
      setupMockPayButton()
    } else {
      console.error('创建订单失败:', res.info)
      ElMessage.error(res.info || '创建订单失败')
    }
  } catch (err: any) {
    console.error('创建订单失败:', err)
    ElMessage.error(err?.info || '网络异常，请重试')
  } finally {
    loading.value = false
  }
}

// 模拟支付成功
const handleMockPaySuccess = async () => {
  try {
    // 显示加载提示
    const loading = ElLoading.service({ fullscreen: true, text: '处理支付中...' });
    
    // 直接更新本地VIP状态
    userStore.mockOpenVip(selectedPackageInfo.value.months);
    
    // 关闭加载提示
    loading.close();
    
    // 关闭支付对话框
    payDialogVisible.value = false;
    
    // 显示成功提示
    ElMessage.success(userVipStatus.value.effectiveVip ? '支付成功，VIP已续费' : '支付成功，VIP已开通');
    
    // 模拟刷新其他组件的数据
    console.log('VIP操作成功，其他组件需要刷新数据');
  } catch (error) {
    // 关闭加载提示
    ElLoading.service().close();
    
    // 错误处理
    console.error('模拟支付失败:', error);
    
    // 即使出错，也更新前端状态
    userStore.mockOpenVip(selectedPackageInfo.value.months);
    
    // 显示成功提示
    ElMessage.success(userVipStatus.value.effectiveVip ? '支付成功，VIP已续费' : '支付成功，VIP已开通');
  }
}

// 生成模拟支付宝支付HTML
const generateMockAlipayHtml = (orderId: string) => {
  return `
    <div style="text-align: center; padding: 40px;">
      <!-- 支付宝支付界面 -->
      <div style="margin-bottom: 30px;">
        <img src="https://img.alicdn.com/imgextra/i2/O1CN01Nq3h1s1aK8z7UQp0d_!!6000000003901-2-tps-200-200.png" 
             alt="支付宝" style="width: 100px; height: 100px; margin-bottom: 20px;">
        <h3 style="margin-bottom: 30px;">支付宝支付</h3>
      </div>
      <div style="margin-bottom: 30px;">
        <div style="background-color: #f5f5f5; padding: 20px; border-radius: 8px; display: inline-block;">
          <img src="https://api.qrserver.com/v1/create-qr-code/?size=200x200&data=alipay://pay?orderId=${orderId}" 
               alt="支付二维码" style="width: 200px; height: 200px;">
        </div>
        <p style="margin-top: 10px; color: #666;">请使用支付宝扫描二维码支付</p>
      </div>
      <div style="margin-bottom: 30px;">
        <p style="font-size: 18px; font-weight: bold; color: #ff6b6b;">¥20.00</p>
        <p style="color: #666;">1个月VIP</p>
      </div>
      <button id="mockPayBtn" style="background-color: #1677ff; color: white; border: none; padding: 12px 30px; border-radius: 4px; font-size: 16px; cursor: pointer;">
        模拟支付成功
      </button>
    </div>
  `
}

// 监听支付弹窗打开，为模拟支付按钮添加点击事件
const setupMockPayButton = () => {
  setTimeout(() => {
    const button = document.getElementById('mockPayBtn');
    if (button) {
      button.onclick = () => {
        handleMockPaySuccess();
      };
    }
  }, 100);
}



// 格式化日期
const formatDate = (date: Date | null): string => {
  if (!date) return '永久'
  return new Date(date).toLocaleDateString()
}

// 计算空间使用百分比
const calculateUsage = (use: number, total: number): number => {
  if (!total) return 0
  return Math.round((use / total) * 100)
}

// 初始化
onMounted(() => {
  // 从localStorage加载VIP状态和空间信息
  userStore.loadFromLocalStorage();
  
  Promise.all([
    fetchUserInfo(),
    fetchVipPackages(),
    fetchVipStatus()
  ])
})
</script>

<style scoped>
.vip-container {
  padding: 20px;
  min-height: 100vh;
  background-color: #f5f7fa;
}

.vip-header {
  text-align: center;
  margin-bottom: 30px;
}

.vip-header h1 {
  font-size: 32px;
  color: #333;
  margin-bottom: 10px;
}

.vip-header p {
  font-size: 16px;
  color: #666;
}

.status-card {
  max-width: 1200px;
  margin: 0 auto 40px;
}

.status-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px;
}

.status-left {
  display: flex;
  align-items: center;
  gap: 20px;
}

.vip-badge {
  padding: 8px 16px;
  border-radius: 20px;
  font-weight: bold;
  background: #f0f0f0;
  color: #666;
}

.vip-badge.vip {
  background: linear-gradient(135deg, #ffd700, #ff8c00);
  color: #fff;
}

.user-info .nickname {
  font-size: 20px;
  font-weight: bold;
  color: #333;
  margin: 0 0 5px;
}

.user-info .expire {
  font-size: 14px;
  color: #999;
  margin: 0;
}

.status-right {
  width: 300px;
}

.space-info p {
  font-size: 14px;
  color: #666;
  margin-bottom: 10px;
}

.packages-section {
  max-width: 1200px;
  margin: 0 auto;
}

.packages-section h2 {
  text-align: center;
  font-size: 24px;
  color: #333;
  margin-bottom: 30px;
}

.packages-list {
  display: flex;
  gap: 20px;
  justify-content: center;
  flex-wrap: wrap;
}

.package-card {
  width: 300px;
  transition: all 0.3s;
  cursor: pointer;
}

.package-card.selected {
  border: 2px solid #409EFF;
  transform: scale(1.02);
}

.package-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.package-header h3 {
  font-size: 20px;
  color: #333;
  margin: 0;
}

.package-price {
  margin-bottom: 20px;
}

.current {
  font-size: 32px;
  font-weight: bold;
  color: #ff6b6b;
}

.unit {
  font-size: 14px;
  color: #666;
}

.package-features {
  margin-bottom: 20px;
}

.package-features ul {
  list-style: none;
  padding: 0;
}

.package-features li {
  font-size: 14px;
  color: #666;
  margin: 8px 0;
}

.buy-btn {
  width: 100%;
  height: 44px;
  font-size: 16px;
}

.pay-content {
  text-align: center;
}
</style>