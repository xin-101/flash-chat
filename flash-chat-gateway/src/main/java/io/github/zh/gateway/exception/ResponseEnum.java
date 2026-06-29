package io.github.zh.gateway.exception;
import io.github.zh.common.exception.BaseExceptionInterface;
import lombok.AllArgsConstructor;
import lombok.Getter;


@Getter
@AllArgsConstructor
public enum ResponseEnum implements BaseExceptionInterface {
    // ====通用的异常枚举====
    // 系统异常
    SYSTEM_ERROR("gateway-10000", "系统异常"),
    // 参数异常
    PARAM_ERROR("gateway-10001", "参数异常"),
    // IP频繁访问
    IP_ERROR("gateway-10002", "IP频繁访问"),


    ;


    // 获取异常码
    private final String errorCode;

    // 获取异常信息
    private final String errorMessage;
}
