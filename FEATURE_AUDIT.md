# Flash Chat 功能审计报告

> 审计时间：2026-06-28  
> 最后更新：2026-06-28（Session 8：修复 Critical/High Bug，启动全栈服务）  
> 项目版本：基于代码库通读结果

---

## 总览

| 状态 | 数量 | 占比 |
|------|------|------|
| ✅ 已实现 | 56 | 96.6% |
| 🔶 部分实现 | 1 | 1.7% |
| ❌ 未实现 | 1 | 1.7% |
| ⚠️ 有 Bug（已修复） | 7 | - |
| **合计** | **58** | **100%** |

---

## 一、基础设施层

### 1.1 网关服务（flash-chat-gateway）

| # | 功能 | 状态 | 详情 |
|---|------|------|------|
| 1 | 透传用户ID并封装 | ✅ 已实现 | Gateway 透传 `userId` Header，Controller 通过 `@RequestHeader("userId")` 读取 |
| 2 | IP 限流 | ✅ 已实现 | `IPLimitFilter` 代码完整，`application.yml` 配置默认值（10次/30s/黑名单60s），`application-prod.yml` 已取消注释 |
| 3 | 网关Token校验 | ✅ 已实现 | `TokenAuthFilter` 校验 `Authorization: Bearer {token}` 与 `flash:chat:user:{userId}` 存储值是否一致，不一致拒绝请求 |
| 4 | WebSocket 路由支持 | ✅ 已实现 | 网关 `/chat/**` 路由转发 WebSocket 升级请求，StripPrefix=1 后到达 `/ws`；先验证 `flash:chat:token:{token}` 反向映射，无效则拒绝；成功则注入 userId header |

### 1.2 工具类

| # | 功能 | 状态 | 详情 |
|---|------|------|------|
| 1 | RedisUtil | ✅ 已实现 | `flash-chat-common` 中，封装 String/Hash/List/Set 操作 |
| 2 | IPUtil | ✅ 已实现 | 获取客户端真实 IP（支持 Servlet 和 Reactive） |
| 3 | JsonUtil | ✅ 已实现 | Jackson 序列化/反序列化，含深度/最大元素截断 |
| 4 | SendSmsUtil | ✅ 已实现 | 腾讯云 SMS SDK 完整封装，`SMSServiceImpl` 中已取消注释 |
| 5 | Minio工具类 | ✅ 已实现 | `MinioFileStrategy` 实现 `FileStrategy`，支持上传/删除/预签名URL（7天），自动创建 bucket |

### 1.3 Nacos 配置

| # | 功能 | 状态 | 详情 |
|---|------|------|------|
| 1 | boostrap.yml 连接 Nacos | ✅ 已实现 | Nacos 运行于 `127.0.0.1:8848`，4 个模块 `bootstrap.yml` 配置正确 |
| 2 | prod 配置注释 | ✅ 已实现 | 所有 `application-prod.yml` 中 Nacos 导入已注释 |
| 3 | 统一配置中心 | ✅ 已实现 | 4 个 `-dev.yaml` 已推送：gateway（端口/Redis/Discovery/IP限流）、auth（端口/Redis/数据源/Druid）、chat（端口）、file（端口） |

---

## 二、用户模块

### 2.1 用户表（users）

| 字段 | 说明 | 状态 |
|------|------|------|
| id | 用户ID | ✅ |
| flash_chat_num | 闪聊号 | ✅ 登录时自动生成 UUID |
| flash_chat_num_img | 闪聊二维码 | ✅ 已实现，在线 QR 码 API 生成 |
| mobile | 手机号 | ✅ |
| nickname | 昵称 | ✅ 自动生成"用户+手机号后4位" |
| face | 头像URL | ✅ 默认头像 CDN |
| sex | 性别 | ✅ |
| signature | 个性签名 | ✅ 字段存在，功能未实现 |
| friend_circle_bg | 朋友圈背景图 | ✅ 字段存在，功能未实现 |

### 2.2 用户功能

| # | 功能 | 状态 | 详情 |
|---|------|------|------|
| 1 | 手机号登录（自动注册） | ✅ 已实现 | `AuthController.login()` 验证码校验通过后自动注册新用户 |
| 2 | 闪聊号一个月修改一次 | ✅ 已实现 | `PUT /auth/auth/user/flashChatNum`，Redis 记录修改时间，30天内禁止再次修改；含前端弹窗 |
| 3 | 用户信息修改 | ✅ 已实现 | `PUT /auth/user/info` 支持修改昵称/性别/头像/签名/朋友圈背景/聊天背景/生日/邮箱/地区等，`UpdateUserBO` 含全字段 |
| 4 | 用户搜索 | ✅ 已实现 | `ChatController.searchUser()` 支持全量搜索；`searchFriends()` 通过 JOIN 好友关系表仅搜索好友（闪聊号+昵称+手机号 LIKE） |

---

## 三、文件服务（flash-chat-web-file）

| # | 功能 | 状态 | 详情 |
|---|------|------|------|
| 1 | 文件本地上传 | ✅ 已实现 | `LocalFileStrategy` + `FileStrategyFactory`，支持 `/file/upload` |
| 2 | 本地静态资源映射 | ✅ 已实现 | `WebConfig` 配置 `addResourceHandlers("/static/**")` 映射到 `uploadDir` |
| 3 | 策略工厂模式 | ✅ 已实现 | `FileStrategy` 接口 + `LocalFileStrategy` 默认实现 + `FileStrategyFactory` 根据配置选择策略 |
| 4 | Minio 依赖与配置 | ✅ 已实现 | pom.xml 添加 `io.minio:minio:8.5.17`；`MinioProperties` 配置类（endpoint/accessKey/secretKey/bucket）；`application.yml` 含默认配置，`application-prod.yml` 切到 minio 策略 |
| 5 | Minio 工具类 | ✅ 已实现 | `MinioFileStrategy` 全功能实现（与 `MinioProperties` 同模块，此条目已合并到 3.5） |
| 6 | 图片上传 | ✅ 已实现 | `FileController` 支持 `/upload`、`/upload/image`、`/upload/avatar` |
| 7 | Web 依赖 | ✅ 已实现 | 继承父 POM 的 `spring-boot-starter-web` |

---

## 四、好友系统

| # | 功能 | 状态 | 详情 |
|---|------|------|------|
| 1 | 用户关系表 | ✅ 已实现 | `user_friend` 表含 userId/friendId/remark/isBlock |
| 2 | 好友申请 | ✅ 已实现 | `friend_request` 表 + `sendRequest`/`approveRequest`/`rejectRequest` 接口 |
| 3 | 分页加载好友申请 | ✅ 已实现 | `GET /friend/requests/page` 分页查询含 total/records 分页信息，前端 `onReachBottom` 触发 loadMore 加载下一页 |
| 4 | 建立好友关系 | ✅ 已实现 | 审批后双向插入 `user_friend` 记录 |
| 5 | 通讯录页面 | ✅ 已实现 | `pages/friend/index.vue` 含好友列表、申请处理、操作菜单 |
| 6 | 好友备注 | ✅ 已实现 | `setRemark` 接口 + 前端可编辑弹窗 |
| 7 | 黑名单（用户级） | ✅ 已实现 | `blockFriend`/`unblockFriend` 接口，`isBlock` 字段 |
| 8 | 删除好友 | ✅ 已实现 | `deleteFriend` 接口 + 双向删除 `user_friend` 记录 |

---

## 五、朋友圈（Moments）

| # | 功能 | 状态 | 详情 |
|---|------|------|------|
| 1 | 朋友圈功能 | ✅ 已实现 | `Moment`/`MomentLike`/`MomentComment` 实体 + Mapper + `MomentServiceImpl` + `MomentController` |
| 2 | 朋友圈发布内容 | ✅ 已实现 | `POST /moment/create` 支持文字+多图上传 |
| 3 | 发布后回显及数据加载 | ✅ 已实现 | `GET /moment/list` 分页查询，含用户信息/点赞/评论 |
| 4 | 点赞、删除点赞、评论、删除评论 | ✅ 已实现 | `POST /moment/{id}/like` / `DELETE` / `POST /moment/{id}/comment` / `DELETE /moment/comment/{id}` |
| 5 | 回复评论 | ✅ 已实现 | `replyUserId` 参数支持回复指定用户评论 |
| 6 | 删除朋友圈内容 | ✅ 已实现 | `DELETE /moment/{id}`（软删除） |

---

## 六、聊天服务（flash-chat-web-chat）

### 6.1 数据模型

| 实体 | 状态 | 说明 |
|------|------|------|
| Message | ✅ | 消息实体，含 id/conversationId/senderId/content/type/status |
| Conversation | ✅ | 会话实体，含 type(1单聊/2群聊)/name/ownerId |
| ConversationMember | ✅ | 会话成员，含 unreadCount/isTop/isMute |
| MessageVO | ✅ | 消息视图对象 |
| ConversationVO | ✅ | 会话视图对象 |
| SendMessageBO | ✅ | 发送消息请求体 |

### 6.2 聊天功能

| # | 功能 | 状态 | 详情 |
|---|------|------|------|
| 1 | 创建会话（单聊） | ✅ 已实现 | `ChatController.createConversation()` 检查存在性，事务创建 conversation + 2 条 member 记录 |
| 2 | 获取会话列表 | ✅ 已实现 | 批量查询会话 + 批量查询用户，无 N+1 问题 |
| 3 | 发送消息 | ✅ 已实现 | `MessageServiceImpl.sendMessage()` 写入 DB + 更新会话最后消息 + 正确累加未读数 (`unread_count = unread_count + 1`) |
| 4 | 消息列表（分页） | ✅ 已实现 | MyBatis-Plus 分页查询，含发送者信息映射 |
| 5 | 标记已读 | ✅ 已实现 | `ChatController.readConversation()` 设置 `unread_count=0` |
| 6 | 聊天枚举 | ✅ 已实现 | `MessageTypeEnum`（TEXT/IMAGE/SYSTEM）+ `ConversationTypeEnum`（SINGLE/GROUP）位于 common 模块，所有 type 操作已使用枚举 |
| 7 | 群组设置 | ✅ 已实现 | `GroupServiceImpl` 支持创建/邀请/踢人/转让/改名 API，`GroupController` 暴露 REST 接口，含成员详情/info 端点。Session 8 修复：`invite()` 空指针 + 3 个端点 `@RequestParam` → `@RequestBody` |
| 8 | 模块解耦 | ✅ 已实现 | chat 模块使用独立 `ChatUsersMapper`，移除对 auth 模块的编译依赖 |
| 9 | 群聊前端 UI | ✅ 已实现 | 建群页面(`pages/group/create.vue`) + 群管理页面(`pages/group/manage.vue`) + 聊天页头部群管理入口 |

### 6.3 WebSocket

| # | 功能 | 状态 | 详情 |
|---|------|------|------|
| 1 | WebSocket 初始化 | ✅ 已实现 | `WebSocketConfig`（`@EnableWebSocket`）+ `ChatWebSocketHandler`（`TextWebSocketHandler`） |
| 2 | 会话管理 | ✅ 已实现 | `ConcurrentHashMap` 本地缓存 + Redis 路由键 `flash:chat:ws:route:{userId}`→`instanceId`，跨实例分发由 RabbitMQ fanout 保障 |
| 3 | 用户与 Session 关联 | ✅ 已实现 | Redis `flash:chat:ws:{userId}`→sessionId + `flash:chat:ws:route:{userId}`→instanceId，实例路由避免冗余推送 |
| 4 | 服务端心跳 | ✅ 已实现 | 服务端被动响应 `ping→pong`，记录最后活动时间，每30s检查超时连接（60s无消息主动断开），客户端10s pong超时检测触发重连 |
| 5 | 实时消息推送 | ✅ 已实现 | `RabbitMQMessageListener` 接收 fanout 广播，调用 `sendToUser(member.getUserId(), body)` 推送给非发送方成员 |
| 6 | 广播方法 | ✅ 已实现 | `broadcastToConversation()` 查询成员 + `sendToUser` 推送；`RabbitMQMessageListener` 复用此方法 |
| - | 清理死代码 | ✅ Session 8 | 移除 `handleTextMessage` 中废弃的 "message" 处理分支（前端使用 REST 发送消息，此分支不可达且含 ClassCastException） |
| 7 | 离线消息推送 | ✅ 已实现 | 广播时用户不在线则存入 `flash:chat:offline:{userId}` Redis list；上线后 `afterConnectionEstablished` 自动推送全部离线消息并清理队列 |
| 8 | Netty 服务 | ❌ 未实现 | 无 Netty 依赖，使用 Spring WebSocket |

### 6.4 消息存储

| # | 功能 | 状态 | 详情 |
|---|------|------|------|
| 1 | 消息表 | ✅ 已实现 | `message` 表含 id/conversationId/senderId/content/type/status/createTime |
| 2 | 会话表 | ✅ 已实现 | `conversation` 表含 type/name/ownerId/lastMessage/lastMessageTime |
| 3 | 会话成员表 | ✅ 已实现 | `conversation_members` 表含 unreadCount/isTop/isMute |
| 4 | SQL 建表脚本 | ✅ 已实现 | 项目根目录 `schema.sql` 含 4 张表完整 DDL |

---

## 七、消息队列

| # | 功能 | 状态 | 详情 |
|---|------|------|------|
| 1 | RabbitMQ 消息队列 | ✅ 已实现 | `RabbitMQConfig`（fanout exchange `flash.chat.fanout` + 匿名队列），`RabbitTemplate` 替代 Redis `convertAndSend`，`RabbitMQMessageListener` 替代 `RedisMessageListener`。Session 8 修复：Nacos 配置端口 `15672` → `5672`，已核实连接成功 |

---


---

## 九、数据库设计对比

### 9.1 文档与实际差异

| 表 | 文档中的表名 | 代码中的表名 | 状态 |
|---|-------------|-------------|------|
| users | `users` | `flash_chat.users` | ✅ 一致 |
| conversations | `conversations` | `conversations` | ✅ 已修正 |
| conversation_members | `conversation_members` | `conversation_members` | ✅ 一致 |
| messages | `messages` | `messages` | ✅ 已修正 |

### 9.2 FEATURE_AUDIT 文档中的 SQL 与实际差异

FEATURE_AUDIT §8 中的 SQL 使用 `BIGINT AUTO_INCREMENT` 主键，但实际代码 POJO 使用 `String` 类型（UUID），表使用 `varchar(32)`。这两个 SQL 定义与实际代码实现不符，应视为参考设计而非实际 DDL。

---

## 十、待实现功能优先级建议

### P0 - 核心体验（建议优先）

| 功能 | 理由 |
|------|------|

### P1 - 文件能力

| 功能 | 理由 |
|------|------|

### P2 - 社交增强

| 功能 | 理由 |
|------|------|
| 群聊页面 UI | 群管理、群聊前端界面 |

### P3 - 深入功能

| 功能 | 理由 |
|------|------|
| 灰度 / 枚举常量 | 用枚举替代魔法数字（type=1/2） |

### P4 - 架构升级

| 功能 | 理由 |
|------|------|
| Netty 替代 Spring WebSocket | 更高性能与扩展性 |
| Flyway / Liquibase 集成 | 数据库迁移自动化 |

---

## 十一、技术栈现状

| 技术 | 状态 | 说明 |
|------|------|------|
| Spring Boot 3.2.4 | ✅ | 核心框架 |
| Spring Cloud 2023.0.1 | ✅ | 微服务框架 |
| Spring Cloud Alibaba 2023.0.1.0 | ✅ | 阿里巴巴组件 |
| Nacos 2.2+ | ✅ | 配置中心 + 服务发现（4 个 `-dev.yaml` 已推送） |
| MySQL 8.0 | ✅ | 数据库 |
| Redis 6.0+ | ✅ | 缓存 + session/路由/离线消息（不再用于 Pub/Sub） |
| MyBatis-Plus 3.5.5 | ✅ | ORM |
| Spring WebSocket | ✅ | 实时通信 |
| Tencent Cloud SMS SDK | ✅ | 已集成但未启用 |
| Hutool | ✅ | 工具库 |
| Minio | ✅ | 文件存储集成（`MinioFileStrategy` + `MinioProperties`，支持预签名URL） |
| Netty | ❌ | 未使用 |
| RabbitMQ | ✅ | 消息队列集成（fanout exchange 替代 Redis Pub/Sub，持久化保障） |
| JWT | ❌ | 未使用 |
| Flyway / Liquibase | ❌ | 未集成（`schema.sql` 已提供，需手动执行） |

---

*文档由 opencode 基于代码库通读生成*
