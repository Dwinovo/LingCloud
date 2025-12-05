package com.dwinovo.ling_cloud.service;

/**
 * 文件拥有关系服务接口
 */
public interface FileOwnershipService {

    /**
     * 判断用户是否拥有指定文件
     */
    boolean userOwnsFile(String fileId, String userId);

    /**
     * 为用户添加文件拥有记录
     */
    void addOwnership(String fileId, String fileHashBase64, String userId);

    /**
     * 删除用户的文件拥有记录
     */
    void removeOwnership(String fileId, String userId);

    /**
     * 获取文件拥有者数量
     */
    int countOwners(String fileId);
}
