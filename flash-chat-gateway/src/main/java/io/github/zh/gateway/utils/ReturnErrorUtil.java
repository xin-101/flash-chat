package io.github.zh.gateway.utils;

import com.alibaba.fastjson.JSON;
import io.github.zh.common.response.Response;
import io.github.zh.gateway.exception.ResponseEnum;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

public class ReturnErrorUtil {
    public static Mono<Void> display(ServerWebExchange exchange, ResponseEnum responseEnum) {
        // 1.拿到响应
        ServerHttpResponse response = exchange.getResponse();
        // 2.构建返回对象
        Response jsonResult = Response.fail(responseEnum);
        // 3.设置返回的header
        if (!response.getHeaders().containsKey("Content-Type")){
            response.getHeaders().add("Content-Type", MimeTypeUtils.APPLICATION_JSON_VALUE);
        }
        // 4.将response的状态码改为500
        response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
        // 5.将返回对象转为json，并将该信息放置到response
        String jsonString= JSON.toJSONString(jsonResult);

        DataBuffer buffer=response.bufferFactory().wrap(jsonString.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }
}
