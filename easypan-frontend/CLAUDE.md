\# EasyPan 前端 Vue3 项目规范文档

\## 项目核心信息

\- 项目名称：EasyPan 云盘前端

\- 技术栈：Vue3 + Vite + TypeScript + Element Plus + Axios + Pinia

\- 后端接口：基于 Spring Boot 开发的 EasyPan 后端，已提供完整 Controller 层代码

\- 核心功能：用户登录/注册、文件管理、分片上传、文件分享、VIP 支付、管理员后台、QQ 第三方登录

\- 开发目标：1:1 还原后端功能，严格匹配提供的前端效果图（登录页、文件列表页）



\## 技术栈要求

\- 框架：Vue3（Composition API + <script setup> 语法）

\- 构建工具：Vite

\- 语言：TypeScript

\- UI 组件库：Element Plus

\- 状态管理：Pinia

\- 网络请求：Axios（统一拦截、错误处理）

\- 路由：Vue Router 4

\- 样式：SCSS + BEM 命名规范

\- 其他：vue3-cookies（Token 存储）、file-saver（文件下载）、spark-md5（文件 MD5 计算）



\## 后端接口总览（核心接口）

\### 1. 用户相关（/userInfo）

\- GET /checkCode：获取图片验证码

\- POST /sendEmailCode：发送邮箱验证码

\- POST /register：用户注册

\- POST /login：用户登录（返回 Token）

\- GET /getAvatar：获取用户头像

\- POST /uploadAvatar：上传头像

\- GET /getUserInfo/{userId}：获取用户信息

\- GET /getUseSpace：获取用户空间信息

\- POST /changePwd：修改密码

\- GET /api/logout：退出登录



\### 2. 文件相关（/file）

\- POST /loadDataList：加载文件列表（分页）

\- POST /createFolder：新建文件夹

\- POST /rename：重命名文件

\- POST /move：移动文件

\- POST /delete：删除文件（回收站）

\- GET /preview/{fileId}：预览文件

\- GET /cover/{fileId}：获取文件封面

\- POST /createDownloadUrl：生成下载链接

\- POST /recycle/list：回收站列表

\- POST /recycle/recover：恢复文件

\- POST /recycle/delete：永久删除文件



\### 3. 分片上传（/file/chunk）

\- POST /init：初始化分片上传

\- POST /upload：上传分片

\- POST /merge：合并分片

\- GET /getUploadedChunks：查询已上传分片



\### 4. 文件分享（/file/share）

\- POST /create：创建分享链接

\- POST /cancel：取消分享

\- POST /verifyCode：校验分享码

\- GET /getFileInfo：获取分享文件信息

\- POST /list：获取用户分享列表

\- POST /save：保存分享文件到网盘



\### 5. VIP 相关（/vip、/vip-order、/aliPay）

\- POST /open：开通 VIP

\- POST /createPayOrder：创建支付订单

\- POST /aliPay/notify：支付宝回调

\- GET /vip-order/payReturn：支付成功页



\### 6. 管理员相关（/admin）

\- POST /getUserList：用户列表（分页）

\- POST /changeUserStatus：修改用户状态

\- POST /changeUserSpace：修改用户空间

\- POST /getAllFile：所有文件列表

\- POST /getSysConfig：获取系统配置

\- POST /saveSysConfig：保存系统配置



\### 7. QQ 登录（/qqlogin）

\- POST /：获取 QQ 授权链接

\- POST /callback：QQ 授权回调



\## 前端页面结构


## 开发规范
1.  所有接口必须统一封装在 `src/api/` 下，按模块拆分
2.  所有请求必须携带 Token（从 Pinia 或本地存储获取）
3.  统一错误处理：拦截器捕获后端错误，用 Element Plus 提示
4.  路由守卫：未登录用户只能访问登录页，已登录用户自动跳转首页
5.  组件必须使用 `<script setup lang="ts">` 语法
6.  样式必须使用 SCSS，遵循 BEM 命名规范
7.  分片上传必须实现断点续传、秒传功能
8.  适配响应式布局，兼容 PC 端
9.  严格匹配后端接口的请求方式、参数、返回值

## 核心功能实现要求
### 1. 登录页（严格匹配效果图）
- 布局：左侧蓝色渐变背景+办公插画，右侧白色登录卡片
- 卡片内容：
  - 标题：Easy云盘
  - 邮箱输入框（默认示例：laoluo_coder@qq.com）
  - 密码输入框（带眼睛图标）
  - 验证码输入框+右侧验证码图片（示例：Tc4Wd）
  - 记住我复选框（默认勾选）
  - 忘记密码？/ 没有账号？ 链接
  - 蓝色登录按钮
  - 底部：快捷登录+QQ图标
- 功能：验证码刷新、登录成功存Token跳首页、QQ快捷登录入口

### 2. 首页（文件列表页，严格匹配效果图）
- 布局：
  - 顶部：Easy云盘logo+右上角用户头像/用户名
  - 左侧侧边栏：
    - 首页/分享/回收站 三个一级菜单
    - 首页下展开：全部、视频、音频、图片、文档、其他 分类
    - 底部：空间使用进度条（0B/15.63GB，0%）
  - 右侧主内容区：
    - 顶部操作栏：蓝色【上传】、绿色【新建文件夹】、粉色【批量删除】、浅橙【批量移动】+搜索框+刷新按钮
    - 中间文件列表区：空状态显示「当前目录为空，上传你的第一个文件吧」+【上传文件】【新建目录】按钮
- 功能：文件列表加载、新建文件夹、上传、搜索、分类筛选、空间进度显示

### 3. 分片上传
- 初始化上传：计算文件 MD5，调用 `/file/chunk/init`
- 分片上传：按 chunkSize 拆分文件，并发上传分片
- 断点续传：查询已上传分片，跳过已上传部分
- 秒传：后端校验 MD5，已存在则直接完成上传
- 合并分片：调用 `/file/chunk/merge` 完成上传

### 4. 文件分享
- 创建分享：选择文件、设置过期时间（1天/7天/永久）、生成分享链接+提取码
- 分享页：输入提取码、查看分享文件、保存到自己网盘
- 分享列表：查看自己的分享、取消分享

### 5. VIP 功能
- VIP 开通页：选择月数、支付宝支付
- 支付成功后自动更新用户 VIP 状态、空间大小
- VIP 专属大文件上传（500MB 分片、不限速）

### 6. 管理员后台
- 用户管理：列表、禁用/启用、修改空间
- 文件管理：所有文件列表、删除文件
- 系统配置：修改系统参数

## Claude Code 交互指令规范
- 生成代码时必须提供完整可运行的单文件代码，无缺失依赖
- 每次修改必须说明修改位置、修改原因
- 严格遵循 Vue3 + TypeScript 最佳实践
- 所有接口必须对应后端提供的 Controller 代码
- 必须处理异常情况（网络错误、后端错误、参数错误）
- 提供详细的代码注释，说明核心逻辑
- 严格按照效果图的布局、配色、文案实现页面，1:1还原

