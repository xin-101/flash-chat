package io.github.zh.file.controller;

import io.github.zh.file.config.MinioProperties;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

@Slf4j
@RestController
public class MinioFileController {

    private static final Map<String, String> MIME_TYPES = Map.of(
            "jpg", "image/jpeg", "jpeg", "image/jpeg", "png", "image/png",
            "gif", "image/gif", "webp", "image/webp", "svg", "image/svg+xml",
            "mp4", "video/mp4", "pdf", "application/pdf",
            "mp3", "audio/mpeg", "wav", "audio/wav"
    );

    private final MinioProperties minioProperties;
    private MinioClient minioClient;

    public MinioFileController(MinioProperties minioProperties) {
        this.minioProperties = minioProperties;
    }

    @PostConstruct
    public void init() {
        this.minioClient = MinioClient.builder()
                .endpoint(minioProperties.getEndpoint())
                .credentials(minioProperties.getAccessKey(), minioProperties.getSecretKey())
                .build();
    }

    @GetMapping("/minio-files/{directory}/{filename:.+}")
    public void serveFile(@PathVariable("directory") String directory,
                          @PathVariable("filename") String filename,
                          HttpServletResponse response) {
        String objectName = directory + "/" + filename;
        String contentType = guessContentType(objectName);
        response.setContentType(contentType);
        response.setHeader("Cache-Control", "public, max-age=31536000");

        try (InputStream is = minioClient.getObject(
                GetObjectArgs.builder().bucket(minioProperties.getBucket()).object(objectName).build());
             OutputStream os = response.getOutputStream()) {
            byte[] buf = new byte[8192];
            int len;
            while ((len = is.read(buf)) != -1) {
                os.write(buf, 0, len);
            }
            os.flush();
        } catch (Exception e) {
            log.warn("Minio 文件未找到: {}", objectName);
            response.setStatus(404);
        }
    }

    private String guessContentType(String objectName) {
        int dot = objectName.lastIndexOf('.');
        if (dot > 0) {
            String ext = objectName.substring(dot + 1).toLowerCase();
            String mime = MIME_TYPES.get(ext);
            if (mime != null) return mime;
        }
        return "application/octet-stream";
    }
}
