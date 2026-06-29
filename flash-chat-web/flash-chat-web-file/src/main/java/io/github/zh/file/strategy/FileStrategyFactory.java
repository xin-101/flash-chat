package io.github.zh.file.strategy;

import io.github.zh.file.config.FileProperties;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class FileStrategyFactory {

    @Resource
    private LocalFileStrategy localFileStrategy;

    @Resource
    private MinioFileStrategy minioFileStrategy;

    @Resource
    private FileProperties fileProperties;

    private FileStrategy currentStrategy;

    @PostConstruct
    public void init() {
        String type = fileProperties.getStrategy();
        if ("minio".equalsIgnoreCase(type)) {
            currentStrategy = minioFileStrategy;
        } else {
            currentStrategy = localFileStrategy;
        }
        log.info("文件存储策略: {}", type);
    }

    public FileStrategy getStrategy() {
        return currentStrategy;
    }
}
