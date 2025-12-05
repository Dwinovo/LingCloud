package com.dwinovo.ling_cloud.service.impl;

import com.dwinovo.ling_cloud.common.BusinessException;
import com.dwinovo.ling_cloud.common.StatusEnum;
import com.dwinovo.ling_cloud.dto.file.InitResponse;
import com.dwinovo.ling_cloud.mapper.FileMapper;
import com.dwinovo.ling_cloud.pojo.File;
import com.dwinovo.ling_cloud.pojo.PowStatus;
import com.dwinovo.ling_cloud.service.FileOwnershipService;
import com.dwinovo.ling_cloud.service.FileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class FileServiceImpl implements FileService {

    @Autowired
    private FileMapper fileMapper;

    @Autowired
    private FileOwnershipService fileOwnershipService;

    @Override
    public File findByH(byte[] H) {
        if (H == null || H.length == 0) {
            return null;
        }
        return fileMapper.findByHash(H);
    }

    @Override
    public InitResponse init(String userId, String hashHex) {
        if (hashHex == null || hashHex.isBlank()) {
            throw new BusinessException(StatusEnum.BAD_REQUEST.getCode(), "文件哈希不能为空");
        }
        if (userId == null || userId.isBlank()) {
            throw new BusinessException(StatusEnum.UNAUTHORIZED.getCode(), "请先登录");
        }

        byte[] hashBytes = hexStringToBytes(hashHex);
        // 先查files表
        File file = findByH(hashBytes);
        if (file == null) {
            // 如果files表中没有，后续也需要补充POW校验
            return new InitResponse(PowStatus.POW);
        }
        // 然后查owns表，如果该用户拥有这个文件，就成功，否则依旧POW
        boolean owns = fileOwnershipService.userOwnsFile(hashBytes, userId);
        PowStatus status = owns ? PowStatus.ALLOW : PowStatus.POW;
        return new InitResponse(status);
    }

    private byte[] hexStringToBytes(String hex) {
        if ((hex.length() & 1) == 1) {
            throw new BusinessException(StatusEnum.BAD_REQUEST.getCode(), "文件哈希格式不正确");
        }
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            int hi = Character.digit(hex.charAt(i), 16);
            int lo = Character.digit(hex.charAt(i + 1), 16);
            if (hi == -1 || lo == -1) {
                throw new BusinessException(StatusEnum.BAD_REQUEST.getCode(), "文件哈希格式不正确");
            }
            data[i / 2] = (byte) ((hi << 4) + lo);
        }
        return data;
    }
}
