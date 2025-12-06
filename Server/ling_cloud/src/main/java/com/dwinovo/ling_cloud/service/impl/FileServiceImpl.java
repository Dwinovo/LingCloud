package com.dwinovo.ling_cloud.service.impl;

import com.dwinovo.ling_cloud.common.BusinessException;
import com.dwinovo.ling_cloud.common.StatusEnum;
import com.dwinovo.ling_cloud.dto.file.FileDownloadResponse;
import com.dwinovo.ling_cloud.dto.file.InitResponse;
import com.dwinovo.ling_cloud.mapper.FileMapper;
import com.dwinovo.ling_cloud.pojo.File;
import com.dwinovo.ling_cloud.pojo.PowSession;
import com.dwinovo.ling_cloud.pojo.PowStatus;
import com.dwinovo.ling_cloud.service.FileOwnershipService;
import com.dwinovo.ling_cloud.service.FileService;
import com.dwinovo.ling_cloud.service.RedisService;
import com.dwinovo.ling_cloud.utils.OSSUtils;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FileServiceImpl implements FileService {

    @Autowired
    private FileMapper fileMapper;

    @Autowired
    private FileOwnershipService fileOwnershipService;

    @Autowired
    private RedisService redisService;

    @Autowired
    private OSSUtils ossUtils;

    private static final long POW_SESSION_TTL_SECONDS = 5 * 60; // 5分钟有效期

    @Override
    public File findByH(String hashBase64) {
        if (hashBase64 == null || hashBase64.isBlank()) {
            return null;
        }
        return fileMapper.findByHash(hashBase64);
    }

    @Override
    public File findById(String id) {
        if (id == null || id.isBlank()) {
            return null;
        }
        return fileMapper.findById(id);
    }

    @Override
    public InitResponse init(String userId, String hashBase64) {
        if (hashBase64 == null || hashBase64.isBlank()) {
            throw new BusinessException(StatusEnum.BAD_REQUEST, "文件哈希不能为空");
        }
        if (userId == null || userId.isBlank()) {
            throw new BusinessException(StatusEnum.UNAUTHORIZED, "请先登录");
        }

        byte[] hashBytes;
        try {
            hashBytes = Base64.getDecoder().decode(hashBase64);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(StatusEnum.BAD_REQUEST, "文件哈希格式不正确");
        }
        if (hashBytes.length == 0) {
            throw new BusinessException(StatusEnum.BAD_REQUEST, "文件哈希不能为空");
        }

        String normalizedHashBase64 = Base64.getEncoder().encodeToString(hashBytes);
        // 先查files表
        File file = findByH(normalizedHashBase64);
        if (file == null) {
            // 如果files表中没有，说明是第一次上床
            cachePowSession(userId, normalizedHashBase64, PowStatus.FIRST);
            return new InitResponse(PowStatus.FIRST);
        }
        // 然后查owns表，如果该用户拥有这个文件，就成功，否则依旧POW
        boolean owns = fileOwnershipService.userOwnsFile(file.getId(), userId);
        PowStatus status = owns ? PowStatus.ALLOW : PowStatus.POW;
        if (status == PowStatus.POW) {
            cachePowSession(userId, normalizedHashBase64, status);
            return new InitResponse(status, file.getIv());
        }
        return new InitResponse(status);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    /**
     * 处理 POW 上传：拆 payload -> 验证 Redis 会话 -> 写入 files/files_owner（含 OSS 上传）
     */
    public void processPowUpload(byte[] payload, String userId, String originalFileName) {
        if (payload == null || payload.length < 60) {
            throw new BusinessException(StatusEnum.BAD_REQUEST, "POW数据格式不正确");
        }

        byte[] ivBytes = Arrays.copyOfRange(payload, 0, 12);
        byte[] hashBytes = Arrays.copyOfRange(payload, 12, 44);
        byte[] tagBytes = Arrays.copyOfRange(payload, 44, 60);
        byte[] cipherBytes = Arrays.copyOfRange(payload, 60, payload.length);

        if (cipherBytes.length == 0) {
            throw new BusinessException(StatusEnum.BAD_REQUEST, "密文内容不能为空");
        }

        String fileHashBase64 = Base64.getEncoder().encodeToString(hashBytes);
        String redisKey = String.format("pow:pending:%s:%s", userId, fileHashBase64);
        PowSession session = redisService.get(redisKey, PowSession.class);
        if (session == null) {
            throw new BusinessException(StatusEnum.BAD_REQUEST, "未找到待验证的POW任务");
        }
        if (!fileHashBase64.equals(session.getHashBase64())) {
            throw new BusinessException(StatusEnum.BAD_REQUEST, "POW数据与初始化信息不一致");
        }

        String ivBase64 = Base64.getEncoder().encodeToString(ivBytes);
        String tagBase64 = Base64.getEncoder().encodeToString(tagBytes);
        String macBase64 = calculateMac(ivBytes, tagBytes, cipherBytes);
        LocalDateTime now = LocalDateTime.now();
        File existing = findByH(fileHashBase64);

        if (session.getStatus() == PowStatus.FIRST) {
            String fileUrl = ossUtils.uploadEncryptedFile(cipherBytes);
            File saved = saveFileRecord(fileHashBase64, ivBase64, tagBase64, fileUrl, macBase64, now, originalFileName);
            fileOwnershipService.addOwnership(saved.getId(), fileHashBase64, userId);
            redisService.delete(redisKey);
            return;
        }

        if (existing == null) {
            throw new BusinessException(StatusEnum.BAD_REQUEST, "文件记录不存在，无法验证POW");
        }

        if (!Objects.equals(existing.getIv(), ivBase64) || !Objects.equals(existing.getTag(), tagBase64)) {
            throw new BusinessException(StatusEnum.BAD_REQUEST, "IV或TAG与记录不符，POW验证失败");
        }

        if (!Objects.equals(existing.getMac(), macBase64)) {
            throw new BusinessException(StatusEnum.BAD_REQUEST, "密文校验失败，POW验证失败");
        }

        fileOwnershipService.addOwnership(existing.getId(), fileHashBase64, userId);
        redisService.delete(redisKey);
    }

    @Override
    public List<File> listUserFiles(String userId) {
        if (userId == null || userId.isBlank()) {
            return Collections.emptyList();
        }
        return fileMapper.findByUserId(userId);
    }

    @Override
    public FileDownloadResponse getFileDownloadInfo(String userId, String fileId) {
        if (userId == null || userId.isBlank()) {
            throw new BusinessException(StatusEnum.UNAUTHORIZED, "请先登录");
        }
        if (fileId == null || fileId.isBlank()) {
            throw new BusinessException(StatusEnum.BAD_REQUEST, "文件ID不能为空");
        }
        File file = findById(fileId);
        if (file == null) {
            throw new BusinessException(StatusEnum.FILE_NOT_FOUND, "文件不存在或已被删除");
        }
        boolean owns = fileOwnershipService.userOwnsFile(fileId, userId);
        if (!owns) {
            throw new BusinessException(StatusEnum.ACCESS_DENIED, "无权下载该文件");
        }
        if (file.getFileUrl() == null || file.getFileUrl().isBlank()) {
            throw new BusinessException(StatusEnum.FILE_DOWNLOAD_FAILED, "文件存储信息缺失，无法下载");
        }
        return new FileDownloadResponse(
            file.getIv(),
            file.getTag(),
            file.getFileUrl()
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteFile(String userId, String fileId) {
        if (userId == null || userId.isBlank()) {
            throw new BusinessException(StatusEnum.UNAUTHORIZED, "请先登录");
        }
        if (fileId == null || fileId.isBlank()) {
            throw new BusinessException(StatusEnum.BAD_REQUEST, "文件ID不能为空");
        }

        File file = findById(fileId);
        if (file == null) {
            throw new BusinessException(StatusEnum.FILE_NOT_FOUND, "文件不存在");
        }

        boolean owns = fileOwnershipService.userOwnsFile(fileId, userId);
        if (!owns) {
            throw new BusinessException(StatusEnum.ACCESS_DENIED, "无权删除该文件");
        }

        fileOwnershipService.removeOwnership(fileId, userId);
        int remainingOwners = fileOwnershipService.countOwners(fileId);
        if (remainingOwners <= 0) {
            fileMapper.deleteById(fileId);
            if (file.getFileUrl() != null && !file.getFileUrl().isBlank()) {
                ossUtils.delete(file.getFileUrl());
            }
        }
    }

    private void cachePowSession(String userId, String hashBase64, PowStatus status) {
        PowSession session = new PowSession(userId, hashBase64, status);
        String key = String.format("pow:pending:%s:%s", userId, hashBase64);
        redisService.set(key, session, POW_SESSION_TTL_SECONDS);
    }

    private File saveFileRecord(String fileHashBase64, String ivBase64, String tagBase64, String fileUrl,
                                String macBase64, LocalDateTime timestamp, String originalFileName) {
        File existing = findByH(fileHashBase64);
        String normalizedName = originalFileName != null ? originalFileName.trim() : null;
        if (normalizedName != null && normalizedName.isEmpty()) {
            normalizedName = null;
        }
        if (existing == null) {
            File record = new File();
            record.setId(UUID.randomUUID().toString());
            record.setFileHash(fileHashBase64);
            record.setIv(ivBase64);
            record.setTag(tagBase64);
            record.setFileUrl(fileUrl);
            record.setMac(macBase64);
            record.setName(normalizedName);
            record.setCreatedAt(timestamp);
            record.setUpdatedAt(timestamp);
            fileMapper.insert(record);
            return record;
        } else {
            existing.setIv(ivBase64);
            existing.setTag(tagBase64);
            existing.setFileUrl(fileUrl);
            existing.setMac(macBase64);
            if (normalizedName != null && !normalizedName.isBlank()) {
                existing.setName(normalizedName);
            }
            existing.setUpdatedAt(timestamp);
            fileMapper.update(existing);
            return existing;
        }
    }

    private String calculateMac(byte[]... segments) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            for (byte[] segment : segments) {
                if (segment != null && segment.length > 0) {
                    digest.update(segment);
                }
            }
            byte[] hash = digest.digest();
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new BusinessException(StatusEnum.INTERNAL_SERVER_ERROR, "计算MAC失败");
        }
    }
}
