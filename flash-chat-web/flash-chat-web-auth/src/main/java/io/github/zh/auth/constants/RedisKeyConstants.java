package io.github.zh.auth.constants;

public interface RedisKeyConstants {

    // 短信发送验证码key
    String SMS_CODE_KEY = "auth:sms:code:";

    String SMS_IP_KEY ="auth:ip:" ;

    // 用户token key
    String USER_TOKEN_KEY = "flash:chat:user:";
    // token反向映射，用于WebSocket验证
    String USER_TOKEN_REVERSE_KEY = "flash:chat:token:";
    // 闪聊号修改时间 key
    String FLASH_CHAT_NUM_UPDATE_KEY = "flash:chat:num:update:";
}
