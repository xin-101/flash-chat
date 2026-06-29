package io.github.zh.auth.controller;


import io.github.zh.auth.service.impl.SMSServiceImpl;
import io.github.zh.common.aspect.log.annotation.ApiOperationLog;
import io.github.zh.common.response.Response;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/test")
public class TestController {

    @Resource
    private SMSServiceImpl sendSms;

    @GetMapping
    @ApiOperationLog(description = "测试111")
    public Response test() {

//        throw new BizException(ResponseEnum.USER_NOT_EXIST);

//        return Response.fail(ResponseEnum.SYSTEM_ERROR);

        sendSms.sendSms("10000000000","123456");

        return Response.success("flash-chat-web-auth");
    }
}

