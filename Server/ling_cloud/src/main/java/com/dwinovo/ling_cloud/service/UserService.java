package com.dwinovo.ling_cloud.service;

import com.dwinovo.ling_cloud.dto.LoginRequest;
import com.dwinovo.ling_cloud.dto.RegisterRequest;
import com.dwinovo.ling_cloud.pojo.User;

public interface UserService {

    /**
     * 用户注册
     */
    User register(RegisterRequest registerRequest);

    /**
     * 用户登录 - 返回用户信息用于验证
     */
    User authenticateUser(LoginRequest loginRequest);

    /**
     * 根据用户名查找用户
     */
    User findByUsername(String username);

    /**
     * 根据ID查找用户
     */
    User findById(String id);

    /**
     * 根据JWT令牌获取用户信息
     */
    User getUserByToken(String token);
}