package io.github.zh.auth.exception;
import io.github.zh.common.exception.BaseExceptionInterface;
import lombok.AllArgsConstructor;
import lombok.Getter;


@Getter
@AllArgsConstructor
public enum ResponseEnum implements BaseExceptionInterface {
    // ====通用的异常枚举====
    // 系统异常
    SYSTEM_ERROR("auth-10000", "系统异常"),
    // 参数异常
    PARAM_ERROR("auth-10001", "参数异常"),
    // 用户不存在
    USER_NOT_EXIST("auth-10002", "用户不存在"),
    // 短信发送频繁
    SMS_SEND_FREQUENTLY("auth-10003", "短信发送频繁"),
    // 验证码错误
    SMS_CODE_ERROR("auth-10004", "验证码错误"),


    ;


    // 获取异常码
    private final String errorCode;

    // 获取异常信息
    private final String errorMessage;
}
