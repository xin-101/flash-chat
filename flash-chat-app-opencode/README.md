# Flash Chat App

基于 UniApp + Vue 3 的即时通讯前端应用。

## 项目特性

- **Vue 3 Composition API** - 使用最新的 Vue 3 语法
- **Pinia 状态管理** - 集中管理应用状态
- **组件化开发** - 可复用的组件设计
- **WebSocket 实时通信** - 支持消息实时推送
- **响应式设计** - 适配多种设备

## 项目结构

```
flash-chat-app-opencode/
├── api/                    # API 接口
│   ├── auth.js            # 认证相关接口
│   └── chat.js            # 聊天相关接口
├── components/             # 公共组件
│   ├── Avatar.vue         # 头像组件
│   ├── ChatBubble.vue     # 聊天气泡
│   ├── EmptyState.vue     # 空状态
│   └── NavBar.vue         # 导航栏
├── config/                 # 配置文件
│   └── env.js             # 环境配置
├── pages/                  # 页面
│   ├── login/             # 登录页
│   ├── index/             # 会话列表页
│   ├── chat/              # 聊天详情页
│   ├── user/              # 用户搜索页
│   └── my/                # 个人中心页
├── store/                  # 状态管理
│   └── index.js           # Pinia Store
├── utils/                  # 工具函数
│   ├── request.js         # HTTP 请求
│   ├── storage.js         # 本地存储
│   ├── time.js            # 时间格式化
│   └── websocket.js       # WebSocket
├── App.vue                # 根组件
├── main.js                # 入口文件
├── pages.json             # 页面配置
├── manifest.json          # 应用配置
└── uni.scss               # 全局样式
```

## 开发环境

- **Node.js**: 16+
- **HBuilderX**: 最新版
- **npm/yarn/pnpm**: 包管理器

## 快速开始

### 1. 安装依赖

```bash
npm install
```

### 2. 启动开发

#### H5 开发

```bash
npm run dev:h5
```

#### 微信小程序开发

```bash
npm run dev:mp-weixin
```

然后使用微信开发者工具打开 `dist/dev/mp-weixin` 目录。

### 3. 构建

#### 构建 H5

```bash
npm run build:h5
```

#### 构建微信小程序

```bash
npm run build:mp-weixin
```

## 配置说明

### 环境配置

编辑 `config/env.js` 文件：

```javascript
export const ENV_CONFIG = {
  development: {
    baseUrl: 'http://localhost:1000',
    wsUrl: 'ws://localhost:1000',
  },
  production: {
    baseUrl: 'https://api.flashchat.com',
    wsUrl: 'wss://api.flashchat.com',
  },
}
```

### 后端服务

确保以下服务已启动：

- 网关服务 (端口 1000)
- 认证服务 (端口 8890)
- 聊天服务 (端口 8892)

## 功能说明

### 已实现功能

- ✅ 用户登录/登出
- ✅ 短信验证码发送
- ✅ 会话列表展示
- ✅ 消息收发
- ✅ 用户搜索
- ✅ WebSocket 实时通信
- ✅ 消息未读数
- ✅ 乐观更新（消息发送）

### 待实现功能

- ⬜ 图片/文件发送
- ⬜ 语音消息
- ⬜ 消息已读回执
- ⬜ 消息撤回
- ⬜ 群聊功能
- ⬜ 用户资料编辑
- ⬜ 好友关系管理

## 组件说明

### Avatar 组件

头像组件，支持图片和文字头像。

```vue
<Avatar :src="user.face" :size="80" :name="user.nickname" :border-radius="8" />
```

### ChatBubble 组件

聊天气泡组件，支持自己和对方的消息样式。

```vue
<ChatBubble
  content="Hello"
  avatar=""
  sender-name="用户"
  :time="new Date()"
  :is-me="true"
/>
```

### EmptyState 组件

空状态组件，用于列表为空时的提示。

```vue
<EmptyState icon="💬" text="暂无会话" />
```

### NavBar 组件

导航栏组件，支持自定义标题和操作。

```vue
<NavBar title="标题" :show-back="true">
  <template #right>
    <view @tap="handleAction">操作</view>
  </template>
</NavBar>
```

## API 接口

### 认证接口

| 接口 | 方法 | 说明 |
|------|------|------|
| /auth/auth/sendSms | GET | 发送短信验证码 |
| /auth/auth/login | POST | 用户登录 |
| /auth/auth/logout | POST | 用户登出 |

### 聊天接口

| 接口 | 方法 | 说明 |
|------|------|------|
| /chat/conversation/list | GET | 获取会话列表 |
| /chat/conversation/create | POST | 创建会话 |
| /chat/message/list | GET | 获取消息列表 |
| /chat/message/send | POST | 发送消息 |
| /chat/user/search | GET | 搜索用户 |

### WebSocket

连接地址：`ws://localhost:1000/chat/ws?token={token}`

消息格式：

```json
// 发送消息
{
  "type": "message",
  "conversationId": "xxx",
  "content": "hello"
}

// 接收消息
{
  "type": "message",
  "conversationId": "xxx",
  "senderId": "xxx",
  "content": "hello"
}

// 心跳
{
  "type": "ping",
  "timestamp": 1234567890
}
```

## 开发规范

### 代码规范

- 使用 Vue 3 Composition API
- 组件命名采用大驼峰
- 页面文件使用小写下划线
- 样式使用 scoped

### 目录规范

- `api/` - 接口定义
- `components/` - 公共组件
- `pages/` - 页面组件
- `store/` - 状态管理
- `utils/` - 工具函数

### Git 提交规范

```
feat: 新功能
fix: 修复 bug
docs: 文档更新
style: 代码格式调整
refactor: 代码重构
test: 测试相关
chore: 构建/工具相关
```

## 常见问题

### 1. 启动后页面空白

检查 `config/env.js` 中的后端地址是否正确。

### 2. WebSocket 连接失败

- 确认后端网关服务已启动
- 检查 WebSocket 地址配置
- 查看浏览器控制台错误信息

### 3. 请求 401 错误

- 检查是否已登录
- 确认 Token 是否有效
- 清除本地存储重新登录

### 4. 小程序真机调试

- 确保手机和电脑在同一网络
- 检查微信开发者工具的调试设置
- 使用内网穿透工具（如 ngrok）

## 相关文档

- [UniApp 官方文档](https://uniapp.dcloud.net.cn/)
- [Vue 3 官方文档](https://vuejs.org/)
- [Pinia 官方文档](https://pinia.vuejs.org/)

## 许可证

本项目采用 Apache License 2.0 许可证。
