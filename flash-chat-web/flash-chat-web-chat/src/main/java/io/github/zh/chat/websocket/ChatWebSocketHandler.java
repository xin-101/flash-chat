package io.github.zh.chat.websocket;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.zh.chat.mapper.ConversationMemberMapper;
import io.github.zh.model.chat.pojo.ConversationMember;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ConversationMemberMapper conversationMemberMapper;

    // 用户ID -> WebSocket会话
    private final ConcurrentHashMap<String, WebSocketSession> userSessions = new ConcurrentHashMap<>();

    // WebSocket会话ID -> 用户ID
    private final ConcurrentHashMap<String, String> sessionUsers = new ConcurrentHashMap<>();

    // 实例唯一标识（用于分布式路由）
    private static final String INSTANCE_ID = UUID.randomUUID().toString().replace("-", "").substring(0, 12);

    private static final String ROUTE_KEY_PREFIX = "flash:chat:ws:route:";
    private static final Duration ROUTE_TTL = Duration.ofMinutes(5);

    private static final String OFFLINE_KEY_PREFIX = "flash:chat:offline:";

    // 会话最后活动时间记录
    private final ConcurrentHashMap<String, Instant> sessionLastActiveTime = new ConcurrentHashMap<>();
    // 心跳超时：60秒无任何消息则断开（客户端30秒发一次ping，允许丢失2次）
    private static final long HEARTBEAT_TIMEOUT_SECONDS = 60;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String userId = getUserIdFromSession(session);
        if (userId != null) {
            userSessions.put(userId, session);
            sessionUsers.put(session.getId(), userId);
            sessionLastActiveTime.put(session.getId(), Instant.now());
            log.info("[WS] 连接建立: userId={}, sessionId={}, 在线用户: {}",
                    userId, session.getId(), userSessions.keySet());

            // 保存会话映射到Redis
            stringRedisTemplate.opsForValue().set("flash:chat:ws:" + userId, session.getId());
            // 保存实例路由（分布式环境标识该用户连接在此实例）
            stringRedisTemplate.opsForValue().set(ROUTE_KEY_PREFIX + userId, INSTANCE_ID, ROUTE_TTL);

            // 推送离线消息
            pushOfflineMessages(userId);
        } else {
            log.warn("[WS] 连接建立但无法解析userId, 关闭连接, URI: {}", session.getUri());
            try {
                session.close(CloseStatus.POLICY_VIOLATION);
            } catch (IOException e) {
                log.warn("[WS] 关闭无效token连接异常", e);
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String userId = sessionUsers.remove(session.getId());
        sessionLastActiveTime.remove(session.getId());
        if (userId != null) {
            userSessions.remove(userId);
            try {
                // 仅当 Redis 中存储的仍是当前 session 时才删除，避免误删新连接的 key
                String wsKey = "flash:chat:ws:" + userId;
                String storedSessionId = stringRedisTemplate.opsForValue().get(wsKey);
                if (session.getId().equals(storedSessionId)) {
                    stringRedisTemplate.delete(wsKey);
                    stringRedisTemplate.delete(ROUTE_KEY_PREFIX + userId);
                }
            } catch (Exception e) {
                log.warn("Redis不可用，跳过清理Redis键: userId={}", userId);
            }
            log.info("WebSocket连接关闭: userId={}, code={}, reason={}",
                    userId, status.getCode(), status.getReason());
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            String userId = getUserIdFromSession(session);
            if (userId == null) {
                return;
            }

            // 更新最后活动时间
            sessionLastActiveTime.put(session.getId(), Instant.now());

            String payload = message.getPayload();
            Map<String, Object> messageMap = objectMapper.readValue(payload, Map.class);
            String type = (String) messageMap.get("type");

            if ("ping".equals(type)) {
                // 处理心跳
                session.sendMessage(new TextMessage("{\"type\":\"pong\"}"));
            } else {
                log.warn("[WS] 未知消息类型: type={}, userId={}", type, userId);
            }
        } catch (Exception e) {
            log.error("处理WebSocket消息失败", e);
        }
    }

    /**
     * 检查并关闭超时的空闲连接
     */
    public void checkIdleConnections() {
        Instant now = Instant.now();
        for (Map.Entry<String, Instant> entry : sessionLastActiveTime.entrySet()) {
            String sessionId = entry.getKey();
            Instant lastActive = entry.getValue();
            if (lastActive == null) continue;

            long idleSeconds = Duration.between(lastActive, now).getSeconds();
            if (idleSeconds > HEARTBEAT_TIMEOUT_SECONDS) {
                String userId = sessionUsers.get(sessionId);
                WebSocketSession session = (userId != null) ? userSessions.get(userId) : null;
                if (session != null && session.isOpen()) {
                    log.warn("[WS] 心跳超时断开: userId={}, sessionId={}, 空闲={}s",
                            userId, sessionId, idleSeconds);
                    try {
                        session.close(CloseStatus.POLICY_VIOLATION);
                    } catch (IOException e) {
                        log.warn("[WS] 关闭超时连接异常: userId={}", userId, e);
                    }
                }
            }
        }
    }

    /**
     * 向指定用户发送消息
     * @return true=发送成功, false=用户未连接
     */
    public boolean sendToUser(String userId, String message) {
        WebSocketSession session = userSessions.get(userId);
        if (session != null && session.isOpen()) {
            try {
                session.sendMessage(new TextMessage(message));
                return true;
            } catch (IOException e) {
                log.error("[WS] 发送消息失败: userId={}", userId, e);
                return false;
            }
        }
        log.debug("[WS] 用户 {} 未连接（可能在其他实例），当前在线: {}", userId, userSessions.keySet());
        return false;
    }

    /**
     * 仅当用户路由指向本实例时发送
     * @return true=发送成功, false=用户不在此实例
     */
    public boolean sendToUserIfLocal(String userId, String message) {
        String route = stringRedisTemplate.opsForValue().get(ROUTE_KEY_PREFIX + userId);
        if (!INSTANCE_ID.equals(route)) {
            log.debug("[WS] 用户 {} 的路由指向其他实例 ({}), 跳过本地发送", userId, route);
            return false;
        }
        return sendToUser(userId, message);
    }

    /**
     * 向会话所有成员广播消息（排除发送者）
     * 分布式环境下各实例只发送本地连接的成员
     */
    public void broadcastToConversation(String conversationId, String message, String excludeUserId) {
        LambdaQueryWrapper<ConversationMember> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ConversationMember::getConversationId, conversationId);
        List<ConversationMember> members = conversationMemberMapper.selectList(wrapper);

        if (members == null || members.isEmpty()) {
            log.warn("[WS] 广播失败: 会话 {} 无成员", conversationId);
            return;
        }

        int localSent = 0;
        int offlineSaved = 0;
        for (ConversationMember member : members) {
            if (member.getUserId() != null && !member.getUserId().equals(excludeUserId)) {
                if (sendToUser(member.getUserId(), message)) {
                    localSent++;
                } else {
                    if (saveOfflineIfLocal(member.getUserId(), message)) {
                        offlineSaved++;
                    }
                }
            }
        }
        log.info("[WS] 广播完成: 会话={}, 排除={}, 本地推送={}, 离线存储={}/{}",
                conversationId, excludeUserId, localSent, offlineSaved, members.size() - (excludeUserId != null ? 1 : 0));
    }

    /**
     * 获取当前在线用户列表
     */
    public java.util.Set<String> getOnlineUsers() {
        return userSessions.keySet();
    }

    /**
     * 如果用户路由指向本实例但不在此实例在线，保存离线消息
     * @return true=已保存离线消息
     */
    private boolean saveOfflineIfLocal(String userId, String message) {
        String route = stringRedisTemplate.opsForValue().get(ROUTE_KEY_PREFIX + userId);
        if (INSTANCE_ID.equals(route)) {
            String key = OFFLINE_KEY_PREFIX + userId;
            stringRedisTemplate.opsForList().rightPush(key, message);
            log.info("[WS] 离线消息已存储: userId={}", userId);
            return true;
        }
        return false;
    }

    /**
     * 用户上线后推送所有离线消息，然后清理
     */
    private void pushOfflineMessages(String userId) {
        String key = OFFLINE_KEY_PREFIX + userId;
        try {
            Long size = stringRedisTemplate.opsForList().size(key);
            if (size == null || size == 0) return;

            List<String> messages = stringRedisTemplate.opsForList().range(key, 0, -1);
            if (messages == null || messages.isEmpty()) return;

            log.info("[WS] 推送离线消息: userId={}, 数量={}", userId, messages.size());

            WebSocketSession session = userSessions.get(userId);
            if (session == null || !session.isOpen()) return;

            for (String msg : messages) {
                session.sendMessage(new TextMessage(msg));
            }

            stringRedisTemplate.delete(key);
            log.info("[WS] 离线消息推送完成: userId={}, 已删除离线队列", userId);
        } catch (Exception e) {
            log.error("[WS] 推送离线消息异常: userId={}", userId, e);
        }
    }

    private String getUserIdFromSession(WebSocketSession session) {
        String query = session.getUri().getQuery();
        if (query != null && query.contains("token=")) {
            String token = query.split("token=")[1].split("&")[0];
            String userIdStr = (String) stringRedisTemplate.opsForValue().get("flash:chat:token:" + token);
            if (userIdStr != null) {
                log.debug("[WS] Token解析成功: token={}, userId={}", token, userIdStr);
                return userIdStr;
            } else {
                log.warn("[WS] Token未找到: flash:chat:token:{} 不存在", token);
            }
        }
        return (String) session.getAttributes().get("userId");
    }
}