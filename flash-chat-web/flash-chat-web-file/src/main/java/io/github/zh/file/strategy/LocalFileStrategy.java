package io.github.zh.file.strategy;

import io.github.zh.file.config.FileProperties;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Slf4j
@Component
public class LocalFileStrategy implements FileStrategy {

    private final FileProperties fileProperties;

    public LocalFileStrategy(FileProperties fileProperties) {
        this.fileProperties = fileProperties;
    }

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(Paths.get(fileProperties.getUploadDir()));
            log.info("本地文件存储目录已创建: {}", fileProperties.getUploadDir());
        } catch (IOException e) {
            log.error("创建文件存储目录失败", e);
        }
    }

    @Override
    public String upload(MultipartFile file, String directory) {
        // 校验 directory 无路径穿越
        if (directory != null && (directory.contains("..") || directory.contains("\\") || directory.startsWith("/"))) {
            throw new RuntimeException("非法目录路径");
        }
        String originalFilename = file.getOriginalFilename();
        String ext = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            ext = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String filename = UUID.randomUUID().toString().replace("-", "") + ext;
        String relativePath = (directory != null ? directory + "/" : "") + filename;
        Path uploadDir = Paths.get(fileProperties.getUploadDir()).toAbsolutePath().normalize();
        Path targetPath = uploadDir.resolve(relativePath).normalize();

        // 再次校验解析后的路径仍在上传目录内
        if (!targetPath.startsWith(uploadDir)) {
            throw new RuntimeException("非法文件路径");
        }

        try {
            Files.createDirectories(targetPath.getParent());
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
            log.info("文件上传成功: {}", relativePath);
            return relativePath;
        } catch (IOException e) {
            log.error("文件上传失败: {}", originalFilename, e);
            throw new RuntimeException("文件上传失败", e);
        }
    }

    @Override
    public void delete(String filePath) {
        try {
            Path uploadDir = Paths.get(fileProperties.getUploadDir()).toAbsolutePath().normalize();
            Path target = uploadDir.resolve(filePath).normalize();
            if (!target.startsWith(uploadDir)) {
                log.warn("非法删除路径: {}", filePath);
                return;
            }
            Files.deleteIfExists(target);
            log.info("文件删除成功: {}", filePath);
        } catch (IOException e) {
            log.warn("文件删除失败: {}", filePath, e);
        }
    }

    @Override
    public String getFileUrl(String filePath) {
        return fileProperties.getAccessPrefix() + "/" + filePath;
    }
}
