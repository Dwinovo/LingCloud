package com.dwinovo.ling_cloud.controller;

import com.dwinovo.ling_cloud.common.ApiResponse;
import com.dwinovo.ling_cloud.dto.LoginRequest;
import com.dwinovo.ling_cloud.dto.RegisterRequest;
import com.dwinovo.ling_cloud.dto.RegisterResponse;
import com.dwinovo.ling_cloud.dto.LoginResponse;
import com.dwinovo.ling_cloud.pojo.User;
import com.dwinovo.ling_cloud.service.UserService;
import com.dwinovo.ling_cloud.utils.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * 用户注册
     */
    @PostMapping("/register")
    public ApiResponse<RegisterResponse> register(@Valid @RequestBody RegisterRequest registerRequest) {
        User user = userService.register(registerRequest);

        // 创建响应对象，只包含必要信息，不包含敏感数据
        RegisterResponse response = new RegisterResponse();
        response.setUsername(user.getUsername());
        response.setNickname(user.getNickname());

        return ApiResponse.success(response, "注册成功");
    }

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest loginRequest, HttpServletResponse response) {

        // 验证用户凭据
        User user = userService.authenticateUser(loginRequest);

        // 生成JWT令牌
        Map<String, Object> claims = Map.of(
            "userId", user.getId(),
            "username", user.getUsername()
        );
        String token = jwtUtil.generateAccessToken(user.getUsername(), claims);

        // 设置HTTP Cookie
        ResponseCookie cookie = ResponseCookie.from("access_token", token)
                // 前端需要读取 token，因此在开发阶段关闭 HttpOnly
                .httpOnly(false)
                .secure(false)   // 开发环境用HTTP，生产环境设为true
                .path("/")
                .maxAge(24 * 60 * 60)  // 24小时
                .sameSite("Lax")  // CSRF保护
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        // 返回用户信息
        LoginResponse userInfo = new LoginResponse();
        userInfo.setUsername(user.getUsername());
        userInfo.setNickname(user.getNickname());

        return ApiResponse.success(userInfo, "登录成功");
    }
}
