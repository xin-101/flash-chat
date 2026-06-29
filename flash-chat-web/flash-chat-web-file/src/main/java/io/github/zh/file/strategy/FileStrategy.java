package io.github.zh.file.strategy;

import org.springframework.web.multipart.MultipartFile;

public interface FileStrategy {

    String upload(MultipartFile file, String directory);

    void delete(String filePath);

    String getFileUrl(String filePath);
}
