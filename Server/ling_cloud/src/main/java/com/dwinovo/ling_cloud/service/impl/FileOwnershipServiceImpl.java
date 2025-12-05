package com.dwinovo.ling_cloud.service.impl;

import com.dwinovo.ling_cloud.mapper.FileOwnershipMapper;
import com.dwinovo.ling_cloud.pojo.FileOwnership;
import com.dwinovo.ling_cloud.service.FileOwnershipService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 文件拥有关系服务实现
 */
@Service
public class FileOwnershipServiceImpl implements FileOwnershipService {

    @Autowired
    private FileOwnershipMapper fileOwnershipMapper;

    @Override
    public boolean userOwnsFile(String fileId, String userId) {
        if (fileId == null || fileId.isBlank() || userId == null || userId.isBlank()) {
            return false;
        }
        FileOwnership ownership = fileOwnershipMapper.findByUserAndFile(userId, fileId);
        return ownership != null;
    }

    @Override
    public void addOwnership(String fileId, String fileHashBase64, String userId) {
        if (fileId == null || fileId.isBlank() || fileHashBase64 == null || fileHashBase64.isBlank()
            || userId == null || userId.isBlank()) {
            return;
        }

        FileOwnership existing = fileOwnershipMapper.findByUserAndFile(userId, fileId);
        if (existing != null) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        FileOwnership ownership = new FileOwnership(
            UUID.randomUUID().toString(),
            fileId,
            fileHashBase64,
            userId,
            now,
            now
        );
        fileOwnershipMapper.insert(ownership);
    }

    @Override
    public void removeOwnership(String fileId, String userId) {
        if (fileId == null || fileId.isBlank() || userId == null || userId.isBlank()) {
            return;
        }
        fileOwnershipMapper.deleteByUserAndFile(userId, fileId);
    }

    @Override
    public int countOwners(String fileId) {
        if (fileId == null || fileId.isBlank()) {
            return 0;
        }
        Integer count = fileOwnershipMapper.countByFileId(fileId);
        return count == null ? 0 : count;
    }
}
