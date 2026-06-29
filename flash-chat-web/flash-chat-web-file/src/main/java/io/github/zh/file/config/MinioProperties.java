package io.github.zh.file.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "minio")
public class MinioProperties {

    private String endpoint = "http://localhost:9002";

    private String accessKey = "minioadmin";

    private String secretKey = "minioadmin";

    private String bucket = "flash-chat";

    private String accessPrefix = "/file/minio-files";
}
