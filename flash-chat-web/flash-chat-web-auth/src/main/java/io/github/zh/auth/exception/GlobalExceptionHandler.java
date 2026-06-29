package io.github.zh.auth.exception;


import io.github.zh.common.exception.BizException;
import io.github.zh.common.response.Response;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Optional;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    /**
     * 捕获自定义业务异常
     * @return
     */
    @ExceptionHandler({BizException.class})
    @ResponseBody
    public Response<Object> handleBizException(HttpServletRequest request, BizException e){
        log.warn("{} request fail,errorCode:{},errorMessage:{}",request.getRequestURI(),e.getErrorCode(),e.getErrorMessage());
        return Response.fail(e);
    }
    /**
     * 捕获参数检验异常
     * @return
     */
    @ExceptionHandler({MethodArgumentNotValidException.class})
    @ResponseBody
    public Response<Object> handleMethodArgumentNotValidException(HttpServletRequest request, MethodArgumentNotValidException e){
        // 参数错误异常码
        String errorCode = ResponseEnum.PARAM_ERROR.getErrorCode();
        // 获取 BindingResult
        BindingResult bindingResult = e.getBindingResult();

        StringBuilder sb=new StringBuilder();
        // 获取检验不通过的字段，并组合错误信息，格式为：email 邮箱格式不正确，当前值：‘123123’
        Optional.ofNullable(bindingResult.getFieldErrors()).ifPresent(errors ->
        {errors.forEach(error ->
            sb.append(error.getField())
                    .append(" ")
                    .append(error.getDefaultMessage())
                    .append("，当前值：")
                    .append(error.getRejectedValue())
                    .append("；")
        );
        });
        // 错误信息
        String errorMessage = sb.toString();

        log.warn("{} request error,errorCode:{},errorMessage:{}",request.getRequestURI(),errorCode,errorMessage);
        return Response.fail(errorCode,errorMessage);
    }
    /**
     * 捕获其他异常
     * @param request
     * @param e
     * @return
     */
    @ExceptionHandler({Exception.class})
    @ResponseBody
    public Response<Object> handleException(HttpServletRequest request, Exception e){
        log.error("{} request error",request.getRequestURI(),e);
        return Response.fail(ResponseEnum.SYSTEM_ERROR);
    }
}
