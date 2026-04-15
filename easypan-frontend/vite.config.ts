import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'

export default defineConfig({
  plugins: [
    vue(),
    AutoImport({ resolvers: [ElementPlusResolver()] }),
    Components({ resolvers: [ElementPlusResolver()] }),
  ],
  resolve: {
    alias: { '@': path.resolve(__dirname, 'src') },
  },
  server: {
    port: 3000,
    proxy: {
      // 唯一代理：所有/api请求转发到后端，不重写
      '/api': {
        target: 'http://localhost:7090',
        changeOrigin: true,
        // ❶ 关键：不重写路径！后端已经有/api前缀
        rewrite: (path) => path
      }
    }
  }
})