# Flash Chat - Session Summary

## Session 8: 2026-06-28
### Completed
- **Bug C1 (CRITICAL) — Token 验证绕过**: `TokenAuthFilter` 增加 `Authorization: Bearer {token}` 头对比，不再仅检查 userId 存在性
- **Bug C2 (CRITICAL) — WebSocket 无 token 仍转发**: WebSocket 升级前验证 `flash:chat:token:{token}` 反向映射，无效拒绝
- **Bug B1 (HIGH) — ClassCastException**: 移除 `ChatWebSocketHandler` 中死的 "message" 处理分支
- **Bug B2 (HIGH) — WebSocket 消息绕过 Service**: 同上，死代码已清理
- **Bug B3 (HIGH) — GroupServiceImpl NPE**: `invite()` 添加 null/empty 检查
- **Bug B4 (HIGH) — @RequestBody/@RequestParam 不匹配**: `GroupController` 中 3 个端点 (`removeMember`/`transferOwner`/`updateName`) 由 `@RequestParam` 改为 `@RequestBody`
- **Nacos 配置修复**: `flash-chat-web-chat-dev.yaml` RabbitMQ 端口 `15672`→`5672`；`flash-chat-web-auth-dev.yaml` 移除 Redis 密码
- **全栈服务启动**: 4 个后端服务全部启动并注册到 Nacos，已验证网关路由/登录鉴权/文件服务通过身份验证后的调用
- **文档**: 创建 `BUGS.md`，更新 `FEATURE_AUDIT.md`/`AGENTS.md`/`DEVELOPMENT.md`

### Key Files Modified
- `flash-chat-gateway/.../filter/TokenAuthFilter.java` (C1, C2)
- `flash-chat-web-chat/.../websocket/ChatWebSocketHandler.java` (B1, B2)
- `flash-chat-web-chat/.../service/impl/GroupServiceImpl.java` (B3)
- `flash-chat-web-chat/.../controller/GroupController.java` (B4)

### Key Files Created
- `BUGS.md`

## Session 7: 2026-06-28
### Completed
- **RabbitMQ 消息队列集成**: `RabbitMQConfig`（fanout exchange `flash.chat.fanout` + 匿名队列），`RabbitTemplate` 替换 `stringRedisTemplate.convertAndSend`，`RabbitMQMessageListener` 替换 `RedisMessageListener`，Service 层无感知切换
- **清理**: 删除 `RedisConfig.java` / `RedisMessageListener.java`（Redis 不再用于 Pub/Sub，仅用于 session/路由/离线消息）
- **表名统一**: `conversation`→`conversations`, `message`→`messages`（POJO `@TableName` + `schema.sql`）
- **IP 限流启用**: `application.yml` 添加默认限制参数，`application-prod.yml` 取消注释
- **群聊广播确认**: `broadcastToConversation` 已正确排除 `excludeUserId`
- **编译验证通过**: `mvn compile -q`

### Session 6: 2026-06-28
### Completed
- **离线消息推送**: 广播时用户不在线 → 存 `flash:chat:offline:{userId}` Redis list；上线后 `afterConnectionEstablished` 自动推送并清理队列
- **Minio 文件代理端点**: `MinioFileController` 代理 `/minio-files/**` → 从 Minio 流式返回文件，URL 永久有效
- **编译验证通过**: `mvn compile -q`

### Session 5: 2026-06-28
### Completed
- **Minio 文件存储集成**: `MinioFileStrategy` implements `FileStrategy`（上传/删除/7天预签名URL/自动创建bucket）
- **配置体系**: `MinioProperties`（endpoint/accessKey/secretKey/bucket），`application.yml` 默认 local 策略可切 minio，`application-prod.yml` 默认 minio
- **策略工厂**: `FileStrategyFactory` 按 `file.strategy` 选择 local 或 minio
- **编译验证通过**: `mvn compile -q`（全项目）

### Session 4: 2026-06-28
- **用户资料编辑前端**: `pages/my/edit.vue` 支持昵称/签名修改 + 头像上传（`uni.chooseImage` → `/file/upload`），保存后自动同步到 storage/store
- **好友申请分页**: 后端 `GET /friend/requests/page?page=&size=` 返回 `PageResult`(records/total/page/size)；前端 `onReachBottom` 触发 loadMore
- **API 扩展**: `api/auth.js` 添加 `updateUserInfo()`；`api/chat.js` 添加 `getFriendRequestsPage()`
- **路由注册**: `pages.json` 注册 `/pages/my/edit`，`我的` 页面替换"头像"占位项 → "编辑资料"导航
- **编译验证通过**: `mvn compile -pl flash-chat-web/flash-chat-web-chat -am -q`

### Session 3: 2026-06-28
- **WebSocket 网关路由**: 确认 `/chat/**` 路由正确转发 WS 升级请求，`TokenAuthFilter` 跳过 WebSocket 升级并注入 `userId` header
- **用户信息修改 API**: `PUT /auth/user/info` + `UpdateUserBO` 支持昵称/性别/头像/签名/朋友圈背景/聊天背景/生日/邮箱/地区等全字段更新
- **FEATURE_AUDIT 纠正**: 枚举常量/用户搜索/WebSocket路由/schema.sql → 实际已实现，状态同步

### Session 2: 2026-06-28
- **群聊前端 UI**: 建群页面(选好友+群名)、群管理页面(成员列表/踢人/转让/改群名/退出)、通讯录入口、会话列表群聊标识、聊天页群管理按钮
- **后端增强**: `GET /group/members/detail` (含成员信息), `GET /group/info/{id}` (ownerId+memberCount)
- **文档更新**: FEATURE_AUDIT.md / AGENTS.md

### Session 1: 2026-06-28
- **朋友圈全套功能**: Moment/MomentLike/MomentComment 实体 + Mapper + Service + Controller + 前端(list/create)
- **群聊管理API**: GroupServiceImpl(创建/邀请/踢人/转让/改名) + GroupController
- **schema.sql 补充**: 新增 moment/moment_like/moment_comment 三张表 DDL
- **DEVELOPMENT.md 更新**: 模块状态反映实际完成度

### Key Files Created
- `flash-chat-app-opencode/pages/my/edit.vue`
- `flash-chat-app-opencode/pages/group/create.vue`, `manage.vue`
- `flash-chat-app-opencode/pages/chat/detail.vue` (modified: group header)
- `flash-chat-app-opencode/pages/index/index.vue` (modified: group badge)
- `flash-chat-app-opencode/pages/friend/index.vue` (modified: create group entry, request pagination)
- `flash-chat-app-opencode/pages.json` (modified: register group pages, edit page)
- `flash-chat-web-chat/.../controller/FriendController.java` (modified: +page endpoint)
- `flash-chat-common/.../response/PageResult.java`
- `flash-chat-web-file/.../strategy/MinioFileStrategy.java`
- `flash-chat-web-file/.../config/MinioProperties.java`

### Pending
- 暂无
- 下一步可考虑：Netty 替换 Spring WebSocket / 图片预览 / 消息撤回

### Dev Commands
```bash
mvn compile -q                          # 编译整个项目
mvn compile -pl flash-chat-web/flash-chat-web-chat -am -q  # 编译 chat 模块
mvn compile -pl flash-chat-web/flash-chat-web-auth -am -q   # 编译 auth 模块
```

### Startup Sequence
```bash
mvn clean compile -q                    # 编译后启动（需先 mvn install -DskipTests）
# 分别启动：gateway → auth → chat → file
cd flash-chat-gateway; mvn spring-boot:run      # port 1000
cd flash-chat-web/flash-chat-web-auth; mvn spring-boot:run  # port 8890
cd flash-chat-web/flash-chat-web-chat; mvn spring-boot:run  # port 8892
cd flash-chat-web/flash-chat-web-file; mvn spring-boot:run  # port 8891
```
