package io.github.zh.auth.config.sms;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "tencent.cloud")
public class SmsConfigProperties {

    private String secretId;
    private String secretKey;
    private String signName;
    private String smsSdkAppId;
    private String templateId;

}
