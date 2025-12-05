package com.dwinovo.ling_cloud.mapper;

import com.dwinovo.ling_cloud.pojo.FileOwnership;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface FileOwnershipMapper {

    /**
     * 根据文件哈希查找所有拥有者
     */
    java.util.List<FileOwnership> findByFileHash(@Param("hash") byte[] hash);

    /**
     * 检查用户是否拥有指定文件
     */
    FileOwnership findByUserAndFile(@Param("userId") String userId, @Param("hash") byte[] hash);

    /**
     * 添加文件拥有关系
     */
    int insert(FileOwnership fileOwnership);

    /**
     * 删除文件拥有关系
     */
    int delete(@Param("id") String id);
}