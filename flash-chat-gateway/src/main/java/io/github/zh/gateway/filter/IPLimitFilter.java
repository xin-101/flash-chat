package io.github.zh.gateway.filter;


import io.github.zh.common.utils.ip.IPUtil;
import io.github.zh.common.utils.redis.RedisUtil;
import io.github.zh.gateway.exception.ResponseEnum;
import io.github.zh.gateway.utils.ReturnErrorUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
@Slf4j
@RefreshScope
public class IPLimitFilter implements GlobalFilter, Ordered {
    @Resource
    private RedisUtil redisUtil;

    // 请求次数
    @Value("${ip.filter.request-count}")
    private int request_count;
    // 请求间隔
    @Value("${ip.filter.request-interval}")
    private int request_interval;
    // 黑名单持续时间
    @Value("${ip.filter.black-ip-time}")
    private int black_ip_time;
    // ip访问key
    private static final String ip_write_key = "flash:chat:gateway:ip:write:limit:";
    private static final String ip_black_key = "flash:chat:gateway:ip:black:limit";

    /**
     * IP拦截
     * 10s内拦截3次，如果超过三次，则加入黑名单
     * 黑名单持续时间60s
     */

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // 跳过WebSocket升级请求
        String upgrade = exchange.getRequest().getHeaders().getFirst("Upgrade");
        if ("websocket".equalsIgnoreCase(upgrade)) {
            return chain.filter(exchange);
        }

        log.info("网关 IP拦截器 开始进行拦截...");
        log.info("continueCounts->{},timeInterval->{},limitTimes->{}",
                request_count, request_interval, black_ip_time);

        return doLimit(exchange, chain);
    }

    /**
     * ip请求次数刷新
     */
    private Mono<Void> doLimit(ServerWebExchange exchange, GatewayFilterChain chain) {
        // 1.获取到请求ip地址
        ServerHttpRequest request = exchange.getRequest();
        String ip = IPUtil.getIp(request);
        // 2.正常/黑名单 存放到 redis中的
        String ipRedisWriteKey = ip_write_key + ip;
        String ipRedisBlackKey = ip_black_key + ip;

        // 1 2 3
        long ttl=redisUtil.ttl(ipRedisBlackKey);
        if (ttl>0){
            // 3.中止请求
            return ReturnErrorUtil.display(exchange, ResponseEnum.IP_ERROR);
        }

        // 3.黑名单正常 为白名单的key 进行累加
        long writeIpCount = redisUtil.increment(ipRedisWriteKey, 1);
        if (writeIpCount==1){
            redisUtil.expire(ipRedisWriteKey, request_interval);
        }
        // 4.判断是否超过3次
        if (writeIpCount>=request_count){
            redisUtil.set(ipRedisBlackKey, "1", black_ip_time);

            // 3.中止请求
            return ReturnErrorUtil.display(exchange, ResponseEnum.IP_ERROR);
        }



        return chain.filter(exchange);
    }


    /* 优先级 越小越先执行，IP限流应在Token验证之前 */
    @Override
    public int getOrder() {
        return -1;
    }
}
