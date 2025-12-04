package com.dwinovo.ling_cloud.service.impl;

import com.dwinovo.ling_cloud.common.BusinessException;
import com.dwinovo.ling_cloud.dto.LoginRequest;
import com.dwinovo.ling_cloud.dto.RegisterRequest;
import com.dwinovo.ling_cloud.mapper.UserMapper;
import com.dwinovo.ling_cloud.pojo.User;
import com.dwinovo.ling_cloud.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

    
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /**
     * 用户注册
     */
    @Override
    public User register(RegisterRequest registerRequest) {
        // 检查用户名是否已存在
        User existingUser = userMapper.findByUsername(registerRequest.getUsername());
        if (existingUser != null) {
            throw new BusinessException("用户名已存在");
        }

        // 创建新用户
        User user = new User();
        user.setId(UUID.randomUUID().toString());
        user.setUsername(registerRequest.getUsername());
        user.setPasswordHash(passwordEncoder.encode(registerRequest.getPassword())); // 使用BCrypt哈希密码
        user.setNickname(registerRequest.getNickname());
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());

        // 保存用户到数据库
        userMapper.insert(user);

        return user;
    }

    /**
     * 用户登录验证 - 返回用户信息
     */
    @Override
    public User authenticateUser(LoginRequest loginRequest) {
        // 查找用户
        User user = userMapper.findByUsername(loginRequest.getUsername());
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        // 使用BCrypt验证密码
        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPasswordHash())) {
            throw new BusinessException("密码错误");
        }

        return user;
    }

    /**
     * 根据用户名查找用户
     */
    @Override
    public User findByUsername(String username) {
        return userMapper.findByUsername(username);
    }

    /**
     * 根据ID查找用户
     */
    @Override
    public User findById(String id) {
        return userMapper.findById(id);
    }
}