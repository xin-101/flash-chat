package io.github.zh.auth.service.impl;

import io.github.zh.auth.config.sms.SmsConfigProperties;
import io.github.zh.auth.utils.sms.SendSmsUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class SMSServiceImpl {
    @Resource
    private SendSmsUtil sendSmsUtil;
    @Resource
    private SmsConfigProperties smsConfigProperties;

    @Async
    public void sendSms(String sendPhone, String sendCode) {
        if (smsConfigProperties.getSecretId() == null || smsConfigProperties.getSignName() == null) {
            log.warn("短信配置不完整（secretId/signName 为空），跳过实际发送，验证码：{} 有效期5分钟", sendCode);
            return;
        }
        try {
            sendSmsUtil.sendSms(sendPhone, sendCode);
        } catch (Exception e) {
            log.error("短信发送失败，手机号：{}，验证码：{}", sendPhone, sendCode, e);
        }
    }
}
