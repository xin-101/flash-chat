package io.github.zh.chat.exception;

import io.github.zh.common.exception.BizException;
import io.github.zh.common.response.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)
    public Response<?> handleBizException(BizException e) {
        log.error("业务异常: {}", e.getMessage());
        return Response.fail(e.getErrorCode(), e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public Response<?> handleException(Exception e) {
        log.error("系统异常: {}", e.getMessage(), e);
        return Response.fail("50000", "系统繁忙，请稍后重试");
    }
}