# Flash Chat

基于微服务架构的即时通讯系统，包含前后端完整解决方案。

**当前状态：** 核心功能已完成，支持单聊、群聊、朋友圈、好友管理、文件存储，可进行全功能体验

## 技术栈

### 后端
| 技术 | 版本 | 说明 |
|------|------|------|
| Spring Boot | 3.2.4 | 核心框架 |
| Spring Cloud | 2023.0.1 | 微服务框架 |
| Spring Cloud Alibaba | 2023.0.1.0 | 阿里巴巴微服务组件 |
| Nacos | 2.2+ | 配置中心 + 服务发现 |
| MySQL | 8.0 | 数据库 |
| Redis | 6.0+ | 缓存 / 会话路由 / 离线消息 |
| RabbitMQ | — | 消息队列（WebSocket 广播替代 Redis Pub/Sub） |
| MyBatis-Plus | 3.5.5 | ORM 框架 |
| Druid | 1.2.16 | 数据库连接池 |
| Minio | — | 分布式文件存储（可选，与本地存储策略切换） |
| JDK | 21 | Java 运行环境 |

### 前端
| 技术 | 说明 |
|------|------|
| UniApp + Vue 3 | 跨平台框架（无 UI 框架依赖） |
| HBuilderX | 开发 IDE |
| 微信开发者工具 | 小程序调试 |

## 项目结构

```
flash-chat/
├── flash-chat-common/          # 公共模块（Response、异常、Redis 工具、IP 工具）
├── flash-chat-model/           # 数据模型（实体、VO、BO）
├── flash-chat-gateway/         # 网关服务 (port: 1000)
├── flash-chat-web/
│   ├── flash-chat-web-auth/    # 认证服务 (port: 8890)
│   ├── flash-chat-web-chat/    # 聊天服务 (port: 8892)
│   └── flash-chat-web-file/    # 文件服务 (port: 8891)
└── flash-chat-app-opencode/    # 前端应用（UniApp + Vue3）
```

## 已实现功能

- ✅ 手机号验证码登录（自动注册）
- ✅ 会话列表（下拉刷新 + 未读徽章）
- ✅ 创建会话 / 获取已有会话
- ✅ 消息发送与接收（分页加载）
- ✅ WebSocket 实时通信（心跳 + 自动重连 + 离线消息推送）
- ✅ RabbitMQ 消息广播（分布式多实例支持）
- ✅ 用户搜索（全量搜索 + 好友内搜索）
- ✅ 个人中心（退出登录 + 资料编辑）
- ✅ 群聊管理（创建 / 邀请 / 踢人 / 转让群主 / 改群名）
- ✅ 好友管理（添加 / 审批 / 备注 / 黑名单 / 删除）
- ✅ 好友申请分页加载
- ✅ 朋友圈（发布 / 点赞 / 评论 / 删除）
- ✅ 文件上传（本地 + Minio 双策略，策略工厂模式）
- ✅ 文件代理端点（Minio 文件流式返回）
- ✅ IP 访问频率限制
- ✅ Token 鉴权（Bearer + Redis 双向校验）
- ✅ CORS 跨域处理
- ✅ 统一响应格式
- ✅ Nacos 配置中心 + 服务注册发现

## 快速开始

### 1. 环境准备

```bash
# 必需
JDK 21
Maven 3.8+
MySQL 8.0 (port: 3306)
Redis 6.0+ (port: 6379, password: 123456)
RabbitMQ (port: 5672)

# 可选
Nacos 2.2+ (配置中心 + 服务发现)
Minio (文件存储，默认使用本地存储)
```

### 2. 初始化数据库

```sql
CREATE DATABASE flash_chat CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 然后执行根目录 schema.sql（共 9 张表）
-- users, conversations, conversation_members, messages,
-- friend_request, user_friend, moment, moment_like, moment_comment
```

### 3. 启动后端

按顺序启动：

```
1. flash-chat-gateway   → GatewayApplication.main()     (port: 1000)
2. flash-chat-web-auth  → AuthApplication.main()         (port: 8890)
3. flash-chat-web-chat  → ChatApplication.main()         (port: 8892)
4. flash-chat-web-file  → FileApplication.main()         (port: 8891)
```

### 4. 启动前端

```
1. HBuilderX → 导入 flash-chat-app-opencode 项目
2. 运行 → 运行到小程序模拟器 → 微信开发者工具
3. 微信开发者工具 → 打开 unpackage/dist/dev/mp-weixin 目录
```

## API 接口

所有接口需通过网关 `localhost:1000` 访问。

### 认证 `/auth/**`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/auth/auth/sendSms?phone={phone}` | 发送验证码 |
| POST | `/auth/auth/login` | 登录 |
| PUT | `/auth/auth/user/info` | 更新用户资料 |
| PUT | `/auth/auth/user/flashChatNum` | 修改闪聊号 |
| POST | `/auth/auth/logout` | 登出 |

### 聊天 `/chat/**`

需要 Header: `Authorization: Bearer {token}`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/chat/conversation/list` | 会话列表 |
| POST | `/chat/conversation/create?targetUserId={id}` | 创建单聊会话 |
| PUT | `/chat/conversation/read/{conversationId}` | 标记已读 |
| GET | `/chat/message/list?conversationId={id}&page=1&size=20` | 消息列表 |
| POST | `/chat/message/send` | 发送消息 |
| GET | `/chat/user/search?keyword={keyword}` | 搜索用户 |
| GET | `/chat/user/info/{targetUserId}` | 获取用户信息 |

### 好友 `/chat/**`

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/chat/friend/request` | 发送好友申请 |
| PUT | `/chat/friend/request/{id}/approve` | 审批通过 |
| PUT | `/chat/friend/request/{id}/reject` | 审批拒绝 |
| GET | `/chat/friend/requests` | 好友申请列表 |
| GET | `/chat/friend/requests/page?page=1&size=10` | 好友申请分页 |
| GET | `/chat/friend/list` | 好友列表 |
| DELETE | `/chat/friend/{friendId}` | 删除好友 |
| PUT | `/chat/friend/{friendId}/remark` | 设置备注 |
| PUT | `/chat/friend/{friendId}/block` | 拉黑 |
| PUT | `/chat/friend/{friendId}/unblock` | 取消拉黑 |

### 群聊 `/chat/**`

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/chat/group/create` | 创建群聊 |
| POST | `/chat/group/invite` | 邀请成员 |
| DELETE | `/chat/group/member` | 移除成员 |
| PUT | `/chat/group/transfer` | 转让群主 |
| PUT | `/chat/group/name` | 修改群名 |
| GET | `/chat/group/members?conversationId={id}` | 成员列表 |
| GET | `/chat/group/members/detail?conversationId={id}` | 成员详情 |
| GET | `/chat/group/info/{conversationId}` | 群信息 |

### 朋友圈 `/chat/**`

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/chat/moment/create` | 发布朋友圈 |
| DELETE | `/chat/moment/{momentId}` | 删除朋友圈 |
| GET | `/chat/moment/list?page=1&size=10` | 朋友圈列表 |
| POST | `/chat/moment/{momentId}/like` | 点赞 |
| DELETE | `/chat/moment/{momentId}/like` | 取消点赞 |
| POST | `/chat/moment/{momentId}/comment` | 评论 |
| DELETE | `/chat/moment/comment/{commentId}` | 删除评论 |

### 文件 `/file/**`

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/file/upload` | 通用文件上传 |
| POST | `/file/upload/image` | 图片上传 |
| POST | `/file/upload/avatar` | 头像上传 |
| GET | `/file/minio-files/{dir}/{filename}` | Minio 文件代理 |

### WebSocket

```
ws://localhost:1000/chat/ws?token={token}
```

## 数据库

| 表名 | 说明 |
|------|------|
| users | 用户表 |
| conversations | 会话表 |
| conversation_members | 会话成员表 |
| messages | 消息表 |
| friend_request | 好友申请表 |
| user_friend | 好友关系表 |
| moment | 朋友圈表 |
| moment_like | 朋友圈点赞表 |
| moment_comment | 朋友圈评论表 |

所有表均在 `flash_chat` 数据库中，建表脚本见根目录 `schema.sql`，详细设计见 `DEVELOPMENT.md`。

## 已知问题

| 问题 | 说明 |
|------|------|
| 短信未实际发送 | 腾讯云 SDK 已集成但未启用，验证码仅打印到日志 |
| WebSocket 非分布式 | 使用内存存储会话，多实例部署时需依赖 RabbitMQ fanout 广播 + Redis 路由键实现跨实例送达 |

## 文档

- [开发手册](DEVELOPMENT.md) — 完整的开发规范、API 文档、数据库设计、启动指南、已知问题
- [功能审计](FEATURE_AUDIT.md) — 全部 58 项功能审计详情

## 许可证

Apache License 2.0

## 联系方式

- 邮箱：xinfukun@outlook.com

---

**注意：此项目仍在开发中，功能可能不完整，不建议用于生产环境。**
