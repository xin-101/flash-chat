package io.github.zh.gateway.filter;

import io.github.zh.common.utils.redis.RedisUtil;
import io.github.zh.gateway.exception.ResponseEnum;
import io.github.zh.gateway.utils.ReturnErrorUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class TokenAuthFilter implements GlobalFilter, Ordered {

    private static final String USER_TOKEN_KEY = "flash:chat:user:";
    private static final AntPathMatcher pathMatcher = new AntPathMatcher();

    private static final String[] WHITE_LIST = {
            "/auth/auth/sendSms",
            "/auth/auth/login",
            "/file/minio-files/**",
            "/file/static/**",
            "/minio-files/**",
            "/files/**",
    };

    @Resource
    private RedisUtil redisUtil;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        // 跳过白名单接口
        for (String pattern : WHITE_LIST) {
            if (pathMatcher.match(pattern, path)) {
                return chain.filter(exchange);
            }
        }

        // WebSocket 升级请求：从 query param 验证 token
        String upgradeHeader = exchange.getRequest().getHeaders().getFirst("Upgrade");
        if ("websocket".equalsIgnoreCase(upgradeHeader)) {
            String token = exchange.getRequest().getQueryParams().getFirst("token");
            if (token == null || token.isEmpty()) {
                log.warn("[TokenAuth] WebSocket 缺少 token: {}", path);
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }
            // 通过 token 反查 userId
            String wsTokenKey = "flash:chat:token:" + token;
            String userId = redisUtil.get(wsTokenKey);
            if (userId == null || userId.isEmpty()) {
                log.warn("[TokenAuth] WebSocket token 无效: {}", path);
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }
            // 注入 userId header，供下游 ChatWebSocketHandler 使用
            ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                    .header("userId", userId)
                    .build();
            return chain.filter(exchange.mutate().request(mutatedRequest).build());
        }

        String userId = exchange.getRequest().getHeaders().getFirst("userId");
        if (userId == null || userId.isEmpty()) {
            log.warn("[TokenAuth] 请求缺少userId header: {}", path);
            return ReturnErrorUtil.display(exchange, ResponseEnum.PARAM_ERROR);
        }

        String authHeader = exchange.getRequest().getHeaders().getFirst("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("[TokenAuth] 缺少Authorization header: {}", path);
            return ReturnErrorUtil.display(exchange, ResponseEnum.PARAM_ERROR);
        }

        String clientToken = authHeader.substring(7);
        String storedToken = redisUtil.get(USER_TOKEN_KEY + userId);
        if (storedToken == null || storedToken.isEmpty() || !storedToken.equals(clientToken)) {
            log.warn("[TokenAuth] token验证失败: userId={}, path={}", userId, path);
            return ReturnErrorUtil.display(exchange, ResponseEnum.SYSTEM_ERROR);
        }

        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
