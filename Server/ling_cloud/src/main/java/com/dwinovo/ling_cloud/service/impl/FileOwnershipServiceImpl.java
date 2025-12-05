package com.dwinovo.ling_cloud.service.impl;

import com.dwinovo.ling_cloud.mapper.FileOwnershipMapper;
import com.dwinovo.ling_cloud.pojo.FileOwnership;
import com.dwinovo.ling_cloud.service.FileOwnershipService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 文件拥有关系服务实现
 */
@Service
public class FileOwnershipServiceImpl implements FileOwnershipService {

    @Autowired
    private FileOwnershipMapper fileOwnershipMapper;

    @Override
    public boolean userOwnsFile(byte[] fileHash, String userId) {
        if (fileHash == null || fileHash.length == 0 || userId == null || userId.isBlank()) {
            return false;
        }

        FileOwnership ownership = fileOwnershipMapper.findByUserAndFile(userId, fileHash);
        return ownership != null;
    }
}
