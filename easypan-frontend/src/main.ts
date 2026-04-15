import { createApp } from 'vue'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import router from '@/router'
import { createPinia } from 'pinia'
import App from './App.vue'
import { request } from './utils/request'

const app = createApp(App)
app.use(ElementPlus)

// 全局错误处理
app.config.errorHandler = (err, instance, info) => {
  console.error('全局错误:', err)
  console.error('错误信息:', info)
  // 可以在这里添加更详细的错误上报
}

app.use(router)
app.use(createPinia())
app.mount('#app')