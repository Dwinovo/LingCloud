package com.dwinovo.ling_cloud.mapper;

import com.dwinovo.ling_cloud.pojo.File;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface FileMapper {

    /**
     * 根据文件哈希查找文件
     */
    File findByHash(@Param("hash") byte[] hash);

    /**
     * 插入新文件
     */
    int insert(File file);

    /**
     * 更新文件信息
     */
    int update(File file);
}