package io.github.zh.auth.interceptor;

import io.github.zh.auth.constants.RedisKeyConstants;
import io.github.zh.auth.exception.ResponseEnum;
import io.github.zh.common.exception.BizException;
import io.github.zh.common.utils.ip.IPUtil;
import io.github.zh.common.utils.redis.RedisUtil;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

@Slf4j
@Component
public class SMSInterceptor implements HandlerInterceptor {
    @Resource
    private RedisUtil redisUtil;
    /**
    * 前置处理器
    */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String requestIp = IPUtil.getRequestIp(request);

        boolean ipInRedis = redisUtil.keyIsExist(RedisKeyConstants.SMS_IP_KEY +requestIp);

        if (ipInRedis){
            log.info("ip:{}已存在", requestIp);
            throw new BizException(ResponseEnum.SMS_SEND_FREQUENTLY);
        }

        /**
         * 返回值true表示继续执行，false表示不执行
         */
        return true;
    }

    /**
    * 请求进入controller但未渲染视图
    */
    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        HandlerInterceptor.super.postHandle(request, response, handler, modelAndView);
    }

    /**
    * 请求处理完毕，渲染视图完毕
    */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        HandlerInterceptor.super.afterCompletion(request, response, handler, ex);
    }
}
