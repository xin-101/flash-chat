package io.github.zh.file.controller;

import io.github.zh.common.response.Response;
import io.github.zh.file.config.FileProperties;
import io.github.zh.file.strategy.FileStrategyFactory;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping
public class FileController {

    @Resource
    private FileStrategyFactory strategyFactory;

    @Resource
    private FileProperties fileProperties;

    @PostMapping("/upload")
    public Response<Map<String, String>> upload(@RequestParam("file") MultipartFile file,
                                                @RequestParam(value = "dir", defaultValue = "common") String directory) {
        if (file.isEmpty()) {
            return Response.fail("文件不能为空");
        }
        if (file.getSize() > fileProperties.getMaxSize()) {
            return Response.fail("文件大小不能超过 " + fileProperties.getMaxSize() / (1024 * 1024) + "MB");
        }

        String filePath = strategyFactory.getStrategy().upload(file, directory);
        String url = strategyFactory.getStrategy().getFileUrl(filePath);

        Map<String, String> data = new HashMap<>();
        data.put("url", url);
        data.put("path", filePath);
        return Response.success(data);
    }

    @PostMapping("/upload/image")
    public Response<Map<String, String>> uploadImage(@RequestParam("file") MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return Response.fail("仅支持图片文件");
        }
        return upload(file, "images");
    }

    @PostMapping("/upload/avatar")
    public Response<Map<String, String>> uploadAvatar(@RequestParam("file") MultipartFile file) {
        return upload(file, "avatar");
    }
}
