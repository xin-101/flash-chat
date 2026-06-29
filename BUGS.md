# Flash Chat 已知 Bug 清单

> 最后更新：2026-06-28 | Session 8

## 已修复

### CRITICAL

| # | 文件 | 描述 | 修复方式 |
|---|------|------|----------|
| C1 | `TokenAuthFilter.java:59-71` | Token 验证绕过：仅检查 `flash:chat:user:{userId}` 存在性，未对比客户端实际 token。知道 userId 即可冒充。 | 增加 `Authorization: Bearer {token}` 头解析，对比客户端 token 与 Redis 存储值是否匹配。 |
| C2 | `TokenAuthFilter.java:44-57` | WebSocket 升级时 token 无效/缺失仍转发，未注入 userId header。 | WebSocket 请求先验证 `flash:chat:token:{token}` 反向映射，无效则拒绝。 |

### HIGH

| # | 文件 | 行 | 描述 | 修复方式 |
|---|------|----|------|----------|
| B1 | `ChatWebSocketHandler.java` | 115 | `ClassCastException`：`type` 字段在第106行已解析为 String `"message"`，第115行又强转为 `(Integer)`。 | 移除死的 "message" 处理分支（前端使用 REST 发送消息，此分支不可达）。 |
| B2 | `ChatWebSocketHandler.java` | 111-122 | WebSocket `handleTextMessage` 的 "message" 分支绕过 `MessageService.sendMessage()`，消息不存库、不广播。 | 此分支为死代码，已整体移除，添加 `else { log.warn("未知消息类型") }`。 |
| B3 | `GroupServiceImpl.java` | 92 | `invite()` 迭代 `newMemberIds` 前无 null 检查 → NPE。 | 添加 `if (newMemberIds == null || newMemberIds.isEmpty())` 前置校验。 |
| B4 | `GroupController.java` + `api/chat.js` | 多行 | 前端 `removeGroupMember`/`transferGroup`/`updateGroupName` 发送 JSON body，后端用 `@RequestParam` 读取 query param → 参数静默忽略。 | 后端改为 `@RequestBody Map<String, String>` 读取 body 参数，与 `create`/`invite` 一致。 |

### MEDIUM

| # | 文件 | 描述 | 状态 |
|---|------|------|------|
| B5 | `api/chat.js` + `pages/index/index.vue` | `getConversationList` 调用已包在 try-catch 中，但仅 `console.error` 不显示用户 toast。 | 非 Bug（已有异常保护） |
| B6 | `pages/index/index.vue` | 模板中 `item.lastMessage` 经 `|| '暂无消息'` 回退处理，无 NPE 风险。 | 非 Bug |
| B7 | `ChatWebSocketHandler.java` | `afterConnectionEstablished` 设置 Redis 路由键 `flash:chat:ws:route:{userId}` TTL=5min，但之后未刷新。路由可能过期导致消息无法送达本实例。 | 待修复 |
| B8 | `ChatWebSocketHandler.java` | `sendToUser` 已包含 `session.isOpen()` 检查（line 162）。 | 非 Bug |
| B9 | `pages/moment/list.vue` | 首次加载列表失败仅 `console.error`，无用户提示。 | LOW |
| B10 | `pages/chat/detail.vue` | `onWsMessage` 监听 WS 广播消息并追加到列表。后端广播已排除发送者，不会产生重复。 | 非 Bug |

## 配置文件问题（已修复）

| # | 问题 | 修复方式 |
|---|------|----------|
| CFG1 | Nacos `flash-chat-web-chat-dev.yaml` 中 RabbitMQ 端口为 `15672`（管理 UI 端口），应为 `5672`（AMQP 协议端口）。 | 已修正为 `port: 5672` |
| CFG2 | Nacos `flash-chat-web-auth-dev.yaml` 中 Redis 配置 `password: 123456`，但实际 Redis Stack 容器无密码。 | 已移除 `password` 字段 |

## 已知问题（待确认/未复现）

| # | 描述 |
|---|------|
| Q1 | Chat 服务 `/user/search` 返回 `50000` 错误码，可能是 MyBatis 查询问题或配置缺失。 |
| Q2 | 网关 `TokenAuthFilter` 中 `USER_TOKEN_KEY` 硬编码 `"flash:chat:user:"`，与 `RedisKeyConstants.USER_TOKEN_KEY` 重复定义，修改时需同步两处。 |
