package io.github.zh.common.response;

import io.github.zh.common.exception.BaseExceptionInterface;
import io.github.zh.common.exception.BizException;
import lombok.Data;

import java.io.Serializable;
@Data
public class Response<T> implements Serializable {

    // 是否成功 默认为true
    private boolean success = true;
    // 响应信息 默认为无
    private String message;
    // 状态码
    private String errorCode;
    // 响应数据
    private T data;


    //===成功响应===
    public static <T> Response<T> success() {
        Response<T> response = new Response<>();
        return response;
    }
    public static <T> Response<T> success(T data) {
        Response<T> response = new Response<>();
        response.setData(data);
        return response;
    }
    //===失败响应===
    public static <T> Response<T> fail() {
        Response<T> response = new Response<>();
        response.setSuccess(false);
        return response;
    }
    public static <T> Response<T> fail(String errorMessage) {
        Response<T> response = new Response<>();
        response.setSuccess(false);
        response.setMessage(errorMessage);
        return response;
    }
    public static <T> Response<T> fail(String errorCode, String errorMessage) {
        Response<T> response = new Response<>();
        response.setSuccess(false);
        response.setMessage(errorMessage);
        response.setErrorCode(errorCode);
        return response;
    }
    public static <T> Response<T> fail(BaseExceptionInterface e) {
        Response<T> response = new Response<>();
        response.setSuccess(false);
        response.setMessage(e.getErrorMessage());
        response.setErrorCode(e.getErrorCode());
        return response;
    }
    public static <T> Response<T> fail(BizException e) {
        Response<T> response = new Response<>();
        response.setSuccess(false);
        response.setMessage(e.getErrorMessage());
        response.setErrorCode(e.getErrorCode());
        return response;
    }

}
