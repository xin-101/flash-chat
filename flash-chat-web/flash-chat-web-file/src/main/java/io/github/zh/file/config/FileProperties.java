package io.github.zh.file.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "file")
public class FileProperties {

    private String uploadDir = "./upload";

    private String accessPrefix = "/files";

    private String strategy = "local";

    private long maxSize = 10 * 1024 * 1024;
}
