package io.github.zh.common.aspect.log.aspect;

import io.github.zh.common.aspect.log.annotation.ApiOperationLog;
import io.github.zh.common.utils.json.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import com.alibaba.fastjson.JSON;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.function.Function;
import java.util.stream.Collectors;

@Aspect
@Slf4j
@Component
public class ApiOperationLogAspect {

    // 切点
    @Pointcut("@annotation(io.github.zh.common.aspect.log.annotation.ApiOperationLog)")
    public void apiOperationLog() {
    }

    // 环绕通知
    @Around("apiOperationLog()")
    public Object doAround(ProceedingJoinPoint joinPoint) throws Throwable {
        // 请求开始时间
        long startTime = System.currentTimeMillis();
        // 获取被请求的类和方法
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        // 请求入参
        Object[] args = joinPoint.getArgs();
        // 入参转 json 字符串
        String argsJsonStr = Arrays.stream(args).map(toJsonStr()).collect(Collectors.joining(","));
        // 功能描述信息
        String description = getApiOperationLogDescription(joinPoint);
        // 打印请求相关参数
        log.info("请求开始：[{}]，入参：{}，请求类：{}，请求方法：{}", description, argsJsonStr, className, methodName);
        // 执行切点方法
        Object result = joinPoint.proceed();
        // 执行耗时
        long executionTime = System.currentTimeMillis() - startTime;
        //  打印返回结果
        log.info("请求结束：[{}]，耗时：{}ms，返回结果：{}",
                description, executionTime, JSON.toJSONString(result));
        return result;
    }


    private String getApiOperationLogDescription(ProceedingJoinPoint joinPoint) {
        // 1.从ProceedingJoinPoint中获取MethodSignature
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        // 2.使用MethodSignature获取当前被注解的Method
        Method method = signature.getMethod();
        // 3.从Method中获取apiOperationLog注解
        ApiOperationLog apiOperationLog = method.getAnnotation(ApiOperationLog.class);

        return apiOperationLog.description();

    }

    private Function<Object, String> toJsonStr() {
        return JsonUtil::toSafeLogString;

    }
}