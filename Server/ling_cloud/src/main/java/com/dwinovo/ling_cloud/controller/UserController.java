package com.dwinovo.ling_cloud.controller;

import com.dwinovo.ling_cloud.common.ApiResponse;
import com.dwinovo.ling_cloud.pojo.User;
import com.dwinovo.ling_cloud.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private UserService userService;

    /**
     * 获取当前用户信息
     */
    @GetMapping("/me")
    public ApiResponse<User> getCurrentUser(HttpServletRequest request) {
        // 从请求中提取JWT令牌
        String token = extractTokenFromRequest(request);

        // 通过Service层处理业务逻辑（包括token验证）
        User user = userService.getUserByToken(token);

        return ApiResponse.success(user, "获取用户信息成功");
    }

    /**
     * 从请求中提取JWT令牌
     */
    private String extractTokenFromRequest(HttpServletRequest request) {
        // 尝试从Cookie中获取
        if (request.getCookies() != null) {
            for (var cookie : request.getCookies()) {
                if ("access_token".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
}