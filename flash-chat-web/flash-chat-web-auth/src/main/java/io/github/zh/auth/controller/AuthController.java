package io.github.zh.auth.controller;

import io.github.zh.auth.constants.RedisKeyConstants;
import io.github.zh.auth.exception.ResponseEnum;
import io.github.zh.auth.service.UsersService;
import io.github.zh.auth.service.impl.SMSServiceImpl;
import io.github.zh.common.aspect.log.annotation.ApiOperationLog;
import io.github.zh.common.response.Response;
import io.github.zh.common.utils.ip.IPUtil;
import io.github.zh.common.utils.redis.RedisUtil;
import io.github.zh.model.auth.bo.LoginUserBO;
import io.github.zh.model.auth.bo.UpdateUserBO;
import io.micrometer.common.util.StringUtils;
import jakarta.annotation.Resource;
import java.util.Map;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 短信发送及登陆相关
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    @Resource
    private SMSServiceImpl smsService;

    @Resource
    private RedisUtil redisUtil;

    @Resource
    private UsersService usersService;

    /**
     * 发送短信验证码
     */
    @GetMapping("/sendSms")
    @ApiOperationLog(description = "短信发送验证码")
    public Response sendSms(@RequestParam("phone") String phone, HttpServletRequest request) {
        if (StringUtils.isEmpty(phone)){
            return Response.fail(ResponseEnum.PARAM_ERROR);
        }

        // 构造短信验证码 -6 位
        String code = String.valueOf((int)((Math.random()*9+1)*100000));
        smsService.sendSms(phone, code);

        redisUtil.setnx(RedisKeyConstants.SMS_CODE_KEY +phone, code, 5*60);

        String requestIp = IPUtil.getRequestIp(request);
        redisUtil.setnx(RedisKeyConstants.SMS_IP_KEY +requestIp, "1", 5*60);

        return Response.success();
    }
    /**
     * 登陆流程
     */
    @PostMapping("/login")
    @ApiOperationLog(description = "短信登录")
    public Response login(@RequestBody @Validated LoginUserBO loginUserBO) {
       return usersService.login(loginUserBO);


//        return Response.success();
    }
    /**
     * 修改用户信息
     */
    @PutMapping("/user/info")
    @ApiOperationLog(description = "修改用户信息")
    public Response updateUserInfo(@RequestHeader("userId") String userId,
                                   @RequestBody UpdateUserBO bo) {
        return usersService.updateUserInfo(userId, bo);
    }

    /**
     * 修改闪聊号
     */
    @PutMapping("/user/flashChatNum")
    @ApiOperationLog(description = "修改闪聊号")
    public Response updateFlashChatNum(@RequestHeader("userId") String userId,
                                       @RequestBody Map<String, String> body) {
        return usersService.updateFlashChatNum(userId, body.get("flashChatNum"));
    }

    /**
     * 登出流程
     */
    @PostMapping("/logout")
    @ApiOperationLog(description = "登出")
    public Response logout(@RequestHeader("userId") String userId) {
        String userTokenKey = RedisKeyConstants.USER_TOKEN_KEY + userId;
        // 原子获取并删除 forward key
        String token = redisUtil.getAndDel(userTokenKey);
        if (token != null && !token.isEmpty()) {
            redisUtil.del(RedisKeyConstants.USER_TOKEN_REVERSE_KEY + token);
        }
        return Response.success();
    }

}
