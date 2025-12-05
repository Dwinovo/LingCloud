package com.dwinovo.ling_cloud.mapper;

import com.dwinovo.ling_cloud.pojo.File;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface FileMapper {

    /**
     * 根据文件哈希查找文件
     */
    File findByHash(@Param("hash") String hash);

    /**
     * 根据主键ID查找文件
     */
    File findById(@Param("id") String id);

    /**
     * 插入新文件
     */
    int insert(File file);

    /**
     * 更新文件信息
     */
    int update(File file);

    /**
     * 查询用户拥有的文件列表
     */
    List<File> findByUserId(@Param("userId") String userId);

    /**
     * 根据ID删除文件
     */
    int deleteById(@Param("id") String id);
}
