package com.dwinovo.ling_cloud.service;

/**
 * 文件拥有关系服务接口
 */
public interface FileOwnershipService {

    /**
     * 判断用户是否拥有指定文件
     *
     * @param fileHash 文件哈希（二进制32字节）
     * @param userId 用户ID
     * @return true如果用户拥有该文件，false如果不拥有
     */
    boolean userOwnsFile(byte[] fileHash, String userId);
}
