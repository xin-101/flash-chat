# Flash Chat

A microservice-based instant messaging system with complete front-end and back-end solutions.

**Current Status:** Core features complete — supports one-on-one chat, group chat, Moments (朋友圈), friend management, and file storage.

## Project Structure

```
flash-chat/
├── flash-chat-common/          # Common module (Response, exceptions, Redis, IP utils)
├── flash-chat-model/           # Data models (entities, VOs, BOs)
├── flash-chat-gateway/         # Gateway service (port: 1000)
├── flash-chat-web/
│   ├── flash-chat-web-auth/    # Auth service (port: 8890)
│   ├── flash-chat-web-chat/    # Chat service (port: 8892)
│   └── flash-chat-web-file/    # File service (port: 8891)
└── flash-chat-app-opencode/    # Frontend (UniApp + Vue3)
```

## Technology Stack

### Backend
- **Core Framework**: Spring Boot 3.2.4
- **Microservices**: Spring Cloud 2023.0.1 + Alibaba 2023.0.1.0
- **Config & Discovery**: Nacos 2.2+
- **Database**: MySQL 8.0
- **Cache**: Redis 6.0+ (sessions, routing, offline messages)
- **Message Queue**: RabbitMQ (WebSocket broadcast)
- **ORM**: MyBatis-Plus 3.5.5
- **Connection Pool**: Druid 1.2.16
- **File Storage**: Minio (optional, swappable with local storage)
- **JDK**: Java 21

### Frontend
- **Framework**: UniApp + Vue 3 (Composition API)
- **IDE**: HBuilderX
- **Target**: WeChat Mini Program

## Features

### Implemented
- ✅ SMS code login (auto-registration)
- ✅ Session list (pull-to-refresh, unread badges)
- ✅ Create / get existing conversations
- ✅ Message send & receive (paginated)
- ✅ WebSocket real-time messaging (heartbeat, auto-reconnect, offline push)
- ✅ RabbitMQ broadcast (distributed multi-instance support)
- ✅ User search (global + friend-scoped)
- ✅ Profile editing (nickname, avatar, signature, etc.)
- ✅ Group chat (create, invite, kick, transfer ownership, rename)
- ✅ Friend management (add, approve, remark, block, delete)
- ✅ Friend request pagination
- ✅ Moments (post, like, comment, delete)
- ✅ File upload (local + Minio strategy via factory pattern)
- ✅ Minio file proxy (stream files directly)
- ✅ IP rate limiting
- ✅ Token authentication (Bearer + Redis bidirectional validation)
- ✅ CORS handling
- ✅ Unified response format
- ✅ Nacos config center + service discovery

### Pending
- Message recall
- Voice / video messages
- Image preview
- End-to-end encryption

## Quick Start

### 1. Prerequisites

```bash
# Required
JDK 21
Maven 3.8+
MySQL 8.0 (port: 3306)
Redis 6.0+ (port: 6379, password: 123456)
RabbitMQ (port: 5672)

# Optional
Nacos 2.2+ (config center + service discovery)
Minio (file storage, local storage is default)
```

### 2. Database Setup

```sql
CREATE DATABASE flash_chat CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
-- Then run schema.sql in project root (9 tables total)
```

### 3. Start Backend

Start in order:

```
1. flash-chat-gateway   → GatewayApplication.main()     (port: 1000)
2. flash-chat-web-auth  → AuthApplication.main()         (port: 8890)
3. flash-chat-web-chat  → ChatApplication.main()         (port: 8892)
4. flash-chat-web-file  → FileApplication.main()         (port: 8891)
```

### 4. Start Frontend

```
1. HBuilderX → Import flash-chat-app-opencode project
2. Run → Run to Mini Program Simulator → WeChat Developer Tools
3. Open unpackage/dist/dev/mp-weixin in WeChat Developer Tools
```

## API Overview

All requests go through the gateway at `localhost:1000`.  
Authenticated endpoints require header `Authorization: Bearer {token}`.

### Auth `/auth/**`
| Method | Path | Description |
|--------|------|-------------|
| GET | `/auth/auth/sendSms?phone={phone}` | Send SMS code |
| POST | `/auth/auth/login` | Login |
| PUT | `/auth/auth/user/info` | Update profile |
| PUT | `/auth/auth/user/flashChatNum` | Update flash chat number |
| POST | `/auth/auth/logout` | Logout |

### Chat `/chat/**`
| Method | Path | Description |
|--------|------|-------------|
| GET | `/chat/conversation/list` | List conversations |
| POST | `/chat/conversation/create?targetUserId={id}` | Create conversation |
| PUT | `/chat/conversation/read/{conversationId}` | Mark as read |
| GET | `/chat/message/list?conversationId={id}&page=1&size=20` | Messages |
| POST | `/chat/message/send` | Send message |
| GET | `/chat/user/search?keyword={keyword}` | Search users |
| GET | `/chat/user/info/{targetUserId}` | User info |

### Friends `/chat/**`
| Method | Path | Description |
|--------|------|-------------|
| POST | `/chat/friend/request` | Send request |
| PUT | `/chat/friend/request/{id}/approve` | Approve |
| PUT | `/chat/friend/request/{id}/reject` | Reject |
| GET | `/chat/friend/requests/page?page=1&size=10` | Requests (paginated) |
| GET | `/chat/friend/list` | Friend list |
| DELETE | `/chat/friend/{friendId}` | Remove friend |
| PUT | `/chat/friend/{friendId}/remark` | Set remark |
| PUT | `/chat/friend/{friendId}/block` | Block |
| PUT | `/chat/friend/{friendId}/unblock` | Unblock |

### Group `/chat/**`
| Method | Path | Description |
|--------|------|-------------|
| POST | `/chat/group/create` | Create group |
| POST | `/chat/group/invite` | Invite members |
| DELETE | `/chat/group/member` | Remove member |
| PUT | `/chat/group/transfer` | Transfer ownership |
| PUT | `/chat/group/name` | Rename group |
| GET | `/chat/group/members?conversationId={id}` | Member IDs |
| GET | `/chat/group/members/detail?conversationId={id}` | Member details |
| GET | `/chat/group/info/{conversationId}` | Group info |

### Moments `/chat/**`
| Method | Path | Description |
|--------|------|-------------|
| POST | `/chat/moment/create` | Create post |
| DELETE | `/chat/moment/{momentId}` | Delete post |
| GET | `/chat/moment/list?page=1&size=10` | List feed |
| POST | `/chat/moment/{momentId}/like` | Like |
| DELETE | `/chat/moment/{momentId}/like` | Unlike |
| POST | `/chat/moment/{momentId}/comment` | Comment |
| DELETE | `/chat/moment/comment/{commentId}` | Delete comment |

### Files `/file/**`
| Method | Path | Description |
|--------|------|-------------|
| POST | `/file/upload` | Upload file |
| POST | `/file/upload/image` | Upload image |
| POST | `/file/upload/avatar` | Upload avatar |
| GET | `/file/minio-files/{dir}/{filename}` | Minio proxy |

### WebSocket
```
ws://localhost:1000/chat/ws?token={token}
```

## Database

| Table | Description |
|-------|-------------|
| users | User profiles |
| conversations | Chat sessions |
| conversation_members | Session members |
| messages | Messages |
| friend_request | Friend requests |
| user_friend | Friend relationships |
| moment | Moments posts |
| moment_like | Moment likes |
| moment_comment | Moment comments |

## Known Issues

| Issue | Description |
|-------|-------------|
| SMS not sent | Tencent Cloud SDK integrated but disabled; codes logged only |
| WebSocket not truly distributed | Uses in-memory sessions; RabbitMQ + Redis routing keys handle cross-instance delivery |

## Documentation

- [DEVELOPMENT.md](DEVELOPMENT.md) — dev standards, API docs, database design, startup guide
- [FEATURE_AUDIT.md](FEATURE_AUDIT.md) — full 58-item feature audit

## License

Apache License 2.0

## Contact

- Email: xinfukun@outlook.com

---

**Note: This project is still under development. Not recommended for production use.**
