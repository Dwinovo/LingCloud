package com.dwinovo.ling_cloud.controller;

import com.dwinovo.ling_cloud.common.ApiResponse;
import com.dwinovo.ling_cloud.dto.auth.LoginRequest;
import com.dwinovo.ling_cloud.dto.auth.LoginResponse;
import com.dwinovo.ling_cloud.dto.auth.RegisterRequest;
import com.dwinovo.ling_cloud.dto.auth.RegisterResponse;
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
        String token = jwtUtil.generateAccessToken(user.getId(), claims);

        // 设置HTTP Cookie
        ResponseCookie cookie = ResponseCookie.from("access_token", token)
                .httpOnly(true)  // 启用HttpOnly，防止XSS攻击
                .secure(true)    // HTTPS环境必须启用Secure
                .path("/")
                .maxAge(24 * 60 * 60)  // 24小时
                .sameSite("None")  // CSRF保护
                .domain(".huining.fun")  // 注意前面的点，允许所有子域名
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        // 返回用户信息
        LoginResponse userInfo = new LoginResponse();
        userInfo.setUsername(user.getUsername());
        userInfo.setNickname(user.getNickname());

        return ApiResponse.success(userInfo, "登录成功");
    }

    /**
     * 用户退出登录
     */
    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpServletResponse response) {
        // 创建一个过期的Cookie来删除现有的Cookie
        ResponseCookie cookie = ResponseCookie.from("access_token", "")
                .httpOnly(true)  // 与login时保持一致
                .secure(true)
                .path("/")
                .maxAge(0)  // 立即过期
                .sameSite("None")
                .domain(".huining.fun")
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        return ApiResponse.success(null, "退出登录成功");
    }
}
