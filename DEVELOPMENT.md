# Flash Chat 开发文档

> 最后更新：2026年6月28日（Session 8：修复 Critical/High Bug + RabbitMQ 配置修正 + 全栈启动）

## 一、项目现状分析

### 1.1 模块完成状态

| 模块 | 状态 | 说明 |
|------|------|------|
| flash-chat-common | ✅ 完成 | Response、BizException、RedisUtil、IPUtil、@Phone 校验、API 日志切面 |
| flash-chat-model | ✅ 完成 | Users/Conversation/ConversationMember/Message/Moment/MomentLike/MomentComment 实体 + VO/BO |
| flash-chat-gateway | ✅ 已完成 | 路由转发、IP 限流（Nacos 已配）、CORS、Token 验证过滤器 |
| flash-chat-web-auth | ✅ 已完成 | 登录注册、短信验证码（实际发送）、二维码生成、闪聊号修改（30天限制）、Token 管理 |
| flash-chat-web-chat | ✅ 已完成 | 会话管理、消息收发、WebSocket（含心跳/分布式）、好友系统、朋友圈、群聊管理、文件上传 |
| flash-chat-web-file | ✅ 已完成 | LocalFileStrategy 本地上传、FileStrategy 工厂模式、静态资源映射、图片/头像上传 |
| flash-chat-app-opencode | ✅ 完成 | 前端 Vue3（无 UI 框架），5 个页面全部实现 |
| flash-chat-app-web | ✅ 完成 | 另一个前端实现：Vanilla JS SPA，单页 HTML + CSS + JS，功能与 uni-app 版本等价 |
| flash-chat-app | ⚠️ 遗留 | 原始 Uni-app 项目（Vue2/3），结构与 opencode 版本镜像，含构建产物 |

### 1.2 已修复的已知问题

| 问题 | 原因 | 解决方案 |
|------|------|----------|
| `Cannot read property 'wx' of undefined` | `@dcloudio/uni-mp-weixin` 的 `wx.js` 在严格模式下无法获取全局对象 | 修复 HBuilderX 内置的 `wx.js` 源码 |
| `TestController` Bean 冲突 | 各服务 `@ComponentScan("io.github.zh")` 扫描了所有模块 | 缩小扫描范围为各自模块包 |
| `UsersMapper` 找不到 | chat 模块扫描范围排除了 auth 模块的 Mapper | 添加 `io.github.zh.auth.mapper` 到 MapperScan |
| chat 模块数据库连接错误 | 连接了 `flash_chat_web_chat` 库，但 `users` 表在 `flash_chat` 库 | 改为连接 `flash_chat` 库 |
| Redis 消息转发 Bug | `RedisMessageListener.onMessage()` 参数错误：`sendToUser(body, body)` 把消息 JSON 当 userId 使用 | 已修正为 `sendToUser(member.getUserId(), body)`，参数正确 |
| 未读数未累加 | `MessageServiceImpl.sendMessage()` 设置 `unreadCount=1` 而非 `+1` | 已修正为 `.setSql("unread_count = unread_count + 1")`，正确累加 |
| 广播方法为空实现 | `ChatWebSocketHandler.broadcastToConversation()` | 已实现：查询成员列表 + `sendToUser` 推送给非发送方；`RedisMessageListener` 已复用 |
| N+1 查询 | `ConversationServiceImpl.getConversationList()` | 批量查询会话 + 批量查询用户，避免循环中逐条查 DB |
| ResponseEnum 方法异常 | `chat/exception/ResponseEnum.java` | 改为 `String errorCode/errorMessage` 字段，返回对应值而非空字符串 |
| env.js wsUrl 闲置 | `config/env.js` | 移除未使用的 `wsUrl` 字段和 `WS_URL` 导出 |
| 无 SQL 建表脚本 | 项目根目录 | 已创建 `schema.sql`，含 4 张表的完整建表语句 |
| SMS 未实际发送 | `SMSServiceImpl.sendSms()` | 已取消注释 `sendSmsUtil.sendSms()` 调用，添加 try-catch 保护 |
| 二维码占位符 | `UsersServiceImpl` | `flashChatNumImg` 已改为在线 QR 码 API：`https://api.qrserver.com/v1/create-qr-code/?size=200x200&data={flashChatNum}` |
| 网关 Token 验证绕过 | `TokenAuthFilter` | Session 8 修复：增加 `Authorization: Bearer {token}` 头对比，不再仅检查 userId 存在性 |
| WebSocket 无 token 仍转发 | `TokenAuthFilter` | Session 8 修复：WS 升级前验证 `flash:chat:token:{token}` 反向映射，无效拒绝 |
| WebSocket 死代码 ClassCastException | `ChatWebSocketHandler` | Session 8 修复：移除废弃的 "message" 处理分支 |
| GroupServiceImpl NPE | `GroupServiceImpl.invite()` | Session 8 修复：添加 null/empty 检查 |
| @RequestBody/@RequestParam 不匹配 | `GroupController` | Session 8 修复：3 个端点由 `@RequestParam` 改为 `@RequestBody` |
| RabbitMQ 端口错误 | Nacos `flash-chat-web-chat-dev.yaml` | Session 8 修复：`15672`→`5672` |
| Redis 密码不匹配 | Nacos `flash-chat-web-auth-dev.yaml` | Session 8 修复：移除 `password: 123456`（Redis Stack 无密码） |
| 网关无 Token 验证 | flash-chat-gateway | 已实现 `TokenAuthFilter`，校验 `flash:chat:user:{userId}` Redis key 是否存在 |
| chat 模块耦合 auth | `flash-chat-web-chat/pom.xml` | chat 模块新增 `ChatUsersMapper`，移除对 auth 模块的 compile 依赖 |
| WebSocket 非分布式 | `ChatWebSocketHandler` | 每个实例分配唯一 ID，WS 连接时存储路由 `flash:chat:ws:route:{userId}`→`instanceId`；各实例仅向本地 session 推送；RabbitMQ fanout 保障跨实例分发 |

### 1.3 现存已知 Bug

| # | 严重度 | 文件 | 描述 |
|---|--------|------|------|
| B7 | LOW | `ChatWebSocketHandler.java` | 路由键 `flash:chat:ws:route:{userId}` TTL=5min 未在活动时刷新，可能过期导致消息无法送达本实例 |
| B9 | LOW | `pages/moment/list.vue` | 首次加载列表失败无用户提示（仅 `console.error`） |
| Q1 | - | `ChatController.searchUser()` | 返回 `50000` 错误码，待确认是否为 MyBatis 查询问题 |
| Q2 | - | `TokenAuthFilter.java` | `USER_TOKEN_KEY` 硬编码与 `RedisKeyConstants.USER_TOKEN_KEY` 重复定义 |


> ✅ **Nacos 配置状态（2026-06-28）：** Nacos 服务已运行于 `127.0.0.1:8848`，已推送 4 个 `-dev.yaml` 配置：
> - `flash-chat-gateway-dev.yaml` — 端口 1000、Redis、Nacos Discovery、IP 限流
> - `flash-chat-web-auth-dev.yaml` — 端口 8890、Redis、MySQL 数据源、Druid
> - `flash-chat-web-chat-dev.yaml` — 端口 8892
> - `flash-chat-web-file-dev.yaml` — 端口 8891
>
> 本地 `application.yml` 中保留的服务名/路由/CORS/腾讯云短信/MyBatis-Plus 等配置与 Nacos 不冲突。`bootstrap.yml` 配置的 Nacos 地址与运行的 Nacos 服务一致。

---

## 二、开发环境

### 2.1 基础环境要求

| 依赖 | 版本 | 必需 | 说明 |
|------|------|------|------|
| JDK | 21 | 是 | Java 运行环境 |
| Maven | 3.8+ | 是 | 项目构建 |
| MySQL | 8.0 | 是 | 数据库（port: 3306, user: root, password: root） |
| Redis | 6.0+ | 是 | 缓存（port: 6379，密码 123456） |
| Nacos | 2.2+ | 是（dev） | 服务注册与配置中心（port: 8848，4 个 `-dev.yaml` 已推送） |
| RabbitMQ | 3.x+ | 是 | 消息队列（port: 5672，user: root，password: root） |
| Minio | latest | 否 | 文件存储（port: 9002 API / 9003 Console），可选，prod 默认 |
| HBuilderX | 最新版 | 是 | 前端 IDE |
| 微信开发者工具 | 最新版 | 是 | 小程序调试 |

### 2.2 服务端口规划

| 服务 | dev 端口 | prod 端口 | 说明 |
|------|----------|-----------|------|
| flash-chat-gateway | 1000 | 1001 | 统一入口 |
| flash-chat-web-auth | 8890 | 9990 | 用户认证 |
| flash-chat-web-chat | 8892 | 9992 | 消息处理 |
| flash-chat-web-file | 8891 | 9991 | 文件管理（空壳） |
| MySQL | 3306 | - | 数据库 |
| Redis | 6379 | - | 缓存（无密码）|
| RabbitMQ | 5672 / 15672 | - | AMQP 协议 / 管理界面（user: root, password: root） |
| Minio | 9002 / 9003 | - | S3 API / Console（user: minioadmin, password: minioadmin）|

> 注意：Redis 使用 `redis-stack` 容器（默认无密码），RabbitMQ 使用 `rabbitmq:management` 容器。本地 `application.yml` 有密码 `123456`，但 Nacos 远程配置已覆盖为无密码。

---

## 三、数据库设计

所有表均在 `flash_chat` 数据库中。

### 3.1 用户表 (users)

```sql
CREATE TABLE `users` (
  `id` varchar(32) NOT NULL COMMENT '用户ID',
  `flash_chat_num` varchar(32) DEFAULT NULL COMMENT '闪聊号',
  `flash_chat_num_img` varchar(255) DEFAULT NULL COMMENT '闪聊二维码',
  `mobile` varchar(20) DEFAULT NULL COMMENT '手机号',
  `nickname` varchar(50) DEFAULT NULL COMMENT '昵称',
  `real_name` varchar(50) DEFAULT NULL COMMENT '真实姓名',
  `sex` int DEFAULT '2' COMMENT '性别：0女，1男，2保密',
  `face` varchar(255) DEFAULT NULL COMMENT '头像URL',
  `email` varchar(100) DEFAULT NULL COMMENT '邮箱',
  `birthday` date DEFAULT NULL COMMENT '生日',
  `country` varchar(50) DEFAULT NULL COMMENT '国家',
  `province` varchar(50) DEFAULT NULL COMMENT '省份',
  `city` varchar(50) DEFAULT NULL COMMENT '城市',
  `district` varchar(50) DEFAULT NULL COMMENT '区县',
  `chat_bg` varchar(255) DEFAULT NULL COMMENT '聊天背景',
  `friend_circle_bg` varchar(255) DEFAULT NULL COMMENT '朋友圈背景',
  `signature` varchar(255) DEFAULT NULL COMMENT '个性签名',
  `created_time` datetime DEFAULT NULL COMMENT '创建时间',
  `updated_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_mobile` (`mobile`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';
```

### 3.2 会话表 (conversations)

```sql
CREATE TABLE `conversations` (
  `id` varchar(32) NOT NULL COMMENT '会话ID',
  `type` int DEFAULT '1' COMMENT '会话类型：1单聊，2群聊',
  `name` varchar(100) DEFAULT NULL COMMENT '会话名称（群聊用）',
  `owner_id` varchar(32) DEFAULT NULL COMMENT '群主ID（群聊用）',
  `last_message_id` varchar(32) DEFAULT NULL COMMENT '最后一条消息ID',
  `last_message_content` varchar(500) DEFAULT NULL COMMENT '最后一条消息内容',
  `last_message_time` datetime DEFAULT NULL COMMENT '最后一条消息时间',
  `created_time` datetime DEFAULT NULL COMMENT '创建时间',
  `updated_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='会话表';
```

### 3.3 会话成员表 (conversation_members)

```sql
CREATE TABLE `conversation_members` (
  `id` varchar(32) NOT NULL COMMENT '主键ID',
  `conversation_id` varchar(32) NOT NULL COMMENT '会话ID',
  `user_id` varchar(32) NOT NULL COMMENT '用户ID',
  `unread_count` int DEFAULT '0' COMMENT '未读消息数',
  `last_read_message_id` varchar(32) DEFAULT NULL COMMENT '最后已读消息ID',
  `is_top` int DEFAULT '0' COMMENT '是否置顶：0否，1是',
  `is_mute` int DEFAULT '0' COMMENT '是否免打扰：0否，1是',
  `created_time` datetime DEFAULT NULL COMMENT '加入时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_conversation_user` (`conversation_id`, `user_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='会话成员表';
```

### 3.4 消息表 (messages)

```sql
CREATE TABLE `messages` (
  `id` varchar(32) NOT NULL COMMENT '消息ID',
  `conversation_id` varchar(32) NOT NULL COMMENT '会话ID',
  `sender_id` varchar(32) NOT NULL COMMENT '发送者ID',
  `content` text COMMENT '消息内容',
  `type` int DEFAULT '1' COMMENT '消息类型：1文本，2图片，3语音，4视频，5文件',
  `status` int DEFAULT '1' COMMENT '消息状态：0撤回，1正常',
  `created_time` datetime DEFAULT NULL COMMENT '发送时间',
  PRIMARY KEY (`id`),
  KEY `idx_conversation_id` (`conversation_id`),
  KEY `idx_sender_id` (`sender_id`),
  KEY `idx_created_time` (`created_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='消息表';
```

---

## 四、API 接口文档

### 4.1 统一响应格式

```json
{
  "success": true,
  "message": null,
  "errorCode": null,
  "data": { ... }
}
```

### 4.2 认证服务（通过网关 `/auth` 前缀）

| 方法 | 路径 | 说明 | 参数 |
|------|------|------|------|
| GET | `/auth/auth/sendSms?phone={phone}` | 发送短信验证码 | Query: phone |
| POST | `/auth/auth/login` | 手机号登录（自动注册） | Body: `{ "phone", "code" }` |
| POST | `/auth/auth/logout` | 登出 | Header: `userId` |
| PUT | `/auth/auth/user/flashChatNum?flashChatNum={num}` | 修改闪聊号（30天一次） | Header: `userId` |

**登录响应 data (UserVO)：**
```json
{
  "id": "uuid-string",
  "token": "uuid-no-dash",
  "flashChatNum": "uuid",
  "flashChatNumImg": "https://api.qrserver.com/v1/create-qr-code/?size=200x200&data=...",
  "mobile": "13800138000",
  "nickname": "用户8000",
  "realName": null,
  "sex": 2,
  "face": "https://cdn.jsdelivr.net/gh/.../avatar/default.png",
  "email": null,
  "birthday": null,
  "country": null,
  "province": null,
  "city": null,
  "district": null,
  "chatBg": null,
  "friendCircleBg": null,
  "signature": null,
  "createdTime": "2024-01-01 00:00:00",
  "updatedTime": "2024-01-01 00:00:00"
}
```

**错误码：**
| 错误码 | 说明 |
|--------|------|
| auth-10000 | 系统错误 |
| auth-10001 | 参数错误 |
| auth-10002 | 用户不存在 |
| auth-10003 | 短信发送过于频繁 |
| auth-10004 | 验证码错误 |

### 4.3 聊天服务（通过网关 `/chat` 前缀）

所有接口需要 Header: `userId={用户ID}`

| 方法 | 路径 | 说明 | 参数 |
|------|------|------|------|
| GET | `/chat/conversation/list` | 获取会话列表 | Header: userId |
| POST | `/chat/conversation/create?targetUserId={id}` | 创建/获取会话 | Header: userId, Query: targetUserId |
| GET | `/chat/message/list?conversationId={id}&page=1&size=20` | 获取消息列表 | Header: userId |
| POST | `/chat/message/send` | 发送消息 | Header: userId, Body: `{ conversationId, content, type: 1 }` |
| GET | `/chat/user/search?keyword={keyword}` | 搜索用户 | Header: userId |
| GET | `/chat/user/info/{targetUserId}` | 获取用户信息 | Header: userId |
| POST | `/chat/friend/request?targetUserId={id}&remark={}` | 发送好友申请 | Header: userId |
| PUT | `/chat/friend/request/{requestId}/approve` | 同意好友申请 | Header: userId |
| PUT | `/chat/friend/request/{requestId}/reject` | 拒绝好友申请 | Header: userId |
| GET | `/chat/friend/requests` | 获取待处理的申请列表 | Header: userId |
| GET | `/chat/friend/list` | 获取好友列表 | Header: userId |
| DELETE | `/chat/friend/{friendId}` | 删除好友 | Header: userId |
| PUT | `/chat/friend/{friendId}/remark?remark={}` | 设置好友备注 | Header: userId |
| PUT | `/chat/friend/{friendId}/block` | 拉黑好友 | Header: userId |
| PUT | `/chat/friend/{friendId}/unblock` | 取消拉黑 | Header: userId |

**会话列表响应 data (ConversationVO[])：**
```json
[{
  "id": "conversation-uuid",
  "name": "对方昵称",
  "face": "https://...",
  "lastMessage": "最后一条消息内容",
  "lastMessageTime": "2024-01-01 12:00:00",
  "unreadCount": 3,
  "type": 1
}]
```

**消息列表响应 data (MessageVO[])：**
```json
[{
  "id": "message-uuid",
  "conversationId": "conversation-uuid",
  "senderId": "sender-uuid",
  "senderName": "发送者昵称",
  "senderFace": "https://...",
  "content": "消息内容",
  "type": 1,
  "createTime": "2024-01-01 12:00:00",
  "me": false
}]
```

**发送消息请求 Body：**
```json
{
  "conversationId": "conversation-uuid",
  "content": "消息内容",
  "type": 1
}
```

**错误码：**
| 错误码 | 说明 |
|--------|------|
| 50001 | 会话不存在 |
| 50002 | 无权访问该会话 |
| 50003 | 消息发送失败 |

### 4.4 WebSocket

```
ws://localhost:1000/chat/ws?token={token}
```

> **注意：** 前端实际通过 `BASE_URL.replace('http://', 'ws://') + '/chat/ws'` 拼接待 WS 地址（`App.vue:14`），即连接网关的 `/chat/ws` 路径，网关无 WS 路由支持，实际直连聊天服务的 `ws://localhost:8892/ws?token={token}` 仍需验证。`config/env.js` 中的 `wsUrl` 字段未被使用。

**消息格式：**

```jsonc
// 客户端 → 服务端：心跳
{"type": "ping"}

// 服务端 → 客户端：心跳响应
{"type": "pong"}

// 服务端 → 客户端：新消息
{
  "type": "message",
  "id": "msg-uuid",
  "conversationId": "conv-uuid",
  "senderId": "sender-uuid",
  "senderName": "用户昵称",
  "senderFace": "https://...",
  "content": "消息内容",
  "createTime": "2024-01-01 12:00:00"
}
```

### 4.5 网关路由

| 路径前缀 | 转发目标 | StripPrefix |
|----------|----------|-------------|
| `/auth/**` | flash-chat-web-auth | 1（去掉 `/auth`） |
| `/chat/**` | flash-chat-web-chat | 1（去掉 `/chat`） |
| `/file/**` | flash-chat-web-file | 1（去掉 `/file`） |

---

## 五、项目结构

### 5.1 后端

```
flash-chat/
├── pom.xml                          # 根 POM，管理依赖版本
├── flash-chat-common/               # 公共模块
│   └── src/main/java/io/github/zh/common/
│       ├── response/Response.java           # 统一响应
│       ├── exception/BizException.java      # 业务异常
│       ├── exception/BaseExceptionInterface.java
│       ├── utils/redis/RedisUtil.java       # Redis 工具
│       ├── utils/json/JsonUtil.java         # JSON 工具
│       ├── utils/ip/IPUtil.java             # IP 工具
│       ├── constants/BizConstants.java      # 常量
│       ├── constants/DateConstants.java     # 日期常量
│       ├── enums/SexEnum.java               # 性别枚举
│       ├── aspect/log/                      # API 日志切面
│       └── aspect/validator/                # @Phone 校验
├── flash-chat-model/                # 数据模型
│   └── src/main/java/io/github/zh/model/
│       ├── auth/pojo/Users.java
│       ├── auth/vo/UserVO.java
│       ├── auth/bo/LoginUserBO.java
│       ├── chat/pojo/Conversation.java
│       ├── chat/pojo/ConversationMember.java
│       ├── chat/pojo/Message.java
│       ├── chat/vo/ConversationVO.java
│       ├── chat/vo/MessageVO.java
│       └── chat/bo/SendMessageBO.java
├── flash-chat-gateway/              # 网关服务 (port: 1000)
│   └── src/main/java/io/github/zh/gateway/
│       ├── GatewayApplication.java
│       ├── filter/IPLimitFilter.java        # IP 限流过滤器
│       ├── filter/TokenAuthFilter.java      # Token 验证过滤器
│       ├── utils/ReturnErrorUtil.java
│       └── exception/ResponseEnum.java
├── flash-chat-web/
│   ├── flash-chat-web-auth/         # 认证服务 (port: 8890)
│   │   └── src/main/java/io/github/zh/auth/
│   │       ├── AuthApplication.java
│   │       ├── controller/AuthController.java
│   │       ├── controller/TestController.java
│   │       ├── service/UsersService.java
│   │       ├── service/impl/UsersServiceImpl.java
│   │       ├── service/impl/SMSServiceImpl.java
│   │       ├── mapper/UsersMapper.java      # 自定义 MyBatis（非 Plus）
│   │       ├── interceptor/SMSInterceptor.java
│   │       ├── config/WebInterceptorConfig.java
│   │       ├── config/sms/SmsConfigProperties.java
│   │       ├── utils/sms/SendSmsUtil.java
│   │       ├── constants/RedisKeyConstants.java
│   │       └── exception/
│   ├── flash-chat-web-chat/         # 聊天服务 (port: 8892)
│   │   └── src/main/java/io/github/zh/chat/
│   │       ├── ChatApplication.java
│   │       ├── controller/ChatController.java
│   │       ├── service/ConversationService.java
│   │       ├── service/MessageService.java
│   │       ├── service/impl/ConversationServiceImpl.java
│   │       ├── service/impl/MessageServiceImpl.java
│   │       ├── mapper/ConversationMapper.java
│   │       ├── mapper/ConversationMemberMapper.java
│   │       ├── mapper/MessageMapper.java
│   │       ├── websocket/ChatWebSocketHandler.java
│   │       ├── websocket/RabbitMQMessageListener.java
│   │       ├── config/WebSocketConfig.java
│   │       ├── config/RabbitMQConfig.java
│   │       └── exception/
│   └── flash-chat-web-file/         # 文件服务 (port: 8891, 空壳)
│       └── src/main/java/io/github/zh/file/
│           ├── FileApplication.java
│           └── controller/TestController.java
```

### 5.2 前端

#### flash-chat-app-opencode（主前端 - Uni-app Vue3 微信小程序）

```
flash-chat-app-opencode/
├── api/
│   ├── auth.js              # sendCode, login, logout
│   └── chat.js              # getConversationList, createConversation,
│                            # getMessageList, sendTextMessage, searchUser, getUserInfo
├── config/
│   └── env.js               # BASE_URL, WS_URL（wsUrl 字段未使用）
├── pages/
│   ├── login/index.vue      # 登录页（手机号+验证码，倒计时）
│   ├── index/index.vue      # 会话列表页（下拉刷新，未读徽章）
│   ├── chat/detail.vue      # 聊天详情页（消息收发，实时推送）
│   ├── user/search.vue      # 搜索用户页
│   └── my/index.vue         # 个人中心页（退出登录）
├── store/
│   └── index.js             # useUserStore, useChatStore（Vue3 reactive）
├── utils/
│   ├── request.js           # uni.request 封装（自动带 userId header）
│   ├── storage.js           # token/用户信息本地存储
│   ├── websocket.js         # WebSocket 管理（心跳+重连+监听器模式）
│   └── time.js              # formatChatTime, formatMessageTime, formatBriefTime
├── static/                  # 图标资源
├── App.vue                  # 入口（登录检查+WebSocket连接）
├── main.js                  # Vue3 入口
├── pages.json               # 路由 + TabBar
├── manifest.json            # 应用配置
└── uni.scss                 # 全局样式变量
```

#### flash-chat-app-web（辅助前端 - Vanilla JS SPA）

```
flash-chat-app-web/
└── www/
    ├── index.html           # 单页应用（登录/会话/聊天/搜索/个人中心）
    ├── css/
    │   └── style.css        # 全局样式
    └── js/
        ├── config.js        # 环境配置
        ├── utils.js         # Storage + Time 工具函数
        ├── api.js           # AuthAPI + ChatAPI（fetch）
        ├── websocket.js     # WebSocket 客户端
        └── app.js           # 主控制器（DOM 操作 + 路由）
```

#### flash-chat-app（遗留前端 - 原始 Uni-app 项目）

```
flash-chat-app/               # 结构与 opencode 版本镜像，含构建产物
└── unpackage/dist/dev/mp-weixin/  # 微信小程序构建输出
```

---

## 六、启动指南

### 6.1 基础服务

```bash
# 1. 启动 MySQL（确保 3306 端口可用）
# 2. 启动 Redis（确保 6379 端口可用，Redis Stack 无密码）
# 3. 创建数据库和表
CREATE DATABASE IF NOT EXISTS flash_chat CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
-- 执行项目根目录的 schema.sql（包含 4 张表的完整建表语句）
# 4. 启动 Nacos（port: 8848），确保 4 个 `-dev.yaml` 配置已推送至 Nacos 配置中心
# 5. 启动 RabbitMQ（port: 5672 AMQP / 15672 管理界面）
docker run -d --name rabbitmq -p 5672:5672 -p 15672:15672 rabbitmq:management
# 6.（可选）启动 Minio（prod 模式需要）
docker run -d --name minio -p 9002:9000 -p 9003:9001 \
  -e MINIO_ROOT_USER=minioadmin -e MINIO_ROOT_PASSWORD=minioadmin \
  quay.io/minio/minio server /data --console-address ":9001"
```

### 6.2 后端启动顺序

```
0. Nacos / MySQL / Redis / RabbitMQ → 确保基础服务全部运行
1. flash-chat-gateway  → GatewayApplication.main()     (port: 1000)
2. flash-chat-web-auth → AuthApplication.main()         (port: 8890)
3. flash-chat-web-chat → ChatApplication.main()         (port: 8892)
4. flash-chat-web-file → FileApplication.main()         (port: 8891)
```

在 IDEA 中分别运行各模块的 Application 类，或在项目根目录 `mvn install -DskipTests` 后各模块目录下执行 `mvn spring-boot:run`。

**注意：** 聊天服务依赖认证服务的 `UsersMapper`（跨模块访问 `users` 表），需确保认证服务的数据库配置正确。先执行 `mvn install -DskipTests` 安装依赖 jar 到本地仓库，再运行各模块。

### 6.3 前端启动

```
1. 打开 HBuilderX
2. 导入 flash-chat-app-opencode 项目
3. 菜单 → 运行 → 运行到小程序模拟器 → 微信开发者工具
4. 在微信开发者工具中打开 unpackage/dist/dev/mp-weixin 目录
```

**已知前端注意事项：**
- `manifest.json` 中 `mp-weixin.appid` 为空，使用微信开发者工具的游客模式
- 如遇 `wx.js` 报错，需修复 HBuilderX 内置源码（路径见第九节）

### 6.4 配置说明

**前端环境配置** (`flash-chat-app-opencode/config/env.js`)：
```js
const ENV = 'development'  // 切换环境：development / production
const config = {
  development: { baseUrl: 'http://localhost:1000', wsUrl: 'ws://localhost:8892' },
  production:  { baseUrl: 'https://api.flashchat.com', wsUrl: 'wss://api.flashchat.com' },
}
```
> ⚠️ `env.js` 中 `wsUrl` 字段未被实际使用（已清理）。`App.vue` 通过 `BASE_URL.replace('http://', 'ws://') + '/chat/ws'` 拼接待 WS 地址，实际值为 `ws://localhost:1000/chat/ws`（通过网关转发）。若网关无 WS 路由支持，需改为直连 `ws://localhost:8892`。

**短信服务配置** (`flash-chat-web-auth/src/main/resources/application.yml`)：
```yaml
tencent:
  cloud:
    secretId: ${TENCENT_CLOUD_SECRET_ID}
    secretKey: ${TENCENT_CLOUD_SECRET_KEY}
    signName:        # 需填写
    smsSdkAppId: ${TENCENT_CLOUD_SMS_SDK_APP_ID}
    templateId:      # 需填写
```
> 短信发送已启用（取消注释），但需腾讯云配置（signName/templateId）方可实际送达。

---

## 七、Redis Key 规范

| Key | TTL | 说明 |
|-----|-----|------|
| `auth:sms:code:{phone}` | 5 分钟 | 短信验证码 |
| `auth:ip:{IP}` | 5 分钟 | 短信发送 IP 限流 |
| `flash:chat:user:{userId}` | 7 天 | userId → token 映射（TokenAuthFilter 对比使用） |
| `flash:chat:token:{token}` | 7 天 | token → userId 反向映射（WebSocket 验证使用） |
| `flash:chat:ws:{userId}` | 连接期间 | WebSocket 会话 ID 映射 |
| `flash:chat:ws:route:{userId}` | 5 分钟 | WebSocket 实例路由：userId → instanceId（RabbitMQ 跨实例分发用） |
| `flash:chat:offline:{userId}` | - | 离线消息队列（List 结构） |
| `flash:chat:gateway:ip:write:limit:{IP}` | request_interval 秒 | 网关 IP 限流计数 |
| `flash:chat:gateway:ip:black:limit{IP}` | black_ip_time 秒 | 网关 IP 黑名单 |
| `flash:chat:num:update:{userId}` | 30 天 | 闪聊号修改时间记录 |

> 网关 IP 限流参数通过 Nacos `flash-chat-gateway-dev.yaml` 配置（request-count: 1000, request-interval: 60, black-ip-time: 60）。

### RabbitMQ 资源

| 资源 | 类型 | 说明 |
|------|------|------|
| `flash.chat.fanout` | fanout exchange | 消息广播，由 `RabbitMQConfig` 自动声明 |
| `flash.chat.queue.{uuid}` | 匿名队列 | 每个实例独立队列，绑定到 fanout exchange，自动删除 |

---

## 八、开发规范

### 8.1 包结构

```
io.github.zh.{module}
├── controller/        # 控制器层
├── service/           # 服务接口
│   └── impl/          # 服务实现
├── mapper/            # MyBatis Mapper 接口
├── config/            # 配置类
├── exception/         # 异常处理
├── constants/         # 常量
├── interceptor/       # 拦截器
└── websocket/         # WebSocket 处理器
```

### 8.2 各模块组件扫描范围

| 模块 | @ComponentScan | @MapperScan |
|------|----------------|-------------|
| gateway | `io.github.zh.gateway`, `io.github.zh.common` | - |
| auth | `io.github.zh.auth`, `io.github.zh.common` | MyBatis XML 映射 |
| chat | `io.github.zh.chat`, `io.github.zh.common` | `io.github.zh.chat.mapper`, `io.github.zh.auth.mapper` |

### 8.3 ID 生成规则

```java
String id = cn.hutool.core.lang.UUID.fastUUID().toString(true); // 无横线 UUID
```

### 8.4 Redis Key 常量

```java
// io.github.zh.auth.constants.RedisKeyConstants
SMS_CODE_KEY = "auth:sms:code:"
SMS_IP_KEY = "auth:ip:"
USER_TOKEN_KEY = "flash:chat:user:"
USER_TOKEN_REVERSE_KEY = "flash:chat:token:"
```

---

## 九、常见问题与解决方案

### Q1: `Cannot read property 'wx' of undefined`

**原因：** `@dcloudio/uni-mp-weixin` 的 `wx.js` 在 webpack 严格模式下无法获取全局对象。

**解决：** 修复 HBuilderX 内置源码，路径：
```
D:\HBuilderX.4.76.2025082103\HBuilderX\plugins\uniapp-cli\node_modules\@dcloudio\uni-mp-weixin\dist\wx.js
```
将第 18-20 行改为：
```js
const target = typeof globalThis !== 'undefined' ? globalThis
  : (typeof self !== 'undefined' ? self
  : (typeof window !== 'undefined' ? window
  : (typeof global !== 'undefined' ? global
  : Function('return this')())));
```

### Q2: `TestController` Bean 冲突

**原因：** 各服务 `@ComponentScan(basePackages = "io.github.zh")` 扫描了所有模块的 `TestController`。

**解决：** 缩小扫描范围：
```java
@ComponentScan(basePackages = {"io.github.zh.chat", "io.github.zh.common"})
```

### Q3: `UsersMapper` 找不到

**原因：** chat 模块的 `@MapperScan` 没有包含 auth 模块的 mapper 包。

**解决：**
```java
@MapperScan({"io.github.zh.chat.mapper", "io.github.zh.auth.mapper"})
```

### Q4: Nacos 启动失败

**原因：** `bootstrap.yml` 中配置了 `spring.config.import: nacos:localhost:8848` 但 Nacos 未运行。

**解决：** 注释掉所有 `bootstrap.yml` 和 `application-dev.yml` 中的 Nacos 配置导入。

### Q5: 前端编译后 `wx.js` 错误仍然存在

**原因：** 修改了 HBuilderX 源码但未重新编译。

**解决：**
1. 关闭微信开发者工具
2. 在 HBuilderX 中删除 `unpackage/dist` 目录
3. 重新运行到小程序模拟器

### Q6: 短信验证码发送失败

**原因：** `SMSServiceImpl.sendSms()` 已取消注释但腾讯云 SMS 配置（signName、templateId）为空。

**解决：** 填写腾讯云 SMS 配置（secretId、secretKey、signName、smsSdkAppId、templateId）。

---

## 十、开发计划

### Phase 1: 核心聊天功能 ✅ 已完成
- [x] 用户登录/注册（自动注册）
- [x] 短信验证码（仅日志输出）
- [x] 会话列表
- [x] 创建会话
- [x] 消息列表（分页）
- [x] 发送消息
- [x] 用户搜索
- [x] WebSocket 实时通信（心跳 + 自动重连）
- [x] IP 限流

### Phase 2: 前端 ✅ 已完成
- [x] 登录页（手机号 + 验证码 + 倒计时）
- [x] 会话列表页（下拉刷新 + 未读徽章）
- [x] 聊天详情页（消息收发 + 实时推送 + 加载更多）
- [x] 搜索用户页
- [x] 个人中心页（退出登录）
- [x] WebSocket 监听器模式

### Phase 3: 功能修复与增强（优先级排序）

- [x] Nacos 配置中心接入（4 个 `-dev.yaml` 已推送至 Nacos）
- [x] 修复 Redis 消息转发 Bug（`RedisMessageListener.onMessage` 参数错误 → 已修正）
- [x] 修复未读数累加逻辑（已使用 `unread_count = unread_count + 1`）
- [x] 实现网关 Token 验证过滤器（`TokenAuthFilter`，校验 `flash:chat:user:{userId}` Redis key）
- [x] 实现实际短信发送（取消注释 `sendSmsUtil.sendSms()`，添加 try-catch）
- [x] 修复 `ResponseEnum` 方法返回值（chat 模块）
- [x] 实现 `broadcastToConversation()` 方法体 + 简化 `RedisMessageListener`
- [x] 修复 N+1 查询（`getConversationList` 批量查询 + `UsersMapper.selectByIds`）
- [x] 生成真实二维码（在线 QR 码 API）
- [x] 清理 `env.js` 中未使用的 `wsUrl` 字段
- [x] 添加 SQL 建表脚本到项目中（`schema.sql`）
- [x] 解耦 chat 模块对 auth 模块的编译依赖（`ChatUsersMapper` + 移除 pom 依赖）
- [x] 修复 WebSocket 分布式问题（实例路由键 + Redis Pub/Sub 跨实例推送）

### Phase 4: 文件服务
- [ ] 文件服务本地存储（`flash-chat-web-file`）
- [ ] 静态资源映射（`addResourceHandlers`）
- [ ] 策略工厂模式（本地 / Minio / 云存储）
- [ ] Minio 集成（依赖 + 配置 + 客户端）
- [ ] 图片上传 Controller + 头像上传
- [ ] 文件/图片消息类型支持

### Phase 5: 好友系统
- [ ] 用户关系表（`user_friends`）+ 实体 + Mapper
- [ ] 好友申请 / 审批 / 拒绝（含通知）
- [ ] 好友列表 + 删除好友
- [ ] 好友备注
- [ ] 黑名单（用户级）
- [ ] 通讯录前端页面

### Phase 6: 群聊功能
- [ ] 创建群聊 + 邀请成员
- [ ] 群聊消息广播（打通 `broadcastToConversation` + Redis Pub/Sub）
- [ ] 群管理（群主转让、成员管理）
- [ ] 群聊前端 UI

### Phase 7: 高级功能
- [ ] 消息已读回执
- [ ] 消息撤回
- [ ] 朋友圈（发布 / 点赞 / 评论 / 回复 / 删除）
- [ ] 闪聊号修改（含一个月限制）
- [ ] 表情包系统
- [ ] 离线消息推送

### Phase 8: 架构升级
- [ ] Netty 替代 Spring WebSocket
- [x] RabbitMQ 替代 Redis Pub/Sub
- [ ] 消息加密传输
- [x] 解耦 chat 模块对 auth 模块的编译依赖

---

**最后更新：** 2026年6月28日（Session 8）
