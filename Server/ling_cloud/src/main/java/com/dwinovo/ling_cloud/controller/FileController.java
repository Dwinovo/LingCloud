package com.dwinovo.ling_cloud.controller;

import com.dwinovo.ling_cloud.common.ApiResponse;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/file")
public class FileController {

    /**
     * 文件上传接口
     */
    @PostMapping("/upload")
    public ApiResponse<Void> upload(
            @RequestParam("hash_plain") String hashPlain,
            @RequestParam("file") MultipartFile file) {

        // TODO: 实现文件上传逻辑
        return ApiResponse.success(null, "文件上传成功");
    }
}