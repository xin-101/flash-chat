package io.github.zh.file.strategy;

import io.github.zh.file.config.MinioProperties;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Slf4j
@Component
public class MinioFileStrategy implements FileStrategy {

    private final MinioProperties minioProperties;
    private MinioClient minioClient;

    public MinioFileStrategy(MinioProperties minioProperties) {
        this.minioProperties = minioProperties;
    }

    @PostConstruct
    public void init() {
        this.minioClient = MinioClient.builder()
                .endpoint(minioProperties.getEndpoint())
                .credentials(minioProperties.getAccessKey(), minioProperties.getSecretKey())
                .build();
        ensureBucketExists();
    }

    private void ensureBucketExists() {
        try {
            boolean found = minioClient.bucketExists(
                    io.minio.BucketExistsArgs.builder().bucket(minioProperties.getBucket()).build());
            if (!found) {
                minioClient.makeBucket(
                        io.minio.MakeBucketArgs.builder().bucket(minioProperties.getBucket()).build());
                log.info("Minio bucket 已创建: {}", minioProperties.getBucket());
            }
        } catch (Exception e) {
            log.error("检查/创建 Minio bucket 失败", e);
        }
    }

    @Override
    public String upload(MultipartFile file, String directory) {
        String originalFilename = file.getOriginalFilename();
        String ext = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            ext = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String objectName = UUID.randomUUID().toString().replace("-", "") + ext;
        if (directory != null) {
            objectName = directory + "/" + objectName;
        }

        try {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(minioProperties.getBucket())
                    .object(objectName)
                    .stream(file.getInputStream(), file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build());
            log.info("Minio 文件上传成功: {}", objectName);
            return objectName;
        } catch (Exception e) {
            log.error("Minio 文件上传失败: {}", originalFilename, e);
            throw new RuntimeException("文件上传失败", e);
        }
    }

    @Override
    public void delete(String filePath) {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(minioProperties.getBucket())
                    .object(filePath)
                    .build());
            log.info("Minio 文件删除成功: {}", filePath);
        } catch (Exception e) {
            log.warn("Minio 文件删除失败: {}", filePath, e);
        }
    }

    @Override
    public String getFileUrl(String filePath) {
        return minioProperties.getAccessPrefix() + "/" + filePath;
    }
}
