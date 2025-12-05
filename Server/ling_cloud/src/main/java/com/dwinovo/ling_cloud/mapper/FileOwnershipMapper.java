package com.dwinovo.ling_cloud.mapper;

import com.dwinovo.ling_cloud.pojo.FileOwnership;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface FileOwnershipMapper {

    /**
     * 检查用户是否拥有指定文件
     */
    FileOwnership findByUserAndFile(@Param("userId") String userId, @Param("fileId") String fileId);

    /**
     * 添加文件拥有关系
     */
    int insert(FileOwnership fileOwnership);

    /**
     * 删除指定用户的拥有关系
     */
    int deleteByUserAndFile(@Param("userId") String userId, @Param("fileId") String fileId);

    /**
     * 统计文件拥有者数量
     */
    int countByFileId(@Param("fileId") String fileId);
}
