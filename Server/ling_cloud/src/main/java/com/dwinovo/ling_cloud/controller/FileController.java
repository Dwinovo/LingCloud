package com.dwinovo.ling_cloud.controller;

import com.dwinovo.ling_cloud.annotation.OperationLog;
import com.dwinovo.ling_cloud.common.ApiResponse;
import com.dwinovo.ling_cloud.common.StatusEnum;
import com.dwinovo.ling_cloud.dto.file.FileDownloadResponse;
import com.dwinovo.ling_cloud.dto.file.InitRequest;
import com.dwinovo.ling_cloud.dto.file.InitResponse;
import com.dwinovo.ling_cloud.dto.file.UserFileResponse;
import com.dwinovo.ling_cloud.service.FileService;
import com.dwinovo.ling_cloud.utils.JwtUtil;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/file")
public class FileController {
    @Autowired
    private FileService fileService;
    @Autowired
    private JwtUtil jwtUtil;
    
    @PostMapping("/init")
    @OperationLog("file:init")
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

    /**
     * POW 提交接口，payload 按顺序拼接 IV(12) | H(32) | TAG(16) | C(剩余密文)
     * Controller 只负责拿到二进制并转交给 Service 处理
     */
    @PostMapping("/pow")
    @OperationLog("file:pow")
    public ApiResponse<String> submitPow(
        @RequestParam("payload") MultipartFile payload,
        @RequestParam("filename") String filename,
        HttpServletRequest httpRequest
    ) {
        if (payload == null || payload.isEmpty()) {
            return ApiResponse.error(StatusEnum.BAD_REQUEST, "POW数据不能为空");
        }

        String userId = resolveUserId(httpRequest);
        if (userId == null) {
            return ApiResponse.error(StatusEnum.UNAUTHORIZED, "请先登录");
        }

        // 只负责读取字节，具体解析和业务逻辑由 Service 完成
        byte[] data;
        try {
            data = payload.getBytes();
        } catch (IOException e) {
            return ApiResponse.error(StatusEnum.BAD_REQUEST, "读取POW数据失败");
        }

        if (data.length < 60) {
            return ApiResponse.error(StatusEnum.BAD_REQUEST, "POW数据格式不正确");
        }

        fileService.processPowUpload(data, userId, filename);

        return ApiResponse.success("POW请求已受理");
    }

    @GetMapping("/list")
    public ApiResponse<List<UserFileResponse>> listUserFiles(HttpServletRequest httpRequest) {
        String userId = resolveUserId(httpRequest);
        if (userId == null) {
            return ApiResponse.error(StatusEnum.UNAUTHORIZED, "请先登录");
        }
        List<UserFileResponse> files = fileService.listUserFiles(userId).stream()
            .map(file -> new UserFileResponse(
                file.getId(),
                file.getFileHash(),
                file.getName(),
                file.getCreatedAt(),
                file.getUpdatedAt()
            ))
            .collect(Collectors.toList());
        return ApiResponse.success(files);
    }

    @GetMapping("/{id}")
    @OperationLog("file:download")
    public ApiResponse<FileDownloadResponse> downloadFile(@PathVariable("id") String id, HttpServletRequest httpRequest) {
        String userId = resolveUserId(httpRequest);
        if (userId == null) {
            return ApiResponse.error(StatusEnum.UNAUTHORIZED, "请先登录");
        }
        if (id == null || id.isBlank()) {
            return ApiResponse.error(StatusEnum.BAD_REQUEST, "文件ID不能为空");
        }
        FileDownloadResponse info = fileService.getFileDownloadInfo(userId, id);
        return ApiResponse.success(info);
    }

    @DeleteMapping("/{id}")
    @OperationLog("file:delete")
    public ApiResponse<String> deleteFile(@PathVariable("id") String id, HttpServletRequest httpRequest) {
        String userId = resolveUserId(httpRequest);
        if (userId == null) {
            return ApiResponse.error(StatusEnum.UNAUTHORIZED, "请先登录");
        }
        if (id == null || id.isBlank()) {
            return ApiResponse.error(StatusEnum.BAD_REQUEST, "文件ID不能为空");
        }
        fileService.deleteFile(userId, id);
        return ApiResponse.success("删除成功");
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
