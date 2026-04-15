import axios, { type AxiosInstance, type AxiosResponse } from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'

const service: AxiosInstance = axios.create({
  baseURL: '/api',   // ✅ 所有请求自动加上 /api 前缀
  timeout: 15000,
  headers: { 'Content-Type': 'application/json' }
})

// 请求拦截器：添加 token
service.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers['token'] = token
    }
    // FormData 时删除 Content-Type，让浏览器自动设置
    if (config.data instanceof FormData) {
      delete config.headers['Content-Type']
    }
    return config
  },
  (error) => Promise.reject(error)
)

// 响应拦截器
service.interceptors.response.use(
  (response: AxiosResponse) => {
    const { data } = response
    // 后端统一返回 { code, status, info, data }
    if (data.code === 200) {
      return data   // ✅ 返回完整响应体，调用方可取 data.data
    } else {
      ElMessage.error(data.info || '请求失败')
      return Promise.reject(new Error(data.info))
    }
  },
  (error) => {
    if (error.response) {
      const { status, data } = error.response
      switch (status) {
        case 401:
          ElMessage.error('登录已过期，请重新登录')
          localStorage.clear()
          router.push('/login')
          break
        case 404:
          // 不显示404错误消息，让调用方处理
          break
        case 500:
          ElMessage.error('服务器内部错误')
          break
        default:
          ElMessage.error(data?.info || `请求失败 (${status})`)
      }
    } else {
      ElMessage.error('网络连接失败，请检查后端服务是否启动')
    }
    return Promise.reject(error)
  }
)

export { service as request }
export default service