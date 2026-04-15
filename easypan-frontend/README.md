# Easy云盘前端项目

基于 Vue3 + TypeScript + Element Plus 构建的云盘系统前端界面。

## 项目结构

```
src/
├── api/              # API 接口
│   └── user.ts      # 用户相关接口
├── router/           # 路由配置
│   └── index.ts     # 路由主文件
├── utils/           # 工具函数
│   └── request.ts   # axios 请求封装
├── views/           # 页面组件
│   ├── Login.vue    # 登录页面
│   ├── Home.vue     # 首页
│   ├── Register.vue # 注册页面
│   └── ForgetPassword.vue # 忘记密码页面
├── App.vue          # 根组件
└── main.ts          # 入口文件
```

## 功能特性

### 登录页面 (Login.vue)
- **左侧蓝色渐变背景**：使用 `linear-gradient(135deg, #667eea 0%, #764ba2 100%)` 渐变
- **办公插画**：支持 `/public/images/login-illustration.png`
- **白色登录卡片**：圆角设计，带阴影效果
- **Easy云盘标题**：醒目的品牌标识
- **邮箱/密码输入框**：带图标和验证
- **验证码输入框+图片**：支持点击刷新验证码
- **记住我复选框**：持久化存储用户邮箱
- **忘记密码/注册链接**：页面导航功能
- **蓝色登录按钮**：渐变色按钮，hover效果
- **QQ快捷登录**：QQ OAuth集成
- **响应式设计**：适配移动端

### 核心功能实现

1. **验证码刷新**：
   - 点击验证码图片即可刷新
   - 添加时间戳防止缓存

2. **登录请求**：
   - 调用 `/api/account/login` 接口
   - 表单验证（邮箱格式、密码长度、验证码）
   - loading状态管理

3. **Token存储**：
   - 登录成功后存储到 `localStorage`
   - 每次请求自动添加 token 头

4. **跳转首页**：
   - 登录成功后自动跳转
   - 支持 redirect 参数

5. **记住我**：
   - 记住用户邮箱
   - 下次自动填充

## 技术栈

- **Vue 3**：渐进式 JavaScript 框架
- **TypeScript**：JavaScript 的超集，添加了静态类型
- **Vue Router 4**：官方路由管理器
- **Element Plus**：基于 Vue 3 的 UI 组件库
- **Axios**：HTTP 客户端
- **Vite**：现代化的前端构建工具

## 环境要求

- Node.js >= 16
- npm >= 7

## 安装依赖

```bash
cd easypan-frontend
npm install
```

## 开发运行

```bash
npm run dev
```

启动后访问：http://localhost:5173

## 构建生产版本

```bash
npm run build
```

## 预览生产版本

```bash
npm run preview
```

## 后端配置

项目已配置代理，将 `/api` 请求转发到后端服务器：
- 后端地址：http://localhost:7090
- 支持的接口：
  - `POST /api/account/login` - 登录
  - `GET /api/checkCode` - 获取验证码
  - `GET /api/qqlogin/callback` - QQ登录回调
  - `POST /api/logout` - 退出登录
  - `GET /api/account/userinfo` - 获取用户信息

## 页面截图

登录页面严格按照设计图实现：
- 左侧：蓝色渐变背景 + 办公插画
- 右侧：白色登录卡片，包含所有表单元素
- 响应式布局，适配各种屏幕尺寸

## 注意事项

1. 确保后端服务在 `http://localhost:7090` 运行
2. 验证码图片路径：`/api/checkCode`
3. QQ登录需要配置正确的 AppID 和 AppKey
4. 建议准备 `login-illustration.png` 放在 `/public/images/` 目录下
5. 项目使用 `localStorage` 存储 token，需要处理登录过期的情况