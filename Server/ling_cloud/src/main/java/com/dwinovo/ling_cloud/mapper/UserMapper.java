package com.dwinovo.ling_cloud.mapper;

import com.dwinovo.ling_cloud.pojo.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserMapper {

    /**
     * 根据用户名查找用户
     */
    User findByUsername(@Param("username") String username);

    /**
     * 插入新用户
     */
    int insert(User user);

    /**
     * 根据ID查找用户
     */
    User findById(@Param("id") String id);

    /**
     * 更新用户信息
     */
    int update(User user);
}