package io.github.zh.chat.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.zh.chat.mapper.ConversationMemberMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class RabbitMQMessageListener {

    @Autowired
    private ChatWebSocketHandler chatWebSocketHandler;

    @Autowired
    private ConversationMemberMapper conversationMemberMapper;

    @Autowired
    private ObjectMapper objectMapper;

    @RabbitListener(queues = "#{chatQueue.name}")
    public void onMessage(String body) {
        try {
            Map<String, Object> msgMap = objectMapper.readValue(body, Map.class);
            String type = (String) msgMap.get("type");

            if ("message".equals(type)) {
                String conversationId = (String) msgMap.get("conversationId");
                String senderId = (String) msgMap.get("senderId");
                if (conversationId == null || conversationId.isEmpty()) {
                    log.warn("[Rabbit] 消息中缺少conversationId");
                    return;
                }
                log.info("[Rabbit] 转发消息: 会话={}, 发送者={}", conversationId, senderId);
                chatWebSocketHandler.broadcastToConversation(conversationId, body, senderId);
            } else if ("friend_request".equals(type)) {
                String toUserId = (String) msgMap.get("toUserId");
                if (toUserId != null) {
                    log.info("[Rabbit] 转发好友申请通知: to={}", toUserId);
                    chatWebSocketHandler.sendToUser(toUserId, body);
                }
            }
        } catch (Exception e) {
            log.error("[Rabbit] 处理消息异常", e);
        }
    }
}
