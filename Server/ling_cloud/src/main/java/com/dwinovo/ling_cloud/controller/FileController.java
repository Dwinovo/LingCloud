package com.dwinovo.ling_cloud.controller;

import com.dwinovo.ling_cloud.common.ApiResponse;
import com.dwinovo.ling_cloud.common.StatusEnum;
import com.dwinovo.ling_cloud.dto.file.InitRequest;
import com.dwinovo.ling_cloud.dto.file.InitResponse;
import com.dwinovo.ling_cloud.service.FileService;
import com.dwinovo.ling_cloud.utils.JwtUtil;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/file")
public class FileController {
    @Autowired
    private FileService fileService;
    @Autowired
    private JwtUtil jwtUtil;

    
    @PostMapping("/init")
    public ApiResponse<InitResponse> initUpload(@RequestBody InitRequest request, HttpServletRequest httpRequest) {
        if (request == null || request.getH() == null || request.getH().isBlank()) {
            return ApiResponse.error(StatusEnum.BAD_REQUEST, "文件哈希不能为空");
        }
        String userId = resolveUserId(httpRequest);
        if (userId == null) {
            return ApiResponse.error(StatusEnum.UNAUTHORIZED, "请先登录");
        }
        InitResponse response = fileService.init(userId, request.getH());
        return ApiResponse.success(response);
    }

    private String resolveUserId(HttpServletRequest request) {
        String token = jwtUtil.extractTokenFromCookies(request);
        
        if (token == null) {
            return null;
        }
        try {
            return jwtUtil.parse(token).getSubject();
        } catch (Exception e) {
            return null;
        }
    }
}
