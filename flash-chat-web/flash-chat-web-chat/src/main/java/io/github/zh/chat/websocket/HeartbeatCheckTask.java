package io.github.zh.chat.websocket;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class HeartbeatCheckTask {

    @Resource
    private ChatWebSocketHandler chatWebSocketHandler;

    @Scheduled(fixedRate = 30000)
    public void checkIdleConnections() {
        chatWebSocketHandler.checkIdleConnections();
    }
}
